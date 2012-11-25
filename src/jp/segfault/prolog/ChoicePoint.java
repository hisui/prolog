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
	public final int         ancestry;
	
	private VarRef head;
	// private int size = 0;
	
	final Iterator<Code> iterator;
	
	ChoicePoint(ChoicePoint parent,
			Binding callee, int ancestry, Iterator<Code> iterator)
	{
		this.parent   = parent;
		this.callee   = callee;
		this.ancestry = ancestry;
		this.iterator = iterator;
	}
	
	public void add(VarRef var) {
		if(var.binding.ordinal <= ancestry) {
			if(var.getTemp() != null) {
				throw new IllegalStateException("getTemp(var) != null");
			}
			var.setTemp(head);
			head = var;
			// ++size;
		}
	}

	public void add(ChoicePoint that) {
		for(ChoicePoint prev = that; prev != this; prev = prev.parent) {
			VarRef next = prev.head;
			while(next != null) {
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
		while(head != null) {
			VarRef var = head;
			head = (VarRef) var.getTemp();
			var.setTemp(null);
			var.setSlot(null);
		}
		// size = 0;
	}
}

