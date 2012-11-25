package jp.segfault.prolog.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import jp.segfault.prolog.parser.Operator.Kind;

/**
 * 演算子テーブルです。
 * @author shun
 */
public class OperatorTable {

	private LinkedHashMap<String,ArrayList<Operator>> table
			= new LinkedHashMap<String,ArrayList<Operator>>();

	public void setOperator(Integer priority, Kind kind, String notation) {
		ArrayList<Operator> l = table.get(notation);
		if(l == null) {
			table.put(notation, ( l = new ArrayList<Operator>(2) ));
		}
		Operator operator = new Operator(priority, kind, notation);
		l.remove(operator);
		if(priority != 0) {
			l.add(operator);
			// System.err.println("OperatorTable: add op("+ kind +"): '"+ notation +"'");
		}
	}

	public List<Operator> getOperator(String notation) {
		ArrayList<Operator> l = table.get(notation);
		return l != null ? Collections.unmodifiableList(l): Collections.<Operator>emptyList();
	}

	public Set<String> keys() {
		return table.keySet();
	}
	
}
