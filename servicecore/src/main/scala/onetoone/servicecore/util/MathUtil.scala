package onetoone.servicecore.util

trait MathUtil {

  def between(i: Long, minValueInclusive: Long, maxValueInclusive: Long): Boolean =
    if (i >= minValueInclusive && i <= maxValueInclusive) true
    else false

  def between(i: Double, minValueInclusive: Double, maxValueInclusive: Double): Boolean =
    if (i >= minValueInclusive && i <= maxValueInclusive) true
    else false

  def between(i: Int, minValueInclusive: Int, maxValueInclusive: Int): Boolean =
    if (i >= minValueInclusive && i <= maxValueInclusive) true
    else false

}
