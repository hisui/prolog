package jp.segfault.prolog.event;

import java.util.EventListener;

/**
 * データベースの変化の通知を受け取るリスナーです。
 * @author shun
 */
public interface DatabaseListener extends EventListener {

	/**
	 * データベース中にあらたなテーブルが作成されたときに呼び出されます。
	 */
	public void tableCreated(DatabaseEvent event);
}
