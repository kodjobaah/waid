/**
 * Created with IntelliJ IDEA.
 * User: whatamidoing
 * Date: 04/12/13
 * Time: 05:08
 * To change this template use File | Settings | File Templates.
 */
object DataForTestInvites extends App {
  import com.whatamidoing.cypher.CypherWriterFunction._
  import com.whatamidoing.cypher.CypherReaderFunction._

 // The following are the test cases that need to be created in order to support fetching users who are watching the stream:

  //1: Create Stream User:

 var res =  createUser("streamuser-first-name","streamuser-last-name", "StreamUser@streamuser.com","password-hash")
 var value = res()
 val token = getUserToken("StreamUser@streamuser.com")
 val tokenResult = token()
 val userToken = tokenResult.results.head.asInstanceOf[(String, String)]._1
 res =  createStream("streamForInvitedAcceptance",userToken)
 value = res()

 // 2: Create Invited Users:

  res = createUser("inviteduseraccpted-one-first-name","inviteduseraccepted-one-last-name","inivteduseracceptedone@acceptedone.com","password")
  value  = res()
  res = createUser("inviteduseraccpted-two-first-name","inviteduseraccepted-two-last-name","inivteduseracceptedtwo@acceptedtwo.com","password")
  value = res()
  res = createUser("invitedusernotaccpted-one-first-name","invitedusernotaccepted-one-last-name","inivtedusernotacceptedone@notacceptedone.com","password")
  value =res()
  res = createUser("invitedusernotaccpted-two-first-name","invitedusernotaccepted-two-last-name","inivtedusernotacceptedtwo@notacceptedtwo.com","password")
  value = res()

  //3: Create 4 Invites

  res =  createInvite("streamForInvitedAcceptance", "inivteduseracceptedone@acceptedone.com", "inviteAcceptanceOne")
  value = res()
  res = createInvite("streamForInvitedAcceptance", "inivteduseracceptedtwo@acceptedtwo.com", "inviteAcceptanceTwo")
  value = res()
  res = createInvite("streamForInvitedAcceptance", "inivtedusernotacceptedone@notacceptedone.com", "inviteNotAcceptanceOne")
  value = res()
  res = createInvite("streamForInvitedAcceptance", "inivtedusernotacceptedtwo@notacceptedtwo.com", "inviteNotAcceptanceTwo")
  value = res()
  //4: Accept Invites
  res = associateDayWithInvite("inviteAcceptanceOne")
  value = res()
  res = associateDayWithInvite("inviteAcceptanceTwo")
  value = res()


  println("done creating invite data")

}
