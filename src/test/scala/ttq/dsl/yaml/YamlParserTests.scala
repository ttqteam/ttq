package ttq.dsl.yaml

import org.scalatest.FunSuite
import ttq.common.NumberType

class YamlParserTests extends FunSuite {
  test("simple test") {
    val str =
      """
        |name: facts name
        |sql: facts sql
        |dimensions:
        |  - name: dim1 name
        |    sql: dim1 sql
        |    type: number
        |    synonyms:
        |      - dim1 synonym1
        |      - dim1 synonym2
        |  - name: dim2 name
        |    sql: dim2 sql
        |    synonyms: dim2 synonym1, dim2 synonym2
        |aggregates:
        |  - name: agg1 name
        |    sql: agg1 sql
        |    # percent is a special char in YAML
        |    units: "%"
        |    synonyms:
        |      - agg1 synonym1
      """.stripMargin

    val res = YamlParser.readOntologyClass(str)

    assert(res.name == "facts name")
    assert(res.getSql == "facts sql")

    assert(res.dimensions.size == 2)
    val d1 = res.dimensions.find(_.name == "dim1 name").get
    assert(d1.name == "dim1 name")
    assert(d1.getSql == "dim1 sql")
    assert(d1.dataType == NumberType)
    assert(d1.getSynonyms.contains("dim1 synonym1"))
    assert(d1.getSynonyms.contains("dim1 synonym2"))

    val d2 = res.dimensions.find(_.name == "dim2 name").get
    assert(d2.name == "dim2 name")
    assert(d2.getSql == "dim2 sql")
    assert(d2.getSynonyms.contains("dim2 synonym1"))
    assert(d2.getSynonyms.contains("dim2 synonym2"))

    assert(res.aggregates.size == 1)
    val a = res.aggregates.find(_.name == "agg1 name").get
    assert(a.name == "agg1 name")
    assert(a.getSql == "agg1 sql")
    assert(a.getUnits == Some("%"))
    assert(a.getSynonyms.contains("agg1 synonym1"))
  }

}
