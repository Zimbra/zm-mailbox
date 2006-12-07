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
 * Created on 2004. 9. 15.
 */
package com.zimbra.cs.service.mail;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.html.HtmlDefang;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SearchParams.ExpandResults;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone.SimpleOnset;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
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
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.wiki.WikiPage;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

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

    public static Element encodeItem(Element parent, ZimbraSoapContext lc, MailItem item)
    throws ServiceException {
        return encodeItem(parent, lc, item, NOTIFY_FIELDS);
    }
    public static Element encodeItem(Element parent, ZimbraSoapContext lc, MailItem item, int fields)
    throws ServiceException {
        if (item instanceof Folder)
            return encodeFolder(parent, lc, (Folder) item, fields);
        else if (item instanceof Tag)
            return encodeTag(parent, lc, (Tag) item, fields);
        else if (item instanceof Note)
            return encodeNote(parent, lc, (Note) item, fields);
        else if (item instanceof Contact)
            return encodeContact(parent, lc, (Contact) item, null, false, null, fields);
        else if (item instanceof CalendarItem) 
            return encodeCalendarItemSummary(parent, lc, (CalendarItem) item, fields);
        else if (item instanceof Conversation)
            return encodeConversationSummary(parent, lc, (Conversation) item, fields);
        else if (item instanceof WikiItem)
            return encodeWiki(parent, lc, (WikiItem) item, fields, -1);
        else if (item instanceof Document)
            return encodeDocument(parent, lc, (Document) item, fields, -1);
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

    public static Element encodeFolder(Element parent, ZimbraSoapContext lc, Folder folder) {
        return encodeFolder(parent, lc, folder, NOTIFY_FIELDS);
    }
    public static Element encodeFolder(Element parent, ZimbraSoapContext lc, Folder folder, int fields) {
        if (folder instanceof SearchFolder)
            return encodeSearchFolder(parent, lc, (SearchFolder) folder, fields);
        else if (folder instanceof Mountpoint)
            return encodeMountpoint(parent, lc, (Mountpoint) folder, fields);

        Element elem = parent.addElement(MailService.E_FOLDER);
        encodeFolderCommon(elem, lc, folder, fields);
        if (needToOutput(fields, Change.MODIFIED_SIZE)) {
            long size = folder.getSize();
            if (size > 0 || fields != NOTIFY_FIELDS)
                elem.addAttribute(MailService.A_NUM, size);
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
                elem.addAttribute(MailService.A_URL, url);
            }
        }
        boolean remote = lc.isDelegatedRequest(), canAdminister = !remote;
        if (remote) {
            // return only effective permissions for remote folders
            try {
                Mailbox.OperationContext octxt = lc.getOperationContext();
                short perms = folder.getMailbox().getEffectivePermissions(octxt, folder.getId(), MailItem.TYPE_FOLDER);
                elem.addAttribute(MailService.A_RIGHTS, ACL.rightsToString(perms));
                canAdminister = (perms & ACL.RIGHT_ADMIN) != 0;
            } catch (ServiceException e) {
                mLog.warn("ignoring exception while fetching effective permissions for folder " + folder.getId(), e);
            }
        }
        if (canAdminister) {
            // return full ACLs for folders we have admin rights on
            if (needToOutput(fields, Change.MODIFIED_ACL)) {
                ACL acl = folder.getPermissions();
                if (acl != null || fields != NOTIFY_FIELDS) {
                    Element eACL = elem.addUniqueElement(MailService.E_ACL);
                    if (acl != null) {
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
            }
        }
        return elem;
    }

    private static Element encodeFolderCommon(Element elem, ZimbraSoapContext lc, Folder folder, int fields) {
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
                elem.addAttribute(MailService.A_FLAGS, flags);
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

    public static Element encodeSearchFolder(Element parent, ZimbraSoapContext lc, SearchFolder search) {
        return encodeSearchFolder(parent, lc, search, NOTIFY_FIELDS);
    }
    public static Element encodeSearchFolder(Element parent, ZimbraSoapContext lc, SearchFolder search, int fields) {
        Element elem = parent.addElement(MailService.E_SEARCH);
        encodeFolderCommon(elem, lc, search, fields);
        if (needToOutput(fields, Change.MODIFIED_QUERY)) {
            elem.addAttribute(MailService.A_QUERY, search.getQuery());
            elem.addAttribute(MailService.A_SORTBY, search.getSortField());
            elem.addAttribute(MailService.A_SEARCH_TYPES, search.getReturnTypes());
        }
        return elem;
    }

    public static Element encodeMountpoint(Element parent, ZimbraSoapContext lc, Mountpoint mpt) {
        return encodeMountpoint(parent, lc, mpt, NOTIFY_FIELDS);
    }
    public static Element encodeMountpoint(Element parent, ZimbraSoapContext lc, Mountpoint mpt, int fields) {
        Element elem = parent.addElement(MailService.E_MOUNT);
        encodeFolderCommon(elem, lc, mpt, fields);
        if (needToOutput(fields, Change.MODIFIED_CONTENT)) {
            elem.addAttribute(MailService.A_ZIMBRA_ID, mpt.getOwnerId());
            elem.addAttribute(MailService.A_REMOTE_ID, mpt.getRemoteId());
            NamedEntry nentry = FolderAction.lookupGranteeByZimbraId(mpt.getOwnerId(), ACL.GRANTEE_USER);
            elem.addAttribute(MailService.A_OWNER_NAME, nentry == null ? null : nentry.getName());
            if (mpt.getDefaultView() != MailItem.TYPE_UNKNOWN)
                elem.addAttribute(MailService.A_DEFAULT_VIEW, MailItem.getNameForType(mpt.getDefaultView()));
        }
        return elem;
    }

    public static Element encodeRestUrl(Element elt, MailItem item) {
        try {
            Account account = item.getMailbox().getAccount();
            String url = UserServlet.getRestUrl(account);
            String path = url + item.getPath();
            if (item instanceof Folder)
                path = path + "/";
            if (url.startsWith("https"))
                url = new HttpsURL(path).toString();
            else
                url = new HttpURL(path).toString();
            return elt.addAttribute(MailService.A_REST_URL, url);
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
                elem.addAttribute(MailService.A_FLAGS, flags);
        }
        if (needToOutput(fields, Change.MODIFIED_TAGS)) {
            String tags = item.getTagString();
            if (!tags.equals("") || fields != NOTIFY_FIELDS)
                elem.addAttribute(MailService.A_TAGS, tags);
        }
    }

    public static Element encodeContact(Element parent, ZimbraSoapContext lc, Contact contact,
                ContactAttrCache cacache, boolean summary, List<String> attrFilter) {
        return encodeContact(parent, lc, contact, cacache, summary, attrFilter, NOTIFY_FIELDS);
    }
    public static Element encodeContact(Element parent, ZimbraSoapContext lc, Contact contact,
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
        if (needToOutput(fields, Change.MODIFIED_CONTENT)) {
            elem.addAttribute(MailService.A_DATE, contact.getDate());
            elem.addAttribute(MailService.A_REVISION, contact.getSavedSequence());
        }

        if (summary || !needToOutput(fields, Change.MODIFIED_CONTENT)) {
            try {
                elem.addAttribute(MailService.A_FILE_AS_STR, contact.getFileAsString());

                String email = contact.get(Contact.A_email);
                if (email != null)
                    elem.addAttribute(Contact.A_email, email);

                email = contact.get(Contact.A_email2);
                if (email != null)
                    elem.addAttribute(Contact.A_email2, email);

                email = contact.get(Contact.A_email3);
                if (email != null)
                    elem.addAttribute(Contact.A_email3, email);

                String type = contact.get(Contact.A_type);
                if (type == null) {
                    String dlist = contact.get(Contact.A_dlist);
                    if (dlist != null) type = Contact.TYPE_GROUP;
                }
                if (type != null)
                    elem.addAttribute(Contact.A_type,  type);

                
                // send back date with summary via search results
                elem.addAttribute(MailService.A_CHANGE_DATE, contact.getChangeDate() / 1000);
            } catch (ServiceException e) { }
            return elem;
        }


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

    public static Element encodeNote(Element parent, ZimbraSoapContext lc, Note note) {
        return encodeNote(parent, lc, note, NOTIFY_FIELDS);
    }
    public static Element encodeNote(Element parent, ZimbraSoapContext lc, Note note, int fields) {
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

    public static Element encodeTag(Element parent, ZimbraSoapContext lc, Tag tag) {
        return encodeTag(parent, lc, tag, NOTIFY_FIELDS);
    }
    public static Element encodeTag(Element parent, ZimbraSoapContext lc, Tag tag, int fields) {
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
            elem.addAttribute(MailService.A_DATE, tag.getDate());
            elem.addAttribute(MailService.A_REVISION, tag.getSavedSequence());
            elem.addAttribute(MailService.A_CHANGE_DATE, tag.getChangeDate() / 1000);
            elem.addAttribute(MailService.A_MODIFIED_SEQUENCE, tag.getModifiedSequence());
        }
        return elem;
    }


    public static Element encodeConversation(Element parent, ZimbraSoapContext lc, Conversation conv, SearchParams params) throws ServiceException {
        int fields = NOTIFY_FIELDS;
        Mailbox mbox = conv.getMailbox();
        Element c = encodeConversationCommon(parent, lc, conv, fields);

        ExpandResults expand = params.getFetchFirst();
        List<Message> messages = mbox.getMessagesByConversation(lc.getOperationContext(), conv.getId());
        for (Message msg : messages) {
            if (msg.isTagged(mbox.mDeletedFlag))
                continue;
            if (expand == ExpandResults.FIRST || expand == ExpandResults.ALL) {
                encodeMessageAsMP(c, lc, msg, null, params.getWantHtml(), params.getNeuterImages());
                if (expand == ExpandResults.FIRST)
                    expand = ExpandResults.NONE;
            } else {
                Element m = c.addElement(MailService.E_MSG);
                m.addAttribute(MailService.A_ID, lc.formatItemId(msg));
                m.addAttribute(MailService.A_DATE, msg.getDate());
                m.addAttribute(MailService.A_SIZE, msg.getSize());
                m.addAttribute(MailService.A_FOLDER, lc.formatItemId(msg.getFolderId()));
                recordItemTags(m, msg, fields);
                m.addAttribute(MailService.E_FRAG, msg.getFragment(), Element.DISP_CONTENT);
                encodeEmail(m, msg.getSender(), EmailType.FROM);
            }
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
    public static Element encodeConversationSummary(Element parent, ZimbraSoapContext lc, Conversation conv, int fields) {
        return encodeConversationSummary(parent, lc, conv, null, OutputParticipants.PUT_SENDERS, fields);
    }

    public static Element encodeConversationSummary(Element parent, ZimbraSoapContext lc, Conversation conv, Message msgHit, OutputParticipants output) {
        return encodeConversationSummary(parent, lc, conv, msgHit, output, NOTIFY_FIELDS);
    }

    private static Element encodeConversationSummary(Element parent, ZimbraSoapContext lc, Conversation conv, Message msgHit, OutputParticipants output, int fields) {
        Element c = encodeConversationCommon(parent, lc, conv, fields);
        if (needToOutput(fields, Change.MODIFIED_DATE))
            c.addAttribute(MailService.A_DATE, msgHit != null ? msgHit.getDate() : conv.getDate());
        if (needToOutput(fields, Change.MODIFIED_SUBJECT))
            c.addAttribute(MailService.E_SUBJECT, conv.getSubject());
        if (fields == NOTIFY_FIELDS && msgHit != null)
            c.addAttribute(MailService.E_FRAG, msgHit.getFragment(), Element.DISP_CONTENT);

        boolean addRecips  = msgHit != null && msgHit.isFromMe() && (output == OutputParticipants.PUT_RECIPIENTS || output == OutputParticipants.PUT_BOTH);
        boolean addSenders = output == OutputParticipants.PUT_BOTH || !addRecips;

        if (addRecips) {
            try {
                InternetAddress[] addrs = InternetAddress.parseHeader(msgHit.getRecipients(), false);
                addEmails(c, addrs, EmailType.TO);
            } catch (AddressException e1) { }
        }

        if (addSenders && needToOutput(fields, Change.MODIFIED_SENDERS)) {
            Mailbox mbox = conv.getMailbox();

            SenderList sl;
            try {
                if (conv.isTagged(mbox.mDeletedFlag)) {
                    sl = new SenderList();
                    for (Message msg : mbox.getMessagesByConversation(lc.getOperationContext(), conv.getId(), Conversation.SORT_DATE_ASCENDING)) {
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
                c.addAttribute(MailService.A_ELIDED, true);
            for (ParsedAddress pa : sl.getLastAddresses())
                encodeEmail(c, pa, EmailType.FROM);
        }

        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            c.addAttribute(MailService.A_CHANGE_DATE, conv.getChangeDate() / 1000);
            c.addAttribute(MailService.A_MODIFIED_SEQUENCE, conv.getModifiedSequence());
        }
        return c;
    }

    private static Element encodeConversationCommon(Element parent, ZimbraSoapContext lc, Conversation conv, int fields) {
        Element c = parent.addElement(MailService.E_CONV);
        c.addAttribute(MailService.A_ID, lc.formatItemId(conv));
        if (needToOutput(fields, Change.MODIFIED_CHILDREN | Change.MODIFIED_SIZE)) {
            int count = conv.getMessageCount(), nondeleted = conv.getNondeletedCount();
            c.addAttribute(MailService.A_NUM, nondeleted);
            if (count != nondeleted)
                c.addAttribute(MailService.A_TOTAL_SIZE, count);
        }
        recordItemTags(c, conv, fields);
        if (fields == NOTIFY_FIELDS)
            c.addAttribute(MailService.E_SUBJECT, conv.getSubject(), Element.DISP_CONTENT);
        return c;
    }

    /** Encodes a Message object into <m> element with <mp> elements for
     *  message body.
     * @param parent  The Element to add the new <code>&lt;m></code> to.
     * @param lc      The SOAP request's context.
     * @param msg     The Message to serialize.
     * @param part    If non-null, we'll serialuize this message/rfc822 subpart
     *                of the specified Message instead of the Message itself.
     * @param wantHTML  <code>true</code> to prefer HTML parts as the "body",
     *                  <code>false</code> to prefer text/plain parts.
     * @param neuter  Whether to rename "src" attributes on HTML <img> tags.
     * @return The newly-created <code>&lt;m></code> Element, which has already
     *         been added as a child to the passed-in <code>parent</code>.
     * @throws ServiceException */
    public static Element encodeMessageAsMP(Element parent, ZimbraSoapContext lc, Message msg, String part, boolean wantHTML, boolean neuter)
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

            addEmails(m, Mime.parseAddressHeader(mm, "From"), EmailType.FROM);
            addEmails(m, Mime.parseAddressHeader(mm, "Reply-To"), EmailType.REPLY_TO);
            addEmails(m, Mime.parseAddressHeader(mm, "To"), EmailType.TO);
            addEmails(m, Mime.parseAddressHeader(mm, "Cc"), EmailType.CC);
            addEmails(m, Mime.parseAddressHeader(mm, "Bcc"), EmailType.BCC);

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
                if (!msg.getDraftIdentityId().equals(""))
                    m.addAttribute(MailService.A_IDENTITY_ID, msg.getDraftIdentityId());
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
     * @param lc TODO
     * @param calItem
     * @param fields
     * 
     * @return
     */
    public static Element encodeCalendarItemSummary(Element parent, ZimbraSoapContext lc,
                CalendarItem calItem, int fields)
    throws ServiceException {
        Element calItemElem;
        if (calItem instanceof Appointment)
            calItemElem = parent.addElement(MailService.E_APPOINTMENT);
        else
            calItemElem = parent.addElement(MailService.E_TASK);

        calItemElem.addAttribute(MailService.A_UID, calItem.getUid());
        calItemElem.addAttribute(MailService.A_ID, lc.formatItemId(calItem));
        calItemElem.addAttribute(MailService.A_FOLDER, lc.formatItemId(calItem.getFolderId()));

        if (needToOutput(fields, Change.MODIFIED_CONTENT) && calItem.getSavedSequence() != 0)
            calItemElem.addAttribute(MailService.A_REVISION, calItem.getSavedSequence());
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            calItemElem.addAttribute(MailService.A_SIZE, calItem.getSize());
        if (needToOutput(fields, Change.MODIFIED_DATE))
            calItemElem.addAttribute(MailService.A_DATE, calItem.getDate());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            calItemElem.addAttribute(MailService.A_FOLDER, lc.formatItemId(calItem.getFolderId()));
        recordItemTags(calItemElem, calItem, fields);
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            calItemElem.addAttribute(MailService.A_CHANGE_DATE, calItem.getChangeDate() / 1000);
            calItemElem.addAttribute(MailService.A_MODIFIED_SEQUENCE, calItem.getModifiedSequence());
        }

        for (int i = 0; i < calItem.numInvites(); i++) {
            Invite inv = calItem.getInvite(i);

            Element ie = calItemElem.addElement(MailService.E_INVITE);
            encodeTimeZoneMap(ie, calItem.getTimeZoneMap());

            ie.addAttribute(MailService.A_CAL_SEQUENCE, inv.getSeqNo());
            encodeReplies(ie, calItem, inv);

            ie.addAttribute(MailService.A_ID, lc.formatItemId(inv.getMailItemId()));
            ie.addAttribute(MailService.A_CAL_COMPONENT_NUM, inv.getComponentNum());
            if (inv.hasRecurId())
                ie.addAttribute(MailService.A_CAL_RECURRENCE_ID, inv.getRecurId().toString());

            encodeInvite(ie, lc, calItem, inv, NOTIFY_FIELDS, false);
        }

        return calItemElem;
    }

    private static void encodeReplies(Element parent, CalendarItem calItem, Invite inv) {
        Element repliesElt = parent.addElement(MailService.E_CAL_REPLIES);

        List /*CalendarItem.ReplyInfo */ replies = calItem.getReplyInfo(inv);
        for (Iterator iter = replies.iterator(); iter.hasNext(); ) {
            CalendarItem.ReplyInfo repInfo = (CalendarItem.ReplyInfo) iter.next();
            ZAttendee attendee = repInfo.mAttendee;

            Element curElt = repliesElt.addElement(MailService.E_CAL_REPLY);
            if (repInfo.mRecurId != null) {
                repInfo.mRecurId.toXml(curElt);
            }
            if (attendee.hasPartStat()) {
                curElt.addAttribute(MailService.A_CAL_PARTSTAT, attendee.getPartStat());
            }
            curElt.addAttribute(MailService.A_DATE, repInfo.mDtStamp);
            curElt.addAttribute(MailService.A_CAL_ATTENDEE, attendee.getAddress());
            if (attendee.hasSentBy())
                curElt.addAttribute(MailService.A_CAL_SENTBY, attendee.getSentBy());
        }
    }


    /** Encodes an Invite stored within a calendar item object into <m> element
     *  with <mp> elements.
     * @param parent  The Element to add the new <code>&lt;m></code> to.
     * @param lc      The SOAP request's context.
     * @param calItem The calendar item to serialize.
     * @param iid     The requested item; the contained subpart will be used to
     *                pick the Invite out of the calendar item's blob & metadata.
     * @param part    If non-null, we'll serialuize this message/rfc822 subpart
     *                of the specified Message instead of the Message itself.
     * @param wantHTML  <code>true</code> to prefer HTML parts as the "body",
     *                  <code>false</code> to prefer text/plain parts.
     * @param neuter  Whether to rename "src" attributes on HTML <img> tags.
     * @return The newly-created <code>&lt;m></code> Element, which has already
     *         been added as a child to the passed-in <code>parent</code>.
     * @throws ServiceException */
    public static Element encodeInviteAsMP(Element parent, ZimbraSoapContext lc, CalendarItem calItem, ItemId iid, String part, boolean wantHTML, boolean neuter)
    throws ServiceException {
        int invId = iid.getSubpartId();
        boolean wholeMessage = (part == null || part.trim().equals(""));

        boolean repliesWithInvites = true;

        Element m;
        if (wholeMessage) {
            m = encodeMessageCommon(parent, lc, calItem, NOTIFY_FIELDS);
            m.addAttribute(MailService.A_ID, lc.formatItemId(calItem, invId));
        } else {
            m = parent.addElement(MailService.E_MSG);
            m.addAttribute(MailService.A_ID, lc.formatItemId(calItem, invId));
            m.addAttribute(MailService.A_PART, part);
        }

        try {
            MimeMessage mm = calItem.getSubpartMessage(invId);
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

            addEmails(m, Mime.parseAddressHeader(mm, "From"), EmailType.FROM);
            addEmails(m, Mime.parseAddressHeader(mm, "Reply-To"), EmailType.REPLY_TO);
            addEmails(m, Mime.parseAddressHeader(mm, "To"), EmailType.TO);
            addEmails(m, Mime.parseAddressHeader(mm, "Cc"), EmailType.CC);
            addEmails(m, Mime.parseAddressHeader(mm, "Bcc"), EmailType.BCC);

            String subject = mm.getSubject();
            if (subject != null)
                m.addAttribute(MailService.E_SUBJECT, StringUtil.stripControlCharacters(subject), Element.DISP_CONTENT);
            String messageID = mm.getMessageID();
            if (messageID != null && !messageID.trim().equals(""))
                m.addAttribute(MailService.E_MSG_ID_HDR, StringUtil.stripControlCharacters(messageID), Element.DISP_CONTENT);

//          if (wholeMessage && msg.isDraft()) {
//              if (msg.getDraftOrigId() > 0)
//                  m.addAttribute(MailService.A_ORIG_ID, msg.getDraftOrigId());
//              if (!msg.getDraftReplyType().equals(""))
//                  m.addAttribute(MailService.A_REPLY_TYPE, msg.getDraftReplyType());
//              String inReplyTo = mm.getHeader("In-Reply-To", null);
//              if (inReplyTo != null && !inReplyTo.equals(""))
//                  m.addAttribute(MailService.E_IN_REPLY_TO, StringUtil.stripControlCharacters(inReplyTo), Element.DISP_CONTENT);
//          }

            if (!wholeMessage)
                m.addAttribute(MailService.A_SIZE, mm.getSize());

            java.util.Date sent = mm.getSentDate();
            if (sent != null)
                m.addAttribute(MailService.A_SENT_DATE, sent.getTime());

            Element invElt = m.addElement(MailService.E_INVITE);
            encodeTimeZoneMap(invElt, calItem.getTimeZoneMap());
            for (Invite inv : calItem.getInvites(invId))
                encodeInvite(invElt, lc, calItem, inv, NOTIFY_FIELDS, repliesWithInvites);

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
     * @param lc TODO
     * @param msg
     * @param part TODO
     * @return
     * @throws ServiceException
     */
    public static Element encodeMessageAsMIME(Element parent, ZimbraSoapContext lc, Message msg, String part)
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
        int size = msg.getSize() + 2048;
        if (!wholeMessage) {
            content.addAttribute(MailService.A_URL, CONTENT_SERVLET_URI + lc.formatItemId(msg) + PART_PARAM_STRING + part);
        } else if (size > MAX_INLINE_MSG_SIZE) {
            content.addAttribute(MailService.A_URL, CONTENT_SERVLET_URI + lc.formatItemId(msg));
        } else {
            try {
                byte[] raw = msg.getMessageContent();
                if (!ByteUtil.isASCII(raw))
                    content.addAttribute(MailService.A_URL, CONTENT_SERVLET_URI + lc.formatItemId(msg));
                else
                    content.setText(new String(raw, "US-ASCII"));
            } catch (IOException ex) {
                throw ServiceException.FAILURE(ex.getMessage(), ex);
            }
        }

        return m;
    }

    public static enum OutputParticipants { PUT_SENDERS, PUT_RECIPIENTS, PUT_BOTH }

    public static Element encodeMessageSummary(Element parent, ZimbraSoapContext lc, Message msg, OutputParticipants output) {
        return encodeMessageSummary(parent, lc, msg, output, NOTIFY_FIELDS);
    }
    public static Element encodeMessageSummary(Element parent, ZimbraSoapContext lc, Message msg, OutputParticipants output, int fields) {
        Element e = encodeMessageCommon(parent, lc, msg, fields);
        e.addAttribute(MailService.A_ID, lc.formatItemId(msg));

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

        e.addAttribute(MailService.E_SUBJECT, StringUtil.stripControlCharacters(msg.getSubject()), Element.DISP_CONTENT);

        // fragment has already been sanitized...
        String fragment = msg.getFragment();
        if (!fragment.equals(""))
            e.addAttribute(MailService.E_FRAG, fragment, Element.DISP_CONTENT);

        if (msg.isInvite()) {
            try {
                encodeInvitesForMessage(e, lc, msg, fields);
            } catch (ServiceException ex) {
                mLog.debug("Caught exception while encoding Invites for msg " + msg.getId(), ex);
            }
        }

        return e;
    }

    private static Element encodeMessageCommon(Element parent, ZimbraSoapContext lc, MailItem item, int fields) {
        Element elem = parent.addElement(MailService.E_MSG);
        // DO NOT encode the item-id here, as some Invite-Messages-In-CalendarItems have special item-id's
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            elem.addAttribute(MailService.A_SIZE, item.getSize());
        if (needToOutput(fields, Change.MODIFIED_DATE))
            elem.addAttribute(MailService.A_DATE, item.getDate());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            elem.addAttribute(MailService.A_FOLDER, lc.formatItemId(item.getFolderId()));
        if (item instanceof Message) {
            Message msg = (Message) item;
            if (needToOutput(fields, Change.MODIFIED_PARENT) && (fields != NOTIFY_FIELDS || msg.getConversationId() != -1))
                elem.addAttribute(MailService.A_CONV_ID, lc.formatItemId(msg.getConversationId()));
        }
        recordItemTags(elem, item, fields);

        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            elem.addAttribute(MailService.A_REVISION, item.getSavedSequence());
            elem.addAttribute(MailService.A_CHANGE_DATE, item.getChangeDate() / 1000);
            elem.addAttribute(MailService.A_MODIFIED_SEQUENCE, item.getModifiedSequence());
        } else if (needToOutput(fields, Change.MODIFIED_CONTENT) && item.getSavedSequence() != 0) {
            elem.addAttribute(MailService.A_REVISION, item.getSavedSequence());
        }

        return elem;
    }

    private static void encodeTimeZoneMap(Element parent, TimeZoneMap tzmap) {
        assert(tzmap != null);
        for (Iterator iter = tzmap.tzIterator(); iter.hasNext(); ) {
            ICalTimeZone tz = (ICalTimeZone) iter.next();
            Element e = parent.addElement(MailService.E_CAL_TZ);
            e.addAttribute(MailService.A_ID, tz.getID());
            e.addAttribute(MailService.A_CAL_TZ_STDOFFSET, tz.getStandardOffset() / 60 / 1000);

            if (tz.useDaylightTime()) {
                SimpleOnset standard = tz.getStandardOnset();
                SimpleOnset daylight = tz.getDaylightOnset();
                if (standard != null && daylight != null) {
                    e.addAttribute(MailService.A_CAL_TZ_DAYOFFSET, tz.getDaylightOffset() / 60 / 1000);

                    Element std = e.addElement(MailService.E_CAL_TZ_STANDARD);
                    int standardWeek = standard.getWeek();
                    if (standardWeek != 0) {
                        std.addAttribute(MailService.A_CAL_TZ_WEEK, standardWeek);
                        std.addAttribute(MailService.A_CAL_TZ_DAYOFWEEK, standard.getDayOfWeek());
                    } else
                        std.addAttribute(MailService.A_CAL_TZ_DAYOFMONTH, standard.getDayOfMonth());
                    std.addAttribute(MailService.A_CAL_TZ_MONTH, standard.getMonth());
                    std.addAttribute(MailService.A_CAL_TZ_HOUR, standard.getHour());
                    std.addAttribute(MailService.A_CAL_TZ_MINUTE, standard.getMinute());
                    std.addAttribute(MailService.A_CAL_TZ_SECOND, standard.getSecond());

                    Element day = e.addElement(MailService.E_CAL_TZ_DAYLIGHT);
                    int daylightWeek = daylight.getWeek();
                    if (daylightWeek != 0) {
                        day.addAttribute(MailService.A_CAL_TZ_WEEK, daylightWeek);
                        day.addAttribute(MailService.A_CAL_TZ_DAYOFWEEK, daylight.getDayOfWeek());
                    } else
                        day.addAttribute(MailService.A_CAL_TZ_DAYOFMONTH, daylight.getDayOfMonth());
                    day.addAttribute(MailService.A_CAL_TZ_MONTH, daylight.getMonth());
                    day.addAttribute(MailService.A_CAL_TZ_HOUR, daylight.getHour());
                    day.addAttribute(MailService.A_CAL_TZ_MINUTE, daylight.getMinute());
                    day.addAttribute(MailService.A_CAL_TZ_SECOND, daylight.getSecond());
                }
            }
        }
    }

    public static Element encodeInvite(Element parent,
                ZimbraSoapContext lc,
                CalendarItem calItem,
                Invite invite,
                int fields,
                boolean includeReplies)
    throws ServiceException {
        boolean allFields = true;

        if (fields != NOTIFY_FIELDS) {
            allFields = false;
            if (!needToOutput(fields, Change.MODIFIED_INVITE)) {
                return parent;
            }
        }

        Element e = parent.addElement(MailService.E_INVITE_COMPONENT);

        e.addAttribute(MailService.A_CAL_COMPONENT_NUM, invite.getComponentNum());

        e.addAttribute(MailService.A_CAL_RSVP, invite.getRsvp());

        Account acct = calItem.getMailbox().getAccount();
        if (allFields) {
            try {
                e.addAttribute("x_uid", invite.getUid());
                e.addAttribute(MailService.A_CAL_SEQUENCE, invite.getSeqNo());

                String itemId = lc.formatItemId(calItem);
                e.addAttribute(MailService.A_CAL_ID, itemId);
                if (invite.isEvent())
                    e.addAttribute(MailService.A_APPT_ID_DEPRECATE_ME, itemId);  // for backward compat

                if (invite.thisAcctIsOrganizer(acct)) {
                    e.addAttribute(MailService.A_CAL_ISORG, true);
                }

                Recurrence.IRecurrence recur = invite.getRecurrence();
                if (recur != null) {
                    Element recurElt = e.addElement(MailService.E_CAL_RECUR);
                    recur.toXml(recurElt);
                }
            } catch(ServiceException ex) {
                ex.printStackTrace();
            }

            e.addAttribute(MailService.A_CAL_STATUS, invite.getStatus());

            String priority = invite.getPriority();
            if (priority != null)
                e.addAttribute(MailService.A_CAL_PRIORITY, priority);

            if (invite.isEvent()) {
                e.addAttribute(MailService.A_APPT_FREEBUSY, invite.getFreeBusy());
    
                Instance inst = Instance.fromInvite(calItem, invite);
                if (calItem instanceof Appointment) {
                    Appointment appt = (Appointment) calItem;
                    e.addAttribute(MailService.A_APPT_FREEBUSY_ACTUAL,
                                appt.getEffectiveFreeBusyActual(invite, inst));
                }
    
                e.addAttribute(MailService.A_APPT_TRANSPARENCY, invite.getTransparency());
    
            }

            if (includeReplies)
                encodeReplies(e, calItem, invite);

            if (invite.getRecurId() != null) {
                e.addAttribute(MailService.A_CAL_IS_EXCEPTION, true);
            }

            boolean allDay = invite.isAllDayEvent();
            if (allDay)
                e.addAttribute(MailService.A_CAL_ALLDAY, true);

            ParsedDateTime dtStart = invite.getStartTime();
            if (dtStart != null) {
                Element startElt = e.addElement(MailService.E_CAL_START_TIME);
                startElt.addAttribute(MailService.A_CAL_DATETIME, dtStart.getDateTimePartString(false));
                if (!allDay) {
                    String tzName = dtStart.getTZName();
                    if (tzName != null)
                        startElt.addAttribute(MailService.A_CAL_TIMEZONE, tzName);
                }
            }

            ParsedDateTime dtEnd = invite.getEndTime();
            if (dtEnd != null) {
                Element endElt = e.addElement(MailService.E_CAL_END_TIME);
                if (!allDay) {
                    String tzName = dtEnd.getTZName();
                    if (tzName != null)
                        endElt.addAttribute(MailService.A_CAL_TIMEZONE, tzName);
                } else {
                    // See CalendarUtils.parseInviteElementCommon, where we parse DTEND
                    // for a description of why we add -1d when sending to the client
                    dtEnd = dtEnd.add(ParsedDuration.NEGATIVE_ONE_DAY);
                }
                endElt.addAttribute(MailService.A_CAL_DATETIME, dtEnd.getDateTimePartString(false));
            }

            ParsedDuration dur = invite.getDuration();
            if (dur != null) {
                dur.toXml(e);
            }

            e.addAttribute(MailService.A_NAME, invite.getName());
            e.addAttribute(MailService.A_CAL_LOCATION, invite.getLocation());

            // Percent Complete (VTODO)
            if (invite.isTodo()) {
                String pct = invite.getPercentComplete();
                if (pct != null)
                    e.addAttribute(MailService.A_TASK_PERCENT_COMPLETE, pct);
                long completed = invite.getCompleted();
                if (completed != 0) {
                    ParsedDateTime c = ParsedDateTime.fromUTCTime(completed);
                    e.addAttribute(MailService.A_TASK_COMPLETED, c.getDateTimePartString());
                }
            }

            // Organizer
            if (invite.hasOrganizer()) {
                ZOrganizer org = invite.getOrganizer();
                Element orgElt = e.addUniqueElement(MailService.E_CAL_ORGANIZER);                
                String str = org.getAddress();
                orgElt.addAttribute(MailService.A_URL, str);
                if (org.hasCn())
                    orgElt.addAttribute(MailService.A_DISPLAY, org.getCn());
                if (org.hasSentBy())
                    orgElt.addAttribute(MailService.A_CAL_SENTBY, org.getSentBy());
                if (org.hasDir())
                    orgElt.addAttribute(MailService.A_CAL_DIR, org.getDir());
                if (org.hasLanguage())
                    orgElt.addAttribute(MailService.A_CAL_LANGUAGE, org.getLanguage());
            }

            // Attendee(s)
            List<ZAttendee> attendees = invite.getAttendees();
            for (ZAttendee at : attendees) {
                Element atElt = e.addElement(MailService.E_CAL_ATTENDEE);
                // address
                atElt.addAttribute(MailService.A_URL, at.getAddress());
                // CN
                if (at.hasCn())
                    atElt.addAttribute(MailService.A_DISPLAY, at.getCn());
                // SENT-BY
                if (at.hasSentBy())
                    atElt.addAttribute(MailService.A_CAL_SENTBY, at.getSentBy());
                // DIR
                if (at.hasDir())
                    atElt.addAttribute(MailService.A_CAL_DIR, at.getDir());
                // LANGUAGE
                if (at.hasLanguage())
                    atElt.addAttribute(MailService.A_CAL_LANGUAGE, at.getLanguage());
                // CUTYPE
                if (at.hasCUType())
                    atElt.addAttribute(MailService.A_CAL_CUTYPE, at.getCUType());
                // ROLE
                if (at.hasRole())
                    atElt.addAttribute(MailService.A_CAL_ROLE, at.getRole());
                // PARTSTAT
                if (at.hasPartStat())
                    atElt.addAttribute(MailService.A_CAL_PARTSTAT, at.getPartStat());
                // RSVP
                if (at.hasRsvp())
                    atElt.addAttribute(MailService.A_CAL_RSVP, at.getRsvp().booleanValue());
                // MEMBER
                if (at.hasMember())
                    atElt.addAttribute(MailService.A_CAL_MEMBER, at.getMember());
                // DELEGATED-TO
                if (at.hasDelegatedTo())
                    atElt.addAttribute(MailService.A_CAL_DELEGATED_TO, at.getDelegatedTo());
                // DELEGATED-FROM
                if (at.hasDelegatedFrom())
                    atElt.addAttribute(MailService.A_CAL_DELEGATED_FROM, at.getDelegatedFrom());
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
            Element propElem = parent.addElement(MailService.E_CAL_XPROP);
            propElem.addAttribute(MailService.A_NAME, propName);
            if (propValue != null)
                propElem.addAttribute(MailService.A_VALUE, propValue);
            for (Iterator<ZParameter> paramIter = xprop.parameterIterator(); paramIter.hasNext(); ) {
                ZParameter xparam = paramIter.next();
                String paramName = xparam.getName();
                if (paramName == null) continue;
                Element paramElem = propElem.addElement(MailService.E_CAL_XPARAM);
                paramElem.addAttribute(MailService.A_NAME, paramName);
                String paramValue = xparam.getValue();
                if (paramValue != null)
                    paramElem.addAttribute(MailService.A_VALUE, paramValue);
            }
        }
    }

    private static Element encodeInvitesForMessage(Element parent, ZimbraSoapContext lc, Message msg, int fields)
    throws ServiceException {
        if (fields != NOTIFY_FIELDS)
            if (!needToOutput(fields, Change.MODIFIED_INVITE))
                return parent;

        Element ie = parent.addElement(MailService.E_INVITE);

        boolean addedMethod = false;
        Mailbox mbox = msg.getMailbox();

        for (Iterator iter = msg.getCalendarItemInfoIterator(); iter.hasNext(); ) {
            Message.CalendarItemInfo info = (Message.CalendarItemInfo) iter.next();

            CalendarItem calItem = null;
            try {
                calItem = mbox.getCalendarItemById(lc.getOperationContext(), info.getCalendarItemId());
            } catch (MailServiceException.NoSuchItemException e) {}
            if (calItem != null) {
                Invite inv = calItem.getInvite(msg.getId(), info.getComponentNo());

                if (inv != null) {
                    if (!addedMethod) {
//                      ie.addAttribute("method", inv.getMethod());
                        addedMethod = true;
                    }
                    encodeTimeZoneMap(ie, calItem.getTimeZoneMap());
                    encodeInvite(ie, lc, calItem, inv, fields, false);
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
        Element elem = parent.addElement(MailService.E_MIMEPART);
        MimePart mp = mpi.getMimePart();

        String part = mpi.getPartName();
        part = prefix + (prefix.equals("") || part.equals("") ? "" : ".") + part;
        elem.addAttribute(MailService.A_PART, part);

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
        elem.addAttribute(MailService.A_CONTENT_TYPE, ctype);

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
            if (fname == null && Mime.CT_MESSAGE_RFC822.equals(ctype)) {
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
        if (bodies != null && bodies.contains(mpi)) {
            elem.addAttribute(MailService.A_BODY, true);
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
     *  The content is added as the value of a new <code>&lt;content></code>
     *  sub-element.  <i>Note: This method will only extract the content of a
     *  <b>text/*</b> or <b>xml/*</b> message part.</i> 
     * @param elt  The element to add the <code>&lt;content></code> to.
     * @param mpi  The message part to extract the content from.
     * @throws MessagingException when message parsing or CTE-decoding fails
     * @throws IOException on error during parsing or defanging */
    private static void addContent(Element elt, MPartInfo mpi) throws IOException, MessagingException {
        addContent(elt, mpi, true);
    }

    /** Adds the decoded text content of a message part to the {@link Element}.
     *  The content is added as the value of a new <code>&lt;content></code>
     *  sub-element.  <i>Note: This method will only extract the content of a
     *  <b>text/*</b> or <b>xml/*</b> message part.</i> 
     * @param elt     The element to add the <code>&lt;content></code> to.
     * @param mpi     The message part to extract the content from.
     * @param neuter  Whether to "neuter" image <code>src</code> attributes.
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
            elt.addAttribute(MailService.E_CONTENT, data, Element.DISP_CONTENT);                
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
        Element elem = parent.addElement(MailService.E_EMAIL);
        elem.addAttribute(MailService.A_ADDRESS, pa.emailPart);
        elem.addAttribute(MailService.A_DISPLAY, pa.firstName);
        elem.addAttribute(MailService.A_PERSONAL, pa.personalPart);
        elem.addAttribute(MailService.A_ADDRESS_TYPE, type.toString());
        return elem;
    }


    public static Element encodeWiki(Element parent, ZimbraSoapContext lc, WikiItem wiki, int rev) {
        return encodeWiki(parent, lc, wiki, NOTIFY_FIELDS, rev);
    }
    public static Element encodeWiki(Element parent, ZimbraSoapContext lc, WikiItem wiki, int fields, int rev) {
        Element m = parent.addElement(MailService.E_WIKIWORD);
        encodeDocumentCommon(m, lc, wiki, fields, rev);
        return m;
    }

    public static Element encodeDocument(Element parent, ZimbraSoapContext lc, Document doc, int rev) {
        return encodeDocument(parent, lc, doc, NOTIFY_FIELDS, rev);
    }

    public static Element encodeDocument(Element parent, ZimbraSoapContext lc, Document doc, int fields, int rev) {
        Element m = parent.addElement(MailService.E_DOC);
        encodeDocumentCommon(m, lc, doc, fields, rev);
        m.addAttribute(MailService.A_CONTENT_TYPE, doc.getContentType());
        return m;
    }

    public static Element encodeDocumentCommon(Element m, ZimbraSoapContext lc, Document doc, int fields, int rev) {
        m.addAttribute(MailService.A_ID, lc.formatItemId(doc));
        if (needToOutput(fields, Change.MODIFIED_NAME)) {
        	m.addAttribute(MailService.A_NAME, doc.getName());
        	encodeRestUrl(m, doc);
        }
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            m.addAttribute(MailService.A_SIZE, doc.getSize());
        if (needToOutput(fields, Change.MODIFIED_DATE))
            m.addAttribute(MailService.A_DATE, doc.getDate());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            m.addAttribute(MailService.A_FOLDER, new ItemId(doc.getMailbox().getAccountId(), doc.getFolderId()).toString(lc));
        recordItemTags(m, doc, fields);

        if (needToOutput(fields, Change.MODIFIED_CONTENT)) {

            try {
                Document.DocumentRevision revision = doc.getRevision(1);
                m.addAttribute(MailService.A_CREATOR, revision.getCreator());
                m.addAttribute(MailService.A_CREATED_DATE, revision.getRevDate());

                revision = (rev > 0) ? doc.getRevision(rev) : doc.getLastRevision(); 
                m.addAttribute(MailService.A_VERSION, revision.getVersion());
                m.addAttribute(MailService.A_LAST_EDITED_BY, revision.getCreator());
                m.addAttribute(MailService.A_MODIFIED_DATE, revision.getRevDate());
                String frag = revision.getFragment();
                if (frag != null && !frag.equals(""))
                    m.addAttribute(MailService.E_FRAG, frag, Element.DISP_CONTENT);
            } catch (Exception ex) {
                mLog.warn("ignoring exception while fetching revision for document " + doc.getSubject(), ex);
            }
        }

        return m;
    }

    public static Element encodeWikiPage(Element parent, WikiPage page) {
        Element m = parent.addElement(MailService.E_WIKIWORD);
        m.addAttribute(MailService.A_NAME, page.getWikiWord());
        m.addAttribute(MailService.A_ID, page.getId());
        m.addAttribute(MailService.A_REST_URL, page.getRestUrl());
        //m.addAttribute(MailService.A_SIZE, page.getSize());
        m.addAttribute(MailService.A_DATE, page.getModifiedDate());
        m.addAttribute(MailService.A_FOLDER, page.getFolderId());
        m.addAttribute(MailService.A_VERSION, page.getLastRevision());
        m.addAttribute(MailService.A_CREATOR, page.getCreator());
        m.addAttribute(MailService.A_CREATED_DATE, page.getCreatedDate());
        m.addAttribute(MailService.A_LAST_EDITED_BY, page.getLastEditor());
        m.addAttribute(MailService.A_MODIFIED_DATE, page.getModifiedDate());
        String frag = page.getFragment();
        if (frag != null && !frag.equals(""))
            m.addAttribute(MailService.E_FRAG, frag, Element.DISP_CONTENT);
        return m;
    }

    public static Element encodeDataSource(Element parent, DataSource ds) {
        Element m = parent.addElement(MailService.E_DS_POP3);
        m.addAttribute(MailService.A_ID, ds.getId());
        m.addAttribute(MailService.A_NAME, ds.getName());
        m.addAttribute(MailService.A_FOLDER, ds.getFolderId());
        m.addAttribute(MailService.A_DS_IS_ENABLED, ds.isEnabled());
        m.addAttribute(MailService.A_DS_LEAVE_ON_SERVER, ds.leaveOnServer());
        
        if (ds.getHost() != null) { 
            m.addAttribute(MailService.A_DS_HOST, ds.getHost());
        }
        if (ds.getPort() != null) {
            m.addAttribute(MailService.A_DS_PORT, ds.getPort());
        }
        if (ds.getConnectionType() != null) {
            m.addAttribute(MailService.A_DS_CONNECTION_TYPE, ds.getConnectionType());
        }
        if (ds.getUsername() != null) {
            m.addAttribute(MailService.A_DS_USERNAME, ds.getUsername());
        }
        return m;
    }
}
