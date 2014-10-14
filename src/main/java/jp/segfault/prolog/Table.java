package jp.segfault.prolog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import jp.segfault.prolog.event.TableChangeEvent;
import jp.segfault.prolog.event.TableChangeListener;
import jp.segfault.prolog.procedure.Procedure;

/**
 * Prologの述語データベースのテーブルです。
 * @author shun
 */
public class Table {
	
	private final Predicate predicate;
	
	@SuppressWarnings("unused")
	private int revision = 0;
	
	@SuppressWarnings("serial")
	private ArrayList<Procedure> rows = new ArrayList<Procedure>() {};
	
	private HashSet<TableChangeListener> tableChangeListenerSet = new HashSet<TableChangeListener>();
	
	public Table(Predicate predicate) {
		this.predicate = predicate;
	}

	public Predicate predicate() {
		return predicate;
	}

	public List<Procedure> rows() {
		return Collections.unmodifiableList(rows);
	}

	public void insert(Procedure procedure, int i) {
		if(i < 0) {
			i += rows.size() + 1;
		}
		rows.add(i, procedure);
		revision ++;
		fireTableChangeEvent(EventType.INSERT, new TableChangeEvent(this));
	}
	
	public void clear() {
		rows.clear();
	}

	public void addTableChangeListener(TableChangeListener listener) {
		tableChangeListenerSet.add(listener);
	}

	public void removeTableChangeListener(TableChangeListener listener) {
		tableChangeListenerSet.remove(listener);
	}
	
	private enum EventType { INSERT, DELETE }
	private void fireTableChangeEvent(EventType type, TableChangeEvent event) {
		for(TableChangeListener l: tableChangeListenerSet) {
			switch(type) {
			case INSERT: l.rowInserted(event); break;
			case DELETE: l.rowDeleted (event); break;
			}
		}
	}
}
