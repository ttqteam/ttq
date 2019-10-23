package ttq.lf

import org.scalatest.FunSuite
import ttq.common.{AggregateRef, StringEntityRef}

class QueryToTokensConverterTest extends FunSuite {

  test("testQueryToSql") {
    val agg = TestOntology.BUY_SELL_AGG
    val dim1 = TestOntology.ASSET_DIM
    val dim2 = TestOntology.CITY_DIM
    val query = AggregateQuery(
      UserAggregateExpr(Some(agg)),
      None,
      Option(
        OrExpr(
          OrExpr(
            DimensionEqualsEntityExpr(Some(dim1), Some(StringEntityRef(dim1, "A"))),
            DimensionEqualsEntityExpr(Some(dim1), Some(StringEntityRef(dim1, "B"))),
          ),
          AndExpr(
            DimensionEqualsEntityExpr(Some(dim1), Some(StringEntityRef(dim1, "C"))),
            OrExpr(
              DimensionEqualsEntityExpr(Some(dim2), Some(StringEntityRef(dim2, "D"))),
              DimensionEqualsEntityExpr(Some(dim2), Some(StringEntityRef(dim2, "E"))),
            )
          )
      ))
    )
    val res = QueryToTokensConverter.convert(query)
    println(res)
    val a = agg.name
    val d1 = dim1.name
    val d2 = dim2.name
    val expected = s"$a where $d1 = A or $d1 = B or $d1 = C and ( $d2 = D or $d2 = E )" // spaces around parens because it's just tokens
    assert(res.map(_.name).mkString(" ") == expected)
  }
}
