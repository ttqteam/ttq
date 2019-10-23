package ttq.parser

import com.typesafe.scalalogging.Logger
import ttq.common.{CancellationToken, Probable}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

trait Node
class NonTerminalNode(symbol: NonTerminal) extends Node {
  val children: ArrayBuffer[Node] = new ArrayBuffer[Node]()
}
case class TerminalNode(symbol: Terminal) extends Node

class PdaParser[TToken, TLeaf](grammar: Grammar[TToken, TLeaf]) {
  private val logger = Logger[PdaParser[TToken,TLeaf]]

  private val rules = grammar.rules.groupBy(_.lhs)

  // Tree parent-poining nodes to store temporary parsing results
  private trait ParsedNode {
    def parent: Option[ParsedNode]
  }
  private class ParsedRuleNode(val rule: Rule, val parent: Option[ParsedRuleNode]) extends ParsedNode
  private class ParsedTokenNode(val token: TLeaf, val parent: Option[ParsedRuleNode]) extends ParsedNode

  // Tree parent-poining nodes to store temporary to-be-parsed symbols
  private case class StackedNode(symbol: Symbol, parentRule: Option[ParsedRuleNode])

  private case class State(
    tree: List[ParsedNode],
    str: List[TToken],
    stack: List[StackedNode],
    editDistance: Double)

  def parse(str: List[TToken], maxDistance: Double): List[Probable[ParseResult]] = {
    parse(str, maxDistance, CancellationToken.fake)
  }

  def parse(str: List[TToken], maxDistance: Double, cancellationToken: CancellationToken): List[Probable[ParseResult]] = {
    val result = new mutable.ArrayBuffer[State]
    val queue = new mutable.Queue[State]

    val rootNode = StackedNode(grammar.start, None)
    queue.enqueue(State(List(), str, List(rootNode), 0))
    // todo: simplify, recompose/refactor
    while (queue.nonEmpty) {
      cancellationToken.throwIfCancelled()

      val state = queue.dequeue()

      state.stack match {
        // no more symbols expected
        case Nil => state.str match {
          case Nil => result.append(State(state.tree, List(), List(), state.editDistance)) // matched
          case a :: strTail =>
            if (state.editDistance + grammar.deleteCost(a) <= maxDistance)
            // delete "extra" symbol
              queue.enqueue(State(state.tree, strTail, state.stack, state.editDistance + grammar.deleteCost(a)))
        }
        case h :: stackTail => {
          h.symbol match {
            case s: NonTerminal =>
              // reduce
              val rs = rules(s) // must be present
              rs.map(rule => {
                val ruleNode = new ParsedRuleNode(rule, h.parentRule)
                State(ruleNode::state.tree, state.str, rule.rhs.map(StackedNode(_, Some(ruleNode))) ++ stackTail, state.editDistance)
              }).foreach(queue.enqueue(_))
            case s: Terminal =>
              state.str match {
                case Nil =>
                  // shift
                  if (state.editDistance + grammar.insertCost(h.parentRule.get.rule, s) <= maxDistance)
                  // insert "missing" symbol
                    queue.enqueue(State(new ParsedTokenNode(grammar.terminalToLeaf(s), h.parentRule) :: state.tree, Nil, stackTail, state.editDistance + grammar.insertCost(h.parentRule.get.rule, s)))
                case a :: strTail =>
                  // shift
                  if (s == grammar.tokenToTerminal(a))
                    queue.enqueue(State(new ParsedTokenNode(grammar.tokenToLeaf(a),    h.parentRule)::state.tree, strTail,   stackTail,   state.editDistance))
                  else if (state.editDistance + grammar.replaceCost(h.parentRule.get.rule, a, s) <= maxDistance)
                    // substitute symbol
                    queue.enqueue(State(new ParsedTokenNode(grammar.terminalToLeaf(s), h.parentRule)::state.tree, strTail,   stackTail,   state.editDistance + grammar.replaceCost(h.parentRule.get.rule, a, s)))
                  if (state.editDistance + grammar.insertCost(h.parentRule.get.rule, s) <= maxDistance)
                  // insert "missing" symbol
                    queue.enqueue(State(new ParsedTokenNode(grammar.terminalToLeaf(s), h.parentRule)::state.tree, state.str, stackTail,   state.editDistance + grammar.insertCost(h.parentRule.get.rule,s)))
                  if (state.editDistance + grammar.deleteCost(a) <= maxDistance)
                  // delete "extra" symbol
                    queue.enqueue(State(state.tree, strTail, state.stack, state.editDistance + grammar.deleteCost(a)))
              }
          }
        }
      }
    }

    // if same tree obtained with different distances, take smallest distance
    val distinctTreesToDistance = result
      .groupBy(r => r.tree)
      .map(g => (g._1, g._2.map(_.editDistance).min))
      .toList

    // make resulting tree(s)
    val resultTrees = ListBuffer[Probable[ParseResult]]()
    for (treeToDistance <- distinctTreesToDistance) {
      val map = mutable.Map[ParsedNode, ListBuffer[ParsedNode]]() // temp map to accumulate children
      for (node <- treeToDistance._1) { // latest tokens iterated first here
        cancellationToken.throwIfCancelled()
        node.parent match {
          case None => ;
          case Some(parent) =>
            map.get(parent) match {
              case Some(children) => children.prepend(node) // latest tokens pushed to head
              case None => map(parent) = ListBuffer[ParsedNode](node)
            }
        }
      }
      val rootNode = map.keys.filter(_.parent.isEmpty).head // must be exactly one such element
      val tree = makeTree(map, rootNode)
      val kindOfProbability = 1.0 / (treeToDistance._2 + 1) // not real probability, just something which we can compare (+1 to avoid division by zero)
      resultTrees.append(Probable(ParseResult(tree._1, tree._2), kindOfProbability))
    }

    resultTrees.toList
  }

  private def makeTree(map: mutable.Map[ParsedNode, ListBuffer[ParsedNode]], root: ParsedNode): (TreeNode, Any) = {
    val childParseNodes = map.getOrElse(root, List())
    val children:List[(TreeNode, Any)] = childParseNodes.map(n => makeTree(map, n)).toList
    root match {
      case n:ParsedRuleNode => (SymbolNode(n.rule.lhs, children.map(_._1)), n.rule.semFn(children.map(_._2)))
      case n:ParsedTokenNode => (TokenNode(n.token, List()), n.token) // shall be no children for token node, and no expr to build
    }
  }
}
