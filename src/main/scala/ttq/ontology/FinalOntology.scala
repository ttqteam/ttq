package ttq.ontology

import ttq.common.{AggregateRef, DimensionRef, FactsRef, MeasureRef}
//todo rename "class" to "table"?
case class FinalOntology(classes: List[FinalClass]) {
  private val classsMap = classes.map(x => (x.name, x)).toMap

  def getClass(className: String): FinalClass = classsMap(className) // todo: why not optional?
  def getFacts(className: String, factsName: String): Option[FactsRef] =
    classsMap.get(className).flatMap(c => c.getFacts(factsName))
  def getDimension(className: String, dimensionName: String): Option[DimensionRef] =
    classsMap.get(className).flatMap(c => c.getDimension(dimensionName))
  def getAggregate(className: String, aggregateName: String): Option[AggregateRef] =
    classsMap.get(className).flatMap(c => c.getAggregate(aggregateName))
  def getMeasure(className: String, measureName: String): Option[MeasureRef] =
    classsMap.get(className).flatMap(c => c.getMeasure(measureName))
}

case class FinalClass(
  name: String,
  sql: String,
  facts: List[FactsRef],
  aggregates: List[AggregateRef],
  measures: List[MeasureRef],
  dimensions: List[DimensionRef],
  hide: Set[String])
{
  private val factsMap = facts.map(x => (x.name, x)).toMap
  private val dimensionsMap = dimensions.map(x => (x.name, x)).toMap
  private val aggregateMap = aggregates.map(x => (x.name, x)).toMap
  private val measureMap = measures.map(x => (x.name, x)).toMap
  def getFacts(factsName: String): Option[FactsRef] = factsMap.get(factsName)
  def getDimension(dimensionName: String): Option[DimensionRef] = dimensionsMap.get(dimensionName)
  def getAggregate(aggregateName: String): Option[AggregateRef] = aggregateMap.get(aggregateName)
  def getMeasure(measureName: String): Option[MeasureRef] = measureMap.get(measureName)
  def visibleDimensions: List[DimensionRef] = dimensions.filter(d => !hide.contains(d.name))
}

