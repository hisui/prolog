package jp.segfault.prolog;

import jp.segfault.prolog.term.CompRef;
import jp.segfault.prolog.term.Complex;
import jp.segfault.prolog.term.Term;
import jp.segfault.prolog.term.Thunk;
import jp.segfault.prolog.term.Template;
import jp.segfault.prolog.term.Var;
import jp.segfault.prolog.term.VarRef;
import jp.segfault.prolog.term.Variable;

/**
 * 単一化を実行します。
 * @author shun
 */
public class Unifier {

	private VarRef[] vars = new VarRef[]{};
	private int      size = 0;
	private final boolean TRACE;
	
	public Unifier(Query query) {
		this.TRACE = query != null
				&& "yes".equals(query.state.getFlag("trace_unification"));
	}

	/**
	 * 単一化を実行し、成功した場合は環境に反映します。
	 */
	public static boolean unify(Query query, Term x, Term y) {
		Unifier unifier = new Unifier(query);
		if(unifier.exec(x, y)) {
			unifier.commit(query);
			return true;
		}
		return false;
	}

	/**
	 * 単一化を実行します。
	 * @return 成功すればtrue
	 */
	public boolean exec(Term x, Term y) {
		return exec(x, null, y, null);
	}

	/**
	 * 単一化を実行します。
	 * @return 成功すればtrue
	 */
	public boolean exec(Term x, Binding bx, Term y, Binding by) {
		boolean succeeded = false;
		try {
			succeeded = unify(x, bx, y, by, 0);
		} finally {
			if(!succeeded) {
				rollback();
			}
		}
		return succeeded;
	}

	/**
	 * 実行結果を環境に反映します。
	 */
	public void commit(Query query) {
		if(TRACE) {
			System.err.println("Unifier.commit: 開始！");
		}
		
		ChoicePoint choicePoint = null;
		if(query != null) {
			choicePoint = query.getChoicePoint();
		}
		int count = 0;
		while(count < size) {
			VarRef var = vars[count++];
			if(TRACE) {
				System.err.println("\t"+ var +" := "+ ref(var.getTemp(), var.binding));
			}
			var.setSlot(var.getTemp());
			var.setTemp(null);
			if(choicePoint != null) choicePoint.add(var);
		}

		if(TRACE) System.err.println("Unifier.commit: "+ count +" 個の変数が変化しました。");
		size = 0;
	}

	/**
	 * 実行結果を取消します。
	 */
	public void rollback() {
		for(int i = 0; i < size; ++i) {
			vars[i].setTemp(null);
		}
		size = 0;
	}

	private boolean unify(Term x, Binding bx, Term y, Binding by, int level) {
		if(x == Variable._ || y == Variable._) {
			return true;
		}
		Term $x = ref(x, bx);
		Term $y = ref(y, by);
		if(TRACE) {
			trace("[unify] $x=" + $x, level);
			trace("        $y=" + $y, level);
		}
		if(($x == $y || $x.equals($y)) && !($x instanceof Template)) {
			trace("==> equal:" + ($x == $y), level);
			return true;
		}
		boolean X = $x instanceof VarRef;
		boolean Y = $y instanceof VarRef;
		// 片方が変数
		if(X != Y) {
			if(X)
			     substitute((VarRef) $x, $y.bind(by));
			else substitute((VarRef) $y, $x.bind(bx));
			trace("==> substitute", level);
			return true;
		}
		// どちらも変数
		if(X) {
			VarRef vx = (VarRef) $x;
			VarRef vy = (VarRef) $y;
			int delta = vx.binding.ordinal - vy.binding.ordinal;
			if(delta != 0 ? delta < 0: vx.var.id < vy.var.id)
			     substitute(vy, vx);
			else substitute(vx, vy);
			trace("==> union", level);
			return true;
		}
		while($x instanceof Thunk) $x = $x.strip();
		while($y instanceof Thunk) $y = $y.strip();
		if($x instanceof Complex &&
		   $y instanceof Complex) {
			Complex fx = (Complex) $x;
			Complex fy = (Complex) $y;
			if(fx.arity() == fy.arity()) {
				if(fx instanceof CompRef) { CompRef ref = (CompRef) fx; bx = ref.binding; fx = ref.comp; }
				if(fy instanceof CompRef) { CompRef ref = (CompRef) fy; by = ref.binding; fy = ref.comp; }
				if(fx.name().equals(fy.name())) {
					trace("==> + recursive: fx=" + fx + ", fy=" + fy, level);
					trace("               : $x=" + $x + ", $y=" + $y, level);
					for(int i = 0; i < fx.arity(); ++i) {
						trace("    + x("+i+") = " + fx.get(i), level);
						trace("    + y("+i+") = " + fy.get(i), level);
						if(!unify(fx.get(i), bx, fy.get(i), by, level+1)) return false;
					}
					return true;
				}
			}
		}
		return false;
	}
	
	private void trace(String msg, int level) {
		if(TRACE) {
			for(int i = 0; i < level; ++i)
			System.err.print("  ");
			System.err.println(msg);
		}
	}

	private void substitute(VarRef var, Term val) {
		if(size == vars.length) {
			System.arraycopy(vars, 0, (vars
					= new VarRef[(size > 1 ? size: 1) * 2]), 0, size);
		}
		( vars[size++] = var ).setTemp(val);
	}

	private Term ref(Term term, Binding binding) {
		while(term instanceof Variable) {
			if(term instanceof VarRef) {
				binding = ((VarRef) term).binding;
				term    = ((VarRef) term).var;
			}
			VarRef ref = ((Var) term).ref(binding);
			if((term = ref.getSlot()) != null) { return term; }
			if((term = ref.getTemp()) == null) { return  ref; }
		}
		return term;
	}

}
