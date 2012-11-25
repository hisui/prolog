package jp.segfault.prolog.code;

/**
 * 何も実行しない{@link Code}です。
 * @author shun
 */
public class Noop extends Ordinal {
	
	public Noop(Code next) {
		super(next);
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visit(this);
	}
}
