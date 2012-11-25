package jp.segfault.prolog.util;

import java.util.NoSuchElementException;

public abstract class SingleValueIterator<E> extends UnmodifiableIterator<E> {

	boolean hasNext = true;

	@Override
	public final boolean hasNext() {
		return hasNext;
	}

	@Override
	public final E next() {
		if(!hasNext) {
			throw new NoSuchElementException();
		}
		hasNext = false;
		return value();
	}
	
	protected abstract E value();

}
