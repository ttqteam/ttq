package ttq.execution

import ttq.common._
import ttq.lf._

object PlanToSqlConverter {
  def planToSql(plan: Plan, modifiers: Seq[String]): SqlQuery = {
    val (select, project) = (plan.select, plan.project)
    val groupByColumns = getGroupBySqlColumns(project, modifiers)
    val sql = "select " +
      getSqlColumns(project, modifiers).mkString(", ") +
      " from " + select.tableName +
      getFilterSqlExpr(select).map(" where " + _).getOrElse("") +
      (if (groupByColumns.nonEmpty) " group by " + groupByColumns.mkString(", ") else "") +
      plan.select.orderBy.map(o => " order by " + o).getOrElse("") +
      plan.select.limit.map(l => " limit " + l).getOrElse("") +
      ";"
    if (project.isInstanceOf[AggregateProject] && groupByColumns.isEmpty)
      SqlQueryScalar(sql)
    else
      SqlQueryTable(sql)
  }

  private def getSqlColumns(project: Project, modifiers: Seq[String]): Seq[String] = project match {
    case AggregateProject(aggExpr, groupByExpr) =>  getGroupBySqlColumns(project, modifiers) ++ getAggExprSql(aggExpr)
    case DimensionProject(dimRefs) => dimRefs.map(_.sql)
  }

  private def getAggExprSql(aggregateExpr: AggregateExpr): Seq[String] = {
    aggregateExpr match {
      case userAggregateExpr: UserAggregateExpr =>
        val aggregate = userAggregateExpr.aggregate.get
        Seq(s"(${aggregate.sql})")
    }
  }

  private def getGroupBySqlColumns(project: Project, modifiers: Seq[String]): Seq[String] = project match {
    case AggregateProject(_, Some(groupByExpr)) => getGroupBySqlColumns(groupByExpr, modifiers)
    case _ => Seq()
  }

  def tryToApplyModifiers(dimensionRef: DimensionRef, modifiers: Seq[String]): String = {
    dimensionRef.dataType match {
      case DateType => tryToApplyDateModifiers(dimensionRef, modifiers)
      case _ => dimensionRef.sql
    }
  }

  def tryToApplyDateModifiers(dimensionRef: DimensionRef, modifiers: Seq[String]): String = {
    QueryModifierUtils.mapDateModifiers(modifiers).headOption match {
      case Some(ByDayQueryModifier) => dimensionRef.sql
      case Some(ByWeekQueryModifier) => s"toMonday(${dimensionRef.sql})"
      case Some(ByMonthQueryModifier) => s"toStartOfMonth(${dimensionRef.sql})"
      case Some(ByYearQueryModifier) => s"toStartOfYear(${dimensionRef.sql})"
      case None => dimensionRef.sql
    }
  }

  private def getGroupBySqlColumns(groupByExpr: GroupByExpr, modifiers: Seq[String]): Seq[String] = {
    groupByExpr match {
      case groupByDimensionExpr: GroupByDimensionExpr =>
        val dimensionRef = groupByDimensionExpr.dimension.get
        val groupBySql = tryToApplyModifiers(dimensionRef, modifiers)
        Seq(s"($groupBySql)")
    }
  }

  private def getFilterSqlExpr(select: Select): Option[String] = select.filterExpr match {
    case Some(filterExpr) => Some(getFilterSqlExpr(filterExpr))
    case _ => None
  }

  private def getFilterSqlExpr(filterExpr: FilterExpr): String = {
    filterExpr match {
      case OrExpr(left, right) => s"(${getFilterSqlExpr(left)} OR ${getFilterSqlExpr(right)})"
      case AndExpr(left, right) => s"(${getFilterSqlExpr(left)} AND ${getFilterSqlExpr(right)})"
      case dimensionEqualsEntityExpr: DimensionEqualsEntityExpr =>
        val dimensionRef = dimensionEqualsEntityExpr.dimension.get
        val entityRef = dimensionEqualsEntityExpr.entity.get
        s"(${dimensionRef.sql}) = ${getSqlRepr(entityRef)}"
      case dimensionOpNumberExpr: DimensionOpNumberExpr =>
        val dimensionRef = dimensionOpNumberExpr.dimension.get
        val entityRef = dimensionOpNumberExpr.entity.get
        val op = dimensionOpNumberExpr.operation
        s"(${dimensionRef.sql}) ${opToStr(op.get)} ${getSqlRepr(entityRef)}"
      case filterByDateExpr: FilterByDateExpr =>
        val dimensionRef = filterByDateExpr.dimension.get
        val entityRef = filterByDateExpr.fromDate.get
        s"(${dimensionRef.sql}) > '${getSqlRepr(entityRef)}'"
      case booleanDimensionExpr: BooleanDimensionExpr =>
        val dimensionRef = booleanDimensionExpr.dimension.get
        s"(${dimensionRef.sql})"
      case expr: LastMonthExpr =>
        val dimensionRef = expr.dimension.get
        s"toStartOfMonth(${dimensionRef.sql}) = toStartOfMonth(now())"
      // expression placeholders to be replaced by validator, not expected here
      case FactorExpr => throw new UnsupportedOperationException
    }
  }

  private def getSqlRepr(entityRef: EntityRef): String = {
    entityRef match {
      case str: StringEntityRef => s"'${str.value}'"
      case number: NumberEntityRef => number.value.toString
      case date: DateEntityRef => entityRef.toString
    }
  }

  private def opToStr(op: Operation): String = op match {
    case GT => ">"
    case LT => "<"
    case EQ => "="
  }
}
