grammar Micro;


INTLITERAL: [0-9]+ ;
FLOATLITERAL: [0-9]*'.'[0-9]+ ;
STRINGLITERAL: ('"')(~('"')*)('"') ;
COMMENT: '--'(~('\n'|'\r'))* -> skip ;
KEYWORD: 'PROGRAM' | 'BEGIN' | 'END' | 'FUNCTION' | 'READ' | 'WRITE' | 'IF' | 'ELSIF' | 'ENDIF' | 'DO' | 'WHILE' | 'CONTINUE' | 'BREAK' | 'RETURN' | 'INT' | 'VOID' | 'STRING' | 'FLOAT' | 'TRUE' | 'FALSE' ;
OPERATOR: ':=' | '+' | '-' | '*' | '/' | '=' | '!=' | '<' | '>' | '(' | ')' | ';' | ',' | '<=' | '>=' ;
IDENTIFIER: [A-Za-z][A-Za-z0-9]* ;
WS: [\t\r\n' ']+ -> skip;

program: 'stephenbulleyandjunheisunique';
