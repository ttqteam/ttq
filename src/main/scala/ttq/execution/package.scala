package ttq

import java.sql.ResultSet

package object execution {
  def roundAt(precision: Int, value: Double): Double = {
    val s = math pow (10, precision)
    (math round value * s) / s
  }

  def resultSetIterator(resultSet: ResultSet) = {
    new Iterator[ResultSet] {
      def hasNext = resultSet.next()
      def next() = resultSet
    }
  }
}
