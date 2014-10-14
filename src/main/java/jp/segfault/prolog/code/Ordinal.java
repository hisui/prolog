package jp.segfault.prolog.code;

/**
 * プロパティ{@link #next}に次に実行する{@link Code}を持ちます。
 * @author shun
 */
public abstract class Ordinal extends Code {

	public final Code next;
	
	public Ordinal(Code next) {
		this.next = next;
	}
}
