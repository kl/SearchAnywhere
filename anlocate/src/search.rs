use crate::util;
use std::cmp::PartialEq;
use std::io::{self, BufReader, Read};
use std::string::FromUtf8Error;

#[derive(Debug, PartialEq)]
pub enum MatchType {
    Include,
    Exclude,
}

#[derive(Debug)]
pub struct SearchQuery<'a> {
    query: &'a str,
    match_type: MatchType,
    ascii_only: bool,
}

impl<'a> SearchQuery<'a> {
    pub fn new(query: &'a str, match_type: MatchType) -> Self {
        SearchQuery {
            query,
            match_type,
            ascii_only: query.is_ascii(),
        }
    }
}

pub fn search(
    reader: &mut BufReader<impl Read>,
    search: &[SearchQuery],
) -> Result<Vec<String>, SearchError> {
    let mut matches = Vec::<String>::new();
    if search.is_empty() {
        return Ok(matches);
    }

    let mut buf = Vec::new();
    util::read_line(reader, &mut buf)?;
    let first = decompress_line(&[], &buf)?;

    // Prev is stored in this local or as the last element of `result` if it matched the search.
    let mut prev = if is_search_match(&first, search) {
        matches.push(first);
        None
    } else {
        Some(first)
    };

    loop {
        buf.clear();
        if util::read_db_entry_include_newline(reader, &mut buf)? == 0 {
            // we have reached EOF
            break;
        }
        // remove the end-of-line \n
        buf.pop();

        let prev_bytes = prev
            .as_ref()
            .unwrap_or_else(|| &matches[matches.len() - 1])
            .as_bytes();

        let curr = decompress_line(prev_bytes, &buf)?;

        if is_search_match(&curr, search) {
            matches.push(curr);
            prev = None;
        } else {
            prev = Some(curr);
        }
    }
    Ok(matches)
}

fn is_search_match(path: &str, search: &[SearchQuery]) -> bool {
    for query in search {
        let hit = util::caseless_contains(path, query.query, query.ascii_only);
        if query.match_type == MatchType::Include && !hit {
            return false;
        }
        if query.match_type == MatchType::Exclude && hit {
            return false;
        }
    }
    true
}

fn decompress_line(prev: &[u8], curr: &[u8]) -> Result<String, FromUtf8Error> {
    #[rustfmt::skip]
    let (data_start_index, common_len) = match curr[0] {
        // common len is 10 (ascii value of newline)
        251 => (1, b'\n' as usize),
        // common len is between 251-255
        252 => (2, curr[1] as usize),
        // common len fits in 2-4 bytes
        253 => (3, u32::from_le_bytes([curr[1], curr[2], 0, 0]) as usize),
        254 => (4, u32::from_le_bytes([curr[1], curr[2], curr[3], 0]) as usize),
        255 => (5, u32::from_le_bytes([curr[1], curr[2], curr[3], curr[4]]) as usize),
        // common len fits in the first byte
        len => (1, len as usize),
    };

    let common = &prev[0..common_len];
    let current = &curr[data_start_index..];

    let mut result = Vec::with_capacity(common.len() + current.len());
    result.extend_from_slice(common);
    result.extend_from_slice(current);

    String::from_utf8(result)
}

#[derive(Debug)]
pub enum SearchError {
    IO(io::Error),
    Encoding(FromUtf8Error),
}

impl From<io::Error> for SearchError {
    fn from(error: io::Error) -> Self {
        SearchError::IO(error)
    }
}

impl From<FromUtf8Error> for SearchError {
    fn from(error: FromUtf8Error) -> Self {
        SearchError::Encoding(error)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs::File;
    use std::io::Write;
    use tempdir::TempDir;

    #[test]
    fn test_search() {
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

        let tmp_dir = TempDir::new("test_search_on_file").unwrap();

        let file_path = tmp_dir.path().join("test_search_on_file");
        {
            let mut tmp_file = File::create(&file_path).unwrap();
            tmp_file.write_all(&compressed).unwrap();
        }

        let mut reader = BufReader::new(File::open(&file_path).unwrap());
        assert_eq!(
            search(&mut reader, &query(&["/a"])).unwrap(),
            vec!["/usr/src/cmd/aardvark.c", "/usr/src/cmd/armadillo.c",]
        );

        let mut reader = BufReader::new(File::open(&file_path).unwrap());
        assert_eq!(
            search(&mut reader, &query(&["/a", "ARK"])).unwrap(),
            vec!["/usr/src/cmd/aardvark.c"]
        );

        let mut reader = BufReader::new(File::open(&file_path).unwrap());
        assert_eq!(
            search(&mut reader, &query(&["?"])).unwrap(),
            vec!["/x/has/com?"]
        );

        let mut reader = BufReader::new(File::open(&file_path).unwrap());
        assert_eq!(
            search(&mut reader, &query(&["sdkjfhljsdhfl"])).unwrap(),
            Vec::<String>::new()
        );

        let mut reader = BufReader::new(File::open(&file_path).unwrap());
        assert_eq!(
            search(&mut reader, &query(&["longer/than/251/bytes"]))
                .unwrap()
                .len(),
            3
        );

        // test excludes
        let mut reader = BufReader::new(File::open(&file_path).unwrap());
        let q = vec![
            SearchQuery::new(".c", MatchType::Include),
            SearchQuery::new("aard", MatchType::Exclude),
        ];
        assert_eq!(
            search(&mut reader, &q).unwrap(),
            vec!["/usr/src/cmd/armadillo.c"]
        );

        // test reading when common length contains 0xA
        let mut reader = BufReader::new(File::open(&file_path).unwrap());
        assert_eq!(search(&mut reader, &query(&["xax"])).unwrap().len(), 1);

        // test reading when first bye is 0xA
        let mut reader = BufReader::new(File::open(&file_path).unwrap());
        assert_eq!(search(&mut reader, &query(&["?"])).unwrap().len(), 1);
    }

    fn query<'a>(q: &'a [&str]) -> Vec<SearchQuery<'a>> {
        q.iter()
            .map(|s| SearchQuery::new(s, MatchType::Include))
            .collect()
    }
}
