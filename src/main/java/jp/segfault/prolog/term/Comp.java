package jp.segfault.prolog.term;

import java.util.List;
import java.util.Map;

import jp.segfault.prolog.Binding;

/**
 * 複合項を表します。
 * @author shun
 */
public class Comp extends Complex {

	private final String name;
	private final Term[] args;

	static Comp make(String name, List<Term> args) {
		boolean forall = true;
		for (Term arg : args) {
			if(!arg.isDefinite()) {
				forall = false;
				break;
			}
		}
		return forall
			? new Instance(name, args.toArray(new Term[]{}))
			: new Template(name, args.toArray(new Term[]{}));
	}

	private Comp(String name, Term[] args) {
		assert args.length > 0;
		this.name = name;
		this.args = args;
	}

	@Override
	public Term[] args() {
		return args; // 本当はコピーしたほうが良い
	}
	
	@Override
	public String name() {
		return name;
	}

	@Override
	public int arity() {
		return args.length;
	}

	@Override
	public Term get(int i) {
		return args[i];
	}

	private static class Instance extends Comp {
		Instance(String name, Term[] args) { super(name, args); }
	}

	private static class Template extends Comp
			implements jp.segfault.prolog.term.Template {

		Template(String name, Term[] args) {
			super(name, args);
		}

		@Override
		public boolean isDefinite() {
			return false;
		}
		
		@Override
		public Functor bind(Binding binding) {
			if (binding == null) { // kore daijoubu kana?
				return this;
			}
			return new CompRef(this, binding);
		}
		
		@Override
		protected Term rebind(Binding binding, int[] counter, Map<Term,Term> done, int olderBound) {
			Term key = binding == null ? this: new CompRef(this, binding);
			Term val = done.get(key);
			if (val == null) {
				
				// 無限再帰を防止
				done.put(key, Atom.CYCLE);
				
				// 引数も全てrebindする
				Term[] args = new Term[super.args.length];
				for (int i = 0; i < args.length; ++i) {
					args[i] = super.args[i].rebind(binding, counter, done, olderBound);
				}
				done.put(key, (val = Functor.create(super.name, args)));
			}
			return val;
		}
	}
	
}
