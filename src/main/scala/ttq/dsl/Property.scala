package ttq.dsl

import ttq.common.DataType

trait Property {
  def name: String
  def getSql: String
  def dataType: DataType
  def getSynonyms: List[String]
}
