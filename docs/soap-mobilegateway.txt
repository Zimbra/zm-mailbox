Zimbra Mobile Gateway SOAP API Extensions
-----------------------------------------

The very first request that an app needs to send to ZMG is the BootstrapMobileGatewayAppRequest.
The response would include a unique app ID, an app key (or a secret) to enable the app to authenticate itself in the
future, as well as an "app auth token".


<BootstrapMobileGatewayAppRequest xmlns="urn:zimbraAccount" [wantAppToken="0|1"]>
</BootstrapMobileGatewayAppRequest>

<BootstrapMobileGatewayAppResponse xmlns="urn:zimbraAccount">
  <appId>{app-uuid}</appId>
  <appKey>{app-key}</appKey>
  [<authToken lifetime="{auth-token-lifetime}">{app-auth-token}</authToken>]
</BootstrapMobileGatewayAppResponse>


The app should persist the app uuid and the app key on the device in a secure manner.

----

In case the app does not have a Zimbra user auth token, it can still add a third-party email account (like gmail)
using the CreateDataSourceRequest API (refer base SOAP API) and passing its app token.
The app is allowed to specify the folder id as "-1" to indicate that the server should auto-create the folder into
which the external data would be imported. The created folder's id would be returned in the response.

<CreateDataSourceRequest/>
  <imap ... [l="-1"] .../>
</CreateDataSourceRequest>

<CreateDataSourceResponse>
  <imap id="{id}" [l="{folder-id}"]/>
</CreateDataSourceResponse>

The client may also need to send ImportDataRequest (refer base SOAP API) to trigger sync with the external data source.
By default the server does not initiate the data sync on its own (there's a server setting to specify an automatic
sync interval though). The sync can be triggered anytime on-demand by invoking ImportDataRequest.

----

If the app auth token expires, the app can request a new auth token.

<RenewMobileGatewayAppTokenRequest xmlns="urn:zimbraAccount">
  <appId>{app-uuid}</appId>
  <appKey>{app-key}</appKey>
</RenewMobileGatewayAppTokenRequest>

<RenewMobileGatewayAppTokenResponse xmlns="urn:zimbraAc count">
  <authToken lifetime="{auth-token-lifetime}">{app-auth-token}</authToken>
</RenewMobileGatewayAppTokenResponse>


------ Push Notifications for Android apps -----

Push notifications for Android Apps using Google Cloud Messaging - 

For push notifications using GCM, a project needs to be created using Google APIs console page. 
The “Project Number” aka sender id of this project is used by android client to register itself 
for GCM. After registering itself, the android client receives a registration Id.
The android client uploads this registration id with its messaging server. The server will use
the registration id to send notifications to that device. Along with registration id, the server also needs a
Server API key for sending notifications, the server API key is also created while creating the project
on Google APIs console
Following attributes have been introduced for configuring the server for sending Push Notifications to
Android devices

1. zimbraGCMSenderId
Value can be set using the following command -
zmprov mcf zimbraGCMSenderId <GCM Sender Id>

2. zimbraGcmAuthorizationKey
Value can be set using the following command -
zmprov mcf zimbraGcmAuthorizationKey <GCM Authorization Key>

Please refer the following links for detailed information about Google Cloud Messaging - 

http://www.androidhive.info/2012/10/android-push-notifications-using-google-cloud-messaging-gcm-php-and-mysql/

http://developer.android.com/google/gcm/gs.html


Android client can get GCM sender id by using the following request/response:

<GetGcmSenderIdRequest xmlns="urn:zimbraAccount">
</GetGcmSenderIdRequest>

<GetGcmSenderIdResponse xmlns="urn:zimbraAccount">
  <gcmSenderId>{sender-id}<gcmSenderId/>
</GetGcmSenderIdResponse>


Registering app/device to receive push notifications:

Android clients using Google Cloud messaging should specify "gcm" as pushProvider.

<RegisterMobileGatewayAppRequest xmlns="urn:zimbraAccount">
  <zmgDevice appId="{appId}" registrationId="{registrationId}" pushProvider="gcm"
  osName="{ios | android}" osVersion="{osVersion number}" maxPayloadSize={maxPayloadSize in bytes}/>
</RegisterMobileGatewayAppRequest>

Note:
1. osName is the name of the operating system installed on the device. Example - ios, android
2. osVersion should be specified in the following formats -
    a) majorVersion.minorVersion.microVersion
    b) majorVersion.minorVersion

    Example - iOS having versions like 7.0, 8.0.3, 8.1 etc
              android has OS version like 2.0, 3.1, 4.4, 5.0 etc

3. maxPayloadSize is the maximum number of bytes allowed for the push notification payload
   Example - iOS 7.0 maxPayloadSize is 256 bytes
             iOS 8.0 onwards maxPayloadSize is 2048 bytes
             Android maxPayloadSize is 4096 bytes

<RegisterMobileGatewayAppResponse xmlns="urn:zimbraAccount"/>

------ Push Notifications for iOS apps -----

iOS clients should specify "apns" as pushProvider.
RegisterMobileGatewayAppRequest can be used to register an iOS device token.

<RegisterMobileGatewayAppRequest xmlns="urn:zimbraAccount">
  <zmgDevice appId="{appId}" registrationId={device-token} pushProvider="apns"/>
</RegisterMobileGatewayAppRequest>

Following attributes have been introduced for configuring the server for sending Push Notifications to
iOS devices -

  1. zimbraAPNSCertificate
    Certificate for Apple Push Notification Service
    Value can be set using the following command -
    zmprov mcf zimbraAPNSCertificate <path-of-p12-file>

  2. zimbraAPNSCertificatePassword
    Password for APNS certificate
    Value can be set using the following command -
    zmprov mcf zimbraAPNSCertificatePassword <password>

  3. zimbraAPNSProduction
    Boolean to decide whether APNS is being used for production or development
    Value can be set using the following command -
    zmprov mcf zimbraAPNSProduction {TRUE|FALSE}

Please follow the steps given in the link below to generate certificate

http://www.raywenderlich.com/32960/apple-push-notification-services-in-ios-6-tutorial-part-1

https://developer.apple.com/library/ios/documentation/IDEs/Conceptual/AppDistributionGuide/ConfiguringPushNotifications/ConfiguringPushNotifications.html


---- New message push notification ----

Payload of a new message GCM push notification contains -

cid  - conversationId
su - subject of the message
sa - sender email address
sdn - sender display name
fr - preview text of the conversation
ra - recipient email address
uc - unread count of the meesages in this folder
ac - action that happened in this notification
ty - type of the object in this notification
id - id of the object in this notification


Notifications are sent in json format. Following are sample json notifications -

GCM -

{"data":{"su":"afadfadfgdg","ac":"CreateMessage","ty":"MESSAGE","id":291,
"sdn":"<sender name>","fr":"asdfdsgdg","sa":"<sender address>","uc":26,"cid":-291,
"ra":"<recipient address>"},"registration_ids":["..."]}

APNS -

{"ac":"CreateMessage","aps":{"badge":26,"alert":"From: <sender name>\n<subject>","sound":"default"},
"ty":"MESSAGE","id":291,"sdn":"<sender name>","fr":"asdfdsgdg","sa":"<sender address>",
"cid":-291,"ra":"<recipient address>"}

In case of iOS versions below 8.0, payload is - 

{"aps":{"badge":26,"alert":"From: <sender name>\n<subject>","sound":"default"},
"id":291,"cid":-291}


------ ZMG "Proxy" mode -----

ZMG can be configured to act as a "Proxy" to another Zimbra system. In the Proxy mode, some accounts in the ZMG could
be syncing email data from a Zimbra account hosted on a different Zimbra system into a data source.

The client can send user credentials on the target system inside the <AuthRequest>. The <AuthResponse> would contain an
additional flag to indicate that the authenticated account corresponds to a Proxy one:

<AuthResponse zmgProxy="0|1">
  ...
</AuthResponse>

In this scenario there would be a data source object that is part of the authenticated account and where the proxied
account data is synced. The data source would have an additional "zimbraDataSourceIsProxy" provisioning attribute set
on it with value TRUE.


