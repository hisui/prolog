package jp.segfault.prolog.parser;

/**
 * Prologテキストのトークンです。
 * @author shun
 */
public class Token {

	public final Kind    kind;
	public final String  value;
	public final boolean quote;
	
	Token(Kind kind, String value, boolean quote) {
		this.kind  = kind;
		this.value = value;
		this.quote = quote;
	}

	Token(Kind kind, String value) {
		this(kind, value, false);
	}

	@Override
	public String toString() {
		return "Token("+ kind +", \""+ value +"\")";
	}

	/**
	 * トークンの種類です。
	 * @author shun
	 */
	public enum Kind {
		FUNC_BGN,
		ATOM_STR,
		ATOM_INT,
		ATOM_FPN,
		VAR,
		SPECIAL,
	}
}
