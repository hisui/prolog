package jp.segfault.prolog;

import java.util.Arrays;

import jp.segfault.prolog.code.Code;
import jp.segfault.prolog.procedure.Procedure;
import jp.segfault.prolog.term.Term;

/**
 * コールフレームを表現します。
 * @author shun
 */
public class Binding {

	public final int       ancestry;
	public final Procedure procedure;
	public final Code      next;
	public final Binding   caller;
	public final int       depth;
	public final int       ordinal;

	// call/1 の為に後から変更できるように・・・
	private Term[] local = new Term[]{};

	public Binding(int ancestry, Procedure procedure, Code next,
			Binding caller, Binding base)
	{
		this.ancestry  = ancestry;
		this.procedure = procedure;
		this.next      = next;
		this.caller    = caller;
		this.depth     = caller != null ? caller.depth + 1: 0;
		if(base != null) {
			ordinal = base.ordinal;
			local   = base.local;
			return;
		}
		ordinal = ancestry;
		if(procedure != null) {
			local = new Term[procedure.locals() * 2];
		}
	}
	
	public Term getSlot(int i) { return local[i*2  ]; }
	public Term getTemp(int i) { return local[i*2+1]; }
	
	public void setSlot(int i, Term term) { local[i*2  ] = term; }
	public void setTemp(int i, Term term) { local[i*2+1] = term; }
	
	/**
	 * ローカル領域のサイズです。
	 */
	public int locals() {
		return local.length / 2;
	}

	/**
	 * ローカル領域を拡張します。
	 */
	public void expandLocal(int locals) {
		if(locals > locals()) {
			local = Arrays.copyOf(local, locals * 2);
		}
	}
}
