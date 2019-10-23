package ttq.ontology

import ttq.ontology.Color.{BLACK, Color, GRAY, WHITE}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class GraphUtils[T] {
  private var nodesColor: mutable.Map[T, Color] = _
  private var result: ArrayBuffer[T] = _

  //todo: return full dependencies circle in exception msg?
  def topSort(graph: Map[T, List[T]]): List[T] = {
    nodesColor = collection.mutable.Map() ++ graph.map(entry => entry._1 -> WHITE)
    result = new ArrayBuffer[T]()

    graph.keys.foreach(node => topSort(graph, node))

    result.toList
  }

  private def topSort(graph: Map[T, List[T]], currentNode: T): Unit = {
    nodesColor(currentNode) match {
      case BLACK => return
      case GRAY => throw new IllegalStateException(s"Circular dependencies for $currentNode")
      case WHITE =>
    }

    nodesColor(currentNode) = incColor(nodesColor(currentNode))
    graph(currentNode).foreach(child => topSort(graph, child))
    nodesColor(currentNode) = incColor(nodesColor(currentNode))
    result += currentNode
  }

  private def incColor(color: Color): Color = {
    color match {
      case WHITE => GRAY
      case GRAY => BLACK
      case BLACK => throw new IllegalStateException(s"Tried to incColor for already black Node")
    }
  }
}

object Color extends Enumeration {
  type Color = Value
  val WHITE, GRAY, BLACK = Value
}