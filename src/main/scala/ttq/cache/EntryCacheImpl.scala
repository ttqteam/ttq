package ttq.cache

import ttq.common.{DimensionRef, UserId}
import ttq.ontology.FinalOntology

import scala.collection.mutable
import java.sql._

// TODO: refactor/test this mess
class EntryCacheImpl(ontology: FinalOntology, cacheFile: String)  extends EntryCache {

  private val lock = new Object()
  private val entriesOrderedByFrequency = mutable.Map[DimensionRef, List[EntryType]]()
  private val lastHitsByDimByUser = mutable.Map[UserId, mutable.Map[DimensionRef, EntryType]]()
  private val conn = DriverManager.getConnection(s"jdbc:h2:$cacheFile")

  initDb()

  override def recordEntityHit(user: UserId, dim: DimensionRef, entry: EntryType): Unit = {
    lock.synchronized {
      lastHitsByDimByUser.getOrElseUpdate(user, mutable.Map[DimensionRef, EntryType]()).put(dim, entry)
      // todo: must be a separate worker thread etc., probably outside of this class
      // todo: sql injection etc.
      val createSql = s"""MERGE INTO user_hits KEY (user_id, class, dim)
                    VALUES ('$user', '${dim.className}', '${dim.name}', '$entry')""".stripMargin
      conn.createStatement().execute(createSql)
    }
  }

  def setEntitiesSortedByUsageDesc(dimension: DimensionRef, entries: List[EntryType]): Unit = {
    lock.synchronized {
      entriesOrderedByFrequency.put(dimension, entries)
    }
  }

  override def getTopEntry(user: UserId, dim: DimensionRef): Option[EntryType] = {
    lock.synchronized {
      lastHitsByDimByUser.get(user).flatMap(h => h.get(dim)) // last hit entry for the user
      .orElse(
      entriesOrderedByFrequency.get(dim).flatMap(x => x.headOption) // top entry by number of occurrences
      )
    }
  }

  override def cache: Map[DimensionRef, List[EntryType]] = entriesOrderedByFrequency.toMap

  // todo: must be a separate worker thread etc., probably outside of this class
  private def initDb() = {
    // multi-threaded access via single H2 connection is synchronized within connection
    val createSql = """CREATE TABLE IF NOT EXISTS user_hits (
                      |user_id varchar(255),
                      |class varchar(255),
                      |dim varchar(255),
                      |str varchar(255)
                      |)""".stripMargin
    conn.createStatement().execute(createSql)

    val sql = s"""SELECT * FROM user_hits"""
    val res = conn.createStatement().executeQuery(sql)
    // todo: don't read from IO under lock!
    lock.synchronized {
      while (res.next()) {
        val userId = res.getString(1)
        val clsString = res.getString(2)
        val dimString = res.getString(3)
        val entryString = res.getString(4)
        ontology.getDimension(clsString, dimString).foreach(dimRef =>
          lastHitsByDimByUser
            .getOrElseUpdate(userId, mutable.Map[DimensionRef, EntryType]())
            .getOrElseUpdate(dimRef, entryString)
        )
      }
    }
  }
}
