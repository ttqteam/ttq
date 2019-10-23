package ttq.common

sealed trait DataType
case object NumberType extends DataType
case object DateType extends DataType
case object StringType extends DataType
case object BooleanType extends DataType
