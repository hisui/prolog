package jp.segfault.prolog.procedure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import jp.segfault.prolog.ArithmeticExpressionEvaluator;
import jp.segfault.prolog.Binding;
import jp.segfault.prolog.Predicate;
import jp.segfault.prolog.Query;
import jp.segfault.prolog.QueryException;
import jp.segfault.prolog.State;
import jp.segfault.prolog.Unifier;
import jp.segfault.prolog.code.Call;
import jp.segfault.prolog.code.Code;
import jp.segfault.prolog.code.Noop;
import jp.segfault.prolog.code.Result;
import jp.segfault.prolog.parser.Operator;
import jp.segfault.prolog.parser.ParseException;
import jp.segfault.prolog.parser.TermParser;
import jp.segfault.prolog.term.Atom;
import jp.segfault.prolog.term.Atomic;
import jp.segfault.prolog.term.CompRef;
import jp.segfault.prolog.term.Complex;
import jp.segfault.prolog.term.Functor;
import jp.segfault.prolog.term.Numeric;
import jp.segfault.prolog.term.Term;
import jp.segfault.prolog.term.Thunk;
import jp.segfault.prolog.term.Variable;
import jp.segfault.prolog.util.SingleValueIterator;
import jp.segfault.prolog.util.UnmodifiableIterator;


import static jp.segfault.prolog.code.Result.*;

/**
 * 組み込み述語の定義を行います。
 * @author shun
 */
public abstract class Foreign extends Procedure {	

	/**
	 * このクラス内で、この注釈が付加された静的フィールド値は、
	 * ランタイムのスタートアップ時に組み込み述語として自動的に登録されます。
	 * @author shun
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Declaration {
		String value();
	}

	@Override
	public Code call(Query query, Term ...args) {
		for(int i = 0; i < args.length; ++i) {
			args[i] = args[i].strip();
		}
		return call0(query, args);
	}

	protected Code call0(Query query, Term ...args) {
		throw new UnsupportedOperationException();
	}
	
	//--------------------------
	// 組み込み述語の定義
	//--------------------------

	/* データベース操作 */
	
	@Declaration("assertz/1")
	public static final Foreign ASSERTZ = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			Coding coding = Coding.create(query.state, args[0].unbind());
			if(coding.head == null) {
				throw QueryException.type_error(Atom.FUNCTOR, Atom.NIL);
			}
			query.state.insert(coding.head.predicate(), coding);
			return True;
		}
	};

	@Declaration("asserta/1")
	public static final Foreign ASSERTA = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			Coding coding = Coding.create(query.state, args[0].unbind());
			if(coding.head == null) {
				throw QueryException.type_error(Atom.FUNCTOR, Atom.NIL);
			}
			query.state.insert(coding.head.predicate(), coding, 0);
			return True;
		}
	};
	
	@Declaration("abolish/1")
	public static final Foreign ABOLISH = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			query.state.delete(Predicate.of(args[0]));
			return True;
		}
	};

	@Declaration("retract/1")
	public static final Foreign RETRACT$1 = new Foreign() {

		@Override
		public Code call(Query query, Term... args) {
			Functor head = args[0].functor();
			if(Predicate.CLAUSE.equals(head.predicate())) {
				head = head.get(0).functor();
			}
			Iterator<Procedure> i = query.state.select(head.predicate()).iterator();
			while(i.hasNext()) {
				Procedure procedure = i.next();
				if(procedure instanceof Coding) {
					query.callee.expandLocal(procedure.locals());
					Unifier unifier = new Unifier(query);
					if(unifier.exec(args[0], ((Coding) procedure).clause.bind(query.callee))) {
						unifier.rollback();
						i.remove();
					}
				}
			}
			return True;
		}
	};

	@Declaration("current_predicate/1")
	public static final Foreign CURRENT_PREDICATE = new Foreign() {
		
		@Override
		protected Code call0(final Query query, final Term... args) {
			final Iterator<Predicate> i = query.state.predicates().iterator();
			query.setChoicePoint(new UnmodifiableIterator<Code> () {

				@Override
				public boolean hasNext() {
					return i.hasNext();
				}

				@Override
				public Code next() {
					return UNIFY.call(query, args[0], i.next().toFunctor());
				}
			});
			return Fail;
		}
	};

	@Declaration("listing/1")
	public static final Foreign LISTING = new Foreign() {
		
		@Override
		protected Code call0(Query query, Term... args) {
			for(Procedure procedure : query.state.select(Predicate.of(args[0]))) {
				String value = "foreign";
				if(procedure instanceof Coding) {
					Coding coding = (Coding) procedure;
					if(coding.clause != null) {
						value = coding.clause.toString(query.state);
					}
				}
				query.state.out.println("\t"+ value +".");
			}
			return True;
		}
	};
	
	@Declaration("clause/2")
	public static final Foreign CLAUSE = new Foreign() {
		
		@Override
		protected Code call0(final Query query, final Term... args) {
			query.setChoicePoint(new UnmodifiableIterator<Code> () {
				
				Iterator<Predicate> i = query.state.predicates().iterator();
				Iterator<Procedure> j = null;

				@Override
				public boolean hasNext() {
					while(j == null || !j.hasNext()) {
						if(!i.hasNext()) {
							return false;
						}
						j = query.state.getTable(i.next()).rows().iterator();
					}
					return true;
				}

				@Override
				public Code next() {
					if(!hasNext()) {
						throw new NoSuchElementException();
					}
					Procedure procedure = j.next();
					if(!(procedure instanceof Coding)) {
						return Fail;
					}
					final Coding coding = (Coding) procedure;
					
					// TODO: expandLocalによるメモリリーク解消
					Functor tmp = Functor.create("[]",
							coding.head,
							coding.clause != null ?
							coding.clause: Atom.NIL).rebind(query.callee).functor();
					return
						UNIFY.call(query, args[0], tmp.get(0)) == Fail ? Fail:
						UNIFY.call(query, args[1], tmp.get(1));
				}
			});
			return Fail;
		}
	};
	
	/* 項の変換 */
	
	@Declaration("=/2")
	public static final Foreign UNIFY = new Foreign() {

		@Override
		public Code call(Query query,
				int ancestry, Binding caller, Code next, Term[] args)
		{
			Unifier unifier = new Unifier(query);
			if(unifier.exec(args[0], caller, args[1], caller)) {
				unifier.commit(query);
				return next;
			}
			return Fail;
		}
		
		@Override
		public Code call(Query query, Term... args) {
			return Result.valueOf(Unifier.unify(query, args[0], args[1]));
		}
	};
	
	@Declaration("\\=/2")
	public static final Foreign NOT_UNIFY = new Foreign() {

		@Override
		public Code call(Query query, Binding caller, Term[] args) {
			return Result.valueOf(!new Unifier(query).exec(args[0], caller, args[1], caller));
		}
	};
	
	@Declaration("arg/3")
	public static final Foreign ARG = new Foreign() {

		@Override
		protected Code call0(final Query query, final Term... args) {
			final Functor functor = args[1].functor();
			if(!(args[0] instanceof Variable)) {
				int arity = args[0].numeric().value().intValue();
				if(arity <= 0 || arity > functor.arity()) {
					return Fail;
				}
				return UNIFY.call(query, functor.get(arity - 1), args[2]);
			}
			query.setChoicePoint(new UnmodifiableIterator<Code> ()
			{
				int i = 0;

				@Override
				public boolean hasNext() { return i < functor.arity(); }

				@Override
				public Code next() {
					if(!hasNext()) {
						throw new NoSuchElementException();
					}
					int arity = i++;
					return
						UNIFY.call(query, args[0], Term.valueOf(arity)) == Fail ? Fail:
						UNIFY.call(query, args[2],  functor.get(arity));
				}
			});
			return Fail;
		}
	};

	@Declaration("=../2")
	public static final Foreign EQUAL_AND_TWO_PERIODS = new Foreign() {
		
		@Override
		public Code call0(Query query, Term... args) {
			Term x = null;
			Term y = null;
			if(args[0] instanceof Functor) {
				Functor functor = (Functor) args[0];
				ArrayDeque<Term> terms = new ArrayDeque<Term>(1 + functor.arity());
				terms.add(Term.valueOf(functor.name()));
				terms.addAll(Arrays.asList(functor.args()));
				x = args[1];
				y = Functor.list(terms.descendingIterator());
			}
			else {
				List<Term> terms = args[1].functor().unlist();
				if(terms.isEmpty()) {
					return Fail;
				}
				x = args[0];
				y = Functor.create(terms.get(0).atom().value(), terms.subList(1, terms.size()));
			}
			Unifier unifier = new Unifier(query);
			if(unifier.exec(x, y)) {
				unifier.commit(query);
				return True;
			}
			return Fail;
		}
	};
	
	@Declaration("functor/3")
	public static final Foreign FUNCTOR = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			Unifier unifier = new Unifier(query);
			if(args[0] instanceof Functor) {
				Functor functor = (Functor) args[0];
				if(!(unifier.exec(args[1], Term.valueOf(functor.name()))
				  && unifier.exec(args[2], Term.valueOf(functor.arity())))) {
					return Fail;
				}
			}
			else {
				Term[] terms = new Term[ args[2].numeric().value().intValue() ];
				for(int i = 0; i < terms.length; ++i) {
					terms[i] = Variable.create(i + query.callee.locals(), "$"+ i);
				}
				query.callee.expandLocal(terms.length);
				if(!unifier.exec(args[0], Functor.create(args[1].atom().value(), terms).bind(query.callee))) {
					return Fail;
				}
			}
			unifier.commit(query);
			return True;
		}
	};

	@Declaration("copy_term/2")
	public static final Foreign COPY_TERM = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			return UNIFY.call(query, args[1], args[0].rebind(query.callee));
		}
	};

	@Declaration("term_variables/2")
	public static final Foreign TERM_VARIABLES = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			return UNIFY.call(query, args[1], Functor.list(args[0].extract()));
		}
	};
	
	/* 処理の制御 */

	@Declaration("true/0")
	public static final Foreign TRUE = new Foreign() {
		@Override public Code call(Query query, Term... args) { return True; }
	};
	
	@Declaration("fail/0")
	public static final Foreign FAIL = new Foreign() {
		@Override public Code call(Query query, Term ...args) { return Fail; }
	};
	
	@Declaration("call/1")
	public static final Foreign CALL = new Foreign() {

		@Override
		public Code call(Query query,
				int ancestry, Binding caller, Code next, Term[] args)
		{
			Term term = args[0].bind(caller).strip();
			if(term instanceof Variable) {
				throw QueryException.instantiation_error();
			}
			Binding base = null;
			if(term instanceof CompRef) {
				// term が属するコールフレームを引き継いだ環境をつくる
				base = ((CompRef) term).binding;
				term = ((CompRef) term).comp;
			}
			// コードのキャッシュを参照
			Code code = query.state.getPoolingCode(term);
			if(code == null) {
				query.state.putPoolingCode(term,
						(code = new Coding.Compile(query.state, term).getCode()));
			}
			query.callee = new Binding(ancestry, this, next, query.callee, base);
			return code;
		}
	};

	@Declaration("!/0") public static final Foreign  CUT = new Foreign() {};
	@Declaration(",/2") public static final Foreign CONJ = new Foreign() {};
	@Declaration(";/2") public static final Foreign DISJ = new Foreign() {};

	@Declaration("catch/3")
	public static final Foreign CATCH = new Foreign() {

		private static final int CATCHER_SLOT = 0;
		private static final int HANDLER_SLOT = 1;

		@Override
		protected Code call0(Query query, Term ...args) {
			Binding callee = query.callee;
			callee.setSlot(CATCHER_SLOT, args[1]);
			callee.setSlot(HANDLER_SLOT, args[2]);
			// Noop を挟むのは、tail-callの最適化を回避するため
			return new Call(new Noop(True), new Term[]{ args[0] }, CALL);
		}
		
		@Override
		public int locals() {
			return 2;
		}

		@Override
		public Code catches(Query query, QueryException e) {
			Binding callee = query.callee;
			if(Unifier.unify(query, e.term, callee.getSlot(CATCHER_SLOT))) {
				// tail-callになるので、ハンドラ内で発生した例外は捕捉できない
				return new Call(True, new Term[]{ callee.getSlot(HANDLER_SLOT) }, CALL);
			}
			return null;
		}
	};
	
	@Declaration("throw/1")
	public static final Foreign THROW = new Foreign() {
		
		@Override
		protected Code call0(Query query, Term ...args) {
			throw new QueryException(args[0]);
		}
	};

	@Declaration("findall/3")
	public static final Foreign FINDALL = new Foreign() {
		
		@Override
		public Code call0(final Query query, final Term... args) {
			
			// _ は特別扱い(高速化)
			if(args[2] == Variable._) {
				query.setChoicePoint(new SingleValueIterator<Code>() {
					@Override protected Code value() { return True; }
				});
				return new Call(Fail, new Term[]{args[1]}, CALL);
			}
			
			final ArrayDeque<Term> results = new ArrayDeque<Term>();
			query.setChoicePoint(new SingleValueIterator<Code>() {
				
				@Override
				protected Code value() {
					return UNIFY.call(query, args[2], Functor.list(results).bind(query.callee));
				}
			});
			
			return new Call(new Call(Fail, new Term[]{}, new Procedure()
			{
				@Override
				public Code call(Query query,
						int ancestry, Binding caller, Code next, Term[] _)
				{
					results.push(args[0].rebind(caller));
					return next;
				}
			}), new Term[]{args[1]}, CALL);
		}
		
	};

	/* 算術式 */
	
	@Declaration("is/2")
	public static final Foreign IS = new Foreign() {
		
		@Override
		protected Code call0(Query query, Term... args) {
			return UNIFY.call(query, args[0], Term.valueOf(eval(args[1])));
		}
	};

	@Declaration("</2")
	public static final Foreign LESS_THAN = new Foreign() {
		
		@Override
		protected Code call0(Query query, Term... args) {
			return Result.valueOf(eval(args[0]) < eval(args[1]));
		}
	};
	
	@Declaration("=:=/2")
	public static final Foreign EQUAL = new Foreign() {
		
		@Override
		protected Code call0(Query query, Term... args) {
			return Result.valueOf(eval(args[0]) == eval(args[1]));
		}
	};
	
	/* アトム操作 */

	@Declaration("atom_length/2")
	public static final Foreign ATOM_LENGTH = new Foreign() {
		
		@Override
		protected Code call0(Query query, Term... args) {
			return UNIFY.call(query, Term.valueOf(args[0].atom().value().length()), args[1]);
		}
	};

	// TODO 効率化
	@Declaration("atom_concat/3")
	public static final Foreign ATOM_CONCAT = new Foreign() {
		
		@Override
		protected Code call0(final Query query, final Term... args) {
			if(args[2] instanceof Variable) {
				return UNIFY.call(query, args[2],
						Term.valueOf(args[0].atom().value() + args[1].atom().value()));
			}
			query.setChoicePoint(new UnmodifiableIterator<Code>() {
				
				String value = args[2].atom().value();
				int i = 0;

				@Override
				public boolean hasNext() {
					return i <= value.length();
				}

				@Override
				public Code next() {
					Unifier unifier = new Unifier(query);
					int j = i++;
					if(unifier.exec(args[0], Term.valueOf(value.substring(0, j)))
					&& unifier.exec(args[1], Term.valueOf(value.substring(j   ))))
					{
						unifier.commit(query);
						return True;
					}
					return Fail;
				}
			});
			return Fail;
		}
	};
	
	// TODO 効率化
	@Declaration("sub_atom/5")
	public static final Foreign SUB_ATOM = new Foreign() {
		
		@Override
		protected Code call0(final Query query, final Term... args) {
			query.setChoicePoint(new UnmodifiableIterator<Code>() {
				
				String value = args[0].atom().value();
				int i = 0;
				int j = 0;

				@Override
				public boolean hasNext() {
					return j <= value.length() ? true: (j = ++i) <= value.length();
				}

				@Override
				public Code next() {
					if(!hasNext()) {
						throw new NoSuchElementException();
					}
					int k = j++;
					Unifier unifier = new Unifier(query);
					if(unifier.exec(args[4], Term.valueOf(value.substring(i, k)))
					&& unifier.exec(args[1], Term.valueOf(i))
					&& unifier.exec(args[2], Term.valueOf(k - i))
					&& unifier.exec(args[3], Term.valueOf(value.length() - k)))
					{
						unifier.commit(query);
						return True;
					}
					return Fail;
				}
			});
			return Fail;
		}
	};
	
	@Declaration("atom_chars/2")
	public static final Foreign ATOM_CHARS = new Foreign() {
		
		@Override
		protected Code call0(final Query query, Term... args) {
			if(args[0] instanceof Variable) {
				String value = "";
				for(Term c: args[1].functor().unlist()) {
					value += c.atom().value();
				}
				return UNIFY.call(query, args[0], Term.valueOf(value));
			}
			final String value = args[0].atom().value();
			return UNIFY.call(query, args[1], Functor.list(new UnmodifiableIterator<Term>() {

				int i = value.length() - 1;
						
				@Override
				public boolean hasNext() {
					return i >= 0;
				}

				@Override
				public Term next() {
					int j = i--;
					return Term.valueOf(value.substring(j, j + 1));
				}
			}));
		}
	};
	
	/* 型のチェック */

	@Declaration("var/1")
	public static final Foreign VAR = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			return Result.valueOf(args[0] instanceof Variable);
		}
	};
	
	@Declaration("atom/1")
	public static final Foreign ATOM = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			return Result.valueOf(args[0] instanceof Atom);
		}
	};
	
	@Declaration("integer/1")
	public static final Foreign INTEGER = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			return Result.valueOf(args[0] instanceof Numeric
					&& ((Numeric) args[0]).value() instanceof Integer);
		}
	};

	@Declaration("float/1")
	public static final Foreign FLOAT = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			return Result.valueOf(args[0] instanceof Numeric
					&& ((Numeric) args[0]).value() instanceof Double);
		}
	};
	@Declaration("number/1")
	public static final Foreign NUMBER = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			return Result.valueOf(args[0] instanceof Numeric);
		}
	};
	
	@Declaration("atomic/1")
	public static final Foreign ATOMIC = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			return Result.valueOf(args[0] instanceof Atomic);
		}
	};

	@Declaration("compound/1")
	public static final Foreign COMPOUND = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			return Result.valueOf(args[0] instanceof Complex);
		}
	};
	
	/* ストリーム */

	@Declaration("current_input/1")
	public static final Foreign CURRENT_INPUT = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			return UNIFY.call(query, args[0], Term.valueOf(query.state.getInput()));
		}
	};

	@Declaration("current_output/1")
	public static final Foreign CURRENT_OUTPUT = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			return UNIFY.call(query, args[0], Term.valueOf(query.state.getOutput()));
		}
	};

	@Declaration("open/3")
	public static final Foreign OPEN = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			try {
				String filename = args[0].atom().value();
				return UNIFY.call(query, args[2]
						, args[1].atom().value().equals("write")
								? Term.valueOf(new PrintStream(new FileOutputStream(filename)))
								: Term.valueOf(new FileInputStream(filename))
								);
			} catch(IOException e) {
				throw QueryException.io_error(Term.valueOf("open"), args[0]);
			}
		}
	};

	@Declaration("close/1")
	public static final Foreign CLOSE = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			Object stream = args[0].atom().value();
			try {
				if(stream instanceof PrintStream) {
					((PrintStream) stream).close();
					return True;
				}
				if(stream instanceof InputStream) {
					((InputStream) stream).close();
					return True;
				}
				throw QueryException.type_error(Atom.STREAM, Term.valueOf("close/1"));
			} catch(IOException e) {
				throw QueryException.io_error(Term.valueOf("close"), args[0]);
			}
		}
	};

	@Declaration("get_char/2")
	public static final Foreign GET_CHAR = new Foreign() {
		
		@Override
		public Code call0(Query query, Term... args) {
			InputStream in = args[0].cast(InputStream.class, Atom.STREAM);
			try {
				return UNIFY.call(query, args[1], Term.valueOf(Character.valueOf((char) in.read()).toString()));
			} catch (IOException e) {
				throw QueryException.io_error(Term.valueOf("read"), args[0]);
			}
		}
	};
	
	@Declaration("read_term/3")
	public static final Foreign READ_TERM = new Foreign() {
		
		@Override
		public Code call0(Query query, Term... args) {
			try {
				TermParser<Term> parser = query.state.newParser(
						new InputStreamReader(args[0].cast(InputStream.class, Atom.STREAM)));
				// TODO
				return UNIFY.call(query, args[1], parser.next().rebind(query.callee));
			}
			catch(IOException e) {
				throw QueryException.io_error(Term.valueOf("read"), args[0]);
			}
			catch(ParseException e) {
				throw QueryException.syntax_error(args[0]);
			}
		}
	};
	
	@Declaration("put_char/2")
	public static final Foreign PUT_CHAR = new Foreign() {
		
		@Override
		public Code call0(Query query, Term... args) {
			PrintStream out = args[0].cast(PrintStream.class, Atom.STREAM);
			if(args[1] instanceof Numeric) {
				out.append((char) args[0].numeric().value().intValue());
				return True;
			}
			if(args[1] instanceof Atom) {
				String value = args[1].atom().value();
				if(value.length() == 1) {
					out.append(value.charAt(0));
					return True;
				}
			}
			throw QueryException.type_error(Atom.CHARACTER, args[1]);
		}
	};
	
	@Declaration("write_term/3")
	public static final Foreign WRITE_TERM = new Foreign() {
		
		@Override
		public Code call0(Query query, Term... args) {
			PrintStream out = args[0].cast(PrintStream.class, Atom.STREAM);
			out.print(args[1].unbind().toString(query.state));
			out.flush();
			return True;
		}
	};
	
	@Declaration("display/2")
	public static final Foreign DISPLAY = new Foreign() {
		
		@Override
		public Code call0(Query query, Term... args) {
			args[0].cast(PrintStream.class, Atom.STREAM).println(args[1].unbind().toString());
			return True;
		}
	};
	
	/* システム */

	@Declaration("halt/1")
	public static final Foreign HALT = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			System.exit(args[0].numeric().value().intValue());
			throw new IllegalStateException();
		}
	};
	
	@Declaration("set_prolog_flag/2")
	public static final Foreign SET_PROLOG_FLAG = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			query.state.setFlag(args[0].atom().value(),
			                    args[1].atom().value());
			return True;
		}
	};

	@Declaration("current_prolog_flag/2")
	public static final Foreign CURRENT_PROLOG_FLAG = new Foreign() {

		@Override
		protected Code call0(final Query query, final Term... args) {
			query.setChoicePoint(new UnmodifiableIterator<Code> () {
				
				Iterator<String> i = query.state.flagKeys().iterator();

				@Override
				public boolean hasNext() {
					return i.hasNext();
				}

				@Override
				public Code next() {
					Unifier unifier = new Unifier(query);
					String key = i.next();
					if(unifier.exec(args[0], Term.valueOf(key))
					&& unifier.exec(args[1], Term.valueOf(query.state.getFlag(key))))
					{
						unifier.commit(query);
						return True;
					}
					return Fail;
				}
			});
			return Fail;
		}
	};

	@Declaration("op/3")
	public static final Foreign OP = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			Operator.Kind kind = null;
			try {
				kind = Operator.Kind.valueOf(args[1].atom().value());
			} catch(IllegalArgumentException e) {
				throw QueryException.domain_error(Atom.OPERATOR_SPECIFIER, args[1]);
			}
			int priority = args[0].numeric().value().intValue();
			if(!(0 <= priority && priority <= 1200)) {
				throw QueryException.domain_error(Atom.OPERATOR_PRIORITY, args[0]);
			}
			query.state.getOperatorTable().setOperator(priority, kind, args[2].atom().value());
			return True;
		}
	};

	@Declaration("current_op/3")
	public static final Foreign CURRENT_OP = new Foreign() {

		@Override
		protected Code call0(final Query query, final Term... args) {
			final Iterator<String> i = query.state.getOperatorTable().keys().iterator();
			query.setChoicePoint(new UnmodifiableIterator<Code> ()
			{
				Iterator<Operator> i2;

				@Override
				public boolean hasNext() {
					while(i2 == null || !i2.hasNext()) {
						if(!i.hasNext()) {
							return false;
						}
						i2 = query.state.getOperatorTable().getOperator(i.next()).iterator();
					}
					return true;
				}

				@Override
				public Code next() {
					if(!hasNext()) {
						throw new NoSuchElementException();
					}
					Unifier unifier = new Unifier(query);
					Operator op = i2.next();
					if(unifier.exec(args[0], Term.valueOf(op.priority))
					&& unifier.exec(args[1], Term.valueOf(op.kind.toString()))
					&& unifier.exec(args[2], Term.valueOf(op.notation)))
					{
						unifier.commit(query);
						return True;
					}
					return Fail;
				}
			});
			return Fail;
		}
	};
	
	@Declaration("consult/1")
	public static final Foreign CONSULT = new Foreign() {

		@Override
		protected Code call0(Query query, Term... args) {
			try {
				query.state.parse(new File(args[0].atom().value()));
			} catch (Exception e) {
				throw QueryException.syntax_error(Term.valueOf(e.getMessage()));
			}
			return True;
		}
	};

	/* 拡張 */

	@Declaration("term_compare/3")
	public static final Foreign TERM_COMPARE = new Foreign() {
		
		@Override
		protected Code call0(Query query, Term... args) {
			return UNIFY.call(query, Term.valueOf(Term.compare(args[0], args[1])), args[2]);
		}
	};

	@Declaration("time/1")
	public static final Foreign TIME = new Foreign() {
		
		@Override
		protected Code call0(Query query, Term... args) {
			return UNIFY.call(query, args[0], Term.valueOf((int) System.currentTimeMillis()));
		}
	};

	@Declaration("random/1")
	public static final Foreign RANDOM = new Foreign() {

		final Random generator = new Random();
		
		@Override
		protected Code call0(Query query, Term... args) {
			return UNIFY.call(query, args[0], Term.valueOf(generator.nextInt()));
		}
	};

	@Declaration("jvm_load_foreign/2")
	public static final Foreign JVM_LOAD_FOREIGN = new Foreign() {
		
		@Override
		protected Code call0(Query query, Term... args) {
			
			ArrayList<URL> classpath = new ArrayList<URL>();
			for(Term term: args[1].functor().unlist()) {
				try {
					classpath.add(new URL(term.atom().value()));
				} catch (MalformedURLException e) {
				}
			}
			try {
				query.state.loadForeign(URLClassLoader.newInstance(
						classpath.toArray(new URL[]{}),
						Thread.currentThread().getContextClassLoader())
					.loadClass(args[0].atom().value()));				
			} catch (ClassNotFoundException e) {
				throw QueryException.existence_error(
						Term.valueOf("class"),
						Term.valueOf("jvm_load_foreign"));
			}
			return True;
		}
	};
	
	// randoms(L) :- thunk(L, [H|T], (random(H), randoms(T))).
	@Declaration("thunk/3")
	public static final Foreign THUNK = new Foreign() {
		
		@Override
		protected Code call0(Query query, Term... args) {

			final Variable bridge = Variable.create();
			final    State  state = query.state;
			final   Coding coding = Coding.create(state,
					Functor.create("?-",
					Functor.create( ",",
					Functor.create( "=", bridge,
							args[1].unbind()),
							args[2].unbind())));

			return UNIFY.call(query, args[0], new Thunk() {
				
				@Override
				public Term strip() {
					List<Term> result = new Query(state, coding).ask();
					if(result == null) {
						throw QueryException.instantiation_error();
					}
					return result.get(coding.vars().get(bridge.name()).id());
				}
			});
		}
	};

	/* パラレル */
	
	@Declaration("sleep/1")
	public static final Foreign SLEEP = new Foreign() {
		
		@Override
		protected Code call0(Query query, Term... args) {
			try {
				Thread.sleep(args[0].numeric().value().intValue());
			} catch (InterruptedException e) {
			}
			return True;
		}
	};

	// TODO
	@Declaration("isolate/2")
	public static final Foreign ISOLATE = new Foreign() {
		
		@Override
		protected Code call0(Query query, Term... args) {
			return True;
		}
	};

	//--------------------------
	// ユーティリティ
	//--------------------------
	
	public static Integer eval(Term expr) {
		return ArithmeticExpressionEvaluator.evaluate(expr);
	}
	
	public static void abort() {
		System.exit(-1);
	}
}
