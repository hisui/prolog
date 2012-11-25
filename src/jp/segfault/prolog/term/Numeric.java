package jp.segfault.prolog.term;

/**
 * Prologの数値のです。
 * @author shun
 */
public class Numeric extends Term implements Atomic<Number> {

	private Number value;

	public static Numeric make(Number value) {
		return new Numeric(value);
	}
	
	private Numeric(Number value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Numeric && value.equals(((Numeric) o).value);
	}
	
	@Override
	public <R,A> R accept(A arg, Visitor<R,A> visitor) {
		return visitor.visit(arg, this);
	}
	
	@Override
	public Numeric numeric() {
		return this;
	}

	@Override
	public Number value() {
		return value;
	}

	@Override
	public int compareTo(Atomic<Number> o) {
		double d = value.doubleValue() - o.value().doubleValue();
		return d < 0 ? -1: d > 0 ? 1: 0;
	}

}
