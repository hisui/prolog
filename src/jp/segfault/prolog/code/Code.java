package jp.segfault.prolog.code;

import java.util.HashMap;

import jp.segfault.prolog.State;
import jp.segfault.prolog.term.Term;

/**
 * 実行木のノードです。
 * @author shun
 */
public abstract class Code implements Cloneable {
	
	public abstract <T> T accept(Visitor<T> visitor);
	
	public interface Visitor<T> {
		
		public T visit(Result code);
		public T visit(Choice code);
		public T visit(Select code);
		public T visit(Cut    code);
		public T visit(Call   code);
		public T visit(Noop   code);
		
	}
	
	@Override
	public Code clone() {
		return this.accept(new Clone());
	}
	
	@Override
	public String toString() {
		return toString(null);
	}
	
	public String toString(State state) {
		return Code.toString(this, state);
	}

	public static String toString(Code code, final State state) {
		return code.accept(new ToString(state));
	}

	/**
	 * {@link Code}を複製します。
	 * @author shun
	 */
	public static class Clone implements Visitor<Code> {
		
		final HashMap<Code,Code> done = new HashMap<Code,Code>();
		
		protected Term[] visit(Term[] src) {
			Term[] dest = new Term[src.length];
			for(int i = 0; i < src.length; ++i) {
				dest[i] = visit(src[i]);
			}
			return dest;
		}
		
		protected <T extends Term> T visit(T term) {
			return term;
		}
		
		protected Code visit(Code code0) {
			Code code = done.get(code0);
			if(code == null) {
				done.put(code0, ( code = code0.accept(this) ));
			}
			return code;
		}

		@Override
		public Code visit(Result code) {
			return code;
		}

		@Override
		public Code visit(Choice code) {
			Code[] codes = new Code[code.codes.length];
			for(int i = 0; i < codes.length; ++i) {
				codes[i] = visit(code.codes[i]);
			}
			return new Choice(codes);
		}

		@Override
		public Code visit(Select code) {
			return new Select(visit(code.next), visit(code.args), code.table);
		}

		@Override
		public Code visit(Cut code) {
			return new Cut(visit(code.next), code.count);
		}

		@Override
		public Code visit(Call code) {
			return new Call(visit(code.next), visit(code.args), code.procedure);
		}

		@Override
		public Code visit(Noop code) {
			return new Noop(visit(code.next));
		}
	}
	
	/**
	 * {@link Code}を文字列に変換します。
	 * @author shun
	 */
	public static class ToString implements Visitor<String> {
			
		private State state;

		public ToString(State state) {
			this.state = state;
		}
		
		protected String visit(Goal code) {
			return "("+ new Term.ToString(state).join(code.args) +")"+ visit((Ordinal) code);
		}
		
		protected String visit(Ordinal code) {
			return ": "+ code.next.accept(this);
		}

		@Override
		public String visit(Result code) {
			return code.toString();
		}
		
		@Override
		public String visit(Choice code) {
			String result = "{";
			if(code.codes.length > 0) {
				for(int i = 0;; ) {
					result += code.codes[i].accept(this);
					if(++i >= code.codes.length) {
						break;
					}
					result += "; ";
				}
			}
			return result +"}";
		}
		
		@Override
		public String visit(Select code) {
			return code.table.predicate().id + visit((Goal) code);
		}
		
		@Override
		public String visit(Cut code) {
			return "!"+ visit((Ordinal) code);
		}

		@Override
		public String visit(Call code) {
			return code.procedure + visit((Goal) code);
		}

		@Override
		public String visit(Noop code) {
			return code.next.accept(this);
		}
	}

}
