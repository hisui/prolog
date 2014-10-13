package jp.segfault.prolog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jp.segfault.prolog.code.Call;
import jp.segfault.prolog.code.Choice;
import jp.segfault.prolog.code.Code;
import jp.segfault.prolog.code.Cut;
import jp.segfault.prolog.code.Goal;
import jp.segfault.prolog.code.Noop;
import jp.segfault.prolog.code.Result;
import jp.segfault.prolog.code.Select;
import jp.segfault.prolog.procedure.Procedure;
import jp.segfault.prolog.term.Atom;
import jp.segfault.prolog.term.Term;
import jp.segfault.prolog.util.UnmodifiableIterator;
import static jp.segfault.prolog.code.Result.*;

/**
 * 一つのクエリを評価・実行するための実行環境です。
 * @author shun
 */
public class Query {

	/**
	 * このクエリが属するStateです。
	 */
	public final State state;

	/**
	 * 現在のコールフレームです。
	 */
	public Binding callee;
	
	// ChoicePoinのリンクリスト。ここに入っているのが一番上になる。
	private              ChoicePoint choicePoint = GUARD;
	private static final ChoicePoint GUARD       = new ChoicePoint(null, null, -1, false, null);

	private int ancestry;
	private Code next;

	/**
	 * 新たにクエリを作成します。
	 */
	public Query(State state, Procedure entry) {
		this.state = state;
		next = call(entry, new Term[]{}, null, 0);
	}
	
	/**
	 * ChoicePointを設定します。
	 */
	public void setChoicePoint(Iterator<Code> iterator) {
		setChoicePoint(iterator, ancestry, false);
	}

	public void setChoicePoint(Iterator<Code> iterator, boolean local) {
		setChoicePoint(iterator, ancestry, local);
	}
	
	/**
	 * 最新のChoicePointを取得します。
	 */
	public ChoicePoint getChoicePoint() {
		return choicePoint;
	}
	
	/**
	 * バックトラッキングすることが出来るかどうかを返します。
	 */
	public boolean canBacktrack() {
		for (ChoicePoint e = choicePoint; e.parent != null; e = e.parent) {
			if (e.iterator.hasNext()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * クエリを実行し、その結果を返します。
	 */
	public List<Term> ask() {
		final boolean TRACE = "yes".equals(state.getFlag("trace_call_frame"));
	outer:
		for (;;) {
			try {
			while (next == Fail) { // バックトラッキング
				if (choicePoint.parent == null) {
					return null; // 失敗
				}
				if (TRACE) trace("fail.");
				// System.err.println("バックトラッキングが発生！:"+ choicePoints.peek().revision);
				choicePoint.undo();
				if (choicePoint.iterator.hasNext()) { // 実行時の節削除(abolish)に対応
					callee = choicePoint.callee;
					  next = choicePoint.iterator.next();
					if (choicePoint.local || choicePoint.iterator.hasNext()) {
						continue;
					}
					// next()内でもChoicePoint#add(VarRef)が発生するので、
					// ここで!hasNext()になっていたとしても、ただpopするだけでは、正しくundo()されない為にバグる
					choicePoint.parent.drain(choicePoint);
				}
				choicePoint = choicePoint.parent;
			}
			// 次のコードを実行する
			// System.err.println("Query.next: next: "+ next);
			next = next.accept(new Code.Visitor<Code>() {
				
				Code callTable(final Table table, final Goal goal) {
					final List<Procedure> rows;
					if (table == null) {
						throw QueryException.existence_error(Atom.PREDICATE, Atom.NIL);
					}
					if ((rows = table.rows()).isEmpty()) {
						throw QueryException.existence_error(
								Atom.PREDICATE, table.predicate().toFunctor());
					}
					if (rows.size() > 1) {
						final int revision = Query.this.ancestry;
						setChoicePoint(new UnmodifiableIterator<Code>()
						{	
							int i = 1;
							
							@Override
							public boolean hasNext() { return i < rows.size(); }

							@Override
							public Code next() {
								return call(rows.get(i++), goal, revision);
							}
						}, revision + 1, false);
					}
					return call(rows.get(0), goal);
				}

				@Override
				public Code visit(Result code) {
					return code;
				}
				
				@Override
				public Code visit(Select code) {
					if (TRACE) {
						StringBuilder builder = new StringBuilder(
								code.table.predicate().id + "(");
						if (code.args.length > 0) {
							for (Term arg: code.args) {
								builder.append(arg.bind(callee).unbind().toString(state));
								builder.append(", ");
							}
							builder.setLength(builder.length() - 2);
						}
						builder.append(")");
						trace("call: "+ builder);
					}
					return callTable(code.table, code);
				}

				@Override
				public Code visit(final Choice code) {
					setChoicePoint(new UnmodifiableIterator<Code>()
					{	
						int i = 1;

						@Override
						public boolean hasNext() { return i < code.codes.length; }

						@Override
						public Code next() { return code.codes[i++]; }
					}, code.local);
					return code.codes[0];
				}
				
				@Override
				public Code visit(Cut code) {
					ChoicePoint top = choicePoint;
					while (choicePoint.ancestry >= callee.ancestry) {
						if (code.local && choicePoint.local) {
							choicePoint = choicePoint.parent;
							break;
						}
						choicePoint = choicePoint.parent;
					}
					if (choicePoint.parent != null) {
						choicePoint.drain(top);
					}
					return code.next;
				}

				@Override
				public Code visit(Call code) {
					return call(code.procedure, code);
				}

				@Override
				public Code visit(Noop code) {
					return code.next;
				}
				
			});
			if (next == True) {
				// エントリーポイントまで戻ってきた
				if (callee.caller == null) {
					ArrayList<Term> values = new ArrayList<Term>(callee.locals());
					for (int i = 0; i < callee.locals(); ++i) {
						values.add(callee.getSlot(i) == null ? null: callee.getSlot(i).unbind());
					}
					next = Fail;
					return values;
				}
				  next = callee.next;
				callee = callee.caller;
			}
			// 例外の捕捉
			} catch(QueryException e) {
				do {
					if ((next = callee.procedure.catches(this, e)) != null) {
						continue outer;
					}
				} while ((callee = callee.caller) != null);
				throw e;
			}
		}
	}

	private Code call(Procedure procedure, Goal goal) {
		return call(procedure, goal.args, goal.next, ancestry);
	}

	private Code call(Procedure procedure, Goal goal, int revision) {
		return call(procedure, goal.args, goal.next, revision);
	}

	private Code call(Procedure procedure, Term[] args, Code next, int revision) {
		final Binding caller = callee;
		// 末尾呼び出しの場合は現在のコールフレームをスキップ
		if (next == True && callee.caller != null) {
			  next = callee.next;
			callee = callee.caller;
		}
		return procedure.call(this, (ancestry = revision + 1), caller, next, args);
	}
	
	private void setChoicePoint(Iterator<Code> iterator, int snapshot, boolean local) {
		choicePoint = new ChoicePoint(choicePoint, callee, snapshot, local, iterator);
	}

	private void trace(Object msg) {
		for (int i = 0; i < callee.depth; ++i) {
			System.err.print("  ");
		}
		System.err.println(msg);
	}

}
