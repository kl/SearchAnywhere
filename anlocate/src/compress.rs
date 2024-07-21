use crate::util::read_line;
use std::io::{self, BufReader, Read};
use std::mem;

/// Forward-compresses all lines (line ending is newline) read from the reader.
/// Empty lines are ignored.
/// The lines from the reader must be sorted.
pub fn compress_from_reader(reader: &mut BufReader<impl Read>) -> io::Result<Vec<u8>> {
    let mut result = Vec::<u8>::new();

    let mut prev = Vec::new();
    read_line(reader, &mut prev)?;
    compress_line(&mut result, &[], prev.as_slice());

    let mut curr = Vec::new();
    loop {
        if read_line(reader, &mut curr)? == 0 {
            // we have reached EOF
            break;
        }
        // ignore empty lines
        if curr.is_empty() {
            continue;
        }
        compress_line(&mut result, prev.as_slice(), curr.as_slice());
        mem::swap(&mut prev, &mut curr);
        curr.clear();
    }

    result.pop(); // remove trailing newline
    Ok(result)
}

/// Forward-compresses the lines in `sorted_lines`.
/// `sorted_lines` must be sorted.
pub fn compress_lines(sorted_lines: &[&[u8]]) -> Vec<u8> {
    if sorted_lines.is_empty() {
        return Vec::new();
    }
    let mut result = Vec::new();

    let mut prev = &sorted_lines[0];
    compress_line(&mut result, &[], prev);

    for line in sorted_lines.iter().skip(1) {
        compress_line(&mut result, prev, line);
        prev = line;
    }

    result.pop(); // remove trailing newline
    result
}

/// Forward-compresses the uncompressed `line` given the (uncompressed) previous line `prev_line`
/// and pushes the compressed result (including a newline) into `result`.
pub fn compress_line(result: &mut Vec<u8>, prev_line: &[u8], line: &[u8]) {
    let common = common_prefix_count(prev_line, line);
    let substring = if common == 0 {
        line
    } else {
        &line[(common as usize)..]
    };
    push_line(result, common, substring);
}

fn push_line(result: &mut Vec<u8>, count: u32, line: &[u8]) {
    encode_count(result, count);
    for byte in line {
        result.push(*byte);
    }
    result.push(b'\n')
}

fn encode_count(result: &mut Vec<u8>, count: u32) {
    // the biggest count we can fit in one byte
    static MAX_COUNT_1_BYTE: u8 = u8::MAX - 4;

    if count <= MAX_COUNT_1_BYTE as u32 {
        // count fits in one byte
        result.push(count as u8);
    } else {
        // we need more than the first byte to encode count.
        let bytes: [u8; 4] = count.to_le_bytes();
        let bytes_needed = bytes
            .iter()
            .cloned()
            .enumerate()
            .rfind(|&(_, b)| b != 0)  // find MSB that isn't 0
            .unwrap()                 // all bytes are not 0 so safe to unwrap
            .0                        // index of MSB + 1
            + 1;

        // the number of bytes needed for the count are encoded as follows:
        // 252 => 1 byte, 253 => 2 bytes, 254 => 3 bytes, 255 => 4 bytes.
        result.push(MAX_COUNT_1_BYTE + bytes_needed as u8);
        for byte in bytes.iter().take(bytes_needed) {
            result.push(*byte);
        }
    }
}

fn common_prefix_count(a: &[u8], b: &[u8]) -> u32 {
    let mut count: u32 = 0;
    let mut a_bytes = a.iter();
    let mut b_bytes = b.iter();
    loop {
        match (a_bytes.next(), b_bytes.next()) {
            (Some(a_byte), Some(b_byte)) if a_byte == b_byte => {
                count = count.checked_add(1).expect("common prefix too large")
            }
            _ => break count,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs::File;
    use std::io::Write;
    use tempfile::TempDir;

    #[test]
    fn test_compress_lines() {
        let input: Vec<&[u8]> = "\
/usr/src
/usr/src/cmd/aardvark.c
/usr/src/cmd/armadillo.c
/usr/tmp/zoo
/x/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/file1.sh
/x/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/file2.jpg
/x/has/com?"
            .split('\n')
            .map(|l| l.as_bytes())
            .collect();

        let expected: Vec<u8> = vec![
            &[0], "/usr/src".as_bytes(), &[b'\n'],
            &[8], "/cmd/aardvark.c".as_bytes(), &[b'\n'],
            &[14], "rmadillo.c".as_bytes(), &[b'\n'],
            &[5], "tmp/zoo".as_bytes(), &[b'\n'],
            &[1], "x/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/file1.sh".as_bytes(), &[b'\n'],
            // 272 common chars -> 253=needs 2 byte to store, 16=LSB, 1=MSB
            &[253, 16, 1], "2.jpg".as_bytes(), &[b'\n'],
            // 10 common chars -> encode newline
            &[b'\n'], "?".as_bytes(),
        ]
            .iter()
            .fold(Vec::new(), |mut fold, bytes| {
                for b in bytes.iter() {
                    fold.push(*b);
                }
                fold
            });

        let result = compress_lines(input.as_slice());
        assert_eq!(result, expected);
    }

    #[test]
    fn test_compress_from_file() {
        let tmp_dir = TempDir::new().unwrap();
        let file_path = tmp_dir.path().join("test_compress_from_file");
        {
            let mut tmp_file = File::create(&file_path).unwrap();
            writeln!(tmp_file, "\
/usr/src
/usr/src/cmd/aardvark.c
/usr/src/cmd/armadillo.c
/usr/tmp/zoo

/x/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/file1.sh
/x/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/file2.jpg").unwrap();
        }

        let tmp_file = File::open(&file_path).unwrap();
        let mut reader = BufReader::new(tmp_file);
        let result = compress_from_reader(&mut reader).unwrap();

        let expected: Vec<u8> = vec![
            &[0], "/usr/src".as_bytes(), &[b'\n'],
            &[8], "/cmd/aardvark.c".as_bytes(), &[b'\n'],
            &[14], "rmadillo.c".as_bytes(), &[b'\n'],
            &[5], "tmp/zoo".as_bytes(), &[b'\n'],
            &[1], "x/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/has/common/prefix/that/is/longer/than/251/bytes/long/file1.sh".as_bytes(), &[b'\n'],
            // 272 common chars -> 253=needs 2 byte to store, 16=LSB, 1=MSB
            &[253, 16, 1], "2.jpg".as_bytes(),
        ]
            .iter()
            .fold(Vec::new(), |mut fold, bytes| {
                for b in bytes.iter() {
                    fold.push(*b);
                }
                fold
            });

        assert_eq!(result, expected);
    }

    #[test]
    fn test_compress_256_common() {
        // there was a bug where the following compress_line returned [251, 108, 101, 110, 10]
        let prev = "/home/kl/.cache/JetBrains/IdeaIC2022.3/index/shared_indexes/shared.index.hashes.org.jetbrains.kotlin.idea.vfilefinder.KotlinJvmModuleAnnotationsIndex/shared.index.hashes.org.jetbrains.kotlin.idea.vfilefinder.KotlinJvmModuleAnnotationsIndex_storage.storage.keystream.len";
        let curr = "/home/kl/.cache/JetBrains/IdeaIC2022.3/index/shared_indexes/shared.index.hashes.org.jetbrains.kotlin.idea.vfilefinder.KotlinJvmModuleAnnotationsIndex/shared.index.hashes.org.jetbrains.kotlin.idea.vfilefinder.KotlinJvmModuleAnnotationsIndex_storage.storage.len";
        let mut ret = vec![];
        compress_line(&mut ret, prev.as_bytes(), curr.as_bytes());

        let expected = &[253, 0, 1, 108, 101, 110, b'\n'];
        assert_eq!(ret, expected.as_slice());
    }
}
