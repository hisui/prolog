package jp.segfault.prolog.parser;

import java.io.IOException;

/**
 * Prologテキストを読み込んで、項に変換するパーサーです。
 * @author shun
 */
public interface TermParser<TERM> {

	/**
	 * ストリーム上の次の項を解析して返します。
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public TERM next() throws IOException, ParseException;

}
