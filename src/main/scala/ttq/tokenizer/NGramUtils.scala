package ttq.tokenizer

object NGramUtils {
  /*
  * Create list of NGrams according to https://ii.nlm.nih.gov/MTI/Details/trigram.shtml
  * Add additional ngram s"{lastNgram}$"
  * (e.g. for token "ccy pair" token "ccy pair block" should not be 100% equal)
  * */
  def stringToNgrams(str: String): List[String] = {
    val terms = str.toLowerCase().split(" ").toList
    val nGramsByTerms = terms.flatMap(term => nGramsByTerm(term))
    boundNgrams(str.toLowerCase()):::nGramsByTerms:::nGramsByAdjucementTerms(terms)
  }

  private def nGramsByTerm(term: String): List[String] = {
    if (term.length == 0) return List()
    val res = if (term.length < 4) {
      List(term)
    } else {
      (0 to term.length - 3).map(ind => term.substring(ind, ind + 3)).toList
    }
    term.charAt(0) + "#" :: res
  }

  private def nGramsByAdjucementTerms(terms: List[String]): List[String] = {
    if (terms.size < 2) return List()
    val nonEmptyTerms = terms.filter(x => x.length > 0)
    nonEmptyTerms.head.charAt(0) + " " + nonEmptyTerms(1).charAt(0) :: nGramsByAdjucementTerms(nonEmptyTerms.tail)
  }

  private def boundNgrams(str: String): List[String] = {
    if (str.isEmpty) return Nil
    if (str.length < 3) return List("^" + str, str + "$")
    "^" + str.substring(0, 2) :: "^" + str.substring(0, 3) :: str.substring(str.length - 3) + "$" :: Nil
  }
}
