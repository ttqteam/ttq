package ttq.lf

import scala.util.{Success, Try}

object ConditionValidation {
  sealed trait Condition
  case object AND extends Condition
  case object OR extends Condition

  /**
    * @throws IllegalStateException if conditions are inconsistent
  * */
  def validate(left: FilterExpr, right: FilterExpr, cond: Condition): Try[Unit] = {
    if (cond == AND) validateAnd(left, right) else validateOr(left, right)
  }

  private def validateAnd(left: FilterExpr, right: FilterExpr): Try[Unit] = Try(
    //date, date
    //date, last month
    //number, number
    //entry, entry +
    (left, right) match {
      case (
        DimensionEqualsEntityExpr(Some(dimRefLeft), Some(strEntityRefLeft)),
        DimensionEqualsEntityExpr(Some(dimRefRight), Some(strEntityRefRight))
        ) if dimRefLeft == dimRefRight && strEntityRefLeft != strEntityRefRight =>
            throw new IllegalStateException("And condition expression contains different entities for one dinRef")

      case _ => //todo
    }
  )

  private def validateOr(left: FilterExpr, right: FilterExpr): Try[Unit] = Try(()=>Unit) //todo

  /**
    * @return None if conditions are inconsistent
    * */
  def reduceCondition(left: FilterExpr, right: FilterExpr, condition: Condition): Option[FilterExpr] = {
    if (validate(left, right, condition).isFailure) {
      return Option.empty
    }

    //last month, last month
    //last month/ date
    //date, date
    //entry, entry +
    if (left.isInstanceOf[LastMonthExpr] && right.isInstanceOf[LastMonthExpr]) {
      return Option(left)
    }

    (left, right) match {
      case (
        DimensionEqualsEntityExpr(Some(dimRefLeft), Some(strEntityRefLeft)),
        DimensionEqualsEntityExpr(Some(dimRefRight), Some(strEntityRefRight))
        ) if dimRefLeft == dimRefRight && strEntityRefLeft == strEntityRefRight =>
         return Option(left)
      case _ => //todo
    }

    if (condition == AND) Option(AndExpr(left, right)) else Option(OrExpr(left, right))
  }
}
