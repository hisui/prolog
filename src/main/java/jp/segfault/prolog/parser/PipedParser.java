package jp.segfault.prolog.parser;

import java.io.IOException;
import java.io.PipedReader;

/**
 * インクリメンタルにパースを行う非同期型のパーサーです。
 * @author shun
 */
public class PipedParser<TERM> implements TermParser<TERM> {
	
	private volatile Exception error;
	private volatile TERM term;
	private PipedReader reader;
	
	public PipedParser(final PipedReader reader
			, final TermFactory<TERM> factory, final OperatorTable optable)
	{
		this.reader = reader;
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					Parser<TERM> parser = new Parser<TERM>(reader, factory, optable);
					for(;;) {
						try {
							term = parser.next();
						} catch(ParseException e) {
							while(reader.ready()) {
								reader.skip(1);
							}
							parser = new Parser<TERM>(reader, factory, optable);
							error  = e;
						}
						PipedParser.this.yield();
					}
				} catch (IOException e) { error = e; }
				PipedParser.this.yield();
			}
		};
		thread.setDaemon(true);
		thread.start();
	}
	
	/**
	 * 次のProlog節の解析に到達していればTermを返し、そうでなければnullを返します。
	 */
	@Override
	public TERM next() throws IOException, ParseException {
		synchronized(reader) {
			yield();
			try {
				if(error instanceof    IOException) { throw (   IOException) error; }
				if(error instanceof ParseException) { throw (ParseException) error; }
				assert error == null;
				return term;
			}
			finally {
				error = null;
				term  = null;
			}
		}
	}
	
	private void yield() {
		synchronized(reader) {
			reader.notify();
			for(;;) {
				try {
					reader.wait();
					reader.notify();
					break;
				} catch(InterruptedException e) {}
			}
		}
	}
	
	@SuppressWarnings("unused")
	private static void trace(Object message) {
		System.err.println( Thread.currentThread().getName() +": "+ message );
	}

}
