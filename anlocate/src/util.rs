use std::io::{self, BufRead, BufReader, Read};

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
