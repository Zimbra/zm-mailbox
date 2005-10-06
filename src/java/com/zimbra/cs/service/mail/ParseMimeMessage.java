/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 29, 2004
 */
package com.zimbra.cs.service.mail;

import net.fortuna.ical4j.model.*;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Invite;
import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.BlobDataSource;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ContentServlet;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.FileItemDataSource;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.ExceptionToString;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author tim
 */
public class ParseMimeMessage {

    private static Log mLog = LogFactory.getLog(ParseMimeMessage.class);
//    private static CalendarBuilder mCalBuilder = new CalendarBuilder();
//    
//    protected static class GenericInviteParser implements ParseMimeMessage.InviteParser { 
//        private String mUid;
//        GenericInviteParser(String uid) { mUid = uid; };
//
//        public ParseMimeMessage.InviteParserResult parseInviteElement(OperationContext octxt, Account account, Element inviteElem) throws ServiceException 
//        {
//            return CalendarUtils.parseInviteForCreate(account, inviteElem, null, mUid, false);
//        }
//    };
    
    public static MimeMessage importMsgSoap(Element msgElem) throws ServiceException {
        /* msgElem == "<m>" E_MSG */
        assert(msgElem.getName().equals(MailService.E_MSG));

        Element contentElement = msgElem.getElement(MailService.E_CONTENT);

        String content = contentElement.getText();
        ByteArrayInputStream messageStream = new ByteArrayInputStream(content.getBytes());
        try {
            return new MimeMessage(JMSession.getSession(), messageStream);
        } catch (MessagingException me) {
            mLog.warn(ExceptionToString.ToString(me));
            throw ServiceException.FAILURE("MessagingExecption", me);
        }
    }
    
    /**
     * @author tim
     * 
     * Callback routine for parsing the <inv> element and building a iCal4j Calendar from it
     * 
     *  We use a callback b/c there are differences in the parsing depending on the operation: 
     *  Replying to an invite is different than Creating or Modifying one, etc etc...
     *
     */
    static abstract class InviteParser {
        abstract protected InviteParserResult parseInviteElement(OperationContext octxt, Account account, Element invElement) throws ServiceException;
        
        public final InviteParserResult parse(OperationContext octxt, Account account, Element invElement) throws ServiceException {
            mResult = parseInviteElement(octxt, account, invElement);
            return mResult;
        }
        
        private InviteParserResult mResult;
        public InviteParserResult getResult() { return mResult; }
    }
    static class InviteParserResult {
        public Calendar mCal;
        public String mUid;
        public String mSummary;
        public Invite mInvite;
    }
    
    // by default, no invite allowed
    static InviteParser NO_INV_ALLOWED_PARSER = new InviteParser() {
        public InviteParserResult parseInviteElement(OperationContext octxt, Account account, Element inviteElem) throws ServiceException 
        {
            throw MailServiceException.INVALID_REQUEST("No <inv> element allowed for this request", null);
        }
    };
    

    /**
     * @author tim
     *
     * Wrapper class for data parsed out of the mime message
     */
    public static class MimeMessageData {
        public List newContacts = new ArrayList();
        public String attachId = null; // NULL unless there are attachments
        public String iCalUUID = null; // NULL unless there is an iCal part
    }
    
    public static MimeMessage parseMimeMsgSoap(OperationContext octxt, Mailbox mbox, Element msgElem, 
                                               MimeBodyPart[] additionalParts, MimeMessageData out) 
    throws ServiceException {
        return parseMimeMsgSoap(octxt, mbox, msgElem, additionalParts, NO_INV_ALLOWED_PARSER, out);
    }
    

    /**
     * Given an <m> element from SOAP, return us a parsed MimeMessage, 
     * and also fill in the MimeMessageData structure with information we parsed out of it (e.g. contained 
     * Invite, msgids, etc etc)
     *  
     * @param mbox
     * @param msgElem the <m> element
     * @param additionalParts - MimeBodyParts that we want to have added to the MimeMessage (ie things the server is adding onto the message)
     * @param inviteParser Callback which handles <inv> embedded invite components
     * @param out Holds info about things we parsed out of the message that the caller might want to know about
     * @return
     * @throws ServiceException
     */
    public static MimeMessage parseMimeMsgSoap(OperationContext octxt, Mailbox mbox, Element msgElem, MimeBodyPart[] additionalParts,
            InviteParser inviteParser, MimeMessageData out) 
    throws ServiceException {
        /* msgElem == "<m>" E_MSG */
        assert(msgElem.getName().equals(MailService.E_MSG));

	    try {
            // anonymous subclass of MimeMessage to preserve Message-ID once it's been set
			MimeMessage mm = new MimeMessage(JMSession.getSession()) { protected void updateHeaders() throws MessagingException { String msgid = getMessageID(); super.updateHeaders(); if (msgid != null) setHeader("Message-ID", msgid); } };
            MimeMultipart mmp = null;

            Element partElem   = msgElem.getOptionalElement(MailService.E_MIMEPART);
            Element attachElem = msgElem.getOptionalElement(MailService.E_ATTACH); 
            Element inviteElem = msgElem.getOptionalElement(MailService.E_INVITE);
            
            if (partElem == null) {
                // well, there's no body part...so we'll just stick the invite 
                // there if we have one
                if (inviteElem != null) {
                    partElem = inviteElem;
                }
                // FIXME: should also try to stick additionalParts in there too!
            }
            
            boolean hasContent  = (partElem != null || inviteElem != null || additionalParts != null);
            boolean isMultipart = (attachElem != null); // || inviteElem != null || additionalParts!=null);
            if (isMultipart) {
                mmp = new MimeMultipart("mixed");  // may need to change to "digest" later
                mm.setContent(mmp);
            }
            
            // Grab the <inv> part now so we can stick it in a multipart/alternative if necessary
            MimeBodyPart[] alternatives = null;
            
            if (inviteElem != null) {
                int additionalLen = 0;
                if (additionalParts != null) {
                    additionalLen+=additionalParts.length;
                }
                alternatives = new MimeBodyPart[additionalLen+1];
                int curAltPart = 0;
                
                // goes into the "content" subpart
                InviteParserResult result = inviteParser.parse(octxt, mbox.getAccount(), inviteElem);
                MimeBodyPart mbp = CalendarUtils.makeICalIntoMimePart(result.mUid, result.mCal);
                alternatives[curAltPart++] = mbp;
                
                if (additionalParts != null) {
                    for (int i = 0; i < additionalParts.length; i++) {
                        alternatives[curAltPart++] = additionalParts[i];
                    }
                }
            } else {
                alternatives = additionalParts;
            }
            
            // handle the content from the client, if any
            if (hasContent) {
                setContent(mm, mmp, partElem, alternatives);
            }
            
            if (isMultipart) {
                if (attachElem != null) {
                    // attachments go into the toplevel "mixed" part
                    String attachId = attachElem.getAttribute(MailService.A_ATTACHMENT_ID, null);
                    if (attachId != null) {
                        attachUploads(mmp, mbox, attachId);
                        out.attachId = attachId;
                    }
                    for (Iterator it = attachElem.elementIterator(); it.hasNext(); ) {
                        Element elem = (Element) it.next();
                        String eName = elem.getName();
                        if (eName.equals(MailService.E_MIMEPART)) {
                            int messageId = (int) elem.getAttributeLong(MailService.A_MESSAGE_ID);
                            String part = elem.getAttribute(MailService.A_PART);
                            attachPart(mmp, mbox.getMessageById(octxt, messageId), part);
                        } else if (eName.equals(MailService.E_MSG)) {
                            int messageId = (int) elem.getAttributeLong(MailService.A_ID);
                            attachMessage(mmp, mbox.getMessageById(octxt, messageId));
                        }
                    }
                }
                
            }

            // <m> attributes: id, f[lags], s[ize], d[ate], cid(conv-id), l(parent folder)
			// <m> child elements: <e> (email), <s> (subject), <f> (fragment), <mp>, <attach>
			MessageAddresses maddrs = new MessageAddresses(out.newContacts);
			for (Iterator it = msgElem.elementIterator(); it.hasNext(); ) {
	            Element elem = (Element) it.next();
	            String eName = elem.getName();
	            if (eName.equals(MailService.E_ATTACH)) {
                    // ignore it...
                } else if (eName.equals(MailService.E_MIMEPART)) { /* <mp> */
                    // processMessagePart(mm, elem);
	            } else if (eName.equals(MailService.E_EMAIL)) { /* <e> */
                    maddrs.add(elem);
                } else if (eName.equals(MailService.E_IN_REPLY_TO)) { /* <irt> */
                    mm.setHeader("In-Reply-To", elem.getText());
                } else if (eName.equals(MailService.E_SUBJECT)) { /* <su> */
                    String subject = elem.getText();
                    mm.setSubject(subject, "utf-8");
                    mLog.debug("\t\tSubject: "+subject);
	            } else if (eName.equals(MailService.E_FRAG)) { /* <f> */
	            	mLog.debug("Ignoring message fragment data");
                } else if (eName.equals(MailService.E_INVITE)) { /* <inv> */
                    // Already processed above.  Ignore it.
	            } else {
	                unsupportedChildElement(elem, msgElem);
	            }
	        }
            // this will be legal once we implement drafts...
//            if (maddrs.isEmpty())
//                throw ServiceException.INVALID_REQUEST("no recipients specified", null);
            if (!maddrs.isEmpty()) {
                addRecipients(mm, maddrs);
            }

			if (!hasContent && !isMultipart)
				mm.setText("", Mime.P_CHARSET_DEFAULT);

            // JavaMail tip: don't forget to call this, it is REALLY confusing.  
            mm.saveChanges();

            dumpMessage(mm);
            
			return mm;
	    } catch (UnsupportedEncodingException encEx) {
	        String excepStr = ExceptionToString.ToString(encEx);
	        mLog.warn(excepStr);
	        throw ServiceException.FAILURE("UnsupportedEncodingExecption", encEx);
	    } catch (SendFailedException failure) {
	        String excepStr = ExceptionToString.ToString(failure);
	        mLog.warn(excepStr);
	        throw ServiceException.FAILURE("SendFailure", failure);
	    } catch (MessagingException me) {
	        String excepStr = ExceptionToString.ToString(me);
	        mLog.warn(excepStr);
	        throw ServiceException.FAILURE("MessagingExecption", me);
	    } catch (IOException e) {
			e.printStackTrace();
            throw ServiceException.FAILURE("IOExecption", e);
		}
    }
    
    /**
     * The <mp>'s from the client and the MimeBodyParts in alternatives[] all want to be "content"
     * of this MimeMessage.  The alternatives[] all need to be "alternative" to whatever the client sends
     * us....but we want to be careful so that we do NOT create a nested multipart/alternative structure
     * within another one (that would be very tacky)....so this is a bit complicated.
     * 
     * @param mm
     * @param mmp
     * @param elem
     * @param alternatives
     * @throws MessagingException
     */
    private static void setContent(MimeMessage mm, Multipart mmp, Element elem, MimeBodyPart[] alternatives) 
    throws MessagingException {
        String type = elem.getAttribute(MailService.A_CONTENT_TYPE, Mime.CT_DEFAULT).trim();
        int slash = type.indexOf('/');
        boolean invalidCT = (slash <= 0 || slash == type.length() - 1);

        // is the client passing us a multipart?
        if (type.startsWith(Mime.CT_MULTIPART + '/')) {

            // yes!  Find out what the subtype is (assume "mixed" if none or invalid one is specified)
            String subType = invalidCT ? "mixed" : type.substring(slash + 1);
            
            // do we need to add a multipart/alternative for the alternatives?
            if (alternatives==null || subType.equals("alternative")) {
                
                // no need to add an extra multipart/alternative!
                
                // create the MimeMultipart and attach it to the existing structure:
                MimeMultipart mmpNew = new MimeMultipart(subType);
                if (mmp == null) {
                    // there were no multiparts at all, we need to create one 
                    mm.setContent(mmpNew);
                } else {
                    // there was already a multipart/mixed at the top of the mm
                    MimeBodyPart mbpWrapper = new MimeBodyPart();
                    mbpWrapper.setContent(mmpNew);
                    mmp.addBodyPart(mbpWrapper);
                }
                
                // add each part in turn (recursively)
                for (Iterator it = elem.elementIterator(); it.hasNext(); ) {
                    // the alternatives will be placed below
                    setContent(mm, mmpNew, (Element) it.next(), null);
                }
                
                // finally, add the alternatives if there are any...
                if (alternatives != null) {
                    for (int i = 0; i < alternatives.length; i++) {
                        mmpNew.addBodyPart(alternatives[i]);
                    }
                }
            } else {
                // create a multipart/alternative to hold all the client's struct + the alternatives
                MimeMultipart mmpNew = new MimeMultipart("alternative");
                if (mmp == null) {
                    mm.setContent(mmpNew);
                } else {
                    MimeBodyPart mbpWrapper = new MimeBodyPart();
                    mbpWrapper.setContent(mmpNew);
                    mmp.addBodyPart(mbpWrapper);
                }
                mmp = mmpNew;
                
                // add the entire client's multipart/whatever here inside our multipart/alternative
                setContent(mm, mmp, elem, null);
                
                // add all the alternatives
                for (int i = 0; i < alternatives.length; i++) {
                    mmp.addBodyPart(alternatives[i]);
                }
            } 
        } else {
            // a single part from the client...we might still have to create a multipart/alternative if
            // there are alternatives[] passed-in, but still this is fairly straightforward...
            
            if (alternatives != null) {
                // create a multipart/alternative to hold all the alternatives
                MimeMultipart mmpNew = new MimeMultipart("alternative");
                if (mmp == null) {
                    mm.setContent(mmpNew);
                } else {
                    MimeBodyPart mbpWrapper = new MimeBodyPart();
                    mbpWrapper.setContent(mmpNew);
                    mmp.addBodyPart(mbpWrapper);
                }
                mmp = mmpNew;
            }

            // once we get here, mmp is either NULL, a multipart/mixed from the toplevel, 
            // or a multipart/alternative created just above....either way we are safe to stick
            // the client's nice and simple body right here
            ContentType ct = new ContentType();
            ct.setPrimaryType(invalidCT ? "text" : type.substring(0, slash));
            ct.setSubType(invalidCT ? "plain" : type.substring(slash + 1));
            ct.setParameter(Mime.P_CHARSET, Mime.P_CHARSET_UTF8);
    
            Element contentElem = elem.getOptionalElement(MailService.E_CONTENT);
            String data = (contentElem == null ? "" : contentElem.getText());
            if (mmp != null) {
                MimeBodyPart mbp = new MimeBodyPart();
                mbp.setText(data, Mime.P_CHARSET_UTF8);
                mbp.setHeader("Content-Type", ct.toString());
                mmp.addBodyPart(mbp);
            } else {
                mm.setText(data, Mime.P_CHARSET_UTF8);
                mm.setHeader("Content-Type", ct.toString());
            }
            
            if (alternatives != null) {
                for (int i = 0; i < alternatives.length; i++) {
                    mmp.addBodyPart(alternatives[i]);
                }
            }
        }
    }

    private static void attachUploads(Multipart mmp, Mailbox mbox, String attachId) throws MailServiceException, MessagingException {
        List uploads = FileUploadServlet.fetchUploads(mbox.getAccountId(), attachId);
        if (uploads == null)
            throw MailServiceException.NO_SUCH_UPLOAD(attachId);

        for (Iterator it = uploads.iterator(); it.hasNext(); ) {
            FileItem fi = (FileItem) it.next();

            // Scan upload for viruses
            StringBuffer info = new StringBuffer();
            UploadScanner.Result result = UploadScanner.accept(fi, info); 
            if (result == UploadScanner.REJECT) {
            	throw MailServiceException.UPLOAD_REJECTED(fi.getName(), info.toString());
            }
            if (result == UploadScanner.ERROR) {
            	throw MailServiceException.SCAN_ERROR(fi.getName());
            }

            MimeBodyPart mbp = new MimeBodyPart();
            mbp.setDataHandler(new DataHandler(new FileItemDataSource(fi)));

            String type = fi.getContentType();
            mbp.setHeader("Content-Type", type == null ? Mime.CT_APPLICATION_OCTET_STREAM : type);

            ContentDisposition cd = new ContentDisposition(Part.ATTACHMENT);
            if (fi.getName() != null) {
                String filename = trimFilename(fi.getName());
                if (filename != null)
                	cd.setParameter("filename", filename);
            }
            mbp.setHeader("Content-Disposition", cd.toString());

            mmp.addBodyPart(mbp);
        }
    }
    
//    private static void attachMimeBodyPart(Multipart mmp, MimeBodyPart mbp)
//    throws MessagingException {
//        mmp.addBodyPart(mbp);
//    }

//    private static void attachCalendar(Multipart mmp, Calendar iCal, String uid) 
//    throws MessagingException {
//        MimeBodyPart mbp = new MimeBodyPart();
//        mbp.setDataHandler(new DataHandler(new CalendarDataSource(iCal, uid))); 
//        mmp.addBodyPart(mbp);
//    }

    private static void attachMessage(Multipart mmp, com.zimbra.cs.mailbox.Message message)
    throws IOException, MessagingException, ServiceException {
        MailboxBlob blob = message.getBlob();

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setDataHandler(new DataHandler(new BlobDataSource(blob)));
        mbp.setHeader("Content-Type", blob.getMimeType());
        mbp.setHeader("Content-Disposition", Part.ATTACHMENT);
        mmp.addBodyPart(mbp);
    }

    private static void attachPart(Multipart mmp, com.zimbra.cs.mailbox.Message message, String part)
    throws IOException, MessagingException, ServiceException {
        MimePart mp = ContentServlet.getMimePart(message, part);
        if (mp == null)
            throw MailServiceException.NO_SUCH_PART(part);

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setDataHandler(new DataHandler(new MimePartDataSource(mp)));

        String type = mp.getContentType();
        mbp.setHeader("Content-Type", type == null ? Mime.CT_APPLICATION_OCTET_STREAM : type);

        ContentDisposition cd = new ContentDisposition(Part.ATTACHMENT);
        if (mp.getFileName() != null)
        	cd.setParameter("filename", mp.getFileName());
        mbp.setHeader("Content-Disposition", cd.toString());

        String desc = mp.getDescription();
        if (desc != null)
            mbp.setHeader("Content-Description", desc);

        mmp.addBodyPart(mbp);
    }

    private static final class MessageAddresses {
        private final HashMap addrs = new HashMap();
        private final List    newContacts;

        MessageAddresses(List contacts) {
            newContacts = contacts;
        }

        public void add(Element elem) throws ServiceException, UnsupportedEncodingException {
            String emailAddress = elem.getAttribute(MailService.A_ADDRESS);
            String personalName = elem.getAttribute(MailService.A_PERSONAL, null);
            String addressType = elem.getAttribute(MailService.A_ADDRESS_TYPE);

            InternetAddress addr = new InternetAddress(emailAddress, personalName);
            if (elem.getAttributeBool(MailService.A_ADD_TO_AB, false))
                newContacts.add(addr);

            Object content = addrs.get(addressType);
            if (content == null)
                addrs.put(addressType, addr);
            else if (content instanceof List)
                ((List) content).add(addr);
            else {
                ArrayList list = new ArrayList();
                list.add(content);
                list.add(addr);
                addrs.put(addressType, list);
            }
        }

        public Address[] get(String addressType) {
            Object content = addrs.get(addressType);
            if (content == null)
                return null;
            else if (content instanceof Address)
                return new Address[] { (Address) content };
            else {
                ArrayList list = (ArrayList) content;
                Address[] result = new Address[list.size()];
                for (int i = 0; i < list.size(); i++)
                    result[i] = (Address) list.get(i);
                return result;
            }
        }

        public boolean isEmpty() {
            return addrs.isEmpty();
        }
    }

    private static void addRecipients(MimeMessage mm, MessageAddresses maddrs)
    throws MessagingException {
        Address[] addrs = maddrs.get("t");
        if (addrs != null) {
            mm.addRecipients(Message.RecipientType.TO, addrs);
            mLog.debug("\t\tTO: " + addrs);
        }

        addrs = maddrs.get("c");
        if (addrs != null) {
            mm.addRecipients(Message.RecipientType.CC, addrs);
            mLog.debug("\t\tCC: " + addrs);
        }

        addrs = maddrs.get("b");
        if (addrs != null) {
            mm.addRecipients(Message.RecipientType.BCC, addrs);
            mLog.debug("\t\tBCC: " + addrs);
        }

        addrs = maddrs.get("f");
        if (addrs != null)
            mLog.warn("Client-Specified FROM address, not currently supported");
    }

    private static void dumpMessage(MimeMessage mm) {
        /* 
         * Dump the outgoing message to stdout for now...
         */
        mLog.debug("--------------------------------------");
        try {
            Enumeration hdrsEnum = mm.getAllHeaders();
            if (hdrsEnum != null)
                while (hdrsEnum.hasMoreElements()) {
                    Header hdr = (Header)hdrsEnum.nextElement();
                    if (!hdr.getName().equals("") && !hdr.getValue().equals("\n"))
                        mLog.debug(hdr.getName()+" = \""+hdr.getValue()+"\"");
                }
            mLog.debug("--------------------------------------");
            Address[] recips = mm.getAllRecipients();
            if (recips != null)
                for (int i = 0; i < recips.length; i++)
                    mLog.debug("Recipient: "+recips[i].toString());

            mLog.debug("--------------------------------------\nMessage size is: "+mm.getSize());

//          System.out.println("--------------------------------------");
//          System.out.print("Message Dump:");
//          mm.writeTo(System.out);
        } catch (Exception e) { e.printStackTrace(); };
        mLog.debug("--------------------------------------\n");
    }

	private static String trimFilename(String filename) {
	    final char[] delimiter = { '/', '\\', ':' };
	
	    if (filename == null || filename.equals(""))
	        return null;
	    for (int i = 0; i < delimiter.length; i++) {
	        int index = filename.lastIndexOf(delimiter[i]);
	        if (index == filename.length() - 1)
	            return null;
	        if (index != -1)
	            filename = filename.substring(index + 1);
	    }
	    return filename;
	}

    private static void unsupportedChildElement(Element child, Element parent) {
		mLog.warn("Unsupported child element \"" + child.getName() + "\" under parent " + parent.getName());
	}
}
