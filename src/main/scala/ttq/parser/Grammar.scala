package ttq.parser

case class Rule(lhs: NonTerminal, rhs: List[Symbol], semFn: List[Any] => Any)

trait Grammar[TToken, TLeaf] {
  def rules: List[Rule]

  def start: NonTerminal

  /**
    * Grammar is defined in non-terminals and terminals, not in terms of tokens.
    * This function is used to map infinite number of token values to finite number of terminal symbols,
    * like DimensionToken(xxx) -> Dimension, DimensionToken(yyy) -> Dimension.
    * @param token
    *   Token to make terminal from.
    * @return
    *   Terminal symbol.
    */
  def tokenToTerminal(token: TToken): Terminal

  /**
    * This function is used to map tokens to leaf values in parse tree.
    * like DimensionToken(xxx) -> Some(DimensionToken(xxx))
    * @param token
    *   Token.
    * @return
    *   Leaf of the parse tree.
    */
  def tokenToLeaf(token: TToken): TLeaf

  /**
    * Grammar "fixes" errors within Levenstein distance and may generate terminal symbols as part of this process.
    * Parse tree contains TLeaf instances in leafs, and we can't put abstract Terminal there.
    * This function is used to generate default leaf values,
    * like Dimension -> None or Dimension -> DimensionPlaceholderToken.
    * @param symbol
    *   Terminal symbol to make default token for.
    * @return
    *   Token generated for the terminal.
    */
  def terminalToLeaf(symbol: Terminal): TLeaf

  /**
    * Grammar "fixes" errors within Levenstein distance by deleting symbols (tokens from input string).
    * @param symbol Terminal to be deleted
    * @return Cost of the operation, >= 0
    */
  def deleteCost(tokenToDelete: TToken): Double

  /**
    * Grammar "fixes" errors within Levenstein distance by inserting symbols.
    * @param rule Rule within which the symbol is to be inserted
    * @param symbolToInsert Terminal to be inserted.
    * @return Cost of the operation, >= 0
    */
  def insertCost(rule: Rule, symbolToInsert: Terminal): Double

  /**
    * Grammar "fixes" errors within Levenstein distance by replacing symbols (delete tokens from input stream, insert new guessed terminal).
    * @param rule Rule within which the symbol is to be replaced
    * @param tokenToDelete Token to be deleted
    * @param symbolToInsert Terminal to be inserted
    * @return Cost of the operation, >= 0deleted
    */
  def replaceCost(rule: Rule, tokenToDelete: TToken, symbolToInsert: Terminal): Double
}
