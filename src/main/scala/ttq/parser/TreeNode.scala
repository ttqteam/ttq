package ttq.parser


trait TreeNode { def children: List[TreeNode] }

case class SymbolNode(symbol: Symbol, children: List[TreeNode]) extends TreeNode {
  override def toString: String = {
    if (children.isEmpty)
      symbol.toString
    else
      s"${symbol.toString}(${children.mkString(",")})"
  }
}


case class TokenNode[TToken](token: TToken, children: List[TreeNode]) extends TreeNode {
  override def toString: String = {
    token.toString
  }
}

