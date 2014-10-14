package jp.segfault.prolog.term;

/**
 * アトムを表現します。
 * @author shun
 */
public class Atom extends Functor implements Atomic<String> {
	
	// リスト
	public static final Atom NIL  = Atom.make("[]");
	public static final Atom CONS = Atom.make(".");
	
	// エラー名
	public static final Atom INSTANTIATION_ERROR = Atom.make("instantiation_error");
	public static final Atom TYPE_ERROR          = Atom.make("type_error");
	public static final Atom DOMAIN_ERROR        = Atom.make("domain_error");
	public static final Atom EXISTENCE_ERROR     = Atom.make("existence_error");
	public static final Atom SYNTAX_ERROR        = Atom.make("syntax_error");
	public static final Atom IO_ERROR            = Atom.make("io_error");
	
	// エラータイプ
	public static final Atom OPERATOR_PRIORITY  = Atom.make("operator_priority");
	public static final Atom OPERATOR_SPECIFIER = Atom.make("operator_specifier");
	public static final Atom SOURCE_SINK        = Atom.make("source_sink");
	public static final Atom END_OF_FILE        = Atom.make("end_of_file");

	// データ型
	public static final Atom ATOM      = Atom.make("atom");
	public static final Atom FUNCTOR   = Atom.make("functor");
	public static final Atom NUMERIC   = Atom.make("numeric");
	public static final Atom VARIABLE  = Atom.make("variable");
	public static final Atom INTEGER   = Atom.make("integer");
	public static final Atom FLOAT     = Atom.make("float");
	public static final Atom STRING    = Atom.make("string");
	public static final Atom STREAM    = Atom.make("stream");
	public static final Atom PROCEDURE = Atom.make("procedure");
	public static final Atom PREDICATE = Atom.make("predicate");
	public static final Atom BYTE      = Atom.make("byte");
	public static final Atom CHARACTER = Atom.make("character");
	
	// その他
	public static final Atom EMPTY = Atom.make("");
	public static final Atom NONE  = Atom.make("()");
	public static final Atom NULL  = Atom.make("{}");
	public static final Atom CYCLE = Atom.make("**");
	
	private final String value;

	private Atom(String value) {
		this.value = value;
	}

	/**
	 * 指定した値を保持する{@link Atom}を作成します。
	 */
	public static Atom make(String value) {
		return new Atom(value);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Atom && value.equals(((Atom) o).value);
	}

	@Override
	public int compareTo(Atomic<String> rhs) {
		return value.compareTo(rhs.value());
	}

	@Override
	public <R,A> R accept(A arg, Visitor<R,A> visitor) {
		return visitor.visit(arg, this);
	}

	@Override
	public Atom atom() {
		return this;
	}

	@Override
	public String name() {
		return value;
	}
	
	/**
	 * このアトムが保持する値です。
	 */
	@Override
	public String value() {
		return value;
	}
	
}
