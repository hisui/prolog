package jp.segfault.prolog.code;

/**
 * バックトラッキングが発生するポイントを設定します。
 * @author shun
 */
public class Choice extends Code {

	public final Code[] codes;
	
	public Choice(Code[] codes) {
		this.codes = codes;
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visit(this);
	}

}
