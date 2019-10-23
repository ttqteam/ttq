package ttq.dsl

import scala.collection.mutable.ArrayBuffer

class Ontology {
  val classes = new ArrayBuffer[Class]()

  def addClass(ontologyClass: Class): Ontology = {
    classes += ontologyClass
    this
  }

  def prettyPrint: Unit = {
    for (ontologyClass <- classes) {
      ontologyClass.prettyPrint()
      println()
    }
  }
}
