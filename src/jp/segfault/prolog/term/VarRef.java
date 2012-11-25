package jp.segfault.prolog.term;

import java.util.Map;

import jp.segfault.prolog.Binding;

/**
 * 実体化された変数({@link Variable})です。
 * @author shun
 */
public class VarRef extends Variable {

	public final Var      var;
	public final Binding  binding;
	
	public VarRef(Var var, Binding binding) {
		if(binding == null) {
			throw new IllegalArgumentException("binding is null.");
		}
		this.var     = var;
		this.binding = binding;
	}

	@Override
	public int hashCode() {
		return (var.id << 8) | binding.ordinal;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof VarRef) {
			VarRef that = (VarRef) o;
			return var.id == that.var.id
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
		return binding.ordinal < olderBound ? this: var.rebind(binding, counter, done, olderBound);
	}
	
	@Override
	public Term strip() {
		VarRef ref = var.ref(binding);
		return ref.get() == null ? ref: ref.get();
	}

	@Override
	public int id() {
		return var.id;
	}

	@Override
	public String name() {
		return var.name + ":"+ binding.ordinal;
	}
	
	@Override
	public Term get() {
		return binding.getSlot(var.id);
	}

	final public void setSlot(Term term) { binding.setSlot(var.id, term); }
	final public void setTemp(Term term) { binding.setTemp(var.id, term); }

	public Term getSlot() { return binding.getSlot(var.id); }
	public Term getTemp() { return binding.getTemp(var.id); }

}
