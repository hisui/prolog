package jp.segfault.prolog.parser;


/**
 * パース時のエラーです。
 * @author shun
 */
@SuppressWarnings("serial")
public class ParseException extends Exception {
	
	final private int row;
	final private int col;
	public int getCol() { return col; }
	public int getRow() { return row; }

	public ParseException(String message, Throwable e, int row, int col) {
		super(message, e);
		this.row = row;
		this.col = col;
	}
	
	public ParseException(String message) {
		this(message, null, 0, 0);
	}
}
