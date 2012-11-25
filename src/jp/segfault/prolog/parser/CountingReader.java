package jp.segfault.prolog.parser;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * 読み込まれた行数と、行頭からの文字数を記録する{@link java.io.Reader}です。
 * @author shun
 */
public class CountingReader extends FilterReader {

	private char last;
	private int row;
	private int col;
	public int getRow() { return row; }
	public int getCol() { return col; }
	
	public CountingReader(Reader reader) {
		super(reader);
	}

	@Override
	public int read() throws IOException {
		int c = super.read();
		if(c != -1) {
			count((char) c);
		}
		return c;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		len = super.read(cbuf, off, len);
		while(off < len) {
			count(cbuf[off++]);
		}
		return len;
	}
	
	private void count(char c) {
		char l = last;
		last = c;
		if(c != '\r') {
			if(c != '\n') {
				++col;
				return;
			}
			if(l == '\r') {
				return;
			}
		}
		++row;
		col = 0;
	}
}
