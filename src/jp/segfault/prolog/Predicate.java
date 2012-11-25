package jp.segfault.prolog;

import jp.segfault.prolog.term.Functor;
import jp.segfault.prolog.term.Term;

/**
 * Prologの述語です。
 * @author shun
 */
public class Predicate {

	public static final Predicate        NONE = new Predicate("none", -1);
	public static final Predicate      CLAUSE = new Predicate(  ":-",  2);
	public static final Predicate   DIRECTIVE = new Predicate(  ":-",  1);
	public static final Predicate       QUERY = new Predicate(  "?-",  1);
	public static final Predicate        RULE = new Predicate( "-->",  2);
	public static final Predicate CONJUNCTION = new Predicate(   ",",  2);
	public static final Predicate DISJUNCTION = new Predicate(   ";",  2);
	public static final Predicate     IF_THEN = new Predicate(  "->",  2);
	public static final Predicate         CUT = new Predicate(   "!",  0);
	public static final Predicate        CALL = new Predicate("call",  1);
	
	public final String id;
	public final int arity;
	
	public Predicate(String id, int arity) {
		this.id    = id;
		this.arity = arity;
	}

	public static Predicate of(String notation) {
		int slash = notation.lastIndexOf('/');
		if(slash == -1) {
			throw new IllegalArgumentException("slash == -1");
		}
		return new Predicate(notation.substring(0, slash),
				Integer.valueOf(notation.substring(slash + 1)));
	}

	public static Predicate of(Term term) {
		Functor functor = (Functor) term;
		if(!new Predicate("/", 2).equals(functor.predicate())) {
			return null;
		}
		return new Predicate(
				functor.get(0).   atom().value(),
				functor.get(1).numeric().value().intValue());
	}

	@Override
	public String toString() {
		return id +"/"+ arity;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof Predicate) {
			Predicate that = (Predicate) o;
			return arity == that.arity && id.equals(that.id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return id.hashCode() * arity;
	}

	public Functor toFunctor() {
		return Functor.create("/"
				, Term.valueOf(id)
				, Term.valueOf(arity));
	}
	
}
