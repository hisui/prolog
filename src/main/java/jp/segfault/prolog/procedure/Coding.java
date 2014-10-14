package jp.segfault.prolog.procedure;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.segfault.prolog.Binding;
import jp.segfault.prolog.Predicate;
import jp.segfault.prolog.Query;
import jp.segfault.prolog.State;
import jp.segfault.prolog.Unifier;
import jp.segfault.prolog.code.Choice;
import jp.segfault.prolog.code.Code;
import jp.segfault.prolog.code.Cut;
import jp.segfault.prolog.code.Result;
import jp.segfault.prolog.code.Select;
import jp.segfault.prolog.term.Atom;
import jp.segfault.prolog.term.Complex;
import jp.segfault.prolog.term.Data;
import jp.segfault.prolog.term.Functor;
import jp.segfault.prolog.term.Numeric;
import jp.segfault.prolog.term.Term;
import jp.segfault.prolog.term.Thunk;
import jp.segfault.prolog.term.Variable;

import static jp.segfault.prolog.Predicate.*;

/**
 * ユーザ定義のプロシージャです。
 * @author shun
 */
public class Coding extends Procedure {

	public final Term    clause;
	public final Functor head;
	public final Code    body;
	
	private final Map<String,Variable> vars;

	public Coding(Functor head, Code body) {
		this(null, head, body);
	}
	
	public Coding(Term clause, Functor head, Code body) {
		this.clause = clause;
		VariableIndexer indexer = new VariableIndexer();
		this.head = head == null ? null :(Functor) head.accept(null, indexer);
		this.body = body.accept(indexer);
		this.vars = indexer.getMap(); // 変数表
	}
	
	public static Coding create(State state, Term clause) {
		Functor functor = (Functor) clause;
		if (CLAUSE.equals(functor.predicate()) && functor.get(0) instanceof Functor) {
			return new Coding(clause, (Functor) functor.get(0),
					new Compile(state, functor.get(1)).getCode());
		}
		if (Arrays.asList(DIRECTIVE, QUERY).contains(functor.predicate())) {
			return new Coding(clause, null, new Compile(state, functor.get(0)).getCode());
		}
		return new Coding(clause, functor, Result.True);
	}

	@Override
	public Code call(Query query, Binding caller, Term[] args) {
		if (head != null) {
			// System.err.println("call: "+ head +", depth="+ (caller.depth + 1));
			Unifier unifier = new Unifier(query);
			for (int i = 0; i < head.arity(); ++i) {
				if (!unifier.exec(args[i], caller, head.get(i), query.callee)) {
					return Result.Fail;
				}
			}
			unifier.commit(query);
		}
		return body;
	}

	@Override
	public int locals() {
		return vars.size();
	}
	
	/**
	 * 変数名と変数の対応表です。
	 */
	public Map<String,Variable> vars() {
		return vars;
	}

	/**
	 * 指定したローカル変数インデックスに対応する変数の名前を返します。
	 */
	public String getOriginalName(int i) {
		for (Map.Entry<String,Variable> e : vars.entrySet()) {
			if (e.getValue().id() == i) return e.getKey();
		}
		return null;
	}
	
	/**
	 * Term をコードツリーに変換します。
	 * @author shun
	 */
	public static class Compile {

		private final State state;
		private final Code code;
		private final ArrayList<Predicate> dependencyList = new ArrayList<>();

		public Compile(State state, Term term) {
			this(state, term, Result.True);
		}
		
		public Compile(State state, Term term, Code next) {
			this.state = state;
			code = disj(term, next);
		}
		
		public Code getCode() {
			return code;
		}
		
		public List<Predicate> getDependencyList() {
			return Collections.unmodifiableList(dependencyList);
		}

		private Code conj(Term term, Code next) {
			if (term instanceof Functor) {
				Functor functor = (Functor) term;
				//if (name.atom().value() instanceof Procedure) {
				//	return new Call(next, functor.args(), name.<Procedure>atom().value());
				//}
				Predicate predicate = functor.predicate();
				if (predicate.equals(CONJUNCTION)) {
					return disj(functor.get(0), disj(functor.get(1), next));
				}
				if (predicate.equals(CUT)) {
					return new Cut(next);
				}
				// A -> B ==> A, !, B
				if (predicate.equals(IF_THEN)) {
					return disj(functor.get(0), new Cut(disj(functor.get(1), next)));
				}
				dependencyList.add(predicate);
				return new Select(next, functor.args(), state.getTable(predicate, true));
			}
			if (term instanceof Variable) {
				return new Select(next, new Term[]{term}, state.getTable(CALL));
			}
			throw new IllegalArgumentException("term: "+ term);
		}

		private Code disj(Term term, Code next) {
			ArrayDeque<Term> worklist = new ArrayDeque<>();
			ArrayDeque<Code>    codes = new ArrayDeque<>();
			worklist.add(term);
			while (!worklist.isEmpty()) {
				Term t = worklist.pop();
				if (t instanceof Complex) {
					Complex complex = (Complex) t;
					if (DISJUNCTION.equals(complex.predicate())) {
						Term A = complex.get(0);
						Term B = complex.get(1);
						Term C;
						// A -> C; B
						if (A instanceof Complex
								&& IF_THEN.equals((complex = (Complex) A).predicate()))
						{
							Code cut = new Cut(next, true);
							A = complex.get(0);
							C = complex.get(1);
							codes.addLast(new Choice(new Code[] {
								new Choice(new Code[] {
									disj(A, new Cut(disj(C, cut), true))
								}, true), disj(B, cut)
							}, true));
						}
						else {
							worklist.push(B);
							worklist.push(A);
						}
						continue;
					}
				}
				codes.addLast(conj(t, next));
			}
			return codes.size() > 1 ? new Choice(codes.toArray(new Code[]{}), false) : codes.getFirst();
		}
	}

	/**
	 * ローカル変数インデックスを再割り当てします。
	 * @author shun
	 */
	public static class VariableIndexer
			extends Code.Clone implements Term.Visitor<Term,Void> {

		private LinkedHashMap<String,Variable> vars = new LinkedHashMap<>();
		private boolean rename = false;

		public Map<String,Variable> getMap() {
			return Collections.unmodifiableMap(vars);
		}

		@Override public Term visit(Void _, Data<?> term) { return term; }
		@Override public Term visit(Void _, Atom    term) { return term; }
		@Override public Term visit(Void _, Numeric term) { return term; }

		@Override
		public Term visit(Void _, Functor term) {
			if (term.isDefinite()) {
				return term;
			}
			ArrayList<Term> args = new ArrayList<Term>(term.arity());
			for (Term arg : term.args()) {
				args.add(arg.accept(null, this));
			}
			return Functor.create(term.name(), args);
		}

		@Override
		public Term visit(Void _, Variable term) {
			if (term.isDefinite()) {
				return term;
			}
			Variable var = vars.get(term.name());
			if (var == null) {
				int id = vars.size();
				vars.put(term.name(),
						(var = Variable.create(id, rename ? toBase26String(id): term.name())));
			}
			return var;
		}

		@Override
		public Term visit(Void _, Thunk term) {
			return term.strip().accept(null, this);
		}
		
		private String toBase26String(int i) {
			String name = "";
			do {
				name += (char) (i % ('Z'-'A' +1) + 'A');
			} while ((i /= ('Z'-'A' +1)) != 0);
			return name;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Term visit(Term term) {
			return term.accept(null, this);
		}
	}

}
