package jp.segfault.prolog.tool;

import java.io.File;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.util.List;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jp.segfault.prolog.Predicate;
import jp.segfault.prolog.Query;
import jp.segfault.prolog.QueryException;
import jp.segfault.prolog.State;
import jp.segfault.prolog.parser.ParseException;
import jp.segfault.prolog.parser.TermParser;
import jp.segfault.prolog.procedure.Coding;
import jp.segfault.prolog.term.Functor;
import jp.segfault.prolog.term.Term;

import static jp.segfault.prolog.parser.Tokenizer.isTokenBoundary;

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

    private final ConsoleReader console = new ConsoleReader();

	public static void main(String[] args) throws Exception {
		State state = new State(System.in, System.out);

		// コマンドラインで指定されたファイルを読む
		for (String filename: args) {
			state.parse(new File(filename));
		}
		System.err.println("% Welcome to sF-Prolog!");
		System.err.println();

		new CLI(state).exec();
	}

	public CLI(final State state) throws IOException {
		this.state = state;
        console.addCompleter(new Completer() {

            @Override
            public int complete(String s, int i, List<CharSequence> candidates) {
                final int n = s.length();
                if (n == 0) {
                    return -1;
                }
                int base = i;
                if (i == n || isTokenBoundary(s.charAt(i))) {
                    base--;
                }
                if (base < 0) {
                    return -1;
                }
                int lhs = base;
                int rhs = base;
                while (lhs-1 > -1 && !isTokenBoundary(s.charAt(lhs-1), s.charAt(lhs))) lhs--;
                while (rhs+1 <  n && !isTokenBoundary(s.charAt(rhs+1), s.charAt(rhs))) rhs++;

                ++rhs;
                String prefix = s.substring(lhs, i);
                String suffix = s.substring(i, rhs);
                for (Predicate e: state.predicates()) {
                    String o = e.id;
                    if (o.startsWith(prefix) && o.endsWith(suffix)) {
                        candidates.add(o.substring(0, o.length() - suffix.length()));
                    }
                }
                return candidates.isEmpty() ? -1: lhs;
            }
        });
	}

	public void exec() throws Exception {
		PipedWriter writer = new PipedWriter();
		PipedReader reader = new PipedReader();
		reader.connect(writer);
		TermParser<Term> parser = state.newParser(reader);
		for (int i = 0;; ) {
            console.setPrompt(String.format("%02d ?- ", ++i));
			for (int j = 0;; ) {
				String line = console.readLine();
				if (line == null) {
					break;
				}
				writer.write(line +"\n");
				try {
					Term term = parser.next();
					if (term == null) {
                        console.setPrompt(String.format("% 4d| ", ++j));
						continue;
					}
					do { ask(term); } while ((term = parser.next()) != null);
				} catch (ParseException e) {
                    console.print("Parse error: " + e.getMessage());
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
				if (answer == null) {
                    console.println("fail");
					return;
				}
				for (int i = 0; i < answer.size(); ++i) {
					if (answer.get(i) != null) {
                        console.println(coding.getOriginalName(i) + " = " + answer.get(i).toString(state));
					}
				}
				if (!query.canBacktrack()) {
					break;
				}
			} while (console.readLine("ok? [y/N]").indexOf("y") == -1);
            console.println("true");
		} catch (QueryException e) {
			e.printStackTrace();
			// state.out.println("エラー:"+ e.getMessage());
		}
	}

}
