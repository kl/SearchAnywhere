use crate::build::DatabaseOptions;
use crate::search;
use crate::search::{MatchType, SearchQuery};
use crate::{build, stat};
use jni::objects::{GlobalRef, JBooleanArray, JObject, JObjectArray, JString, JValue};
use jni::strings::JNIString;
use jni::sys::{jint, jlong, jobjectArray, jsize, JNI_ERR, JNI_VERSION_1_6};
use jni::JavaVM;
use jni::{JNIEnv, NativeMethod};
use std::ffi::c_void;
use std::fmt::Debug;
use std::fs::File;
use std::io::BufReader;
use std::panic;
use std::sync::OnceLock;

static ANDROID_ENTRY_POINT_CLASS: &str = "se/kalind/searchanywhere/data/files/AnlocateLibrary";

static STRING_CLASS: OnceLock<GlobalRef> = OnceLock::new();
static LOG_CLASS: OnceLock<GlobalRef> = OnceLock::new();

#[allow(non_snake_case)]
#[no_mangle]
pub extern "C" fn JNI_OnLoad(vm: JavaVM) -> jint {
    let Ok(mut env) = vm.get_env() else {
        return JNI_ERR;
    };

    // cache classes for performance
    let Ok(string_class) = class_global_ref(&mut env, "java/lang/String") else {
        return JNI_ERR;
    };
    let _ = STRING_CLASS.set(string_class);

    let Ok(log_class) = class_global_ref(&mut env, "android/util/Log") else {
        return JNI_ERR;
    };
    let _ = LOG_CLASS.set(log_class);

    let Ok(entry_point) = env.find_class(ANDROID_ENTRY_POINT_CLASS) else {
        return JNI_ERR;
    };

    let ret = env.register_native_methods(
        entry_point,
        &[
            NativeMethod {
                name: "nativeBuildDatabase".into(),
                sig: "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V".into(),
                fn_ptr: native_build_database as *mut c_void,
            },
            NativeMethod {
                name: "nativeFindFiles".into(),
                sig: "(Ljava/lang/String;[Ljava/lang/String;[Z)[Ljava/lang/String;".into(),
                fn_ptr: native_find_files as *mut c_void,
            },
            NativeMethod {
                name: "nativeGetStatIndexedFiles".into(),
                sig: "(Ljava/lang/String;)J".into(),
                fn_ptr: native_get_stat_indexed_files as *mut c_void,
            },
        ],
    );
    if ret.is_err() {
        logcat_d(&mut env, "JNI_OnLoad error");
        return JNI_ERR;
    }
    logcat_d(&mut env, "JNI_OnLoad ok");
    JNI_VERSION_1_6
}

pub extern "C" fn native_build_database<'local>(
    mut env: JNIEnv<'local>,
    _obj: JObject<'local>,
    db_file: JString<'local>,
    scan_root: JString<'local>,
    temp_dir: JString<'local>,
) {
    let Ok(db_file) = get_string(&mut env, &db_file) else {
        return;
    };
    let Ok(scan_root) = get_string(&mut env, &scan_root) else {
        return;
    };
    let Ok(temp_dir) = get_string(&mut env, &temp_dir) else {
        return;
    };

    logcat_d(&mut env, format!("db: {}", db_file));
    logcat_d(&mut env, format!("scan_root: {}", scan_root));
    logcat_d(&mut env, format!("temp_dir: {}", temp_dir));

    let result = panic::catch_unwind(|| {
        let options = DatabaseOptions {
            temp_dir: temp_dir.into(),
            remove_root: true,
            ..Default::default()
        };
        build::build_database(db_file, scan_root, options)
    });
    throw_if_err(&mut env, &result);
    logcat_d(&mut env, "build db ok");
}

// this function gets called frequently, so it should be as fast as possible
pub extern "C" fn native_find_files<'local>(
    mut env: JNIEnv<'local>,
    _obj: JObject<'local>,
    db_file: JString<'local>,
    query: JObjectArray<'local>,
    include_exclude: JBooleanArray<'local>,
) -> jobjectArray {
    let null = JObject::null().into_raw();

    let Some(string_class) = STRING_CLASS.get() else {
        throw(
            &mut env,
            "java/lang/IllegalStateException",
            "class cache was not initialized",
        );
        return null;
    };

    // if the following fails something is seriously wrong so don't attempt to throw an exception
    let Ok(db_file) = get_string(&mut env, &db_file) else {
        return null;
    };
    let Ok(query_len) = env.get_array_length(&query) else {
        return null;
    };
    let Ok(include_exclude_len) = env.get_array_length(&include_exclude) else {
        return null;
    };

    if query_len != include_exclude_len {
        throw(
            &mut env,
            "java/lang/IllegalStateException",
            "include_exclude length must be the same as query length",
        );
        return null;
    }

    let mut include_exclude_vec = vec![0u8; query_len as usize];
    if env
        .get_boolean_array_region(&include_exclude, 0, &mut include_exclude_vec)
        .is_err()
    {
        return null;
    }

    let mut query_vec: Vec<String> = Vec::new();

    for i in 0..query_len {
        let Ok(obj) = env.get_object_array_element(&query, i) else {
            return null;
        };
        let Ok(query_str) = get_string(&mut env, &JString::from(obj)) else {
            return null;
        };
        query_vec.push(query_str);
    }

    let search_query = make_search_queries(&query_vec, &include_exclude_vec);

    // call the lib search function
    let result = panic::catch_unwind(|| {
        let mut reader = BufReader::new(File::open(db_file).expect("failed to open database file"));
        search::search(&mut reader, &search_query)
    });

    throw_if_err(&mut env, &result);

    if let Ok(Ok(files)) = result {
        // will probably never happen but better safe than sorry
        let files = if files.len() > (jsize::MAX as usize) {
            &files[0..(jsize::MAX as usize)]
        } else {
            files.as_slice()
        };

        let Ok(obj_array) = env.new_object_array(
            files.len().try_into().unwrap(), // safety: never panics
            string_class,
            JObject::null(),
        ) else {
            return null;
        };

        for (i, file) in files.iter().enumerate() {
            let Ok(java_string) = env.new_string(file) else {
                return null;
            };
            let index: jsize = i.try_into().unwrap(); // safety: never panics
            if env
                .set_object_array_element(&obj_array, index, java_string)
                .is_err()
            {
                return null;
            }
        }
        obj_array.into_raw()
    } else {
        null
    }
}

pub extern "C" fn native_get_stat_indexed_files<'local>(
    mut env: JNIEnv<'local>,
    _obj: JObject<'local>,
    db_file: JString<'local>,
) -> jlong {
    let Ok(db_file) = get_string(&mut env, &db_file) else {
        return -1;
    };
    // call the lib stats function
    let result = panic::catch_unwind(|| {
        let mut reader = BufReader::new(File::open(db_file).expect("failed to open database file"));
        stat::get_stats(&mut reader)
    });

    throw_if_err(&mut env, &result);

    if let Ok(Ok(stats)) = result {
        stats.indexed_files.min(jlong::MAX as u64) as jlong
    } else {
        -1
    }
}

#[inline]
fn class_global_ref(
    env: &mut JNIEnv,
    class: impl Into<JNIString>,
) -> jni::errors::Result<GlobalRef> {
    env.find_class(class).and_then(|c| env.new_global_ref(c))
}

#[inline]
fn throw_if_err<E1, T2, E2>(env: &mut JNIEnv, result: &Result<Result<T2, E2>, E1>)
where
    E1: Debug,
    E2: Debug,
{
    match result {
        Err(err) => throw(
            env,
            "java/lang/Exception",
            format!("anlocate error: {:?}", err),
        ),
        Ok(Err(err)) => throw(
            env,
            "java/lang/Exception",
            format!("anlocate error: {:?}", err),
        ),
        _ => {}
    }
}

#[inline]
fn get_string<'local>(
    env: &mut JNIEnv<'local>,
    jstring: &JString<'local>,
) -> jni::errors::Result<String> {
    env.get_string(jstring)
        .map(|java_string| java_string.into())
}

#[inline]
fn throw(env: &mut JNIEnv, exception_class: &str, message: impl Into<JNIString>) {
    if let Ok(false) = env.exception_check() {
        let _ = env.throw_new(exception_class, message);
    }
}

fn logcat_d(env: &mut JNIEnv, message: impl Into<JNIString>) {
    // trying to log if there is an exception will crash
    match env.exception_check() {
        Err(_) | Ok(true) => return,
        _ => {}
    }

    let Some(log_class) = LOG_CLASS.get() else {
        throw(
            env,
            "java/lang/IllegalStateException",
            "class cache was not initialized",
        );
        return;
    };
    let Ok(tag_string) = env.new_string("ANLOCATE_NATIVE") else {
        return;
    };
    let Ok(message_string) = env.new_string(message) else {
        return;
    };

    let _ = env.call_static_method(
        log_class,
        "d",
        "(Ljava/lang/String;Ljava/lang/String;)I",
        &[
            JValue::Object(&JObject::from(tag_string)),
            JValue::Object(&JObject::from(message_string)),
        ],
    );
}

fn make_search_queries<'a>(
    query_vec: &'a [String],
    include_exclude_vec: &[u8],
) -> Vec<SearchQuery<'a>> {
    query_vec
        .iter()
        .zip(include_exclude_vec.iter())
        .map(|(q, i)| {
            let match_type = if *i == 1 {
                MatchType::Include
            } else {
                MatchType::Exclude
            };
            SearchQuery::new(q.as_str(), match_type)
        })
        .collect()
}
