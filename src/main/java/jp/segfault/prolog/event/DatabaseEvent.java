package jp.segfault.prolog.event;

import java.util.EventObject;

/**
 * データベースの変化を表すイベントオブジェクトです。
 * @author shun
 */
@SuppressWarnings("serial")
public class DatabaseEvent extends EventObject {
	public DatabaseEvent(Object source) {
		super(source);
	}
}
