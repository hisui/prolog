package jp.segfault.prolog.parser;

import java.util.List;

/**
 * Prologの項(term)を作成します。
 * @author shun
 */
public abstract class TermFactory<TERM> {

	/**
	 * 文字列アトムを生成します。
	 */
	public abstract TERM newAtom(String value);
	
	/**
	 * 整数値アトムを生成します。
	 */
	public abstract TERM newAtom(int value);
	
	/**
	 * 実数値アトムを生成します。
	 */
	public abstract TERM newAtom(double value);
	
	/**
	 * ひとつ以上の引数を持つ関数子を作成します。
	 */
	public abstract TERM newFunctor(String value, List<TERM> args);

	/**
	 * 変数を作成します。
	 */
	public abstract TERM newVariable(String value);

}
