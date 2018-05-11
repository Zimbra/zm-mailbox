# Webhook Setup

## HipChat Integration

    1. Login to Hipchat on the web and click on *Integrations*.
    2. Click and type 'Zimbra Infrastructure Room' into the choose your room.
    3. Click on `Build Your Own Integration`
    4. Create a name for your integration - CircleCI 2.0 and push *Create*
    5. Add a command extension: /build-status
    6. Specify the target URI as: http://<server>:5000/post-status
    7. Copy the value of "Send messages to this room by posting to this URL" in to the config.json file.
       This value will be read in and used to post the build completion messages to the specified channel.
       Note: This file is created in the WebHook Configuration section below.
       
## CircleCI 2.0 Installation

    
## WebHook Configuration

    Create an empty config.json file and/or run the start_server.sh script once to create an initial file:
    
        {"URL": "<HipChat Room Notification URL goes here>",
         "auth_token": "<auth token from Hipchat Integration goes here"}
         
     The values that go in this configuration file were created in the HipChat Integration section above.


