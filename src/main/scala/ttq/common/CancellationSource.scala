package ttq.common

class CancellationSource {
  @volatile private var cancelled: Boolean = false

  def cancel(): Unit = cancelled = true

  def token: CancellationToken = new CancellationToken(this)

  def isCancelled: Boolean = cancelled
}
