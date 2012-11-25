package jp.segfault.prolog.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.util.List;

import jp.segfault.prolog.Query;
import jp.segfault.prolog.QueryException;
import jp.segfault.prolog.State;
import jp.segfault.prolog.parser.ParseException;
import jp.segfault.prolog.parser.TermParser;
import jp.segfault.prolog.procedure.Coding;
import jp.segfault.prolog.term.Functor;
import jp.segfault.prolog.term.Term;

/**
 * 
 * Prolog のインタラクティブシェルです。
 * <p>使い方</p>
 * <pre><code>&gt; java toast.prolog.CLI [OPTION]..
 *01 ?- write('Hello, World!').
 *Hello, World!</code></pre>
 * 
 * @author shun
 *
 */
public class CLI {

	private final State state;
	private final BufferedReader reader;

	public static void main(String[] args) throws Exception {
		State state = new State(System.in, System.out);

		// コマンドラインで指定されたファイルを読む
		for(String filename: args) {
			state.parse(new File(filename));
		}
		System.err.println("% Welcome to sF-Prolog!");
		System.err.println();

		new CLI(state).exec();
	}

	public CLI(State state) {
		this.state = state;
		reader = new BufferedReader(new InputStreamReader(state.in));
	}

	public void exec() throws Exception {
		PipedWriter writer = new PipedWriter();
		PipedReader reader = new PipedReader();
		reader.connect(writer);
		TermParser<Term> parser = state.newParser(reader);
		for(int i = 0;; ) {
			state.out.printf("%02d ?- ", ++i);
			state.out.flush();
			for(int j = 0;; ) {
				String line = this.reader.readLine();
				if(line == null) {
					break;
				}
				writer.write(line +"\n");
				try {
					Term term = parser.next();
					if(term == null) {
						state.out.printf("% 4d| ", ++j);
						state.out.flush();
						continue;
					}
					do { ask(term); } while((term = parser.next()) != null);
				} catch(ParseException e) {
					state.out.println("解析エラー: "+ e.getMessage());
				}
				break;
			}
		}
	}

	public void ask(Term term) throws IOException {
		ask( Coding.create(state, Functor.create("?-", term)) );
	}

	public void ask(Coding coding) throws IOException {
		Query query = new Query(state, coding);
		try {
			do {
				List<Term> answer = query.ask();
				if(answer == null) {
					state.out.println("fail.");
					return;
				}
				for(int i = 0; i < answer.size(); ++i) {
					if(answer.get(i) != null) {
						state.out.println(coding.getOriginalName(i)
								+" = "+ answer.get(i).toString(state));
					}
				}
				if(!query.canBacktrack()) {
					break;
				}
				state.out.print("ok? [y/N]");
				state.out.flush();
			} while(reader.readLine().indexOf("y") == -1);
			state.out.println("true.");
		} catch(QueryException e) {
			e.printStackTrace();
			// state.out.println("エラー:"+ e.getMessage());
		}
	}

}
