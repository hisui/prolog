package jp.segfault.prolog.term;

import java.util.Map;

import jp.segfault.prolog.Binding;

/**
 * 実体化された関数子を表現します。
 * @author shun
 */
public class CompRef extends Complex {

	public final Comp    comp;
	public final Binding binding;
	
	public CompRef(Comp comp, Binding binding) {
		if(binding == null) {
			throw new IllegalArgumentException("binding is null.");
		}
		this.comp    = comp;
		this.binding = binding;
	}

	@Override
	public int hashCode() {
		return (comp.hashCode() << 8) | binding.ordinal;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof CompRef) {
			CompRef that = (CompRef) o;
			return comp.equals(that.comp)
					&& binding.ordinal == that.binding.ordinal;
		}
		return false;
	}
	
	@Override
	public boolean isDefinite() {
		return false;
	}
	
	@Override
	protected Term rebind(Binding _, int[] counter, Map<Term,Term> done, int olderBound) {
		return binding.ordinal < olderBound
				? this: comp.rebind(binding, counter, done, olderBound);
	}
	
	@Override
	public String name() {
		return comp.name();
	}
	
	@Override
	public Term[] args() {
		Term[] args = new Term[comp.arity()];
		for(int i = 0; i < args.length; ++i) {
			args[i] = comp.get(i).bind(binding);
		}
		return args;
	}
	
	public int arity() {
		return comp.arity();
	}
	
	public Term get(int i) {
		return comp.get(i).bind(binding);
	}
}
