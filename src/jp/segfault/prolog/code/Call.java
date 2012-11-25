package jp.segfault.prolog.code;

import jp.segfault.prolog.procedure.Procedure;
import jp.segfault.prolog.term.Term;

/**
 * {@link Procedure}をゴールとして実行します。
 * @author shun
 */
public class Call extends Goal {

	public final Procedure procedure;
	
	public Call(Code next, Term[] args, Procedure procedure) {
		super(next, args);
		this.procedure = procedure;
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visit(this);
	}

}
