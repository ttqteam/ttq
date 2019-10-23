package ttq.tokenizer

import java.time.LocalDateTime

import com.typesafe.scalalogging.Logger
import org.springframework.util.StopWatch
import ttq.common.Probable
import ttq.tokenizer.CombinatorialUtils.{generateAllCombinations, getIndexesOfAllSeqSubsetsWithLenUpToK, splitListToSegments}

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

class Tokenizer(knowledgeBase: KnowledgeBase) {
  private val logger = Logger[Tokenizer]

  val acceptanceBoundDefault = 0.5
  val acceptanceBoundCurToken = 0.1
  val numOfEntryTokensInReplacementList = 6

  def parseUserInput(input: List[Token], curToken: Int = -1): List[Probable[List[Token]]] = {
    if (input.isEmpty) return List()

    val stopwatch = new StopWatch(s"Tokenizer")
    stopwatch.start("find segmentReplacements")

    /*
    * Split input to word & !word segments
    * For each word segment find all replacements
    */
    val segmentReplacements = splitListToSegments[Token](input, x => x.isInstanceOf[WordToken])
      .map(
        x => if (x._2.head.isInstanceOf[WordToken]) {
          findAllReplacementsForWordSegment(x._2.asInstanceOf[List[WordToken]], x._1, curToken)
        } else {
          List(Probable(x._2, 1))
        }
      )
      .filter(_.nonEmpty)

    stopwatch.stop()
    stopwatch.start("generateAllCombinations")

    /*
    * Generate and sort all results from segments replacement options
    * */
    val res = generateAllCombinations[Probable[List[Token]]](segmentReplacements)
      .map(parseResult => parseResult.reduce((x , y) => Probable(x.value:::y.value, x.probability + y.probability)))
      .sortBy(res => - res.probability)
      .distinct

    stopwatch.stop()

    logger.info(stopwatch.prettyPrint())

    res
  }

  private def findAllReplacementsForWordSegment(
    segment: List[WordToken],
    curTokenIndexOffset: Int = 0,
    curToken: Int = -1
  ): List[Probable[List[Token]]] = {
    val stopwatch = new StopWatch(s"Tokenizer findAllReplacementsForWordSegment")

    stopwatch.start("getIndexesOfAllSeqSubsetsWithLenUpToK")

    val listOfSubsegmentIndexes: List[(Int, Int)] = getIndexesOfAllSeqSubsetsWithLenUpToK(
      segment.size,
      knowledgeBase.maxTokenLen
    )
    stopwatch.stop()

    stopwatch.start("sub segment Replacements")
    /*
    * Map index of segment start/end to the list of tokens it could be replaced with probability.
    * We need this step to calculate replacements for each subSegment only once
    * */
    val subsegmentReplacements: Map[(Int, Int), List[Probable[Token]]] = listOfSubsegmentIndexes
      .map(index => (index, segment.slice(index._1, index._2)))
      .map(pair => (pair._1, findReplacementsOfSegmentToToken(pair._2, getAcceptanceBound(curTokenIndexOffset, pair._1, curToken))))
      .toMap

    stopwatch.stop()

    //at index i store results for [0, i] segment
    var partialResult: ArrayBuffer[List[Probable[List[Token]]]] = new ArrayBuffer[List[Probable[List[Token]]]]()
    for (ind <- segment.indices) {
      val curSegmentRes = new ArrayBuffer[Probable[List[Token]]]()

      var offset = 0
      //todo: this could be done in parallel
      //todo: may be calculate here best tokenization (so tokenizer will return List[Probable[Token]] not List[List[Probable[Token]]])
      //todo: add memoization?
      while (ind - offset >= 0 && offset <= knowledgeBase.maxTokenLen - 1) {
        val leftBound = ind - offset
        val curIntervalReplacements: List[Probable[Token]] = subsegmentReplacements.getOrElse((leftBound, ind + 1), List())
        val prevSegmentReplacements: List[Probable[List[Token]]] = if (leftBound > 0) partialResult(leftBound - 1) else List()

        if (prevSegmentReplacements.isEmpty) {
          curIntervalReplacements.foreach(x => curSegmentRes += Probable(List(x.value), x.probability * x.probability))
        } else if (curIntervalReplacements.isEmpty) {
          curSegmentRes ++= prevSegmentReplacements
        } else {
          curSegmentRes ++= curIntervalReplacements
            .flatMap(curRepl => prevSegmentReplacements
              .map(
                prevSegmRepl =>
                  if (prevSegmRepl.value.last != curRepl.value)
                    Probable(
                      prevSegmRepl.value:::List(curRepl.value),
                      prevSegmRepl.probability + curRepl.probability * curRepl.probability
                    )
                  else {
                    prevSegmRepl
                  }
              )
            )
        }

        offset += 1
      }

      partialResult += curSegmentRes.toList.sortBy(x => -x.probability).distinct.take(20)
    }

    partialResult(segment.size - 1)
  }

  private def getSumProbability(list: List[Probable[Token]]): Probable[List[Token]] = {
    // p^2 to give better weight to few well-matching tokens compared to many ill-matched
    Probable(list.map(x => x.value), list.map(x => Math.pow(x.probability, 2)).sum)
  }

  //todo: cache ?
  def findReplacementsOfSegmentToToken(
                                        segment: List[WordToken],
                                        acceptanceBound: Double
                                      ): List[Probable[Token]] = {
    val txtSegment = segment.map(x => x.name).reduce((x, y) => s"$x $y")
    val segmentNgrams = NGramUtils.stringToNgrams(txtSegment)
    //find tokens which contains this ngrams
    val replacementOptions: List[TokenWithAlias] = segmentNgrams
      .flatMap(ngram => knowledgeBase.getTokensByNgram(ngram))
      .distinct
    //calc probabilities, filter
    val tokens = replacementOptions
      .map(tokenWithAlias => (tokenWithAlias.token, getReplacementProbability(tokenWithAlias.alias, txtSegment)))
      .filter(tokenProb => tokenProb._2 > acceptanceBound)
      .map(tokenProb => Probable(tokenProb._1, tokenProb._2))
      .sortBy(x => -x.probability)
      .distinct

    //decrease number of entry tokens in replacements list
    val filteredTokens = tokens
      .filter(token => token.value.isInstanceOf[StringEntityToken])
      .take(numOfEntryTokensInReplacementList) ::: tokens.filter(token => !token.value.isInstanceOf[StringEntityToken])

    val dates = DateParser.parse(txtSegment, LocalDateTime.now).map(d => Probable(DateToken(d),1.0)).toList

    val numbers = NumberParser.parse(txtSegment).map(n => Probable(NumberToken(n), 1.0)).toList

    numbers ::: dates ::: filteredTokens
  }

  def getReplacementProbability(tokenTxt: String, txt: String): Double = {
    val txtNgrams = NGramUtils.stringToNgrams(txt)
    val tokenNgrams = NGramUtils.stringToNgrams(tokenTxt)
    val ngramUnion = (txtNgrams:::tokenNgrams).distinct
    val tokenNgramsProbVector = ngramUnion.map(ngram => tokenNgrams.contains(ngram)).map(x => if (x) 1.0 else 0)
    val txtNgramsProbVector = ngramUnion.map(ngram => txtNgrams.contains(ngram)).map(x => if (x) 1.0 else 0)
    cosineDist(tokenNgramsProbVector, txtNgramsProbVector)
  }

  private def getAcceptanceBound(offset: Int, segIndexes: (Int, Int), curTokenInd: Int): Double = {
    if (segIndexes._1 + offset == curTokenInd && segIndexes._2 + offset - 1 == curTokenInd)
      acceptanceBoundCurToken
    else
      acceptanceBoundDefault
  }

  /*
  * https://en.wikipedia.org/wiki/Cosine_similarity
  * */
  private def cosineDist(v1: List[Double], v2: List[Double]): Double = {
    v1.zip(v2).map(x => x._1 * x._2).sum / (Math.sqrt(v1.map(x => x * x).sum) * Math.sqrt(v2.map(x => x * x).sum))
  }
}

