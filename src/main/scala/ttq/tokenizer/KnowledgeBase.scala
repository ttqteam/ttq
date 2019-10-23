package ttq.tokenizer

import ttq.cache.EntryCache
import ttq.ontology.FinalOntology
import ttq.pipeline.Keywords
import ttq.tokenizer.NGramUtils.stringToNgrams

class KnowledgeBase(tokens: List[TokenWithAlias]) {
  def getTokensByNgram(ngram: String): Set[TokenWithAlias] = ngramsToTokens.getOrElse(ngram, Set())

  private val ngramsToTokens: Map[String, Set[TokenWithAlias]] = tokens
      .flatMap(token => stringToNgrams(token.alias).map(ngram => (ngram, token)))
      .groupBy(ngramToToken => ngramToToken._1)
      .map(ngramsToTokens => (ngramsToTokens._1, ngramsToTokens._2.map(pair => pair._2).toSet))

  val maxTokenLen: Int = if (tokens.nonEmpty)
    tokens.map(token => token.alias.split(" ").length).max
  else
    0
}

object KnowledgeBase {
  private val KeywordTokens = Keywords.list.map(keyword => TokenWithAlias(KeywordToken(keyword), keyword))

  def fromOntology(ontology: FinalOntology, entryCache: EntryCache): KnowledgeBase = {
    /*val measTokens = ontology.classes //todo: add after default functions handling ?
      .flatMap(ontClass => ontClass.measures.map(meas => MeasureToken(MeasureRef(meas.name, meas.className))))*/

    val factsTokens: List[TokenWithAlias] = ontology.classes
      .flatMap(
        ontClass => ontClass
          .facts
          .flatMap(
            factsRef => (factsRef.name::factsRef.synonyms)
              .map(alias => TokenWithAlias(FactsToken(factsRef), alias))
          )
      )

    val aggTokens: List[TokenWithAlias] = ontology.classes
      .flatMap(
        ontClass => ontClass
          .aggregates
            .flatMap(
              agg => (agg.name::agg.synonyms)
                .map(alias => TokenWithAlias(AggregateToken(agg), alias))
            )
      )

    val dimTokens = ontology.classes
      .flatMap(
        ontClass =>
          ontClass
            .dimensions
            .flatMap(
              dim => (dim.name::dim.synonyms)
                .map(alias => TokenWithAlias(DimensionToken(dim), alias))
            )
      )

    val entryTokens = entryCache.cache
      .flatMap(kvp => kvp._2.map(entry => TokenWithAlias(StringEntityToken(kvp._1, entry), entry)))
      .toList

    new KnowledgeBase(factsTokens:::aggTokens:::dimTokens:::entryTokens:::KeywordTokens)
  }
}

case class TokenWithAlias(token: Token, alias: String)