package jp.segfault.prolog.event;

import java.util.EventListener;

/**
 * テーブルの変化の通知を受けるリスナーです。
 * @author shun
 */
public interface TableChangeListener extends EventListener {

	/**
	 * テーブルに行が挿入されたときに呼び出されます。
	 */
	public void rowInserted(TableChangeEvent event);

	/**
	 * テーブルの中の行が削除されたときに呼び出されます。
	 */
	public void rowDeleted(TableChangeEvent event);

}
