package jp.segfault.prolog;

import jp.segfault.prolog.term.Atom;
import jp.segfault.prolog.term.Functor;
import jp.segfault.prolog.term.Term;

/**
 * クエリ実行中に発生する例外です。
 * @author shun
 */
@SuppressWarnings("serial")
public class QueryException extends RuntimeException {

	public final Term term;

	public QueryException(Term term) {
		super(term.toString());
		this.term = term;
	}

	public QueryException(String message, Throwable cause) {
		super(message, cause);
		term = Atom.NIL;
	}

	public QueryException(String message) {
		this(message, null);
	}

	public QueryException(Throwable cause) {
		this(null, cause);
	}
	
	public static QueryException instantiation_error() {
		return new QueryException(Atom.INSTANTIATION_ERROR);
	}
	
	public static QueryException type_error(Term validType, Term culprit) {
		return new QueryException(Functor.create("type_error", validType, culprit));
	}
	
	public static QueryException domain_error(Term validDomain, Term culprit) {
		return new QueryException(Functor.create("domain_error", validDomain, culprit));
	}

	public static QueryException existence_error(Term objectType, Term culprit) {
		return new QueryException(Functor.create("existence_error", objectType, culprit));
	}
	
	public static QueryException syntax_error(Term content) {
		return new QueryException(Functor.create("syntax_error", content));
	}
	
	public static QueryException io_error(Term operation, Term stream) {
		return new QueryException(Functor.create("io_error", operation, stream));
	}

}
