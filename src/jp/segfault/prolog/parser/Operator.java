package jp.segfault.prolog.parser;

import static jp.segfault.prolog.parser.Operator.Kind.*;

/**
 * Prologの演算子を表現します。
 * @author shun
 */
public class Operator {

	public final int priority;
	public final Kind kind;
	public final String notation;
	public final int lprio;
	public final int rprio;

	public Operator(int priority, Kind kind, String notation, int lprio, int rprio) {
		this.priority = priority;
		this.notation = notation;
		this.kind     = kind;
		this.lprio    = lprio;
		this.rprio    = rprio;
	}
	
	public Operator(int priority, Kind kind, String notation) {
		this(priority, kind, notation,
				kind.round() == fx ? -1 : priority*2 + kind.lprio(),
				kind.round() == xf ? -1 : priority*2 + kind.rprio());
	}

	/**
	 * 与えられた演算子の並び順が表記上正しいかどうかを判別します。
	 */
	public static boolean isCorrectOrder(Kind l, Kind r) {
		l = l.round();
		r = r.round();
		switch(r) {
		case   x: case  fx: return l != x  && l != xf  ;
		case xfx: case  xf: return l != fx && l != xfx ;
		default:
			throw new IllegalStateException(l +", "+ r);
		}
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof Operator) {
			Operator that = (Operator) o;
			return kind.round() == that.kind.round() && notation.equals(that.notation);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "op("+ lprio +":"+ rprio +", "+ kind +", '"+ notation +"')";
	}

	/**
	 * 演算子の種類です。
	 * @author shun
	 */
	public enum Kind {
		
		/**
		 * 中置の二項演算子です。
		 */
		xfx(2),
		
		/**
		 * 中置の二項演算子です。(右結合)
		 */
		xfy(2),
		
		/**
		 * 中置の二項演算子です。(左結合)
		 */
		yfx(2),
		
		/**
		 * 前置演算子です。
		 */
		fx(1), fy(1),
		
		/**
		 * 前置演算子です。
		 */
		xf(1),  yf(1),
		
		/**
		 * オペランドを表現します。
		 */
		x(0);
		
		/**
		 * この演算子が結合する項数です。
		 */
		public final int arity;
		
		Kind(int arity) {
			this.arity = arity;
		}
		
		public Kind round() {
			switch(this) {
			case xfy:
			case yfx:
				return xfx;
			case fy: return fx;
			case yf: return xf;
			default: return this;
			}
		}
		
		private int lprio() {
			switch(this) {
			case yfx: case yf: return 1;
			default:
				return 0;
			}
		}
		
		private int rprio() {
			switch(this) {
			case xfy: case fy: return 1;
			default:
				return 0;
			}
		}
	}
	
}
