package jp.segfault.prolog.code;

/**
 * 成功または失敗します。
 * @author shun
 */
public class Result extends Code {

	public static final Result True = new Result("true");
	public static final Result Fail = new Result("fail");
	
	private String name;
	
	private Result(String name) {
		this.name = name;
	}
	
	public static Result valueOf(boolean b) {
		return b ? True: Fail;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public <T> T accept(Visitor<T> visitor) {
		return visitor.visit(this);
	}
}
