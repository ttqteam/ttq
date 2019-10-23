package ttq.ontology

import ttq.dsl._
import ValidatorUtils.getInnerPropertyNames
import ttq.common.{AggregateRef, DimensionRef, FactsRef, FinalProperty, MeasureRef}

import scala.collection.JavaConverters._
import scala.collection.mutable

object OntologyParser {
  def getFinalOntology(ontology: Ontology): FinalOntology = {
    FinalOntology(ontology.classes.map(ontologyClass => toFinalClass(ontologyClass)).toList)
  }

  private def toFinalClass(ontologyClass: Class): FinalClass = {
    if (ontologyClass.getSql == null) {
      throw new IllegalStateException(s"Sql/table not set for class $ontologyClass")
    }

    // todo - just one facts set for one table for now
    val factsRefs = List(FactsRef(ontologyClass.getSql, ontologyClass.name, ontologyClass.name, ontologyClass.getOrderBy))

    val dimensionRefs: List[DimensionRef] = deriveFinalProperties(ontologyClass.dimensions.toList, ontologyClass)
      .asInstanceOf[List[DimensionRef]]

    val measureRefs: List[MeasureRef] = deriveFinalProperties(ontologyClass.measures.toList, ontologyClass)
      .asInstanceOf[List[MeasureRef]]

    val aggregateRefs: List[AggregateRef] = deriveAggregateRefs(
      ontologyClass.aggregates.toList,
      measureRefs,
      ontologyClass
    )

    //ontologyClass.aggregates.foreach(aggregate => validateAggregate(aggregate, ontologyClass)) todo
    FinalClass(ontologyClass.name,
      ontologyClass.getSql,
      factsRefs,
      aggregateRefs,
      measureRefs,
      dimensionRefs,
      ontologyClass.hide.toSet)
  }

   def deriveFinalProperties(properties: List[Property], ontologyClass: Class): List[FinalProperty] = {
    val propertyByName = properties.map(property => property.name -> property).toMap
    val graph = properties
      .map(property => property -> getInnerPropertyNames(property.getSql).map(name => propertyByName(name))).toMap
    val sortedProperties = new GraphUtils[Property].topSort(graph)

    val finalDimMap = mutable.Map[String, FinalProperty]()

    for (property <- sortedProperties) {
      var sql = property.getSql
      graph(property)
        .foreach(innerProperty => sql = sql.replace(
          "$" + s"${innerProperty.name}",
          s"(${finalDimMap(innerProperty.name).sql})"
        ))
      val finalProperty = property match {
        case _: Dimension => DimensionRef(sql, property.name, property.dataType, ontologyClass.name, property.getSynonyms)
        case a: Aggregate => AggregateRef(sql, property.name, ontologyClass.name, a.getUnits, property.getSynonyms)
        case _: Measure => MeasureRef(sql, property.name, property.dataType, ontologyClass.name, property.getSynonyms)
      }
      finalDimMap.put(property.name, finalProperty)
    }

    finalDimMap.values.toList
  }

  private def deriveAggregateRefs(
      aggregates: List[Aggregate],
      MeasureRefs: List[MeasureRef],
      ontologyClass: Class
  ): List[AggregateRef] = {
    val measureSqlByName = MeasureRefs.map(measure => measure.name -> measure.sql).toMap
    for (aggregate <- aggregates) {
      var sql:String = aggregate.getSql
      getInnerPropertyNames(aggregate.getSql).foreach(unresolvedProperty =>
        if (measureSqlByName.contains(unresolvedProperty)) {
          sql = sql.replace(
            "$" + s"$unresolvedProperty",
            s"(${measureSqlByName(unresolvedProperty)})"
          )
        })
      aggregate.fromSql(sql)
    }

    deriveFinalProperties(aggregates, ontologyClass).asInstanceOf[List[AggregateRef]]
  }

  /*
  * Result set for aggregates should contain exactly 1 row and 1 column
  * */
  private def validateAggregate(aggregate: AggregateRef): Unit = ???
}
