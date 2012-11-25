package jp.segfault.prolog.code;

import jp.segfault.prolog.term.Term;

/**
 * 全てのゴールを表す{@link Code}のスーパークラスです。
 * @author shun
 */
public abstract class Goal extends Ordinal {

	public final Term[] args;

	public Goal(Code next, Term[] args) {
		super(next);
		this.args = args;
	}
}
