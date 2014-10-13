package jp.segfault.prolog.code;

/**
 * バックトラッキングが発生するポイントを設定します。
 * @author shun
 */
public class Choice extends Code {

	public final Code[] codes;
	public final boolean local;
	
	public Choice(Code[] codes, boolean local) {
		this.codes = codes;
		this.local = local;
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visit(this);
	}

}
