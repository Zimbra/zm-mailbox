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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.html.HtmlDefang;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SearchParams.ExpandResults;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.mailbox.Contact.Attachment;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone.SimpleOnset;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mailbox.calendar.Alarm;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.mime.handler.TextEnrichedHandler;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.wiki.WikiPage;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpURL;

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

    public static void encodeItem(Element parent, ItemIdFormatter ifmt, OperationContext octxt, MailItem item, int fields)
    throws ServiceException {
        if (item instanceof Folder)
            encodeFolder(parent, ifmt, octxt, (Folder) item, fields);
        else if (item instanceof Tag)
            encodeTag(parent, ifmt, (Tag) item, fields);
        else if (item instanceof Note)
            encodeNote(parent, ifmt, (Note) item, fields);
        else if (item instanceof Contact)
            encodeContact(parent, ifmt, (Contact) item, false, null, fields);
        else if (item instanceof CalendarItem) 
            encodeCalendarItemSummary(parent, ifmt, (CalendarItem) item, fields, true);
        else if (item instanceof Conversation)
            encodeConversationSummary(parent, ifmt, octxt, (Conversation) item, fields);
        else if (item instanceof WikiItem)
            encodeWiki(parent, ifmt, (WikiItem) item, fields, -1);
        else if (item instanceof Document)
            encodeDocument(parent, ifmt, (Document) item, fields, -1);
        else if (item instanceof Message) {
            OutputParticipants output = (fields == NOTIFY_FIELDS ? OutputParticipants.PUT_BOTH : OutputParticipants.PUT_SENDERS);
            encodeMessageSummary(parent, ifmt, octxt, (Message) item, output, fields);
        }
    }

    private static boolean needToOutput(int fields, int fieldMask) {
        return ((fields & fieldMask) > 0);
    }

    public static Element encodeMailbox(Element parent, Mailbox mbox) {
        return encodeMailbox(parent, mbox, NOTIFY_FIELDS);
    }
    public static Element encodeMailbox(Element parent, Mailbox mbox, int fields) {
        Element elem = parent.addUniqueElement(MailConstants.E_MAILBOX);
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            elem.addAttribute(MailConstants.A_SIZE, mbox.getSize());
        return elem;
    }

    public static Element encodeFolder(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Folder folder) {
        return encodeFolder(parent, ifmt, octxt, folder, NOTIFY_FIELDS);
    }

    public static Element encodeFolder(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Folder folder, int fields) {
        if (folder instanceof SearchFolder)
            return encodeSearchFolder(parent, ifmt, (SearchFolder) folder, fields);
        else if (folder instanceof Mountpoint)
            return encodeMountpoint(parent, ifmt, (Mountpoint) folder, fields);

        Element elem = parent.addElement(MailConstants.E_FOLDER);
        encodeFolderCommon(elem, ifmt, folder, fields);
        if (needToOutput(fields, Change.MODIFIED_SIZE)) {
            elem.addAttribute(MailConstants.A_NUM, folder.getSize());
            elem.addAttribute(MailConstants.A_SIZE, folder.getTotalSize());
        }

        if (needToOutput(fields, Change.MODIFIED_URL)) {
            String url = folder.getUrl();
            if (!url.equals("") || fields != NOTIFY_FIELDS) {
                if (url.indexOf('@') != -1) {
                    try {
                        HttpURL httpurl = new HttpURL(url);
                        if (httpurl.getPassword() != null) {
                            httpurl.setPassword("");
                            url = httpurl.toString();
                        }
                    } catch (org.apache.commons.httpclient.URIException urie) { }
                }
                elem.addAttribute(MailConstants.A_URL, url);
            }
        }

        Mailbox mbox = folder.getMailbox();
        boolean remote = octxt != null && octxt.isDelegatedRequest(mbox);
        boolean canAdminister = !remote;
        if (remote) {
            // return only effective permissions for remote folders
            try {
                short perms = mbox.getEffectivePermissions(octxt, folder.getId(), MailItem.TYPE_FOLDER);
                elem.addAttribute(MailConstants.A_RIGHTS, ACL.rightsToString(perms));
                canAdminister = (perms & ACL.RIGHT_ADMIN) != 0;
            } catch (ServiceException e) {
                mLog.warn("ignoring exception while fetching effective permissions for folder " + folder.getId(), e);
            }
        }

        if (canAdminister) {
            // return full ACLs for folders we have admin rights on
            if (needToOutput(fields, Change.MODIFIED_ACL)) {
                ACL acl = folder.getEffectiveACL();
                if (acl != null || fields != NOTIFY_FIELDS) {
                    Element eACL = elem.addUniqueElement(MailConstants.E_ACL);
                    if (acl != null) {
                        for (ACL.Grant grant : acl.getGrants()) {
                            NamedEntry nentry = FolderAction.lookupGranteeByZimbraId(grant.getGranteeId(), grant.getGranteeType());
                            eACL.addElement(MailConstants.E_GRANT)
                                .addAttribute(MailConstants.A_ZIMBRA_ID, grant.getGranteeId())
                                .addAttribute(MailConstants.A_GRANT_TYPE, FolderAction.typeToString(grant.getGranteeType()))
                                .addAttribute(MailConstants.A_RIGHTS, ACL.rightsToString(grant.getGrantedRights()))
                                .addAttribute(MailConstants.A_DISPLAY, nentry == null ? null : nentry.getName());
                        }
                    }
                }
            }
        }
        return elem;
    }

    private static Element encodeFolderCommon(Element elem, ItemIdFormatter ifmt, Folder folder, int fields) {
        int folderId = folder.getId();
        elem.addAttribute(MailConstants.A_ID, ifmt.formatItemId(folder));

        if (folderId != Mailbox.ID_FOLDER_ROOT) {
            if (needToOutput(fields, Change.MODIFIED_NAME)) {
                String name = folder.getName();
                if (name != null && name.length() > 0)
                    elem.addAttribute(MailConstants.A_NAME, name);
            }
            if (needToOutput(fields, Change.MODIFIED_FOLDER))
                elem.addAttribute(MailConstants.A_FOLDER, ifmt.formatItemId(folder.getFolderId()));

            // rest URL if either the parent or name has changed
            if (needToOutput(fields, Change.MODIFIED_FOLDER | Change.MODIFIED_NAME) && !folder.isHidden()) {
                if (folder.getDefaultView() == MailItem.TYPE_WIKI ||
                        folder.getDefaultView() == MailItem.TYPE_APPOINTMENT ||
                        folder.getDefaultView() == MailItem.TYPE_TASK ||
                        folder.getDefaultView() == MailItem.TYPE_CONTACT)
                    encodeRestUrl(elem, folder);
            }
        }
        if (needToOutput(fields, Change.MODIFIED_FLAGS)) {
            String flags = folder.getFlagString();
            if (fields != NOTIFY_FIELDS || !flags.equals(""))
                elem.addAttribute(MailConstants.A_FLAGS, flags);
        }
        if (needToOutput(fields, Change.MODIFIED_COLOR)) {
            byte color = folder.getColor();
            if (color != MailItem.DEFAULT_COLOR || fields != NOTIFY_FIELDS)
                elem.addAttribute(MailConstants.A_COLOR, color);
        }
        if (needToOutput(fields, Change.MODIFIED_UNREAD)) {
            int unread = folder.getUnreadCount();
            if (unread > 0 || fields != NOTIFY_FIELDS)
                elem.addAttribute(MailConstants.A_UNREAD, unread);
        }
        if (needToOutput(fields, Change.MODIFIED_VIEW)) {
            byte view = folder.getDefaultView();
            if (view != MailItem.TYPE_UNKNOWN)
                elem.addAttribute(MailConstants.A_DEFAULT_VIEW, MailItem.getNameForType(view));
        }
        if (needToOutput(fields, Change.MODIFIED_CONTENT)) {
            elem.addAttribute(MailConstants.A_REVISION, folder.getSavedSequence());
        }
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            elem.addAttribute(MailConstants.A_CHANGE_DATE, folder.getChangeDate() / 1000);
            elem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, folder.getModifiedSequence());
        }
        return elem;
    }

    public static Element encodeSearchFolder(Element parent, ItemIdFormatter ifmt, SearchFolder search) {
        return encodeSearchFolder(parent, ifmt, search, NOTIFY_FIELDS);
    }

    public static Element encodeSearchFolder(Element parent, ItemIdFormatter ifmt, SearchFolder search, int fields) {
        Element elem = parent.addElement(MailConstants.E_SEARCH);
        encodeFolderCommon(elem, ifmt, search, fields);
        if (needToOutput(fields, Change.MODIFIED_QUERY)) {
            elem.addAttribute(MailConstants.A_QUERY, search.getQuery());
            elem.addAttribute(MailConstants.A_SORTBY, search.getSortField());
            elem.addAttribute(MailConstants.A_SEARCH_TYPES, search.getReturnTypes());
        }
        return elem;
    }

    public static Element encodeMountpoint(Element parent, ItemIdFormatter ifmt, Mountpoint mpt) {
        return encodeMountpoint(parent, ifmt, mpt, NOTIFY_FIELDS);
    }

    public static Element encodeMountpoint(Element parent, ItemIdFormatter ifmt, Mountpoint mpt, int fields) {
        Element elem = parent.addElement(MailConstants.E_MOUNT);
        encodeFolderCommon(elem, ifmt, mpt, fields);
        if (needToOutput(fields, Change.MODIFIED_CONTENT)) {
            elem.addAttribute(MailConstants.A_ZIMBRA_ID, mpt.getOwnerId());
            elem.addAttribute(MailConstants.A_REMOTE_ID, mpt.getRemoteId());
            NamedEntry nentry = FolderAction.lookupGranteeByZimbraId(mpt.getOwnerId(), ACL.GRANTEE_USER);
            elem.addAttribute(MailConstants.A_OWNER_NAME, nentry == null ? null : nentry.getName());
            if (mpt.getDefaultView() != MailItem.TYPE_UNKNOWN)
                elem.addAttribute(MailConstants.A_DEFAULT_VIEW, MailItem.getNameForType(mpt.getDefaultView()));
        }
        return elem;
    }

    public static Element encodeRestUrl(Element elt, MailItem item) {
        try {
        	String url = UserServlet.getRestUrl(item);
            return elt.addAttribute(MailConstants.A_REST_URL, url);
        } catch (ServiceException se) {
            mLog.error("cannot generate REST url", se);
            return elt;
        } catch (IOException ioe) {
            mLog.error("cannot generate REST url", ioe);
            return elt;
        }
    }

    public static void recordItemTags(Element elem, MailItem item, int fields) {
        if (needToOutput(fields, Change.MODIFIED_FLAGS | Change.MODIFIED_UNREAD)) {
            String flags = item.getFlagString();
            if (fields != NOTIFY_FIELDS || !flags.equals(""))
                elem.addAttribute(MailConstants.A_FLAGS, flags);
        }
        if (needToOutput(fields, Change.MODIFIED_TAGS)) {
            String tags = item.getTagString();
            if (!tags.equals("") || fields != NOTIFY_FIELDS)
                elem.addAttribute(MailConstants.A_TAGS, tags);
        }
    }

    public static Element encodeContact(Element parent, ItemIdFormatter ifmt, Contact contact, boolean summary, List<String> attrFilter) {
        return encodeContact(parent, ifmt, contact, summary, attrFilter, NOTIFY_FIELDS);
    }

    public static Element encodeContact(Element parent, ItemIdFormatter ifmt, Contact contact, boolean summary, List<String> attrFilter, int fields) {
        Element elem = parent.addElement(MailConstants.E_CONTACT);
        elem.addAttribute(MailConstants.A_ID, ifmt.formatItemId(contact));
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            elem.addAttribute(MailConstants.A_FOLDER, ifmt.formatItemId(contact.getFolderId()));
        recordItemTags(elem, contact, fields);
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            elem.addAttribute(MailConstants.A_CHANGE_DATE, contact.getChangeDate() / 1000);
            elem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, contact.getModifiedSequence());
            elem.addAttribute(MailConstants.A_DATE, contact.getDate());
            elem.addAttribute(MailConstants.A_REVISION, contact.getSavedSequence());
        } else if (needToOutput(fields, Change.MODIFIED_CONTENT)) {
            elem.addAttribute(MailConstants.A_DATE, contact.getDate());
            elem.addAttribute(MailConstants.A_REVISION, contact.getSavedSequence());
        }

        if (!needToOutput(fields, Change.MODIFIED_CONTENT)) {
            if (summary) {
                try {
                    elem.addAttribute(MailConstants.A_FILE_AS_STR, contact.getFileAsString());
                } catch (ServiceException e) { }
    
                elem.addAttribute(Contact.A_email, contact.get(Contact.A_email));
                elem.addAttribute(Contact.A_email2, contact.get(Contact.A_email2));
                elem.addAttribute(Contact.A_email3, contact.get(Contact.A_email3));

                String type = contact.get(Contact.A_type);
                String dlist = contact.get(Contact.A_dlist);
                if (type == null && dlist != null)
                    type = Contact.TYPE_GROUP;
                elem.addAttribute(Contact.A_type, type);
                if (dlist != null)
                    elem.addAttribute(Contact.A_dlist, dlist);
                
                // send back date with summary via search results
                elem.addAttribute(MailConstants.A_CHANGE_DATE, contact.getChangeDate() / 1000);
            }

            // stop here if we're not returning the actual contact content
            return elem;
        }

        try {
            elem.addAttribute(MailConstants.A_FILE_AS_STR, contact.getFileAsString());
        } catch (ServiceException e) { }

        Map<String, String> attrs = contact.getFields();
        List<Attachment> attachments = contact.getAttachments();
        if (attrFilter != null) {
            for (String name : attrFilter) {
                // XXX: How to distinguish between a non-existent attribute and
                //      an existing attribute with null or empty string value?
                String value = attrs.get(name);
                if (value != null && !value.equals("")) {
                    elem.addKeyValuePair(name, value);
                } else if (attachments != null) {
                    for (Attachment attach : attachments) {
                        if (attach.getName().equals(name))
                            encodeContactAttachment(elem, attach);
                    }
                }
            }
        } else {
            for (Map.Entry<String, String> me : attrs.entrySet()) {
                String name = me.getKey();
                String value = me.getValue();
                if (name != null && !name.trim().equals("") && value != null && !value.equals(""))
                    elem.addKeyValuePair(name, value);
            }
            if (attachments != null) {
                for (Attachment attach : attachments)
                    encodeContactAttachment(elem, attach);
            }
        }
        return elem;
    }

    private static void encodeContactAttachment(Element elem, Attachment attach) {
        Element.KeyValuePair kvp = elem.addKeyValuePair(attach.getName(), null);
        kvp.addAttribute(MailConstants.A_PART, attach.getPartName()).addAttribute(MailConstants.A_CONTENT_TYPE, attach.getContentType());
        kvp.addAttribute(MailConstants.A_SIZE, attach.getSize()).addAttribute(MailConstants.A_CONTENT_FILENAME, attach.getFilename());
    }

    public static Element encodeNote(Element parent, ItemIdFormatter ifmt, Note note) {
        return encodeNote(parent, ifmt, note, NOTIFY_FIELDS);
    }

    public static Element encodeNote(Element parent, ItemIdFormatter ifmt, Note note, int fields) {
        Element elem = parent.addElement(MailConstants.E_NOTE);
        elem.addAttribute(MailConstants.A_ID, ifmt.formatItemId(note));
        if (needToOutput(fields, Change.MODIFIED_CONTENT) && note.getSavedSequence() != 0)
            elem.addAttribute(MailConstants.A_REVISION, note.getSavedSequence());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            elem.addAttribute(MailConstants.A_FOLDER, ifmt.formatItemId(note.getFolderId()));
        if (needToOutput(fields, Change.MODIFIED_DATE))
            elem.addAttribute(MailConstants.A_DATE, note.getDate());
        recordItemTags(elem, note, fields);
        if (needToOutput(fields, Change.MODIFIED_POSITION))
            elem.addAttribute(MailConstants.A_BOUNDS, note.getBounds().toString());
        if (needToOutput(fields, Change.MODIFIED_COLOR))
            elem.addAttribute(MailConstants.A_COLOR, note.getColor());
        if (needToOutput(fields, Change.MODIFIED_CONTENT))
            elem.addAttribute(MailConstants.E_CONTENT, note.getText(), Element.Disposition.CONTENT);
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            elem.addAttribute(MailConstants.A_CHANGE_DATE, note.getChangeDate() / 1000);
            elem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, note.getModifiedSequence());
        }
        return elem;
    }

    public static Element encodeTag(Element parent, ItemIdFormatter ifmt, Tag tag) {
        return encodeTag(parent, ifmt, tag, NOTIFY_FIELDS);
    }

    public static Element encodeTag(Element parent, ItemIdFormatter ifmt, Tag tag, int fields) {
        Element elem = parent.addElement(MailConstants.E_TAG);
        elem.addAttribute(MailConstants.A_ID, ifmt.formatItemId(tag));
        if (needToOutput(fields, Change.MODIFIED_NAME))
            elem.addAttribute(MailConstants.A_NAME, tag.getName());
        if (needToOutput(fields, Change.MODIFIED_COLOR))
            elem.addAttribute(MailConstants.A_COLOR, tag.getColor());
        if (needToOutput(fields, Change.MODIFIED_UNREAD)) {
            int unreadCount = tag.getUnreadCount();
            if (unreadCount > 0 || fields != NOTIFY_FIELDS)
                elem.addAttribute(MailConstants.A_UNREAD, unreadCount);
        }
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            elem.addAttribute(MailConstants.A_DATE, tag.getDate());
            elem.addAttribute(MailConstants.A_REVISION, tag.getSavedSequence());
            elem.addAttribute(MailConstants.A_CHANGE_DATE, tag.getChangeDate() / 1000);
            elem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, tag.getModifiedSequence());
        }
        return elem;
    }


    public static Element encodeConversation(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Conversation conv, SearchParams params) throws ServiceException {
        Mailbox mbox = conv.getMailbox();
        List<Message> msgs = mbox.getMessagesByConversation(octxt, conv.getId(), Conversation.SORT_DATE_ASCENDING);
        return encodeConversation(parent, ifmt, octxt, conv, msgs, params);
    }

    public static Element encodeConversation(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Conversation conv, List<Message> msgs, SearchParams params) throws ServiceException {
        int fields = NOTIFY_FIELDS;
        Element c = encodeConversationCommon(parent, ifmt, conv, msgs, fields);
        if (msgs.isEmpty())
            return c;

        c.addAttribute(MailConstants.E_SUBJECT, msgs.get(0).getSubject(), Element.Disposition.CONTENT);

        Mailbox mbox = conv.getMailbox();
        ExpandResults expand = params.getFetchFirst();
        for (Message msg : msgs) {
            if (msg.isTagged(mbox.mDeletedFlag))
                continue;
            if (expand == ExpandResults.FIRST || expand == ExpandResults.ALL) {
                encodeMessageAsMP(c, ifmt, octxt, msg, null, params.getWantHtml(), params.getNeuterImages(), params.getInlinedHeaders(), true);
                if (expand == ExpandResults.FIRST)
                    expand = ExpandResults.NONE;
            } else {
                Element m = c.addElement(MailConstants.E_MSG);
                m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(msg));
                m.addAttribute(MailConstants.A_DATE, msg.getDate());
                m.addAttribute(MailConstants.A_SIZE, msg.getSize());
                m.addAttribute(MailConstants.A_FOLDER, ifmt.formatItemId(msg.getFolderId()));
                recordItemTags(m, msg, fields);
                m.addAttribute(MailConstants.E_FRAG, msg.getFragment(), Element.Disposition.CONTENT);
                encodeEmail(m, msg.getSender(), EmailType.FROM);
            }
        }
        return c;
    }

    /**
     * This version lets you specify the Date and Fragment -- we use this when sending Query Results back to the client, 
     * the conversation date returned and fragment correspond to those of the matched message.
     * @param ifmt TODO
     * @param conv
     * @param msgHit TODO
     * @param eecache
     * @throws ServiceException 
     */
    public static Element encodeConversationSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Conversation conv, int fields) throws ServiceException {
        return encodeConversationSummary(parent, ifmt, octxt, conv, null, OutputParticipants.PUT_SENDERS, fields);
    }

    public static Element encodeConversationSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Conversation conv, Message msgHit, OutputParticipants output) throws ServiceException {
        return encodeConversationSummary(parent, ifmt, octxt, conv, msgHit, output, NOTIFY_FIELDS);
    }

    private static Element encodeConversationSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Conversation conv, Message msgHit, OutputParticipants output, int fields) throws ServiceException {
        boolean addRecips  = msgHit != null && msgHit.isFromMe() && (output == OutputParticipants.PUT_RECIPIENTS || output == OutputParticipants.PUT_BOTH);
        boolean addSenders = (output == OutputParticipants.PUT_BOTH || !addRecips) && needToOutput(fields, Change.MODIFIED_SENDERS);

        Mailbox mbox = conv.getMailbox();
        List<Message> msgs = null;
        if ((octxt != null && octxt.isDelegatedRequest(mbox)) || (addSenders && conv.isTagged(mbox.mDeletedFlag)))
            msgs = mbox.getMessagesByConversation(octxt, conv.getId(), Conversation.SORT_DATE_ASCENDING);

        Element c = encodeConversationCommon(parent, ifmt, conv, msgs, fields);
        if (msgs != null && msgs.isEmpty())
            return c;

        if (needToOutput(fields, Change.MODIFIED_DATE))
            c.addAttribute(MailConstants.A_DATE, msgHit != null ? msgHit.getDate() : conv.getDate());
        if (needToOutput(fields, Change.MODIFIED_SUBJECT))
            c.addAttribute(MailConstants.E_SUBJECT, conv.getSubject(), Element.Disposition.CONTENT);
        if (fields == NOTIFY_FIELDS && msgHit != null)
            c.addAttribute(MailConstants.E_FRAG, msgHit.getFragment(), Element.Disposition.CONTENT);

        if (addRecips) {
            try {
                InternetAddress[] addrs = InternetAddress.parseHeader(msgHit.getRecipients(), false);
                addEmails(c, addrs, EmailType.TO);
            } catch (AddressException e1) { }
        }

        if (addSenders) {
            SenderList sl;
            try {
                if (msgs != null) {
                    sl = new SenderList();
                    for (Message msg : msgs) {
                        if (!msg.isTagged(mbox.mDeletedFlag))
                            sl.add(msg);
                    }
                } else {
                    sl = mbox.getConversationSenderList(conv.getId());
                }
            } catch (SenderList.RefreshException slre) {
                ZimbraLog.soap.warn("out-of-order messages returned for conversation " + conv.getId());
                return c;
            } catch (ServiceException e) {
                return c;
            }

            if (sl.getFirstAddress() != null)
                encodeEmail(c, sl.getFirstAddress(), EmailType.FROM);
            if (sl.isElided())
                c.addAttribute(MailConstants.A_ELIDED, true);
            for (ParsedAddress pa : sl.getLastAddresses())
                encodeEmail(c, pa, EmailType.FROM);
        }

        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            c.addAttribute(MailConstants.A_CHANGE_DATE, conv.getChangeDate() / 1000);
            c.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, conv.getModifiedSequence());
        }
        return c;
    }

    private static Element encodeConversationCommon(Element parent, ItemIdFormatter ifmt, Conversation conv, List<Message> msgs, int fields) {
        Element c = parent.addElement(MailConstants.E_CONV);
        c.addAttribute(MailConstants.A_ID, ifmt.formatItemId(conv));

        Mailbox mbox = conv.getMailbox();
        if (needToOutput(fields, Change.MODIFIED_CHILDREN | Change.MODIFIED_SIZE)) {
            int count = 0, nondeleted = 0;
            if (msgs == null) {
                count = conv.getMessageCount();
                nondeleted = conv.getNondeletedCount();
            } else {
                count = nondeleted = msgs.size();
                for (Message msg : msgs)
                    if (msg.isTagged(mbox.mDeletedFlag))
                        nondeleted--;
            }

            c.addAttribute(MailConstants.A_NUM, nondeleted);
            if (count != nondeleted)
                c.addAttribute(MailConstants.A_TOTAL_SIZE, count);
        }

        if (msgs == null) {
            recordItemTags(c, conv, fields);
        } else if (needToOutput(fields, Change.MODIFIED_FLAGS | Change.MODIFIED_UNREAD | Change.MODIFIED_TAGS)) {
            int flags = 0;  long tags = 0;
            for (Message msg : msgs) {
                if (!msg.isTagged(mbox.mDeletedFlag)) {
                    flags |= msg.getFlagBitmask();  tags |= msg.getTagBitmask();
                }
            }
            if (needToOutput(fields, Change.MODIFIED_FLAGS | Change.MODIFIED_UNREAD)) {
                if (fields != NOTIFY_FIELDS || flags != 0)
                    c.addAttribute(MailConstants.A_FLAGS, Flag.bitmaskToFlags(flags));
            }
            if (needToOutput(fields, Change.MODIFIED_TAGS)) {
                if (fields != NOTIFY_FIELDS || tags != 0)
                    c.addAttribute(MailConstants.A_TAGS, Tag.bitmaskToTags(tags));
            }
        }

        return c;
    }

    /** Encodes a Message object into <m> element with <mp> elements for
     *  message body.
     * @param parent  The Element to add the new <tt>&lt;m></tt> to.
     * @param ifmt    The formatter to sue when serializing item ids.
     * @param msg     The Message to serialize.
     * @param part    If non-null, we'll serialuize this message/rfc822 subpart
     *                of the specified Message instead of the Message itself.
     * @param wantHTML  <tt>true</tt> to prefer HTML parts as the "body",
     *                  <tt>false</tt> to prefer text/plain parts.
     * @param neuter  Whether to rename "src" attributes on HTML <img> tags.
     * @param headers Extra message headers to include in the returned element.
     * @param serializeType If <tt>false</tt>, always serializes as an
     *                      <tt>&lt;m></tt> element.
     * @return The newly-created <tt>&lt;m></tt> Element, which has already
     *         been added as a child to the passed-in <tt>parent</tt>.
     * @throws ServiceException */
    public static Element encodeMessageAsMP(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Message msg, String part,
                                            boolean wantHTML, boolean neuter, Set<String> headers, boolean serializeType)
    throws ServiceException {
        boolean wholeMessage = (part == null || part.trim().equals(""));

        Element m;
        if (wholeMessage) {
            m = encodeMessageCommon(parent, ifmt, msg, NOTIFY_FIELDS, serializeType);
            m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(msg));
        } else {
            m = parent.addElement(MailConstants.E_MSG);
            m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(msg));
            m.addAttribute(MailConstants.A_PART, part);
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
            } else {
                part = "";
            }

            addEmails(m, Mime.parseAddressHeader(mm, "From"), EmailType.FROM);
            addEmails(m, Mime.parseAddressHeader(mm, "Sender"), EmailType.SENDER);
            addEmails(m, Mime.parseAddressHeader(mm, "Reply-To"), EmailType.REPLY_TO);
            addEmails(m, Mime.parseAddressHeader(mm, "To"), EmailType.TO);
            addEmails(m, Mime.parseAddressHeader(mm, "Cc"), EmailType.CC);
            addEmails(m, Mime.parseAddressHeader(mm, "Bcc"), EmailType.BCC);

            String subject = mm.getSubject();
            if (subject != null)
                m.addAttribute(MailConstants.E_SUBJECT, StringUtil.stripControlCharacters(subject), Element.Disposition.CONTENT);

            String fragment = msg.getFragment();
            if (fragment != null && !fragment.equals(""))
                m.addAttribute(MailConstants.E_FRAG, fragment, Element.Disposition.CONTENT);

            String messageID = mm.getMessageID();
            if (messageID != null && !messageID.trim().equals(""))
                m.addAttribute(MailConstants.E_MSG_ID_HDR, StringUtil.stripControlCharacters(messageID), Element.Disposition.CONTENT);

            if (wholeMessage && msg.isDraft()) {
                if (msg.getDraftOrigId() > 0)
                    m.addAttribute(MailConstants.A_ORIG_ID, ifmt.formatItemId(msg.getDraftOrigId()));
                if (!msg.getDraftReplyType().equals(""))
                    m.addAttribute(MailConstants.A_REPLY_TYPE, msg.getDraftReplyType());
                if (!msg.getDraftIdentityId().equals(""))
                    m.addAttribute(MailConstants.A_IDENTITY_ID, msg.getDraftIdentityId());
                String inReplyTo = mm.getHeader("In-Reply-To", null);
                if (inReplyTo != null && !inReplyTo.equals(""))
                    m.addAttribute(MailConstants.E_IN_REPLY_TO, StringUtil.stripControlCharacters(inReplyTo), Element.Disposition.CONTENT);
            }

            if (!wholeMessage)
                m.addAttribute(MailConstants.A_SIZE, mm.getSize());

            java.util.Date sent = mm.getSentDate();
            if (sent != null)
                m.addAttribute(MailConstants.A_SENT_DATE, sent.getTime());

            if (msg.isInvite() && msg.hasCalendarItemInfos())
                encodeInvitesForMessage(m, ifmt, octxt, msg, NOTIFY_FIELDS);

            if (headers != null) {
                for (String name : headers) {
                    String[] values = mm.getHeader(name);
                    if (values == null)
                        continue;
                    for (int i = 0; i < values.length; i++)
                        m.addKeyValuePair(name, values[i], MailConstants.A_HEADER, MailConstants.A_ATTRIBUTE_NAME);
                }
            }

            List<MPartInfo> parts = Mime.getParts(mm);
            if (parts != null && !parts.isEmpty()) {
                Set<MPartInfo> bodies = Mime.getBody(parts, wantHTML);
                addParts(m, parts.get(0), bodies, part, neuter);
            }
        } catch (IOException ex) {
            throw ServiceException.FAILURE(ex.getMessage(), ex);
        } catch (MessagingException ex) {
            throw ServiceException.FAILURE(ex.getMessage(), ex);
        }
        return m;
    }
    
    /**
     * Encodes the basic search / sync fields onto an existing calendar item element 
     * 
     * @param calItemElem
     * @param ifmt
     * @param calItem
     * @param fields
     * @throws ServiceException
     */
    public static void setCalendarItemFields(Element calItemElem, ItemIdFormatter ifmt, CalendarItem calItem, int fields, boolean encodeInvites)
    throws ServiceException {
        recordItemTags(calItemElem, calItem, fields);

        calItemElem.addAttribute(MailConstants.A_UID, calItem.getUid());
        calItemElem.addAttribute(MailConstants.A_ID, ifmt.formatItemId(calItem));
        calItemElem.addAttribute(MailConstants.A_FOLDER, ifmt.formatItemId(calItem.getFolderId()));

        if (needToOutput(fields, Change.MODIFIED_CONTENT) && calItem.getSavedSequence() != 0)
            calItemElem.addAttribute(MailConstants.A_REVISION, calItem.getSavedSequence());
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            calItemElem.addAttribute(MailConstants.A_SIZE, calItem.getSize());
        if (needToOutput(fields, Change.MODIFIED_DATE))
            calItemElem.addAttribute(MailConstants.A_DATE, calItem.getDate());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            calItemElem.addAttribute(MailConstants.A_FOLDER, ifmt.formatItemId(calItem.getFolderId()));
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            calItemElem.addAttribute(MailConstants.A_CHANGE_DATE, calItem.getChangeDate() / 1000);
            calItemElem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, calItem.getModifiedSequence());
        }

        if (needToOutput(fields, Change.MODIFIED_CONTENT) && encodeInvites) {
            for (int i = 0; i < calItem.numInvites(); i++)
                encodeInvite(calItemElem, ifmt, calItem, calItem.getInvite(i));
        }
    }

    public static Element encodeInvite(Element parent, ItemIdFormatter ifmt, CalendarItem cal, Invite inv) throws ServiceException {
        Element ie = parent.addElement(MailConstants.E_INVITE);
        setCalendarItemType(ie, cal);
        encodeTimeZoneMap(ie, cal.getTimeZoneMap());

        ie.addAttribute(MailConstants.A_CAL_SEQUENCE, inv.getSeqNo());
        encodeReplies(ie, cal, inv);

        ie.addAttribute(MailConstants.A_ID, ifmt.formatItemId(inv.getMailItemId()));
        ie.addAttribute(MailConstants.A_CAL_COMPONENT_NUM, inv.getComponentNum());
        if (inv.hasRecurId())
            ie.addAttribute(MailConstants.A_CAL_RECURRENCE_ID, inv.getRecurId().toString());

        encodeInviteComponent(ie, ifmt, cal, inv, NOTIFY_FIELDS, false);

        return ie;
    }

    /**
     * Encode the metadata for a calendar item.
     * 
     * The content for the calendar item is a big multipart/digest containing each
     * invite in the calendar item as a sub-mimepart -- it can be retreived from the content 
     * servlet: 
     *    http://servername/service/content/get?id=<calItemId>
     * 
     * The client can ALSO request just the content for each individual invite using a
     * compound item-id request:
     *    http://servername/service/content/get?id=<calItemId>-<invite-mail-item-id>
     *    
     * DO NOT use the raw invite-mail-item-id to fetch the content: since the invite is a 
     * standard mail-message it can be deleted by the user at any time!
     * @param parent
     * @param ifmt TODO
     * @param calItem
     * @param fields
     * 
     * @return
     */
    public static Element encodeCalendarItemSummary(Element parent, ItemIdFormatter ifmt, CalendarItem calItem, int fields, boolean includeInvites)
    throws ServiceException {
        Element calItemElem;
        if (calItem instanceof Appointment)
            calItemElem = parent.addElement(MailConstants.E_APPOINTMENT);
        else
            calItemElem = parent.addElement(MailConstants.E_TASK);
        
        setCalendarItemFields(calItemElem, ifmt, calItem, fields, includeInvites);
        
        return calItemElem;
    }

    private static void encodeReplies(Element parent, CalendarItem calItem, Invite inv) {
        Element repliesElt = parent.addElement(MailConstants.E_CAL_REPLIES);

        List /*CalendarItem.ReplyInfo */ replies = calItem.getReplyInfo(inv);
        for (Iterator iter = replies.iterator(); iter.hasNext(); ) {
            CalendarItem.ReplyInfo repInfo = (CalendarItem.ReplyInfo) iter.next();
            ZAttendee attendee = repInfo.mAttendee;

            Element curElt = repliesElt.addElement(MailConstants.E_CAL_REPLY);
            if (repInfo.mRecurId != null) {
                repInfo.mRecurId.toXml(curElt);
            }
            if (attendee.hasPartStat()) {
                curElt.addAttribute(MailConstants.A_CAL_PARTSTAT, attendee.getPartStat());
            }
            curElt.addAttribute(MailConstants.A_DATE, repInfo.mDtStamp);
            curElt.addAttribute(MailConstants.A_CAL_ATTENDEE, attendee.getAddress());
            if (attendee.hasSentBy())
                curElt.addAttribute(MailConstants.A_CAL_SENTBY, attendee.getSentBy());
        }
    }


    /** Encodes an Invite stored within a calendar item object into <m> element
     *  with <mp> elements.
     * @param parent  The Element to add the new <tt>&lt;m></tt> to.
     * @param ifmt    The SOAP request's context.
     * @param calItem The calendar item to serialize.
     * @param iid     The requested item; the contained subpart will be used to
     *                pick the Invite out of the calendar item's blob & metadata.
     * @param part    If non-null, we'll serialuize this message/rfc822 subpart
     *                of the specified Message instead of the Message itself.
     * @param wantHTML  <tt>true</tt> to prefer HTML parts as the "body",
     *                  <tt>false</tt> to prefer text/plain parts.
     * @param neuter  Whether to rename "src" attributes on HTML <img> tags.
     * @param headers Extra message headers to include in the returned element.
     * @param serializeType If <tt>false</tt>, always serializes as an
     *                      <tt>&lt;m></tt> element.
     * @return The newly-created <tt>&lt;m></tt> Element, which has already
     *         been added as a child to the passed-in <tt>parent</tt>.
     * @throws ServiceException */
    public static Element encodeInviteAsMP(Element parent, ItemIdFormatter ifmt, CalendarItem calItem, ItemId iid, String part,
                                           boolean wantHTML, boolean neuter, Set<String> headers, boolean serializeType)
    throws ServiceException {
        int invId = iid.getSubpartId();
        boolean wholeMessage = (part == null || part.trim().equals(""));

        boolean repliesWithInvites = true;

        Element m;
        if (wholeMessage) {
            m = encodeMessageCommon(parent, ifmt, calItem, NOTIFY_FIELDS, serializeType);
            m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(calItem, invId));
        } else {
            m = parent.addElement(MailConstants.E_MSG);
            m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(calItem, invId));
            m.addAttribute(MailConstants.A_PART, part);
        }

        try {
            MimeMessage mm = calItem.getSubpartMessage(invId);
            if (mm == null)
                throw MailServiceException.INVITE_OUT_OF_DATE("Invite id=" + ifmt.formatItemId(iid));
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

            addEmails(m, Mime.parseAddressHeader(mm, "From"), EmailType.FROM);
            addEmails(m, Mime.parseAddressHeader(mm, "Sender"), EmailType.SENDER);
            addEmails(m, Mime.parseAddressHeader(mm, "Reply-To"), EmailType.REPLY_TO);
            addEmails(m, Mime.parseAddressHeader(mm, "To"), EmailType.TO);
            addEmails(m, Mime.parseAddressHeader(mm, "Cc"), EmailType.CC);
            addEmails(m, Mime.parseAddressHeader(mm, "Bcc"), EmailType.BCC);

            String subject = mm.getSubject();
            if (subject != null)
                m.addAttribute(MailConstants.E_SUBJECT, StringUtil.stripControlCharacters(subject), Element.Disposition.CONTENT);
            String messageID = mm.getMessageID();
            if (messageID != null && !messageID.trim().equals(""))
                m.addAttribute(MailConstants.E_MSG_ID_HDR, StringUtil.stripControlCharacters(messageID), Element.Disposition.CONTENT);

//          if (wholeMessage && msg.isDraft()) {
//              if (msg.getDraftOrigId() > 0)
//                  m.addAttribute(MailService.A_ORIG_ID, msg.getDraftOrigId());
//              if (!msg.getDraftReplyType().equals(""))
//                  m.addAttribute(MailService.A_REPLY_TYPE, msg.getDraftReplyType());
//              String inReplyTo = mm.getHeader("In-Reply-To", null);
//              if (inReplyTo != null && !inReplyTo.equals(""))
//                  m.addAttribute(MailService.E_IN_REPLY_TO, StringUtil.stripControlCharacters(inReplyTo), Element.Disposition.CONTENT);
//          }

            if (!wholeMessage)
                m.addAttribute(MailConstants.A_SIZE, mm.getSize());

            java.util.Date sent = mm.getSentDate();
            if (sent != null)
                m.addAttribute(MailConstants.A_SENT_DATE, sent.getTime());

            Element invElt = m.addElement(MailConstants.E_INVITE);
            setCalendarItemType(invElt, calItem);
            encodeTimeZoneMap(invElt, calItem.getTimeZoneMap());
            for (Invite inv : calItem.getInvites(invId))
                encodeInviteComponent(invElt, ifmt, calItem, inv, NOTIFY_FIELDS, repliesWithInvites);

            if (headers != null) {
                for (String name : headers) {
                    String[] values = mm.getHeader(name);
                    if (values == null)
                        continue;
                    for (int i = 0; i < values.length; i++)
                        m.addKeyValuePair(name, values[i], MailConstants.A_HEADER, MailConstants.A_ATTRIBUTE_NAME);
                }
            }

            List<MPartInfo> parts = Mime.getParts(mm);
            if (parts != null && !parts.isEmpty()) {
                Set<MPartInfo> bodies = Mime.getBody(parts, wantHTML);
                addParts(m, parts.get(0), bodies, part, neuter);
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
     * @param ifmt TODO
     * @param msg
     * @param part TODO
     * @return
     * @throws ServiceException
     */
    public static Element encodeMessageAsMIME(Element parent, ItemIdFormatter ifmt, Message msg, String part, boolean serializeType)
    throws ServiceException {
        boolean wholeMessage = (part == null || part.trim().equals(""));

        Element m;
        if (wholeMessage) {
            m = encodeMessageCommon(parent, ifmt, msg, NOTIFY_FIELDS, serializeType);
            m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(msg));
        } else {
            m = parent.addElement(MailConstants.E_MSG);
            m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(msg));
            m.addAttribute(MailConstants.A_PART, part);
        }

        Element content = m.addUniqueElement(MailConstants.E_CONTENT);
        int size = msg.getSize() + 2048;
        if (!wholeMessage) {
            content.addAttribute(MailConstants.A_URL, CONTENT_SERVLET_URI + ifmt.formatItemId(msg) + PART_PARAM_STRING + part);
        } else if (size > MAX_INLINE_MSG_SIZE) {
            content.addAttribute(MailConstants.A_URL, CONTENT_SERVLET_URI + ifmt.formatItemId(msg));
        } else {
            try {
                byte[] raw = msg.getContent();
                if (!ByteUtil.isASCII(raw))
                    content.addAttribute(MailConstants.A_URL, CONTENT_SERVLET_URI + ifmt.formatItemId(msg));
                else
                    content.setText(new String(raw, "US-ASCII"));
            } catch (IOException ex) {
                throw ServiceException.FAILURE(ex.getMessage(), ex);
            }
        }

        return m;
    }

    public static enum OutputParticipants { PUT_SENDERS, PUT_RECIPIENTS, PUT_BOTH }

    public static Element encodeMessageSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Message msg, OutputParticipants output) {
        return encodeMessageSummary(parent, ifmt, octxt, msg, output, NOTIFY_FIELDS);
    }

    public static Element encodeMessageSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Message msg, OutputParticipants output, int fields) {
        Element e = encodeMessageCommon(parent, ifmt, msg, fields, true);
        e.addAttribute(MailConstants.A_ID, ifmt.formatItemId(msg));

        if (!needToOutput(fields, Change.MODIFIED_CONTENT))
            return e;

        boolean addRecips  = msg.isFromMe() && (output == OutputParticipants.PUT_RECIPIENTS || output == OutputParticipants.PUT_BOTH);
        boolean addSenders = output == OutputParticipants.PUT_BOTH || !addRecips;
        if (addRecips) {
            try {
                addEmails(e, InternetAddress.parseHeader(msg.getRecipients(), false), EmailType.TO);
            } catch (AddressException e1) { }
        }

        if (addSenders)
            encodeEmail(e, msg.getSender(), EmailType.FROM);

        e.addAttribute(MailConstants.E_SUBJECT, StringUtil.stripControlCharacters(msg.getSubject()), Element.Disposition.CONTENT);

        // fragment has already been sanitized...
        String fragment = msg.getFragment();
        if (!fragment.equals(""))
            e.addAttribute(MailConstants.E_FRAG, fragment, Element.Disposition.CONTENT);

        if (msg.isInvite() && msg.hasCalendarItemInfos()) {
            try {
                encodeInvitesForMessage(e, ifmt, octxt, msg, fields);
            } catch (ServiceException ex) {
                mLog.debug("Caught exception while encoding Invites for msg " + msg.getId(), ex);
            }
        }

        return e;
    }

    private static Element encodeMessageCommon(Element parent, ItemIdFormatter ifmt, MailItem item, int fields, boolean serializeType) {
        String name = serializeType && item instanceof Chat ? MailConstants.E_CHAT : MailConstants.E_MSG;
        Element elem = parent.addElement(name);
        // DO NOT encode the item-id here, as some Invite-Messages-In-CalendarItems have special item-id's
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            elem.addAttribute(MailConstants.A_SIZE, item.getSize());
        if (needToOutput(fields, Change.MODIFIED_DATE))
            elem.addAttribute(MailConstants.A_DATE, item.getDate());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            elem.addAttribute(MailConstants.A_FOLDER, ifmt.formatItemId(item.getFolderId()));
        if (item instanceof Message) {
            Message msg = (Message) item;
            if (needToOutput(fields, Change.MODIFIED_PARENT) && (fields != NOTIFY_FIELDS || msg.getConversationId() != -1))
                elem.addAttribute(MailConstants.A_CONV_ID, ifmt.formatItemId(msg.getConversationId()));
        }
        recordItemTags(elem, item, fields);

        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            elem.addAttribute(MailConstants.A_REVISION, item.getSavedSequence());
            elem.addAttribute(MailConstants.A_CHANGE_DATE, item.getChangeDate() / 1000);
            elem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, item.getModifiedSequence());
        } else if (needToOutput(fields, Change.MODIFIED_CONTENT) && item.getSavedSequence() != 0) {
            elem.addAttribute(MailConstants.A_REVISION, item.getSavedSequence());
        }

        return elem;
    }

    private static void encodeTimeZoneMap(Element parent, TimeZoneMap tzmap) {
        assert(tzmap != null);
        for (Iterator iter = tzmap.tzIterator(); iter.hasNext(); ) {
            ICalTimeZone tz = (ICalTimeZone) iter.next();
            Element e = parent.addElement(MailConstants.E_CAL_TZ);
            e.addAttribute(MailConstants.A_ID, tz.getID());
            e.addAttribute(MailConstants.A_CAL_TZ_STDOFFSET, tz.getStandardOffset() / 60 / 1000);

            if (tz.useDaylightTime()) {
                SimpleOnset standard = tz.getStandardOnset();
                SimpleOnset daylight = tz.getDaylightOnset();
                if (standard != null && daylight != null) {
                    e.addAttribute(MailConstants.A_CAL_TZ_DAYOFFSET, tz.getDaylightOffset() / 60 / 1000);

                    Element std = e.addElement(MailConstants.E_CAL_TZ_STANDARD);
                    int standardWeek = standard.getWeek();
                    if (standardWeek != 0) {
                        std.addAttribute(MailConstants.A_CAL_TZ_WEEK, standardWeek);
                        std.addAttribute(MailConstants.A_CAL_TZ_DAYOFWEEK, standard.getDayOfWeek());
                    } else
                        std.addAttribute(MailConstants.A_CAL_TZ_DAYOFMONTH, standard.getDayOfMonth());
                    std.addAttribute(MailConstants.A_CAL_TZ_MONTH, standard.getMonth());
                    std.addAttribute(MailConstants.A_CAL_TZ_HOUR, standard.getHour());
                    std.addAttribute(MailConstants.A_CAL_TZ_MINUTE, standard.getMinute());
                    std.addAttribute(MailConstants.A_CAL_TZ_SECOND, standard.getSecond());

                    Element day = e.addElement(MailConstants.E_CAL_TZ_DAYLIGHT);
                    int daylightWeek = daylight.getWeek();
                    if (daylightWeek != 0) {
                        day.addAttribute(MailConstants.A_CAL_TZ_WEEK, daylightWeek);
                        day.addAttribute(MailConstants.A_CAL_TZ_DAYOFWEEK, daylight.getDayOfWeek());
                    } else
                        day.addAttribute(MailConstants.A_CAL_TZ_DAYOFMONTH, daylight.getDayOfMonth());
                    day.addAttribute(MailConstants.A_CAL_TZ_MONTH, daylight.getMonth());
                    day.addAttribute(MailConstants.A_CAL_TZ_HOUR, daylight.getHour());
                    day.addAttribute(MailConstants.A_CAL_TZ_MINUTE, daylight.getMinute());
                    day.addAttribute(MailConstants.A_CAL_TZ_SECOND, daylight.getSecond());
                }
            }
        }
    }

    public static Element encodeInviteComponent(Element parent, ItemIdFormatter ifmt, CalendarItem calItem, Invite invite, int fields, boolean includeReplies)
    throws ServiceException {
        boolean allFields = true;

        if (fields != NOTIFY_FIELDS) {
            allFields = false;
            if (!needToOutput(fields, Change.MODIFIED_INVITE)) {
                return parent;
            }
        }

        Element e = parent.addElement(MailConstants.E_INVITE_COMPONENT);

        e.addAttribute(MailConstants.A_CAL_COMPONENT_NUM, invite.getComponentNum());

        e.addAttribute(MailConstants.A_CAL_RSVP, invite.getRsvp());
        
        Account acct = calItem.getMailbox().getAccount();
        if (allFields) {
            boolean isRecurring = false;
            try {
                e.addAttribute("x_uid", invite.getUid());
                e.addAttribute(MailConstants.A_CAL_SEQUENCE, invite.getSeqNo());

                String itemId = ifmt.formatItemId(calItem);
                e.addAttribute(MailConstants.A_CAL_ID, itemId);
                if (invite.isEvent())
                    e.addAttribute(MailConstants.A_APPT_ID_DEPRECATE_ME, itemId);  // for backward compat

                if (invite.thisAcctIsOrganizer(acct)) {
                    e.addAttribute(MailConstants.A_CAL_ISORG, true);
                }

                Recurrence.IRecurrence recur = invite.getRecurrence();
                if (recur != null) {
                    isRecurring = true;
                    Element recurElt = e.addElement(MailConstants.E_CAL_RECUR);
                    recur.toXml(recurElt);
                }
            } catch(ServiceException ex) {
                ex.printStackTrace();
            }

            e.addAttribute(MailConstants.A_CAL_STATUS, invite.getStatus());

            String priority = invite.getPriority();
            if (priority != null)
                e.addAttribute(MailConstants.A_CAL_PRIORITY, priority);

            if (invite.isEvent()) {
                e.addAttribute(MailConstants.A_APPT_FREEBUSY, invite.getFreeBusy());
    
                Instance inst = Instance.fromInvite(calItem, invite);
                if (calItem instanceof Appointment) {
                    Appointment appt = (Appointment) calItem;
                    e.addAttribute(MailConstants.A_APPT_FREEBUSY_ACTUAL,
                                appt.getEffectiveFreeBusyActual(invite, inst));
                }
    
                e.addAttribute(MailConstants.A_APPT_TRANSPARENCY, invite.getTransparency());
    
            }

            if (includeReplies)
                encodeReplies(e, calItem, invite);

            boolean isException = invite.hasRecurId();
            if (isException)
                e.addAttribute(MailConstants.A_CAL_IS_EXCEPTION, true);

            boolean allDay = invite.isAllDayEvent();
            if (allDay)
                e.addAttribute(MailConstants.A_CAL_ALLDAY, true);

            boolean forceUTC =
                DebugConfig.calendarForceUTC && !isRecurring && !isException && !allDay;
            ParsedDateTime dtStart = invite.getStartTime();
            if (dtStart != null) {
                if (forceUTC)
                    dtStart.toUTC();
                Element startElt = e.addElement(MailConstants.E_CAL_START_TIME);
                startElt.addAttribute(MailConstants.A_CAL_DATETIME, dtStart.getDateTimePartString(false));
                if (!allDay) {
                    String tzName = dtStart.getTZName();
                    if (tzName != null)
                        startElt.addAttribute(MailConstants.A_CAL_TIMEZONE, tzName);
                }
            }

            ParsedDateTime dtEnd = invite.getEndTime();
            if (dtEnd != null) {
                if (forceUTC)
                    dtEnd.toUTC();
                Element endElt = e.addElement(MailConstants.E_CAL_END_TIME);
                if (!allDay) {
                    String tzName = dtEnd.getTZName();
                    if (tzName != null)
                        endElt.addAttribute(MailConstants.A_CAL_TIMEZONE, tzName);
                } else {
                    if (!invite.isTodo()) {
                        // See CalendarUtils.parseInviteElementCommon, where we parse DTEND
                        // for a description of why we add -1d when sending to the client
                        dtEnd = dtEnd.add(ParsedDuration.NEGATIVE_ONE_DAY);
                    }
                }
                endElt.addAttribute(MailConstants.A_CAL_DATETIME, dtEnd.getDateTimePartString(false));
            }

            ParsedDuration dur = invite.getDuration();
            if (dur != null) {
                dur.toXml(e);
            }

            e.addAttribute(MailConstants.A_NAME, invite.getName());
            e.addAttribute(MailConstants.A_CAL_LOCATION, invite.getLocation());

            // Percent Complete (VTODO)
            if (invite.isTodo()) {
                String pct = invite.getPercentComplete();
                if (pct != null)
                    e.addAttribute(MailConstants.A_TASK_PERCENT_COMPLETE, pct);
                long completed = invite.getCompleted();
                if (completed != 0) {
                    ParsedDateTime c = ParsedDateTime.fromUTCTime(completed);
                    e.addAttribute(MailConstants.A_TASK_COMPLETED, c.getDateTimePartString());
                }
            }

            // Organizer
            if (invite.hasOrganizer()) {
                ZOrganizer org = invite.getOrganizer();
                Element orgElt = e.addUniqueElement(MailConstants.E_CAL_ORGANIZER);
                String str = org.getAddress();
                orgElt.addAttribute(MailConstants.A_ADDRESS, str);
                orgElt.addAttribute(MailConstants.A_URL, str);  // for backward compatibility
                if (org.hasCn())
                    orgElt.addAttribute(MailConstants.A_DISPLAY, org.getCn());
                if (org.hasSentBy())
                    orgElt.addAttribute(MailConstants.A_CAL_SENTBY, org.getSentBy());
                if (org.hasDir())
                    orgElt.addAttribute(MailConstants.A_CAL_DIR, org.getDir());
                if (org.hasLanguage())
                    orgElt.addAttribute(MailConstants.A_CAL_LANGUAGE, org.getLanguage());
            }

            // Attendee(s)
            List<ZAttendee> attendees = invite.getAttendees();
            for (ZAttendee at : attendees) {
                at.toXml(e);
            }

            // Alarms
            Iterator<Alarm> alarmsIter = invite.alarmsIterator();
            while (alarmsIter.hasNext()) {
                Alarm alarm = alarmsIter.next();
                alarm.toXml(e);
            }

            // x-prop
            encodeXProps(e, invite.xpropsIterator());
        }

        return e;
    }

    private static void encodeXProps(Element parent, Iterator<ZProperty> xpropsIterator) {
        for (; xpropsIterator.hasNext(); ) {
            ZProperty xprop = xpropsIterator.next();
            String propName = xprop.getName();
            if (propName == null) continue;
            String propValue = xprop.getValue();
            Element propElem = parent.addElement(MailConstants.E_CAL_XPROP);
            propElem.addAttribute(MailConstants.A_NAME, propName);
            if (propValue != null)
                propElem.addAttribute(MailConstants.A_VALUE, propValue);
            for (Iterator<ZParameter> paramIter = xprop.parameterIterator(); paramIter.hasNext(); ) {
                ZParameter xparam = paramIter.next();
                String paramName = xparam.getName();
                if (paramName == null) continue;
                Element paramElem = propElem.addElement(MailConstants.E_CAL_XPARAM);
                paramElem.addAttribute(MailConstants.A_NAME, paramName);
                String paramValue = xparam.getValue();
                if (paramValue != null)
                    paramElem.addAttribute(MailConstants.A_VALUE, paramValue);
            }
        }
    }

    private static Element encodeInvitesForMessage(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Message msg, int fields)
    throws ServiceException {
        if (fields != NOTIFY_FIELDS)
            if (!needToOutput(fields, Change.MODIFIED_INVITE))
                return parent;

        Element ie = parent.addElement(MailConstants.E_INVITE);

        boolean addedMethod = false;
        Mailbox mbox = msg.getMailbox();

        for (Iterator iter = msg.getCalendarItemInfoIterator(); iter.hasNext(); ) {
            Message.CalendarItemInfo info = (Message.CalendarItemInfo) iter.next();

            CalendarItem calItem = null;
            try {
                calItem = mbox.getCalendarItemById(octxt, info.getCalendarItemId());
            } catch (MailServiceException.NoSuchItemException e) {
                // ignore
            } catch (ServiceException e) {
                // eat PERM_DENIED
                if (e.getCode() != ServiceException.PERM_DENIED)
                    throw e;
            }

            if (calItem != null) {
                setCalendarItemType(ie, calItem);
                Invite inv = calItem.getInvite(msg.getId(), info.getComponentNo());

                if (inv != null) {
                    if (!addedMethod) {
//                      ie.addAttribute("method", inv.getMethod());
                        addedMethod = true;
                    }
                    encodeTimeZoneMap(ie, calItem.getTimeZoneMap());
                    encodeInviteComponent(ie, ifmt, calItem, inv, fields, false);
                } else {
                    // invite not in this appointment anymore
                }
            } else {
                // couldn't find appointment
            }

        }

        return ie;
    }


    private static void addParts(Element parent, MPartInfo mpi, Set<MPartInfo> bodies, String prefix, boolean neuter) {
        Element elem = parent.addElement(MailConstants.E_MIMEPART);
        MimePart mp = mpi.getMimePart();

        String part = mpi.getPartName();
        part = prefix + (prefix.equals("") || part.equals("") ? "" : ".") + part;
        elem.addAttribute(MailConstants.A_PART, part);

        String ctype = StringUtil.stripControlCharacters(mpi.getContentType());
        if (Mime.CT_XML_ZIMBRA_SHARE.equals(ctype)) {
            Element shr = parent.getParent().addElement("shr");
            try {
                addContent(shr, mpi);
            } catch (IOException e) {
                if (mLog.isWarnEnabled())
                    mLog.warn("error writing body part: ", e);
            } catch (MessagingException e) {
            }
        } else if (Mime.CT_TEXT_ENRICHED.equals(ctype)) {
            // we'll be replacing text/enriched with text/html
            ctype = Mime.CT_TEXT_HTML;
        }
        elem.addAttribute(MailConstants.A_CONTENT_TYPE, ctype);

        // figure out attachment size
        try {
            int size = mp.getSize();
            if ("base64".equalsIgnoreCase(mp.getEncoding()))
                size = (int) ((size * 0.75) - (size / 76));
            elem.addAttribute(MailConstants.A_SIZE, size);
        } catch (MessagingException me) {
            // don't put out size if we get exception
        }

        // figure out attachment disposition
        try {
            String disp = mp.getHeader("Content-Disposition", null);
            if (disp != null) {
                ContentDisposition cdisp = new ContentDisposition(MimeUtility.decodeText(disp));
                elem.addAttribute(MailConstants.A_CONTENT_DISPOSTION, StringUtil.stripControlCharacters(cdisp.getDisposition()));
            }
        } catch (MessagingException me) {
        } catch (UnsupportedEncodingException uee) {
        }

        // figure out attachment name
        try {
            String fname = Mime.getFilename(mp);
            if (fname == null && Mime.CT_MESSAGE_RFC822.equals(ctype)) {
                // "filename" for attached messages is the Subject
                Object content = Mime.getMessageContent(mp);
                if (content instanceof MimeMessage)
                    fname = ((MimeMessage) content).getSubject();
            }
            if (fname != null && !fname.equals(""))
                elem.addAttribute(MailConstants.A_CONTENT_FILENAME, StringUtil.stripControlCharacters(fname));
        } catch (MessagingException me) {
        } catch (IOException ioe) {
        }

        // figure out content-id (used in displaying attached images)
        try {
            String cid = mp.getContentID();
            if (cid != null)
                elem.addAttribute(MailConstants.A_CONTENT_ID, StringUtil.stripControlCharacters(cid));
        } catch (MessagingException me) { }


        // figure out content-location (used in displaying attached images)
        try {
            String cl = mp.getHeader("Content-Location", null);
            if (cl != null)
                elem.addAttribute(MailConstants.A_CONTENT_LOCATION, StringUtil.stripControlCharacters(cl));
        } catch (MessagingException me) { }

        // include the part's content if this is the displayable "memo part"
        if (bodies != null && bodies.contains(mpi)) {
            elem.addAttribute(MailConstants.A_BODY, true);
            try {
                addContent(elem, mpi, neuter);
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
                addParts(elem, cp, bodies, prefix, neuter);
            }
        }
    }

    /** Adds the decoded text content of a message part to the {@link Element}.
     *  The content is added as the value of a new <tt>&lt;content></tt>
     *  sub-element.  <i>Note: This method will only extract the content of a
     *  <b>text/*</b> or <b>xml/*</b> message part.</i> 
     * @param elt  The element to add the <tt>&lt;content></tt> to.
     * @param mpi  The message part to extract the content from.
     * @throws MessagingException when message parsing or CTE-decoding fails
     * @throws IOException on error during parsing or defanging */
    private static void addContent(Element elt, MPartInfo mpi) throws IOException, MessagingException {
        addContent(elt, mpi, true);
    }

    /** Adds the decoded text content of a message part to the {@link Element}.
     *  The content is added as the value of a new <tt>&lt;content></tt>
     *  sub-element.  <i>Note: This method will only extract the content of a
     *  <b>text/*</b> or <b>xml/*</b> message part.</i> 
     * @param elt     The element to add the <tt>&lt;content></tt> to.
     * @param mpi     The message part to extract the content from.
     * @param neuter  Whether to "neuter" image <tt>src</tt> attributes.
     * @throws MessagingException when message parsing or CTE-decoding fails
     * @throws IOException on error during parsing or defanging
     * @see HtmlDefang#defang(String, boolean) */
    private static void addContent(Element elt, MPartInfo mpi, boolean neuter) throws IOException, MessagingException {
        // TODO: support other parts
        String ctype = mpi.getContentType();
        if (!ctype.matches(Mime.CT_TEXT_WILD) && !ctype.matches(Mime.CT_XML_WILD))
            return;

        MimePart mp = mpi.getMimePart();
        String data = null;
        if (ctype.equals(Mime.CT_TEXT_HTML)) {
            String charset = mpi.getContentTypeParameter(Mime.P_CHARSET);
            if (!(charset == null || charset.trim().equals(""))) {
                data = Mime.getStringContent(mp);
                data = HtmlDefang.defang(data, neuter);                    
            } else {
                InputStream is = null;
                try {
                    is = mp.getInputStream();
                    data = HtmlDefang.defang(is, neuter);
                } finally {
                    if (is != null) is.close();
                }
            }
        } else if (ctype.equals(Mime.CT_TEXT_ENRICHED)) {
            data = TextEnrichedHandler.convertToHTML(Mime.getStringContent(mp));
        } else {
            data = Mime.getStringContent(mp);
        }

        if (data != null) {
            data = StringUtil.stripControlCharacters(data);
            elt.addAttribute(MailConstants.E_CONTENT, data, Element.Disposition.CONTENT);
        }
        // TODO: CDATA worth the effort?
    }


    public enum EmailType {
        NONE(null), FROM("f"), TO("t"), CC("c"), BCC("b"), REPLY_TO("r"), SENDER("s");

        private final String mRep;
        private EmailType(String c)  { mRep = c; }

        public String toString()     { return mRep; }
    }

    /**
     * @param m
     * @param recipients
     * @param email_type_to
     */
    private static void addEmails(Element m, InternetAddress[] recipients, EmailType emailType) {
        for (int i = 0; i < recipients.length; i++)
            encodeEmail(m, recipients[i], emailType);
    }

    public static Element encodeEmail(Element parent, InternetAddress ia, EmailType type) {
        return encodeEmail(parent, new ParsedAddress(ia).parse(), type);
    }

    public static Element encodeEmail(Element parent, String addr, EmailType type) {
        return encodeEmail(parent, new ParsedAddress(addr).parse(), type);
    }

    public static Element encodeEmail(Element parent, ParsedAddress pa, EmailType type) {
        Element elem = parent.addElement(MailConstants.E_EMAIL);
        elem.addAttribute(MailConstants.A_ADDRESS, pa.emailPart);
        elem.addAttribute(MailConstants.A_DISPLAY, pa.firstName);
        elem.addAttribute(MailConstants.A_PERSONAL, pa.personalPart);
        elem.addAttribute(MailConstants.A_ADDRESS_TYPE, type.toString());
        return elem;
    }

    public static Element encodeWiki(Element parent, ItemIdFormatter ifmt, WikiItem wiki, int rev) {
        return encodeWiki(parent, ifmt, wiki, NOTIFY_FIELDS, rev);
    }

    public static Element encodeWiki(Element parent, ItemIdFormatter ifmt, WikiItem wiki, int fields, int rev) {
        Element m = parent.addElement(MailConstants.E_WIKIWORD);
        encodeDocumentCommon(m, ifmt, wiki, fields, rev);
        return m;
    }

    public static Element encodeDocument(Element parent, ItemIdFormatter ifmt, Document doc, int rev) {
        return encodeDocument(parent, ifmt, doc, NOTIFY_FIELDS, rev);
    }

    public static Element encodeDocument(Element parent, ItemIdFormatter ifmt, Document doc, int fields, int rev) {
        Element m = parent.addElement(MailConstants.E_DOC);
        encodeDocumentCommon(m, ifmt, doc, fields, rev);
        m.addAttribute(MailConstants.A_CONTENT_TYPE, doc.getContentType());
        return m;
    }

    public static Element encodeDocumentCommon(Element m, ItemIdFormatter ifmt, Document doc, int fields, int rev) {
        m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(doc));
        if (needToOutput(fields, Change.MODIFIED_NAME)) {
        	m.addAttribute(MailConstants.A_NAME, doc.getName());
        	encodeRestUrl(m, doc);
        }
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            m.addAttribute(MailConstants.A_SIZE, doc.getSize());
        if (needToOutput(fields, Change.MODIFIED_DATE))
            m.addAttribute(MailConstants.A_DATE, doc.getDate());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            m.addAttribute(MailConstants.A_FOLDER, new ItemId(doc.getMailbox().getAccountId(), doc.getFolderId()).toString(ifmt));
        recordItemTags(m, doc, fields);

        if (needToOutput(fields, Change.MODIFIED_CONTENT)) {

            try {
                Document.DocumentRevision revision = doc.getRevision(1);
                m.addAttribute(MailConstants.A_CREATOR, revision.getCreator());
                m.addAttribute(MailConstants.A_CREATED_DATE, revision.getRevDate());

                revision = (rev > 0) ? doc.getRevision(rev) : doc.getLastRevision(); 
                m.addAttribute(MailConstants.A_VERSION, revision.getVersion());
                m.addAttribute(MailConstants.A_LAST_EDITED_BY, revision.getCreator());
                m.addAttribute(MailConstants.A_MODIFIED_DATE, revision.getRevDate());
                String frag = revision.getFragment();
                if (frag != null && !frag.equals(""))
                    m.addAttribute(MailConstants.E_FRAG, frag, Element.Disposition.CONTENT);
            } catch (Exception ex) {
                mLog.warn("ignoring exception while fetching revision for document " + doc.getSubject(), ex);
            }
        }

        return m;
    }

    public static Element encodeWikiPage(Element parent, WikiPage page) {
        Element m = parent.addElement(MailConstants.E_WIKIWORD);
        m.addAttribute(MailConstants.A_NAME, page.getWikiWord());
        m.addAttribute(MailConstants.A_ID, page.getId());
        m.addAttribute(MailConstants.A_REST_URL, page.getRestUrl());
        //m.addAttribute(MailService.A_SIZE, page.getSize());
        m.addAttribute(MailConstants.A_DATE, page.getModifiedDate());
        m.addAttribute(MailConstants.A_FOLDER, page.getFolderId());
        m.addAttribute(MailConstants.A_VERSION, page.getLastRevision());
        m.addAttribute(MailConstants.A_CREATOR, page.getCreator());
        m.addAttribute(MailConstants.A_CREATED_DATE, page.getCreatedDate());
        m.addAttribute(MailConstants.A_LAST_EDITED_BY, page.getLastEditor());
        m.addAttribute(MailConstants.A_MODIFIED_DATE, page.getModifiedDate());
        String frag = page.getFragment();
        if (frag != null && !frag.equals(""))
            m.addAttribute(MailConstants.E_FRAG, frag, Element.Disposition.CONTENT);
        return m;
    }

    public static Element encodeDataSource(Element parent, DataSource ds) {
        Element m;
        if (ds.getType() == DataSource.Type.imap) {
            m = parent.addElement(MailConstants.E_DS_IMAP);
        } else {
            m = parent.addElement(MailConstants.E_DS_POP3);
        }
        m.addAttribute(MailConstants.A_ID, ds.getId());
        m.addAttribute(MailConstants.A_NAME, ds.getName());
        m.addAttribute(MailConstants.A_FOLDER, ds.getFolderId());
        m.addAttribute(MailConstants.A_DS_IS_ENABLED, ds.isEnabled());
        
        if (ds.getType() == DataSource.Type.pop3) {
            m.addAttribute(MailConstants.A_DS_LEAVE_ON_SERVER, ds.leaveOnServer());
        }
        
        if (ds.getHost() != null) { 
            m.addAttribute(MailConstants.A_DS_HOST, ds.getHost());
        }
        if (ds.getPort() != null) {
            m.addAttribute(MailConstants.A_DS_PORT, ds.getPort());
        }
        if (ds.getConnectionType() != null) {
            m.addAttribute(MailConstants.A_DS_CONNECTION_TYPE, ds.getConnectionType().name());
        }
        if (ds.getUsername() != null) {
            m.addAttribute(MailConstants.A_DS_USERNAME, ds.getUsername());
        }
        if (ds.getPollingInterval() > 0) {
            m.addAttribute(MailConstants.A_DS_POLLING_INTERVAL,
                ds.getAttr(Provisioning.A_zimbraDataSourcePollingInterval));
        }
        return m;
    }

    private static void setCalendarItemType(Element elem, CalendarItem calItem) {
        elem.addAttribute(MailConstants.A_CAL_ITEM_TYPE,
                calItem.getType() == MailItem.TYPE_APPOINTMENT ? "appt" : "task");
    }
}
