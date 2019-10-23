package ttq.common

import scala.concurrent.CancellationException

class CancellationToken private[common] (source: CancellationSource) {
  def throwIfCancelled(): Unit = {
    if (source.isCancelled)
      throw new CancellationException()
  }
}

case object CancellationToken {
  def fake = new CancellationToken(new CancellationSource)
}