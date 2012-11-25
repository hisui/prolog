%%
%% 演算子の定義
%%

% (^_^;)
:-( op(1200, fx, :-) ).

:- op(1200, xfx, ':-').
:- op(1200, xfx,  -->).
:- op(1200,  fx,   ?-).
:- op(1100, xfy,    ;).
:- op(1050, xfx,   ->).
:- op(1000, xfy,    ,).
:- op( 900,  fy,   \+).
:- op( 700, xfx,   is).
:- op( 700, xfx,    <).
:- op( 700, xfx,    >).
:- op( 700, xfx,   =<).
:- op( 700, xfx,   >=).
:- op( 700, xfx,  =:=).
:- op( 700, xfx,  =\=).
:- op( 700, xfx,    =).
:- op( 700, xfx,  =..).
:- op( 700, xfx,   @<).
:- op( 700, xfx,   @>).
:- op( 700, xfx,  @>=).
:- op( 700, xfx,  @=<).
:- op( 700, xfx,   ==).
:- op( 700, xfx,  \==).
:- op( 500, yfx,    +).
:- op( 500, yfx,    -).
:- op( 500, yfx,   /\).
:- op( 500, yfx,   \/).
:- op( 400, yfx,    *).
:- op( 400, yfx,    /).
:- op( 400, yfx,   //).
:- op( 400, yfx,  mod).
:- op( 400, yfx,  rem).
:- op( 400, yfx,   <<).
:- op( 400, yfx,   >>).
:- op( 200, xfx,   **).
:- op( 200,  fy,    -).
:- op( 200, yfx,    .).
:- op( 200, xfy,    ^).
:- op(  50, xfx,    :).


%%
%% インタープリタの設定
%%

:- set_prolog_flag(trace_unification, no).
:- set_prolog_flag(trace_tokenizer,   no).
:- set_prolog_flag(trace_call_frame,  no).
:- set_prolog_flag(benchmark_parse,  yes).


%%
%% システム
%%

halt :- halt(0).
,    :- halt.

listing :-
	current_predicate(Pred),
	  write(Pred),
	listing(Pred), nl, fail; true.

[File| Files] :- consult(File), Files.
[].


%%
%% ストリーム
%%

write(Term) :- write_term(Term, []).
write_term(Term, Options) :-
	current_output(Out), write_term(Out, Term, Options).

display(Term) :- current_output(Out), display(Out, Term).

read(    Term) :- read_term(    Term, []).
read(In, Term) :- read_term(In, Term, []).
read_term(Term, Options) :-
	current_input(In), read_term(In, Term, Options).

put_char(Char) :- current_output(Out), put_char(Out, Char).
get_char(Char) :- current_input(In)  , get_char(In, Char).

nl :- put_char('\n').


%%
%% 制御処理
%%

\+ A :- A, !, fail; true.

true(X) :- X; true.
once(X) :- X, !.

repeat.
repeat :- repeat.

repeat(0) :- !.
repeat(N).
repeat(N) :- N1 is N - 1, repeat(N1).

setof(Tp, X1, Bag) :- bagof(Tp, X1, L), uniq(L, Bag).
bagof(Tp, X1, Bag) :-
	
	% 自由変数を列挙
	foldl(A^B^C^(B^A=C, var(B); B=C), X2, VarsA, X1),
	term_variables(X2, VarsB),
	term_variables(Tp, VarsC),
	append(VarsA, VarsC, VarsD),
	uniq(VarsD, Vd),
	uniq(VarsB, Vb),
	bagof_diff(Vb, Vd, FreeVars),

	% findallで先ずすべて解を集める
	findall(FreeVars:Tp, X2, O1),
	term_sort(O1, O2),
	foldl([A:A2|T]
		^ B:B2 ^ C
		^ (A==B -> C=[B:[B2|A2]  |T]
		         ; C=[B:[B2],A:A2|T]), [_:[]], O2, O3),
	!,
	
	% ひとつひとつ取り出す
	member(FreeVars:Bag, O3), Bag \== [].

bagof_diff([],  _, []) :- !.
bagof_diff(Xs, [], Xs) :- !.
bagof_diff([X|Xs], [Y|Ys], [X|Z]) :- X @< Y, !, bagof_diff(Xs, [Y|Ys], Z).
bagof_diff([X|Xs], [Y|Ys], Z) :-
	X == Y -> bagof_diff(   Xs , Ys, Z)
	        ; bagof_diff([X|Xs], Ys, Z).

forall(X, Y) :- \+ (X, \+Y).

% findall(E, member(E, [1,2,3,4]), E mod 2 =:= 0, A, B).
findall(Tpl, X, Y, A, B) :-
	findall(Tpl, (X,   Y), A),
	findall(Tpl, (X, \+Y), B).


%%
%% 型チェック
%%

nonvar(A) :- \+ var(A).
callable(A) :- atom(A); compound(A).


%%
%% 項の比較
%%

A @>  B :- term_compare(A, B, C), C > 0.
A @<  B :- term_compare(A, B, C), C < 0.
A @>= B :- term_compare(A, B, C), C >= 0.
A @=< B :- term_compare(A, B, C), C =< 0.
A  == B :-    term_compare(A, B, 0).
A \== B :- \+ term_compare(A, B, 0).


%%
%% 算術
%%

A > B :- \+ (A < B; A =:= B).

A >= B :- \+ A < B.
A =< B :- \+ A > B.

A =\= B :- \+ A =:= B.


%%
%% リスト操作
%%

append([], Right, Right).
append([H|T], Right, [H|List]) :- append(T, Right, List).

member(E, [E|T]).
member(E, [H|T]) :- member(E, T).

% これだとAがインスタンスじゃないと無限ループ起こす(´・ω・｀)
%reverse(A, B) :- reverse(A, B, []).
%reverse([], B, B).
%reverse([H|T1], B, T2) :- reverse(T1, B, [H|T2]).

reverse(A, B) :- reverse(A, B, B, []).
reverse([], [], B, B).
reverse([H1|T1], [H2|T2], B, T3) :- reverse(T1, T2, B, [H1|T3]).

select(E, [E|T], T).
select(E, [H|T], [H|List]) :- select(E, T, List).

take(0,     _, []) :- !.
take(N, [H|T], [H|Dest]) :- N1 is N - 1, take(N1, T, Dest).

zip([X|XS], [Y|YS], [(X,Y)|T]) :- !, zip(XS, YS, T).
zip(_, _, []).

length(List, N) :- length0(List, N, 0).

length0(   [], N, N) :- !.
length0([H|T], X, N) :- N1 is N + 1, length0(T, X, N1).

for(Init0, X, Last0) :-
	Init is Init0, integer(Init),
	Last is Last0, integer(Last),
	(Init =< Last, !
		, for0_inc(X, Init, Last)
		; for0_dec(X, Init, Last)).

for0_inc(A, A, A) :- !.
for0_inc(X, A, B) :- X = A; A1 is A + 1, for0_inc(X, A1, B).
for0_dec(A, A, A) :- !.
for0_dec(X, A, B) :- X = A; A1 is A - 1, for0_dec(X, A1, B).

range0(A, A, [])    :- !.
range0(A, B, [A|T]) :- !, A1 is A + 1, range0(A1, B, T).
range(A0, B0, List) :-
	A is A0, B is B0,
	integer(A),
	integer(B),
	A =< B,
	range0(A, B, List).

uniq(List, UniqList) :- term_sort(List, SortedList), uniq0(SortedList, UniqList).
uniq0([], []) :- !.
uniq0([H], [H]) :- !.
uniq0([A,B|T], L1) :-
	A == B -> uniq0([A|T], L1)
	        ; uniq0([B|T], L0), L1 = [A|L0].

sublist([], []).
sublist([H|T], T2) :- sublist(T, T1), (T2 = [H|T1]; T2 = T1).

power(List0, List) :-
	nonvar(List0), findall(Sub, sublist(List0, Sub), List).

flatten(XS, YS) :-
	flatten0(XS, [], YS).

flatten0(H, T, [H|T]) :- var(H), !.

flatten0([H|T], Succ, List) :- !,
	flatten0(T, Succ, Temp),
	flatten0(H, Temp, List).

flatten0([], T,    T ) :- !.
flatten0( H, T, [H|T]).

sub_term(Y, X) :- var(X), !, X = Y.
sub_term(X, X).
sub_term(Z, X) :- X =.. [_|T], member(Y, T), sub_term(Y, Z).


%%
%% lambda
%%

apply(X, Args1) :- 
	X =.. [Name|Args0],
	append(Args0, Args1, Args2), !,
	Y =.. [Name|Args2],
	call(Y).

call(X,A)         :- apply(X, [A]).
call(X,A,B)       :- apply(X, [A,B]).
call(X,A,B,C)     :- apply(X, [A,B,C]).
call(X,A,B,C,D)   :- apply(X, [A,B,C,D]).
call(X,A,B,C,D,E) :- apply(X, [A,B,C,D,E]).

'^'(A1, A2^A3^A4^A5^X, B1,B2,B3,B4,B5) :- copy_term(A1^A2^A3^A4^A5^X, B1^B2^B3^B4^B5^Y), call(Y).
'^'(A1,    A2^A3^A4^X, B1,B2,B3,B4)    :- copy_term(   A1^A2^A3^A4^X,    B1^B2^B3^B4^Y), call(Y).
'^'(A1,       A2^A3^X, B1,B2,B3)       :- copy_term(      A1^A2^A3^X,       B1^B2^B3^Y), call(Y).
'^'(A1,          A2^X, B1,B2)          :- copy_term(         A1^A2^X,          B1^B2^Y), call(Y).
'^'(A1,             X, B1)             :- copy_term(            A1^X,             B1^Y), call(Y).

partition(_, [], [], [])  :- !.
partition(F, [H|T], A, B) :-
	partition(F, T, A_, B_),
	(call(F, H) -> A = [H|A_], B = B_
	             ; B = [H|B_], A = A_).

quicksort(_, [], []) :- !.
quicksort(Less, [H|T], Sorted) :-
	partition(X^call(Less, X), T, G, L),
	quicksort(Less, G, G_),
	quicksort(Less, L, L_),
	append(L_, [H|G_], Sorted).

merge(_, Xs, [], Xs) :- !.
merge(_, [], Ys, Ys) :- !.
merge(Less, [X|Xs], [Y|Ys], [H|T]) :-
	call(Less, X, Y) -> H = X, merge(Less, Xs, [Y|Ys], T)
	                  ; H = Y, merge(Less, Ys, [X|Xs], T).

sort(A, B, C) :- mergesort(A, B, C).

term_sort(Terms, Sorted) :- sort(@<, Terms, Sorted).

sorted(Less, [ ]) :- !.
sorted(Less, [X]) :- !.
sorted(Less, [X0,X1|Xs]) :- call(Less, X0, X1), sorted(Less, [X1|Xs]).

listlist([], []) :- !.
listlist([H|T0], [[H]|T1]) :- listlist(T0, T1).

mergesort0(Less, [ ], [Y], Y) :- !.
mergesort0(Less, [ ], Ys, Zs) :- !, mergesort0(Less, Ys,     [], Zs).
mergesort0(Less, [X], Ys, Zs) :- !, mergesort0(Less, [], [X|Ys], Zs).
mergesort0(Less, [X0,X1|Xs], Ys, Zs) :-
	merge(Less, X0, X1, Y),
	mergesort0(Less, Xs, [Y|Ys], Zs).

mergesort(   _, [], []) :- !.
mergesort(Less, L0, L2) :- listlist(L0, L1), mergesort0(Less, L1, [], L2).

map( _, [], []) :- !.
map(Fn, [X|XS], [Y|YS]) :- call(Fn, X, Y), map(Fn, XS, YS).

flatmap(Fn, XS, YS) :-
	map(Fn, XS, ZS),
	flatten(ZS, YS).

filter( _, [], []) :- !.
filter(Fn, [H|T], [H|T1]) :- call(Fn, H) -> filter(Fn, T, T1).
filter(Fn, [H|T],    T1 ) :-                filter(Fn, T, T1).

foldl(Fn, [H|T], V) :- foldl(Fn, H, T, V).

foldl(Fn, V0, L, V) :- var(L), !, unfoldl(Fn, V, L, V0, []).

foldl(Fn, V1, [H|T], V3) :- call(Fn, V1, H, V2), foldl(Fn, V2, T, V3).
foldl( _, V1,    [], V1) :- !.

unfoldl(Fn, V1, L, V0, T) :-
	call(Fn, V, E, V1),
	(var(V) -> L = T, V0 = E
	         ; unfoldl(Fn, V, L, V0, [E|T])).

foldr(Fn,   [V], V) :- !.
foldr(Fn, [H|T], V) :- foldr(Fn, T, V0), call(Fn, V0, H, V).

foldr( _, V1,    [], V1) :- !.
foldr(Fn, V1, [H|T], V3) :- foldr(Fn, V1, T, V2), call(Fn, V2, H, V3).


%%
%% 連想配列
%%

keysort(AssocList, Sorted) :-
	sort((A-_)^(B-_)^(A @< B), AssocList, Sorted).

keyget([Key-Val|_], Key, Val) :- !.
keyget([      _|T], Key, Val) :- keyget(T, Key, Val).

keyset([Key-Val|T], Old, [H|T], Key, Val) :- H = (Key-_) -> H = (_-Old), !.
keyset([      H|U], Old, [H|T], Key, Val) :- !, keyset(U, Old, T, Key, Val).
keyset([Key-Val], _, [], Key, Val) :- !.


%%
%% 他のモジュールを読み込む
%%

% :- jvm_load_foreign('MyProcedureList', ['./']).
