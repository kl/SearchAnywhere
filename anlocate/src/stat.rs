use crate::util;
use std::io::{self, BufReader, Read};

#[derive(Debug, PartialEq)]
pub struct Stats {
    pub indexed_files: u64,
    pub size_bytes: u64,
}

pub fn get_stats(database_file_reader: &mut BufReader<impl Read>) -> Result<Stats, StatsError> {
    let mut buf = vec![];
    let mut lines = 0;
    let mut bytes = 0;

    loop {
        let read = util::read_db_entry_include_newline(database_file_reader, &mut buf)?;
        if read == 0 {
            break;
        }
        buf.clear();
        bytes += read as u64;
        lines += 1;
    }

    Ok(Stats {
        indexed_files: lines,
        size_bytes: bytes,
    })
}

#[derive(Debug)]
pub enum StatsError {
    IO(io::Error),
}

impl From<io::Error> for StatsError {
    fn from(error: io::Error) -> Self {
        StatsError::IO(error)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs::File;
    use std::io::Write;
    use tempfile::TempDir;

    #[test]
    fn test_stats() {
        let compressed: Vec<u8> = vec![
            &[0], "/usr/src".as_bytes(), &[b'\n'],
            &[8], "/cmd/aardvark.c".as_bytes(), &[b'\n'],
            &[14], "rmadillo.c".as_bytes(), &[b'\n'],
            &[5], "tmp/zoo".as_bytes(), &[b'\n'],
            &[1], "x/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/file1.sh".as_bytes(), &[b'\n'],
            // 272 common chars -> 253=needs 2 byte to store, 16=LSB, 1=MSB
            &[253, 16, 1], "2.jpg".as_bytes(), &[b'\n'],
            // common length contains 0xA (newline value)
            &[253, 10, 1], "xax".as_bytes(), &[b'\n'],
            // 10 common chars -> 251
            &[b'\n'], "?".as_bytes(), &[b'\n'],
        ]
            .iter()
            .fold(Vec::new(), |mut fold, bytes| {
                for b in bytes.iter() {
                    fold.push(*b);
                }
                fold
            });

        let tmp_dir = TempDir::new().unwrap();

        let file_path = tmp_dir.path().join("test_stats");
        {
            let mut tmp_file = File::create(&file_path).unwrap();
            tmp_file.write_all(&compressed).unwrap();
        }

        let mut reader = BufReader::new(File::open(&file_path).unwrap());
        assert_eq!(
            get_stats(&mut reader).unwrap(),
            Stats {
                indexed_files: 8,
                size_bytes: compressed.len() as u64,
            }
        );
    }
}
