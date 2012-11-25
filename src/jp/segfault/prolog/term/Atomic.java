package jp.segfault.prolog.term;

public interface Atomic<V> extends Comparable<Atomic<V>> {

	public V value();

}
