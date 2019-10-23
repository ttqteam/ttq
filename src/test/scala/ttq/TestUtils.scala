package ttq

import ttq.tokenizer.{Token, TokenWithAlias}

object TestUtils {
  def tokensToTokensWithAlias(tokens: Token*): List[TokenWithAlias] = tokens
    .toList
    .map(token => tokenizer.TokenWithAlias(token, token.name))
}
