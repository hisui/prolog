package jp.segfault.prolog.code;

/**
 * cut/0 を実行します。
 * @author shun
 */
public class Cut extends Ordinal {
	
	public final int count;
	
	public Cut(Code next) {
		this(next, 0);
	}
	
	public Cut(Code next, int count) {
		super(next);
		this.count = count;
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visit(this);
	}
}
