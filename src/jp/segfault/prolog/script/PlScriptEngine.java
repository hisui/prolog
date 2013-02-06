package jp.segfault.prolog.script;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import jp.segfault.prolog.Query;
import jp.segfault.prolog.State;
import jp.segfault.prolog.parser.ParseException;
import jp.segfault.prolog.procedure.Coding;
import jp.segfault.prolog.term.Functor;

public class PlScriptEngine extends AbstractScriptEngine {
	
	private State state = new State();

	@Override
	public Object eval(String script, ScriptContext context)
			throws ScriptException {
		return eval(new StringReader(script), context);
	}

	@Override
	public Object eval(Reader reader, ScriptContext context)
			throws ScriptException {
		try {
			return new Query(state, Coding.create(state,
					Functor.create("?-", state.newParser(reader).next()))).ask();
		} catch (IOException | ParseException e) {
			throw new ScriptException(e);
		}
	}

	@Override
	public Bindings createBindings() {
		return new SimpleBindings();
	}

	@Override
	public ScriptEngineFactory getFactory() {
		return new PlScriptEngineFactory();
	}

}
