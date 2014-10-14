package jp.segfault.prolog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import jp.segfault.prolog.code.Code;
import jp.segfault.prolog.event.DatabaseEvent;
import jp.segfault.prolog.event.DatabaseListener;
import jp.segfault.prolog.parser.OperatorTable;
import jp.segfault.prolog.parser.ParseException;
import jp.segfault.prolog.parser.Parser;
import jp.segfault.prolog.parser.PipedParser;
import jp.segfault.prolog.parser.TermParser;
import jp.segfault.prolog.procedure.Coding;
import jp.segfault.prolog.procedure.Foreign;
import jp.segfault.prolog.procedure.Procedure;
import jp.segfault.prolog.term.Functor;
import jp.segfault.prolog.term.Term;
import jp.segfault.prolog.term.Variable;

/**
 * 
 * インタープリターのグローバルな状態をカプセル化します。
 * <p>使い方</p>
 * <pre>
 * State state = new State(System.in, System.out);
 * 
 * // スクリプトファイルのロード
 * state.parse(new BufferedReader(new InputStreamReader(
 *         State.class.getResourceAsStream("resource/init.pl"), "UTF-8")));
 * 
 * // クエリの実行
 * state.parse("?- append([a, b, c], [1, 2, 3], List).");
 * </pre>
 * 
 * @author shun
 *
 */
public class State {

	// (述語 -> テーブル) のマッピングで、データベースの実体
	private LinkedHashMap<Predicate,Table> tables = new LinkedHashMap<Predicate,Table>();
	
	// set_prolog_flag/2 で設定できるプロパティ
	private LinkedHashMap<String,String> flags = new LinkedHashMap<String,String>();

	// op/3 で設定できる演算子の宣言
	private OperatorTable operatorTable = new OperatorTable();
	
	// データベースの変化の通知を受けるリスナー
	private HashSet<DatabaseListener> databaseListenerSet = new HashSet<DatabaseListener>();
	
	// コンパイル済みのコードのキャッシュ
	private WeakHashMap<Term,SoftReference<Code>> codePool = new WeakHashMap<Term,SoftReference<Code>>();
	
	// キャッシュのリファレンスの開放の通知を受けるキュー
	private ReferenceQueue<Code> codePoolQueue = new ReferenceQueue<Code>();

	public final InputStream  in;
	public final PrintStream out;
	
	public State(InputStream in, PrintStream out) {
		
		this.in  =  in;
		this.out = out;
		
		// 組み込みの述語を組み込む
		loadForeign(Foreign.class);

		try {
			parse(new InputStreamReader(getClass().getResourceAsStream("init.pl"), "UTF-8"));
		} catch (Exception e) {
			// e.printStackTrace();
			throw new IllegalStateException("init.pl の読み込みに失敗！", e);
		}
	}

	public State() {
		this(System.in, System.out);
	}

	public OperatorTable getOperatorTable() {
		return operatorTable;
	}
	
	/**
	 * {@link DatabaseListener}を追加します。
	 */
	public void addDatabaseListener(DatabaseListener listener) {
		databaseListenerSet.add(listener);
	}
	
	/**
	 * {@link DatabaseListener}を削除します。
	 */
	public void removeDatabaseListener(DatabaseListener listener) {
		databaseListenerSet.remove(listener);
	}

	/**
	 * Prolog Flag を設定します。
	 */
	public String setFlag(String key, String value) {
		return flags.put(key, value);
	}

	/**
	 * Prolog Flag を取得します。
	 */
	public String getFlag(String key) {
		return flags.get(key);
	}
	
	/**
	 * 指定した述語に割り当てられた{@link Table}取得します。存在しない場合はnullを返します。
	 */
	public Table getTable(Predicate predicate) {
		return tables.get(predicate);
	}

	/**
	 * 指定した述語に割り当てられた{@link Table}取得します。create == true の場合、存在しない場合に空のテーブルを作成します。
	 */
	public Table getTable(Predicate predicate, boolean create) {
		Table table = tables.get(predicate);
		if (create && table == null) {
			tables.put(predicate, ( table = new Table(predicate) ));
			if (!databaseListenerSet.isEmpty()) {
				DatabaseEvent event = new DatabaseEvent(this);
				for (DatabaseListener l: databaseListenerSet) {
					l.tableCreated(event);
				}
			}
		}
		return table;
	}

	/**
	 * 全ての述語を取得します。
	 */
	public Set<Predicate> predicates() {
		return Collections.unmodifiableSet(tables.keySet());
	}

	/**
	 * 全てのProlog Flagのキーを取得します。
	 */
	public Set<String> flagKeys() {
		return Collections.unmodifiableSet(flags.keySet());
	}
	
	/**
	 * 指定した述語に属する{@link Procedure}を全て取得します。
	 */
	public List<Procedure> select(Predicate predicate) {
		Table table = getTable(predicate);
		return table != null ? table.rows(): Collections.<Procedure>emptyList();
	}

	/**
	 * 指定した述語に{@link Procedure}を挿入します。
	 */
	public void insert(Predicate predicate, Procedure procedure) {
		insert(predicate, procedure, -1);
	}

	/**
	 * 指定した述語(Prologの項表現)に{@link Procedure}を挿入します。
	 */
	public void insert(String predicate, Procedure procedure) {
		insert(Predicate.of(predicate), procedure, -1);
	}
	
	/**
	 * 指定した述語の指定した位置に{@link Procedure}を挿入します。
	 */
	public void insert(Predicate predicate, Procedure procedure, int i) {
		getTable(predicate, true).insert(procedure, i);
	}

	/**
	 * 指定した述語に属するi番目の{@link Procedure}を削除します。
	 */
	public void delete(Predicate predicate) {
		Table table = getTable(predicate);
		if (table != null) {
			table.clear();
		}
	}
	
	public Code getPoolingCode(Term term) {
		for (;;) {
			Reference<? extends Code> ref = codePoolQueue.poll();
			if (ref == null) {
				break;
			}
			Iterator<Map.Entry<Term,SoftReference<Code>>> i = codePool.entrySet().iterator();
			while (i.hasNext()) {
				if (ref == i.next().getValue()) {
					i.remove();
					break;
				}
			}
		}
		Reference<Code> ref = codePool.get(term);
		return ref == null ? null: ref.get();
	}
	
	public void putPoolingCode(Term term, Code code) {
		codePool.put(term, new SoftReference<Code>(code));
	}

	/**
	 * 指定したクラスの静的フィールドから{@link Procedure}をインポートします。
	 */
	public <T> void loadForeign(Class<T> clazz) {
		try {
			for (Field field: clazz.getFields()) {
				Foreign.Declaration decl = field.getAnnotation(Foreign.Declaration.class);
				if (decl != null) {
					// System.err.println("登録:"+ decl.value());
					insert(Predicate.of(decl.value()), (Procedure) field.get(null));
				}
			}
		} catch(IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * 現在の出力ストリームを取得します。
	 */
	public PrintStream getOutput() {
		return out;
	}
	
	/**
	 * 現在の入力ストリームを取得します。
	 */
	public InputStream getInput() {
		return in;
	}
	
	/**
	 * Prologテキストを解析し、評価します。
	 */
	public void parse(String script) throws ParseException {
		try {
			parse(new StringReader(script));
		} catch(IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Prologテキストをファイルから読み取って解析し、評価します。
	 */
	public void parse(File file) throws IOException, ParseException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), "UTF-8"));
		try {
			parse(reader);
		} finally {
			reader.close();
		}
	}

	/**
	 * Prologテキストをストリームから読み取って解析し、評価します。
	 */
	public void parse(Reader reader) throws IOException, ParseException {
		
		// ロード時間の計測
		Long start = "yes".equals(getFlag("benchmark_parse")) ? System.currentTimeMillis(): null;
		
		for (TermParser<Term> parser = newParser(reader);; ) {
			Term term = parser.next();
			if (term == null) {
				break;
			}
			if (getTable(Predicate.of("term_expansion/2")) != null) {
				List<Term> l = new Query(this,Coding.create(this
						, Functor.create("?-", Functor.create
								("term_expansion", term, Variable.create())))).ask();
				if (l != null) {
					term = l.get(l.size() - 1);
				}
			}
			Coding coding = Coding.create(this, term);
			if (coding.head != null) { // 節の追加
				insert(coding.head.predicate(), coding);
			}
			else { // クエリまたはディレクティブの実行
				new Query(this, coding).ask();
			}
		}
		if (start != null) {
			System.err.println("State.parse: Elapsed time: "
					+ (System.currentTimeMillis() - start) +" msec.");
		}
	}

	/**
	 * 
	 * クエリを作成します。
	 * <p>
	 * <p>使い方</p>
	 * <pre><code>State state = ...;
	 *Query query = state.query("write(%msg).", "Hello, World!");
	 *query.ask(); // output: Hello, World!</code></pre>
	 * </p>
	 * 
	 */
	public Query newQuery(String script0, Object ...args) throws ParseException {
		HashMap<String,Integer> vars = new HashMap<>();
		String script = "";
		for (int i = 0; i < script0.length(); ) {
			int j = script0.indexOf('%', i);
			if (j == -1) {
				script += script0.substring(i);
				break;
			}
			script += script0.substring(i, j);
			i = ++j;
			while (i < script0.length() && Character.isLowerCase(script0.charAt(i))) i++;
			if (i == j) {
				throw new IllegalArgumentException("変数名がありません: col="+ i);
			}
			String key = script0.substring(j, i);
			if (!vars.containsKey(key)) {
				vars.put(key, vars.size());
			}
			script += " _"+ key;
		}
		// System.err.println("State.newQuery: script="+ script);
		Coding coding;
		try {
			coding = Coding.create(this, Functor.create
					("?-", newParser(new StringReader(script)).next()));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		Query query = new Query(this, coding);
		for (Map.Entry<String,Integer> e: vars.entrySet()) {
			query.callee.setSlot(coding.vars().get("_"+ e.getKey()).id(), Term.valueOf(args[e.getValue()]));
		}
		return query;
	}
	
	/**
	 * {@link TermParser<Term>}を生成します。
	 */
	public TermParser<Term> newParser(Reader reader) {
		if (reader instanceof PipedReader) {
			return new PipedParser<Term>((PipedReader) reader
					, new TermFactoryDef(), getOperatorTable());
		}
		return new Parser<Term>(reader, new TermFactoryDef(), getOperatorTable());
	}

	@SuppressWarnings("resource")
	public TermParser<Term> newParser(PipedWriter writer) {
		PipedReader reader = new PipedReader();
		try {
			reader.connect(writer);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		return newParser(reader);
	}

}
