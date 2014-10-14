package jp.segfault.prolog.code;

/**
 * cut/0 を実行します。
 * @author shun
 */
public class Cut extends Ordinal {
	
	public final boolean local;
	
	public Cut(Code next) {
		this(next, false);
	}
	
	public Cut(Code next, boolean local) {
		super(next);
		this.local = local;
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visit(this);
	}
}
