package jp.segfault.prolog.term;

public class Thunk extends Term {
	
	@Override
	public Term strip() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public <R, A> R accept(A arg, Visitor<R, A> visitor) {
		return visitor.visit(arg, this);
	}

}
