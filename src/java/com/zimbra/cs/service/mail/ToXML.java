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

import javax.mail.MessagingException;
import javax.mail.internet.*;

import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.html.HtmlDefang;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone.SimpleOnset;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.EmailElementCache.CacheNode;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;


/**
 * @author jhahm
 *
 * Class containing static methods for encoding various MailItem-derived
 * objects into XML.
 */
public class ToXML {

    // we usually don't want to return last modified date...
    public static final int NOTIFY_FIELDS = Change.ALL_FIELDS & ~Change.MODIFIED_CONFLICT;

    private static Log mLog = LogFactory.getLog(ToXML.class);

	// no construction
	private ToXML()  {}

    public static Element encodeItem(Element parent, ZimbraContext lc, MailItem item) {
        return encodeItem(parent, lc, item, NOTIFY_FIELDS);
    }
    public static Element encodeItem(Element parent, ZimbraContext lc, MailItem item, int fields) {
        if (item instanceof SearchFolder)
            return encodeSearchFolder(parent, lc, (SearchFolder) item, fields);
        else if (item instanceof Folder)
            return encodeFolder(parent, lc, (Folder) item, fields);
        else if (item instanceof Tag)
            return encodeTag(parent, lc, (Tag) item, fields);
        else if (item instanceof Note)
            return encodeNote(parent, lc, (Note) item, fields);
        else if (item instanceof Contact)
            return encodeContact(parent, lc, (Contact) item, null, false, null, fields);
        else if (item instanceof Appointment) 
            return encodeApptSummary(parent, lc, (Appointment) item, fields);
        else if (item instanceof Conversation)
            return encodeConversationSummary(parent, lc, (Conversation) item, fields);
        else if (item instanceof WikiItem)
            return encodeWiki(parent, lc, (WikiItem) item, fields, -1);
        else if (item instanceof Message) {
            OutputParticipants output = (fields == NOTIFY_FIELDS ? OutputParticipants.PUT_BOTH : OutputParticipants.PUT_SENDERS);
            return encodeMessageSummary(parent, lc, (Message) item, output, fields);
        }
        return null;
    }

    private static boolean needToOutput(int fields, int fieldMask) {
        return ((fields & fieldMask) > 0);
    }

    public static Element encodeMailbox(Element parent, Mailbox mbox) {
        return encodeMailbox(parent, mbox, NOTIFY_FIELDS);
    }
    public static Element encodeMailbox(Element parent, Mailbox mbox, int fields) {
        Element elem = parent.addUniqueElement(MailService.E_MAILBOX);
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            elem.addAttribute(MailService.A_SIZE, mbox.getSize());
        return elem;
    }

    public static Element encodeFolder(Element parent, ZimbraContext lc, Folder folder) {
        return encodeFolder(parent, lc, folder, NOTIFY_FIELDS);
    }
    public static Element encodeFolder(Element parent, ZimbraContext lc, Folder folder, int fields) {
        if (folder instanceof SearchFolder)
            return encodeSearchFolder(parent, lc, (SearchFolder) folder, fields);
        else if (folder instanceof Mountpoint)
            return encodeMountpoint(parent, lc, (Mountpoint) folder, fields);

        Element elem = parent.addElement(MailService.E_FOLDER);
        encodeFolderCommon(elem, lc, folder, fields);
        if (needToOutput(fields, Change.MODIFIED_FLAGS))
            elem.addAttribute(MailService.A_EXCLUDE_FREEBUSY, folder.isTagged(folder.getMailbox().mExcludeFBFlag));
        if (needToOutput(fields, Change.MODIFIED_SIZE)) {
            long size = folder.getSize();
            if (size > 0 || fields != NOTIFY_FIELDS)
                elem.addAttribute(MailService.A_SIZE, size);
        }
        if (needToOutput(fields, Change.MODIFIED_MSG_COUNT)) {
            int msgs = folder.getMessageCount();
            if (msgs > 0 || fields != NOTIFY_FIELDS)
                elem.addAttribute(MailService.A_NUM, msgs);
        }
        if (needToOutput(fields, Change.MODIFIED_URL)) {
            String url = folder.getUrl();
            if (!url.equals("") || fields != NOTIFY_FIELDS) {
                if (url.indexOf('@') != -1)
                    try {
                        HttpURL httpurl = new HttpURL(url);
                        if (httpurl.getPassword() != null) {
                            httpurl.setPassword("");
                            url = httpurl.toString();
                        }
                    } catch (org.apache.commons.httpclient.URIException urie) { }
                elem.addAttribute(MailService.A_URL, url);
            }
        }
        if (lc.isDelegatedRequest())
            try {
                Mailbox.OperationContext octxt = lc.getOperationContext();
                short perms = folder.getMailbox().getEffectivePermissions(octxt, folder.getId(), MailItem.TYPE_FOLDER);
                elem.addAttribute(MailService.A_RIGHTS, ACL.rightsToString(perms));
            } catch (ServiceException e) {
                mLog.warn("ignoring exception while fetching effective permissions for folder " + folder.getId(), e);
            }
        if (needToOutput(fields, Change.MODIFIED_ACL)) {
            ACL acl = folder.getPermissions();
            if (acl != null || fields != NOTIFY_FIELDS) {
                Element eACL = elem.addUniqueElement(MailService.E_ACL);
                if (acl != null)
                    for (Iterator it = acl.grantIterator(); it.hasNext(); ) {
                        ACL.Grant grant = (ACL.Grant) it.next();
                        NamedEntry nentry = FolderAction.lookupGranteeByZimbraId(grant.getGranteeId(), grant.getGranteeType());
                        eACL.addElement(MailService.E_GRANT)
                            .addAttribute(MailService.A_ZIMBRA_ID, grant.getGranteeId())
                            .addAttribute(MailService.A_GRANT_TYPE, FolderAction.typeToString(grant.getGranteeType()))
                            .addAttribute(MailService.A_INHERIT, grant.isGrantInherited())
                            .addAttribute(MailService.A_RIGHTS, ACL.rightsToString(grant.getGrantedRights()))
                            .addAttribute(MailService.A_DISPLAY, nentry == null ? null : nentry.getName());
                    }
            }
        }
        return elem;
	}

    private static Element encodeFolderCommon(Element elem, ZimbraContext lc, Folder folder, int fields) {
        int folderId = folder.getId();
        elem.addAttribute(MailService.A_ID, lc.formatItemId(folder));

        if (folderId != Mailbox.ID_FOLDER_ROOT) {
            if (needToOutput(fields, Change.MODIFIED_NAME)) {
                String name = folder.getName();
                if (name != null && name.length() > 0)
                    elem.addAttribute(MailService.A_NAME, name);
            }
            if (needToOutput(fields, Change.MODIFIED_FOLDER))
                elem.addAttribute(MailService.A_FOLDER, lc.formatItemId(folder.getFolderId()));
        }
        if (needToOutput(fields, Change.MODIFIED_COLOR)) {
            byte color = folder.getColor();
            if (color != MailItem.DEFAULT_COLOR || fields != NOTIFY_FIELDS)
                elem.addAttribute(MailService.A_COLOR, color);
        }
        if (needToOutput(fields, Change.MODIFIED_UNREAD)) {
            int unread = folder.getUnreadCount();
            if (unread > 0 || fields != NOTIFY_FIELDS)
                elem.addAttribute(MailService.A_UNREAD, unread);
        }
        if (needToOutput(fields, Change.MODIFIED_VIEW)) {
            byte view = folder.getDefaultView();
            if (view != MailItem.TYPE_UNKNOWN)
                elem.addAttribute(MailService.A_DEFAULT_VIEW, MailItem.getNameForType(view));
        }
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            elem.addAttribute(MailService.A_CHANGE_DATE, folder.getChangeDate() / 1000);
            elem.addAttribute(MailService.A_MODIFIED_SEQUENCE, folder.getModifiedSequence());
        }
        return elem;
    }

    public static Element encodeSearchFolder(Element parent, ZimbraContext lc, SearchFolder search) {
        return encodeSearchFolder(parent, lc, search, NOTIFY_FIELDS);
    }
    public static Element encodeSearchFolder(Element parent, ZimbraContext lc, SearchFolder search, int fields) {
        Element elem = parent.addElement(MailService.E_SEARCH);
        encodeFolderCommon(elem, lc, search, fields);
        if (needToOutput(fields, Change.MODIFIED_QUERY)) {
            elem.addAttribute(MailService.A_QUERY, search.getQuery());
            elem.addAttribute(MailService.A_SORTBY, search.getSortField());
            elem.addAttribute(MailService.A_SEARCH_TYPES, search.getReturnTypes());
        }
        return elem;
    }

    public static Element encodeMountpoint(Element parent, ZimbraContext lc, Mountpoint mpt) {
        return encodeMountpoint(parent, lc, mpt, NOTIFY_FIELDS);
    }
    public static Element encodeMountpoint(Element parent, ZimbraContext lc, Mountpoint mpt, int fields) {
        Element elem = parent.addElement(MailService.E_MOUNT);
        encodeFolderCommon(elem, lc, mpt, fields);
        NamedEntry nentry = FolderAction.lookupGranteeByZimbraId(mpt.getOwnerId(), ACL.GRANTEE_USER);
        elem.addAttribute(MailService.A_DISPLAY, nentry == null ? null : nentry.getName());
        if (fields == NOTIFY_FIELDS && mpt.getDefaultView() != MailItem.TYPE_UNKNOWN)
            elem.addAttribute(MailService.A_DEFAULT_VIEW, MailItem.getNameForType(mpt.getDefaultView()));
        return elem;
    }

    public static void recordItemTags(Element elem, MailItem item, int fields) {
        if (needToOutput(fields, Change.MODIFIED_FLAGS | Change.MODIFIED_UNREAD)) {
        	String flags = item.getFlagString();
        	if (fields != NOTIFY_FIELDS || !flags.equals(""))
                elem.addAttribute(MailService.A_FLAGS, flags);
        }
        if (needToOutput(fields, Change.MODIFIED_TAGS)) {
            String tags = item.getTagString();
            if (!tags.equals("") || fields != NOTIFY_FIELDS)
                elem.addAttribute(MailService.A_TAGS, tags);
        }
    }

    public static Element encodeContact(Element parent, ZimbraContext lc, Contact contact,
										ContactAttrCache cacache, boolean summary, List<String> attrFilter) {
        return encodeContact(parent, lc, contact, cacache, summary, attrFilter, NOTIFY_FIELDS);
    }
    public static Element encodeContact(Element parent, ZimbraContext lc, Contact contact,
            ContactAttrCache cacache, boolean summary, List<String> attrFilter, int fields) {
        Element elem = parent.addElement(MailService.E_CONTACT);
        elem.addAttribute(MailService.A_ID, lc.formatItemId(contact));
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            elem.addAttribute(MailService.A_FOLDER, lc.formatItemId(contact.getFolderId()));
        recordItemTags(elem, contact, fields);
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
        	elem.addAttribute(MailService.A_CHANGE_DATE, contact.getChangeDate() / 1000);
        	elem.addAttribute(MailService.A_MODIFIED_SEQUENCE, contact.getModifiedSequence());
        }
        if (needToOutput(fields, Change.MODIFIED_CONTENT))
            elem.addAttribute(MailService.A_REVISION, contact.getSavedSequence());

        if (summary || !needToOutput(fields, Change.MODIFIED_CONTENT))
            return elem;

        Map<String, String> attrs = contact.getFields();
        if (attrFilter != null) {
            for (String name : attrFilter) {
            	// XXX: How to distinguish between a non-existent attribute and
            	//      an existing attribute with null or empty string value?
                String value = attrs.get(name);
				if (value == null || value.equals(""))
				    continue;
                if (cacache != null)
                    cacache.makeAttr(elem, name, value);
                else
                    elem.addAttribute(name, value, Element.DISP_ELEMENT);
            }
        } else {
            for (Map.Entry<String, String> me : attrs.entrySet()) {
                String name = me.getKey();
                String value = me.getValue();
				if (name == null || name.trim().equals("") || value == null || value.equals(""))
				    continue;
                if (cacache != null)
                    cacache.makeAttr(elem, name, value);
                else
                    elem.addAttribute(name, value, Element.DISP_ELEMENT);
            }
        }
        return elem;
    }

	public static Element encodeNote(Element parent, ZimbraContext lc, Note note) {
        return encodeNote(parent, lc, note, NOTIFY_FIELDS);
    }
    public static Element encodeNote(Element parent, ZimbraContext lc, Note note, int fields) {
        Element elem = parent.addElement(MailService.E_NOTE);
        elem.addAttribute(MailService.A_ID, lc.formatItemId(note));
        if (needToOutput(fields, Change.MODIFIED_CONTENT) && note.getSavedSequence() != 0)
            elem.addAttribute(MailService.A_REVISION, note.getSavedSequence());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            elem.addAttribute(MailService.A_FOLDER, lc.formatItemId(note.getFolderId()));
        if (needToOutput(fields, Change.MODIFIED_DATE))
            elem.addAttribute(MailService.A_DATE, note.getDate());
        recordItemTags(elem, note, fields);
        if (needToOutput(fields, Change.MODIFIED_POSITION))
            elem.addAttribute(MailService.A_BOUNDS, note.getBounds().toString());
        if (needToOutput(fields, Change.MODIFIED_COLOR))
            elem.addAttribute(MailService.A_COLOR, note.getColor());
        if (needToOutput(fields, Change.MODIFIED_CONTENT))
            elem.addAttribute(MailService.E_CONTENT, note.getContent(), Element.DISP_CONTENT);
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            elem.addAttribute(MailService.A_CHANGE_DATE, note.getChangeDate() / 1000);
            elem.addAttribute(MailService.A_MODIFIED_SEQUENCE, note.getModifiedSequence());
        }
        return elem;
	}

	public static Element encodeTag(Element parent, ZimbraContext lc, Tag tag) {
        return encodeTag(parent, lc, tag, NOTIFY_FIELDS);
    }
    public static Element encodeTag(Element parent, ZimbraContext lc, Tag tag, int fields) {
		Element elem = parent.addElement(MailService.E_TAG);
		elem.addAttribute(MailService.A_ID, lc.formatItemId(tag));
		if (needToOutput(fields, Change.MODIFIED_NAME))
            elem.addAttribute(MailService.A_NAME, tag.getName());
		if (needToOutput(fields, Change.MODIFIED_COLOR))
            elem.addAttribute(MailService.A_COLOR, tag.getColor());
        if (needToOutput(fields, Change.MODIFIED_UNREAD)) {
    		int unreadCount = tag.getUnreadCount();
    		if (unreadCount > 0 || fields != NOTIFY_FIELDS)
                elem.addAttribute(MailService.A_UNREAD, unreadCount);
        }
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            elem.addAttribute(MailService.A_CHANGE_DATE, tag.getChangeDate() / 1000);
            elem.addAttribute(MailService.A_MODIFIED_SEQUENCE, tag.getModifiedSequence());
        }
		return elem;
	}


    public static Element encodeConversation(Element parent, ZimbraContext lc, Conversation conv) throws ServiceException {
        int fields = NOTIFY_FIELDS;
        Element c = encodeConversationCommon(parent, lc, conv, fields);
        EmailElementCache eecache = new EmailElementCache();
        Message[] messages = conv.getMailbox().getMessagesByConversation(lc.getOperationContext(), conv.getId());
        for (int i = 0; i < messages.length; i++) {
            Message msg = messages[i];
            Element m = c.addElement(MailService.E_MSG);
            m.addAttribute(MailService.A_ID, lc.formatItemId(msg));
            m.addAttribute(MailService.A_DATE, msg.getDate());
            m.addAttribute(MailService.A_SIZE, msg.getSize());
            m.addAttribute(MailService.A_FOLDER, lc.formatItemId(msg.getFolderId()));
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
     * @param lc TODO
     * @param conv
     * @param msgHit TODO
     * @param eecache
     */
    public static Element encodeConversationSummary(Element parent, ZimbraContext lc, Conversation conv, int fields) {
        return encodeConversationSummary(parent, lc, conv, null, null, OutputParticipants.PUT_SENDERS, fields);
    }
    public static Element encodeConversationSummary(Element parent, ZimbraContext lc, Conversation conv, Message msgHit,
                                                   EmailElementCache eecache, OutputParticipants output) {
        return encodeConversationSummary(parent, lc, conv, msgHit, eecache, output, NOTIFY_FIELDS);
    }
    private static Element encodeConversationSummary(Element parent, ZimbraContext lc, Conversation conv, Message msgHit,
                                                     EmailElementCache eecache, OutputParticipants output, int fields) {
        Element c = encodeConversationCommon(parent, lc, conv, fields);
        if (needToOutput(fields, Change.MODIFIED_DATE))
            c.addAttribute(MailService.A_DATE, msgHit != null ? msgHit.getDate() : conv.getDate());
        if (needToOutput(fields, Change.MODIFIED_SUBJECT))
            c.addAttribute(MailService.E_SUBJECT, conv.getSubject());
        if (fields == NOTIFY_FIELDS && msgHit != null)
        	c.addAttribute(MailService.E_FRAG, msgHit.getFragment(), Element.DISP_CONTENT);

        boolean addRecips  = msgHit != null && msgHit.isFromMe() && (output == OutputParticipants.PUT_RECIPIENTS || output == OutputParticipants.PUT_BOTH);
        boolean addSenders = output == OutputParticipants.PUT_BOTH || !addRecips;
        if (addRecips)
            try {
                InternetAddress[] addrs = InternetAddress.parseHeader(msgHit.getRecipients(), false);
                addEmails(c, eecache, addrs, EmailElementCache.EMAIL_TYPE_TO);
            } catch (AddressException e1) { }
        if (addSenders && needToOutput(fields, Change.MODIFIED_SENDERS)) {
            if (eecache == null)
                eecache = new EmailElementCache();
            SenderList sl;
			try {
				sl = conv.getMailbox().getConversationSenderList(conv.getId());
			} catch (ServiceException e) {
				return c;
			}
			CacheNode fa = sl.getFirstAddress();
            if (fa != null) {
                eecache.makeEmail(c, fa, EmailElementCache.EMAIL_TYPE_FROM, null);
                // "<e/>" indicates that some senders may be omitted...
                if (sl.isElided())
                    c.addElement(MailService.E_EMAIL);
            }
            CacheNode[] la = sl.getLastAddresses();
            for (int i = 0; i < la.length; i++) {
                if (la[i] != null)
                    eecache.makeEmail(c, la[i], EmailElementCache.EMAIL_TYPE_FROM, null);
            }
        }

        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            c.addAttribute(MailService.A_CHANGE_DATE, conv.getChangeDate() / 1000);
            c.addAttribute(MailService.A_MODIFIED_SEQUENCE, conv.getModifiedSequence());
        }
        return c;
    }

    private static Element encodeConversationCommon(Element parent, ZimbraContext lc, Conversation conv, int fields) {
        Element c = parent.addElement(MailService.E_CONV);
        c.addAttribute(MailService.A_ID, lc.formatItemId(conv));
        if (needToOutput(fields, Change.MODIFIED_CHILDREN))
            c.addAttribute(MailService.A_NUM, conv.getMessageCount());
        recordItemTags(c, conv, fields);
        if (fields == NOTIFY_FIELDS)
            c.addAttribute(MailService.E_SUBJECT, conv.getSubject(), Element.DISP_CONTENT);
        return c;
    }

	/**
	 * Encodes a Message object into <m> element with <mp> elements
	 * for message body.
	 * @param lc TODO
	 * @param msg
	 * @param metaDataOnly
	 * @return
	 * @throws ServiceException
	 */
	public static Element encodeMessageAsMP(Element parent, ZimbraContext lc, Message msg, boolean wantHTML, String part)
	throws ServiceException {
        boolean wholeMessage = (part == null || part.trim().equals(""));
        
        Element m;
        if (wholeMessage) {
            m = encodeMessageCommon(parent, lc, msg, NOTIFY_FIELDS);
            m.addAttribute(MailService.A_ID, lc.formatItemId(msg));
        } else {
            m = parent.addElement(MailService.E_MSG);
            m.addAttribute(MailService.A_ID, lc.formatItemId(msg));
            m.addAttribute(MailService.A_PART, part);
        }

        try {
            MimeMessage mm = msg.getMimeMessage();
            if (!wholeMessage) {
                MimePart mp = Mime.getMimePart(mm, part);
                if (mp == null)
                    throw MailServiceException.NO_SUCH_PART(part);
                Object content = Mime.getMessageContent(mp);
                if (!(content instanceof MimeMessage))
                    throw MailServiceException.NO_SUCH_PART(part);
                mm = (MimeMessage) content;
            } else
                part = "";

            EmailElementCache eecache = new EmailElementCache();
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "From"), EmailElementCache.EMAIL_TYPE_FROM);
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "Reply-To"), EmailElementCache.EMAIL_TYPE_REPLY_TO);
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
                    m.addAttribute(MailService.A_ORIG_ID, lc.formatItemId(msg.getDraftOrigId()));
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
                encodeInvitesForMessage(m, lc, msg, NOTIFY_FIELDS);
            
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
     * Encode the metadata for the appointment.
     * 
     * The content for the appointment is a big multipart/digest containing each
     * invite in the appointment as a sub-mimepart -- it can be retreived from the content 
     * servlet: 
     *    http://servername/service/content/get?id=<apptId>
     * 
     * The client can ALSO request just the content for each individual invite using a
     * compound item-id request:
     *    http://servername/service/content/get?id=<apptId>-<invite-mail-item-id>
     *    
     * DO NOT use the raw invite-mail-item-id to fetch the content: since the invite is a 
     * standard mail-message it can be deleted by the user at any time!
     * @param parent
     * @param lc TODO
     * @param appt
     * @param fields
     * 
     * @return
     */
    public static Element encodeApptSummary(Element parent, ZimbraContext lc, Appointment appt, int fields) {
        Element apptElt = parent.addElement(MailService.E_APPOINTMENT);
        
        apptElt.addAttribute(MailService.A_UID, appt.getUid());
        apptElt.addAttribute(MailService.A_ID, lc.formatItemId(appt));
        apptElt.addAttribute(MailService.A_FOLDER, lc.formatItemId(appt.getFolderId()));
        
        if (needToOutput(fields, Change.MODIFIED_CONTENT) && appt.getSavedSequence() != 0)
            apptElt.addAttribute(MailService.A_REVISION, appt.getSavedSequence());
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            apptElt.addAttribute(MailService.A_SIZE, appt.getSize());
        if (needToOutput(fields, Change.MODIFIED_DATE))
            apptElt.addAttribute(MailService.A_DATE, appt.getDate());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            apptElt.addAttribute(MailService.A_FOLDER, lc.formatItemId(appt.getFolderId()));
        recordItemTags(apptElt, appt, fields);
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            apptElt.addAttribute(MailService.A_CHANGE_DATE, appt.getChangeDate() / 1000);
            apptElt.addAttribute(MailService.A_MODIFIED_SEQUENCE, appt.getModifiedSequence());
        }

        for (int i = 0; i < appt.numInvites(); i++) {
            Invite inv = appt.getInvite(i);

            Element ie = apptElt.addElement(MailService.E_INVITE);
            encodeTimeZoneMap(ie, appt.getTimeZoneMap());

            encodeReplies(ie, appt, inv);
            
            ie.addAttribute(MailService.A_ID, lc.formatItemId(inv.getMailItemId()));
            ie.addAttribute(MailService.A_APPT_COMPONENT_NUM, inv.getComponentNum());
            if (inv.hasRecurId())
                ie.addAttribute(MailService.A_APPT_RECURRENCE_ID, inv.getRecurId().toString());

            encodeInvite(ie, lc, appt, inv, NOTIFY_FIELDS, false);
        }
        
        return apptElt;
    }
    
    private static void encodeReplies(Element parent, Appointment appt, Invite inv) {
        Element repliesElt = parent.addElement(MailService.A_APPT_REPLIES);

        List /*Appointment.ReplyInfo */ fbas = appt.getReplyInfo(inv);
        for (Iterator iter = fbas.iterator(); iter.hasNext(); ) {
            Appointment.ReplyInfo repInfo = (Appointment.ReplyInfo) iter.next();

            Element curElt = repliesElt.addElement(MailService.E_APPT_REPLY);
            if (repInfo.mRecurId != null) {
                repInfo.mRecurId.toXml(curElt);
            }
            if (repInfo.mAttendee.hasPartStat()) {
                curElt.addAttribute(MailService.A_APPT_PARTSTAT, repInfo.mAttendee.getPartStat());
            }
            curElt.addAttribute(MailService.A_DATE, repInfo.mDtStamp);
            curElt.addAttribute("at", repInfo.mAttendee.getAddress());
        }
    }
    
    
    /**
     * Encodes an Invite stored within an Appointment object into <m> element with <mp> elements
     * for message body.
     * @param lc TODO
     * @param msg
     * @param metaDataOnly
     * @return
     * @throws ServiceException
     */
    public static Element encodeApptInviteAsMP(Element parent, ZimbraContext lc, Appointment appt, ItemId iid, boolean wantHTML, String part)
    throws ServiceException {
        int invId = iid.getSubpartId();
        boolean wholeMessage = (part == null || part.trim().equals(""));

        boolean repliesWithInvites = true;
        
        Element m;
        if (wholeMessage) {
            m = encodeMessageCommon(parent, lc, appt, NOTIFY_FIELDS);
            m.addAttribute(MailService.A_ID, lc.formatItemId(appt, invId));
        } else {
            m = parent.addElement(MailService.E_MSG);
            m.addAttribute(MailService.A_ID, lc.formatItemId(appt, invId));
            m.addAttribute(MailService.A_PART, part);
        }
        
        try {
            MimeMessage mm = appt.getMimeMessage(invId);
            if (mm == null)
                throw MailServiceException.INVITE_OUT_OF_DATE("Invite id=" + lc.formatItemId(iid));
            if (!wholeMessage) {
                MimePart mp = Mime.getMimePart(mm, part);
                if (mp == null)
                    throw MailServiceException.NO_SUCH_PART(part);
                Object content = Mime.getMessageContent(mp);
                if (!(content instanceof MimeMessage))
                    throw MailServiceException.NO_SUCH_PART(part);
                mm = (MimeMessage) content;
            } else
                part = "";

            EmailElementCache eecache = new EmailElementCache();
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "From"), EmailElementCache.EMAIL_TYPE_FROM);
            addEmails(m, eecache, Mime.parseAddressHeader(mm, "Reply-To"), EmailElementCache.EMAIL_TYPE_REPLY_TO);
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
            encodeTimeZoneMap(invElt, appt.getTimeZoneMap());
            for (Invite inv : appt.getInvites(invId))
                encodeInvite(invElt, lc, appt, inv, NOTIFY_FIELDS, repliesWithInvites);

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
	 * @param lc TODO
	 * @param msg
	 * @param part TODO
	 * @return
	 * @throws ServiceException
	 */
	public static Element encodeMessageAsMIME(Element parent, ZimbraContext lc, Message msg, String part)
	throws ServiceException {
        boolean wholeMessage = (part == null || part.trim().equals(""));

        Element m;
        if (wholeMessage) {
            m = encodeMessageCommon(parent, lc, msg, NOTIFY_FIELDS);
            m.addAttribute(MailService.A_ID, lc.formatItemId(msg));
        } else {
            m = parent.addElement(MailService.E_MSG);
            m.addAttribute(MailService.A_ID, lc.formatItemId(msg));
            m.addAttribute(MailService.A_PART, part);
        }

        Element content = m.addUniqueElement(MailService.E_CONTENT);
        int size = (int) msg.getSize() + 2048;
        if (!wholeMessage)
            content.addAttribute(MailService.A_URL, CONTENT_SERVLET_URI + lc.formatItemId(msg) + PART_PARAM_STRING + part);
        else if (size > MAX_INLINE_MSG_SIZE)
            content.addAttribute(MailService.A_URL, CONTENT_SERVLET_URI + lc.formatItemId(msg));
        else
			try {
				byte[] raw = msg.getMessageContent();
    			if (!ByteUtil.isASCII(raw))
                    content.addAttribute(MailService.A_URL, CONTENT_SERVLET_URI + lc.formatItemId(msg));
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

    public static Element encodeMessageSummary(Element parent, ZimbraContext lc, Message msg, OutputParticipants output) {
        return encodeMessageSummary(parent, lc, msg, output, NOTIFY_FIELDS);
    }
    public static Element encodeMessageSummary(Element parent, ZimbraContext lc, Message msg, OutputParticipants output, int fields) {
        Element e = encodeMessageCommon(parent, lc, msg, fields);
        e.addAttribute(MailService.A_ID, lc.formatItemId(msg));

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
                encodeInvitesForMessage(e, lc, msg, fields);
            } catch (ServiceException ex) {
                mLog.debug("Caught exception while encoding Invites for msg " + msg.getId(), ex);
            }
        
        return e;
    }
    
    private static Element encodeMessageCommon(Element parent, ZimbraContext lc, MailItem mi, int fields) {
        Element elem = parent.addElement(MailService.E_MSG);
        // DO NOT encode the item-id here, as some Invite-Messages-In-Appointments have special item-id's
        if (needToOutput(fields, Change.MODIFIED_CONTENT) && mi.getSavedSequence() != 0)
            elem.addAttribute(MailService.A_REVISION, mi.getSavedSequence());
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            elem.addAttribute(MailService.A_SIZE, mi.getSize());
        if (needToOutput(fields, Change.MODIFIED_DATE))
            elem.addAttribute(MailService.A_DATE, mi.getDate());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            elem.addAttribute(MailService.A_FOLDER, lc.formatItemId(mi.getFolderId()));
        if (mi instanceof Message) {
            Message msg = (Message)mi;
            if (needToOutput(fields, Change.MODIFIED_PARENT) && (fields != NOTIFY_FIELDS || msg.getConversationId() != -1))
                elem.addAttribute(MailService.A_CONV_ID, lc.formatItemId(msg.getConversationId()));
        }
        recordItemTags(elem, mi, fields);
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            elem.addAttribute(MailService.A_CHANGE_DATE, mi.getChangeDate() / 1000);
            elem.addAttribute(MailService.A_MODIFIED_SEQUENCE, mi.getModifiedSequence());
        }
        return elem;
    }

    private static void encodeTimeZoneMap(Element parent, TimeZoneMap tzmap) {
        assert(tzmap != null);
        for (Iterator iter = tzmap.tzIterator(); iter.hasNext(); ) {
            ICalTimeZone tz = (ICalTimeZone) iter.next();
            Element e = parent.addElement(MailService.E_APPT_TZ);
            e.addAttribute(MailService.A_ID, tz.getID());
            e.addAttribute(MailService.A_APPT_TZ_STDOFFSET, tz.getStandardOffset() / 60 / 1000);

            if (tz.useDaylightTime()) {
                SimpleOnset standard = tz.getStandardOnset();
                SimpleOnset daylight = tz.getDaylightOnset();
                if (standard != null && daylight != null) {
                    e.addAttribute(MailService.A_APPT_TZ_DAYOFFSET, tz.getDaylightOffset() / 60 / 1000);

                    Element std = e.addElement(MailService.E_APPT_TZ_STANDARD);
                    if (standard.hasRule()) {
                        std.addAttribute(MailService.A_APPT_TZ_WEEK, standard.getWeek());
                        std.addAttribute(MailService.A_APPT_TZ_DAYOFWEEK, standard.getDayOfWeek());
                    } else
                        std.addAttribute(MailService.A_APPT_TZ_DAYOFMONTH, standard.getDayOfMonth());
                    std.addAttribute(MailService.A_APPT_TZ_MONTH, standard.getMonth());
                    std.addAttribute(MailService.A_APPT_TZ_HOUR, standard.getHour());
                    std.addAttribute(MailService.A_APPT_TZ_MINUTE, standard.getMinute());
                    std.addAttribute(MailService.A_APPT_TZ_SECOND, standard.getSecond());

                    Element day = e.addElement(MailService.E_APPT_TZ_DAYLIGHT);
                    if (daylight.hasRule()) {
                        day.addAttribute(MailService.A_APPT_TZ_WEEK, daylight.getWeek());
                        day.addAttribute(MailService.A_APPT_TZ_DAYOFWEEK, daylight.getDayOfWeek());
                    } else
                        day.addAttribute(MailService.A_APPT_TZ_DAYOFMONTH, daylight.getDayOfMonth());
                    day.addAttribute(MailService.A_APPT_TZ_MONTH, daylight.getMonth());
                    day.addAttribute(MailService.A_APPT_TZ_HOUR, daylight.getHour());
                    day.addAttribute(MailService.A_APPT_TZ_MINUTE, daylight.getMinute());
                    day.addAttribute(MailService.A_APPT_TZ_SECOND, daylight.getSecond());
                }
            }
        }
    }

    public static Element encodeInvite(Element parent, ZimbraContext lc, Appointment apptOrNull, Invite invite, int fields, boolean includeReplies) {
        boolean allFields = true;
        
        if (fields != NOTIFY_FIELDS) {
            allFields = false;
            if (!needToOutput(fields, Change.MODIFIED_INVITE)) {
                return parent;
            }
        }

        Element e = parent.addElement(MailService.E_INVITE_COMPONENT);

        e.addAttribute(MailService.A_APPT_COMPONENT_NUM, invite.getComponentNum());
        
        e.addAttribute(MailService.A_APPT_RSVP, invite.getRsvp());
        
        if (allFields) {
            try {
                e.addAttribute("x_uid", invite.getUid());
                
                Appointment appt = invite.getAppointment();
                if (appt != null)
                    e.addAttribute(MailService.A_APPT_ID, lc.formatItemId(appt));
                
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
            
            if (includeReplies && apptOrNull != null) {
                List /*Appointment.ReplyInfo */ fbas = apptOrNull.getReplyInfo(invite);

                Element repliesElt = e.addElement(MailService.A_APPT_REPLIES);
                for (Iterator iter = fbas.iterator(); iter.hasNext(); ) {
                    Appointment.ReplyInfo repInfo = (Appointment.ReplyInfo) iter.next();

                    Element curElt = repliesElt.addElement(MailService.E_APPT_REPLY);
                    if (repInfo.mRecurId != null) {
                        repInfo.mRecurId.toXml(curElt);
                    }

//                        PartStat ps = (PartStat)(repInfo.mAttendee.getParameters().getParameter(Parameter.PARTSTAT));
                    if (repInfo.mAttendee.hasPartStat()) {
                        String psStr = repInfo.mAttendee.getPartStat();
                        curElt.addAttribute(MailService.A_APPT_PARTSTAT, psStr);
                        String fbaStr = invite.partStatToFreeBusyActual(psStr);
                        curElt.addAttribute(MailService.A_APPT_FREEBUSY_ACTUAL, fbaStr);
                    }

                    curElt.addAttribute("stamp", repInfo.mDtStamp);
                    curElt.addAttribute("at", repInfo.mAttendee.getAddress());
                }
            }

            e.addAttribute(MailService.A_APPT_TRANSPARENCY, invite.getTransparency());
            
            if (invite.getRecurId() != null) {
                e.addAttribute(MailService.A_APPT_IS_EXCEPTION, true);
            }
            
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
                if (invite.isAllDayEvent()) {
                    // See CalendarUtils.parseInviteElementCommon, where we parse DTEND
                    // for a description of why we add -1d when sending to the client
                    dtEnd = dtEnd.add(ParsedDuration.NEGATIVE_ONE_DAY);
                }
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
            ZOrganizer org = invite.getOrganizer();
            if (org != null) {
                Element orgElt = e.addUniqueElement(MailService.E_APPT_ORGANIZER);
                
                if (org.hasCn()) {
                    orgElt.addAttribute(MailService.A_DISPLAY, org.getCn());
                }
                String str = org.getAddress();
                orgElt.addAttribute(MailService.A_URL, str);
            }
            
            // Attendee(s)
            Collection /*Invite.ZAttendee*/ ats = invite.getAttendees();
            for (Iterator atsIter = ats.iterator();atsIter.hasNext();) {
                Element atElt = e.addElement(MailService.E_APPT_ATTENDEE);
                
                ZAttendee at = (ZAttendee)atsIter.next();
                
                // display name 
                if (at.hasCn()) {
                    atElt.addAttribute(MailService.A_DISPLAY, at.getCn());
                }

                // calendar user type
                if (at.hasCUType()) {
                	atElt.addAttribute(MailService.A_APPT_CUTYPE, at.getCUType());
                }

                // role
                if (at.hasRole()) {
                    atElt.addAttribute(MailService.A_APPT_ROLE, at.getRole());
                }
                
                // participation status
                if (at.hasPartStat()) {
                    atElt.addAttribute(MailService.A_APPT_PARTSTAT, at.getPartStat());
                }
                
                // uri
                atElt.addAttribute(MailService.A_URL, at.getAddress());
            }
        }

        return e;
    }

    private static Element encodeInvitesForMessage(Element parent, ZimbraContext lc, Message msg, int fields)
    throws ServiceException {
        if (fields != NOTIFY_FIELDS)
            if (!needToOutput(fields, Change.MODIFIED_INVITE))
                return parent;
        
        Element ie = parent.addElement(MailService.E_INVITE);
        
        boolean addedMethod = false;
        Mailbox mbox = msg.getMailbox();
        
        for (Iterator iter = msg.getApptInfoIterator(); iter.hasNext(); ) {
            Message.ApptInfo info = (Message.ApptInfo) iter.next();
            
            Appointment appt = null;
            try {
                appt = mbox.getAppointmentById(lc.getOperationContext(), info.getAppointmentId());
            } catch (MailServiceException.NoSuchItemException e) {}
            if (appt != null) {
                Invite inv = appt.getInvite(msg.getId(), info.getComponentNo());
                
                if (inv != null) {
                    if (!addedMethod) {
//                        ie.addAttribute("method", inv.getMethod());
                        addedMethod = true;
                    }
                    encodeTimeZoneMap(ie, appt.getTimeZoneMap());
                    encodeInvite(ie, lc, null, inv, fields, true); // NULL b/c we don't want to encode all of the reply-status here!
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
        if (Mime.CT_XML_ZIMBRA_SHARE.equals(contentTypeString)) {
            Element shr = parent.getParent().addElement("shr");
            try {
                getContent(shr, mpi);
            } catch (IOException e) {
                if (mLog.isWarnEnabled())
                    mLog.warn("error writing body part: ", e);
            } catch (MessagingException e) {
            }
        }
        
        elem.addAttribute(MailService.A_CONTENT_TYPE, contentTypeString);
        
        // figure out attachment size
        try {
            int size = mp.getSize();
            if (size >= 0) {
                if ("base64".equalsIgnoreCase(mp.getEncoding())) size = (int) ((size * 0.75) - (size/76));
                elem.addAttribute(MailService.A_SIZE, size);
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
            String fname = Mime.getFilename(mp);
            if (fname == null && Mime.CT_MESSAGE_RFC822.equals(contentTypeString)) {
                // "filename" for attached messages is the Subject
                Object content = Mime.getMessageContent(mp);
                if (content instanceof MimeMessage)
                    fname = ((MimeMessage) content).getSubject();
            }
            if (fname != null && !fname.equals(""))
                elem.addAttribute(MailService.A_CONTENT_FILENAME, StringUtil.stripControlCharacters(fname));
        } catch (MessagingException me) {
        } catch (IOException ioe) {
        }
        
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
        ContentType ct = pi.getContentType();
        if (ct.match(Mime.CT_TEXT_WILD) || ct.match(Mime.CT_XML_WILD)) {
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
    
	public static Element encodeWiki(Element parent, ZimbraContext lc, WikiItem wiki, int rev) {
		return encodeWiki(parent, lc, wiki, NOTIFY_FIELDS, rev);
	}
	public static Element encodeWiki(Element parent, ZimbraContext lc, WikiItem wiki, int fields, int rev) {

        Element m = parent.addElement(MailService.E_WIKIWORD);
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            m.addAttribute(MailService.A_SIZE, wiki.getSize());
        if (needToOutput(fields, Change.MODIFIED_DATE))
            m.addAttribute(MailService.A_DATE, wiki.getDate());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            m.addAttribute(MailService.A_FOLDER, lc.formatItemId(wiki.getFolderId()));
        recordItemTags(m, wiki, fields);
        
        if (needToOutput(fields, Change.MODIFIED_CONTENT)) {
        	m.addAttribute(MailService.A_ID, lc.formatItemId(wiki));
        	m.addAttribute(MailService.A_NAME, wiki.getSubject());
        	
            try {
            	Document.DocumentRevision revision = (rev > 0) ? wiki.getRevision(rev) : wiki.getLastRevision(); 
            	m.addAttribute(MailService.A_VERSION, revision.getVersion());
            	m.addAttribute(MailService.A_CREATOR, revision.getCreator());
            	m.addAttribute(MailService.A_MODIFIED_DATE, revision.getRevDate());
            } catch (Exception ex) {
                mLog.warn("ignoring exception while fetching blob for Wiki " + wiki.getSubject(), ex);
            }
        }

        return m;
	}
}