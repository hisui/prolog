package jp.segfault.prolog.code;

import jp.segfault.prolog.Predicate;
import jp.segfault.prolog.Table;
import jp.segfault.prolog.term.Term;

/**
 * {@link Predicate}が指し示すデータベース上の{@link jp.segfault.prolog.procedure.Procedure}をゴールとして実行します。
 * @author shun
 */
public class Select extends Goal {
	
	public final Table table;
	
	public Select(Code next, Term[] args, Table table) {
		super(next, args);
		this.table = table;
	}

	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visit(this);
	}
}
