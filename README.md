# sfprolog

 ふつうのPrologのインタープリターです。Javaのコードから呼び出すためのAPIを備えています。


REPL
-------

```sh
java -jar prolog.jar
```
これでインタラクティブシェルが起動します。引数にファイル名を与えると、Prologスクリプトとして開いて、述語の定義をロードします。

Javaからの呼び出し
-------

### クエリーの実行

```Java
import jp.segfault.prolog.*;

public class A {
    public static void main(String[] args) {
        State state = new State(System.in, System.out);
        Query query = state.query("write(%msg).", "Hello, World!");
        query.ask(); // output: Hello, World!
    }
}
```

### 組み込み述語の作成

```Java
import jp.segfault.prolog.*;
import jp.segfault.prolog.procedure.*;
import jp.segfault.prolog.term.*;

public class Exports extends Foreign {
    @Declaration("rand_int/1")
    public static final Foreign RAND_INT = new Foreign() {
        final Random generator = new Random();
        @Override
        protected Code call0(Query query, Term... args) {
            return UNIFY.call(query, args[0], Term.valueOf(generator.nextInt()));
        }
    };
}
```


TODO
-------

 - GUIとデバッガーの開発
 - ISO Prologへの準拠
   - 浮動小数点数のサポート
   - 名前空間(モジュール)
   - 未実装のISO標準述語(unify_with_occurs_check/2, acyclic_term/1など)
 - 処理速度をもうちょっとマシに
 - APIの改善
   - 組み込みの述語の作成をもっと簡単に！
   - Scala向けのインターフェイス

Version
-------
1.0.0

License
-------
MIT
