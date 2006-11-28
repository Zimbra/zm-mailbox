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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.im.IMGetChat;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.Element;

/**
 * @author tim
 *
 */
/**
 * @author tim
 *
 */
public class IMChat {
    
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
    private int mDraftId = -1;
    
    static enum TIMER_STATE {
        WAITING_TO_CLOSE,
        WAITING_TO_SAVE,
        NONE;
    }
    
    private TIMER_STATE mTimerState = TIMER_STATE.NONE;
    private FlushTask mTimer = null;
    static final int sSaveTimerMs =  5  * 1000; // 5 sec
    static final int sCloseTimerMs = 30 * 1000; // 30 sec
    

    IMChat(Mailbox mbox, IMPersona persona, String threadId, Participant initialPart)
    {
        mMailbox = mbox;
        mPersona = persona;
        mThreadId = threadId;
        mParticipants.put(initialPart.getAddress(), initialPart);
    }
    
    void closeChat() {
        enableTimer(TIMER_STATE.NONE);
        if (!mIsClosed) {
            flush();
            mIsClosed = true;
        }
    }
    
    private void enableTimer(TIMER_STATE requestedState) {
        ZimbraLog.im.info("Chat + " +this.getThreadId() + " setting timer to " +requestedState);
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
            
            ZimbraLog.im.info("ImChat.TimerExecute "+mTimerState);
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
        public void run() {
                timerExecute();
        }
    }
    
    private static class ImXmlPartDataSource implements DataSource {
        
        ImXmlPartDataSource(Element elt)  {
            mElt = elt;
        }

        public String getContentType() {
            return "application/zimbra-im-xml";
        }
        
        private Element mElt;
        private byte[] mBuf = null;

        public InputStream getInputStream() throws IOException {
            synchronized(this) {
                if (mBuf == null) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    OutputStreamWriter wout =
                        new OutputStreamWriter(buf, Mime.P_CHARSET_UTF8);
                    String text = mElt.toXML().asXML(); 
                    wout.write(text);
                    wout.flush();
                    mBuf = buf.toByteArray();
                }
            }
            ByteArrayInputStream in = new ByteArrayInputStream(mBuf);
            return in;
        }

        public String getName() {
            return "ImXmlPartDataSource";
        }

        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }
        
    }
    
    private static class HtmlPartDataSource implements DataSource {
        
        HtmlPartDataSource(String text)  {
            mText = text;
        }

        public String getContentType() {
            return "text/html";
        }
        
        private String mText;
        private byte[] mBuf = null;

        public InputStream getInputStream() throws IOException {
            synchronized(this) {
                if (mBuf == null) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    OutputStreamWriter wout =
                        new OutputStreamWriter(buf, Mime.P_CHARSET_UTF8);
                    String text = mText;
                    wout.write(text);
                    wout.flush();
                    mBuf = buf.toByteArray();
                }
            }
            ByteArrayInputStream in = new ByteArrayInputStream(mBuf);
            return in;
        }

        public String getName() {
            return "HtmlPartDataSource";
        }

        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }
        
    }
    
    private static final String sColors[] = new String[] {
        "#0000FF",
        "#FF0000",
        "#00FF00",
        "#FF00FF",
    };
    
    /**
     * Write this chat as a MimeMessage into the user's IMs folder
     */
    private void flush() {
        if (mLastFlushSeqNo >= getHighestSeqNo())
            return;
        
        try {
            
            MimeMessage mm = new MimeMessage(JMSession.getSession());
            MimeMultipart mmp = new MimeMultipart("alternative");
            mm.setContent(mmp);
            
            StringBuilder subject = new StringBuilder();
            StringBuilder plainText = new StringBuilder();
            List<Address> addrs = new ArrayList<Address>();
            DateFormat df = new SimpleDateFormat("h:mm a");
            Date highestDate = new Date(0);
            StringBuilder html = new StringBuilder("<html>");
            Integer colorOff = 0; // an index of the # unique users we've seen so far in this im chat
            HashMap<String /*addr*/, String /*colorId*/> colorMap = new HashMap<String, String>();
            
            for (IMMessage msg : mMessages) {
                InternetAddress ia = new InternetAddress(msg.getFrom().getAddr());
                if (!addrs.contains(ia))
                    addrs.add(ia);
                
                String from = msg.getFrom() != null ? msg.getFrom().toString() : "";

                String msgBody = msg.getBody() !=  null ? msg.getBody().getPlainText() : "";
                
                // strip off a trailing newline, for presentation's sake
                if (msgBody.length() > 0 && msgBody.charAt(msgBody.length()-1) == '\n')
                    msgBody = msgBody.substring(0, msgBody.length()-1);
                
                // append the first few messages into the Subject of the transcript
                if (subject.length() < 40)
                    subject.append(msgBody).append("   ");
                
                plainText.append(new Formatter().format("%s[%s]: %s\n", from, df.format(msg.getDate()), msgBody));
                
                // date tracking: find the date of the latest message in the conv
                if (msg.getDate().after(highestDate))
                    highestDate = msg.getDate();
                
                String msgBodyHtml = msg.getBody() != null ? msg.getBody().getHtmlText() : "";
                
                // find the color for this user
                if (!colorMap.containsKey(from)) {
                    if (colorOff == -1)
                        colorMap.put(from, "#000000");
                    else
                        colorMap.put(from, sColors[colorOff++]);
                    
                    if (colorOff >= sColors.length)
                        colorOff = -1;
                }
                String colorId = colorMap.get(from);
                
                html.append(new Formatter().format("<font color=\"%s\"><b>%s</b><i>[%s]</i>: %s</font><br>\n", 
                            colorId, msg.getFrom().toString(), df.format(msg.getDate()), msgBodyHtml));
            }
            html.append("</html>");
            
            // subject
            int subjLen = Math.min(40, subject.length());
            mm.setSubject(subject.substring(0, subjLen));
            
            // sender list
            Address[] addrArray  = new Address[addrs.size()];
            addrs.toArray(addrArray);
            mm.addFrom(addrArray);
            
            // date
            mm.setSentDate(highestDate);
            
            // plain text part
            MimeBodyPart textPart = new MimeBodyPart();
            mmp.addBodyPart(textPart);
            textPart.setText(plainText.toString(), Mime.P_CHARSET_UTF8);
            
            // xml part
            Element root = new Element.XMLElement("im");
            IMGetChat.chatToXml(this, root);
            MimeBodyPart xmlPart = new MimeBodyPart();
            mmp.addBodyPart(xmlPart);
            xmlPart.setDataHandler(new DataHandler(new ImXmlPartDataSource(root)));
            
            // html
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(html.toString())));
            mmp.addBodyPart(htmlPart);
            
            mm.saveChanges(); // don't forget to call this, or bad things will happen!
            
            ParsedMessage pm  = new ParsedMessage(mm, true);
            
            Message msg;
            
            try {
                msg = mMailbox.saveIM(null, pm, mDraftId, 0, null);
            } catch(NoSuchItemException e) {
                // they deleted the chat from their mailbox.  Bad user.
                msg = mMailbox.saveIM(null, pm, -1, 0, null);
            }
            mDraftId = msg.getId();
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
    
    void addParticipant(Participant part) {
        mParticipants.put(part.getAddress(), part);
    }
    
    public String toString() {
        return "CHAT:"+mThreadId+"("+mParticipants.size()+" parts)";
    }
    
    public String getThreadId() { return mThreadId; }
    
    Participant lookupParticipant(IMAddr addrFrom) {
        return mParticipants.get(addrFrom);
    }
    
    private Participant findAddParticipant(IMAddr addrFrom,String resourceFrom, String nameFrom)
    {
        Participant part = mParticipants.get(addrFrom);
        if (part == null) {
            part = new Participant(addrFrom, resourceFrom,  nameFrom);
            mParticipants.put(addrFrom, part);
            return part;
        }
        
        return mParticipants.get(addrFrom);
    }
    
    public Collection<Participant> participants() {
        return Collections.unmodifiableCollection(mParticipants.values());
    }

    public List<IMMessage> messages() {
        return Collections.unmodifiableList(mMessages);
    }
    
    int addMessage(IMAddr addrFrom, String resourceFrom, String nameFrom, IMMessage msg)
    {
        // will trigger the add
        findAddParticipant(addrFrom, resourceFrom, nameFrom);
        
        mMessages.add(msg);
        
        enableTimer(TIMER_STATE.WAITING_TO_SAVE);
        
        return mMessages.size()+mFirstSeqNo;
    }
    
    /**
     * Message from us
     * 
     * @param msg
     */
    int addMessage(IMMessage msg)
    {
        mMessages.add(msg);
        
        enableTimer(TIMER_STATE.WAITING_TO_SAVE);
        
        return mMessages.size()+mFirstSeqNo;
    }
}
