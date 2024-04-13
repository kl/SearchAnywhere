use crate::{compress, util};
use nanorand::Rng;
use std::fs::File;
use std::io::{BufReader, BufWriter, ErrorKind, Write};
use std::os::unix::ffi::OsStrExt;
use std::path::{Path, PathBuf};
use std::sync::mpsc;
use std::sync::mpsc::Receiver;
use std::{env, fs, io};
use std::{mem, thread};

#[derive(Debug, Clone)]
/// Options for creating the database
pub struct DatabaseOptions {
    /// The file system walker will store file paths in memory up until `mem_limit` bytes of
    /// memory is used. After this limit is exceeded, the path buffer is flushed to a part file.
    pub mem_limit: usize,
    /// Whether to compress the database or not.
    pub compress: bool,
    /// If true the scan root directory prefix path will not be included in the database.
    pub remove_root: bool,
    /// The path to the dir where temporary .part files are written.
    pub temp_dir: PathBuf,
}

impl Default for DatabaseOptions {
    fn default() -> Self {
        DatabaseOptions {
            mem_limit: 2 * 1000 * 1000, // 2 MB
            compress: true,
            remove_root: false,
            temp_dir: env::temp_dir(),
        }
    }
}

pub fn build_database<P1: AsRef<Path>, P2: Into<PathBuf>>(
    db_file: P1,
    scan_root: P2,
    options: DatabaseOptions,
) -> io::Result<()> {
    let (tx, rx) = mpsc::channel::<Vec<PathBuf>>();

    // db writer thread
    let database = db_file.as_ref().to_owned();
    let options_clone = options.clone();
    let handle =
        thread::spawn(move || -> io::Result<()> { write_database(rx, database, options_clone) });

    // scan root dir and send files to writer thread
    walk_dir(scan_root, options, |files| {
        match tx.send(files) {
            Ok(_) => WalkStatus::Ok,
            // the writer thread returned an error or panicked so abort the walk
            Err(_) => WalkStatus::Aborted,
        }
    })?;

    drop(tx);
    handle.join().unwrap()?;
    Ok(())
}

struct RemoveDirOnDrop<'a>(&'a Path);

impl Drop for RemoveDirOnDrop<'_> {
    fn drop(&mut self) {
        if self.0.is_dir() {
            let _ = fs::remove_dir_all(self.0);
        }
    }
}

fn write_database(
    rx: Receiver<Vec<PathBuf>>,
    db_path: PathBuf,
    options: DatabaseOptions,
) -> io::Result<()> {
    let temp_dir = options.temp_dir.join(format!(
        "anlocate-{}",
        nanorand::tls_rng().generate::<u64>()
    ));
    fs::create_dir_all(&temp_dir)?;
    let _dropper = RemoveDirOnDrop(&temp_dir);

    let mut part_file_paths = Vec::new();
    for (i, mut files) in rx.iter().enumerate() {
        let path = temp_dir.join(format!("{i}.part"));
        let mut part_file = BufWriter::new(File::create(&path)?);
        part_file_paths.push(path);

        files.sort_unstable_by(|a, b| a.as_os_str().cmp(b.as_os_str()));
        for path in files {
            part_file.write_all(path.as_os_str().as_bytes())?;
            part_file.write_all(&[b'\n'])?;
        }
        part_file.flush()?;
    }

    write_database_from_parts(&db_path, &part_file_paths, options.compress)?;
    Ok(())
}

struct LineHolder {
    file: BufReader<File>,
    line: Vec<u8>,
}

impl LineHolder {
    fn new(file: File) -> LineHolder {
        LineHolder {
            file: BufReader::new(file),
            line: Vec::new(),
        }
    }

    fn read_line(&mut self) -> io::Result<()> {
        self.line.clear();
        loop {
            let bytes_read = util::read_line(&mut self.file, &mut self.line)?;
            if bytes_read == 0 {
                break;
            }
            // skip consecutive newlines
            if !self.line.is_empty() {
                break;
            }
        }
        Ok(())
    }
}

fn write_database_from_parts(
    db_file: &Path,
    part_files: &[PathBuf],
    compress: bool,
) -> io::Result<()> {
    if let Some(parent) = db_file.parent() {
        fs::create_dir_all(parent)?;
    }
    let mut database = File::create(db_file)?;
    if part_files.is_empty() {
        return Ok(());
    }

    let mut holders: Vec<LineHolder> = part_files
        .iter()
        .map(|p| -> io::Result<LineHolder> {
            let file = File::open(p)?;
            let mut holder = LineHolder::new(file);
            holder.read_line()?; // prime the pump
            Ok(holder)
        })
        .collect::<io::Result<_>>()?;

    let mut line_buf = Vec::new();
    let mut prev = Vec::new();
    loop {
        let smallest = holders
            .iter_mut()
            .reduce(|acc, e| {
                if acc.line.is_empty() {
                    e
                } else if e.line.is_empty() {
                    acc
                } else if e.line <= acc.line {
                    e
                } else {
                    acc
                }
            })
            .unwrap(); // we always have at least one holder

        // no more lines
        if smallest.line.is_empty() {
            break;
        } else {
            if compress {
                compress::compress_line(&mut line_buf, &prev, &smallest.line);
            } else {
                line_buf.write_all(&smallest.line)?;
                line_buf.push(b'\n');
            }
            prev = mem::take(&mut smallest.line);
            database.write_all(&line_buf)?;
            line_buf.clear();
            smallest.read_line()?;
        }
    }
    Ok(())
}

#[derive(Debug, PartialEq)]
enum WalkStatus {
    Ok,
    Aborted,
}

fn walk_dir<P, F>(root: P, options: DatabaseOptions, on_flush: F) -> io::Result<()>
where
    P: Into<PathBuf>,
    F: Fn(Vec<PathBuf>) -> WalkStatus,
{
    #[inline]
    fn strip_root(path: PathBuf, root: Option<&Path>) -> PathBuf {
        if let Some(root) = root {
            path.strip_prefix(root)
                .expect("root was not prefix")
                .to_path_buf()
        } else {
            path
        }
    }

    fn walk_dir_internal<F>(
        dir: PathBuf,
        remove_root: Option<&Path>, // if set the root (prefix path) will not be in the output
        mem_limit: usize,
        on_flush: &F,
        files: &mut Vec<PathBuf>,
        size: &mut usize,
    ) -> io::Result<WalkStatus>
    where
        F: Fn(Vec<PathBuf>) -> WalkStatus,
    {
        let mut entries = match fs::read_dir(&dir) {
            Ok(entries) => entries.peekable(),
            // if permission denied, ignore and continue walk
            Err(err) if err.kind() == ErrorKind::PermissionDenied => return Ok(WalkStatus::Ok),
            Err(err) => return Err(err),
        };
        if entries.peek().is_none() {
            // dir is a leaf (empty dir) so add it to the files list
            files.push(strip_root(dir, remove_root));
        } else {
            for entry in entries {
                let entry = entry?;
                let path = entry.path();
                if path.is_dir() {
                    let status =
                        walk_dir_internal(path, remove_root, mem_limit, on_flush, files, size);
                    if let Err(_) | Ok(WalkStatus::Aborted) = status {
                        return status;
                    }
                } else {
                    let path = strip_root(path, remove_root);
                    let elem_size = path.as_os_str().as_bytes().len() + mem::size_of::<PathBuf>();
                    let new_size = *size + elem_size;
                    if new_size >= mem_limit {
                        *size = elem_size;
                        if on_flush(mem::take(files)) == WalkStatus::Aborted {
                            return Ok(WalkStatus::Aborted);
                        }
                    } else {
                        *size = new_size;
                    }
                    files.push(path);
                }
            }
        }
        Ok(WalkStatus::Ok)
    }

    let root = root.into();
    if !root.is_dir() {
        panic!("root is not a directory: {:?}", root);
    }
    let mut files = Vec::new();
    let remove_root = if options.remove_root {
        Some(root.as_ref())
    } else {
        None
    };
    let status = walk_dir_internal(
        root.clone(),
        remove_root,
        options.mem_limit,
        &on_flush,
        &mut files,
        &mut 0,
    )?;
    // flush any remaining file paths
    if status == WalkStatus::Ok && !files.is_empty() {
        on_flush(files);
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempdir::TempDir;

    #[test]
    fn test_build_database() {
        let tmp_dir = TempDir::new("test_build_database").unwrap();
        let db_path = tmp_dir.path().join("database.anlocate");
        let options = DatabaseOptions::default();
        build_database(&db_path, "tests/root", options).unwrap();
        assert!(db_path.is_file());
        let content = fs::read(db_path).unwrap();
        // println!("{}", String::from_utf8_lossy(&content));

        let expected: Vec<u8> = vec![
            &[0], "tests/root/cmd".as_bytes(), &[b'\n'],
            &[11], "usr/src/aardvark.c".as_bytes(), &[b'\n'],
            &[20], "rmadillo.c".as_bytes(), &[b'\n'],
            &[15], "tmp/zoo".as_bytes(), &[b'\n'],
            &[11], "x/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/file1.sh".as_bytes(), &[b'\n'],
            // 253=needs 2 byte to store, 26=LSB, 1=MSB
            &[253, 26, 1], "2.jpg".as_bytes(), &[b'\n'],
        ]
            .iter()
            .fold(Vec::new(), |mut fold, bytes| {
                for b in bytes.iter() {
                    fold.push(*b);
                }
                fold
            });

        assert_eq!(content, expected);
    }

    #[test]
    fn test_build_database_remove_root() {
        let tmp_dir = TempDir::new("test_build_database_remove_root").unwrap();
        let db_path = tmp_dir.path().join("database.anlocate");
        let mut options = DatabaseOptions::default();
        options.remove_root = true;
        build_database(&db_path, "tests/root", options).unwrap();
        assert!(db_path.is_file());
        let content = fs::read(db_path).unwrap();
        println!("{}", String::from_utf8_lossy(&content));

        let expected: Vec<u8> = vec![
            &[0], "cmd".as_bytes(), &[b'\n'],
            &[0], "usr/src/aardvark.c".as_bytes(), &[b'\n'],
            &[9], "rmadillo.c".as_bytes(), &[b'\n'],
            &[4], "tmp/zoo".as_bytes(), &[b'\n'],
            &[0], "x/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/file1.sh".as_bytes(), &[b'\n'],
            // 253=needs 2 byte to store, 28=LSB, 1=MSB
            &[253, 15, 1], "2.jpg".as_bytes(), &[b'\n'],
        ]
            .iter()
            .fold(Vec::new(), |mut fold, bytes| {
                for b in bytes.iter() {
                    fold.push(*b);
                }
                fold
            });

        assert_eq!(content, expected);
    }
}
