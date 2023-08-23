use crate::build;
use crate::build::DatabaseOptions;
use crate::search;
use jni::objects::{JClass, JObject, JString, JValue, ReleaseMode};
use jni::strings::JNIString;
use jni::sys::jobjectArray;
use jni::sys::jstring;
use jni::JNIEnv;
use std::fmt::Debug;
use std::fs::File;
use std::io::{BufReader, Write};
use std::panic;
use std::path::Path;

#[allow(non_snake_case)]
#[no_mangle]
pub extern "C" fn Java_se_kalind_searchanywhere_data_files_AnlocateLibrary_nativeBuildDatabase<
    'local,
>(
    mut env: JNIEnv<'local>,
    _obj: JObject<'local>,
    db_file: JString<'local>,
    scan_root: JString<'local>,
    temp_dir: JString<'local>,
) {
    let db_file = get_string(&mut env, &db_file);
    let scan_root = get_string(&mut env, &scan_root);
    let temp_dir = get_string(&mut env, &temp_dir);

    logcat_d(&mut env, format!("db: {}", db_file));
    logcat_d(&mut env, format!("scan_root: {}", scan_root));
    logcat_d(&mut env, format!("temp_dir: {}", temp_dir));

    let result = panic::catch_unwind(|| {
        let mut options = DatabaseOptions::default();
        options.temp_dir = temp_dir.into();
        build::build_database(db_file, scan_root, options)
    });
    throw_if_err(&mut env, &result);
    logcat_d(&mut env, "build db ok");
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "C" fn Java_se_kalind_searchanywhere_data_files_AnlocateLibrary_nativeFindFiles<
    'local,
>(
    mut env: JNIEnv<'local>,
    _obj: JObject<'local>,
    db_file: JString<'local>,
    query: JString<'local>,
) -> jobjectArray {
    let db_file = get_string(&mut env, &db_file);
    let query = get_string(&mut env, &query);

    let result = panic::catch_unwind(|| {
        let mut reader = BufReader::new(File::open(db_file).expect("failed to open database file"));
        search::search(&mut reader, &query)
    });
    let string_class = env.find_class("java/lang/String").unwrap();
    throw_if_err(&mut env, &result);
    if let Ok(Ok(files)) = result {
        logcat_d(&mut env, "search ok");
        let obj_array = env
            .new_object_array(
                files.len().try_into().unwrap(),
                string_class,
                JObject::null(),
            )
            .unwrap();
        for (i, file) in files.into_iter().enumerate() {
            let java_string = env.new_string(file).unwrap();
            env.set_object_array_element(&obj_array, i.try_into().unwrap(), java_string)
                .unwrap();
        }
        obj_array.into_raw()
    } else {
        JObject::null().into_raw()
    }
}

fn throw_if_err<'local, E1, T2, E2>(env: &mut JNIEnv<'local>, result: &Result<Result<T2, E2>, E1>)
where
    E1: Debug,
    E2: Debug,
{
    match result {
        Err(err) => throw(env, format!("anlocate error: {:?}", err)),
        Ok(Err(err)) => throw(env, format!("anlocate error: {:?}", err)),
        _ => {}
    }
}

fn get_string<'local>(env: &mut JNIEnv<'local>, jstring: &JString<'local>) -> String {
    env.get_string(jstring)
        .unwrap() // this should never panic because jstring is a valid java string
        .into()
}

fn throw<'a>(env: &mut JNIEnv<'a>, message: impl Into<JNIString>) {
    if let Ok(false) = env.exception_check() {
        let _ = env.throw_new("java/lang/Exception", message);
    }
}

fn logcat_d<'a>(env: &mut JNIEnv<'a>, message: impl Into<JNIString>) {
    // trying to log if there is an exception will crash
    match env.exception_check() {
        Err(_) | Ok(true) => return,
        _ => {}
    }

    let log_class = env.find_class("android/util/Log").unwrap(); // should never panic
    let tag = JObject::from(env.new_string("ANLOCATE_NATIVE").unwrap()); // should never panic
    let message = JObject::from(env.new_string(message).unwrap()); // should never panic

    let _ = env.call_static_method(
        &log_class,
        "d",
        "(Ljava/lang/String;Ljava/lang/String;)I",
        &[JValue::Object(&tag), JValue::Object(&message)],
    );
}
