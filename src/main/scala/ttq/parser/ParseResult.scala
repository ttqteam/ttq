package ttq.parser

case class ParseResult(parseTree: TreeNode, expression: Any) {
  override def toString: String = s"expression: $expression, tree: $parseTree"
}
