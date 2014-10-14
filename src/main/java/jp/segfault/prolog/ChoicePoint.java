package jp.segfault.prolog;

import java.util.Iterator;

import jp.segfault.prolog.code.Code;
import jp.segfault.prolog.term.VarRef;

/**
 * バックトラッキングの実行位置を表現します。
 * @author shun
 */
public class ChoicePoint {

	public final ChoicePoint parent;
	public final Binding     callee;
	public final boolean     local;
	public final int         ancestry;
	
	private VarRef head;
	
	final Iterator<Code> iterator;
	
	ChoicePoint(ChoicePoint parent, Binding callee
			, int ancestry, boolean local, Iterator<Code> iterator)
	{
		this.parent   = parent;
		this.callee   = callee;
		this.local    = local;
		this.ancestry = ancestry;
		this.iterator = iterator;
	}
	
	public void add(VarRef var) {
		if (var.binding.ordinal <= ancestry) {
			if (var.getTemp() != null) {
				throw new IllegalStateException("getTemp(var) != null");
			}
			var.setTemp(head);
			head = var;
		}
	}

	public void drain(ChoicePoint that) {
		for (ChoicePoint prev = that; prev != this; prev = prev.parent) {
			VarRef next = prev.head;
			while (next != null) {
				VarRef temp = (VarRef) next.getTemp();
				if(next.binding.ordinal <= ancestry) {
					next.setTemp(head);
					head = next;
				}
				next = temp;
			}
		}
	}

	public void undo() {
		while (head != null) {
			VarRef var = head;
			head = (VarRef) var.getTemp();
			var.setTemp(null);
			var.setSlot(null);
		}
	}
}

