package jp.segfault.prolog.script;

import java.util.Arrays;

import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import jp.segfault.prolog.parser.Quotemeta;

public class PlScriptEngineFactory implements ScriptEngineFactory {

	static {
		new ScriptEngineManager().registerEngineName(
				"prolog", new PlScriptEngineFactory());
	}
	
	@Override
	public String getEngineName() {
		return "sF-Prolog";
	}

	@Override
	public String getEngineVersion() {
		return "0.1.1";
	}

	@Override
	public List<String> getExtensions() {
		return Arrays.asList("pl", "pro");
	}

	@Override
	public List<String> getMimeTypes() {
		return Arrays.asList("text/x-prolog");
	}

	@Override
	public List<String> getNames() {
		return Arrays.asList("prolog");
	}

	@Override
	public String getLanguageName() {
		return "prolog";
	}

	@Override
	public String getLanguageVersion() {
		return "";
	}

	@Override
	public Object getParameter(String key) {
		switch(key) {
		case ScriptEngine.ENGINE:
			return getEngineName();
		case ScriptEngine.ENGINE_VERSION:
			return getEngineVersion();
		case ScriptEngine.NAME:
			return getNames();
		case ScriptEngine.LANGUAGE:
			return getLanguageName();
		case ScriptEngine.LANGUAGE_VERSION:
			return getLanguageVersion();
		case "THREADING":
			return "THREAD-ISOLATED";
		}
		return null;
	}

	@Override
	public String getMethodCallSyntax(String obj, String m, String... args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getOutputStatement(String toDisplay) {
		return Quotemeta.quote(toDisplay);
	}

	@Override
	public String getProgram(String... statements) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScriptEngine getScriptEngine() {
		return new PlScriptEngine();
	}

}
