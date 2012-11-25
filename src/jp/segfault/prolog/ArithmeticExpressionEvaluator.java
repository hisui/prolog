package jp.segfault.prolog;

import java.util.HashMap;

import jp.segfault.prolog.term.Atom;
import jp.segfault.prolog.term.Data;
import jp.segfault.prolog.term.Functor;
import jp.segfault.prolog.term.Numeric;
import jp.segfault.prolog.term.Term;
import jp.segfault.prolog.term.Thunk;
import jp.segfault.prolog.term.Variable;

/**
 * is/2 述語の右辺式の実行を行います。
 * @author shun
 */
public class ArithmeticExpressionEvaluator {
	
	/**
	 * {@link ArithmeticExpressionEvaluator}で実行出来る関数です。
	 * @author shun
	 */
	public static abstract class Function {

		public Integer apply(Functor functor) {
			Integer[] args = new Integer[functor.arity()];
			for(int i = 0; i < args.length; ++i) {
				args[i] = evaluate(functor.get(i));
			}
			return apply(args);
		}
		
		public Integer apply(Integer[] args) {
			throw new UnsupportedOperationException();
		}
	}
	
	private static final HashMap<Predicate,Function> functions = new HashMap<>();
	static {
		
		//
		// 四則演算
		//
		
		functions.put(new Predicate("+", 2), new Function() {
			public Integer apply(Integer[] args) { return args[0] + args[1]; }
		});
		
		functions.put(new Predicate("-", 2), new Function() {
			public Integer apply(Integer[] args) { return args[0] - args[1]; }
		});
		
		functions.put(new Predicate("*", 2), new Function() {
			public Integer apply(Integer[] args) { return args[0] * args[1]; }
		});
		
		functions.put(new Predicate("/", 2), new Function() {
			public Integer apply(Integer[] args) { return args[0] / args[1]; }
		});
		
		functions.put(new Predicate("mod", 2), new Function() {
			public Integer apply(Integer[] args) { return args[0] % args[1]; }
		});

		
		//
		// 符号
		//

		functions.put(new Predicate("+", 1), new Function() {
			public Integer apply(Integer[] args) { return  args[0]; }
		});
		
		functions.put(new Predicate("-", 1), new Function() {
			public Integer apply(Integer[] args) { return -args[0]; }
		});

	}
	
	public static Integer evaluate(Term term) {
		return term.accept(null, new Term.Visitor<Integer,Void>() {
			
			@Override
			public Integer visit(Void arg, Atom term) {
				throw QueryException.type_error(Atom.NUMERIC, term);
			}

			@Override
			public Integer visit(Void arg, Data<?> term) {
				throw QueryException.type_error(Atom.NUMERIC, term);
			}

			@Override
			public Integer visit(Void arg, Numeric term) {
				return term.value().intValue();
			}

			@Override
			public Integer visit(Void arg, Functor term) {
				Function function = functions.get(term.predicate());
				if(function == null) {
					throw QueryException.existence_error(Atom.PREDICATE, term.predicate().toFunctor());
				}
				return function.apply(term);
			}
			
			@Override
			public Integer visit(Void arg, Variable term) {
				Term val = term.get();
				if(val == null) {
					throw QueryException.instantiation_error();
				}
				return val.accept(null, this);
			}

			@Override
			public Integer visit(Void arg, Thunk term) {
				return term.strip().accept(null, this);
			}
			
		});
	}
}
