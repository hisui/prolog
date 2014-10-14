package jp.segfault.prolog.parser;

/**
 * 文字列リテラルのエスケープシーケンスの変換処理を行います。
 * @author shun
 */
public final class Quotemeta {

	private Quotemeta() {
	}
	
	/**
	 * アンエスケープします。
	 */
	public static String decode(CharSequence value) {
		String result = "";
		int j = 0;
		for(int i = 0; i < value.length(); ++i) {
			if(value.charAt(i) == '\\') {
				if(j < i) {
					result += value.subSequence(j, i);
				}
				switch(value.charAt(++i)) {
				case 'b': result += "\b"; break;
				case 't': result += "\t"; break;
				case 'n': result += "\n"; break;
				case 'f': result += "\f"; break;
				case 'r': result += "\r"; break;
				case '0': result += "\0"; break;
				case 'x':
					String hex = "";
					hex += value.charAt(++i);
					hex += value.charAt(++i);
					result += (char) Integer.parseInt(hex, 16);
					break;
				default:
					result += value.charAt(i);
					break;
				}
				j = i + 1;
			}
		}
		if(j < value.length()) {
			result += value.subSequence(j, value.length());
		}
		return result;
	}

	/**
	 * エスケープします。
	 */
	public static String encode(CharSequence value) {
		String result = "";
		for(int i = 0; i < value.length(); ++i) {
			char c = value.charAt(i);
			switch(c) {
			case '\b': result += "\\b"; continue;
			case '\t': result += "\\t"; continue;
			case '\n': result += "\\n"; continue;
			case '\f': result += "\\f"; continue;
			case '\r': result += "\\r"; continue;
			case '\0': result += "\\0"; continue;
			case '\'': result += "\\'"; continue;
			default:
				if(Character.isISOControl(c)) {
					result += "\\x";
					for(int j = 0; j < 2; ++j) {
						result += "0123456789abcdef".charAt(c >>> j * 4 & 0x0f);
					}
					continue;
				}
				break;
			}
			result += c;
		}
		return result;
	}
	
	/**
	 * 必要であればクオートします。
	 */
	public static String quote(CharSequence value) {
		if(value.equals("")) {
			return "''";
		}
		for(int i = 0; i < value.length(); ++i) {
			char c = value.charAt(i);
			if(Character.isWhitespace(c) ||
			   Character.isISOControl(c) || "()[]{}'".indexOf(c) != -1)
			{
				return "'"+ encode(value) +"'";
			}
		}
		return value.toString();
	}

}
