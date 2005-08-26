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
 * Created on 2004. 9. 15.
 */
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.MessagingException;
import javax.mail.internet.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.Attendee;

import com.zimbra.cs.html.HtmlDefang;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.EmailElementCache.CacheNode;
import com.zimbra.cs.service.util.ParsedItemID;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.StringUtil;


/**
 * @author jhahm
 *
 * Class containing static methods for encoding various MailItem-derived
 * objects into XML.
 */
public class ToXML {

    private static Log mLog = LogFactory.getLog(ToXML.class); 

	// no construction
	private ToXML()  {}

    public static Element encodeItem(Element parent, MailItem item) {
        return encodeItem(parent, item, Change.ALL_FIELDS);
    }
    public static Element encodeItem(Element parent, MailItem item, int fields) {
        if (item instanceof SearchFolder)
            return encodeSearchFolder(parent, (SearchFolder) item, fields);
        else if (item instanceof Folder)
            return encodeFolder(parent, (Folder) item, fields);
        else if (item instanceof Tag)
            return encodeTag(parent, (Tag) item, fields);
        else if (item instanceof Note)
            return encodeNote(parent, (Note) item, fields);
        else if (item instanceof Contact)
            return encodeContact(parent, (Contact) item, null, false, null, fields);
        else if (item instanceof Message) {
            OutputParticipants output = (fields == Change.ALL_FIELDS ? OutputParticipants.PUT_BOTH : OutputParticipants.PUT_SENDERS);
            return encodeMessageSummary(parent, (Message) item, output, fields);
        } else if (item instanceof Conversation) {
            Conversation conv = (Conversation) item;
            return encodeConversationSummary(parent, conv, conv.getDate(), null, null, fields);
        }
        return null;
    }

    private static boolean needToOutput(int fields, int fieldMask) {
        return ((fields & fieldMask) > 0);
    }

    public static Element encodeMailbox(Element parent, Mailbox mbox) {
        return encodeMailbox(parent, mbox, Change.ALL_FIELDS);
    }
    public static Element encodeMailbox(Element parent, Mailbox mbox, int fields) {
        Element elem = parent.addUniqueElement(MailService.E_MAILBOX);
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            elem.addAttribute(MailService.A_SIZE, mbox.getSize());
        return elem;
    }

    public static Element encodeFolder(Element parent, Folder folder) {
        if (folder instanceof SearchFolder)
            return encodeSearchFolder(parent, (SearchFolder) folder, Change.ALL_FIELDS);
        else
            return encodeFolder(parent, folder, Change.ALL_FIELDS);
    }
    public static Element encodeFolder(Element parent, Folder folder, int fields) {
        if (folder instanceof SearchFolder)
            return encodeSearchFolder(parent, (SearchFolder) folder, fields);
        Element elem = parent.addElement(MailService.E_FOLDER);
        int folderId = folder.getId();
        elem.addAttribute(MailService.A_ID, folderId);
        if (folderId != Mailbox.ID_FOLDER_ROOT) {
            if (needToOutput(fields, Change.MODIFIED_NAME)) {
            	String name = folder.getName();
    	        if (name != null && name.length() > 0)
    		        elem.addAttribute(MailService.A_NAME, name);
            }
            if (needToOutput(fields, Change.MODIFIED_FOLDER))
                elem.addAttribute(MailService.A_FOLDER, folder.getFolderId());
        }
        if (needToOutput(fields, Change.MODIFIED_UNREAD)) {
            int unread = folder.getUnreadCount();
            if (unread > 0 || fields != Change.ALL_FIELDS)
                elem.addAttribute(MailService.A_UNREAD, unread);
        }
        if (needToOutput(fields, Change.MODIFIED_SIZE)) {
            long size = folder.getSize();
            if (size > 0 || fields != Change.ALL_FIELDS)
                elem.addAttribute(MailService.A_SIZE, size);
        }
        if (needToOutput(fields, Change.MODIFIED_MSG_COUNT)) {
            int msgs = folder.getMessageCount();
            if (msgs > 0 || fields != Change.ALL_FIELDS)
                elem.addAttribute(MailService.A_NUM, msgs);
        }
        if (needToOutput(fields, Change.MODIFIED_CONFLICT))
            elem.addAttribute(MailService.A_CHANGE_DATE, folder.getChangeDate() / 1000);
        return elem;
	}

	public static Element encodeSearchFolder(Element parent, SearchFolder search) {
        return encodeSearchFolder(parent, search, Change.ALL_FIELDS);
    }
    public static Element encodeSearchFolder(Element parent, SearchFolder search, int fields) {
        Element elem = parent.addElement(MailService.E_SEARCH);
        elem.addAttribute(MailService.A_ID, search.getId());
        if (needToOutput(fields, Change.MODIFIED_NAME))
            elem.addAttribute(MailService.A_NAME, search.getName());
        if (needToOutput(fields, Change.MODIFIED_QUERY)) {
            elem.addAttribute(MailService.A_QUERY, search.getQuery());
            elem.addAttribute(MailService.A_SORTBY, search.getSortField());
            elem.addAttribute(MailService.A_SEARCH_TYPES, search.getReturnTypes());
        }
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            elem.addAttribute(MailService.A_FOLDER, search.getFolderId());
        if (needToOutput(fields, Change.MODIFIED_UNREAD)) {
            int unread = search.getUnreadCount();
            if (unread > 0 || fields != Change.ALL_FIELDS)
                elem.addAttribute(MailService.A_UNREAD, unread);
        }
        if (needToOutput(fields, Change.MODIFIED_CONFLICT))
            elem.addAttribute(MailService.A_CHANGE_DATE, search.getChangeDate() / 1000);
		return elem;
	}

    public static void recordItemTags(Element elem, MailItem item, int fields) {
        if (needToOutput(fields, Change.MODIFIED_FLAGS | Change.MODIFIED_UNREAD)) {
        	String flags = item.getFlagString();
        	if (fields != Change.ALL_FIELDS || !flags.equals(""))
                elem.addAttribute(MailService.A_FLAGS, flags);
        }
        if (needToOutput(fields, Change.MODIFIED_TAGS)) {
            String tags = item.getTagString();
            if (!tags.equals("") || fields != Change.ALL_FIELDS)
                elem.addAttribute(MailService.A_TAGS, tags);
        }
    }

    public static Element encodeContact(Element parent, Contact contact, ContactAttrCache cacache,
										boolean summary, List attrFilter) {
        return encodeContact(parent, contact, cacache, summary, attrFilter, Change.ALL_FIELDS);
    }
    public static Element encodeContact(Element parent, Contact contact, ContactAttrCache cacache,
            boolean summary, List attrFilter, int fields) {
        Element elem = parent.addElement(MailService.E_CONTACT);
        elem.addAttribute(MailService.A_ID, contact.getId());
        if (needToOutput(fields, Change.MODIFIED_CONTENT) && contact.getSavedSequence() != 0)
            elem.addAttribute(MailService.A_REVISION, contact.getSavedSequence());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            elem.addAttribute(MailService.A_FOLDER, contact.getFolderId());
        recordItemTags(elem, contact, fields);
        if (needToOutput(fields, Change.MODIFIED_CONFLICT))
            elem.addAttribute(MailService.A_CHANGE_DATE, contact.getChangeDate() / 1000);

        if (summary || !needToOutput(fields, Change.MODIFIED_CONTENT))
            return elem;

        Map attrs = contact.getAttrs();
        if (attrFilter != null) {
            for (Iterator it = attrFilter.iterator(); it.hasNext(); ) {
            	// HERE: How to distinguish between a non-existent attribute and
            	// an existing attribute with null or empty string value?
                String name = (String) it.next();
                String value = (String) attrs.get(name);
				if (value == null || value.equals(""))
				    continue;
                if (cacache != null)
                    cacache.makeAttr(elem, name, value);
                else
                    elem.addAttribute(name, value, Element.DISP_ELEMENT);
            }
        } else {
            for (Iterator it = attrs.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry me = (Entry) it.next();
                String name = (String) me.getKey();
                String value = (String) me.getValue();
				if (value == null || value.equals(""))
				    continue;
                if (cacache != null)
                    cacache.makeAttr(elem, name, value);
                else
                    elem.addAttribute(name, value, Element.DISP_ELEMENT);
            }
        }
        return elem;
    }

	public static Element encodeNote(Element parent, Note note) {
        return encodeNote(parent, note, Change.ALL_FIELDS);
    }
    public static Element encodeNote(Element parent, Note note, int fields) {
        Element elem = parent.addElement(MailService.E_NOTE);
        elem.addAttribute(MailService.A_ID, note.getId());
        if (needToOutput(fields, Change.MODIFIED_CONTENT) && note.getSavedSequence() != 0)
            elem.addAttribute(MailService.A_REVISION, note.getSavedSequence());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            elem.addAttribute(MailService.A_FOLDER, note.getFolderId());
        if (needToOutput(fields, Change.MODIFIED_DATE))
            elem.addAttribute(MailService.A_DATE, note.getDate());
        recordItemTags(elem, note, fields);
        if (needToOutput(fields, Change.MODIFIED_POSITION))
            elem.addAttribute(MailService.A_BOUNDS, note.getBounds().toString());
        if (needToOutput(fields, Change.MODIFIED_COLOR))
            elem.addAttribute(MailService.A_COLOR, note.getColor());
        if (needToOutput(fields, Change.MODIFIED_CONTENT))
            elem.addAttribute(MailService.E_CONTENT, note.getContent(), Element.DISP_CONTENT);
        if (needToOutput(fields, Change.MODIFIED_CONFLICT))
            elem.addAttribute(MailService.A_CHANGE_DATE, note.getChangeDate() / 1000);
        return elem;
	}

	public static Element encodeTag(Element parent, Tag tag) {
        return encodeTag(parent, tag, Change.ALL_FIELDS);
    }
    public static Element encodeTag(Element parent, Tag tag, int fields) {
		Element elem = parent.addElement(MailService.E_TAG);
		elem.addAttribute(MailService.A_ID, tag.getId());
		if (needToOutput(fields, Change.MODIFIED_NAME))
            elem.addAttribute(MailService.A_NAME, tag.getName());
		if (needToOutput(fields, Change.MODIFIED_COLOR))
            elem.addAttribute(MailService.A_COLOR, tag.getColor());
        if (needToOutput(fields, Change.MODIFIED_UNREAD)) {
    		int unreadCount = tag.getUnreadCount();
    		if (unreadCount > 0 || fields != Change.ALL_FIELDS)
                elem.addAttribute(MailService.A_UNREAD, unreadCount);
        }
        if (needToOutput(fields, Change.MODIFIED_CONFLICT))
            elem.addAttribute(MailService.A_CHANGE_DATE, tag.getChangeDate() / 1000);
		return elem;
	}


    public static Element encodeConversation(Element parent, Conversation conv) throws ServiceException {
        int fields = Change.ALL_FIELDS;
        Element c = encodeConversationCommon(parent, conv, fields);
        EmailElementCache eecache = new EmailElementCache();
        Message[] messages = conv.getMailbox().getMessagesByConversation(conv.getId());
        for (int i = 0; i < messages.length; i++) {
            Message msg = messages[i];
            Element m = c.addElement(MailService.E_MSG);
            m.addAttribute(MailService.A_ID, msg.getId());
            m.addAttribute(MailService.A_DATE, msg.getDate());
            m.addAttribute(MailService.A_SIZE, msg.getSize());
            m.addAttribute(MailService.A_FOLDER, msg.getFolderId());
            recordItemTags(m, msg, fields);
            m.addAttribute(MailService.E_FRAG, msg.getFragment(), Element.DISP_CONTENT);
            eecache.makeEmail(m, msg.getSender(), EmailElementCache.EMAIL_TYPE_FROM, null);
        }
		return c;
    }

    /**
     * 
     * This version lets you specify the Date and Fragment -- we use this when sending Query Results back to the client, 
     * the conversation date returned and fragment correspond to those of the matched message.
     * 
     * @param conv
     * @param date - Use this date
     * @param fragment - Use this fragment
     * @param eecache
     */
    public static Element encodeConversationSummary(Element parent, Conversation conv, long date,
            String fragment, EmailElementCache eecache) {
        return encodeConversationSummary(parent, conv, date, fragment, eecache, Change.ALL_FIELDS);
    }
    public static Element encodeConversationSummary(Element parent, Conversation conv, long date,
            String fragment, EmailElementCache eecache, int fields) {
        Element c = encodeConversationCommon(parent, conv, fields);
        if (needToOutput(fields, Change.MODIFIED_DATE))
            c.addAttribute(MailService.A_DATE, date);
        if (fragment != null && fields == Change.ALL_FIELDS)
        	c.addAttribute(MailService.E_FRAG, fragment, Element.DISP_CONTENT);
        if (needToOutput(fields, Change.MODIFIED_SENDERS)) {
            if (eecache == null)
                eecache = new EmailElementCache();
            SenderList sl;
			try {
				sl = conv.getSenderList();
			} catch (ServiceException e) {
				return c;
			}
			CacheNode fa = sl.getFirstAddress();
            if (fa != null) {
                eecache.makeEmail(c, fa, EmailElementCache.EMAIL_TYPE_NONE, null);
                // "<e/>" indicates that some senders may be omitted...
                if (sl.isElided())
                    c.addElement(MailService.E_EMAIL);
            }
            CacheNode[] la = sl.getLastAddresses();
            for (int i = 0; i < la.length; i++) {
                if (la[i] != null)
                    eecache.makeEmail(c, la[i], EmailElementCache.EMAIL_TYPE_NONE, null);
            }
        }
        if (needToOutput(fields, Change.MODIFIED_CONFLICT))
            c.addAttribute(MailService.A_CHANGE_DATE, conv.getChangeDate() / 1000);
        return c;
    }

    private static Element encodeConversationCommon(Element parent, Conversation conv, int fields) {
        Element c = parent.addElement(MailService.E_CONV);
        c.addAttribute(MailService.A_ID, conv.getId());
        if (needToOutput(fields, Change.MODIFIED_CHILDREN))
            c.addAttribute(MailService.A_NUM, conv.getMessageCount());
        recordItemTags(c, conv, fields);
        if (fields == Change.ALL_FIELDS)
            c.addAttribute(MailService.E_SUBJECT, conv.getSubject(), Element.DISP_CONTENT);
        return c;
    }

	/**
	 * Encodes a Message object into <m> element with <mp> elements
	 * for message body.
	 * @param msg
	 * @param metaDataOnly
	 * @return
	 * @throws ServiceException
	 */
	public static Element encodeMessageAsMP(Element parent, Message msg, boolean wantHTML, String part)
	throws ServiceException {
        boolean wholeMessage = (part == null || part.trim().equals(""));
        
        Element m;
        if (wholeMessage) {
            m = encodeMessageCommon(parent, msg, Change.ALL_FIELDS);
            m.addAttribute(MailService.A_ID, msg.getId());
        } else {
            m = parent.addElement(MailService.E_MSG);
            m.addAttribute(MailService.A_ID, msg.getId());
            m.addAttribute(MailService.A_PART, part);
        }

        try {
            MimeMessage mm = msg.getMimeMessage();
            if (!wholeMessage) {
                MimePart mp = Mime.getMimePart(mm, part);
                if (mp == null)
                    throw MailServiceException.NO_SUCH_PART(part);
                Object content = mp.getContent();
                if (!(content instanceof MimeMessage))
                    throw MailServiceException.NO_SUCH_PART(part);
                mm = (MimeMessage) content;
            } else
                part = "";

            EmailElementCache eecache = new EmailElementCache();
            HashSet unique = new HashSet();
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "From"), EmailElementCache.EMAIL_TYPE_FROM, unique);
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "Reply-To"), EmailElementCache.EMAIL_TYPE_REPLY_TO, unique);
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "To"), EmailElementCache.EMAIL_TYPE_TO);
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "Cc"), EmailElementCache.EMAIL_TYPE_CC);
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "Bcc"), EmailElementCache.EMAIL_TYPE_BCC);

            String subject = mm.getSubject();
            if (subject != null)
                m.addAttribute(MailService.E_SUBJECT, StringUtil.stripControlCharacters(subject), Element.DISP_CONTENT);
            String messageID = mm.getMessageID();
            if (messageID != null && !messageID.trim().equals(""))
                m.addAttribute(MailService.E_MSG_ID_HDR, StringUtil.stripControlCharacters(messageID), Element.DISP_CONTENT);

            if (wholeMessage && msg.isDraft()) {
                if (msg.getDraftOrigId() > 0)
                    m.addAttribute(MailService.A_ORIG_ID, msg.getDraftOrigId());
                if (!msg.getDraftReplyType().equals(""))
                    m.addAttribute(MailService.A_REPLY_TYPE, msg.getDraftReplyType());
                String inReplyTo = mm.getHeader("In-Reply-To", null);
                if (inReplyTo != null && !inReplyTo.equals(""))
                    m.addAttribute(MailService.E_IN_REPLY_TO, StringUtil.stripControlCharacters(inReplyTo), Element.DISP_CONTENT);
            }

            if (!wholeMessage)
                m.addAttribute(MailService.A_SIZE, mm.getSize());

            java.util.Date sent = mm.getSentDate();
            if (sent != null)
                m.addAttribute(MailService.A_SENT_DATE, sent.getTime());
            
            if (msg.isInvite())
                encodeInvitesForMessage(m, msg, Change.ALL_FIELDS);
            
            List parts = Mime.getParts(mm);
            if (parts != null && !parts.isEmpty()) {
                MPartInfo body = Mime.getBody(parts, wantHTML);
                addParts(m, (MPartInfo) parts.get(0), body, part);
            }
        } catch (IOException ex) {
            throw ServiceException.FAILURE(ex.getMessage(), ex);
        } catch (MessagingException ex) {
            throw ServiceException.FAILURE(ex.getMessage(), ex);
        }
        return m;
	}
    
    
    /**
     * Encodes a Message object into <m> element with <mp> elements
     * for message body.
     * @param msg
     * @param metaDataOnly
     * @return
     * @throws ServiceException
     */
    public static Element encodeApptAsMP(Element parent, Appointment appt, int invId, boolean wantHTML, String part)
    throws ServiceException {
        boolean wholeMessage = (part == null || part.trim().equals(""));
        
        Element m;
        if (wholeMessage) {
            m = encodeMessageCommon(parent, appt, Change.ALL_FIELDS);
            ParsedItemID pid = ParsedItemID.create(appt.getId(), invId);
            m.addAttribute(MailService.A_ID, pid.toString());
        } else {
            m = parent.addElement(MailService.E_MSG);
            ParsedItemID pid = ParsedItemID.create(appt.getId(), invId);
            m.addAttribute(MailService.A_ID, pid.toString());
            m.addAttribute(MailService.A_PART, part);
        }

        try {
            MimeMessage mm = appt.getMimeMessage(invId);
            if (!wholeMessage) {
                MimePart mp = Mime.getMimePart(mm, part);
                if (mp == null)
                    throw MailServiceException.NO_SUCH_PART(part);
                Object content = mp.getContent();
                if (!(content instanceof MimeMessage))
                    throw MailServiceException.NO_SUCH_PART(part);
                mm = (MimeMessage) content;
            } else
                part = "";

            EmailElementCache eecache = new EmailElementCache();
            HashSet unique = new HashSet();
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "From"), EmailElementCache.EMAIL_TYPE_FROM, unique);
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "Reply-To"), EmailElementCache.EMAIL_TYPE_REPLY_TO, unique);
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "To"), EmailElementCache.EMAIL_TYPE_TO);
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "Cc"), EmailElementCache.EMAIL_TYPE_CC);
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "Bcc"), EmailElementCache.EMAIL_TYPE_BCC);

            String subject = mm.getSubject();
            if (subject != null)
                m.addAttribute(MailService.E_SUBJECT, StringUtil.stripControlCharacters(subject), Element.DISP_CONTENT);
            String messageID = mm.getMessageID();
            if (messageID != null && !messageID.trim().equals(""))
                m.addAttribute(MailService.E_MSG_ID_HDR, StringUtil.stripControlCharacters(messageID), Element.DISP_CONTENT);

//            if (wholeMessage && msg.isDraft()) {
//                if (msg.getDraftOrigId() > 0)
//                    m.addAttribute(MailService.A_ORIG_ID, msg.getDraftOrigId());
//                if (!msg.getDraftReplyType().equals(""))
//                    m.addAttribute(MailService.A_REPLY_TYPE, msg.getDraftReplyType());
//                String inReplyTo = mm.getHeader("In-Reply-To", null);
//                if (inReplyTo != null && !inReplyTo.equals(""))
//                    m.addAttribute(MailService.E_IN_REPLY_TO, StringUtil.stripControlCharacters(inReplyTo), Element.DISP_CONTENT);
//            }

            if (!wholeMessage)
                m.addAttribute(MailService.A_SIZE, mm.getSize());

            java.util.Date sent = mm.getSentDate();
            if (sent != null)
                m.addAttribute(MailService.A_SENT_DATE, sent.getTime());

            Element invElt = m.addElement(MailService.E_INVITE); 
            Invite[] invs = appt.getInvites(invId);
            for (int i = 0; i < invs.length; i++) {
                encodeInvite(invElt, invs[i], Change.ALL_FIELDS);
            }
            
//            if (msg.isInvite())
//                encodeInvitesForMessage(m, msg, Change.ALL_FIELDS);
            
            List parts = Mime.getParts(mm);
            if (parts != null && !parts.isEmpty()) {
                MPartInfo body = Mime.getBody(parts, wantHTML);
                addParts(m, (MPartInfo) parts.get(0), body, part);
            }
        } catch (IOException ex) {
            throw ServiceException.FAILURE(ex.getMessage(), ex);
        } catch (MessagingException ex) {
            throw ServiceException.FAILURE(ex.getMessage(), ex);
        }
        return m;
    }
    

    private static final int    MAX_INLINE_MSG_SIZE = 40000;
    private static final String CONTENT_SERVLET_URI = "/service/content/get?id=";
    private static final String PART_PARAM_STRING   = "&part=";

	/**
	 * Encodes a Message object into <m> element with standard MIME content.
	 * @param msg
	 * @param part TODO
	 * @return
	 * @throws ServiceException
	 */
	public static Element encodeMessageAsMIME(Element parent, Message msg, String part)
	throws ServiceException {
        boolean wholeMessage = (part == null || part.trim().equals(""));

        Element m;
        if (wholeMessage) {
            m = encodeMessageCommon(parent, msg, Change.ALL_FIELDS);
            m.addAttribute(MailService.A_ID, msg.getId());
        } else {
            m = parent.addElement(MailService.E_MSG);
            m.addAttribute(MailService.A_ID, msg.getId());
            m.addAttribute(MailService.A_PART, part);
        }

        Element content = m.addUniqueElement(MailService.E_CONTENT);
        int size = (int) msg.getSize() + 2048;
        if (!wholeMessage)
            content.addAttribute(MailService.A_URL, CONTENT_SERVLET_URI + msg.getId() + PART_PARAM_STRING + part);
        else if (size > MAX_INLINE_MSG_SIZE)
            content.addAttribute(MailService.A_URL, CONTENT_SERVLET_URI + msg.getId());
        else
			try {
				byte[] raw = ByteUtil.getContent(msg.getRawMessage(), size);
    			if (!ByteUtil.isASCII(raw))
                    content.addAttribute(MailService.A_URL, CONTENT_SERVLET_URI + msg.getId());
                else
                    content.setText(new String(raw, "US-ASCII"));
            } catch (IOException ex) {
                throw ServiceException.FAILURE(ex.getMessage(), ex);
            }

        return m;
	}

    public static final class OutputParticipants {
        public static final OutputParticipants PUT_SENDERS    = new OutputParticipants();
        public static final OutputParticipants PUT_RECIPIENTS = new OutputParticipants();
        public static final OutputParticipants PUT_BOTH       = new OutputParticipants();
        private OutputParticipants() { }
    }

    public static Element encodeMessageSummary(Element parent, Message msg, OutputParticipants output) {
        return encodeMessageSummary(parent, msg, output, Change.ALL_FIELDS);
    }
    public static Element encodeMessageSummary(Element parent, Message msg, OutputParticipants output, int fields) {
        Element e = encodeMessageCommon(parent, msg, fields);
        e.addAttribute(MailService.A_ID, msg.getId());

        if (!needToOutput(fields, Change.MODIFIED_CONTENT))
            return e;

        EmailElementCache eecache = new EmailElementCache();
        boolean addRecips  = msg.isFromMe() && (output == OutputParticipants.PUT_RECIPIENTS || output == OutputParticipants.PUT_BOTH);
        boolean addSenders = output == OutputParticipants.PUT_BOTH || !addRecips;
        if (addRecips)
			try {
				addEmails(e, eecache, InternetAddress.parseHeader(msg.getRecipients(), false), EmailElementCache.EMAIL_TYPE_TO);
			} catch (AddressException e1) { }
		if (addSenders)
            eecache.makeEmail(e, msg.getSender(), EmailElementCache.EMAIL_TYPE_FROM, null);

        e.addAttribute(MailService.E_SUBJECT, StringUtil.stripControlCharacters(msg.getSubject()), Element.DISP_CONTENT);

        // fragment has already been sanitized...
        String fragment = msg.getFragment();
        if (!fragment.equals(""))
            e.addAttribute(MailService.E_FRAG, fragment, Element.DISP_CONTENT);

        
        if (msg.isInvite()) 
            try {
                encodeInvitesForMessage(e, msg, fields);
            } catch (ServiceException ex) {
                mLog.debug("Caught exception while encoding Invites for msg "+msg.getId(),ex);
            }
        
        return e;
    }
    
    private static Element encodeMessageCommon(Element parent, MailItem mi, int fields) {
        Element elem = parent.addElement(MailService.E_MSG);
        // DO NOT encode the item-id here, as some Invite-Messages-In-Appointments have special item-id's
        if (needToOutput(fields, Change.MODIFIED_CONTENT) && mi.getSavedSequence() != 0)
            elem.addAttribute(MailService.A_REVISION, mi.getSavedSequence());
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            elem.addAttribute(MailService.A_SIZE, mi.getSize());
        if (needToOutput(fields, Change.MODIFIED_DATE))
            elem.addAttribute(MailService.A_DATE, mi.getDate());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            elem.addAttribute(MailService.A_FOLDER, mi.getFolderId());
        if (mi instanceof Message) {
            Message msg = (Message)mi;
            if (needToOutput(fields, Change.MODIFIED_PARENT) && (fields != Change.ALL_FIELDS || msg.getConversationId() != -1))
                elem.addAttribute(MailService.A_CONV_ID, msg.getConversationId());
        }
        recordItemTags(elem, mi, fields);
        if (needToOutput(fields, Change.MODIFIED_CONFLICT))
            elem.addAttribute(MailService.A_CHANGE_DATE, mi.getChangeDate() / 1000);
        return elem;
    }
    
    public static Element encodeInvite(Element parent, Invite invite, int fields) 
    {
        boolean allFields = true;
        
        if (fields != Change.ALL_FIELDS) {
            allFields = false;
            if (!needToOutput(fields, Change.MODIFIED_INVITE)) {
                return parent;
            }
        }
            
        Element e = parent.addElement(MailService.E_INVITE_COMPONENT);

        e.addAttribute(MailService.A_APPT_COMPONENT_NUM, invite.getComponentNum());
        
        // currently the RSVP attribute is the only attribute that changes
        e.addAttribute(MailService.A_APPT_RSVP, invite.needsReply());
        
        if (allFields) {
            try {
                e.addAttribute("x_uid", invite.getUid());
                
                Appointment appt = invite.getAppointment();
                if (appt != null) {
                    e.addAttribute(MailService.A_APPT_ID, appt.getId());
                }
                
                if (invite.thisAcctIsOrganizer(appt.getMailbox().getAccount())) {
                    e.addAttribute(MailService.A_APPT_ISORG, true);
                }
                
                Recurrence.IRecurrence recur = invite.getRecurrence();
                if (recur != null) {
                    Element recurElt = e.addElement(MailService.E_APPT_RECUR);
                    recur.toXml(recurElt);
                }
            } catch(ServiceException ex) {
                ex.printStackTrace();
            }
            
            e.addAttribute(MailService.A_APPT_TYPE, invite.getType());
            
            e.addAttribute(MailService.A_APPT_STATUS, invite.getStatus());

            e.addAttribute(MailService.A_APPT_FREEBUSY, invite.getFreeBusy());

            e.addAttribute(MailService.A_APPT_FREEBUSY_ACTUAL, invite.getFreeBusyActual());

            e.addAttribute(MailService.A_APPT_TRANSPARENCY, invite.getTransparency());
            
            if (invite.isAllDayEvent())
                e.addAttribute(MailService.A_APPT_ALLDAY, true);
            
            if (invite.getStartTime() != null) {
                Element startElt = e.addElement(MailService.E_APPT_START_TIME);
                ParsedDateTime dtStart = invite.getStartTime();
                startElt.addAttribute(MailService.A_APPT_DATETIME, dtStart.getDateTimePartString());
                String tzName = dtStart.getTZName();
                if (tzName != null) {
                    startElt.addAttribute(MailService.A_APPT_TIMEZONE, tzName);
                }
            }
            
            ParsedDateTime dtEnd = invite.getEndTime();
            if (dtEnd != null) {
                Element endElt = e.addElement(MailService.E_APPT_END_TIME);
                endElt.addAttribute(MailService.A_APPT_DATETIME, dtEnd.getDateTimePartString());
                String tzName = dtEnd.getTZName();
                if (tzName != null) {
                    endElt.addAttribute(MailService.A_APPT_TIMEZONE, tzName);
                }
            }
            
            ParsedDuration dur = invite.getDuration();
            if (dur != null) {
                dur.toXml(e);
            }
            
            e.addAttribute(MailService.A_NAME, invite.getName());
            e.addAttribute(MailService.A_APPT_LOCATION, invite.getLocation());
            
            // Organizer
            Organizer org = invite.getOrganizer();
            if (org != null) {
                Element orgElt = e.addUniqueElement(MailService.E_APPT_ORGANIZER);
                orgElt.addAttribute(MailService.A_DISPLAY, CalendarUtils.paramVal(org, Parameter.CN));
                orgElt.addAttribute(MailService.A_URL, org.getCalAddress().toASCIIString());
            }
            
            // Attendee(s)
            Collection /*Invite.Attendee*/ ats = invite.getAttendees();
            for (Iterator atsIter = ats.iterator();atsIter.hasNext();) {
                Element atElt = e.addElement(MailService.E_APPT_ATTENDEE);
                
                Attendee at = (Attendee)atsIter.next();
                
                // display name 
                atElt.addAttribute(MailService.A_DISPLAY, CalendarUtils.paramVal(at,Parameter.CN ));
                
                // role
                String role = CalendarUtils.paramVal(at, Parameter.ROLE);
                atElt.addAttribute(MailService.A_APPT_ROLE, IcalXmlStrMap.sRoleMap.toXml(role));
                
                // participation status
                String partStat = CalendarUtils.paramVal(at, Parameter.PARTSTAT);
                atElt.addAttribute(MailService.A_APPT_PARTSTAT, IcalXmlStrMap.sPartStatMap.toXml(partStat));
                
                // uri
                atElt.addAttribute(MailService.A_URL, at.getCalAddress().toASCIIString());
            }
        }
        return e;
    }
    

//  public static Element encodeInviteMsg(Element parent, Message invite) {
//  return encodeInviteMsg(parent, invite, Change.ALL_FIELDS);
//  }
    
    private static Element encodeInvitesForMessage(Element parent, Message msg, int fields) throws ServiceException 
    {
        if (fields != Change.ALL_FIELDS) {
            if (!needToOutput(fields, Change.MODIFIED_INVITE)) {
                return parent;
            }
        }
        
        Element ie = parent.addElement(MailService.E_INVITE);
        
        boolean addedMethod = false;
        Mailbox mbox = msg.getMailbox();
        
        for (Iterator iter = msg.getApptInfoIterator(); iter.hasNext();) {
            Message.ApptInfo info = (Message.ApptInfo)iter.next();
            
            Appointment appt = null;
            try {
                appt = mbox.getAppointmentById(info.getAppointmentId());
            } catch (MailServiceException.NoSuchItemException e) {}
            if (appt != null) {
                Invite inv = appt.getInvite(msg.getId(), info.getComponentNo());
                
                if (inv != null) {
                    if (!addedMethod) {
//                        ie.addAttribute("method", inv.getMethod());
                        addedMethod = true;
                    }
                    encodeInvite(ie, inv, fields);
                } else {
                    // invite not in this appointment anymore
                }
            } else {
                // couldn't find appointment
            }
            
        }
        
        return ie;
    }

    
    private static void addParts(Element parent, MPartInfo mpi, MPartInfo body, String prefix) {
        Element elem = parent.addElement(MailService.E_MIMEPART);
        MimePart mp = mpi.getMimePart();
        
        String part = mpi.getPartName();
        part = prefix + (prefix.equals("") || part.equals("") ? "" : ".") + part;
        elem.addAttribute(MailService.A_PART, part);
        
        String contentTypeString = StringUtil.stripControlCharacters(mpi.getContentTypeString());
        
        elem.addAttribute(MailService.A_CONTENT_TYPE, contentTypeString);
        
        // figure out attachment size
        try {
            int size = mp.getSize();
            if (size >= 0) {
                elem.addAttribute(MailService.A_SIZE, mp.getSize());
            }
        } catch (MessagingException me) {
            // don't put out size if we get exception
        }
        
        // figure out attachment disposition
        try {
            String disp = mp.getHeader("Content-Disposition", null);
            if (disp != null) {
                ContentDisposition cdisp = new ContentDisposition(MimeUtility.decodeText(disp));
                elem.addAttribute(MailService.A_CONTENT_DISPOSTION, StringUtil.stripControlCharacters(cdisp.getDisposition())); 
            }
        } catch (MessagingException me) {
        } catch (UnsupportedEncodingException uee) {
        }
        
        // figure out attachment name
        try {
            String fname = mp.getFileName();
            if (fname != null)
                elem.addAttribute(MailService.A_CONTENT_FILENAME, StringUtil.stripControlCharacters(fname));
        } catch (MessagingException me) { }
        
        // figure out content-id (used in displaying attached images)
        try {
            String cid = mp.getContentID();
            if (cid != null)
                elem.addAttribute(MailService.A_CONTENT_ID, StringUtil.stripControlCharacters(cid));
        } catch (MessagingException me) { }
        
        
        // figure out content-location (used in displaying attached images)
        try {
            String cl = mp.getHeader("Content-Location", null);
            if (cl != null)
                elem.addAttribute(MailService.A_CONTENT_LOCATION, StringUtil.stripControlCharacters(cl));
        } catch (MessagingException me) { }

        // include the part's content if this is the displayable "memo part"
        if (mpi == body) {
            elem.addAttribute(MailService.A_BODY, true);
            try {
                getContent(elem, mpi);
            } catch (IOException ioe) {
                if (mLog.isWarnEnabled())
                    mLog.warn("error writing body part: ", ioe);
            } catch (MessagingException me) {
            }
        }
        
        // recurse to child parts, if any
        if (mpi.hasChildren()) {
            for (Iterator it = mpi.getChildren().iterator(); it.hasNext();) {
                MPartInfo cp = (MPartInfo) it.next();
                addParts(elem, cp, body, prefix);
            }
        }
    }

    /**
     * @param mp
     * @param pi
     * @throws MessagingException
     * @throws IOException
     */
    private static void getContent(Element mp, MPartInfo pi) throws IOException, MessagingException {
        // TODO: support other parts
        if (pi.getContentType().match(Mime.CT_TEXT_WILD)) {
            MimePart part = pi.getMimePart();

            Object o = part.getContent();
            String cstr = null;
            if (o instanceof String) {
                cstr = (String) o;
            } else if (o instanceof InputStream) {
                InputStream is = (InputStream) o;
                cstr = new String(ByteUtil.getContent(is, 4096));
            } else {
                return;
            }

            //mLog.info("before strip: "+cstr);
            String data = StringUtil.stripControlCharacters(cstr);
            //mLog.info("after strip: "+data);
            boolean isHtml = pi.getContentType().match(Mime.CT_TEXT_HTML);
            if (isHtml) {
                data = HtmlDefang.defang(data, true);
                //mLog.info("after defang: "+data);
            }

            mp.addAttribute(MailService.E_CONTENT, data, Element.DISP_CONTENT);
            //content.addAttribute(MailService.A_CONTENT_TYPE, )
            //content.addText((String) part.getContent());
            // TODO: CDATA worth the effort?
//            if (isHtml && data.indexOf("]]>") == -1)
//                content.addCDATA(data);
//            else
//                content.addText(data);
        }
    }

	/**
     * @param m
     * @param eecache
     * @param recipients
     * @param email_type_to
     */
    private static void addEmails(Element m, EmailElementCache eecache,
    							  InternetAddress[] recipients, int emailType) {
        addEmails(m, eecache, recipients, emailType, null);
    }
    private static void addEmails(Element m, EmailElementCache eecache,
    		                      InternetAddress[] recipients, int emailType, HashSet unique) {
        if (recipients == null || recipients.length == 0)
            return;
        for (int i = 0; i < recipients.length; i++)
            eecache.makeEmail(m, recipients[i], emailType, unique);
    }
}
