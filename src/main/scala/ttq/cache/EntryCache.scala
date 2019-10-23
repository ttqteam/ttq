package ttq.cache

import ttq.common.{DimensionRef, UserId}

trait EntryCache {
  type EntryType = String
  def recordEntityHit(user: UserId, dim: DimensionRef, entry: EntryType): Unit
  def getTopEntry(user: UserId, dim: DimensionRef): Option[EntryType]
  def cache: Map[DimensionRef, List[EntryType]]
}
