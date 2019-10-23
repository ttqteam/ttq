package ttq.lf

import java.time.LocalDate

import com.typesafe.scalalogging.Logger
import ttq.cache.EntryCache
import ttq.common._
import ttq.ontology.{FinalClass, FinalOntology}
import ttq.lf
import ttq.lf.ConditionValidation._

import scala.collection.mutable
import scala.util.{Failure, Random, Success, Try}

class QueryValidator(ontology: FinalOntology, entryCache: EntryCache, userId: UserId) {
  private val logger = Logger[QueryValidator]
  val random = new Random

  def validateAndExpandQuery(query: Query): List[Query] = {
    validateQueryAndGetClass(query) match {
      case Failure(exception) =>
        logger.warn(s"Validation error: $query -> ${exception.getMessage}")
        List()
      case Success(queryClass) =>
        val expanded = query match {
          case q: AggregateQuery => expandQuery(q, queryClass)
          case q: FactsQuery => expandQuery(q, queryClass)
        }
        expanded match {
          case Failure(exception) =>
            logger.error(s"Failed to expand $query: ${exception.getMessage}")
            List()
          case Success(value) =>
            logger.info(s"Validation success: $query -> $value")
            value
        }
    }
  }

  /*
  * 0) All referenced dimensions, aggregates and classes should exist in ontology
  * 1) Dimension type should be the same as entry type (if they are not placeholders)
  * 2) UserAggregateExpr, GroupByDimension, FilterExpr should refer to 1 class
  * */
  private def validateQueryAndGetClass(query: Query): Try[FinalClass] = Try {
    def getFactsClass(factsExpr: FactsExpr) = factsExpr.factsRef.map(f => ontology.getClass(f.className))

    def getAggregateClass(aggExpr: AggregateExpr) = aggExpr match {
      case userAggExpr: UserAggregateExpr =>
        userAggExpr.aggregate.map(
          agg => ontology.getClass(agg.className)
        )
    }

    def getGroupByClass(groupByExpr: Option[GroupByExpr]) = groupByExpr match {
      case Some(expr: GroupByDimensionExpr) => validateGroupBy(expr)
      case None => None
    }

    def getFilterClass(filterExpr: Option[FilterExpr]) = filterExpr match {
      case Some(filterExpr: FilterExpr) => validateFilterAndGetClass(filterExpr)
      case None => None
    }

    val queryClasses = (query match {
      case AggregateQuery(aggExpr, groupByExpr, filterExpr) =>
        getAggregateClass(aggExpr) ++ getGroupByClass(groupByExpr) ++ getFilterClass(filterExpr)
      case FactsQuery(factsExpr, filterExpr) =>
        getFactsClass(factsExpr) ++ getFilterClass(filterExpr)
    }).toSet

    if (queryClasses.isEmpty) {
      return Try(getRandomElement(ontology.classes).get)
    }

    if (queryClasses.size > 1) {
      throw new IllegalStateException(
        s"All properties should be from exactly 1 ontology class, but found classes: ${queryClasses.map(x => x.name)}"
      )
    }

    queryClasses.head
  }

  private def expandQuery(query: FactsQuery, queryClass: FinalClass): Try[List[Query]] = Try {
    val factsExprs = query.factsExpr.factsRef match {
      case Some(_) => List(query.factsExpr)
      case None => queryClass
        .facts
        .map(f => FactsExpr(Some(f)))
    }

    val filterByReplacements: Option[List[FilterExpr]] = query.filter.map {
      filterExpr => getFilterReplacements(filterExpr, queryClass)
    }

    for {
      fact <- factsExprs
      filterBy <- filterByReplacements.map(x => x.map(Some(_))).getOrElse(List(None))
    } yield FactsQuery(fact, filterBy)
  }

  /*
  * Replace all placeholders
  * */
  private def expandQuery(query: AggregateQuery, queryClass: FinalClass): Try[List[AggregateQuery]] = Try {
//    logger.info(s"Going to expand query: $query")
    val aggregateReplacements: List[UserAggregateExpr] = query.agg match {
      case userAggExpr: UserAggregateExpr => userAggExpr.aggregate match {
        case Some(_) => List(userAggExpr)
        case None => queryClass
          .aggregates
          .map(aggregate => UserAggregateExpr(Some(aggregate)))
      }
    }

    val groupByReplacements: Option[List[GroupByExpr]] = query.groupBy.map {
      case groupByDimExpr: GroupByDimensionExpr =>
        groupByDimExpr.dimension match {
          case Some(_) => List(groupByDimExpr)
          case None => getDimensions(queryClass, StringType).map(dim => lf.GroupByDimensionExpr(Some(dim)))
        }
    }

    val filterByReplacements: Option[List[FilterExpr]] = query.filterBy.map {
      filterExpr => getFilterReplacements(filterExpr, queryClass)
    }

    createQueryFromReplacements(aggregateReplacements, groupByReplacements, filterByReplacements).toList
  }

  //try to fillUp with String entities, then with last month
  //reduce if !clients request
  private def getFilterReplacements(filterExpr: FilterExpr, queryClass: FinalClass): List[FilterExpr] = {
    def getUsedEntryDimensions(expr: FilterExpr): Set[DimensionRef] = {
      expr match {
        case FactorExpr => Set[DimensionRef]()
        case expr: DimensionEqualsEntityExpr => if (expr.dimension.isDefined) Set(expr.dimension.get) else Set()
        case _: DimensionOpNumberExpr => Set[DimensionRef]()
        case _: FilterByDateExpr => Set[DimensionRef]()
        case _: LastMonthExpr => Set[DimensionRef]()
        case _: BooleanDimensionExpr => Set[DimensionRef]()
        case AndExpr(left, right) => getUsedEntryDimensions(left)++getUsedEntryDimensions(right)
        case OrExpr(left, right) => getUsedEntryDimensions(left)++getUsedEntryDimensions(right)
      }
    }

    val usedDimensions = getUsedEntryDimensions(filterExpr)
    val unusedStringEntryDimensions: mutable.Set[DimensionRef] = mutable.Set(
      getDimensions(queryClass, StringType).filter(el => !usedDimensions.contains(el)) :_*
    )
    val dateEntryDimensions = getDimensions(queryClass, DateType)

    def getFilterReplacements(filterExpr: FilterExpr): List[FilterExpr] = {
      filterExpr match {
        case dimFilterExpr: DimensionEqualsEntityExpr =>
          dimFilterExpr match {
            case DimensionEqualsEntityExpr(None, None) =>
              getDimensionReplacements(None, queryClass)
                .filter(dimRef => dimRef.dataType.equals(StringType))
                .flatMap(dimRef =>
                  getEntityReplacement(dimRef)
                    .map(entity => DimensionEqualsEntityExpr(Some(dimRef), Some(entity)))
                )
            case DimensionEqualsEntityExpr(Some(dimRef), Some(entityRef)) =>
              List(dimFilterExpr)
            case DimensionEqualsEntityExpr(Some(dimRef), None) =>
              getEntityReplacement(dimRef).map(entity => DimensionEqualsEntityExpr(Some(dimRef), Some(entity))).toList
            case DimensionEqualsEntityExpr(None, Some(entityRef:StringEntityRef)) =>
              List(DimensionEqualsEntityExpr(Some(entityRef.dimension), Some(entityRef)))
            case _ => List()
          }
        case numberFilter: DimensionOpNumberExpr => getDimensionReplacements(numberFilter.dimension, queryClass)
          .filter(dim => dim.dataType.equals(NumberType))
          .map(dim => numberFilter.copy(dimension = Option(dim)))
          .flatMap(filtrExpr => getOpReplacements(filtrExpr.operation).map(op => filtrExpr.copy(operation = Some(op))))
          .flatMap(filtrExpr => getNumEntityReplacements(filtrExpr.entity).map(e => filtrExpr.copy(entity = Some(e))))
        case dateFilter: FilterByDateExpr =>
          dateFilter match {
            case FilterByDateExpr(Some(_), Some(_)) =>
              List(dateFilter)
            case FilterByDateExpr(None, None) =>
              getDimensions(queryClass, DateType).map(dimRef =>
                FilterByDateExpr(Some(dimRef), Some(DateEntityRef(LocalDate.now.withDayOfMonth(1))))
              )
            case FilterByDateExpr(Some(dimRef), None) =>
              List(FilterByDateExpr(Some(dimRef), Some(DateEntityRef(LocalDate.now.withDayOfMonth(1)))))
            case FilterByDateExpr(None, Some(date)) =>
              getDimensions(queryClass, DateType).map(dimRef => FilterByDateExpr(Some(dimRef), Some(date)))
          }
        case lastMonthExpr: LastMonthExpr =>
          lastMonthExpr match {
            case LastMonthExpr(Some(_)) =>
              List(lastMonthExpr)
            case LastMonthExpr(None) =>
              getDimensions(queryClass, DateType).map(dimRef => LastMonthExpr(Some(dimRef)))
          }
        case booleanDimensionExpr: BooleanDimensionExpr =>
          booleanDimensionExpr match {
            case BooleanDimensionExpr(Some(_)) =>
              List(booleanDimensionExpr)
            case BooleanDimensionExpr(None) =>
              getDimensions(queryClass, BooleanType).map(dimRef => BooleanDimensionExpr(Some(dimRef)))
          }
        case FactorExpr =>
          if (unusedStringEntryDimensions.nonEmpty) {
            val replacementDim = unusedStringEntryDimensions.head
            unusedStringEntryDimensions.remove(replacementDim)
            List(DimensionEqualsEntityExpr(Option(replacementDim), getEntityReplacement(replacementDim)))
          } else {
            val dateDimRef = getRandomElement(dateEntryDimensions)
            List(LastMonthExpr(dateDimRef))
          }
        case AndExpr(left, right) => getConditionFilterReplacements(left, right, AND)

        case OrExpr(left, right) => getConditionFilterReplacements(left, right, OR)
      }
    }

    def getConditionFilterReplacements(left: FilterExpr, right: FilterExpr, cond: Condition): List[FilterExpr] = {
      val leftReplacements = getFilterReplacements(left)
      val rightReplacements = getFilterReplacements(right)
      (
        for {
          leftRepl <- leftReplacements
          rightRepl <- rightReplacements
        } yield ConditionValidation.reduceCondition(leftRepl, rightRepl, cond)
        )
        .filter(x => x.isDefined)
        .map(x => x.get)
        .distinct
    }

    getFilterReplacements(filterExpr).distinct
  }

  private def validateGroupBy(expr: GroupByDimensionExpr): Option[FinalClass] = {
    expr.dimension.map(dimRef =>
      if (dimRef.dataType == StringType || dimRef.dataType == DateType) {
        validateAndGetOntologyClassByDimRef(dimRef)
      } else {
        throw new IllegalStateException("Dimension in GroupByDimensionExpr should be of StringType or DateType")
      }
    )
  }

  private def validateFilterAndGetClass(filterExpression: FilterExpr): Option[FinalClass] = {
    def validateConditionAndGetClass(left: FilterExpr, right: FilterExpr, cond: Condition): Option[FinalClass] = {
      val leftClass = validateFilterAndGetClass(left)
      val rightClass = validateFilterAndGetClass(right)
      if (leftClass.isDefined && rightClass.isDefined && leftClass != rightClass) {
        throw new IllegalStateException("One class expected for filters in 1 query")
      }
      if (leftClass.isDefined && rightClass.isDefined) {
        ConditionValidation.validate(left, right, cond).get
      }
      if (leftClass.isDefined) leftClass else rightClass
    }

    filterExpression match {
      case filterExpr: DimensionEqualsEntityExpr =>
        val dimClass = filterExpr.dimension.map(dimRef => validateAndGetOntologyClassByDimRef(dimRef))
        val entityClass = filterExpr.entity.map(entityRef => validateAndGetOntologyClassByDimRef(entityRef.dimension))
        validateDimensionVsEntityType(filterExpr.dimension, filterExpr.entity)
        dimClass.orElse(entityClass)
      case filterExpr: DimensionOpNumberExpr =>
        filterExpr.dimension match {
          case Some(dimRef: DimensionRef) if !dimRef.dataType.equals(NumberType) =>
            throw new IllegalStateException("Dimension in DimensionOpNumberExpr should be of NumberType")
          case _ =>
        }
        validateDimensionVsEntityType(filterExpr.dimension, filterExpr.entity)
        filterExpr.dimension.map(dimRef => validateAndGetOntologyClassByDimRef(dimRef))
      case filterExpr: FilterByDateExpr =>
        filterExpr.dimension match {
          case Some(dimRef: DimensionRef) if !dimRef.dataType.equals(DateType) =>
            throw new IllegalStateException("Dimension in FilterByDateExpr should be of DateType")
          case _ =>
        }
        val dimClass = filterExpr.dimension.map(dimRef => validateAndGetOntologyClassByDimRef(dimRef))
        validateDimensionVsEntityType(filterExpr.dimension, filterExpr.fromDate)
        dimClass
      case filterExpr: LastMonthExpr =>
        filterExpr.dimension match {
          case Some(dimRef: DimensionRef) if !dimRef.dataType.equals(DateType) =>
            throw new IllegalStateException("Dimension in LastMonthExpr should be of DateType")
          case _ =>
        }
        val dimClass = filterExpr.dimension.map(dimRef => validateAndGetOntologyClassByDimRef(dimRef))
        dimClass
      case booleanDimensionExpr: BooleanDimensionExpr =>
        booleanDimensionExpr.dimension match {
          case Some(dimRef: DimensionRef) if !dimRef.dataType.equals(BooleanType) =>
            throw new IllegalStateException("Dimension in BooleanDimensionExpr should be of BooleanType")
          case _ =>
        }
        val dimClass = booleanDimensionExpr.dimension.map(dimRef => validateAndGetOntologyClassByDimRef(dimRef))
        dimClass
      case FactorExpr => None
      case OrExpr(left, right) => validateConditionAndGetClass(left, right, OR)
      case AndExpr(left, right) => validateConditionAndGetClass(left, right, AND)
    }
  }

  private def getOpReplacements(op: Option[Operation]): List[Operation] = {
    op match {
      case None => List(EQ, LT, GT)
      case Some(x) => List(x)
    }
  }

  private def createQueryFromReplacements(
    aggregateReplacements: List[UserAggregateExpr],
    groupByReplacements: Option[List[GroupByExpr]],
    filterByReplacements: Option[List[FilterExpr]]
  ): Seq[AggregateQuery] = {
    val res = for {
      agg <- aggregateReplacements
      groupBy <- groupByReplacements.map(x => x.map(Some(_))).getOrElse(List(None))
      filterBy <- filterByReplacements.map(x => x.map(Some(_))).getOrElse(List(None))
    } yield AggregateQuery(agg, groupBy, filterBy)
    res
  }

  private def getEntityReplacement(dimRef: DimensionRef): Option[StringEntityRef] =
    entryCache.getTopEntry(userId, dimRef).map(x => StringEntityRef(dimRef, x))

  private def getRandomElement[T](list: List[T]): Option[T] = {
    if (list == null || list.isEmpty) return Option.empty
    Option(list(random.nextInt(list.length)))
  }

  private def getDimensionReplacements(
    dimension: Option[DimensionRef],
    queryClass: FinalClass
  ): List[DimensionRef] = {
    dimension match {
      case None => queryClass.dimensions
      case Some(dim) => List(dim)
    }
  }

  private def getDimensions(queryClass: FinalClass, dateType: DataType): List[DimensionRef] = {
    queryClass.dimensions.filter(_.dataType == dateType)
  }

  private def validateAndGetOntologyClassByDimRef(dimRef: DimensionRef): FinalClass = {
    ontology.getClass(dimRef.className)
  }

  private def validateDimensionVsEntityType(dimension: Option[DimensionRef], entity: Option[EntityRef]): Unit = {
    (dimension, entity) match {
      case (Some(dim), Some(ent)) =>
        val dimensionType = dim.dataType
        val entityType = getEntityType(ent)
        if (!entityType.equals(dimensionType)) {
          throw new IllegalStateException(s"Dimension and entity has different types: $dim, $ent")
        }
        if (entityType == StringType) {
          if (dimension.get != entity.get.asInstanceOf[StringEntityRef].dimension) {
            throw new IllegalStateException(s"Entity from different dimension: $dim, $ent")
          }
        }
      case _ => ;
    }
  }

  private def getEntityType(entity: EntityRef): DataType = entity match {
    case _ : NumberEntityRef => NumberType
    case _ : DateEntityRef => DateType
    case _ : StringEntityRef => StringType
  }

  private def getNumEntityReplacements(entity: Option[NumberEntityRef]): Option[NumberEntityRef] = {
    entity match {
      case None => Option(NumberEntityRef(100000000))
      case Some(_) => entity
    }
  }
}
