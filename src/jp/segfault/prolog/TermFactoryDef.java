package jp.segfault.prolog;

import java.util.List;

import jp.segfault.prolog.parser.TermFactory;
import jp.segfault.prolog.term.Functor;
import jp.segfault.prolog.term.Term;
import jp.segfault.prolog.term.Variable;

public class TermFactoryDef extends TermFactory<Term> {

	@Override
	public Term newAtom(String value) {
		return Term.valueOf(value);
	}

	@Override
	public Term newAtom(int value) {
		return Term.valueOf(value);
	}

	@Override
	public Term newAtom(double value) {
		return Term.valueOf(value);
	}

	@Override
	public Term newFunctor(String value, List<Term> args) {
		return Functor.create(value, args);
	}

	@Override
	public Term newVariable(String value) {
		return Variable.create(value);
	}

}
