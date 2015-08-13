package com.whatamidoing.cypher


object CypherWriter {

  def createUser(fn: String, ln: String, em: String, pw_hash: String, domId: String): String = {
    val s = s"""
              create (user:User {id:"$em", email:"$em", password:"$pw_hash",firstName:"$fn",lastName:"$ln" , domId: "domId"})
               """

    s
  }

  def createToken(token: String, valid: String): String = {
    val t = s"""
                 create (token:AuthenticationToken {id:"$token" ,token:"$token",valid:"$valid"})
                """
    t
  }

  def createStream(stream: String): String = {
    val t = s"""
                 create (stream:Stream {id:"$stream",name:"$stream", state:"active"})
                """
    t
  }

  def linkStreamToToken(stream: String, token: String): String = {
    val t = s"""
    
    		match (a:Stream), (b:AuthenticationToken)
    		where a.name="$stream" and b.token="$token"
    		create a-[r:USING]->b
    		return r
    """
    t

  }

  def invalidateAuthenticationTokenForUser(token: String): String = {

    val t = s"""
    		match (a:AuthenticationToken)
    		where a.token ="$token"
    		with a
    		match (b)-[:HAS_TOKEN]->(a)
    		with b
    		match (b)-[:HAS_TOKEN]->(c)
    		SET c.valid = "false"
    		return c as token;
    """
    t
  }

  def linkStreamToDay(stream: String, day: String, time: String): String = {
    val t = s"""
    			match (a:Stream), (b:Day)
    			where a.name="$stream" AND b.description="$day"
    			create a-[r:BROADCAST_ON {time:"$time"}]->b
                return r
    			"""
    t
  }

  def linkUserToToken(em: String, token: String): String = {
    val linkToToken = s"""
 			  match (a:User), (b:AuthenticationToken)
			  where a.email="$em" AND b.token = "$token"
			  create a-[r:HAS_TOKEN]->b
			  return r
			  """
    linkToToken
  }

  def associateStreamCloseToDay(stream: String, day: String, time: String): String = {
    val linkCloseStreamToDay = s"""
     
 			  match (a:Stream), (b:Day)
			  where a.name="$stream" AND b.description = "$day"
			  create a-[r:BROADCAST_ENDED_ON {time:"$time"}]->b
			  return r
			  
			  """
    linkCloseStreamToDay
  }

  def closeStream(stream: String): String = {

    val res = s"""
    		match (stream:Stream)
    		where stream.name="$stream"
    		SET stream.state ="inactive"
    		return stream.state as state
    """
    res
  }

  def createInvite(stream: String, email: String, id: String): String = {
  val res = s"""
    		match (stream:Stream), (user:User)
    		where stream.name="$stream" and user.email="$email"
    		create (invite:Invite {name:"$stream-$email", id:"$id"}),
    		(invite)-[r:TO_WATCH]->(stream),
    		(user)-[s:RECEIVED]->(invite)
    		return s,r
    """
    res
  }

  def createInviteTwitter(stream: String, twitter: String, inviteId: String): String = {

    val res = s"""
                match (stream:Stream)
    		where stream.name="$stream" 
                with stream
    		create (invite:InviteTwitter {name:"$stream-$twitter}", id:"$inviteId"})
                with invite,stream
    		create (invite)-[r:TO_WATCH]->(stream)
    		return distinct invite.id as inviteId
    """
    res

  }

  def createInviteFacebook(stream: String, facebook: String, inviteId: String): String = {

    val res = s"""

                match (stream:Stream)
    		where stream.name="$stream" 
                with stream
    		create (invite:InviteFacebook {name:"$stream-$facebook}", id:"$inviteId"})
                with invite,stream
    		create (invite)-[r:TO_WATCH]->(stream)
    		return distinct invite.id as inviteId
    """
    res

  }

  def createInviteLinkedin(stream: String, linkedin: String, inviteId: String): String = {

    val res = s"""

                match (stream:Stream)
    		where stream.name="$stream" 
                with stream
    		create (invite:InviteLinkedin {name:"$stream-$linkedin}", id:"$inviteId"})
                with invite,stream
    		create (invite)-[r:TO_WATCH]->(stream)
    		return distinct invite.id as inviteId
    """
    res

  }


  def invalidateAllTokensForUser(email: String): String = {
    val res = s"""
        match (u:User)
        where u.email="$email"
        with u
        match (u)-[HAS_TOKEN]-(tok)
        set tok.valid = "false"
        return tok.valid as valid
      """
    res

  }

  def invalidateToken(token: String): String = {
    val res = s"""
    		match (a:AuthenticationToken)
    		where a.token = "$token"
    		SET a.valid ="false"
    		return a.valid as valid
      """
    res

  }

  def createTokenForUser(token: String, email: String): String = {
    val res = s"""
    		match (a:User)
    		where a.email = "$email"
    		with a
    		create (token:AuthenticationToken {token:"$token",valid:"true"})
    		create a-[r:HAS_TOKEN]->token
    		return r
      """
    res
  }

  def associateDayWithInvite(inviteId: String, day: String, time: String): String = {
    val res = s"""
    		match (a:Invite), (b:Day)
    		where a.id = "$inviteId" and b.description="$day"
    		with a,b
    		create a-[r:ACCEPTED_ON {time:"$time"}]->b
    		return r
      """
    res
  }


  def associateInviteTwitterWithReferer(inviteId: String, day: String, time: String, referer: String, sessionId: String): String = {
    val res = s"""
    		match (inviteTwitter:InviteTwitter), (day:Day)
    		where inviteTwitter.id = "$inviteId" and day.description="$day"
    		with inviteTwitter,day
                create (referer:Referer{id:"$referer" , sessionId: "$sessionId"})
                with inviteTwitter,day,referer
                create (inviteTwitter)-[r:USING_REFERER]->(referer)-[a:ACCEPTED_ON {time:"$time"}]->(day)
    		return r,referer
      """
    // Logger.info("--associateInviteTwitterReferer["+res+"]")
    res
  }

  def associateInviteFacebookWithReferer(inviteId: String, day: String, time: String, referer: String, sessionId: String): String = {
    val res = s"""
    		match (inviteFacebook:InviteFacebook), (day:Day)
    		where inviteFacebook.id = "$inviteId" and day.description="$day"
    		with inviteFacebook,day
                create (referer:Referer{id:"$referer", sessionId: "$sessionId" })
                with inviteFacebook,day,referer
                create (inviteFacebook)-[r:USING_REFERER]->(referer)-[a:ACCEPTED_ON {time:"$time"}]->(day)
    		return r,referer
      """
    res
  }

  def associateInviteLinkedinWithReferer(inviteId: String, day: String, time: String, referer: String, sessionId: String): String = {
    val res = s"""
    		match (inviteLinkedin:InviteLinkedin), (day:Day)
    		where inviteLinkedin.id = "$inviteId" and day.description="$day"
    		with inviteLinkedin,day
                create (referer:Referer{id:"$referer", sessionId: "$sessionId" })
                with inviteLinkedin,day,referer
                create (inviteLinkedin)-[r:USING_REFERER]->(referer)-[a:ACCEPTED_ON {time:"$time"}]->(day)
    		return r,referer
      """
    // Logger.info("--associateInviteLinkedinReferer["+res+"]")
    res
  }

  def createChangePassword(id: String): String = {
    val t = s"""
	   create (changePassword:ChangePassword {id:"$id",state:"active"})
	   """
    t
  }

  def changePasswordRequest(email: String, day: String, time: String, changePasswordId: String): String = {

    val res = s"""
     	 match (u:User), (d:Day)
	 where u.email = "$email" and d.description="$day"
	 with u,d
	 create (cp:ChangePassword {id:"$changePasswordId", state:"active"})
	 with  u,d,cp
	 create u-[r:CHANGE_PASSWORD_REQUEST]->cp-[m:MADE_ON {time:"$time"}]->d
	 return u,d,cp
     """
    // Logger.info("--change password["+res+"]")
    res
  }

  def updatePassword(cpId: String, day: String, newPassword: String, time: String): String = {

    val res = s"""
      match (cp:ChangePassword), (day:Day)
      where cp.id = "$cpId" and day.description="$day"
      with cp, day
      match a-[s:CHANGE_PASSWORD_REQUEST]-cp
      SET cp.state = "inactive", a.password="$newPassword"
      with cp, day,a
      create cp-[c:CHANGED_ON {time:"$time"}]->day
      return a, cp,day
    """
    //  Logger.info("--update password["+res+"]")
    res
  }

  def deactivatePreviousChangePasswordRequest(email: String): String = {

    val res = s"""
       match (a:User) 
       where a.email="$email"
       match (a)-[cp:CHANGE_PASSWORD_REQUEST]-(c)
       set c.state = "inactive"
       return a,c
    """
    //  Logger.info("--deactivePreviousChangePasswordRequest["+res+"]")
    res
  }

  def updateUserDetails(token: String, firstName: String, lastName: String): String = {
    val res = s"""
      match a-[HAS_TOKEN]-t where t.token = "$token"
      set a.firstName = "$firstName", a.lastName="$lastName"
      return a;
      """
    //   Logger.info("--updateuserdetails["+res+"]")
    res

  }

  def createLocationForStream(token: String, latitude: Double, longitude: Double): String = {

    val res = s"""
      match (a:AuthenticationToken) where a.token = "$token"
      with a
      match s-[u:USING]->a
      where s.state="active"
      with s
      create (s)-[l:LOCATED_AT]->(ul:Location {latitude:$latitude,longitude:$longitude})
      return ul,l,s
      """
    //  Logger.info("---createLocationForStream["+res+"]")
    res
  }

  def associateRoomWithStream(token: String, roomId: String): String = {
    val res = s"""
      match (a:AuthenticationToken) where a.token = "$token"
      with a
      match (s)-[u:USING]->(a)
      where s.state="active"
      with s
      create (s)-[ur:USING_ROOM]->(rm:Room {id:"$roomId"})
      return s,ur,rm
      """
    //  Logger.info("---associateRoomWithStream["+res+"]")
    res
  }

  def invalidateAllStreams(token: String): String = {
    val res = s"""
      match (t:AuthenticationToken) where t.token="$token"
      with t
      match (t)-[u:USING]-(s)
      set s.state = "inactive"
      return s
      """
    //  Logger.info("---invalidateAllStreams["+res+"]")
    res
  }

  def updateUserInformation(token: String, domId: String): String = {
    val res = s"""
      	  match (a)-[ht:HAS_TOKEN]-(b)
     	  where b.valid="true" and b.token="$token"
	  set a.domId = "$domId"
     	  return a.domId
     """
    // Logger.info("--updateUserInformation["+res+"]")
    res

  }


  def deactivateAllStreamActions(inviteId: String): String = {
    val res = s"""
    		match (inv:Invite) 
		where inv.id="$inviteId" 
    		with inv
                match (inv)-[sp:STOPPED_PLAYING|START_PLAYING]->(ss)
		SET ss.state="inactive"
    		return ss
      """
    // Logger.info("--deactivateAllStreamActions["+res+"]")
    res


  }

  def deactivateAllRefererStreamActions(sessionId: String): String = {
    val res = s"""
    		match (referer:Referer) 
		where referer.sessionId="$sessionId" 
    		with referer
                match (referer)-[sp:STOPPED_PLAYING|START_PLAYING]->(ss)
		SET ss.state="inactive"
    		return ss
      """
    // Logger.info("--deactivateAllRefererStreamActions["+res+"]")
    res

  }


  def videoStreamStopped(inviteId: String, day: String, time: String): String = {
    val res = s"""
    		match (inv:Invite) , (day:Day)
		where inv.id="$inviteId" and day.description="$day"
		with inv,day
		create (streamStopped:STREAM_STOPPED  {state:"active"})
    		with streamStopped,inv,day
                create (inv)-[ps:STOP_PLAYING]->(streamStopped)-[o:ON {time:"$time"}]->(day)
    		return inv
      """
    //     Logger.info("--videoStreamStopped["+res+"]")
    res

  }

  def videoStreamStarted(inviteId: String, day: String, time: String): String = {
    val res = s"""
    		match (inv:Invite) , (day:Day)
		where inv.id="$inviteId" and day.description="$day"
		with inv,day
		create (streamStarted:STREAM_STARTED  {state:"active"})
    		with streamStarted,inv,day
                create (inv)-[ps:START_PLAYING]->(streamStarted)-[o:ON {time:"$time"}]->(day)
    		return inv
      """
    //   Logger.info("--videoStreamStarted["+res+"]")
    res

  }


  def videoStreamStartedSocialMedia(sessionId: String, day: String, time: String): String = {
    val res = s"""
    		match (referer:Referer) , (day:Day)
		where referer.sessionId="$sessionId" and day.description="$day"
		with referer,day
		create (streamStarted:STREAM_STARTED  {state:"active"})
    		with streamStarted,referer,day
                create (referer)-[ps:START_PLAYING]->(streamStarted)-[o:ON {time:"$time"}]->(day)
    		return referer
      """
    // Logger.info("--videoStreamStartedSocialMedia["+res+"]")
    res

  }


  def videoStreamStoppedSocialMedia(sessionId: String, day: String, time: String): String = {
    val res = s"""
    		match (referer:Referer) , (day:Day)
		where referer.sessionId="$sessionId" and day.description="$day"
		with referer,day
		create (streamStopped:STREAM_STOPPED  {state:"active"})
    		with streamStopped,referer,day
                create (referer)-[ps:STOP_PLAYING]->(streamStoped)-[o:ON {time:"$time"}]->(day)
    		return referer
      """
    // Logger.info("--videoStreamStoppedSocialMedia["+res+"]")
    res

  }

}