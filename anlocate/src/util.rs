use std::io::{self, BufRead, BufReader, Read};
use unicase::UniCase;

pub fn read_line(reader: &mut BufReader<impl Read>, buf: &mut Vec<u8>) -> io::Result<usize> {
    let bytes_read = reader.read_until(b'\n', buf)?;
    if let Some(b'\n') = buf.last() {
        buf.pop();
    }
    Ok(bytes_read)
}

pub fn read_line_include_newline(
    reader: &mut BufReader<impl Read>,
    buf: &mut Vec<u8>,
) -> io::Result<usize> {
    reader.read_until(b'\n', buf)
}

/// We read up to and including a \n. This \n could be the end-of-line \n,
/// or it may be a 0xA byte in the common length number. If the latter is true we need to
/// keep reading until we get to the end-of-line \n.
pub fn read_db_entry_include_newline(
    reader: &mut BufReader<impl Read>,
    buf: &mut Vec<u8>,
) -> io::Result<usize> {
    let mut read = read_line_include_newline(reader, buf)?;
    if read == 0 {
        // we have reached EOF
        return Ok(0);
    }
    while !last_char_is_line_sep_newline(buf) {
        read += read_line_include_newline(reader, buf)?;
    }
    Ok(read)
}

fn last_char_is_line_sep_newline(buf: &[u8]) -> bool {
    assert!(!buf.is_empty(), "buf cannot be empty");
    assert_eq!(
        buf[buf.len() - 1],
        b'\n',
        "last char in buf was not a newline"
    );

    if buf.len() == 1 {
        return false;
    }

    match buf[0] {
        253 => buf.len() > 3,
        254 => buf.len() > 4,
        255 => buf.len() > 5,
        _ => true,
    }
}

/// Returns true if a contains b using Unicode-aware case-insensitive compare
/// note: does not do normalization
pub fn caseless_contains(a: &str, b: &str, b_is_ascii: bool) -> bool {
    let a_is_ascii = a.is_ascii();
    let b = if b_is_ascii {
        UniCase::ascii(b)
    } else {
        UniCase::unicode(b)
    };

    for (i, _) in a.char_indices() {
        let haystack = &a[i..];
        if haystack.len() < b.len() {
            break;
        }
        let haystack = if haystack.is_char_boundary(b.len()) {
            &haystack[..b.len()]
        } else {
            continue;
        };

        let haystack_case = if a_is_ascii {
            UniCase::ascii(haystack)
        } else {
            UniCase::new(haystack)
        };
        if haystack_case == b {
            return true;
        }
    }
    false
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_caseless_contains() {
        let s = "this IS a string";
        let s2 = "y̆es it is";
        assert!(!caseless_contains(s, s2, false));

        let s = "YES we can";
        let s2 = "yes We Can";
        assert!(caseless_contains(s, s2, true));

        let s = "this is a Big Haystack";
        let s2 = "BIG HAYSTACK";
        assert!(caseless_contains(s, s2, true));

        let s = "/usr/src/cmd/aardvark.c";
        let s2 = "/a";
        assert!(caseless_contains(s, s2, true));

        let s = "";
        let s2 = "";
        assert!(!caseless_contains(s, s2, true));

        let s = "aBc漢字xYz";
        let s2 = "XYZ";
        assert!(caseless_contains(s, s2, true));

        let s = "aBc漢字xYz2";
        let s2 = "XYZ2";
        assert!(caseless_contains(s, s2, true));

        let s = "svenskaÅÖÄjapanskaあいうえお";
        let s2 = "å";
        assert!(caseless_contains(s, s2, false));
    }
}
