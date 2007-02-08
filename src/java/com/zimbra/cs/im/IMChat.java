/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import javax.mail.MessagingException;

import org.xmpp.muc.JoinRoom;
import org.xmpp.muc.LeaveRoom;
import org.xmpp.packet.JID;
import org.xmpp.packet.Presence;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ClassLogger;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.im.IMMessage.Lang;
import com.zimbra.cs.im.IMMessage.TextPart;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.Zimbra;

/**
 *
 */
public class IMChat extends ClassLogger {

    public static class Participant {
        private IMAddr mAddress;
        private String mName;
        private String mResource;

        private void init(IMAddr address, String resource, String name)
        {
            mAddress = address;
            mName = name;
            mResource = resource;
        }

        public IMAddr getAddress() { return mAddress; }
        public String getName()    { return mName; }
        public String mResource()  { return mResource; }

        public Participant(IMAddr address) {
            init(address, null, null);
        }
        public Participant(IMAddr address, String resource, String name) {
            init(address, resource, name);
        }
    }

    /**
     * If true, then this is a MUC (XEP-0045) session, otherwise it is normal 1:1 chat
     */
    private boolean mIsMUC = false;

    /**
     * Sequence # of the first message on the list...this is here so that we can (if we want to, we don't currently) 
     * truncate mMessages when it gets large (to save memory)
     */
    private int mFirstSeqNo = 0;

    /**
     * The highest seq ID when we last flushed
     */
    private int mLastFlushSeqNo = -1;

    /**
     * @return the sequence no of the first message in mMessages
     */
    public int getFirstSeqNo() { return mFirstSeqNo; }

    /**
     * @return the sequence no of the last message in mMessages
     */
    public int getHighestSeqNo() { return mFirstSeqNo + mMessages.size(); }

    private List<IMMessage> mMessages = Collections.synchronizedList(new ArrayList<IMMessage>());
    private String mThreadId;
    private boolean mIsClosed = false;
    private Map<IMAddr, Participant> mParticipants = Collections.synchronizedMap(new HashMap<IMAddr, Participant>());
    private Mailbox mMailbox;
    private IMPersona mPersona;
    private int mSavedChatId = -1;
    private JID mDestJid = null; // the JID we send to: a remote user, or a chatroom if this is a MUC

    static enum TIMER_STATE {
        WAITING_TO_CLOSE,
        WAITING_TO_SAVE,
        NONE;
    }

    private TIMER_STATE mTimerState = TIMER_STATE.NONE;
    private FlushTask mTimer = null;
    static final int sSaveTimerMs =  10 * 60 * 1000; // 1 min
    static final int sCloseTimerMs = 10 * 60 * 1000; // 10 min
    
    boolean isMUC() { return mIsMUC; }
    
    @Override
    protected Object formatObject(Object o) {
        if (o instanceof org.xmpp.packet.Packet)
            return ((org.xmpp.packet.Packet) o).toXML();
        else
            return super.formatObject(o);
    }
    @Override
    protected String getInstanceInfo() {
        return toString();
    }

    IMChat(Mailbox mbox, IMPersona persona, String threadId, Participant initialPart)
    {
        super(ZimbraLog.im);
        
        mMailbox = mbox;
        mPersona = persona;
        mThreadId = threadId;
        mParticipants.put(initialPart.getAddress(), initialPart);
        mDestJid = initialPart.getAddress().makeJID();
        mIsMUC = false;
    }

    void closeChat() {
        if (isMUC()) {
            LeaveRoom l = new LeaveRoom(mPersona.getFullJidAsString(), getMUCJidWithNickname());
            mPersona.xmppRoute(l);
        }
        enableTimer(TIMER_STATE.NONE);
        if (!mIsClosed) {
            flush();
            mIsClosed = true;
        }
    }

    /*
    MUC Example 137. Service Sends Configuration Form after room opened:
    <iq from='darkcave@macbeth.shakespeare.lit'
       id='create1'
       to='crone1@shakespeare.lit/desktop'
       type='result'>
       <query xmlns='http://jabber.org/protocol/muc#owner'>
         <x...>*
       </query>
     </iq>    

    MUC Example 140. Service Informs New Room Owner of Success:
    <iq from='darkcave@macbeth.shakespeare.lit'
        id='create2'
        to='crone1@shakespeare.lit/desktop'
        type='result'/>

    MUC Example 141. Service Informs Owner that Requested Configuration Options Are Unacceptable:
    <iq from='darkcave@macbeth.shakespeare.lit'
        id='create2'
        to='crone1@shakespeare.lit/desktop'
        type='error'>
      <error type='modify'>
        <not-acceptable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
      </error>
    </iq>
     */
    void handleIQPacket(org.xmpp.packet.IQ iq) {
        info("Got an IQ packet for chat: ", iq);
    }

    /*
    MUC Example 132. Service Informs User of Inability to Create a Room:
    <presence
        from='CHATROOM@conference.host.com/NICKNAME'
        to='hag66@shakespeare.lit/pda'
        type='error'>
      <error code='405' type='cancel'>
        <not-allowed xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
      </error>
    </presence>

    MUC Example 18. Service Sends Presence from Existing Occupants to New Occupant:
    <presence
        from='CHATROOM@conference.host.com/NICKNAME'
        to='hag66@shakespeare.lit/pda'>
      <x xmlns='http://jabber.org/protocol/muc#user'>
        <item affiliation='owner' role='moderator'/>
      </x>
    </presence>
    <presence
        from='CHATROOM@conference.host.com/NICKNAME'
        to='hag66@shakespeare.lit/pda'>
      <x xmlns='http://jabber.org/protocol/muc#user'>
        <item affiliation='admin' role='moderator'/>
      </x>
    </presence>

    MUC Example 38. Service Sends Presence Related to Departure of Occupant:
    <presence
        from='CHATROOM@conference.host.com/NICKNAME'
        to='hag66@shakespeare.lit/pda'
        type='unavailable'>
      <x xmlns='http://jabber.org/protocol/muc#user'>
        <item affiliation='member' role='none'/>
      </x>
    </presence>
     */    
    void handlePresencePacket(boolean toMe, org.xmpp.packet.Presence pres) {
        info("Got a presence update packet for chat: %s", pres);
        if (isMUC()) {
            if (pres.getType() == org.xmpp.packet.Presence.Type.error) {
                addMessage(true, new IMAddr(pres.getFrom()), null, "ERROR: "+pres.toXML()); 
            } else {
                JID  fromFullJID = null;
                IMAddr fromNick = new IMAddr(pres.getFrom().getResource());
                
                // find the full jid if available
                org.dom4j.Element x;
                if ((x = pres.getChildElement("x", "http://jabber.org/protocol/muc#user")) != null) {
                    org.dom4j.Element item = x.element("item");
                    if (item != null) {
                        fromFullJID = new JID(item.attributeValue("jid"));
                    }
                }

                String action = "entered";
                if (pres.getType()==Presence.Type.unavailable) { 
                    action = "left";
                    removeParticipant(fromNick);
                    if (fromFullJID != null) 
                        removeParticipant(new IMAddr(fromFullJID));
                } else {
                    if (fromFullJID != null) {
                        getParticipant(true, new IMAddr(fromFullJID), fromFullJID.getResource(), fromFullJID.getNode());
                    } else {
                        getParticipant(true, fromNick, "", fromNick.toString());
                    }
                }
                String body = new Formatter().format("%s has %s the chat.",
                            pres.getFrom().getResource(), action).toString();
                addMessage(true, new IMAddr(pres.getFrom()), null, body);
            }
        }
    }

    /*
    MUC Example 32. Delivery of Discussion History:
    <message
        from='darkcave@macbeth.shakespeare.lit/firstwitch'
        to='hecate@shakespeare.lit/broom'
        type='groupchat'>
      <body>Thrice the brinded cat hath mew'd.</body>
      <x xmlns='jabber:x:delay'
         from='crone1@shakespeare.lit/desktop'
         stamp='20021013T23:58:37'/>
    </message>

    MUC Example 47. Room Sends Invitation to Invitee on Behalf of Invitor:
    <message
        from='darkcave@macbeth.shakespeare.lit'
        to='hecate@shakespeare.lit'>
      <body>You have been invited to darkcave@macbeth by crone1@shakespeare.lit.</body>
      <x xmlns='http://jabber.org/protocol/muc#user'>
        <invite from='crone1@shakespeare.lit'>
          <reason>
            Hey Hecate, this is the place for all good witches!
          </reason>
        </invite>
        <password>cauldronburn</password>
      </x>
    </message>

    MUC Example 49. Room Informs Invitor that Invitation Was Declined:
    <message
        from='darkcave@macbeth.shakespeare.lit'
        to='crone1@shakespeare.lit/desktop'>
      <x xmlns='http://jabber.org/protocol/muc#user'>
        <decline from='hecate@shakespeare.lit'>
          <reason>
            Sorry, I'm too busy right now.
          </reason>
        </decline>
      </x>
    </message>
     */    
    /**
     * @param toMe
     * @param msg
     * @return FALSE if the chat should be removed 
     */
    void handleMessagePacket(boolean toMe, org.xmpp.packet.Message msg) 
    {
        int seqNoStart = this.getHighestSeqNo();
        try {
            String mucInvitationFrom = null;
            String messageFrom = msg.getFrom().toBareJID();
            String subject = msg.getSubject();
            String body = null;
            boolean isError = false;
            if (msg.getType() == org.xmpp.packet.Message.Type.error) {
                isError = true;
                body = "ERROR: "+msg.toXML();
            } else {
                body = msg.getBody();

                org.dom4j.Element xElt = null;                 
                if ((xElt = msg.getChildElement("x", "http://jabber.org/protocol/muc#user"))!=null) { 
                    org.dom4j.Element inviteElement = xElt.element("invite");
                    if (inviteElement != null) {
                        mucInvitationFrom = inviteElement.attributeValue("from");
                        if (body == null || body.length()==0) {
                            org.dom4j.Element reason = inviteElement.element("reason");
                            if (reason != null) 
                                body = reason.getText() + " (/join "+msg.getFrom().toBareJID()+")";
                            else
                                body = mucInvitationFrom + " has invited you into a groupchat "+ " (/join "+msg.getFrom().toBareJID()+")";
                        }
                    }
                }
            }
            
            if (mIsMUC && messageFrom.equals(mDestJid.toBareJID())) {
                String resource = msg.getFrom().getResource();
                if (resource != null && resource.length() > 0) {
                    messageFrom = msg.getFrom().getResource();
                    if (!isError && mPersona.getAddr().getNode().equals(messageFrom)) {
                        info("Skipping MUC message from me: %s", msg);
                        return;
                    }
                } else {
                    debug("Message is from Conference itself"); 
                }
            }
            
            if (mucInvitationFrom == null && 
                        (subject == null || subject.length() == 0) &&
                        (body == null || body.length() == 0)) 
            {
                // ignore empty message for now (<composing> update!)
                ZimbraLog.im.debug("Ignoring empty <message>  (composing update): %s"+msg.toXML());
            } else {
                
                if (mucInvitationFrom != null && mMessages.size() > 0) {
                    if (mucInvitationFrom != null) {
                        systemNotification("Automatically joining converted multi-user-chat");
                        joinMUCChat(msg.getFrom().toBareJID());
                    }
                } else {
                    IMChatInviteNotification inviteNot = 
                        new IMChatInviteNotification(new IMAddr(msg.getFrom().toBareJID()),
                                    getThreadId(), body);
                    mPersona.postIMNotification(inviteNot);
                    addMessage(true, new IMAddr(messageFrom), subject, body);
                    assert(this.getHighestSeqNo() > seqNoStart);
                }
            }
        } finally {
            if (this.getHighestSeqNo() > seqNoStart) {
                enableTimer(TIMER_STATE.WAITING_TO_SAVE);
            } else {
                enableTimer(TIMER_STATE.WAITING_TO_CLOSE);
            }
        }
    }
    
    /**
     * Send a notification message in the current thread, but only to only my client
     * @param text
     */
    void systemNotification(String text) {
        IMMessage message = new IMMessage(null, new TextPart(text));
        
        IMMessageNotification notification = new IMMessageNotification(new IMAddr("SYSTEM"), getThreadId(), message, 0);
        mPersona.postIMNotification(notification);
    }
    
    void sendPresenceUpdate(Presence pres) {
        if (mIsMUC) {
            Presence toSend = pres.createCopy();
            toSend.setFrom(mPersona.getFullJidAsString());
            toSend.setTo(getMUCJidWithNickname());
            mPersona.xmppRoute(toSend);
        }
    }

    void sendMessage(OperationContext octxt, IMAddr toAddr, String threadId, IMMessage message) throws ServiceException 
    {
        IMAddr fromAddr = mPersona.getAddr();
        message.setFrom(fromAddr);
        addMessage(true, message);

        org.xmpp.packet.Message xmppMsg = new org.xmpp.packet.Message();
        xmppMsg.setFrom(fromAddr.makeFullJID());
        xmppMsg.setTo(mDestJid);
        if (mIsMUC) {
            xmppMsg.setType(org.xmpp.packet.Message.Type.groupchat);
        } else {
            xmppMsg.setThread(this.getThreadId());
            xmppMsg.setType(org.xmpp.packet.Message.Type.chat);
        }

        if (message.getBody(Lang.DEFAULT) != null)
            xmppMsg.setBody(message.getBody(Lang.DEFAULT).getPlainText());

        if (message.getSubject(Lang.DEFAULT) != null)
            xmppMsg.setSubject(message.getSubject(Lang.DEFAULT).getPlainText());

        mPersona.xmppRoute(xmppMsg);
    }
    
    private String getMUCJidWithNickname() {
        if (!isMUC())
            throw new IllegalStateException("MUC mode only");
        
        String myNickname = mPersona.getAddr().getNode();
        assert(myNickname != null && myNickname.length() > 0);
        return mDestJid.toString()+"/"+myNickname;
    }

    private void requestCreateChatroom() throws ServiceException {
        mDestJid = new JID(getThreadId(), mPersona.getMucDomain(), "");
        mIsMUC = true;

        org.xmpp.muc.JoinRoom createRoom = new org.xmpp.muc.JoinRoom(mPersona.getFullJidAsString(), getMUCJidWithNickname());
        mPersona.xmppRoute(createRoom);
        
        /* yuk: MUC: 10.1.2 Creating an Instant Room

            If the initial room owner wants to accept the default room configuration (i.e., create an "instant room"), 
            the room owner MUST decline an initial configuration form by sending an IQ set to the <room@service> itself
            containing a <query/> element qualified by the 'http://jabber.org/protocol/muc#owner' namespace, where 
            the only child of the <query/> is an empty <x/> element that is qualified by the 'jabber:x:data' namespace
             and that possesses a 'type' attribute whose value is "submit":
         */
        org.xmpp.muc.RoomConfiguration rc = new org.xmpp.muc.RoomConfiguration(new HashMap<String, Collection<String>>());
        rc.setTo(getMUCJidWithNickname());
        mPersona.xmppRoute(rc);
    }

    void joinMUCChat(String roomAddr) {
        /*
        MUC Example 15. Jabber User Seeks to Enter a Room (Groupchat 1.0)
        <presence
            from='hag66@shakespeare.lit/pda'
            to='darkcave@macbeth.shakespeare.lit/thirdwitch'/>
         */
        this.mIsMUC = true;
        mDestJid = new JID(roomAddr);
        JoinRoom join = new JoinRoom(mPersona.getFullJidAsString(), getMUCJidWithNickname());
        mPersona.xmppRoute(join);
        debug("Join MUC: DestJID=%s",mDestJid);
    }

    void addUserToChat(IMAddr addr, String inviteMessage) throws ServiceException {
        if (!this.mIsMUC) {
            // convert chat to MUC
            requestCreateChatroom();
            
            for (IMAddr currentAddr : this.mParticipants.keySet()) {
                // Invite existing users to chat....
                if (!currentAddr.equals(mPersona.getAddr()) && !currentAddr.equals(addr)) {
                    org.xmpp.muc.Invitation invite = new org.xmpp.muc.Invitation(currentAddr.toString(), inviteMessage);
                    invite.setTo(mDestJid);
                    mPersona.xmppRoute(invite);
                }
            }
        }
        // add user to chat
        org.xmpp.muc.Invitation invite = new org.xmpp.muc.Invitation(addr.toString(), inviteMessage);
        invite.setTo(mDestJid);
        mPersona.xmppRoute(invite);
    }
    
    @Override
    public String toString() {
        StringBuilder partsStr = new StringBuilder();
        boolean atStart = true;
        for (Participant part : mParticipants.values()) {
            if (!atStart)
                partsStr.append(", ");
            else
                atStart = false;
            partsStr.append(part.getAddress());
        }
        
        return new Formatter().format("IMChat(%s%s, dest=%s, participants={%s})",
                    mThreadId, (mIsMUC ? ", MUC" : ""), mDestJid, partsStr.toString()).toString();
    }

    public String getThreadId() { return mThreadId; }
    
    Participant getParticipant(IMAddr addrFrom) {
        return this.getParticipant(false, addrFrom, null, null);
    }

    private Participant getParticipant(boolean create, IMAddr addrFrom, String resourceFrom, String nameFrom)
    {
        Participant part = mParticipants.get(addrFrom);
        if (create && part == null) {
            part = new Participant(addrFrom, resourceFrom,  nameFrom);
            mParticipants.put(addrFrom, part);
            IMEnteredChatNotification entered = new IMEnteredChatNotification(addrFrom, getThreadId());
            mPersona.postIMNotification(entered);
        }
        return part;
    }
    
    private Participant removeParticipant(IMAddr addr) {
        Participant p = mParticipants.remove(addr);
        if (p != null) {
            IMLeftChatNotification left = new IMLeftChatNotification(addr, getThreadId());
            mPersona.postIMNotification(left);
        }
        return p;
    }

    public Collection<Participant> participants() {
        return Collections.unmodifiableCollection(mParticipants.values());
    }

    public List<IMMessage> messages() {
        return Collections.unmodifiableList(mMessages);
    }

    /**
     * Message from us
     * 
     * @param msg
     */
    private void addMessage(boolean notify, IMAddr from, String subject, String body)
    {
        IMMessage immsg = new IMMessage(subject==null?null:new TextPart(subject),
                    body==null?null:new TextPart(body));
        immsg.setFrom(from);
        addMessage(notify, immsg);
    }

    private void addMessage(boolean notify, IMMessage immsg) {
        int seqNoStart = this.getHighestSeqNo();
        try {
            // will trigger the add of the participant if necessary
            //this.getParticipant(true, immsg.getFrom(), null, null);
            mMessages.add(immsg);

            assert(this.getHighestSeqNo() > seqNoStart);
            int seqNo = mMessages.size()+mFirstSeqNo;

            if (notify) {
                IMMessageNotification notification = new IMMessageNotification(immsg.getFrom(), getThreadId(), immsg, seqNo);
                mPersona.postIMNotification(notification);
            }
        } finally {
            if (this.getHighestSeqNo() > seqNoStart) {
                enableTimer(TIMER_STATE.WAITING_TO_SAVE);
            } else {
                enableTimer(TIMER_STATE.WAITING_TO_CLOSE);
            }
        }
    }

    /**
     * Set the close/save timer to the appropriate state, depending on what
     * state it currently is in...
     * 
     * @param requestedState
     */
    private void enableTimer(TIMER_STATE requestedState) {
        ZimbraLog.im.debug("Chat + " +this.getThreadId() + " setting timer to " +requestedState);
        //                   requested
        // current:                        CLOSE           SAVE          NONE 
        //                     CLOSE         nop              nop           start-close
        //                     SAVE          start-save    nop           start-save  
        //                     NONE          cancel          cancel       none
        //
        switch (requestedState) {
            case WAITING_TO_CLOSE:
                switch (mTimerState) {
                    case WAITING_TO_CLOSE:
                        break;
                    case WAITING_TO_SAVE:
                        break;
                    case NONE:
                        startCloseTimer();
                        break;
                }
                break;
            case WAITING_TO_SAVE:
                switch (mTimerState) {
                    case WAITING_TO_CLOSE:
                        startSaveTimer();
                        break;
                    case WAITING_TO_SAVE:
                        break;
                    case NONE:
                        startSaveTimer();
                        break;
                }
                break;
            case NONE:
                switch (mTimerState) {
                    case WAITING_TO_CLOSE:
                    case WAITING_TO_SAVE:
                        if (mTimer != null) {
                            mTimer.cancel();
                            mTimer = null;
                        }
                        mTimerState = TIMER_STATE.NONE;
                        break;
                    case NONE:
                        break;
                }
                break;
        }
    }

    private void timerExecute() {
        synchronized(mMailbox ) {

            ZimbraLog.im.debug("ImChat.TimerExecute "+mTimerState);
            mTimer = null; // it is firing!  don't cancel it!
            switch (mTimerState) {
                case WAITING_TO_CLOSE:
                    mPersona.closeChat(null, this);
                    break;
                case WAITING_TO_SAVE:
                    startCloseTimer();
                    flush();
                    break;
                case NONE:
                    // nop
                    break;
            }
        }
    }

    private void startSaveTimer() {
        mTimerState = TIMER_STATE.WAITING_TO_SAVE;
        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = new FlushTask();
        Zimbra.sTimer.schedule(mTimer, sSaveTimerMs);
    }

    private void startCloseTimer() {
        mTimerState = TIMER_STATE.WAITING_TO_CLOSE;
        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = new FlushTask();
        Zimbra.sTimer.schedule(mTimer, sCloseTimerMs);
    }


    private class FlushTask extends TimerTask {
        @Override
        public void run() {
            timerExecute();
        }
    }
    
    /**
     * Write this chat as a MimeMessage into the user's IMs folder
     */
    private void flush() {
        if (mLastFlushSeqNo >= getHighestSeqNo())
            return;

        ZimbraLog.im.info("Flushing chat: "+toString());

        try {
            ParsedMessage pm  = ChatWriter.writeChat(this);
            Message msg;
            try {
                msg = mMailbox.updateOrCreateChat(null, pm, mSavedChatId);
            } catch(NoSuchItemException e) {
                // they deleted the chat from their mailbox.  Bad user.
                msg = mMailbox.updateOrCreateChat(null, pm, -1);
            }
            mSavedChatId = msg.getId();
        } catch (ServiceException e) {
            System.out.println("Caught ServiceException " + e);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Caught IO exception " + e);
            e.printStackTrace();
        } catch (MessagingException e) {
            System.out.println("Caught messaging exception " + e);
            e.printStackTrace();
        }

        mLastFlushSeqNo = getHighestSeqNo();
    }

}
