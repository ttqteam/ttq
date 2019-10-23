package ttq.parser

sealed trait Symbol
trait NonTerminal extends Symbol
trait Terminal extends Symbol
