package jp.segfault.prolog.term;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jp.segfault.prolog.Binding;
import jp.segfault.prolog.Predicate;
import jp.segfault.prolog.QueryException;
import jp.segfault.prolog.State;
import jp.segfault.prolog.parser.Operator;
import jp.segfault.prolog.parser.Quotemeta;
import jp.segfault.prolog.parser.Tokenizer;

/**
 * Prologにおける項を表現します。
 * @author shun
 */
public abstract class Term {

	public static Numeric valueOf(Number o) { return Numeric.make(o); }
	public static    Atom valueOf(String o) { return    Atom.make(o); }
	
	/**
	 * {@link java.lang.Object}を{@link Term}に変換します。
	 */
	public static Term valueOf(Object o) {
		if(o instanceof Number) return valueOf((Number) o);
		if(o instanceof String) return valueOf((String) o);
		return Data.<Object>make(o);
	}
	
	/**
	 * この{@link Term}の構成が、環境が変わっても変化しないことを示します。
	 */
	public boolean isDefinite() { return true; }
	
	/**
	 * コールフレームと{@link Term}を関連付けて、実体化します。
	 */
	public Term bind(Binding binding) { return this; }

	/**
	 * コールフレームと{@link Term}を関連付けを解除します。
	 */
	public final Term unbind() {
		return rebind(null, new int[]{0}, new HashMap<Term,Term>(), 0);
	}

	/**
	 * 未束縛の変数を新しいコールフレームに関連付けます。
	 */
	public final Term rebind(Binding binding) {
		return rebind(binding, 0);
	}
	
	/**
	 * 未束縛の変数を新しいコールフレームに関連付けます。
	 */
	public final Term rebind(Binding binding, int olderBound) {
		int[] counter = new int[]{ binding.locals() };
		// TODO 効率化
		Term term = rebind(null, counter
				, new HashMap<Term,Term>(), olderBound).bind(binding);
		binding.expandLocal(counter[0]);
		return term;
	}
	
	/**
	 * {@link Term#rebind()}を実装する為に、サブクラスによってオーバーライドされます。
	 */
	protected Term rebind(Binding binding,
			int[] counter, Map<Term,Term> done, int olderBound) {
		return this;
	}
	
	/**
	 * この{@link Term}が{@link Variable}であり、尚且つ{@link Variable}が値を束縛するとき、その値を返します。
	 * <p>条件に合致しない場合、thisを返します。</p>
	 */
	public Term strip() { return this; }
	
	/**
	 * {@link Atom}にキャストします。
	 * <p>失敗した場合、<code>type_error(atom, this)</code>をスローします。</p>
	 */
	public Atom atom() {
		throw QueryException.type_error(Atom.ATOM, this);
	}

	/**
	 * {@link Functor}にキャストします。
	 * <p>失敗した場合、<code>type_error(functor, this)</code>をスローします。</p>
	 */
	public Functor functor() {
		throw QueryException.type_error(Atom.FUNCTOR, this);
	}
	
	/**
	 * {@link Numeric}にキャストします。
	 * <p>失敗した場合、<code>type_error(numeric, this)</code>をスローします。</p>
	 */
	public Numeric numeric() {
		throw QueryException.type_error(Atom.NUMERIC, this);
	}

	/**
	 * {@link Term}が{@link Atomic}である場合、{@link Atomic#value())}を型Tにキャストします。
	 * <p>失敗した場合、<code>type_error(name, this)</code>をスローします。</p>
	 */
	public <T> T cast(Class<T> type, Atom name) {
		if(this instanceof Atomic) {
			Object o = ((Atomic<?>) this).value();
			if(type.isInstance(o)) {
				return type.cast(o);
			}
		}
		throw QueryException.type_error(name, this);
	}

	/**
	 * {@link Visitor}を受け付けます。
	 * <p>{@link Term#accept(Object, Visitor)}の第一引数にnullを渡したときと同等です。</p>
	 */
	public final <R,A> R accept(Visitor<R,A> visitor) {
		return accept(null, visitor);
	}

	/**
	 * {@link Visitor}を受け付けます。
	 */
	public abstract <R,A> R accept(A arg, Visitor<R,A> visitor);

	/**
	 * {@link Term}のVisitorです。
	 * @author shun
	 */
	public interface Visitor<R,A> {
		
		public R visit(A arg, Functor  term);
		public R visit(A arg, Numeric  term);
		public R visit(A arg, Data<?>  term);
		public R visit(A arg, Atom     term);
		public R visit(A arg, Variable term);
		public R visit(A arg, Thunk    term);
		
	}

	public static abstract class VisitorAdapter<R,A> implements Visitor<R,A> {

		protected final HashSet<Term> done = new HashSet<>();

		public void visitDescendents(A arg, Functor term) {
			for(Term e: term.args()) { e.accept(arg, this); }
		}

		@Override public R visit(A arg, Data<?> term) { return null; }
		@Override public R visit(A arg, Atom    term) { return null; }
		@Override public R visit(A arg, Numeric term) { return null; }

		@Override
		public R visit(A arg, Functor term) {
			return safeVisit(arg, term, !done.add(term));
		}

		@Override
		public R visit(A arg, Variable term) {
			Term next = term.strip();
			return next instanceof Variable ? safeVisit(arg, (Variable) next): next.accept(arg, this);
		}

		@Override
		public R visit(A arg, Thunk term) {
			return null;
		}
		
		protected abstract R safeVisit(A arg, Variable term);
		protected abstract R safeVisit(A arg, Functor  term, boolean cycle);
	}

	/**
	 * {@link Term}同士を比較します。
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static int compare(Term x, Term y) {
		Visitor<Integer,Void> order = new Visitor<Integer,Void>() {
			@Override public Integer visit(Void arg, Thunk term)    { return 0; }
			@Override public Integer visit(Void arg, Variable term) { return 1; }
			@Override public Integer visit(Void arg, Numeric  term) { return 2; }
			@Override public Integer visit(Void arg, Atom     term) { return 3; }
			@Override public Integer visit(Void arg, Data<?>  term) { return 4; }
			@Override public Integer visit(Void arg, Functor  term) { return 4 + term.arity(); }
		};
		int delta = x.accept(order) - y.accept(order);
		if(delta == 0) {
			if(x instanceof Variable) {
				VarRef vx = (VarRef) x;
				VarRef vy = (VarRef) y;
				delta =
						  vx.binding.ordinal
						- vy.binding.ordinal;
				return delta != 0 ? delta: vx.id() - vy.id();
				//return ((Variable) x).name().compareTo(
				//       ((Variable) y).name());
			}
			if(x instanceof Atomic) {
				return ((Atomic) x).compareTo(((Atomic) y));
			}
			Complex $x = (Complex) x;
			Complex $y = (Complex) y;
			if((delta = $x.name().compareTo($y.name())) == 0) {
				for(int i = 0; i < $x.arity() && (delta = compare($x.get(i), $y.get(i))) == 0; ++i);
			}
		}
		return delta;
	}

	/**
	 * この{@link Term}を複製します。
	 */
	@Override
	public Term clone() {
		return accept(null, new Clone());
	}
	
	/**
	 * この{@link Term}をPrologの項のテキスト表現に変換します。
	 */
	@Override
	public String toString() {
		return toString(this, null);
	}

	/**
	 * この{@link Term}をPrologの項のテキスト表現に変換します。
	 * <p>{@link Term#toString()}よりも読みやすい形に出力できます。</p>
	 */
	public String toString(State state) {
		return toString(this, state);
	}

	private static String toString(Term term, final State state) {
		return new ToString(state).visit(term, false);
	}

	/**
	 * {@link Term}をcloneします。
	 * @author shun
	 */
	public static class Clone implements Visitor<Term,Void> {
		
		@Override public Term visit(Void arg, Atom    term) { return term; }
		@Override public Term visit(Void arg, Numeric term) { return term; }
		@Override public Term visit(Void arg, Data<?> term) { return term; }

		@Override
		public Term visit(Void arg, Functor term) {
			Term[] args = new Term[term.arity()];
			for(int i = 0; i < args.length; ++i) {
				args[i] = term.get(i).accept(null, this);
			}
			return Functor.create(term.name(), args);
		}
		
		@Override
		public Term visit(Void arg, Variable term) {
			return term;
		}

		@Override
		public Term visit(Void arg, Thunk term) {
			return term;
		}
		
	}
	
	/**
	 * {@link Term}を文字列に変換します。
	 * @author shun
	 */
	public static class ToString extends VisitorAdapter<String,Boolean> {

		private final State state;
		
		public ToString(State state) {
			this.state = state;
		}

		Operator getOperator(Term term) {
			if(state != null) {
				try {
					Functor functor = (Functor) term;
					for(Operator op: state.getOperatorTable().getOperator(functor.name())) {
						if(functor.arity() == op.kind.arity) return op;
					}
				} catch(ClassCastException e) {}
			}
			return null;
		}
		
		String visit(Term term, boolean inList) {
			return term.accept(inList, this);
		}
		
		@Override
		public String visit(Boolean _, Atom term) {
			return Quotemeta.quote(term.value().toString());
		}

		@Override
		public String visit(Boolean arg, Numeric term) {
			return term.value().toString();
		}

		@Override
		public String visit(Boolean arg, Data<?> term) {
			return "{data}";
		}

		@Override
		public String visit(Boolean _, Thunk term) {
			return "{thunk}";
		}

		@Override
		protected String safeVisit(Boolean inList, Functor term, boolean cycle) {
			if(cycle) {
				return Atom.CYCLE.value();
			}
			List<Term> list = term.unlist(Predicate.of("./2"), true);
			if(list.size() < 2) {
				Operator op = getOperator(term);
				if(op != null) {
					String[] parts = new String[op.kind.arity];
					for(int i = 0; i < parts.length; ++i) {
						int a = 1;
						int b = 0;
						Operator tmp = getOperator(term.get(i));
						if(tmp != null) {
							Operator.Kind kind = op.kind.round();
							if(kind == Operator.Kind.xfx) {
								kind = i == 0 ? Operator.Kind.fx: Operator.Kind.xf;
							}
							switch(kind) {
							case fx: a = op.lprio; b = tmp.rprio; break;
							case xf: a = op.rprio; b = tmp.lprio; break;
							default:
								throw new IllegalStateException("kind="+ kind);
							}
						}
						parts[i] = a < b
								? "("+ visit(term.get(i), false) +")"
								: visit(term.get(i), inList && !op.notation.equals(","));
					}
					String result = null;
					switch(op.kind.round()) {
					case xfx: result = concat(parts[0], op.notation, parts[1]); break;
					case  fx: result = concat(          op.notation, parts[0]); break;
					case  xf: result = concat(parts[0], op.notation          ); break;
					default:;
					}
					if(result != null) {
						return inList && op.notation.equals(",") ? "("+ result +")": result;
					}
				}
				if(term.arity() == 1 && term.name().equals(Atom.NULL)) {
					return "{"+ visit(term.get(0), false) +"}";
				}
				return Quotemeta.quote(term.name()) +"("+ join(Arrays.asList(term.args())) +")";
			}
			String result = "["+ join(list.subList(0, list.size() - 1));
			Term tail = list.get(list.size() - 1);
			if(!tail.equals(Atom.NIL)) {
				result += "|"+ visit(tail, false);
			}
			return result + "]";
		}
		
		public String join(Term[] a) {
			return join(Arrays.asList(a));
		}

		public String join(List<Term> list) {
			String result = "";
			if(!list.isEmpty()) {
				result = visit(list.get(0), true);
				for(int i = 1; i < list.size(); ++i) {
					result += ","+ visit(list.get(i), true);
				}
			}
			return result;
		}

		public String concat(String ...parts) {
			String result = parts[0];
			for(int i = 1; i < parts.length; ++i) {
				if(!Tokenizer.isAgglutinatable(parts[i-1], parts[i])) {
					result += " ";
				}
				result += parts[i];
			}
			return result;
		}

		@Override
		protected String safeVisit(Boolean _, Variable term) {
			return term.name();
		}
	}

	/**
	 * 束縛されていない{@link Variable}をリストします。
	 */
	public List<Variable> extract() {
		final ArrayList<Variable> vars = new ArrayList<>();
		accept(new VisitorAdapter<Void,Void>() {

			@Override
			protected Void safeVisit(Void arg, Variable term) {
				vars.add(term);
				return null;
			}

			@Override
			protected Void safeVisit(Void arg, Functor term, boolean cycle) {
				if(!cycle) {
					visitDescendents(arg, term);
				}
				return null;
			}
		});
		return vars;
	}
}
