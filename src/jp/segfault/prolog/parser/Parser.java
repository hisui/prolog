package jp.segfault.prolog.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;


import static jp.segfault.prolog.parser.Token.Kind.*;

/**
 * {@link TermParser}の実装です。
 * @author shun
 */
public class Parser<TERM> implements TermParser<TERM> {

	private final CountingReader reader;
	private final Tokenizer tokenizer;
	private final TermFactory<TERM> factory;
	private final OperatorTable optable;

	public Parser(Reader reader, TermFactory<TERM> factory, OperatorTable optable) {
		tokenizer = new Tokenizer(( this.reader = new CountingReader(reader) ));
		this.factory = factory;
		this.optable = optable;
	}
	
	/**
	 * 次のProlog節を解析して返します。
	 */
	@Override
	public TERM next() throws IOException, ParseException {
		try {
			Token token = tokenizer.next(true);
			if(token == null) {
				return null;
			}
		    return term(token, false, ".");
		} catch(ParseException e) {
			int row = reader.getRow();
			int col = reader.getCol();
			throw new ParseException(
					e.getMessage() +" ["+ row +":"+ col +"]", e, row, col);
		}
	}

	private TERM term(boolean nullable,
			String ...terminators) throws IOException, ParseException {
		return term(tokenizer.next(true), nullable, terminators);
	}

	private TERM term(Token token, boolean nullable, String ...terminators)
			throws IOException, ParseException {
		OperatorJoiner<TERM> joiner = new OperatorJoiner<TERM>() {
			@Override
			protected TERM join(String notation, List<TERM> args) {
				return factory.newFunctor(notation, args);
			}
		};
	outer:
		for(int i = 0; ; ++i, token = tokenizer.next(joiner.accept(Operator.Kind.x))) {
			if(token == null) {
				throw new ParseException("不正な位置でEOFを検出しました。");
			}
			if(!token.quote) {
				if(nullable && i == 0 || joiner.accept(Operator.Kind.xf)) {
					for(String terminator: terminators) {
						if(token.value.equals(terminator)) {
							return i > 0 ? joiner.complete(): null;
						}
					}
				}
				if(token.kind == ATOM_STR) {
					for(Operator right: optable.getOperator(token.value)) {
						if(joiner.accept(right.kind)) {
							joiner.push(right);
							continue outer;
						}
					}
				}
			}
			if(!joiner.accept(Operator.Kind.x)) {
				throw new ParseException("演算子の解決が出来ません！token="+ token);
			}
			joiner.push(literal(token));
		}
	}
	
	private TERM literal(Token token) throws IOException, ParseException {
		TERM term;
		switch(token.kind) {
		case VAR:      return factory.newVariable(token.value);
		case FUNC_BGN: return complex(token.value);
		case ATOM_STR: return factory.newAtom(token.value);
		case ATOM_INT: return factory.newAtom(Integer.valueOf(token.value));
		case ATOM_FPN: return factory.newAtom( Double.valueOf(token.value));
		case SPECIAL:
			switch(token.value.charAt(0)) {
			case '(': return (term = term(true, ")")) == null ? factory.newAtom("()"): term;
			case '{': return (term = term(true, "}")) == null ? factory.newAtom("{}"): factory.newFunctor("{}", asList(term));
			case '[':
				if((term = term(true, ",", "|", "]")) == null) {
					if(!"]".equals(tokenizer.peek().value)) {
						throw new ParseException("要素がありません・・・。");
					}
					return factory.newAtom("[]");
				}
				ArrayDeque<TERM> elements = new ArrayDeque<>();
				elements.push(term);
				while(",".equals(tokenizer.peek().value)) {
					elements.push(term(false, ",", "|", "]"));
				}
				TERM head = factory.newAtom("[]");
				if("|".equals(tokenizer.peek().value)) {
					head = term(false, "]");
				}
				for(TERM e: elements) {
					head = factory.newFunctor(".", asList(e, head));
				}
				return head;
			}
		}
		throw new ParseException("不明なトークン: "+ token);
	}

	private TERM complex(String name) throws IOException, ParseException {
		ArrayList<TERM> args = new ArrayList<>();
		do {
			args.add(term(false, ",", ")"));
		} while(",".equals(tokenizer.peek().value));
		return factory.newFunctor(name, args);
	}

}
