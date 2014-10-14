%%
%% DCG文法ルール定義用マクロ
%%
	
dcg_rule(Head0 --> Body0, Head1 :- Body1) :-
	Head0 =.. [X|XS],
	append(XS, [Pred, Succ], YS), !,
	Head1 =.. [X|YS],
	dcg_body(Body0, Body1, Pred, Succ).

dcg_body((X0;Y0), (X1,Succ=A;Y1,Succ=B), Pred, Succ) :- !,
	dcg_body(X0, X1, Pred, A),
	dcg_body(Y0, Y1, Pred, B).

dcg_body((X0,Y0), (X1,Y1), Pred, Succ) :- !,
	dcg_body(X0, X1, Pred, Mean),
	dcg_body(Y0, Y1, Mean, Succ).

dcg_body([X|XS], append([X|XS], Succ, Pred), Pred, Succ) :- !.
dcg_body([], Pred = [], Pred, Pred) :- !.

dcg_body({Term}, Term, Pred, Pred) :- !.

dcg_body(Term0, Term1, Pred, Succ) :-
	Term0 =.. [X|XS],
	append(XS, [Pred, Succ], YS), !,
	Term1 =.. [X|YS].

term_expansion(Head --> Body, Clause) :-
	write(here is Clause),
	dcg_rule(Head --> Body, Clause).
