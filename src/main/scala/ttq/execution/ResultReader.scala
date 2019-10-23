package ttq.execution

import java.sql.ResultSet

trait ResultReader {
  def readResult(rs: ResultSet, modifiers: Seq[String]): ExecutionResult
}



/*
probably to be supported later:

scatter chart: z1, z2
z1 z2
-----
 1  2
 3  4

bubble chart: r1, r2, r3
r1 r2 r3
--------
 1  2  3
 4  5  6

table2
  | a  b
--+-----
c | 1  2
d | 3  4

x  y r1
--------
a  b  1
c  d  3
*/

