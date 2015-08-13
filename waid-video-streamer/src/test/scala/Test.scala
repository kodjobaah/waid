/**
 * Created by kodjobaah on 09/08/2015.
 */
object Test extends App {


  val num = 270.0

  var result = 0.0

  if (num != 0.0) {
    val mod = num % 90

    if (mod == 0) {
      //Multiple of 90
      result = (num /90) * 90
    } else {
      val start:Int = (num.toInt/90)  * 90
      val end = ((num.toInt/90) + 1) * 90
      val  lowestDiff  = num - start
      val highDiff = end - num
      if (start == 0) {
          result = 90
      } else {
        if (lowestDiff > highDiff) {
          result = end
        } else {
          result = start
        }
      }
    }

  }
  println("val["+result+"]")
}
