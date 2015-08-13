# waid-graph

## This project is a graph library built using redis. Below are a description of the elements within the graph.

### System Counters
 
 
|Node Type| Node Reference| Example Content|
|:---------------|:-------------|:------------------|
|**\<userId\>**|counter:user|12|
|**\<userTokenId\>**|counter:token:_**\<userId\>**| 434
|**\<userStreamId\>**|counter:stream:_**\<userId\>**|12|
|**\<userInviteId\>**|counter:invite:_**\<userId\>**|23|
|**\<userInviteeId\>**|counter:invitee:_**\<userId\>**|12|

 
### Nodes

|Node Type|Node Reference| Example Content|
|:--------|:--------|:------------------|
|**\<userNodeId\>**|node:user:_**\<userId\>**|`email` k@email.com `password` "password"|
|**\<userTokenNodeId\>**|node:token:_**\<userId\>**_:_**\<userTokenId\>**|  `token` "token_for_1" `state` "valid" |
|**\<userStreamNodeId\>**|node:stream:_**\<userTokenNodeId\>**_:_**\<userStreamId\>**_|`loc` "germany" `streamToken` "streamToken" `creationDate` 20150531 `playList` #count `type` [ethemeral\|persist]|
|**\<userInviteNodeId\>**|node:invite:_**\<userStreamNodeId\>**_:_**\<userInviteId\>**|`token` "inviteToken" `type` "[email\|linkedin\|facebook\|twitter]" `creationDay` "20150835"|
|**\<userInviteeNodeId\>**|node:invitee:_**\<userInviteNodeId\>**|`email` "k@a.com" `creationDate` "20150580" `type` "[email\|linkedin\|facebook\|twitter]"|


### Global Lookup lists

SETS:

This holds all the streams that are created each hour

|Node Reference|Example Content|
|:-----------|:--------------|
|lookup:stream:invitees:**\<epoch_time\>**| **\<streamId:1.1.1\>**|

Hash:  

|Node Reference|Example Content|
|:-------------|:-------------------|
|lookup:valid_tokens| $authenticationToken **\<userTokenNodeId\>**|
|lookup:valid_streams| $streamToken **\<streamNodeId\>**|
|lookup:stream:invites:_**\<userNodeId\>**_**\<inviteNodeId\>**| Not Defined Yet|
|lookup:valid_invites| $inviteToken **\<inviteTokenId\>** |
|lookup:user| $email **\<userNodeId\>**|
|lookup:valid_logins| $email **\<userTokenNodId\>**|

Log facitlity:

|Node Reference|Example Conntent|
|:-------------|:---------------|
|store:allusers_**\<epoch_hour_time\>**| **\<userNodeId\>**«

Index Lookups:


|Node Reference|Example Conntent|
|:-------------|:---------------|
|store:streams:**\<user.id\>**|streamNodeId|
|store:streams:invites:**\<user.id\>**|inviteNodeId|
|store:access_times**\<epoch_hour_time\>**|userTokenId|
|store:logout_times**\<epoch_hour_time\>**|userTokenId|

### Below are some of the scenario for using the graph data.

#### Create Token
Given email,password:

1. lookup:user
  1. If user exists within lookup:users
    1. Fetch user details
      1. User exist
        1. If password match
          1. Create auth token node and set state
          2. Add auth token to list of vald tokens
        2. if password does not match do nothing
      2. User details does not exist
             -- Log error:
             CLEANUP(Potential to hide problems) - remove user details from lookup:users
  2. user does not exist
      Do nothing.

#### Logout:

Given authenticationToken

1. Check if token exists within lookup:valid_tokens
  1. If token exists in lookup:valid_tokens
    1. Get the token and update the state
    2. remove token from lookup:valid_tokens
  2.Do nothing


#### Create user:

Given email, optional (pasword, firstname, lastname)

1. Create a new user
  1. If user does not exist within lookup:user
    1. Create userNode
    2. Create authentication token
    3. Add token to lookup:valid_tokens


#### Create stream:

Given authenticationToken

1. If authentication token is valid:
  1. Create streamNode
  2. Add stream to lookup:valid_streams 

#### End Stream:

Given an authenticationToken, streamTokenId

1. If token is valid
  1. Remove stream from lookup:valid_streams
  2. Remove all invites associated with the stream from lookup:invites_valid:_**\<userId\>**

#### Create an invite

Given an authenticationToken and streamToken and invite type

1. If authenticationToken is valid:
  1. If stream exists within lookup:valid_streams
    1. create inviteNode using the invite type.
    2. add invite node to lookup:stream:invites:_**\<userNodeId\>** **\<inviteNode\>** **\<epochTime\>**
    3. add invite node to lookup:stream:valid $inviteToken **\<inviteNode\>**

#### Join Stream:

Given a inviteToken

1. If token exists within lookup:stream:valid
  1. Extropolate the streamNodeId from the inviteTokenId 
  2. create an inviteeNodeId 
  3. add inviteeNodeId to lookup:invitee:**\<userId\>** 
2. Using the streamId, if the invitee exists within lookup:stream:valid:invitee
  1. If Join stream from linkedin invite
    1. create an invitee
    2. add to lookup:stream:invitees:acecpted:**\<epoch_time\>**:
    3. add to lookup:stream:invitees:valid




