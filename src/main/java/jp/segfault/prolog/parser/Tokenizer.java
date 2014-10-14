package jp.segfault.prolog.parser;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;


import static java.lang.Character.*;
import static jp.segfault.prolog.parser.Token.Kind.*;

/**
 * 入力ストリームをPrologテキストとみなし、トークン列に分解します。
 * @author shun
 */
public class Tokenizer {

	private PushbackReader reader;
	private Token token;
	
	public static final String PUNCTUATION = "#&*+-./\\:;?@^$<=>";
	public static final String PARENTHESIS = "(){}[]";
	
	Tokenizer(Reader reader) {
		this.reader = new PushbackReader(reader, 3);
	}

	public static boolean isAgglutinatable(String l, String r) {
		return isTokenBoundary(l.charAt(l.length() - 1), r.charAt(0));
	}
	
	/**
	 * 指定した二文字の間がトークンの境界になりうるかどうかを調べます。
	 */
	public static boolean isTokenBoundary(char l, char r) {
		if(isWhitespace(l) ||
		   isWhitespace(r) ||
		   PARENTHESIS.indexOf(l) != -1 ||
		   PARENTHESIS.indexOf(r) != -1) {
			return true;
		}
		return isJavaIdentifierPart(l) != isJavaIdentifierPart(r);
	}

	/**
	 * 次のトークンを返します。
	 */
	public Token next(boolean value) throws ParseException, IOException {
		return (token = getToken(value));
	}
	
	/**
	 * 前に解析したトークンを返します。
	 */
	public Token peek() {
		return token;
	}
	
	private Token getToken(boolean value) throws IOException, ParseException {
		skipWhitespaces();
		int chr = reader.read();
		if(chr == -1) {
			return null;
		}
		if(value) {
			// 括弧など
			if("([{".indexOf(chr) != -1) {
				return new Token( SPECIAL, String.valueOf((char) chr) );
			}
			// 整数値アトム
			if(chr == '-') {
				int c = reader.read();
				if(isDigit(c)) {
					return getNumber(c, "-");
				}
				ungetc(c);
			}
			else if(isDigit(chr)) {
				return getNumber(chr, "");
			}
		}
		if("}])".indexOf(chr) != -1) {
			return new Token( SPECIAL, String.valueOf((char) chr) );
		}
		Token token = getAtom(chr);
		if(token == null) {
			throw new ParseException("不正な文字: `"+ (char) chr +":0x"+ Integer.toHexString(chr) +"'.");
		}
		if(value && token.kind == ATOM_STR) {
			if((chr = reader.read()) == '(') {
				return new Token(FUNC_BGN, token.value);
			}
			ungetc(chr);
		}
		return token;
	}
	
	private Token getAtom(int chr) throws IOException, ParseException {
		String val = "";
		// 単体でアトムを構成
		if(",!|".indexOf(chr) != -1) {
			return new Token( ATOM_STR, String.valueOf((char) chr) );
		}
		// アルファベットのみで構成されるアトムか変数
		if(isJavaIdentifierStart(chr)) {
			do { val += (char) chr; } while(isJavaIdentifierPart(chr = reader.read()));
			ungetc(chr);
			return new Token(isUpperCase(val.charAt(0)) || val.charAt(0) == '_' ? VAR: ATOM_STR, val);
		}
		// 'アトム'
		if(chr == '\'') {
			while((chr = readFully()) != '\'') {
				val += (char) chr;
				if(chr == '\\') { val += readFully(); }
			}
			return new Token( ATOM_STR, Quotemeta.decode(val), true );
		}
		// アトム
		ungetc(chr);
		if(!(val = repeat(PUNCTUATION)).isEmpty()) {
			return new Token( ATOM_STR, val );
		}
		return null;
	}
	
	private Token getNumber(int chr, String prefix) throws IOException, ParseException {
		String number = prefix;
		if(chr == '0') {
			chr = reader.read();
			if(chr == 'x') {
				return new Token(ATOM_INT, number +"0x"+ repeat1("0123456789abcdefABCDEF"));
			}
			ungetc(chr);
			if(isDigit(chr)) {
				number += repeat("01234567");
				if(!isDigit(chr = reader.read())) {
					ungetc(chr);
					return new Token(ATOM_INT, "0"+ number);
				}
				number += (char) chr;
				number += repeat("0123456789");
			}
			else {
				number += "0";
			}
		}
		else {
			number += (char) chr + repeat("0123456789");
		}
		Token.Kind kind = ATOM_INT;
		chr = reader.read();
		if(chr == '.') {
			if(!isDigit(chr = reader.read())) {
				ungetc(chr);
				ungetc('.');
				return new Token(ATOM_INT, number);	
			}
			kind = ATOM_FPN;
			number += "."+ (char) chr + repeat("0123456789");
			chr = reader.read();
		}
		if(chr == 'e' || chr == 'E') {
			String sign = "";
			chr = reader.read();
			if(chr == '+' || chr == '-') {
				sign = String.valueOf((char) chr);
			}
			else {
				ungetc(chr);
			}
			kind = ATOM_FPN;
			number += "e"+ sign + repeat1("0123456789");
		}
		else {
			ungetc(chr);
		}
		return new Token(kind, number);		
	}
	
	private String repeat1(String chars) throws IOException, ParseException {
		String result = repeat(chars);
		if(result.isEmpty()) {
			throw new ParseException("文字がありません。chars=\""+ chars +"\"");
		}
		return result;
	}
	
	private String repeat(String chars) throws IOException {
		String result = "";
		for(;;) {
			int c = reader.read();
			if(chars.indexOf(c) == -1) {
				ungetc(c);
				break;
			}
			result += (char) c;
		}
		return result;
	}
	
	private char readFully() throws IOException {
		int c = reader.read();
		if(c == -1) {
			throw new EOFException();
		}
		return (char) c;
	}

	private void skipWhitespaces() throws IOException {
		for(;;) {
			int chr = reader.read();
			if(!isWhitespace(chr)) {
				if(chr == '%') {
					new BufferedReader(reader, 1).readLine();
					continue;
				}
				if(chr == '/') {
					int c = reader.read();
					if(c == '*') {
						while(readFully() != '*' ||
						      readFully() != '/');
						continue;
					}
					ungetc(c);
				}
				ungetc(chr);
				break;
			}
		}
	}
	
	private void ungetc(int c) throws IOException {
		if(c != -1) { reader.unread(c); }
	}

}
