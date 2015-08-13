package com.whatamidoing.cypher

import play.Logger


object CypherReader {

  def searchForUser(user: String): String = {
    val search = s"""
    		match (a:User)
    		where a.email = "$user" 
    		return a.password as password, a.email as email
    		"""
    return search
  }

   def getTokenForUser(em: String): String = {

    val res = s"""
    		  match (a:User)
    		  where a.email = "$em"
			  with a
    		  match (a)-[:HAS_TOKEN]->(b)
    		  where b.valid = "true"
			  return b.token as token , b.valid as status
	  """
    return res
  }
  
  def getValidToken(token: String): String = {
    
    val res=s"""
    		match (token:AuthenticationToken)
    		where token.token="$token" and token.valid="true"
    		return token.token as token
      
      """
      return res
    
  }
  
  def findActiveStreamForToken(token: String) : String = {
    
    val res=s"""
    		match (a:AuthenticationToken)
    		where a.token="$token" and a.valid="true"
    		with a
    		match (a)-[r]-(b)
    		where type(r) = 'USING' and b.state='active'
    		return b.name as name
      
      """
      return res
    
  }
  
  def findStreamForInvitedId(invitedId: String) : String = {
    val res=s"""
    		match (a:Invite)
    		where a.id = "$invitedId"
    		with a
    		match (a)-[:TO_WATCH]->(r)
    		where r.state = "active"
    		return r.id as name
      """
//      Logger.info("--findStreamForInviteId:["+res+"]")
      return res
  }

  def findStreamForInviteTwitter(invitedId: String) : String = {
    val res=s"""
    		match (a:InviteTwitter)
    		where a.id = "$invitedId"
    		with a
    		match (a)-[:TO_WATCH]->(r)
    		where r.state = "active"
    		return r.id as name
      """
      return res
  }

  def findStreamForInviteFacebook(invitedId: String) : String = {
    val res=s"""
    		match (a:InviteFacebook)
    		where a.id = "$invitedId"
    		with a
    		match (a)-[:TO_WATCH]->(r)
    		where r.state = "active"
    		return r.id as name
      """
      return res
  }

  def findStreamForInviteLinkedin(invitedId: String) : String = {
    val res=s"""
    		match (a:InviteLinkedin)
    		where a.id = "$invitedId"
    		with a
    		match (a)-[:TO_WATCH]->(r)
    		where r.state = "active"
    		return r.id as name
      """
      return res
  }


  def checkToSeeIfTwitterInviteAcceptedAlreadyByReferer(inviteId: String, referer: String) : String  = {
     val res=s"""
    		match (a:InviteTwitter), (b:Referer)
    		where a.id = "$inviteId" and  b.id = "$referer"
    		with a,b
                match (a)-[r:USING_REFERAL]-(b)
    		return b.id as id
      """
  //   Logger.info("---checkToSeeIfTwitterInviteAcceptedAlreadyByReferer["+res+"]")
      return res
 

  }

  def checkToSeeIfFacebookInviteAcceptedAlreadyByReferer(inviteId: String, referer: String) : String  = {
     val res=s"""
    		match (a:InviteFacebook), (b:Referer)
    		where a.id = "$inviteId" and  b.id = "$referer"
    		with a,b
                match (a)-[r:USING_REFERAL]-(b)
    		return b.id as id
      """
  //   Logger.info("---checkToSeeIfFacebookInviteAcceptedAlreadyByReferer["+res+"]")
      return res
 

  }

  def checkToSeeIfLinkedinInviteAcceptedAlreadyByReferer(inviteId: String, referer: String) : String  = {
     val res=s"""
    		match (a:InviteLinkedin), (b:Referer)
    		where a.id = "$inviteId" and  b.id = "$referer"
    		with a,b
                match (a)-[r:USING_REFERAL]-(b)
    		return b.id as id
      """
  //   Logger.info("---checkToSeeIfLinkedinInviteAcceptedAlreadyByReferer["+res+"]")
      return res
 

  }


  
  def findAllInvites(token: String): String = {
        val res=s"""
          match (tok:AuthenticationToken)
          where tok.token="$token"
          with tok
          match (tok)-[:HAS_TOKEN]-(theUser)
          with theUser
          match (theUser)-[:HAS_TOKEN]-(allTokens)-[:USING]-(stream)-[TO_WATCH]-(invite)-[:RECEIVED]-(user)
          return distinct user, user.email+":"+user.firstName+":"+user.lastName as email ;
     """
      return res

  }


  def countNumberAllStreamsForDay(token: String): String = {

    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token"
    with tok
    match (tok)-[u:USING]-(s)
    with s
    match (s)-[si:BROADCAST_ON]-(c)
    return count(distinct s) as count

    """
    return res
  }


  def countAllInvitesForToken(token: String): String = {
    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token"
    with tok
    match  (tok:AuthenticationToken)-[:USING]-(stream1:Stream)-[:TO_WATCH]-(invite:Invite)
    return count(distinct invite) as count;


    """
    return res
     }


  def findAllInvitesForStream(token: String, displayStart: Int, displayLength: Int, sortColumn: Int, sortDirection: String, streamId: String): String = {
    val sort = sortColumn match {
      case 0 => {
        "Order by d.description "+sortDirection
      }

      case 1 => {
        "Order by a.time "+sortDirection
      }
      case 2 => {
        "Order by user.email "+sortDirection
      }
      case 3 => {
        "Order by user.firstName "+sortDirection
      }
      case 4 => {
        "Order by user.lastName "+sortDirection
      }
   }

    val skip = displayStart

    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token"
    with tok
    match (d)-[a?:ACCEPTED_ON]-(invite)-[:TO_WATCH]-(stream)-[:USING]-(tok)
    where stream.name="$streamId"
    with d,a,tok,stream,invite
    match (tok)-[:USING]-(stream)-[:TO_WATCH]-(invite)-[:RECEIVED]-(user)
    return d.description as day , a.time as time , user.email as email, user.firstName as firstName, user.lastName as lastName
    $sort
    SKIP $skip
    LIMIT $displayLength
    """
    return res;

  }


  def findAllStreamsForDay(token: String, displayStart: Int, displayLength: Int, sortColumn: Int, sortDirection: String): String = {

    val sort = sortColumn match {
      case 1 => {
          "Order by s.name "+sortDirection
      }

      case 2 => {
        "Order by a.description "+sortDirection
      }
      case 3 => {
        "Order by si.time "+sortDirection
      }
      case 4 => {
        "Order by d.description "+sortDirection
      }
      case 5 => {
        "Order by se.time "+sortDirection
      }
    }

    val skip = displayStart
    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token"
    with tok
    match (tok)-[u:USING]-(s)
    with s
    match (a:AuthenticationToken)<-[si:BROADCAST_ON]-(s)-[se?:BROADCAST_ENDED_ON]->(d)
    return s.name as stream ,a.description as day,si.time as startTime, d.description as end, se.time as endTime
    $sort
    SKIP $skip
    LIMIT $displayLength

    """
    return res
  }

  def findAllTokensForUser(email: String) : String = {
    val res =s"""
    match (u:User)
    where u.email="$email"
    with u
    match (u:User)-[ht:HAS_TOKEN]-(tok:AuthenticationToken)-[u:USING]-(stream:Stream)
    return distinct tok.token as token;

    """
    return res
  }

  def getUsersWhoHaveAcceptedToWatchStream(token: String): String = {
    val res=s"""
          match (a:AuthenticationToken) where a.token="$token" and a.valid = "true"
          with a
          match (a:AuthenticationToken)-[r:USING]-(s:Stream) where s.state = "active"
          with s
          match (s:Stream)-[TO_WATCH]-(i:Invite)-[:ACCEPTED_ON]->(d:Day)
          with i
          match (i:Invite)<-[:RECEIVED]-(u:User)
          return distinct u.email as email, u.firstName as firstName, u.lastName as lastName
    """
    Logger.info("accepted to watch ["+res+"]")
    return res
  }

  def getCountOfAllUsersWhoHaveAcceptedToWatchStream(token: String): String = {
    val res=s"""
          match (a:AuthenticationToken) where a.token="$token" and a.valid = "true"
          with a
          match (a:AuthenticationToken)-[r:USING]-(s:Stream) where s.state = "active"
          with s
          match (s:Stream)-[TO_WATCH]-(i:Invite)-[:ACCEPTED_ON]->(d:Day)
          with i
          match (i:Invite)<-[:RECEIVED]-(u:User)
          return count(distinct u.email)  as count
    """
   // Logger.info("count of all accepted to watch ["+res+"]")
    return res
  }


  def getUsersWhoHaveBeenInvitedToWatchStream(token: String): String = {
    val res=s"""

    match (a:AuthenticationToken) where a.token="$token" and a.valid = "true"
    with a
    match (a:AuthenticationToken)-[r:USING]-(s:Stream) where s.state="active"
    with s
    match (s:Stream)-[t:TO_WATCH]-(i:Invite)
    with i
    match (i:Invite)-[:RECEIVED]-(u:User)
    where u.email is not null
    return distinct u.email as email, u.firstName as firstName, u.lastName as lastName
    """
   // Logger.info("invited to watch ["+res+"]")
    return res
  }

  def getUsersWhoHaveAcceptedToWatchStreamUsingStreamId(streamId: String): String = {
    val res=s"""
          match (s:Stream)-[:TO_WATCH]-(i:Invite)-[ACCEPTED_ON]->(d:Day)
          where s.id = "$streamId" and s.state="active"
          with i
          match (i:Invite)<-[:RECEIVED]-(u:User)
          return distinct u.email as email, u.firstName as firstName, u.lastName as lastName
    """

   // Logger.info(res);
    return res
  }

  def getUsersWhoHaveBeenInvitedToWatchStreamUsingStreamId(streamId: String): String = {
    val res=s"""
     match s-[t:TO_WATCH]-i
     where s.id = "$streamId" and s.state="active"
     with i
     match i-[:RECEIVED]-u
     where u.email is not null
    return distinct u.email as email, u.firstName as firstName, u.lastName as lastName
    """
   // Logger.info(res)
    return res
  }



  def getStreamsForCalendarThatHaveEnded(email: String,
                            startYear: Int, endYear: Int,
                            startMonth: Int, endMonth: Int,
                            startDay: Int, endDay: Int): String = {
    var res=""


    if (startYear == endYear) {
      if (startMonth == endMonth) {
        res = s"""

         match (y:Year)-[:MONTH]-(m:Month)-[:DAY]-(d:Day)-[broadcast:BROADCAST_ENDED_ON]-(s:Stream)-[:USING]-(t:AuthenticationToken)-[:HAS_TOKEN]-(u:User)
         where u.email="$email" and (y.value >= $startYear and y.value <= $endYear)
         and (m.value >= $startMonth and m.value <= $endMonth)  and (d.value >= $startDay  and d.value <= $endDay)
         return y.value as year, m.value as month, d.value as day, broadcast.time as time,s.id as streamId,s.name as streamName,u.email as email

       """

      } else {
        res = s"""
      match (y:Year)-[:MONTH]-(m:Month)-[:DAY]-(d:Day)-[broadcast:BROADCAST_ENDED_ON]-(s:Stream)-[:USING]-(t:AuthenticationToken)-[:HAS_TOKEN]-(u:User)
      where u.email="$email" and (y.value >= $startYear and y.value <= $endYear)
      and (m.value >= $startMonth and m.value <= $endMonth)
      return y.value as year, m.value as month, d.value as day, broadcast.time as time,s.id as streamId,s.name as streamName,u.email as email

       """

      }
    } else {
      res = s"""
       match (y:Year)-[:MONTH]-(m:Month)-[:DAY]-(d:Day)-[broadcast:BROADCAST_ENDED_ON]-(s:Stream)-[:USING]-(t:AuthenticationToken)-[:HAS_TOKEN]-(u:User)
       where u.email="$email" and (y.value >= $startYear and y.value <= $endYear)
       return y.value as year, m.value as month, d.value as day, broadcast.time as time,s.id as streamId,s.name as streamName,u.email as email

       """
    }
   // Logger.info(res)
    return res
  }

  def getStreamsForCalendar(email: String,
                            startYear: Int, endYear: Int,
                            startMonth: Int, endMonth: Int,
                            startDay: Int, endDay: Int): String = {
    var res=""


    if (startYear == endYear) {
      if (startMonth == endMonth) {
        res = s"""

         match (y:Year)-[:MONTH]-(m:Month)-[:DAY]-(d:Day)-[broadcast:BROADCAST_ON]-(s:Stream)-[:USING]-(t:AuthenticationToken)-[:HAS_TOKEN]-(u:User)
         where u.email="$email" and (y.value >= $startYear and y.value <= $endYear)
         and (m.value >= $startMonth and m.value <= $endMonth)  and (d.value >= $startDay  and d.value <= $endDay)
         return y.value as year, m.value as month, d.value as day, broadcast.time as time,s.id as streamId,s.name as streamName, u.email as email

       """

      } else {
        res = s"""
      match (y:Year)-[:MONTH]-(m:Month)-[:DAY]-(d:Day)-[broadcast:BROADCAST_ON]-(s:Stream)-[:USING]-(t:AuthenticationToken)-[:HAS_TOKEN]-(u:User)
      where u.email="$email" and (y.value >= $startYear and y.value <= $endYear)
      and (m.value >= $startMonth and m.value <= $endMonth)
      return y.value as year, m.value as month, d.value as day, broadcast.time as time,s.id as streamId,s.name as streamName, u.email as email

       """

      }
    } else {
      res = s"""
       match (y:Year)-[:MONTH]-(m:Month)-[:DAY]-(d:Day)-[broadcast:BROADCAST_ON]-(s:Stream)-[:USING]-(t:AuthenticationToken)-[:HAS_TOKEN]-(u:User)
       where u.email="$email" and (y.value >= $startYear and y.value <= $endYear)
       return y.value as year, m.value as month, d.value as day, broadcast.time as time,s.id as streamId,s.name as streamName,u.email as email

       """
    }
   // Logger.info(res)
    return res
  }

  def getEmailUsingToken(token: String): String = {

    val res=s"""

    match u-[HAS_TOKEN]-t
        where t.token="$token"
        return u.email as email
    """
   // Logger.info("getEmailUsingToken["+res+"]")
    return res

  }

  def checkToSeeIfCheckPasswordIdIsValid(cpId: String): String = {

    val res = s"""

        match (cp:ChangePassword) 
        where cp.id = "$cpId" and cp.state="active"
    	return cp.state as state;
    """
   //  Logger.info("--checkToSeeIfCheckPasswordIdIsValid["+res+"]")
     return res

  }

 def fetchUserDetails(token: String): String = {
     val res= s"""
     match (a:AuthenticationToken) where a.token="$token" and a.valid="true"
     match (b)-[r:HAS_TOKEN]-(a)
     return b.email as email, b.firstName as firstName , b.lastName as lastName
     """
    // Logger.info("--fecthUserDetails["+res+"]")
     return res
  }


 def fetchUserInformation(token: String): String = {

     val res= s"""
     	 match (a:AuthenticationToken) where a.token="$token"
     	 match (b)-[r:HAS_TOKEN]-(a)
     	 return b.email as email, b.firstName as firstName , b.lastName as lastName, b.domId as domId
     """
   //  Logger.info("--fecthUserInformation["+res+"]")
     return res
  }


def fetchLocationForStream(stream: String): String = {

        val res=s"""
	   match (s:Stream) where s.id = "$stream"
	    with s
	    match (s)-[l:LOCATED_AT]-(loc)
            return loc.latitude as latitude, loc.longitude as longitude
       """
     //  Logger.info("--fetchLocationForStream["+res+"]")
       return res
}

def fetchLocationForActiveStream(inviteId: String): String = {

        val res=s"""
	    match (i:Invite) where i.id="$inviteId"
	    with i
	    match (i)-[to:TO_WATCH]-(s) where s.state="active"
	    with s
	    match (s)-[l:LOCATED_AT]-(loc)
            return loc.latitude as latitude, loc.longitude as longitude
       """
      // Logger.info("--fetchLocationForActiveStream["+res+"]")
       return res

}

def fetchLocationForActiveStreamTwitter(inviteId: String): String = {

        val res=s"""
	    match (i:InviteTwitter) where i.id="$inviteId"
	    with i
	    match (i)-[to:TO_WATCH]-(s) where s.state="active"
	    with s
	    match (s)-[l:LOCATED_AT]-(loc)
            return loc.latitude as latitude, loc.longitude as longitude
       """
      // Logger.info("--fetchLocationForActiveStream["+res+"]")
       return res

}

def fetchLocationForActiveStreamFacebook(inviteId: String): String = {

        val res=s"""
	    match (i:InviteFacebook) where i.id="$inviteId"
	    with i
	    match (i)-[to:TO_WATCH]-(s) where s.state="active"
	    with s
	    match (s)-[l:LOCATED_AT]-(loc)
            return loc.latitude as latitude, loc.longitude as longitude
       """
       //Logger.info("--fetchLocationForActiveStream["+res+"]")
       return res

}

def fetchLocationForActiveStreamLinkedin(inviteId: String): String = {

        val res=s"""
	    match (i:InviteLinkedin) where i.id="$inviteId"
	    with i
	    match (i)-[to:TO_WATCH]-(s) where s.state="active"
	    with s
	    match (s)-[l:LOCATED_AT]-(loc)
            return loc.latitude as latitude, loc.longitude as longitude
       """
      // Logger.info("--fetchLocationForActiveStreamLinkedin["+res+"]")
       return res

}

  def countAllTwitterInvites(token: String, clause: String): String = {

    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token"
    with tok
    match (tok)-[u:USING]-(s) $clause
    with s
    match (s)-[t:TO_WATCH]-(i:InviteTwitter)
    return count(distinct i) as count
    """
   // Logger.info("--countAllTwitterInvites["+res+"]")
    return res
  }


  def countAllFacebookInvites(token: String, clause: String): String = {

    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token"
    with tok
    match (tok)-[u:USING]-(s) $clause
    with s
    match (s)-[t:TO_WATCH]-(i:InviteFacebook)
    return count(distinct i) as count
    """
   // Logger.info("--countAllFacebookInvites["+res+"]")
    return res
  }


  def countAllLinkedinInvites(token: String, clause: String): String = {

    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token"
    with tok
    match (tok)-[u:USING]-(s) $clause
    with s
    match (s)-[t:TO_WATCH]-(i:InviteLinkedin)
    return count(distinct i) as count
    """
   // Logger.info("--countAllLinkedinInvites["+res+"]")
    return res
  }


  def getTwitterViewers(token: String, streamClause: String = ""): String = {

    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token" 
    with tok
    match (tok)-[u:USING]-(s) $streamClause
    with s
    match (s)-[t:TO_WATCH]-(i:InviteTwitter)-[ur:USING_REFERER]-(r)-[ps:START_PLAYING]-(ss)
    where ss.state="active"
    return count(distinct ss) as count
    """
    //Logger.info("--getTwitterViewers["+res+"]")
    return res
  }
  def getLinkedinViewers(token: String, streamClause: String = ""): String = {

    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token" 
    with tok
    match (tok)-[u:USING]-(s) $streamClause
    with s
    match (s)-[t:TO_WATCH]-(i:InviteLinkedin)-[ur:USING_REFERER]-(r)-[ps:START_PLAYING]-(ss)
    where ss.state="active"
    return count(distinct ss) as count
    """
    //Logger.info("--getLinkedinViewers["+res+"]")
    return res
  }
  def getFacebookViewers(token: String, streamClause: String = ""): String = {

    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token" 
    with tok
    match (tok)-[u:USING]-(s) $streamClause
    with s
    match (s)-[t:TO_WATCH]-(i:InviteFacebook)-[ur:USING_REFERER]-(r)-[ps:START_PLAYING]-(ss)
    where ss.state="active"
    return count(distinct ss) as count
    """
  //  Logger.info("--getFacebookViewers["+res+"]")
    return res
  }


  def getEmailViewers(token: String, streamClause: String = ""): String = {

    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token" 
    with tok
    match (tok)-[u:USING]-(s) $streamClause
    with s
    match (s)-[t:TO_WATCH]-(i:Invite)-[ps:START_PLAYING]-(ss)
    where ss.state="active"
    return count(distinct ss) as count
    """
   // Logger.info("--getEmailViewers["+res+"]")
    return res
  }


  def getTwitterAcceptanceCount(token: String, streamClause: String = ""): String = {

    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token" 
    with tok
    match (tok)-[u:USING]-(s) $streamClause
    with s
    match (s)-[t:TO_WATCH]-(i:InviteTwitter)-[ur:USING_REFERER]-(r)
    return count(distinct r) as count
    """
   // Logger.info("--getTwitterAcceptanceCount["+res+"]")
    return res
  }


  def getLinkedinAcceptanceCount(token: String, streamClause: String = ""): String = {

    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token"
    with tok
    match (tok)-[u:USING]-(s) $streamClause
    with s
    match (s)-[t:TO_WATCH]-(i:InviteLinkedin)-[ur:USING_REFERER]-(r)
    return count(distinct r) as count
    """
    //Logger.info("--getLinkedinAcceptanceCount["+res+"]")
    return res
  }


  def getFacebookAcceptanceCount(token: String, streamClause: String = ""): String = {

    val res =s"""
    match (tok:AuthenticationToken)
    where tok.token="$token" 
    with tok
    match (tok)-[u:USING]-(s) $streamClause
    with s
    match (s)-[t:TO_WATCH]-(i:InviteFacebook)-[ur:USING_REFERER]-(r)
    return count(distinct r) as count
    """
   // Logger.info("--getFacebookAcceptanceCount["+res+"]")
    return res
  }

  def getReferersForLinkedin(stream: String): String = {

    val res =s"""
        match (s:Stream) where s.id="$stream"
	with s
	match (s)-[TO_WATCH]-(inv:InviteLinkedin)-[USING_REFERER]-(ref)
	return ref.id as ip
    """
   // Logger.info("--getRefererForLinkedIn["+res+"]")
    return res
  }

  def getReferersForTwitter(stream: String): String = {

    val res =s"""
        match (s:Stream) where s.id="$stream"
	with s
	match (s)-[TO_WATCH]-(inv:InviteTwitter)-[USING_REFERER]-(ref)
	return ref.id as ip
    """
   // Logger.info("--getRefererForTwitter["+res+"]")
    return res
  }

 def getReferersForFacebook(stream: String): String = {

    val res =s"""
        match (s:Stream) where s.id="$stream"
	with s
	match (s)-[TO_WATCH]-(inv:InviteFacebook)-[USING_REFERER]-(ref)
	return ref.id as ip
    """
   // Logger.info("--getRefererForFacebook["+res+"]")
    return res
  }

 def getRoomJid(token: String): String = {

    val res =s"""
       match (a:AuthenticationToken) where a.token = "$token"
      with a
      match s-[u:USING]->a
      where s.state="active"
      with s
      match (s)-[ur:USING_ROOM]-(rm:Room)
      return distinct rm.id as jid
      """
     // Logger.info("--getGroupJid["+res+"]")
    return res
  }


  def getRoomJidForStream(stream: String): String = {
      
      val res = s"""
      match (s:Stream) where s.id ="$stream"
      with s
      match (s)-[ur:USING_ROOM]-(rm:Room)
      return distinct rm.id as jid
      """
      //Logger.info("--getRoomJidForStream["+res+"]")
      return res

  }

  def getUserInformationUsingInviteId(inviteId: String): String = {

      val res = s"""
          match (invite:Invite) where invite.id="$inviteId"
	  with invite
      	  match (invite)-[:RECEIVED]-(user)
	  return user.email as email, user.firstName as firstName , user.lastName as lastName, user.domId as domId

      """
      //Logger.info("---getUserInformationUsingInviteId--")

      return res
  }


}