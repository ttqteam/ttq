package ttq.tokenizer

object CombinatorialUtils {
  type SegmentWithStartIndex[T] = (Int, List[T])

  def splitListToSegments[T](list: List[T], cond: T => Boolean): List[SegmentWithStartIndex[T]] = {
    if (list.isEmpty) {
      return List()
    }
    splitListToSegments(list.tail, (0, List(list.head)), cond)
  }

  private def splitListToSegments[T](
    list: List[T],
    lastSegment: SegmentWithStartIndex[T],
    cond: T => Boolean
  ): List[SegmentWithStartIndex[T]] = {
    if (list.isEmpty) {
      return List(lastSegment)
    }
    if (cond(list.head) == cond(lastSegment._2.head)) {
      splitListToSegments(list.tail, (lastSegment._1, lastSegment._2 ::: List(list.head)), cond)
    } else {
      List(lastSegment):::splitListToSegments(list.tail, (lastSegment._1 + lastSegment._2.size, List(list.head)), cond)
    }
  }

  /*
  * e.g. for listSize = 3, k = 3 result should be
  * ((0, 1), (1, 2), (2, 3), (0, 2), (1, 3), (0, 3))
  * */
  def getIndexesOfAllSeqSubsetsWithLenUpToK(listSize: Int, k: Int): List[(Int, Int)] = {
    var res = List[(Int, Int)]()
    for (segLen <- 1 to Math.min(k, listSize)) {
      for (ind <- 0 to listSize - segLen) {
        res = (ind, ind + segLen)::res
      }
    }
    res
  }

  /*
  * e.g. for in: (("a", "b"), ("c", "d"), ("e"))
  * result should be (("a", "c", "e"), ("a", "d", "e"), ("b", "c, "e""), ("b", "d", "e"))
  * */
  def generateAllCombinations[T](in: List[List[T]]): List[List[T]] = {
    if (in.nonEmpty)
      in.filter(list => list.nonEmpty).foldLeft(List[List[T]](List()))((x: List[List[T]], y: List[T]) => x.flatMap(e1 => y.map(e2 => e1:::List(e2))))
    else
      List.empty
  }

  /*
  * e.g. for list = (a, b, c), k = 3 result should be
  * (((0, 1), (1, 2), (2, 3))
  * ((0, 1), (1, 3))
  * ((0, 2), (2, 3))
  * ((0, 3)))
  * */
  def splitToSubsetsWithLenUpToK(listLen: Int, k: Int): List[List[(Int, Int)]] = {
    splitToSubsetsWithLenUpToK((0, listLen), k, List[(Int, Int)]())
  }

  private def splitToSubsetsWithLenUpToK(
    segBoundaries: (Int, Int),
    k: Int,
    curSplit: List[(Int, Int)]
  ): List[List[(Int, Int)]] = {
    if (segBoundaries._1 >= segBoundaries._2) {
      return List(curSplit)
    }

    var res = List[List[(Int, Int)]]()
    for (i <- segBoundaries._1 + 1 to Math.min(segBoundaries._1 + k, segBoundaries._2)) {
      res = res:::splitToSubsetsWithLenUpToK((i, segBoundaries._2), k, curSplit:::List((segBoundaries._1, i)))
    }
    res
  }
}
