package jp.segfault.prolog.procedure;

import jp.segfault.prolog.Binding;
import jp.segfault.prolog.Query;
import jp.segfault.prolog.QueryException;
import jp.segfault.prolog.code.Code;
import jp.segfault.prolog.term.Term;

/**
 * プロシージャです。
 * @author shun
 */
public abstract class Procedure {

	/**
	 * コールフレームを作成して、プロシージャを実行します。
	 */
	public Code call(Query query,
			int ancestry, Binding caller, Code next, Term[] args)
	{
		query.callee = new Binding(ancestry, this, next, query.callee, null);
		return call(query, caller, args);
	}

	/**
	 * プロシージャを実行します。
	 * @param query 実行環境
	 * @param args　  未バインドの引数
	 * @return 次に実行する命令
	 */
	public Code call(Query query, Binding caller, Term[] args0) {
		Term[] args = new Term[args0.length];
		for(int i = 0; i < args.length; ++i) {
			args[i] = args0[i].bind(caller); // 引数を現在のコールフレームで実体化
		}
		return call(query, args);
	}
	
	/**
	 * プロシージャを実行します。
	 */
	public Code call(Query query, Term ...args) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * プロシージャが処理の中で利用するローカル変数の初期数です。
	 */
	public int locals() {
		return 0;
	}
	
	/**
	 * 例外が発生したとき、このプロシージャがそれをハンドルするなら、ハンドラのコードを返します。
	 */
	public Code catches(Query query, QueryException e) {
		return null;
	}

	/**
	 * listing/1 の結果を生成します。
	 */
	public String listing() {
		return null;
	}

}
