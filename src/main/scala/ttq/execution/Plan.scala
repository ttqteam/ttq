package ttq.execution

import ttq.common.DimensionRef
import ttq.lf.{AggregateExpr, FilterExpr, GroupByExpr}

case class Plan(select: Select, project: Project)

// Select and Project are loosely based on those in https://en.wikipedia.org/wiki/Relational_algebra
case class Select(tableName: String, filterExpr: Option[FilterExpr], orderBy: Option[String] = None, limit: Option[Int] = None)
trait Project
case class AggregateProject(aggExpr: AggregateExpr, groupByExpr: Option[GroupByExpr]) extends Project
case class DimensionProject(dimRefs: Seq[DimensionRef]) extends Project
