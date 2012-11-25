package jp.segfault.prolog.term;

import java.util.Map;

import jp.segfault.prolog.Binding;

/**
 * 実体化されていない変数({@link Variable})です。
 * @author shun
 */
public class Var extends Variable {
	
	public final int    id;
	public final String name;
	
	public Var(int id, String name) {
		this.id   = id;
		this.name = name;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof Var && ((Var) o).name.equals(name);
	}
	
	@Override
	public boolean isDefinite() { return false; }
	
	@Override
	public VarRef bind(Binding binding) {
		return new VarRef(this, binding);
	}
	
	@Override
	protected Term rebind(Binding binding, int[] counter, Map<Term,Term> done, int olderBound) {
		String name = this.name;
		Term    key = this;
		if(binding != null) {
			VarRef var = ref(binding);
			Term   val = var.get();
			if(val != null) {
				return val.rebind(binding, counter, done, olderBound);
			}
			name = var.var.name;
			 key = var;
			if(binding.ordinal != 1) {
				name += "#"+ binding.ordinal;
			}
		}
		Term val = done.get(key);
		if(val == null) {
			done.put(key, (val = new Var(counter[0]++, name)));
		}
		return val;
	}
	
	@Override
	public int id() { return id; }
	
	@Override
	public String name() {
		return name;
	}

	public VarRef ref(Binding binding) {
		final Term term = binding.getSlot(id);
		if(!(term instanceof VarRef)) {
			return new VarRef(this, binding);
		}
		VarRef var = (VarRef) term;
		for(;;) {
			Term value = var.binding.getSlot(var.var.id);
			if(!(value instanceof VarRef)) {
				if(var != term) {
					binding.setSlot(id, var);
				}
				return var;
			}
			var = (VarRef) value;
		}
	}
}
