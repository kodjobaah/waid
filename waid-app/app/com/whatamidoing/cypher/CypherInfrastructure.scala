package com.whatamidoing.cypher

object CypherInfrastructure {
  
  def createTimeLine(): String = {
    val t = s"""
                 create (timeline:TimeLine {value:"timeline"})
                """
    t
  }
  
  def createYear(year: Int): String = {
    val t = s"""
                 create (year:Year {value:$year})
                """
    t
  }
  
  def createMonth(month: Int, description: String): String = {
    val t = s"""
                 create (month:Month {value:$month, description: "$description"})
                """
    t
  }
  
   def createDay(day: Int, description: String): String = {
    val t = s"""
                 create (day:Day {value:$day, description: "$description"})
                """
    t
  }
  
  
  
 def linkTimeLineWithYear(year: Int): String = {
    val linkMonthWithYear = s"""
 			  match (a:TimeLine), (b:Year)
			  where a.value="timeline" AND b.value = $year
			  create a-[r:YEAR]->b
			  return r
			  """
    linkMonthWithYear
   
 }

 def linkMonthWithYear(month: String, year: Int): String = {
    val linkMonthWithYear = s"""
 			  match (a:Year), (b:Month)
			  where a.value=$year AND b.description = "$month"
			  create a-[r:MONTH]->b
			  return r
			  """
    linkMonthWithYear
   
 }

  def linkMonthToDay(month: String, day: String): String = {
    val linkToToken = s"""
 			  match (a:Month), (b:Day)
			  where a.description="$month" AND b.description = "$day"
			  create a-[r:DAY]->b
			  return r
			  """
    linkToToken
  }
  
}