package jp.segfault.prolog.util;

import java.util.Iterator;

/**
 * remove() オペレーションが利用不可である{@link java.util.Iterator}です。
 * @author shun
 */
public abstract class UnmodifiableIterator<E> implements Iterator<E> {

	/**
	 * {@link java.lang.UnsupportedOperationException}をスローします。
	 */
	@Override
	public final void remove() {
		throw new UnsupportedOperationException();
	}
}
