package ttq.common

sealed trait Operation
case object EQ extends Operation { override def toString: String = "=" }
case object GT extends Operation { override def toString: String = ">" }
case object LT extends Operation { override def toString: String = "<" }

