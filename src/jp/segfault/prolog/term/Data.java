package jp.segfault.prolog.term;

public class Data<V> extends Term implements Atomic<V> {

	private V value;

	public static <V> Data<V> make(V value) {
		return new Data<>(value);
	}
	
	private Data(V value) {
		this.value = value;
	}

	@Override
	public <R,A> R accept(A arg, Visitor<R,A> visitor) {
		return visitor.visit(arg, this);
	}

	@Override
	public V value() {
		return value;
	}

	@Override
	public int compareTo(Atomic<V> o) {
		return value.hashCode() - o.value().hashCode();
	}

}
