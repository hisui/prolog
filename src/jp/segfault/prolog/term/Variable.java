package jp.segfault.prolog.term;

import jp.segfault.prolog.QueryException;

/**
 * 変数を表現します。
 * @author shun
 */
public abstract class Variable extends Term {

	private static int UniqueIdCounter = 0;
	
	/**
	 * ワイルドカード変数(_)です。
	 */
	public static final Variable _ = new Variable() {

		@Override
		public int id() {
			return -1;
		}

		@Override
		public String name() {
			return "_";
		}
	};
	
	/**
	 * 変数を作成します。
	 */
	public static Variable create(int id, String name) {
		return new Var(id, name);
	}
	
	/**
	 * 指定した名前を持つ変数を作成します。
	 * <p>ローカル変数インデックスは-1に設定されます。</p>
	 */
	public static Variable create(String name) {
		return name.equals("_") ? _: create(-1, name);
	}

	/**
	 * 他の変数と重複しない名前を持つ変数を作成します。
	 */
	public static Variable create() {
		return create("$"+ ++UniqueIdCounter);
	}
	
	@Override
	public <R,A> R accept(A arg, Visitor<R,A> visitor) {
		return visitor.visit(arg, this);
	}

	/**
	 * 常に<code>instantiation_error</code>をスローします。
	 */
	@Override
	public Atom atom() {
		throw QueryException.instantiation_error();
	}
	
	/**
	 * <code>instantiation_error</code>をスローします。
	 */
	@Override
	public Functor functor() {
		throw QueryException.instantiation_error();
	}

	/**
	 * ローカル変数インデックスです。
	 */
	public abstract int id();
	
	/**
	 * 変数名です。
	 */
	public abstract String name();
	
	/**
	 * 変数から値を取得します。
	 */
	public Term get() {
		throw new UnsupportedOperationException();
	}
}
