package ttq.common

case class Probable[+T](value: T, probability: Double) {
  override lazy val toString: String = f"[$probability%.2f, $value]"

  override lazy val hashCode: Int = value.hashCode()

  def canEqual(other: Any): Boolean = other.isInstanceOf[Probable[T]]

  override def equals(other: Any): Boolean = other match {
    case that: Probable[T] =>
      (that canEqual this) &&
        value.equals(that.value)
    case _ => false
  }
}
