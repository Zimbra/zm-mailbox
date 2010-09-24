/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 29, 2004
 */
package com.zimbra.cs.service.mail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.SendFailedException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.DataSourceWrapper;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ExceptionToString;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailSender.SafeSendFailedException;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mime.MailboxBlobDataSource;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.UploadDataSource;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.cs.service.mail.ToXML.EmailType;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

public class ParseMimeMessage {

    static Log mLog = LogFactory.getLog(ParseMimeMessage.class);

    private static final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024;
    
    /**
     * Overrides the default transfer encoding and sets the encoding
     * of text attachments to base64, so that we preserve line endings
     * (bug 45858).
     */
    private static class Base64TextMimeBodyPart
    extends MimeBodyPart {
        protected void updateHeaders() throws MessagingException {
            super.updateHeaders();
            if (LC.text_attachments_base64.booleanValue() &&
                Mime.getContentType(this).startsWith(MimeConstants.CT_TEXT_PREFIX)) {
                setHeader("Content-Transfer-Encoding", "base64");
            }
        }
    }

    public static MimeMessage importMsgSoap(Element msgElem) throws ServiceException {
        /* msgElem == "<m>" E_MSG */
        assert(msgElem.getName().equals(MailConstants.E_MSG));

        Element contentElement = msgElem.getElement(MailConstants.E_CONTENT);

        byte[] content;
        try {
            content = contentElement.getText().getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.FAILURE("encoding error", e);
        }
        long maxSize = Provisioning.getInstance().getConfig().getLongAttr(Provisioning.A_zimbraMtaMaxMessageSize, DEFAULT_MAX_SIZE);
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
     * Callback routine for parsing the <inv> element and building a iCal4j Calendar from it
     * 
     *  We use a callback b/c there are differences in the parsing depending on the operation: 
     *  Replying to an invite is different than Creating or Modifying one, etc etc...
     *
     */
    static abstract class InviteParser {
        abstract protected InviteParserResult parseInviteElement(ZimbraSoapContext zsc, OperationContext octxt, Account account, Element invElement) throws ServiceException;

        public final InviteParserResult parse(ZimbraSoapContext zsc, OperationContext octxt, Account account, Element invElement) throws ServiceException {
            mResult = parseInviteElement(zsc, octxt, account, invElement);
            return mResult;
        }

        private InviteParserResult mResult;
        public InviteParserResult getResult() { return mResult; }
    }

    static class InviteParserResult {
        public ZCalendar.ZVCalendar mCal;
        public String mUid;
        public String mSummary;
        public Invite mInvite;
    }

    // by default, no invite allowed
    static InviteParser NO_INV_ALLOWED_PARSER = new InviteParser() {
        @Override
        public InviteParserResult parseInviteElement(ZimbraSoapContext zsc, OperationContext octxt, Account account, Element inviteElem)
        throws ServiceException {
            throw ServiceException.INVALID_REQUEST("No <inv> element allowed for this request", null);
        }
    };


    /** Wrapper class for data parsed out of the mime message */
    public static class MimeMessageData {
        public List<InternetAddress> newContacts = new ArrayList<InternetAddress>();
        public List<Upload> fetches = null;    // NULL unless we fetched messages from another server
        public List<Upload> uploads = null;    // NULL unless there are uploaded attachments
        public String iCalUUID = null;         // NULL unless there is an iCal part

        void addUpload(Upload up) {
            if (uploads == null)  uploads = new ArrayList<Upload>(4);
            uploads.add(up);
        }

        void addFetch(Upload up) {
            if (fetches == null)  fetches = new ArrayList<Upload>(4);
            fetches.add(up);
        }
    }

    public static MimeMessage parseMimeMsgSoap(ZimbraSoapContext zsc, OperationContext octxt, Mailbox mbox,
                                               Element msgElem, MimeBodyPart[] additionalParts, MimeMessageData out)
    throws ServiceException {
        return parseMimeMsgSoap(zsc, octxt, mbox, msgElem, additionalParts, NO_INV_ALLOWED_PARSER, out);
    }

    public static String getTextPlainContent(Element elem) {
        return getFirstContentByType(elem, MimeConstants.CT_TEXT_PLAIN);
    }

    public static String getTextHtmlContent(Element elem) {
        return getFirstContentByType(elem, MimeConstants.CT_TEXT_HTML);
    }

    // Recursively find and return the content of the first part with the specified content type.
    private static String getFirstContentByType(Element elem, String contentType) {
        if (elem == null) return null;
        if (MailConstants.E_MSG.equals(elem.getName())) {
            elem = elem.getOptionalElement(MailConstants.E_MIMEPART);
            if (elem == null) return null;
        }
        String type = elem.getAttribute(MailConstants.A_CONTENT_TYPE, contentType).trim().toLowerCase();
        if (type.equals(contentType)) {
            return elem.getAttribute(MailConstants.E_CONTENT, null);
        } else if (type.startsWith(MimeConstants.CT_MULTIPART_PREFIX)) {
            for (Element childElem : elem.listElements(MailConstants.E_MIMEPART)) {
                String text = getFirstContentByType(childElem, contentType);
                if (text != null)
                    return text;
            }
        }
        return null;
    }


    /** Class encapsulating common data passed among methods. */
    private static class ParseMessageContext {
        MimeMessageData out;
        ZimbraSoapContext zsc;
        OperationContext octxt;
        Mailbox mbox;
        boolean use2231;
        String defaultCharset;
        long size;
        long maxSize;
        
        ParseMessageContext() {
            try {
                Config config = Provisioning.getInstance().getConfig();
                maxSize = config.getIntAttr(Provisioning.A_zimbraMtaMaxMessageSize, -1);
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("Unable to determine max message size.  Disabling limit check.", e);
            }
            if (maxSize < 0) {
                maxSize = Long.MAX_VALUE;
            }
        }
        
        void incrementSize(String name, long numBytes) throws MailServiceException {
            size += numBytes;
            mLog.debug("Adding %s, incrementing size by %d to %d.", name, numBytes, size);
            if (size > maxSize) {
                throw MailServiceException.MESSAGE_TOO_BIG(maxSize, size);
            }
        }
    }

    /**
     * Given an <m> element from SOAP, return us a parsed MimeMessage, 
     * and also fill in the MimeMessageData structure with information we parsed out of it (e.g. contained 
     * Invite, msgids, etc etc)
     * @param zsc TODO
     * @param octxt TODO
     * @param mbox
     * @param msgElem the <m> element
     * @param additionalParts - MimeBodyParts that we want to have added to the MimeMessage (ie things the server is adding onto the message)
     * @param inviteParser Callback which handles <inv> embedded invite components
     * @param out Holds info about things we parsed out of the message that the caller might want to know about
     * @return
     * @throws ServiceException
     */
    public static MimeMessage parseMimeMsgSoap(ZimbraSoapContext zsc, OperationContext octxt, Mailbox mbox, Element msgElem,
                                               MimeBodyPart[] additionalParts, InviteParser inviteParser, MimeMessageData out)
    throws ServiceException {
        /* msgElem == "<m>" E_MSG */
        assert(msgElem.getName().equals(MailConstants.E_MSG));

        Account target = DocumentHandler.getRequestedAccount(zsc);
        ParseMessageContext ctxt = new ParseMessageContext();
        ctxt.out = out;
        ctxt.zsc = zsc;
        ctxt.octxt = octxt;
        ctxt.mbox = mbox;
        ctxt.use2231 = target.getBooleanAttr(Provisioning.A_zimbraPrefUseRfc2231, false);
        ctxt.defaultCharset = target.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, MimeConstants.P_CHARSET_UTF8);
        if (ctxt.defaultCharset.equals(""))
            ctxt.defaultCharset = MimeConstants.P_CHARSET_UTF8;

        try {
            MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
            MimeMultipart mmp = null;

            Element partElem   = msgElem.getOptionalElement(MailConstants.E_MIMEPART);
            Element attachElem = msgElem.getOptionalElement(MailConstants.E_ATTACH);
            Element inviteElem = msgElem.getOptionalElement(MailConstants.E_INVITE);

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
                if (additionalParts != null)
                    additionalLen += additionalParts.length;
                alternatives = new MimeBodyPart[additionalLen+1];
                int curAltPart = 0;

                // goes into the "content" subpart
                InviteParserResult result = inviteParser.parse(zsc, octxt, mbox.getAccount(), inviteElem);
                
                if (partElem != null && result.mCal != null) {
                    // If textual content is provided and there's an invite,
                    // set the text as DESCRIPTION of the iCalendar.  This helps
                    // clients that ignore alternative text content and only
                    // displays the DESCRIPTION specified in the iCalendar part.
                    // (e.g. MS Entourage for Mac)
                    String desc = getTextPlainContent(partElem);
                    String html = getTextHtmlContent(partElem);
                    result.mCal.addDescription(desc, html);
                    if (result.mInvite != null) {
                        // Only use the desc/html from MIME parts if at least one of them is non-empty.
                        // It's possible the notes were given in <inv> node only, with no corresponding MIME parts.
                        if ((desc != null && desc.length() > 0) || (html != null && html.length() > 0)) {
                            result.mInvite.setDescription(desc, html);
                            if (desc != null && desc.length() > 0) {
                                result.mInvite.setFragment(Fragment.getFragment(desc, true));
                            }
                        }
                    }
                }
                MimeBodyPart mbp = CalendarMailSender.makeICalIntoMimePart(result.mUid, result.mCal);
                alternatives[curAltPart++] = mbp;

                if (additionalParts != null) {
                    for (int i = 0; i < additionalParts.length; i++)
                        alternatives[curAltPart++] = additionalParts[i];
                }
            } else {
                alternatives = additionalParts;
            }

            // handle the content from the client, if any
            if (hasContent)
                setContent(mm, mmp, partElem != null ? partElem : inviteElem, alternatives, ctxt);

            // attachments go into the toplevel "mixed" part
            if (isMultipart && attachElem != null)
                handleAttachments(attachElem, mmp, ctxt, null);

            // <m> attributes: id, f[lags], s[ize], d[ate], cid(conv-id), l(parent folder)
            // <m> child elements: <e> (email), <s> (subject), <f> (fragment), <mp>, <attach>
            MessageAddresses maddrs = new MessageAddresses(out.newContacts);
            for (Element elem : msgElem.listElements()) {
                String eName = elem.getName();
                if (eName.equals(MailConstants.E_ATTACH)) {
                    // ignore it...
                } else if (eName.equals(MailConstants.E_MIMEPART)) { /* <mp> */
                    // processMessagePart(mm, elem);
                } else if (eName.equals(MailConstants.E_EMAIL)) { /* <e> */
                    maddrs.add(elem, ctxt.defaultCharset);
                } else if (eName.equals(MailConstants.E_IN_REPLY_TO)) { /* <irt> */
                    // mm.setHeader("In-Reply-To", elem.getText());
                } else if (eName.equals(MailConstants.E_SUBJECT)) { /* <su> */
                    // mm.setSubject(elem.getText(), "utf-8");
                } else if (eName.equals(MailConstants.E_FRAG)) { /* <f> */
                    mLog.debug("Ignoring message fragment data");
                } else if (eName.equals(MailConstants.E_INVITE)) { /* <inv> */
                    // Already processed above.  Ignore it.
                } else if (eName.equals(MailConstants.E_CAL_TZ)) { /* <tz> */
                    // Ignore as a special case.
                } else {
                    mLog.warn("unsupported child element '" + elem.getName() + "' under parent " + msgElem.getName());
                }
            }

            // deal with things that can be either <m> attributes or subelements
            String subject = msgElem.getAttribute(MailConstants.E_SUBJECT, "");
            mm.setSubject(subject, StringUtil.checkCharset(subject, ctxt.defaultCharset));

            String irt = msgElem.getAttribute(MailConstants.E_IN_REPLY_TO, null);
            if (irt != null)
                mm.setHeader("In-Reply-To", irt);

            // can have no addresses specified if it's a draft...
            if (!maddrs.isEmpty())
                addAddressHeaders(mm, maddrs, ctxt.defaultCharset);
            	
            if (!hasContent && !isMultipart)
                mm.setText("", MimeConstants.P_CHARSET_DEFAULT);

            String flagStr = msgElem.getAttribute(MailConstants.A_FLAGS, "");
            if (flagStr.indexOf(Flag.getAbbreviation(Flag.ID_FLAG_HIGH_PRIORITY)) != -1) {
                mm.addHeader("X-Priority", "1");
                mm.addHeader("Importance", "high");
            } else if (flagStr.indexOf(Flag.getAbbreviation(Flag.ID_FLAG_LOW_PRIORITY)) != -1) {
                mm.addHeader("X-Priority", "5");
                mm.addHeader("Importance", "low");
            }

            // JavaMail tip: don't forget to call this, it is REALLY confusing.  
            mm.saveChanges();

            if (mLog.isDebugEnabled())
                dumpMessage(mm);

            return mm;
        } catch (UnsupportedEncodingException encEx) {
            String excepStr = ExceptionToString.ToString(encEx);
            mLog.warn(excepStr);
            throw ServiceException.FAILURE("UnsupportedEncodingExecption", encEx);
        } catch (SendFailedException sfe) {
            SafeSendFailedException ssfe = new SafeSendFailedException(sfe);
            String excepStr = ExceptionToString.ToString(ssfe);
            mLog.warn(excepStr);
            throw ServiceException.FAILURE("SendFailure", ssfe);
        } catch (MessagingException me) {
            String excepStr = ExceptionToString.ToString(me);
            mLog.warn(excepStr);
            throw ServiceException.FAILURE("MessagingExecption", me);
        } catch (IOException e) {
            e.printStackTrace();
            throw ServiceException.FAILURE("IOExecption", e);
        }
    }

    private static void handleAttachments(Element attachElem, MimeMultipart mmp, ParseMessageContext ctxt, String contentID)
    throws ServiceException, MessagingException, IOException {
        if (contentID != null)
            contentID = '<' + contentID + '>';

        String attachIds = attachElem.getAttribute(MailConstants.A_ATTACHMENT_ID, null);
        if (attachIds != null) {
            for (String uploadId : attachIds.split(FileUploadServlet.UPLOAD_DELIMITER)) {
                Upload up = FileUploadServlet.fetchUpload(ctxt.zsc.getAuthtokenAccountId(), uploadId, ctxt.zsc.getAuthToken());
                if (up == null)
                    throw MailServiceException.NO_SUCH_UPLOAD(uploadId);
                attachUpload(mmp, up, contentID, ctxt, null);
                ctxt.out.addUpload(up);
            }
        }

        for (Element elem : attachElem.listElements()) {
            String attachType = elem.getName();
            boolean optional = elem.getAttributeBool(MailConstants.A_OPTIONAL, false);
            try {
                if (attachType.equals(MailConstants.E_MIMEPART)) {
                    ItemId iid = new ItemId(elem.getAttribute(MailConstants.A_MESSAGE_ID), ctxt.zsc);
                    String part = elem.getAttribute(MailConstants.A_PART);
                    attachPart(mmp, iid, part, contentID, ctxt);
                } else if (attachType.equals(MailConstants.E_MSG)) {
                    ItemId iid = new ItemId(elem.getAttribute(MailConstants.A_ID), ctxt.zsc);
                    attachMessage(mmp, iid, contentID, ctxt);
                } else if (attachType.equals(MailConstants.E_CONTACT)) {
                    ItemId iid = new ItemId(elem.getAttribute(MailConstants.A_ID), ctxt.zsc);
                    attachContact(mmp, iid, contentID, ctxt);
                } else if (attachType.equals(MailConstants.E_DOC)) {
                    String path = elem.getAttribute(MailConstants.A_PATH, null);
                    if (path != null) {
                        attachDocument(mmp, path, contentID, ctxt);
                    } else {
                        ItemId iid = new ItemId(elem.getAttribute(MailConstants.A_ID), ctxt.zsc);
                        attachDocument(mmp, iid, contentID, ctxt);
                    }
                }
            } catch (NoSuchItemException nsie) {
                if (!optional)
                    throw nsie;
                ZimbraLog.soap.info("skipping missing optional attachment: " + elem);
            }
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
     * @param defaultCharset TODO
     * @throws MessagingException
     * @throws IOException 
     * @throws ServiceException 
     */
    private static void setContent(MimeMessage mm, MimeMultipart mmp, Element elem, MimeBodyPart[] alternatives, ParseMessageContext ctxt)
    throws MessagingException, ServiceException, IOException {
        String type = elem.getAttribute(MailConstants.A_CONTENT_TYPE, MimeConstants.CT_DEFAULT).trim();
        ContentType ctype = new ContentType(type, ctxt.use2231).cleanup();

        // is the client passing us a multipart?
        if (ctype.getPrimaryType().equals("multipart")) {
            // handle multipart content separately...
            setMultipartContent(ctype.getSubType(), mm, mmp, elem, alternatives, ctxt);
            return;
        }

        Element inline = elem.getOptionalElement(MailConstants.E_ATTACH);
        if (inline != null) {
            handleAttachments(inline, mmp, ctxt, elem.getAttribute(MailConstants.A_CONTENT_ID, null));
            return;
        }

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
        String data = elem.getAttribute(MailConstants.E_CONTENT, "");
        ctxt.incrementSize("message body", data.getBytes().length);

        // if the user has specified an alternative charset, make sure it exists and can encode the content
        String charset = StringUtil.checkCharset(data, ctxt.defaultCharset);
        ctype.setCharset(charset).setParameter(MimeConstants.P_CHARSET, charset);

        if (mmp != null) {
            MimeBodyPart mbp = new MimeBodyPart();
            mbp.setText(data, charset);
            mbp.setHeader("Content-Type", ctype.toString());
            mmp.addBodyPart(mbp);
        } else {
            mm.setText(data, charset);
            mm.setHeader("Content-Type", ctype.toString());
        }

        if (alternatives != null) {
            for (int i = 0; i < alternatives.length; i++) {
                ctxt.incrementSize("alternative body", alternatives[i].getSize());
                mmp.addBodyPart(alternatives[i]);
            }
        }
    }

    private static void setMultipartContent(String subType, MimeMessage mm, MimeMultipart mmp, Element elem, MimeBodyPart[] alternatives, ParseMessageContext ctxt)
    throws MessagingException, ServiceException, IOException {
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
                setContent(mm, mmpNew, subpart, null, ctxt);

            // finally, add the alternatives if there are any...
            if (alternatives != null) {
                for (int i = 0; i < alternatives.length; i++) {
                    ctxt.incrementSize("alternative", alternatives[i].getSize());
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

            // add the entire client's multipart/whatever here inside our multipart/alternative
            setContent(mm, mmpNew, elem, null, ctxt);

            // add all the alternatives
            if (alternatives != null) {
                for (int i = 0; i < alternatives.length; i++) {
                    ctxt.incrementSize("alternative", alternatives[i].getSize());
                    mmpNew.addBodyPart(alternatives[i]);
                }
            }
        }
    }

    private static void attachUpload(MimeMultipart mmp, Upload up, String contentID, ParseMessageContext ctxt, ContentType ctypeOverride)
    throws ServiceException, MessagingException {
        // make sure we haven't exceeded the max size
        ctxt.incrementSize("upload " + up.getName(), (long) (up.getSize() * 1.33));

        // scan upload for viruses
        StringBuffer info = new StringBuffer();
        UploadScanner.Result result = UploadScanner.accept(up, info);
        if (result == UploadScanner.REJECT)
            throw MailServiceException.UPLOAD_REJECTED(up.getName(), info.toString());
        if (result == UploadScanner.ERROR)
            throw MailServiceException.SCAN_ERROR(up.getName());
        String filename = up.getName();

        // create the part and override the DataSource's default ctype, if required
        MimeBodyPart mbp = new Base64TextMimeBodyPart();
        
        UploadDataSource uds = new UploadDataSource(up);
        if (ctypeOverride != null && !ctypeOverride.equals(""))
            uds.setContentType(ctypeOverride);
        mbp.setDataHandler(new DataHandler(uds));

        // set headers -- ctypeOverride non-null has magical properties that I'm going to regret tomorrow
        ContentType ctype = ctypeOverride;
        ContentDisposition cdisp = new ContentDisposition(Part.ATTACHMENT, ctxt.use2231);
        if (ctype == null) {
            ctype = new ContentType(up.getContentType() == null ? MimeConstants.CT_APPLICATION_OCTET_STREAM : up.getContentType());
            ctype.cleanup().setParameter("name", filename);
            cdisp.setParameter("filename", filename);
        }
        mbp.setHeader("Content-Type", ctype.setCharset(ctxt.defaultCharset).toString());
        mbp.setHeader("Content-Disposition", cdisp.setCharset(ctxt.defaultCharset).toString());
        mbp.setContentID(contentID);

        // add to the parent part
        mmp.addBodyPart(mbp);
    }

    private static void attachRemoteItem(MimeMultipart mmp, ItemId iid, String contentID, ParseMessageContext ctxt,
                                         Map<String, String> params, ContentType ctypeOverride)
    throws ServiceException, MessagingException {
        try {
            Upload up = UserServlet.getRemoteResourceAsUpload(ctxt.zsc.getAuthToken(), iid, params);
            attachUpload(mmp, up, contentID, ctxt, ctypeOverride);
            ctxt.out.addFetch(up);
            return;
        } catch (IOException ioe) {
            throw ServiceException.FAILURE("can't serialize remote item", ioe);
        }
    }

    @SuppressWarnings("unchecked")
    private static void attachMessage(MimeMultipart mmp, ItemId iid, String contentID, ParseMessageContext ctxt)
    throws MessagingException, ServiceException {
        if (!iid.isLocal()) {
            attachRemoteItem(mmp, iid, contentID, ctxt, Collections.EMPTY_MAP, new ContentType(MimeConstants.CT_MESSAGE_RFC822));
            return;
        }

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(iid.getAccountId());
        Message msg = mbox.getMessageById(ctxt.octxt, iid.getId());
        ctxt.incrementSize("attached message", msg.getSize());

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setDataHandler(new DataHandler(new MailboxBlobDataSource(msg.getBlob())));
        mbp.setHeader("Content-Type", MimeConstants.CT_MESSAGE_RFC822);
        mbp.setHeader("Content-Disposition", Part.ATTACHMENT);
        mbp.setContentID(contentID);
        mmp.addBodyPart(mbp);
    }

    private static final Map<String, String> FETCH_CONTACT_PARAMS = new HashMap<String, String>(3);
        static {
            FETCH_CONTACT_PARAMS.put(UserServlet.QP_FMT, "vcf");
        }

    private static void attachContact(MimeMultipart mmp, ItemId iid, String contentID, ParseMessageContext ctxt)
    throws MessagingException, ServiceException {
        if (!iid.isLocal()) {
            attachRemoteItem(mmp, iid, contentID, ctxt, FETCH_CONTACT_PARAMS, null);
            return;
        }

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(iid.getAccountId());
        VCard vcf = VCard.formatContact(mbox.getContactById(ctxt.octxt, iid.getId()));

        ctxt.incrementSize("contact", vcf.formatted.length());
        String filename = vcf.fn + ".vcf";
        String charset = StringUtil.checkCharset(vcf.formatted, ctxt.defaultCharset);

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setText(vcf.formatted, charset);
        mbp.setHeader("Content-Type", new ContentType("text/x-vcard", ctxt.use2231).setCharset(ctxt.defaultCharset).setParameter("name", filename).setParameter("charset", charset).toString());
        mbp.setHeader("Content-Disposition", new ContentDisposition(Part.ATTACHMENT, ctxt.use2231).setCharset(ctxt.defaultCharset).setParameter("filename", filename).toString());
        mbp.setContentID(contentID);
        mmp.addBodyPart(mbp);
    }

    @SuppressWarnings("unchecked")
    private static void attachDocument(MimeMultipart mmp, ItemId iid, String contentID, ParseMessageContext ctxt) 
    throws MessagingException, ServiceException {
        if (!iid.isLocal()) {
            attachRemoteItem(mmp, iid, contentID, ctxt, Collections.EMPTY_MAP, null);
            return;
        }

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(iid.getAccountId());
        Document doc = mbox.getDocumentById(ctxt.octxt, iid.getId());
        attachDocument(mmp, doc, contentID, ctxt);
    }

    private static void attachDocument(MimeMultipart mmp, String path, String contentID, ParseMessageContext ctxt) 
    throws MessagingException, ServiceException {
        MailItem item = null;
        try {
            // first, just blindly try to fetch the item
            item = ctxt.mbox.getItemByPath(ctxt.octxt, path);
        } catch (NoSuchItemException nsie) { }

        if (item == null) {
            // on a miss, check for a mountpoint and, if so, fetch via UserServlet
            Pair<Folder, String> match = ctxt.mbox.getFolderByPathLongestMatch(ctxt.octxt, Mailbox.ID_FOLDER_USER_ROOT, path);
            if (!(match.getFirst() instanceof Mountpoint))
                throw MailServiceException.NO_SUCH_DOC(path);

            Map<String, String> params = new HashMap<String, String>(3);
            params.put(UserServlet.QP_NAME, match.getSecond());
            attachRemoteItem(mmp, ((Mountpoint) match.getFirst()).getTarget(), contentID, ctxt, params, null);
            return;
        }

        // on a hit, attach it directly
        if (!(item instanceof Document))
            throw MailServiceException.NO_SUCH_DOC(path);
        attachDocument(mmp, (Document) item, contentID, ctxt);
    }

    private static void attachDocument(MimeMultipart mmp, Document doc, String contentID, ParseMessageContext ctxt) 
    throws MessagingException, ServiceException {
        ctxt.incrementSize("attached document", (long) (doc.getSize() * 1.33));
        String ct = doc.getContentType();

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setDataHandler(new DataHandler(new MailboxBlobDataSource(doc.getBlob(), ct)));
        mbp.setHeader("Content-Type", new ContentType(ct).cleanup().setParameter("name", doc.getName()).setCharset(ctxt.defaultCharset).toString());
        mbp.setHeader("Content-Disposition", new ContentDisposition(Part.ATTACHMENT).setParameter("filename", doc.getName()).toString());
        mbp.setContentID(contentID);
        mmp.addBodyPart(mbp);
    }


    private static void attachPart(MimeMultipart mmp, ItemId iid, String part, String contentID, ParseMessageContext ctxt)
    throws IOException, MessagingException, ServiceException {
        if (!iid.isLocal()) {
            Map<String, String> params = new HashMap<String, String>(3);
            params.put(UserServlet.QP_PART, part);
            attachRemoteItem(mmp, iid, contentID, ctxt, params, null);
            return;
        }

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(iid.getAccountId());
        MimeMessage mm;
        if (iid.hasSubpart()) {
            mm = mbox.getCalendarItemById(ctxt.octxt, iid.getId()).getSubpartMessage(iid.getSubpartId());
        } else {
            mm = mbox.getMessageById(ctxt.octxt, iid.getId()).getMimeMessage();
        }
        MimePart mp = Mime.getMimePart(mm, part);
        if (mp == null)
            throw MailServiceException.NO_SUCH_PART(part);

        String filename = Mime.getFilename(mp);
        ctxt.incrementSize("part " + filename, mp.getSize());

        MimeBodyPart mbp = new Base64TextMimeBodyPart();

        String type = mp.getContentType();
        if (type != null) {
            // Clean up the content type and pass it to the new MimeBodyPart via DataSourceWrapper.
            // If we don't do this, JavaMail ignores the Content-Type header.  See bug 42452,
            // JavaMail bug 1650154.
            String ctype = new ContentType(type, ctxt.use2231).cleanup().setCharset(ctxt.defaultCharset).setParameter("name", filename).toString();
            DataHandler originalHandler = mp.getDataHandler();
            DataSourceWrapper wrapper = new DataSourceWrapper(originalHandler.getDataSource());
            wrapper.setContentType(ctype);
            mbp.setDataHandler(new DataHandler(wrapper));
            mbp.setHeader("Content-Type", ctype);
        } else {
            mbp.setDataHandler(mp.getDataHandler());
        }

        mbp.setHeader("Content-Disposition", new ContentDisposition(Part.ATTACHMENT, ctxt.use2231).setCharset(ctxt.defaultCharset).setParameter("filename", filename).toString());

        String desc = mp.getDescription();
        if (desc != null)
            mbp.setHeader("Content-Description", desc);

        mbp.setContentID(contentID);

        mmp.addBodyPart(mbp);
    }


    private static final class MessageAddresses {
        private final HashMap<String, Object> addrs = new HashMap<String, Object>();
        private final List<InternetAddress> newContacts;

        MessageAddresses(List<InternetAddress> contacts) {
            newContacts = contacts;
        }

        @SuppressWarnings("unchecked")
        public void add(Element elem, String defaultCharset) throws ServiceException, UnsupportedEncodingException {
            String emailAddress = IDNUtil.toAscii(elem.getAttribute(MailConstants.A_ADDRESS));
            String personalName = elem.getAttribute(MailConstants.A_PERSONAL, null);
            String addressType = elem.getAttribute(MailConstants.A_ADDRESS_TYPE);

            InternetAddress addr = new InternetAddress(emailAddress, personalName, StringUtil.checkCharset(personalName, defaultCharset));
            if (elem.getAttributeBool(MailConstants.A_ADD_TO_AB, false))
                newContacts.add(addr);

            Object content = addrs.get(addressType);
            if (content == null || addressType.equals(EmailType.FROM.toString()) || addressType.equals(EmailType.SENDER.toString())) {
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

        @SuppressWarnings("unchecked")
        public InternetAddress[] get(String addressType) {
            Object content = addrs.get(addressType);
            if (content == null) {
                return null;
            } else if (content instanceof InternetAddress) {
                return new InternetAddress[] { (InternetAddress) content };
            } else {
                return ((List<InternetAddress>)content).toArray(new InternetAddress[0]);
            }
        }

        public boolean isEmpty() {
            return addrs.isEmpty();
        }
    }

    private static void addAddressHeaders(MimeMessage mm, MessageAddresses maddrs, String defaultCharset)
    throws MessagingException {
        InternetAddress[] addrs = maddrs.get(EmailType.TO.toString());
        if (addrs != null && addrs.length > 0) {
            mm.addRecipients(javax.mail.Message.RecipientType.TO, addrs);
            mLog.debug("\t\tTO: " + Arrays.toString(addrs));
        }

        addrs = maddrs.get(EmailType.CC.toString());
        if (addrs != null && addrs.length > 0) {
            mm.addRecipients(javax.mail.Message.RecipientType.CC, addrs);
            mLog.debug("\t\tCC: " + Arrays.toString(addrs));
        }

        addrs = maddrs.get(EmailType.BCC.toString());
        if (addrs != null && addrs.length > 0) {
            mm.addRecipients(javax.mail.Message.RecipientType.BCC, addrs);
            mLog.debug("\t\tBCC: " + Arrays.toString(addrs));
        }

        addrs = maddrs.get(EmailType.FROM.toString());
        if (addrs != null && addrs.length == 1) {
            mm.setFrom(addrs[0]);
            mLog.debug("\t\tFrom: " + addrs[0]);
        }

        addrs = maddrs.get(EmailType.SENDER.toString());
        if (addrs != null && addrs.length == 1) {
            mm.setSender(addrs[0]);
            mLog.debug("\t\tSender: " + addrs[0]);
        }

        addrs = maddrs.get(EmailType.REPLY_TO.toString());
        if (addrs != null && addrs.length > 0) {
            mm.setReplyTo(addrs);
            mLog.debug("\t\tReply-To: " + addrs[0]);
        }

        addrs = maddrs.get(EmailType.READ_RECEIPT.toString());
        if (addrs != null && addrs.length > 0) {
            mm.addHeader("Disposition-Notification-To", InternetAddress.toString(addrs));
            mLog.debug("\t\tDisposition-Notification-To: " + Arrays.toString(addrs));
        }
    }

    private static void dumpMessage(MimeMessage mm) {
        /* 
         * Dump the outgoing message to stdout for now...
         */
        mLog.debug("--------------------------------------");
        try {
            Enumeration<?> hdrsEnum = mm.getAllHeaders();
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

    public static void main(String[] args) throws ServiceException, IOException, MessagingException, com.zimbra.cs.account.AuthTokenException {
        Element m = new Element.JSONElement(MailConstants.E_MSG);
        m.addAttribute(MailConstants.E_SUBJECT, "dinner appt");
        m.addUniqueElement(MailConstants.E_MIMEPART).addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain").addAttribute(MailConstants.E_CONTENT, "foo bar");
        m.addElement(MailConstants.E_EMAIL).addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString()).addAttribute(MailConstants.A_ADDRESS, "test@localhost");
        System.out.println(m.prettyPrint());

        Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.name, "user1");
        HashMap<String, Object> context = new HashMap<String, Object>();
        context.put(com.zimbra.soap.SoapServlet.ZIMBRA_AUTH_TOKEN, AuthProvider.getAuthToken(acct).getEncoded());
        ZimbraSoapContext zsc = new ZimbraSoapContext(null, context, com.zimbra.common.soap.SoapProtocol.SoapJS);
        OperationContext octxt = new OperationContext(acct);

        MimeMessage mm = parseMimeMsgSoap(zsc, octxt, null, m, null, new MimeMessageData());
        mm.writeTo(System.out);
    }
}
