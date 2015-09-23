package com.waid.redis

/**
 * Created by kodjobaah on 15/07/2015.
 */
object KeyPrefixGenerator {

  val RegistrationToken = "registrationToken"


  val Token = "token"
  val Email = "email"
  val FirstName ="firstName"
  val LastName = "lastName"
  val Password = "password"
  val CreatedDate = "createdDate"
  val EndDate = "endDate"
  val PlayListCount = "playListCount"
  val SegmentLocation = "segmentLocation"

  val UserCounter = "counter:user"
  val TokenCounter = "counter:token:_" //counter:token:_<userId>  ==> user specify
  val StreamCounter = "counter:stream:_"// <userStreamId> counter:stream:_<userId>
  val InviteCounter = "counter:invite:_"//<userInviteId> counter:invite:_<userId>
  val InviteeCounter = "counter:invitee:_"//<userInviteeId> counter:invitee:_<userId>

  val UserNodePrefix = "node:user:_"//<userNodeId> node:user:_<userId> email k@email.com password "password"
  val UserTokenNodePrefix ="node:token:_"//<userTokenNodeId> node:token:_<userNodeId>_:_<userTokenId> token "token_for_1" state "valid"
  val UserStreamNodePrefix ="node:stream:"//<userStreamNodeId> node:stream:_<userTokenNodeId>_:_<userStreamId>_ loc "germany" streamToken "streamToken" created
  val UserInviteNodePrefix ="node:invite:_"//<userInviteNodeId> node:invite:_<userStreamNodeId>_:_<userInviteId> token "inviteToken" type="[email|linkedin|facebook|twitter]" creationDay "20150835"
  val UserInviteeNodePrefix = "node:invitee:_"//<userInviteeNodeId> node:invitee:_<userInviteNodeId> email "k@a.com" creationDate "20150580" type="[email|linkedin|facebook|twitter]"

  val LookupCurrentToken = "lookup:currentToken"  //lookup:currentToken $email $registrationToken

  val LookupLostPassword  = "lookup:forgottopassword" // lookup:forgottenpassword $email $token
  val LookupRegistration = "lookup:registration" //lookup:registration $registrationToken <userNodeId>
  val LookupValidTokens = "lookup:valid_tokens" //lookup:valid_tokens $authenticationToken <userTokenNodeId>
  val LookupValidStreams = "lookup:valid_streams"//lookup:valid_streams $streamToken <streamNodeId>
  var LookupValidStreamsEmail = "lookup:valid_streams_email" //lookup:valid_streams email $streamToken
  val LookupStreamInvites = "lookup:stream:invites:_"//lookup:stream:invites:_<userNodeId> <inviteNodeId> <epochTime>
  val LookupValidInvites = "lookup:valid_invites"//lookup:valid_invites $inviteToken <inviteTokenId>
  val LookupUser ="lookup:user" //lookup:user $email <userNodeId>
  val LookupValidLogins = "lookup:valid_logins"//lookup:valid_logins $email <userTokenNodId>
  val LookupValidLoginsEmail = "lookup:valid_logins_email"//lookup:valid_logins $email <userTokenNodId>
  val LookupLiveStreams = "lookup:stream:live"   //lookup:stream:live:$streamTokenId   $viewerToken lastSequnceNumber
  val LookupAllStreams = "lookup:stream:all:"   //lookup:stream:all/_<userId_

  val StoreInvitesEmail = "store:invites:email:" //store:invites:email:streamNodeId <email>


  val StoreAllUsers = "store:allusers:"//store:allusers:<epoch_hour_time>: <userNodeId>
  val StoreStreams = "store:streams:_" //store:streams:<user.id> streamNodeId
  val StoreStreamsInvits ="store:streams:invites:"//store:streams:invites:<user.id> inviteNodeId
  val StoreAccessTimes = "store:access_times:"//store:access_times<epoch_hour_time> userTokenId
  val StoreLogoutTimes = "store:logout_times:"//store:logout_times:"<epoch_hour_time> userTokenId
  val StoreRegistrationDate = "store:registration:" //store:registration:<epoch_hour_time> userTokenId $epoch_time
  val StoreRegistrationAcceptance = "store:registration:acceptance" //store:registration:acceptance:<epoch_hour_time> userTokenId $epochTime
}
