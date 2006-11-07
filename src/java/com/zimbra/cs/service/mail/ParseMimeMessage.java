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

/*
 * Created on Sep 29, 2004
 */
package com.zimbra.cs.service.mail;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mime.BlobDataSource;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeCompoundHeader.ContentDisposition;
import com.zimbra.cs.mime.MimeCompoundHeader.ContentType;
import com.zimbra.cs.service.ContentServlet;
import com.zimbra.cs.service.UploadDataSource;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.cs.service.mail.EmailElementCache.EmailType;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.JMSession;
import com.zimbra.common.util.ExceptionToString;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.activation.DataHandler;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.SendFailedException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimePartDataSource;

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

    private static final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024;

    public static MimeMessage importMsgSoap(Element msgElem) throws ServiceException {
        /* msgElem == "<m>" E_MSG */
        assert(msgElem.getName().equals(MailService.E_MSG));

        Element contentElement = msgElem.getElement(MailService.E_CONTENT);

        byte[] content = contentElement.getText().getBytes();
        long maxSize = Provisioning.getInstance().getLocalServer().getLongAttr(Provisioning.A_zimbraFileUploadMaxSize, DEFAULT_MAX_SIZE);
        if (content.length > maxSize)
            throw ServiceException.INVALID_REQUEST("inline message too large", null);

        ByteArrayInputStream messageStream = new ByteArrayInputStream(content);
        try {
            return new Mime.FixedMimeMessage(JMSession.getSession(), messageStream);
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
        abstract protected InviteParserResult parseInviteElement(ZimbraSoapContext zsc, Account account, Element invElement) throws ServiceException;
        
        public final InviteParserResult parse(ZimbraSoapContext zsc, Account account, Element invElement) throws ServiceException {
            mResult = parseInviteElement(zsc, account, invElement);
            return mResult;
        }
        
        private InviteParserResult mResult;
        public InviteParserResult getResult() { return mResult; }
    }
    static class InviteParserResult {
//        public Calendar mCal;
        public ZCalendar.ZVCalendar mCal;
        public String mUid;
        public String mSummary;
        public Invite mInvite;
    }
    
    // by default, no invite allowed
    static InviteParser NO_INV_ALLOWED_PARSER = new InviteParser() {
        public InviteParserResult parseInviteElement(ZimbraSoapContext zsc, Account account, Element inviteElem)
        throws ServiceException {
            throw ServiceException.INVALID_REQUEST("No <inv> element allowed for this request", null);
        }
    };
    

    /**
     * @author tim
     *
     * Wrapper class for data parsed out of the mime message
     */
    public static class MimeMessageData {
        public List<InternetAddress> newContacts = new ArrayList<InternetAddress>();
        public List<Upload> uploads = null;    // NULL unless there are attachments
        public String iCalUUID = null;         // NULL unless there is an iCal part
    }

    public static MimeMessage parseMimeMsgSoap(ZimbraSoapContext zsc, Mailbox mbox, Element msgElem, 
                                               MimeBodyPart[] additionalParts, MimeMessageData out) 
    throws ServiceException {
        return parseMimeMsgSoap(zsc, mbox, msgElem, additionalParts, NO_INV_ALLOWED_PARSER, out);
    }
    

    // Recursively find and return the content of the first text/plain part.
    public static String getTextPlainContent(Element elem) {
        if (elem == null) return null;
        if (MailService.E_MSG.equals(elem.getName())) {
            elem = elem.getOptionalElement(MailService.E_MIMEPART);
            if (elem == null) return null;
        }
        String type =
            elem.getAttribute(MailService.A_CONTENT_TYPE, Mime.CT_DEFAULT).trim().toLowerCase();
        if (type.equals(Mime.CT_DEFAULT)) {
            Element contentElem = elem.getOptionalElement(MailService.E_CONTENT);
            return contentElem != null ? contentElem.getText() : null;
        } else if (type.startsWith(Mime.CT_MULTIPART_PREFIX)) {
            for (Iterator<Element> it = elem.elementIterator(MailService.E_MIMEPART); it.hasNext(); ) {
                Element childElem = it.next();
                String text = getTextPlainContent(childElem);
                if (text != null)
                    return text;
            }
        }
        return null;
    }

    /**
     * Given an <m> element from SOAP, return us a parsed MimeMessage, 
     * and also fill in the MimeMessageData structure with information we parsed out of it (e.g. contained 
     * Invite, msgids, etc etc)
     * @param zsc TODO
     * @param mbox
     * @param msgElem the <m> element
     * @param additionalParts - MimeBodyParts that we want to have added to the MimeMessage (ie things the server is adding onto the message)
     * @param inviteParser Callback which handles <inv> embedded invite components
     * @param out Holds info about things we parsed out of the message that the caller might want to know about
     *  
     * @return
     * @throws ServiceException
     */
    public static MimeMessage parseMimeMsgSoap(ZimbraSoapContext zsc, Mailbox mbox, Element msgElem, MimeBodyPart[] additionalParts,
                                               InviteParser inviteParser, MimeMessageData out) 
    throws ServiceException {
        /* msgElem == "<m>" E_MSG */
        assert(msgElem.getName().equals(MailService.E_MSG));
        OperationContext octxt = zsc.getOperationContext();

        boolean use2231 = DocumentHandler.getRequestedAccount(zsc).getBooleanAttr(Provisioning.A_zimbraPrefUseRfc2231, false);

	    try {
			MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
            MimeMultipart mmp = null;

            Element partElem   = msgElem.getOptionalElement(MailService.E_MIMEPART);
            Element attachElem = msgElem.getOptionalElement(MailService.E_ATTACH); 
            Element inviteElem = msgElem.getOptionalElement(MailService.E_INVITE);
            
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
                    additionalLen += additionalParts.length;
                }
                alternatives = new MimeBodyPart[additionalLen+1];
                int curAltPart = 0;
                
                // goes into the "content" subpart
                InviteParserResult result = inviteParser.parse(zsc, mbox.getAccount(), inviteElem);
                if (partElem != null && result.mCal != null) {
                    // If textual content is provided and there's an invite,
                    // set the text as DESCRIPTION of the iCalendar.  This helps
                    // clients that ignore alternative text content and only
                    // displays the DESCRIPTION specified in the iCalendar part.
                    // (e.g. MS Entourage for Mac)
                    String desc = getTextPlainContent(partElem);
                    if (desc != null && desc.length() > 0) {
                        result.mCal.addDescription(desc);
                        // Don't set desc as fragment of result.mInvite.
                        // If it's set and desc is very long, we'll run into
                        // 64KB limit of metadata db column.
                    }
                }
                MimeBodyPart mbp = CalendarMailSender.makeICalIntoMimePart(result.mUid, result.mCal);
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
                setContent(mm, mmp, partElem != null ? partElem : inviteElem, alternatives, use2231);
            }
            
            if (isMultipart) {
                if (attachElem != null) {
                    // attachments go into the toplevel "mixed" part
                    String attachIds = attachElem.getAttribute(MailService.A_ATTACHMENT_ID, null);
                    if (attachIds != null)
                        out.uploads = attachUploads(mmp, zsc, attachIds, use2231);
                    for (Iterator it = attachElem.elementIterator(); it.hasNext(); ) {
                        Element elem = (Element) it.next();
                        String eName = elem.getName();
                        if (eName.equals(MailService.E_MIMEPART)) {
//                            int messageId = (int) elem.getAttributeLong(MailService.A_MESSAGE_ID);
                            ItemId iid = new ItemId(elem.getAttribute(MailService.A_MESSAGE_ID), null);
                            String part = elem.getAttribute(MailService.A_PART);
                            if (!iid.hasSubpart()) {
                                attachPart(mmp, mbox.getMessageById(octxt, iid.getId()), part, use2231);
                            } else {
                                Appointment appt = mbox.getAppointmentById(octxt, iid.getId());
                                MimeMessage apptMm = appt.getSubpartMessage(iid.getSubpartId());
                                MimePart apptMp = Mime.getMimePart(apptMm, part);
                                if (apptMp == null)
                                    throw MailServiceException.NO_SUCH_PART(part);
                                attachPart(mmp, apptMp, use2231);
                            }
                        } else if (eName.equals(MailService.E_MSG)) {
                            int messageId = (int) elem.getAttributeLong(MailService.A_ID);
                            attachMessage(mmp, mbox.getMessageById(octxt, messageId));
                        } else if (eName.equals(MailService.E_CONTACT)) {
                            int contactId = (int) elem.getAttributeLong(MailService.A_ID);
                            attachContact(mmp, mbox.getContactById(octxt, contactId), use2231);
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
                } else if (eName.equals(MailService.E_APPT_TZ)) { /* <tz> */
                    // Ignore as a special case.
	            } else {
	                unsupportedChildElement(elem, msgElem);
	            }
	        }

            // can have no addresses specified if it's a draft...
            if (!maddrs.isEmpty())
                addAddressHeaders(mm, maddrs);

			if (!hasContent && !isMultipart)
				mm.setText("", Mime.P_CHARSET_DEFAULT);

            // JavaMail tip: don't forget to call this, it is REALLY confusing.  
            mm.saveChanges();

            if (mLog.isDebugEnabled())
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
    private static void setContent(MimeMessage mm, MimeMultipart mmp, Element elem, MimeBodyPart[] alternatives, boolean use2231) 
    throws MessagingException {
        String type = elem.getAttribute(MailService.A_CONTENT_TYPE, Mime.CT_DEFAULT).trim();
        ContentType ct = new ContentType(type, use2231);

        // is the client passing us a multipart?
        if (ct.getPrimaryType().equals("multipart")) {
            // yes!  Find out what the subtype is
            String subType = ct.getSubType();

            // do we need to add a multipart/alternative for the alternatives?
            if (alternatives == null || subType.equals("alternative")) {
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

                // add each part in turn (recursively) below
                for (Element subpart : elem.listElements())
                    setContent(mm, mmpNew, subpart, null, use2231);

                // finally, add the alternatives if there are any...
                if (alternatives != null) {
                    for (int i = 0; i < alternatives.length; i++)
                        mmpNew.addBodyPart(alternatives[i]);
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
                setContent(mm, mmp, elem, null, use2231);
                
                // add all the alternatives
                for (int i = 0; i < alternatives.length; i++)
                    mmp.addBodyPart(alternatives[i]);
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
                for (int i = 0; i < alternatives.length; i++)
                    mmp.addBodyPart(alternatives[i]);
            }
        }
    }

    private static List<Upload> attachUploads(MimeMultipart mmp, ZimbraSoapContext zsc, String attachIds, boolean use2231)
    throws ServiceException, MessagingException {
        List<Upload> uploads = new ArrayList<Upload>();
        String[] uploadIds = attachIds.split(FileUploadServlet.UPLOAD_DELIMITER);

        for (int i = 0; i < uploadIds.length; i++) {
            Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), uploadIds[i], zsc.getRawAuthToken());
            if (up == null)
                throw MailServiceException.NO_SUCH_UPLOAD(uploadIds[i]);
            uploads.add(up);

            // scan upload for viruses
            StringBuffer info = new StringBuffer();
            UploadScanner.Result result = UploadScanner.accept(up, info); 
            if (result == UploadScanner.REJECT)
            	throw MailServiceException.UPLOAD_REJECTED(up.getName(), info.toString());
            if (result == UploadScanner.ERROR)
            	throw MailServiceException.SCAN_ERROR(up.getName());
            String filename = up.getName();

            MimeBodyPart mbp = new MimeBodyPart();
            mbp.setDataHandler(new DataHandler(new UploadDataSource(up)));

            String ctype = up.getContentType() == null ? Mime.CT_APPLICATION_OCTET_STREAM : up.getContentType();
            mbp.setHeader("Content-Type", new ContentType(ctype, use2231).setParameter("name", filename).toString());
            mbp.setHeader("Content-Disposition", new ContentDisposition(Part.ATTACHMENT, use2231).setParameter("filename", filename).toString());

            mmp.addBodyPart(mbp);
        }
        return uploads;
    }

    private static void attachMessage(MimeMultipart mmp, com.zimbra.cs.mailbox.Message message)
    throws MessagingException, ServiceException {
        MailboxBlob blob = message.getBlob();

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setDataHandler(new DataHandler(new BlobDataSource(blob)));
        mbp.setHeader("Content-Type", blob.getMimeType());
        mbp.setHeader("Content-Disposition", Part.ATTACHMENT);
        mmp.addBodyPart(mbp);
    }

    private static void attachContact(MimeMultipart mmp, com.zimbra.cs.mailbox.Contact contact, boolean use2231)
    throws MessagingException {
        VCard vcf = VCard.formatContact(contact);
        String filename = vcf.fn + ".vcf";

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setText(vcf.formatted, Mime.P_CHARSET_UTF8);
        mbp.setHeader("Content-Type", new ContentType("text/x-vcard; charset=utf-8", use2231).setParameter("name", filename).toString());
        mbp.setHeader("Content-Disposition", new ContentDisposition(Part.ATTACHMENT, use2231).setParameter("filename", filename).toString());
        mmp.addBodyPart(mbp);
    }

    // subclass of MimePartDataSource that cleans up Content-Type headers before returning them so JavaMail doesn't barf
    private static class FixedMimePartDataSource extends MimePartDataSource {
        private FixedMimePartDataSource(MimePart mp)  { super(mp); }
        public String getContentType() {
            return new ContentType(super.getContentType()).toString();
        }
    }

    private static void attachPart(MimeMultipart mmp, com.zimbra.cs.mailbox.Message message, String part, boolean use2231)
    throws IOException, MessagingException, ServiceException {
        MimePart mp = ContentServlet.getMimePart(message, part);
        if (mp == null)
            throw MailServiceException.NO_SUCH_PART(part);
        String filename = Mime.getFilename(mp);

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setDataHandler(new DataHandler(new FixedMimePartDataSource(mp)));

        String ctype = mp.getContentType();
        if (ctype != null)
            mbp.setHeader("Content-Type", new ContentType(ctype, use2231).setParameter("name", filename).toString());

        mbp.setHeader("Content-Disposition", new ContentDisposition(Part.ATTACHMENT, use2231).setParameter("filename", filename).toString());

        String desc = mp.getDescription();
        if (desc != null)
            mbp.setHeader("Content-Description", desc);

        mmp.addBodyPart(mbp);
    }

    private static void attachPart(MimeMultipart mmp, MimePart mp, boolean use2231)
    throws MessagingException {
        String filename = Mime.getFilename(mp);

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setDataHandler(new DataHandler(new FixedMimePartDataSource(mp)));

        String ctype = mp.getContentType();
        if (ctype != null)
            mbp.setHeader("Content-Type", new ContentType(ctype, use2231).setParameter("name", filename).toString());

        mbp.setHeader("Content-Disposition", new ContentDisposition(Part.ATTACHMENT, use2231).setParameter("filename", filename).toString());

        String desc = mp.getDescription();
        if (desc != null)
            mbp.setHeader("Content-Description", desc);

        mmp.addBodyPart(mbp);
    }


    private static final class MessageAddresses {
        private final HashMap<String, Object> addrs = new HashMap<String, Object>();
        private final List<InternetAddress> newContacts;

        MessageAddresses(List<InternetAddress> contacts) {
            newContacts = contacts;
        }

        public void add(Element elem) throws ServiceException, UnsupportedEncodingException {
            String emailAddress = elem.getAttribute(MailService.A_ADDRESS);
            String personalName = elem.getAttribute(MailService.A_PERSONAL, null);
            String addressType = elem.getAttribute(MailService.A_ADDRESS_TYPE);

            InternetAddress addr = new InternetAddress(emailAddress, personalName, Mime.P_CHARSET_UTF8);
            if (elem.getAttributeBool(MailService.A_ADD_TO_AB, false))
                newContacts.add(addr);

            Object content = addrs.get(addressType);
            if (content == null || addressType.equals(EmailType.FROM.toString())) {
                addrs.put(addressType, addr);
            } else if (content instanceof List) {
                ((List<InternetAddress>) content).add(addr);
            } else {
                List<InternetAddress> list = new ArrayList<InternetAddress>();
                list.add((InternetAddress) content);
                list.add(addr);
                addrs.put(addressType, list);
            }
        }

        public InternetAddress[] get(String addressType) {
            Object content = addrs.get(addressType);
            if (content == null) {
                return null;
            } else if (content instanceof InternetAddress) {
                return new InternetAddress[] { (InternetAddress) content };
            } else {
                ArrayList list = (ArrayList) content;
                InternetAddress[] result = new InternetAddress[list.size()];
                for (int i = 0; i < list.size(); i++)
                    result[i] = (InternetAddress) list.get(i);
                return result;
            }
        }

        public boolean isEmpty() {
            return addrs.isEmpty();
        }
    }

    private static void addAddressHeaders(MimeMessage mm, MessageAddresses maddrs)
    throws MessagingException {
        InternetAddress[] addrs = maddrs.get(EmailType.TO.toString());
        if (addrs != null) {
            mm.addRecipients(Message.RecipientType.TO, addrs);
            mLog.debug("\t\tTO: " + addrs);
        }

        addrs = maddrs.get(EmailType.CC.toString());
        if (addrs != null) {
            mm.addRecipients(Message.RecipientType.CC, addrs);
            mLog.debug("\t\tCC: " + addrs);
        }

        addrs = maddrs.get(EmailType.BCC.toString());
        if (addrs != null) {
            mm.addRecipients(Message.RecipientType.BCC, addrs);
            mLog.debug("\t\tBCC: " + addrs);
        }

        addrs = maddrs.get(EmailType.FROM.toString());
        if (addrs != null && addrs.length == 1) {
            mm.setFrom(addrs[0]);
            mLog.debug("\t\tFrom: " + addrs[0]);
        }
        
        addrs = maddrs.get(EmailType.REPLY_TO.toString());
        if (addrs != null && addrs.length > 0) {
            mm.setReplyTo(addrs);
            mLog.debug("\t\tReply-To: " + addrs[0]);
        }
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
                    Header hdr = (Header) hdrsEnum.nextElement();
                    if (!hdr.getName().equals("") && !hdr.getValue().equals("\n"))
                        mLog.debug(hdr.getName()+" = \""+hdr.getValue()+"\"");
                }
            mLog.debug("--------------------------------------");
            javax.mail.Address[] recips = mm.getAllRecipients();
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

    private static void unsupportedChildElement(Element child, Element parent) {
		mLog.warn("Unsupported child element \"" + child.getName() + "\" under parent " + parent.getName());
	}
}
