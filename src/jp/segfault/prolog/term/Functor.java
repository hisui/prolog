package jp.segfault.prolog.term;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import jp.segfault.prolog.Predicate;

/**
 * 関数子を表現します。
 * @author shun
 */
public abstract class Functor extends Term {

	@SuppressWarnings("unchecked")
	public static <T extends Term> Functor create(String name, List<T> args) {
		return (args == null || args.isEmpty())
				? Atom.make(name)
				: Comp.make(name, (List<Term>) args);
	}
	
	public static Functor create(String name, Term ...args) {
		return create(name, Arrays.asList(args));
	}
	
	public static Term list(Term ...terms) {
		return list(Arrays.asList(terms));
	}

	public static <T extends Term> Term list(Iterable<T> iterable) {
		return list(iterable.iterator());
	}
	
	public static <T extends Term> Term list(Iterator<T> i) {
		return list(i, Atom.NIL);
	}

	public static <T extends Term> Term list(Iterator<T> i, Term tail) {
		return foldBy(".", i, tail);
	}

	public static Term conj(Term ...terms) {
		return foldBy(",", Arrays.asList(terms));
	}

	public static <T extends Term> Term disj(Term ...terms) {
		return foldBy(";", Arrays.asList(terms));
	}

	public static <T extends Term> Term foldBy(String name, List<T> a) {
		assert !a.isEmpty();
		return foldBy(name, a.subList(0, a.size() - 1).iterator(), a.get(a.size() - 1));
	}

	public static <T extends Term> Term foldBy(String name, Iterator<T> i, Term tail) {
		while (i.hasNext()) {
			tail = create(name, Arrays.asList(i.next(), tail));
		}
		return tail;
	}

	@Override
	public <R,A> R accept(A arg, Visitor<R,A> visitor) {
		return visitor.visit(arg, this);
	}
	
	@Override
	public Functor functor() {
		return this;
	}

	/**
	 * 関数子の名前です。
	 */
	public abstract String name();

	/**
	 * この関数子の{@link Predicate}表現です。
	 */
	public Predicate predicate() {
		return new Predicate(name(), arity());
	}
	
	/**
	 * 関数子の引数を返します。
	 * <p>戻り値の配列に破壊的操作を加えることは出来ません。</p>
	 */
	public Term[] args() {
		return new Term[]{};
	}

	/**
	 * 関数子のアリティ(引数の数)です。
	 */
	public int arity() {
		return 0;
	}
	
	/**
	 * i番目の引数を取得します。
	 */
	public Term get(int i) {
		throw new ArrayIndexOutOfBoundsException(String.valueOf(i));
	}

	/**
	 * この関数子を、リストを構成するconsセルまたはcdrとみなし、リストの要素をすべて返します。
	 */
	public List<Term> unlist() {
		return unlist(new Predicate(".", 2), false);
	}

	/**
	 * この関数子を、リストを構成するconsセルまたはcdrとみなし、リストの要素をすべて返します。
	 * <p>{@link Functor#asList()}と違い、consセルとみなされる関数子の形を{@link Predicate}で指定できます。</p>
	 */
	public List<Term> unlist(Predicate cons, boolean withNil) {
		ArrayList<Term> list = new ArrayList<Term>();
		Term tail = this;
		while ((tail = tail.strip()) instanceof Functor) {
			Functor functor = (Functor) tail;
			if (!cons.equals(functor.predicate())) {
				break;
			}
			list.add(functor.get(0).strip());
			tail =  (functor.get(1).strip());
		}
		if (withNil) {
			list.add(tail);
		}
		return list;
	}

}
