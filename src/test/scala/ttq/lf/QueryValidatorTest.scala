package ttq.lf

import java.time.LocalDate

import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite
import ttq.cache.EntryCache
import ttq.common._
import ttq.lf.TestOntology._
import ttq.tokenizer.StringEntityToken

class QueryValidatorTest extends FunSuite with MockFactory {
  private val CITY_LONDON = "London"
  private val CITY_SPB = "Spb"
  private val ASSET_EUR_USD = "EURUSD"
  private val ASSET_USD_JPY = "USDJPY"

  private val STRING_ENTITY_REF_ASSET_EUR_USD = StringEntityRef(ASSET_DIM, "EURUSD")
  private val STRING_ENTITY_REF_ASSET_USD_JPY = StringEntityRef(ASSET_DIM, "USDJPY")
  private val STRING_ENTITY_REF_CITY_SPB = StringEntityRef(CITY_DIM, "Spb")
  private val STRING_ENTITY_REF_CITY_LONDON = StringEntityRef(CITY_DIM, "London")

  private val DATE_ENTITY_REF_LAST_YEAR = DateEntityRef(LocalDate.of(2018, 1, 1))

  private val DIM_EQ_ENTITY_ASSET_EUR_USD = DimensionEqualsEntityExpr(Option(ASSET_DIM), Option(STRING_ENTITY_REF_ASSET_EUR_USD))
  private val DIM_EQ_ENTITY_ASSET_USD_JPY = DimensionEqualsEntityExpr(Option(ASSET_DIM), Option(STRING_ENTITY_REF_ASSET_USD_JPY))
  private val DIM_EQ_ENTITY_CITY_SPB = DimensionEqualsEntityExpr(Option(CITY_DIM), Option(STRING_ENTITY_REF_CITY_SPB))
  private val DIM_EQ_ENTITY_CITY_LDN = DimensionEqualsEntityExpr(Option(CITY_DIM), Option(STRING_ENTITY_REF_CITY_LONDON))

  private val DIM_OP_NUM_PRICE_GT_1000 = DimensionOpNumberExpr(Option(PRICE_DIM), Option(GT), Option(NumberEntityRef(1000)))
  private val DIM_OP_NUM_PRICE_LT_100 = DimensionOpNumberExpr(Option(PRICE_DIM), Option(LT), Option(NumberEntityRef(100)))
  private val DIM_OP_NUM_PRICE_EQ_500 = DimensionOpNumberExpr(Option(PRICE_DIM), Option(LT), Option(NumberEntityRef(500)))
  private val DIM_OP_NUM_WITH_HOLES = DimensionOpNumberExpr(Option(PRICE_DIM), None, None)

  private val ORDER_DATE_LAST_MONTH = LastMonthExpr(Option(ORDER_DATE_DIM))
  private val ORDER_DATE_LAST_YEAR = FilterByDateExpr(Option(ORDER_DATE_DIM), Option(DATE_ENTITY_REF_LAST_YEAR))

  private val map = Map[DimensionRef, List[String]](
    ASSET_DIM -> List(ASSET_EUR_USD, ASSET_USD_JPY),
    CITY_DIM -> List(CITY_LONDON, CITY_SPB)
  )

  private val stubEntryCache = stub[EntryCache]
  (stubEntryCache.cache _).when().returns(map)
  (stubEntryCache.getTopEntry _).when(*, ASSET_DIM).returns(Some(ASSET_EUR_USD))
  (stubEntryCache.getTopEntry _).when(*, CITY_DIM).returns(Some(CITY_LONDON))

  val queryValidator = new QueryValidator(TestOntology.ontology, stubEntryCache, "AnyUser")

  test("Expand inner FactorExpr with holes in regular expressions") {
    val filter = AndExpr(
      FactorExpr,
      OrExpr(DIM_OP_NUM_WITH_HOLES, FactorExpr)
    )

    val query = AggregateQuery(
      UserAggregateExpr(Some(BUY_SELL_AGG)),
      None,
      Option(filter)
    )
    val res = queryValidator.validateAndExpandQuery(query)
    print(res)
    assert(res.nonEmpty)
  }

  ignore("Expand inner FactorExpr") {
    val filter = AndExpr(
      FactorExpr,
      OrExpr(ORDER_DATE_LAST_MONTH, FactorExpr)
    )

    val query = AggregateQuery(
      UserAggregateExpr(Some(AggregateRef("buy_sell_ratio", "buy sell ratio", "orders"))),
      None,
      Option(filter)
    )
    val res = queryValidator.validateAndExpandQuery(query)
    print(res)
    assert(res.nonEmpty)
  }

  ignore("Expand FactorExpr") {
    val filter = FactorExpr

    val query = AggregateQuery(
      UserAggregateExpr(Some(AggregateRef("buy_sell_ratio", "buy sell ratio", "orders"))),
      None,
      Option(filter)
    )
    val res = queryValidator.validateAndExpandQuery(query)
    print(res)
    assert(res.nonEmpty)
  }

  test("Reduce conditions") {
    //todo: ((x AND x) AND x) -> (x)
    //todo: (a > 10) AND (a > 20) -> a > 20
    //todo: etc...
    //todo: (need we?)
  }

  test("Failed on conflict number condition") {
    //todo (x > 10) AND (x < 5)
    //todo (x > 10) AND (x < 10)
  }

  test("Failed on conflict entity condition") {
    val filter = AndExpr(
      AndExpr(DIM_EQ_ENTITY_CITY_LDN, DIM_EQ_ENTITY_CITY_SPB), //should fail on this cond!
      OrExpr(ORDER_DATE_LAST_MONTH, DIM_OP_NUM_PRICE_LT_100)
    )

    val query = AggregateQuery(
      UserAggregateExpr(Some(AggregateRef("buy_sell_ratio", "buy sell ratio", "orders"))),
      None,
      Option(filter)
    )
    val res = queryValidator.validateAndExpandQuery(query)
    print(res)
    assert(res.isEmpty)
  }

  test("Happy pass multi level filter") {
    val filter = AndExpr(
        OrExpr(DIM_EQ_ENTITY_CITY_LDN, DIM_EQ_ENTITY_CITY_SPB),
        OrExpr(ORDER_DATE_LAST_MONTH, DIM_OP_NUM_PRICE_LT_100)
    )

    val query = AggregateQuery(
      UserAggregateExpr(Some(AggregateRef("buy_sell_ratio", "buy sell ratio", "orders"))),
      None,
      Option(filter)
    )

    val res = queryValidator.validateAndExpandQuery(query).nonEmpty
    print(res)
    assert(res)
  }

  test("Different types of dimension and entity in filter") {
    val query = AggregateQuery(
      UserAggregateExpr(Some(AggregateRef("buy_sell_ratio", "buy sell ratio", "orders"))),
      None,
      Option(DimensionOpNumberExpr(Some(DimensionRef("asset", "asset", StringType, "orders")), Some(GT), Some(NumberEntityRef(10))))
    )
    assert(queryValidator.validateAndExpandQuery(query).isEmpty)
  }

  test("Valid query") {
    val query = AggregateQuery(
      UserAggregateExpr(Some(AggregateRef("buy_sell_ratio", "buy sell ratio", "orders"))),
      None,
      Option(DimensionOpNumberExpr(Some(DimensionRef("price", "price", NumberType, "orders")), Some(GT), Some(NumberEntityRef(10))))
    )
    queryValidator.validateAndExpandQuery(query).foreach(x => println(x))
    assert(queryValidator.validateAndExpandQuery(query).size == 1)
  }

  test("Query with OP placeholder") {
    val query = AggregateQuery(
      UserAggregateExpr(Some(AggregateRef("buy_sell_ratio", "buy sell ratio", "orders"))),
      None,
      Option(DimensionOpNumberExpr(Some(DimensionRef("price", "price", NumberType, "orders")), None, Some(NumberEntityRef(10))))
    )
    assert(queryValidator.validateAndExpandQuery(query).size == 3)
  }

  test("Query with placeholder which cannot be filled is not expanded") {
    val query = AggregateQuery(
      UserAggregateExpr(Some(AggregateRef("buy_sell_ratio", "buy sell ratio", "orders"))),
      None,
      Option(DimensionEqualsEntityExpr(Some(DimensionRef("price", "price", NumberType, "orders")), None))
    )
    assert(queryValidator.validateAndExpandQuery(query).isEmpty)
  }
}
