package jp.segfault.prolog.event;

import java.util.EventObject;

/**
 * テーブルの変化を表すイベントオブジェクトです。
 * @author shun
 */
@SuppressWarnings("serial")
public class TableChangeEvent extends EventObject {
	public TableChangeEvent(Object source) {
		super(source);
	}
}
