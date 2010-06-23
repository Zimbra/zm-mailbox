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
 * Created on 2004. 9. 15.
 */
package com.zimbra.cs.service.mail;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.TruncatingWriter;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.html.HtmlDefang;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.SearchParams.ExpandResults;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.CalendarItem.AlarmData;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.mailbox.Contact.Attachment;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone.SimpleOnset;
import com.zimbra.cs.mailbox.calendar.Recurrence.IRecurrence;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mailbox.calendar.Alarm;
import com.zimbra.cs.mailbox.calendar.Geo;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.RecurId;
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

import javax.mail.MessagingException;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;

import java.io.*;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;

/**
 * Class containing static methods for encoding various MailItem-derived
 * objects into XML.
 */
public class ToXML {

    // we usually don't want to return last modified date...
    public static final int NOTIFY_FIELDS = Change.ALL_FIELDS & ~Change.MODIFIED_CONFLICT;

    private static Log mLog = LogFactory.getLog(ToXML.class);

    // no construction
    private ToXML()  {}

    public static Element encodeItem(Element parent, ItemIdFormatter ifmt, OperationContext octxt, MailItem item, int fields)
    throws ServiceException {
        if (item instanceof Folder) {
            return encodeFolder(parent, ifmt, octxt, (Folder) item, fields);
        } else if (item instanceof Tag) {
            return encodeTag(parent, ifmt, (Tag) item, fields);
        } else if (item instanceof Note) {
            return encodeNote(parent, ifmt, (Note) item, fields);
        } else if (item instanceof Contact) {
            return encodeContact(parent, ifmt, (Contact) item, false, null, fields);
        } else if (item instanceof CalendarItem) { 
            return encodeCalendarItemSummary(parent, ifmt, octxt, (CalendarItem) item, fields, true);
        } else if (item instanceof Conversation) {
            return encodeConversationSummary(parent, ifmt, octxt, (Conversation) item, null, OutputParticipants.PUT_SENDERS, fields, false);
        } else if (item instanceof WikiItem) {
            return encodeWiki(parent, ifmt, octxt, (WikiItem) item, fields);
        } else if (item instanceof Document) {
            return encodeDocument(parent, ifmt, octxt, (Document) item, fields);
        } else if (item instanceof Message) {
            OutputParticipants output = (fields == NOTIFY_FIELDS ? OutputParticipants.PUT_BOTH : OutputParticipants.PUT_SENDERS);
            return encodeMessageSummary(parent, ifmt, octxt, (Message) item, output, fields);
        } else {
            return null;
        }
    }

    private static boolean needToOutput(int fields, int fieldMask) {
        return ((fields & fieldMask) > 0);
    }

    public static Element encodeMailbox(Element parent, OperationContext octxt, Mailbox mbox) {
        return encodeMailbox(parent, octxt, mbox, NOTIFY_FIELDS);
    }

    public static Element encodeMailbox(Element parent, OperationContext octxt, Mailbox mbox, int fields) {
        Element elem = parent.addElement(MailConstants.E_MAILBOX);
        if (octxt.isDelegatedRequest(mbox))
            elem.addAttribute(HeaderConstants.A_ACCOUNT_ID, mbox.getAccountId());
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            elem.addAttribute(MailConstants.A_SIZE, mbox.getSize());
        return elem;
    }
    
    public static Element encodeFolder(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Folder folder) {
        return encodeFolder(parent, ifmt, octxt, folder, NOTIFY_FIELDS);
    }
    
    public static Element encodeFolder(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Folder folder, int fields) {
        return encodeFolder(parent, ifmt, octxt, folder, fields, false);
    }

    public static Element encodeFolder(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Folder folder, int fields, boolean exposeAclAccessKey) {
        if (folder instanceof SearchFolder)
            return encodeSearchFolder(parent, ifmt, (SearchFolder) folder, fields);
        else if (folder instanceof Mountpoint)
            return encodeMountpoint(parent, ifmt, (Mountpoint) folder, fields);

        Element elem = parent.addElement(MailConstants.E_FOLDER);
        encodeFolderCommon(elem, ifmt, folder, fields);
        if (needToOutput(fields, Change.MODIFIED_SIZE)) {
            elem.addAttribute(MailConstants.A_NUM, folder.getItemCount());
            elem.addAttribute(MailConstants.A_SIZE, folder.getTotalSize());
            elem.addAttribute(MailConstants.A_IMAP_MODSEQ, folder.getImapMODSEQ());
            elem.addAttribute(MailConstants.A_IMAP_UIDNEXT, folder.getImapUIDNEXT());
        }

        if (needToOutput(fields, Change.MODIFIED_URL)) {
            String url = folder.getUrl();
            if (!url.equals("") || fields != NOTIFY_FIELDS)
                elem.addAttribute(MailConstants.A_URL, HttpUtil.sanitizeURL(url));
        }

        Mailbox mbox = folder.getMailbox();
        boolean remote = octxt != null && octxt.isDelegatedRequest(mbox);
        boolean canAdminister = !remote;
        if (remote) {
            // return effective permissions only for remote folders
            String perms = encodeEffectivePermissions(folder, octxt);
            elem.addAttribute(MailConstants.A_RIGHTS, perms);
            canAdminister = perms != null && perms.indexOf(ACL.ABBR_ADMIN) != -1;
        }

        if (canAdminister) {
            // return full ACLs for folders we have admin rights on
            if (needToOutput(fields, Change.MODIFIED_ACL)) {
                if (fields != NOTIFY_FIELDS || folder.isTagged(Flag.ID_FLAG_NO_INHERIT))
                    encodeACL(octxt, elem, folder.getEffectiveACL(), exposeAclAccessKey);
            }
        }
        return elem;
    }

    public static String encodeEffectivePermissions(Folder folder, OperationContext octxt) {
        try {
            short perms = folder.getMailbox().getEffectivePermissions(octxt, folder.getId(), MailItem.TYPE_FOLDER);
            return ACL.rightsToString(perms);
        } catch (ServiceException e) {
            mLog.warn("ignoring exception while fetching effective permissions for folder " + folder.getId(), e);
            return null;
        }
    }

    // encode mailbox ACL
    public static Element encodeACL(OperationContext octxt, Element parent, ACL acl, boolean exposeAclAccessKey) {
        Element eACL = parent.addUniqueElement(MailConstants.E_ACL);
        if (acl == null)
            return eACL;

        boolean needDispName = OperationContextData.getNeedGranteeName(octxt);
        
        for (ACL.Grant grant : acl.getGrants()) {
            String name = null;
            byte granteeType = grant.getGranteeType();
            
            if (needDispName) {
                //
                // Get name of the grantee
                //
    
                // 1. try getting the name from the Grant, the name is set on the Grant 
                //    if we are in the path of proxying sharing in ZD
                name = grant.getGranteeName();
                if (name == null) {
                    // 2. (for bug 35079), see if the name is already resolved in the in the OperationContextData
                    OperationContextData.GranteeNames granteeNames = OperationContextData.getGranteeNames(octxt);
                    if (granteeNames != null)
                        name = granteeNames.getNameById(grant.getGranteeId(), granteeType);
                    
                    // 3. fallback to the old way to lookup the name using the Provisioning interface, 
                    //    this *may* lead to a LDAP search if the id is not in cache
                    if (name == null) {
                        NamedEntry nentry = FolderAction.lookupGranteeByZimbraId(grant.getGranteeId(), granteeType);
                        if (nentry != null)
                            name = nentry.getName();
                    }
                }
            }
            
            Element eGrant = eACL.addElement(MailConstants.E_GRANT);
            eGrant.addAttribute(MailConstants.A_ZIMBRA_ID, grant.getGranteeId())
                  .addAttribute(MailConstants.A_GRANT_TYPE, ACL.typeToString(granteeType))
                  .addAttribute(MailConstants.A_RIGHTS, ACL.rightsToString(grant.getGrantedRights()));
            
            if (needDispName) {
                // Note: use == instead of equals, the if returns true if and only if INVALID_GRANT and name 
                // refer to the same object 
                if (OperationContextData.GranteeNames.INVALID_GRANT == name) {
                    eGrant.addAttribute(MailConstants.A_INVALID, true);
                    eGrant.addAttribute(MailConstants.A_DISPLAY, OperationContextData.GranteeNames.EMPTY_NAME);
                } else {
                    eGrant.addAttribute(MailConstants.A_DISPLAY, name);
                }
            }
            
            if (granteeType == ACL.GRANTEE_KEY) {
                if (exposeAclAccessKey)
                    eGrant.addAttribute(MailConstants.A_ACCESSKEY, grant.getPassword());
            } else
                eGrant.addAttribute(MailConstants.A_PASSWORD, grant.getPassword());
        }
        return eACL;
    }
    
    // encode account ACE
    public static Element encodeACE(Element parent, ZimbraACE ace) {
        Element eACE = parent.addElement(MailConstants.E_ACE)
                .addAttribute(MailConstants.A_ZIMBRA_ID, ace.getGrantee())
                .addAttribute(MailConstants.A_GRANT_TYPE, ace.getGranteeType().getCode())
                .addAttribute(MailConstants.A_RIGHT, ace.getRight().getName())
                .addAttribute(MailConstants.A_DISPLAY, ace.getGranteeDisplayName());
        
        if (ace.getGranteeType() == GranteeType.GT_KEY)
            eACE.addAttribute(MailConstants.A_ACCESSKEY, ace.getSecret());
        else if (ace.getGranteeType() == GranteeType.GT_GUEST)
            eACE.addAttribute(MailConstants.A_PASSWORD, ace.getSecret());

        if (ace.deny())
            eACE.addAttribute(MailConstants.A_DENY, ace.deny());
       
        return eACE;
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
        }

        if (needToOutput(fields, Change.MODIFIED_FLAGS)) {
            String flags = folder.getMailbox().getItemFlagString(folder); //so that offline override can filter flags
            if (fields != NOTIFY_FIELDS || !flags.equals(""))
                elem.addAttribute(MailConstants.A_FLAGS, flags);
        }
        if (needToOutput(fields, Change.MODIFIED_COLOR)) {
			MailItem.Color color = folder.getRgbColor();
			if (color.hasMapping()) {
				byte mappedColor = color.getMappedColor();
				if (mappedColor != MailItem.DEFAULT_COLOR || fields != NOTIFY_FIELDS) {
					elem.addAttribute(MailConstants.A_COLOR, color.getMappedColor());
				}
			}
			if (color.getValue() != 0) {
				elem.addAttribute(MailConstants.A_RGB, color.toString());
			}
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
            // this attribute ("ms") *normally* goes with MODIFIED_CONFLICT, but we need it
            //   serialized in this case as well in order to make dav ctag caching work
            elem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, folder.getModifiedSequence());
        }
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            elem.addAttribute(MailConstants.A_CHANGE_DATE, folder.getChangeDate() / 1000);
            elem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, folder.getModifiedSequence());
        }
        if (needToOutput(fields, Change.MODIFIED_METADATA)) {
            encodeAllCustomMetadata(elem, folder, fields);
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

    public static Element encodeRestUrl(Element elem, MailItem item) {
        try {
            return elem.addAttribute(MailConstants.A_REST_URL, UserServlet.getRestUrl(item));
        } catch (ServiceException se) {
            mLog.error("cannot generate REST url", se);
            return elem;
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

    public static void encodeAllCustomMetadata(Element elem, MailItem item, int fields) {
        List<String> sections = item.getCustomDataSections();
        if (sections == null || sections.isEmpty()) {
            // if we care specifically about custom metadata and there is none, let the caller know
            if (fields != NOTIFY_FIELDS)
                elem.addElement(MailConstants.E_METADATA);
            return;
        }

        for (String section : sections) {
            try {
                encodeCustomMetadata(elem, item.getCustomData(section));
            } catch (ServiceException e) {
                ZimbraLog.soap.warn("could not deserialize custom metadata; skipping (section '" + section + "', item " + new ItemId(item) + ")");
            }
        }
    }

    public static Element encodeCustomMetadata(Element elem, CustomMetadata custom) {
        if (custom == null)
            return null;

        Element serialized = elem.addElement(MailConstants.E_METADATA);
        serialized.addAttribute(MailConstants.A_SECTION, custom.getSectionKey());
        for (Map.Entry<String, String> entry : custom.entrySet())
            serialized.addKeyValuePair(entry.getKey(), entry.getValue());
        return serialized;
    }

    public static Element encodeContact(Element parent, ItemIdFormatter ifmt, Contact contact, boolean summary, Collection<String> attrFilter) {
        return encodeContact(parent, ifmt, contact, summary, attrFilter, NOTIFY_FIELDS);
    }

    public static Element encodeContact(Element parent, ItemIdFormatter ifmt, Contact contact, boolean summary, Collection<String> attrFilter, int fields) {
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
        if (needToOutput(fields, Change.MODIFIED_METADATA))
            encodeAllCustomMetadata(elem, contact, fields);

        if (!needToOutput(fields, Change.MODIFIED_CONTENT)) {
            if (summary) {
                try {
                    elem.addAttribute(MailConstants.A_FILE_AS_STR, contact.getFileAsString());
                } catch (ServiceException e) { }
    
                elem.addAttribute(ContactConstants.A_email, contact.get(ContactConstants.A_email));
                elem.addAttribute(ContactConstants.A_email2, contact.get(ContactConstants.A_email2));
                elem.addAttribute(ContactConstants.A_email3, contact.get(ContactConstants.A_email3));

                String type = contact.get(ContactConstants.A_type);
                String dlist = contact.get(ContactConstants.A_dlist);
                if (type == null && dlist != null)
                    type = ContactConstants.TYPE_GROUP;
                elem.addAttribute(ContactConstants.A_type, type);
                if (dlist != null)
                    elem.addAttribute(ContactConstants.A_dlist, dlist);
                
                // send back date with summary via search results
                elem.addAttribute(MailConstants.A_CHANGE_DATE, contact.getChangeDate() / 1000);
            }

            // stop here if we're not returning the actual contact content
            return elem;
        }

        try {
            elem.addAttribute(MailConstants.A_FILE_AS_STR, contact.getFileAsString());
        } catch (ServiceException e) { }

        List<Attachment> attachments = contact.getAttachments();
        if (attrFilter != null) {
            for (String name : attrFilter) {
                // XXX: How to distinguish between a non-existent attribute and
                //      an existing attribute with null or empty string value?
                String value = contact.get(name);
                if (value != null && !value.equals("")) {
                    encodeContactAttr(elem, name, value);
                } else if (attachments != null) {
                    for (Attachment attach : attachments) {
                        if (attach.getName().equals(name))
                            encodeContactAttachment(elem, attach);
                    }
                }
            }
        } else {
            for (Map.Entry<String, String> me : contact.getFields().entrySet()) {
                String name = me.getKey();
                String value = me.getValue();
                if (name != null && !name.trim().equals("") && value != null && !value.equals("")) {
                    encodeContactAttr(elem, name, value);
                }
            }
            if (attachments != null) {
                for (Attachment attach : attachments)
                    encodeContactAttachment(elem, attach);
            }
        }

        return elem;
    }

    private static void encodeContactAttr(Element elem, String name, String value) {
        if (Contact.isMultiValueAttr(value)) {
            try {
                for (String v : Contact.parseMultiValueAttr(value))
                    elem.addKeyValuePair(name, v);
                return;
            } catch (JSONException e) {}
        }
        elem.addKeyValuePair(name, value);
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
        if (needToOutput(fields, Change.MODIFIED_COLOR)) {
            elem.addAttribute(MailConstants.A_RGB, note.getRgbColor().toString());
            elem.addAttribute(MailConstants.A_COLOR, note.getColor());
        }
        if (needToOutput(fields, Change.MODIFIED_CONTENT))
            elem.addAttribute(MailConstants.E_CONTENT, note.getText(), Element.Disposition.CONTENT);
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            elem.addAttribute(MailConstants.A_CHANGE_DATE, note.getChangeDate() / 1000);
            elem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, note.getModifiedSequence());
        }
        if (needToOutput(fields, Change.MODIFIED_METADATA))
            encodeAllCustomMetadata(elem, note, fields);
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
        if (needToOutput(fields, Change.MODIFIED_COLOR)) {
            elem.addAttribute(MailConstants.A_RGB, tag.getRgbColor().toString());
            elem.addAttribute(MailConstants.A_COLOR, tag.getColor());
        }
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
        if (needToOutput(fields, Change.MODIFIED_METADATA))
            encodeAllCustomMetadata(elem, tag, fields);
        return elem;
    }


    public static Element encodeConversation(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Conversation conv, SearchParams params) throws ServiceException {
        Mailbox mbox = conv.getMailbox();
        List<Message> msgs = mbox.getMessagesByConversation(octxt, conv.getId(), SortBy.DATE_ASCENDING);
        return encodeConversation(parent, ifmt, octxt, conv, msgs, params);
    }

    public static Element encodeConversation(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Conversation conv, List<Message> msgs, SearchParams params) throws ServiceException {
        int fields = NOTIFY_FIELDS;
        Element c = encodeConversationCommon(parent, ifmt, conv, msgs, fields);
        if (msgs.isEmpty())
            return c;

        c.addAttribute(MailConstants.E_SUBJECT, msgs.get(0).getSubject(), Element.Disposition.CONTENT);

        ExpandResults expand = params.getInlineRule();
        for (Message msg : msgs) {
            if (msg.isTagged(Flag.ID_FLAG_DELETED))
                continue;
            if (expand == ExpandResults.FIRST || expand == ExpandResults.ALL || expand.matches(msg)) {
                encodeMessageAsMP(c, ifmt, octxt, msg, null, params.getMaxInlinedLength(), params.getWantHtml(), params.getNeuterImages(), params.getInlinedHeaders(), true);
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
        return encodeConversationSummary(parent, ifmt, octxt, conv, null, OutputParticipants.PUT_SENDERS, fields, true);
    }

    public static Element encodeConversationSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Conversation conv, Message msgHit, OutputParticipants output) throws ServiceException {
        return encodeConversationSummary(parent, ifmt, octxt, conv, msgHit, output, NOTIFY_FIELDS, true);
    }

    private static Element encodeConversationSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Conversation conv, Message msgHit, OutputParticipants output, int fields, boolean alwaysSerialize)
    throws ServiceException {
        boolean addRecips  = msgHit != null && msgHit.isFromMe() && (output == OutputParticipants.PUT_RECIPIENTS || output == OutputParticipants.PUT_BOTH);
        boolean addSenders = (output == OutputParticipants.PUT_BOTH || !addRecips) && needToOutput(fields, Change.MODIFIED_SENDERS);

        Mailbox mbox = conv.getMailbox();
        // if the caller might not be able to see all the messages (due to rights or \Deleted),
        //   need to calculate some fields from the visible messages
        List<Message> msgs = null;
        if ((octxt != null && octxt.isDelegatedRequest(mbox)) || conv.isTagged(Flag.ID_FLAG_DELETED))
            msgs = mbox.getMessagesByConversation(octxt, conv.getId(), SortBy.DATE_ASCENDING);

        boolean noneVisible = msgs != null && msgs.isEmpty();
        Element c = noneVisible && !alwaysSerialize ? null : encodeConversationCommon(parent, ifmt, conv, msgs, fields);
        if (noneVisible)
            return c;

        if (needToOutput(fields, Change.MODIFIED_DATE))
            c.addAttribute(MailConstants.A_DATE, msgHit != null ? msgHit.getDate() : conv.getDate());
        if (needToOutput(fields, Change.MODIFIED_SUBJECT))
            c.addAttribute(MailConstants.E_SUBJECT, msgHit != null ? msgHit.getSubject() : conv.getSubject(), Element.Disposition.CONTENT);
        if (fields == NOTIFY_FIELDS && msgHit != null)
            c.addAttribute(MailConstants.E_FRAG, msgHit.getFragment(), Element.Disposition.CONTENT);

        if (addRecips && msgHit != null)
            addEmails(c, Mime.parseAddressHeader(msgHit.getRecipients()), EmailType.TO);

        if (addSenders) {
            SenderList sl;
            try {
                if (msgs != null) {
                    sl = new SenderList();
                    for (Message msg : msgs) {
                        if (!msg.isTagged(Flag.ID_FLAG_DELETED))
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

        if (needToOutput(fields, Change.MODIFIED_METADATA))
            encodeAllCustomMetadata(c, conv, fields);

        if (needToOutput(fields, Change.MODIFIED_CHILDREN | Change.MODIFIED_SIZE)) {
            int count = conv.getMessageCount(), nondeleted = count;
            if (msgs != null) {
                count = nondeleted = msgs.size();
                for (Message msg : msgs) {
                    if (msg.isTagged(Flag.ID_FLAG_DELETED))
                        nondeleted--;
                }
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
                if (!msg.isTagged(Flag.ID_FLAG_DELETED)) {
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
     * @param part    If non-null, serialize this message/rfc822 subpart of
     *                the Message instead of the Message itself.
     * @param maxSize TODO
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
                                            int maxSize, boolean wantHTML, boolean neuter, Set<String> headers, boolean serializeType)
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
            // read-receipts only get sent by the mailbox's owner
            if (!(octxt.isDelegatedRequest(msg.getMailbox()) && octxt.isOnBehalfOfRequest(msg.getMailbox())))
                addEmails(m, Mime.parseAddressHeader(mm, "Disposition-Notification-To"), EmailType.READ_RECEIPT);

            String calIntendedFor = msg.getCalendarIntendedFor();
            m.addAttribute(MailConstants.A_CAL_INTENDED_FOR, calIntendedFor);

            String subject = Mime.getSubject(mm);
            if (subject != null)
                m.addAttribute(MailConstants.E_SUBJECT, StringUtil.stripControlCharacters(subject), Element.Disposition.CONTENT);

            String fragment = msg.getFragment();
            if (fragment != null && !fragment.equals(""))
                m.addAttribute(MailConstants.E_FRAG, fragment, Element.Disposition.CONTENT);

            String messageID = mm.getMessageID();
            if (messageID != null && !messageID.trim().equals(""))
                m.addAttribute(MailConstants.E_MSG_ID_HDR, StringUtil.stripControlCharacters(messageID), Element.Disposition.CONTENT);

            if (wholeMessage && msg.isDraft()) {
                if (!msg.getDraftOrigId().equals(""))
                    m.addAttribute(MailConstants.A_ORIG_ID, ifmt.formatItemId(new ItemId(msg.getDraftOrigId(), msg.getMailbox().getAccountId())));
                if (!msg.getDraftReplyType().equals(""))
                    m.addAttribute(MailConstants.A_REPLY_TYPE, msg.getDraftReplyType());
                if (!msg.getDraftIdentityId().equals(""))
                    m.addAttribute(MailConstants.A_IDENTITY_ID, msg.getDraftIdentityId());
                if (!msg.getDraftAccountId().equals(""))
                    m.addAttribute(MailConstants.A_FOR_ACCOUNT, msg.getDraftAccountId());
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
                encodeInvitesForMessage(m, ifmt, octxt, msg, NOTIFY_FIELDS, neuter);

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
                addParts(m, parts.get(0), bodies, part, maxSize, neuter, false, getDefaultCharset(msg));
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
    public static void setCalendarItemFields(Element calItemElem, ItemIdFormatter ifmt,
                                             OperationContext octxt, CalendarItem calItem,
                                             int fields, boolean encodeInvites, boolean includeContent,
                                             boolean neuter)
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
            boolean isPublic = true;
            for (int i = 0; i < calItem.numInvites(); i++) {
                Invite inv = calItem.getInvite(i);
                encodeInvite(calItemElem, ifmt, octxt, calItem, calItem.getInvite(i), includeContent, neuter);
                if (!inv.isPublic())
                    isPublic = false;
            }
            if (isPublic || allowPrivateAccess(octxt, calItem))
                encodeCalendarReplies(calItemElem, calItem);

            encodeAlarmTimes(calItemElem, calItem);
        }
    }

    public static void setCalendarItemFields(Element calItemElem, ItemIdFormatter ifmt,
                                             OperationContext octxt, CalendarItem calItem,
                                             int fields, boolean encodeInvites, boolean neuter)
    throws ServiceException {
        setCalendarItemFields(calItemElem, ifmt, octxt, calItem, fields, encodeInvites, false, neuter);
    }

    public static Element encodeInvite(Element parent, ItemIdFormatter ifmt,
                                       OperationContext octxt, CalendarItem cal, Invite inv, boolean neuter)
    throws ServiceException {
        return encodeInvite(parent, ifmt, octxt, cal, inv, false, neuter);
    }

    public static Element encodeInvite(Element parent, ItemIdFormatter ifmt,
                                       OperationContext octxt, CalendarItem cal, Invite inv,
                                       boolean includeContent, boolean neuter)
    throws ServiceException {
        Element ie = parent.addElement(MailConstants.E_INVITE);
        setCalendarItemType(ie, cal.getType());
        encodeTimeZoneMap(ie, cal.getTimeZoneMap());

        //encodeAlarmTimes(ie, cal);

        ie.addAttribute(MailConstants.A_CAL_SEQUENCE, inv.getSeqNo());
        ie.addAttribute(MailConstants.A_ID, ifmt.formatItemId(inv.getMailItemId()));
        ie.addAttribute(MailConstants.A_CAL_COMPONENT_NUM, inv.getComponentNum());
        if (inv.hasRecurId())
            ie.addAttribute(MailConstants.A_CAL_RECURRENCE_ID, inv.getRecurId().toString());

        encodeInviteComponent(ie, ifmt, octxt, cal, inv, NOTIFY_FIELDS, neuter);

        if (includeContent && (inv.isPublic() || allowPrivateAccess(octxt, cal))) {
            int invId = inv.getMailItemId();
            MimeMessage mm = cal.getSubpartMessage(invId);
            if (mm != null) {
                List<MPartInfo> parts;
                try {
                    parts = Mime.getParts(mm);
                } catch (IOException ex) {
                    throw ServiceException.FAILURE(ex.getMessage(), ex);
                } catch (MessagingException ex) {
                    throw ServiceException.FAILURE(ex.getMessage(), ex);
                }
                if (parts != null && !parts.isEmpty())
                    addParts(ie, parts.get(0), null, "", -1, false, true, getDefaultCharset(cal));
            }
        }

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
    public static Element encodeCalendarItemSummary(Element parent, ItemIdFormatter ifmt,
                                                    OperationContext octxt, CalendarItem calItem,
                                                    int fields, boolean includeInvites, boolean includeContent)
    throws ServiceException {
        Element elem;
        if (calItem instanceof Appointment)
            elem = parent.addElement(MailConstants.E_APPOINTMENT);
        else
            elem = parent.addElement(MailConstants.E_TASK);
        
        setCalendarItemFields(elem, ifmt, octxt, calItem, fields, includeInvites, includeContent, true);

        if (needToOutput(fields, Change.MODIFIED_METADATA))
            encodeAllCustomMetadata(elem, calItem, fields);
        return elem;
    }

    public static Element encodeCalendarItemSummary(Element parent, ItemIdFormatter ifmt,
                                                    OperationContext octxt, CalendarItem calItem,
                                                    int fields, boolean includeInvites)
    throws ServiceException {
        return encodeCalendarItemSummary(parent, ifmt, octxt, calItem, fields, includeInvites, false);
    }

    public static void encodeCalendarReplies(Element parent, CalendarItem calItem, Invite inv, String recurIdZ) {
        List<CalendarItem.ReplyInfo> replies = calItem.getReplyInfo(inv, recurIdZ);
        encodeCalendarReplies(parent, calItem, replies);
    }

    public static void encodeCalendarReplies(Element parent, CalendarItem calItem) {
        List<CalendarItem.ReplyInfo> replies = calItem.getAllReplies();
        encodeCalendarReplies(parent, calItem, replies);
    }

    private static void encodeCalendarReplies(Element parent, CalendarItem calItem, List<CalendarItem.ReplyInfo> replies) {
        Element repliesElt = parent.addElement(MailConstants.E_CAL_REPLIES);
        for (CalendarItem.ReplyInfo repInfo : replies) {
            Element curElt = repliesElt.addElement(MailConstants.E_CAL_REPLY);
            curElt.addAttribute(MailConstants.A_SEQ, repInfo.getSeq()); //zdsync
            curElt.addAttribute(MailConstants.A_DATE, repInfo.getDtStamp());
            ZAttendee attendee = repInfo.getAttendee();
            curElt.addAttribute(MailConstants.A_CAL_ATTENDEE, attendee.getAddress());
            if (attendee.hasSentBy())
                curElt.addAttribute(MailConstants.A_CAL_SENTBY, attendee.getSentBy());
            if (attendee.hasPartStat())
                curElt.addAttribute(MailConstants.A_CAL_PARTSTAT, attendee.getPartStat());
            RecurId rid = repInfo.getRecurId();
            if (rid != null)
                rid.toXml(curElt);
        }
    }


    private static String getDefaultCharset(MailItem item) throws ServiceException {
        Account acct = (item == null ? null : item.getAccount());
        return acct == null ? null : acct.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, null);
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
     * @param maxSize The maximum amount of content to inline (<=0 is unlimited).
     * @param wantHTML  <tt>true</tt> to prefer HTML parts as the "body",
     *                  <tt>false</tt> to prefer text/plain parts.
     * @param neuter  Whether to rename "src" attributes on HTML <img> tags.
     * @param headers Extra message headers to include in the returned element.
     * @param serializeType If <tt>false</tt>, always serializes as an
     *                      <tt>&lt;m></tt> element.
     * @return The newly-created <tt>&lt;m></tt> Element, which has already
     *         been added as a child to the passed-in <tt>parent</tt>.
     * @throws ServiceException */
    public static Element encodeInviteAsMP(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
                                           CalendarItem calItem, String recurIdZ, ItemId iid, String part,
                                           int maxSize, boolean wantHTML, boolean neuter,
                                           Set<String> headers, boolean serializeType)
    throws ServiceException {
        int invId = iid.getSubpartId();
        Invite[] invites = calItem.getInvites(invId);
        boolean isPublic = calItem.isPublic();
        boolean showAll = isPublic || allowPrivateAccess(octxt, calItem);

        boolean wholeMessage = (part == null || part.trim().equals(""));

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
            if (mm != null) {
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
    
                if (showAll) {
                    addEmails(m, Mime.parseAddressHeader(mm, "From"), EmailType.FROM);
                    addEmails(m, Mime.parseAddressHeader(mm, "Sender"), EmailType.SENDER);
                    addEmails(m, Mime.parseAddressHeader(mm, "Reply-To"), EmailType.REPLY_TO);
                    addEmails(m, Mime.parseAddressHeader(mm, "To"), EmailType.TO);
                    addEmails(m, Mime.parseAddressHeader(mm, "Cc"), EmailType.CC);
                    addEmails(m, Mime.parseAddressHeader(mm, "Bcc"), EmailType.BCC);
        
                    String subject = Mime.getSubject(mm);
                    if (subject != null)
                        m.addAttribute(MailConstants.E_SUBJECT, StringUtil.stripControlCharacters(subject), Element.Disposition.CONTENT);
                    String messageID = mm.getMessageID();
                    if (messageID != null && !messageID.trim().equals(""))
                        m.addAttribute(MailConstants.E_MSG_ID_HDR, StringUtil.stripControlCharacters(messageID), Element.Disposition.CONTENT);
        
                    if (!wholeMessage)
                        m.addAttribute(MailConstants.A_SIZE, mm.getSize());
        
                    java.util.Date sent = mm.getSentDate();
                    if (sent != null)
                        m.addAttribute(MailConstants.A_SENT_DATE, sent.getTime());
                }
            }

            Element invElt = m.addElement(MailConstants.E_INVITE);
            setCalendarItemType(invElt, calItem.getType());
            encodeTimeZoneMap(invElt, calItem.getTimeZoneMap());
            if (invites.length > 0) {
                if (showAll)
                    encodeCalendarReplies(invElt, calItem, invites[0], recurIdZ);
                for (Invite inv : invites)
                    encodeInviteComponent(invElt, ifmt, octxt, calItem, inv, NOTIFY_FIELDS, neuter);
            }

            //encodeAlarmTimes(invElt, calItem);

            if (mm != null && showAll) {
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
                    addParts(m, parts.get(0), bodies, part, maxSize, neuter, true, getDefaultCharset(calItem));
                }
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
        return encodeMessageAsMIME(parent, ifmt, msg, part, false, serializeType);
    }

    public static Element encodeMessageAsMIME(Element parent, ItemIdFormatter ifmt, Message msg, String part, boolean mustInline, boolean serializeType)
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
        long size = msg.getSize() + 2048;
        if (!wholeMessage) {
            content.addAttribute(MailConstants.A_URL, CONTENT_SERVLET_URI + ifmt.formatItemId(msg) + PART_PARAM_STRING + part);
        } else if (!mustInline && size > MAX_INLINE_MSG_SIZE) {
            content.addAttribute(MailConstants.A_URL, CONTENT_SERVLET_URI + ifmt.formatItemId(msg));
        } else {
            try {
                byte[] raw = msg.getContent();
                if (!ByteUtil.isASCII(raw)) {
                    if (!mustInline) {
                        content.addAttribute(MailConstants.A_URL, CONTENT_SERVLET_URI + ifmt.formatItemId(msg));
                    } else {
                        // Assume the data is utf-8.
                        content.setText(new String(raw, "utf-8"));
                    }
                } else {
                    content.setText(new String(raw, "US-ASCII"));
                }
            } catch (IOException ex) {
                throw ServiceException.FAILURE(ex.getMessage(), ex);
            }
        }

        return m;
    }

    public static enum OutputParticipants { PUT_SENDERS, PUT_RECIPIENTS, PUT_BOTH }

    public static Element encodeMessageSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
                                               Message msg, OutputParticipants output) {
        return encodeMessageSummary(parent, ifmt, octxt, msg, output, NOTIFY_FIELDS);
    }

    public static Element encodeMessageSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
                                               Message msg, OutputParticipants output, int fields) {
        Element elem = encodeMessageCommon(parent, ifmt, msg, fields, true);
        elem.addAttribute(MailConstants.A_ID, ifmt.formatItemId(msg));

        if (!needToOutput(fields, Change.MODIFIED_CONTENT))
            return elem;

        boolean addRecips  = msg.isFromMe() && (output == OutputParticipants.PUT_RECIPIENTS || output == OutputParticipants.PUT_BOTH);
        boolean addSenders = output == OutputParticipants.PUT_BOTH || !addRecips;
        if (addRecips)
            addEmails(elem, Mime.parseAddressHeader(msg.getRecipients()), EmailType.TO);

        if (addSenders)
            encodeEmail(elem, msg.getSender(), EmailType.FROM);

        elem.addAttribute(MailConstants.E_SUBJECT, StringUtil.stripControlCharacters(msg.getSubject()), Element.Disposition.CONTENT);

        // fragment has already been sanitized...
        String fragment = msg.getFragment();
        if (!fragment.equals(""))
            elem.addAttribute(MailConstants.E_FRAG, fragment, Element.Disposition.CONTENT);

        if (msg.isInvite() && msg.hasCalendarItemInfos()) {
            try {
                encodeInvitesForMessage(elem, ifmt, octxt, msg, fields, true);
            } catch (ServiceException ex) {
                mLog.debug("Caught exception while encoding Invites for msg " + msg.getId(), ex);
            }
        }

        return elem;
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
        if (needToOutput(fields, Change.MODIFIED_METADATA))
            encodeAllCustomMetadata(elem, item, fields);

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
        for (Iterator<ICalTimeZone> iter = tzmap.tzIterator(); iter.hasNext(); ) {
            ICalTimeZone tz = iter.next();
            Element e = parent.addElement(MailConstants.E_CAL_TZ);
            e.addAttribute(MailConstants.A_ID, tz.getID());
            e.addAttribute(MailConstants.A_CAL_TZ_STDOFFSET, tz.getStandardOffset() / 60 / 1000);
            e.addAttribute(MailConstants.A_CAL_TZ_STDNAME, tz.getStandardTzname());

            if (tz.useDaylightTime()) {
                SimpleOnset standard = tz.getStandardOnset();
                SimpleOnset daylight = tz.getDaylightOnset();
                if (standard != null && daylight != null) {
                    e.addAttribute(MailConstants.A_CAL_TZ_DAYOFFSET, tz.getDaylightOffset() / 60 / 1000);
                    e.addAttribute(MailConstants.A_CAL_TZ_DAYNAME, tz.getDaylightTzname());

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

    private static boolean allowPrivateAccess(OperationContext octxt, CalendarItem calItem)
    throws ServiceException {
        Account authAccount = octxt != null ? octxt.getAuthenticatedUser() : null;
        boolean asAdmin = octxt != null ? octxt.isUsingAdminPrivileges() : false;
        return calItem.allowPrivateAccess(authAccount, asAdmin);
    }

    public static Element encodeInviteComponent(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
                                                CalendarItem calItem /* may be null */,
                                                Invite invite,
                                                int fields, boolean neuter)
    throws ServiceException {
        boolean allFields = true;

        if (fields != NOTIFY_FIELDS) {
            allFields = false;
            if (!needToOutput(fields, Change.MODIFIED_INVITE)) {
                return parent;
            }
        }

        Element e = parent.addElement(MailConstants.E_INVITE_COMPONENT);
        e.addAttribute(MailConstants.A_CAL_METHOD, invite.getMethod());

        e.addAttribute(MailConstants.A_CAL_COMPONENT_NUM, invite.getComponentNum());

        e.addAttribute(MailConstants.A_CAL_RSVP, invite.getRsvp());

        boolean allowPrivateAccess = calItem != null ? allowPrivateAccess(octxt, calItem) : true;
        if (allFields) {
            if (invite.isPublic() || allowPrivateAccess) {
                String priority = invite.getPriority();
                if (priority != null)
                    e.addAttribute(MailConstants.A_CAL_PRIORITY, priority);

                e.addAttribute(MailConstants.A_NAME, invite.getName());
                e.addAttribute(MailConstants.A_CAL_LOCATION, invite.getLocation());

                List<String> categories = invite.getCategories();
                if (categories != null) {
                    for (String cat : categories) {
                        e.addElement(MailConstants.E_CAL_CATEGORY).setText(cat);
                    }
                }
                List<String> comments = invite.getComments();
                if (comments != null) {
                    for (String cmt : comments) {
                        e.addElement(MailConstants.E_CAL_COMMENT).setText(cmt);
                    }
                }
                List<String> contacts = invite.getContacts();
                if (contacts != null) {
                    for (String contact : contacts) {
                        e.addElement(MailConstants.E_CAL_CONTACT).setText(contact);
                    }
                }
                Geo geo = invite.getGeo();
                if (geo != null)
                    geo.toXml(e);

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

                // fragment
                String fragment = invite.getFragment();
                if (fragment != null && fragment.length() > 0) {
                    e.addAttribute(MailConstants.E_FRAG, fragment, Element.Disposition.CONTENT);
                }

                if (!invite.hasBlobPart())
                    e.addAttribute(MailConstants.A_CAL_NO_BLOB, true);

                // Description (plain and html)
                String desc = invite.getDescription();
                if (desc != null) {
                    Element descElem = e.addElement(MailConstants.E_CAL_DESCRIPTION);
                    descElem.setText(desc);
                }
                String descHtml = invite.getDescriptionHtml();
                if (descHtml != null) {
                    try {
                        descHtml = StringUtil.stripControlCharacters(descHtml);
                        descHtml = HtmlDefang.defang(descHtml, neuter);
                        Element descHtmlElem = e.addElement(MailConstants.E_CAL_DESC_HTML);
                        descHtmlElem.setText(descHtml);
                    } catch (IOException ex) {
                        ZimbraLog.calendar.warn("Unable to defang HTML for SetAppointmentRequest", ex);
                    }
                }

                if (invite.isEvent()) {
                    if (calItem != null && calItem instanceof Appointment) {
                        Instance inst = Instance.fromInvite(calItem.getId(), invite);
                        Appointment appt = (Appointment) calItem;
                        e.addAttribute(MailConstants.A_APPT_FREEBUSY_ACTUAL,
                                    appt.getEffectiveFreeBusyActual(invite, inst));
                    }
                    e.addAttribute(MailConstants.A_APPT_FREEBUSY, invite.getFreeBusy());
                    e.addAttribute(MailConstants.A_APPT_TRANSPARENCY, invite.getTransparency());
                }
            }

            if (invite.isOrganizer())
                e.addAttribute(MailConstants.A_CAL_ISORG, true);

            // Organizer
            if (invite.hasOrganizer()) {
                ZOrganizer org = invite.getOrganizer();
                org.toXml(e);
            }

            boolean isRecurring = false;
            e.addAttribute("x_uid", invite.getUid());
            e.addAttribute(MailConstants.A_UID, invite.getUid());
            e.addAttribute(MailConstants.A_CAL_SEQUENCE, invite.getSeqNo());
            e.addAttribute(MailConstants.A_CAL_DATETIME, invite.getDTStamp()); //zdsync

            if (calItem != null) {
                String itemId = ifmt.formatItemId(calItem);
                e.addAttribute(MailConstants.A_CAL_ID, itemId);
                if (invite.isEvent())
                    e.addAttribute(MailConstants.A_APPT_ID_DEPRECATE_ME, itemId);  // for backward compat
            }

            Recurrence.IRecurrence recur = invite.getRecurrence();
            if (recur != null) {
                isRecurring = true;
                Element recurElt = e.addElement(MailConstants.E_CAL_RECUR);
                recur.toXml(recurElt);
            }

            e.addAttribute(MailConstants.A_CAL_STATUS, invite.getStatus());
            e.addAttribute(MailConstants.A_CAL_CLASS, invite.getClassProp());
            e.addAttribute(MailConstants.A_CAL_URL, invite.getUrl());

            boolean allDay = invite.isAllDayEvent();
            boolean isException = invite.hasRecurId();
            if (isException) {
                e.addAttribute(MailConstants.A_CAL_IS_EXCEPTION, true);
                RecurId rid = invite.getRecurId();
                e.addAttribute(MailConstants.A_CAL_RECURRENCE_ID_Z, rid.getDtZ());
                encodeRecurId(e, rid, allDay);
            }

            boolean forceUTC =
                DebugConfig.calendarForceUTC && !isRecurring && !isException && !allDay;
            ParsedDateTime dtStart = invite.getStartTime();
            if (dtStart != null)
                encodeDtStart(e, dtStart, allDay, forceUTC);

            ParsedDateTime dtEnd = invite.getEndTime();
            if (dtEnd != null)
                encodeDtEnd(e, dtEnd, allDay, invite.isTodo(), forceUTC);

            ParsedDuration dur = invite.getDuration();
            if (dur != null) {
                dur.toXml(e);
            }

            if (allDay)
                e.addAttribute(MailConstants.A_CAL_ALLDAY, true);
        }

        return e;
    }

    public static void encodeXParams(Element parent, Iterator<ZParameter> xparamsIterator) {
        while (xparamsIterator.hasNext()) {
            ZParameter xparam = xparamsIterator.next();
            String paramName = xparam.getName();
            if (paramName == null) continue;
            Element paramElem = parent.addElement(MailConstants.E_CAL_XPARAM);
            paramElem.addAttribute(MailConstants.A_NAME, paramName);
            String paramValue = xparam.getValue();
            if (paramValue != null)
                paramElem.addAttribute(MailConstants.A_VALUE, paramValue);
        }
    }

    public static void encodeXProps(Element parent, Iterator<ZProperty> xpropsIterator) {
        while (xpropsIterator.hasNext()) {
            ZProperty xprop = xpropsIterator.next();
            String propName = xprop.getName();
            if (propName == null) continue;
            String propValue = xprop.getValue();
            Element propElem = parent.addElement(MailConstants.E_CAL_XPROP);
            propElem.addAttribute(MailConstants.A_NAME, propName);
            if (propValue != null)
                propElem.addAttribute(MailConstants.A_VALUE, propValue);
            encodeXParams(propElem, xprop.parameterIterator());
        }
    }

    private static Element encodeInvitesForMessage(Element parent, ItemIdFormatter ifmt,
                                                   OperationContext octxt, Message msg, int fields, boolean neuter)
    throws ServiceException {
        if (fields != NOTIFY_FIELDS)
            if (!needToOutput(fields, Change.MODIFIED_INVITE))
                return parent;

        Element ie = parent.addElement(MailConstants.E_INVITE);

        Mailbox mbox = msg.getMailbox();

        for (Iterator<Message.CalendarItemInfo> iter = msg.getCalendarItemInfoIterator(); iter.hasNext(); ) {
            Message.CalendarItemInfo info = iter.next();
            CalendarItem calItem = null;
            ICalTok method = ICalTok.REQUEST;
            Invite invCi = info.getInvite();
            if (invCi != null)
                method = Invite.lookupMethod(invCi.getMethod());
            Invite invite = invCi;
            if (info.calItemCreated()) {
                try {
                    calItem = mbox.getCalendarItemById(octxt, info.getCalendarItemId());
                } catch (MailServiceException.NoSuchItemException e) {
                    // ignore
                } catch (ServiceException e) {
                    // eat PERM_DENIED
                    if (e.getCode() != ServiceException.PERM_DENIED)
                        throw e;
                }
                // Do staleness check for invitation messages.
                if (ICalTok.REQUEST.equals(method) || ICalTok.PUBLISH.equals(method)) {
                    if (calItem != null && calItem.getFolderId() != Mailbox.ID_FOLDER_TRASH) {
                        if (invCi != null) {
                            // See if the messsage's invite is outdated.
                            Invite invCurr = calItem.getInvite(invCi.getRecurId());
                            if (invCurr != null) {
                                if (invCi.getSeqNo() >= invCurr.getSeqNo()) {
                                    // Invite is new or same as what's in the appointment.  Show it.
                                    invite = invCi;
                                } else {
                                    // Outdated.  Don't show it.
                                    invite = null;
                                }
                            } else {
                                // New invite.  Show it.
                                invite = invCi;
                            }
                        } else {
                            // legacy case
                            invite = calItem.getInvite(msg.getId(), info.getComponentNo());
                            // invite == null if the invite was outdated by a newer update
                        }
                    }
                }
            } else {
                // We have an invite that wasn't auto-added.
                if (invCi != null) {
                    if (!Invite.isOrganizerMethod(invCi.getMethod()) || ICalTok.DECLINECOUNTER.equals(method)) {
                        invite = invCi;
                    } else {
                        try {
                            calItem = mbox.getCalendarItemByUid(octxt, invCi.getUid());
                        } catch (MailServiceException.NoSuchItemException e) {
                            // ignore
                        } catch (ServiceException e) {
                            // eat PERM_DENIED
                            if (e.getCode() != ServiceException.PERM_DENIED)
                                throw e;
                        }
                        if (calItem != null) {
                            // See if the messsage's invite is outdated.
                            Invite invCurr = calItem.getInvite(invCi.getRecurId());
                            if (invCurr != null) {
                                if (invCi.getSeqNo() >= invCurr.getSeqNo()) {
                                    // Invite is new or same as what's in the appointment.  Show it.
                                    invite = invCi;
                                } else {
                                    // Outdated.  Don't show it.
                                    invite = null;
                                }
                            } else {
                                // New invite.  Show it.
                                invite = invCi;
                            }
                        } else {
                            // Appointment doesn't exist.  The invite in the message should be displayed and the
                            // user can manually add the appointment.
                            invite = invCi;
                        }
                    }
                }
            }
            if (invite != null) {
                setCalendarItemType(ie, invite.getItemType());
                encodeTimeZoneMap(ie, invite.getTimeZoneMap());
                encodeInviteComponent(ie, ifmt, octxt, calItem, invite, fields, neuter);
            }
        }

        return ie;
    }

    private enum VisitPhase { PREVISIT, POSTVISIT }

    private static void addParts(Element root, MPartInfo mpi, Set<MPartInfo> bodies, String prefix, int maxSize,
                                 boolean neuter, boolean excludeCalendarParts, String defaultCharset) {
        LinkedList<Pair<Element, LinkedList<MPartInfo>>> queue = new LinkedList<Pair<Element, LinkedList<MPartInfo>>>();
        Pair<Element, LinkedList<MPartInfo>> level = new Pair<Element, LinkedList<MPartInfo>>(root, new LinkedList<MPartInfo>());
        level.getSecond().add(mpi);
        queue.add(level);

        VisitPhase phase = VisitPhase.PREVISIT;
        while (!queue.isEmpty()) {
            level = queue.getLast();
            LinkedList<MPartInfo> parts = level.getSecond();
            if (parts.isEmpty()) {
                queue.removeLast();  phase = VisitPhase.POSTVISIT;  continue;
            }

            mpi = parts.getFirst();
            Element child = addPart(phase, level.getFirst(), root, mpi, bodies, prefix, maxSize, neuter, excludeCalendarParts, defaultCharset);
            if (phase == VisitPhase.PREVISIT && child != null && mpi.hasChildren()) {
                queue.addLast(new Pair<Element, LinkedList<MPartInfo>>(child, new LinkedList<MPartInfo>(mpi.getChildren())));
            } else {
                parts.removeFirst();  phase = VisitPhase.PREVISIT;
            }
        }
    }

    private static Element addPart(VisitPhase phase, Element parent, Element root, MPartInfo mpi, Set<MPartInfo> bodies, String prefix,
                                   int maxSize, boolean neuter, boolean excludeCalendarParts, String defaultCharset) {
        if (phase == VisitPhase.POSTVISIT)
            return null;

        String ctype = StringUtil.stripControlCharacters(mpi.getContentType());

        if (excludeCalendarParts && MimeConstants.CT_TEXT_CALENDAR.equalsIgnoreCase(ctype))
            return null;

        Element elem = parent.addElement(MailConstants.E_MIMEPART);
        MimePart mp = mpi.getMimePart();

        String part = mpi.getPartName();
        part = prefix + (prefix.equals("") || part.equals("") ? "" : ".") + part;
        elem.addAttribute(MailConstants.A_PART, part);

        String fname = Mime.getFilename(mp);
        if (MimeConstants.CT_XML_ZIMBRA_SHARE.equals(ctype)) {
            // the <shr> share info goes underneath the top-level <m>
            Element shr = root.addElement(MailConstants.E_SHARE_NOTIFICATION);
            try {
                addContent(shr, mpi, maxSize, defaultCharset);
            } catch (IOException e) {
                if (mLog.isWarnEnabled())
                    mLog.warn("error writing body part: ", e);
            } catch (MessagingException e) {
            }
        } else if (MimeConstants.CT_TEXT_ENRICHED.equals(ctype)) {
            // we'll be replacing text/enriched with text/html
            ctype = MimeConstants.CT_TEXT_HTML;
        } else if (fname != null && (MimeConstants.CT_APPLICATION_OCTET_STREAM.equals(ctype) || MimeConstants.CT_APPLICATION_TNEF.equals(ctype))) {
            String guess = MimeDetect.getMimeDetect().detect(fname);
            if (guess != null)
                ctype = guess;
        }
        elem.addAttribute(MailConstants.A_CONTENT_TYPE, ctype);

        // figure out attachment size
        try {
            elem.addAttribute(MailConstants.A_SIZE, Mime.getSize(mp));
        } catch (Exception me) {
            // don't put out size if we get exception
            ZimbraLog.mailbox.warn("Unable to determine MIME part size: " + me);
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
            if (fname == null && MimeConstants.CT_MESSAGE_RFC822.equals(ctype)) {
                // "filename" for attached messages is the Subject
                Object content = Mime.getMessageContent(mp);
                if (content instanceof MimeMessage)
                    fname = Mime.getSubject((MimeMessage) content);
            }
            if (fname != null && !fname.equals(""))
                elem.addAttribute(MailConstants.A_CONTENT_FILENAME, StringUtil.stripControlCharacters(fname));
        } catch (MessagingException me) {
        } catch (IOException ioe) {
        }

        // figure out content-id (used in displaying attached images)
        String cid = mpi.getContentID();
        if (cid != null)
            elem.addAttribute(MailConstants.A_CONTENT_ID, StringUtil.stripControlCharacters(cid));

        // figure out content-location (used in displaying attached images)
        try {
            String cl = mp.getHeader("Content-Location", null);
            if (cl != null)
                elem.addAttribute(MailConstants.A_CONTENT_LOCATION, StringUtil.stripControlCharacters(cl));
        } catch (MessagingException me) { }

        // include the part's content if this is the displayable "memo part",
        // or if it was requested to include all parts
        if (bodies == null || bodies.contains(mpi)) {
            if (bodies != null)
                elem.addAttribute(MailConstants.A_BODY, true);
            try {
                addContent(elem, mpi, maxSize, neuter, defaultCharset);
            } catch (IOException ioe) {
                if (mLog.isWarnEnabled())
                    mLog.warn("error writing body part: ", ioe);
            } catch (MessagingException me) {
            }
        }

        return elem;
    }

    /** Adds the decoded text content of a message part to the {@link Element}.
     *  The content is added as the value of a new <tt>&lt;content></tt>
     *  sub-element.  <i>Note: This method will only extract the content of a
     *  <b>text/*</b> or <b>xml/*</b> message part.</i> 
     * @param elt  The element to add the <tt>&lt;content></tt> to.
     * @param mpi  The message part to extract the content from.
     * @param maxSize The maximum amount of content to inline (<=0 is unlimited).
     * @parame defaultCharset  The user's default charset preference.
     * @throws MessagingException when message parsing or CTE-decoding fails
     * @throws IOException on error during parsing or defanging */
    private static void addContent(Element elt, MPartInfo mpi, int maxSize, String defaultCharset)
    throws IOException, MessagingException {
        addContent(elt, mpi, maxSize, true, defaultCharset);
    }

    /** Adds the decoded text content of a message part to the {@link Element}.
     *  The content is added as the value of a new <tt>&lt;content></tt>
     *  sub-element.  <i>Note: This method will only extract the content of a
     *  <b>text/*</b> or <b>xml/*</b> message part.</i> 
     * 
     * @param elt     The element to add the <tt>&lt;content></tt> to.
     * @param mpi     The message part to extract the content from.
     * @param maxSize The maximum number of characters to inline (<=0 is unlimited).
     * @param neuter  Whether to "neuter" image <tt>src</tt> attributes.
     * @parame defaultCharset  The user's default charset preference.
     * @throws MessagingException when message parsing or CTE-decoding fails
     * @throws IOException on error during parsing or defanging
     * @see HtmlDefang#defang(String, boolean) */
    private static void addContent(Element elt, MPartInfo mpi, int maxSize, boolean neuter, String defaultCharset)
    throws IOException, MessagingException {
        // TODO: support other parts
        String ctype = mpi.getContentType();
        if (!ctype.matches(MimeConstants.CT_TEXT_WILD) && !ctype.matches(MimeConstants.CT_XML_WILD))
            return;

        MimePart mp = mpi.getMimePart();
        Mime.repairTransferEncoding(mp);

        
        String data = null;
        try {
            if (maxSize <= 0) {
                maxSize = (int) Provisioning.getInstance().getLocalServer().getMailContentMaxSize();
            } else {
                maxSize = Math.min(maxSize, (int) Provisioning.getInstance().getLocalServer().getMailContentMaxSize());
            }
        } catch (ServiceException e) {
            ZimbraLog.soap.warn("Unable to determine max content size", e);
        }

        if (ctype.equals(MimeConstants.CT_TEXT_HTML)) {
            String charset = mpi.getContentTypeParameter(MimeConstants.P_CHARSET);
            InputStream stream = null;
            StringWriter sw = new StringWriter();
            TruncatingWriter tw = null;
            Writer out = sw;
            if (maxSize > 0) {
                tw = new TruncatingWriter(sw, maxSize + 1);
                out = tw;
            }
            Reader reader = null;
            
            try {
                if (charset != null && !charset.trim().equals("")) {
                    stream = mp.getInputStream();
                    // make sure to feed getTextReader() a full Content-Type header, not just the primary/subtype portion
                    reader = Mime.getTextReader(stream, mp.getContentType(), defaultCharset);
                    HtmlDefang.defang(reader, neuter, out);
                    data = sw.toString();
                } else {
                    String cte = mp.getEncoding();
                    if (cte != null && !cte.trim().toLowerCase().equals(MimeConstants.ET_7BIT)) {
                        try {
                            stream = mp.getInputStream();
                            HtmlDefang.defang(stream, neuter, out);
                            data = sw.toString();
                        } catch (IOException ioe) { }
                    }
                    if (data == null) {
                        reader = Mime.getContentAsReader(mp, defaultCharset);
                        HtmlDefang.defang(reader, neuter, out);
                        data = sw.toString();
                    }
                }
            } finally {
                ByteUtil.closeStream(stream);
                ByteUtil.closeReader(reader);
            }
        } else if (ctype.equals(MimeConstants.CT_TEXT_ENRICHED)) {
            // Enriched text handling is a little funky because TextEnrichedHandler
            // doesn't use Reader and Writer.  As a result, we truncate
            // the source before converting to HTML.
            Reader reader = Mime.getContentAsReader(mp, defaultCharset);
            int maxChars = (maxSize > 0 ? maxSize + 1 : -1);
            String enriched = ByteUtil.getContent(reader, maxChars, true);
            if (enriched.length() == maxChars) {
                // The normal check for truncated data won't work because
                // the length of the converted text is different than the length
                // of the source, so set the attr here.
                elt.addAttribute(MailConstants.A_TRUNCATED_CONTENT, true);
            }
            data = TextEnrichedHandler.convertToHTML(enriched);
        } else {
            Reader reader = Mime.getContentAsReader(mp, defaultCharset);
            int maxChars = (maxSize > 0 ? maxSize + 1 : -1);
            data = ByteUtil.getContent(reader, maxChars, true);
        }

        if (data != null) {
            data = StringUtil.stripControlCharacters(data);
            if (maxSize > 0 && data.length() > maxSize) {
                data = data.substring(0, maxSize);
                elt.addAttribute(MailConstants.A_TRUNCATED_CONTENT, true);
            }
            elt.addAttribute(MailConstants.E_CONTENT, data, Element.Disposition.CONTENT);
        }
        // TODO: CDATA worth the effort?
    }


    public enum EmailType {
        NONE(null), FROM("f"), TO("t"), CC("c"), BCC("b"), REPLY_TO("r"), SENDER("s"), READ_RECEIPT("n");

        private final String mRep;
        private EmailType(String c)  { mRep = c; }

        @Override public String toString()     { return mRep; }
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
        elem.addAttribute(MailConstants.A_ADDRESS, IDNUtil.toUnicode(pa.emailPart));
        elem.addAttribute(MailConstants.A_DISPLAY, pa.firstName);
        elem.addAttribute(MailConstants.A_PERSONAL, pa.personalPart);
        elem.addAttribute(MailConstants.A_ADDRESS_TYPE, type.toString());
        return elem;
    }

    public static Element encodeWiki(Element parent, ItemIdFormatter ifmt, OperationContext octxt, WikiItem wiki) {
        return encodeWiki(parent, ifmt, octxt, wiki, NOTIFY_FIELDS);
    }

    public static Element encodeWiki(Element parent, ItemIdFormatter ifmt, OperationContext octxt, WikiItem wiki, int fields) {
        Element elem = parent.addElement(MailConstants.E_WIKIWORD);
        encodeDocumentCommon(elem, ifmt, octxt, wiki, fields);
        return elem;
    }

    public static Element encodeDocument(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Document doc) {
        return encodeDocument(parent, ifmt, octxt, doc, Change.ALL_FIELDS);
    }

    public static Element encodeDocument(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Document doc, int fields) {
        Element elem = parent.addElement(MailConstants.E_DOC);
        encodeDocumentCommon(elem, ifmt, octxt, doc, fields);
        elem.addAttribute(MailConstants.A_CONTENT_TYPE, doc.getContentType());
        return elem;
    }

    public static Element encodeDocumentCommon(Element m, ItemIdFormatter ifmt, OperationContext octxt, Document doc, int fields) {
        m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(doc));
        if (needToOutput(fields, Change.MODIFIED_NAME))
            m.addAttribute(MailConstants.A_NAME, doc.getName());
        if (needToOutput(fields, Change.MODIFIED_SIZE))
            m.addAttribute(MailConstants.A_SIZE, doc.getSize());
        if (needToOutput(fields, Change.MODIFIED_DATE))
            m.addAttribute(MailConstants.A_DATE, doc.getDate());
        if (needToOutput(fields, Change.MODIFIED_FOLDER))
            m.addAttribute(MailConstants.A_FOLDER, new ItemId(doc.getMailbox().getAccountId(), doc.getFolderId()).toString(ifmt));
        if (needToOutput(fields, Change.MODIFIED_CONFLICT)) {
            m.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, doc.getModifiedSequence());
            m.addAttribute(MailConstants.A_CHANGE_DATE, (doc.getChangeDate() / 1000));
            m.addAttribute(MailConstants.A_REVISION, doc.getSavedSequence());
        }
        recordItemTags(m, doc, fields);
        if (needToOutput(fields, Change.MODIFIED_METADATA))
            encodeAllCustomMetadata(m, doc, fields);

        if (needToOutput(fields, Change.MODIFIED_CONTENT)) {
            try {
                m.addAttribute(MailConstants.A_VERSION, doc.getVersion());
                m.addAttribute(MailConstants.A_LAST_EDITED_BY, doc.getCreator());
                m.addAttribute(MailConstants.A_MODIFIED_DATE, doc.getDate());
                String fragment = doc.getFragment();
                if (fragment != null && !fragment.equals(""))
                    m.addAttribute(MailConstants.E_FRAG, fragment, Element.Disposition.CONTENT);

                Document revision = (Document) doc.getMailbox().getItemRevision(octxt, doc.getId(), doc.getType(), 1);
                if (revision != null) {
                    m.addAttribute(MailConstants.A_CREATOR, revision.getCreator());
                    m.addAttribute(MailConstants.A_CREATED_DATE, revision.getDate());
                }
            } catch (Exception ex) {
                mLog.warn("ignoring exception while fetching revision for document " + doc.getSubject(), ex);
            }
        }

        return m;
    }
    public static Element encodeDataSource(Element parent, DataSource ds) {
        Element m;
        
        m = parent.addElement(getDsType(ds));

        m.addAttribute(MailConstants.A_ID, ds.getId());
        m.addAttribute(MailConstants.A_NAME, ds.getName());
        m.addAttribute(MailConstants.A_FOLDER, ds.getFolderId());
        m.addAttribute(MailConstants.A_DS_IS_ENABLED, ds.isEnabled());

        if (ds.getType() == DataSource.Type.pop3)
            m.addAttribute(MailConstants.A_DS_LEAVE_ON_SERVER, ds.leaveOnServer());

        if (ds.getHost() != null)
            m.addAttribute(MailConstants.A_DS_HOST, ds.getHost());
        if (ds.getPort() != null)
            m.addAttribute(MailConstants.A_DS_PORT, ds.getPort());
        if (ds.getConnectionType() != null)
            m.addAttribute(MailConstants.A_DS_CONNECTION_TYPE, ds.getConnectionType().name());
        if (ds.getUsername() != null)
            m.addAttribute(MailConstants.A_DS_USERNAME, ds.getUsername());

        try {
            if (ds.getPollingInterval() > 0)
                m.addAttribute(MailConstants.A_DS_POLLING_INTERVAL, ds.getAttr(Provisioning.A_zimbraDataSourcePollingInterval));
        } catch (ServiceException e) {
            mLog.warn("Unable to get polling interval from %s", ds, e);
        }

        m.addAttribute(MailConstants.A_DS_EMAIL_ADDRESS, ds.getEmailAddress());
        m.addAttribute(MailConstants.A_DS_USE_ADDRESS_FOR_FORWARD_REPLY, ds.useAddressForForwardReply());
        m.addAttribute(MailConstants.A_DS_DEFAULT_SIGNATURE, ds.getDefaultSignature());
        m.addAttribute(MailConstants.A_DS_FROM_DISPLAY, ds.getFromDisplay());
        m.addAttribute(MailConstants.A_DS_FROM_ADDRESS, ds.getFromAddress());
        m.addAttribute(MailConstants.A_DS_REPLYTO_ADDRESS, ds.getReplyToAddress());
        m.addAttribute(MailConstants.A_DS_REPLYTO_DISPLAY, ds.getReplyToDisplay());
        
        Date date = ds.getGeneralizedTimeAttr(Provisioning.A_zimbraDataSourceFailingSince, null);
        if (date != null) {
            m.addAttribute(MailConstants.A_DS_FAILING_SINCE, date.getTime() / 1000);
        }
        
        String lastError = ds.getAttr(Provisioning.A_zimbraDataSourceLastError); 
        if (lastError != null) {
            m.addElement(MailConstants.E_DS_LAST_ERROR).setText(lastError);
        }
        return m;
    }

    private static String getDsType(DataSource ds) {
        switch (ds.getType()) {
        case imap:
            return MailConstants.E_DS_IMAP;
        case pop3:
            return MailConstants.E_DS_POP3;
        case caldav:
            return MailConstants.E_DS_CALDAV;
        case yab:
            return MailConstants.E_DS_YAB;
        case rss:
            return MailConstants.E_DS_RSS;
        case gal:
            return MailConstants.E_DS_GAL;
        case cal:
            return MailConstants.E_DS_CAL;
        default:
            return MailConstants.E_DS_UNKNOWN;
        }
    }
    
    private static void setCalendarItemType(Element elem, byte itemType) {
        elem.addAttribute(MailConstants.A_CAL_ITEM_TYPE,
                itemType == MailItem.TYPE_APPOINTMENT ? "appt" : "task");
    }

    public static void encodeAlarmTimes(Element elem, CalendarItem calItem) {
        AlarmData alarmData = calItem.getAlarmData();
        if (alarmData != null) {
            long nextAlarm = alarmData.getNextAt();
            if (nextAlarm < Long.MAX_VALUE)
                elem.addAttribute(MailConstants.A_CAL_NEXT_ALARM, nextAlarm);
        }
    }

    public static Element encodeAlarmData(Element parent, CalendarItem calItem, AlarmData alarmData) {
        Element alarmElem = parent.addElement(MailConstants.E_CAL_ALARM_DATA);
        encodeAlarmTimes(alarmElem, calItem);
        // Start time of the meeting instance we're reminding about.
        long alarmInstStart = alarmData.getNextInstanceStart();
        alarmElem.addAttribute(MailConstants.A_CAL_ALARM_INSTANCE_START, alarmInstStart);
        int alarmInvId = alarmData.getInvId();
        int alarmCompNum = alarmData.getCompNum();
        Invite alarmInv = calItem.getInvite(alarmInvId, alarmCompNum);
        if (alarmInv != null) {
            // Some info on the meeting instance the reminder is for.
            // These allow the UI to display tooltip and issue a Get
            // call on the correct meeting instance.
            alarmElem.addAttribute(MailConstants.A_NAME, alarmInv.getName());
            alarmElem.addAttribute(MailConstants.A_CAL_LOCATION, alarmInv.getLocation());
            alarmElem.addAttribute(MailConstants.A_CAL_INV_ID, alarmInvId);
            alarmElem.addAttribute(MailConstants.A_CAL_COMPONENT_NUM, alarmCompNum);
        }
        Alarm alarmObj = alarmData.getAlarm();
        if (alarmObj != null)
            alarmObj.toXml(alarmElem);
        return alarmElem;
    }
    
    public static Element encodeFreeBusy(Element parent, FreeBusy fb) {
        Element resp = parent.addElement(MailConstants.E_FREEBUSY_USER);
        resp.addAttribute(MailConstants.A_ID, fb.getName());
        for (Iterator<FreeBusy.Interval> iter = fb.iterator(); iter.hasNext(); ) {
        	FreeBusy.Interval cur = iter.next();
        	String status = cur.getStatus();
        	Element elt;
        	if (status.equals(IcalXmlStrMap.FBTYPE_FREE)) {
        		elt = resp.addElement(MailConstants.E_FREEBUSY_FREE);
        	} else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY)) {
        		elt = resp.addElement(MailConstants.E_FREEBUSY_BUSY);
        	} else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE)) {
        		elt = resp.addElement(MailConstants.E_FREEBUSY_BUSY_TENTATIVE);
        	} else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE)) {
        		elt = resp.addElement(MailConstants.E_FREEBUSY_BUSY_UNAVAILABLE);
        	} else {
        		assert(false);
        		elt = null;
        	}

        	if (elt != null) {
            	elt.addAttribute(MailConstants.A_CAL_START_TIME, cur.getStart());
            	elt.addAttribute(MailConstants.A_CAL_END_TIME, cur.getEnd());
        	}
        }
    	
        return resp;
    }

    private static Element encodeRecurId(Element parent, RecurId recurId, boolean allDay) {
        ParsedDateTime ridDt = recurId.getDt();
        Element ridElem = parent.addElement(MailConstants.E_CAL_EXCEPTION_ID);
        ridElem.addAttribute(MailConstants.A_CAL_DATETIME, ridDt.getDateTimePartString(false));
        if (!allDay)
            ridElem.addAttribute(MailConstants.A_CAL_TIMEZONE, ridDt.getTZName());
        int rangeType = recurId.getRange();
        if (rangeType != RecurId.RANGE_NONE)
            ridElem.addAttribute(MailConstants.A_CAL_RECURRENCE_RANGE_TYPE, rangeType);
        return ridElem;
    }

    private static Element encodeDtStart(Element parent, ParsedDateTime dtStart, boolean allDay, boolean forceUtc) {
        if (forceUtc) {
            dtStart = (ParsedDateTime) dtStart.clone();
            dtStart.toUTC();
        }
        // fixup for bug 30121
        if (allDay && dtStart.hasTime()) {
            // If this is supposed to be an all-day event but DTSTART has time part, clear the time part.
            dtStart = (ParsedDateTime) dtStart.clone();
            dtStart.setHasTime(false);
        } else if (!allDay && !dtStart.hasTime()){
            // If the event isn't marked all-day but DTSTART has no time part, mark the event all-day.
            allDay = true;
        }
        Element dtStartElem = parent.addElement(MailConstants.E_CAL_START_TIME);
        dtStartElem.addAttribute(MailConstants.A_CAL_DATETIME, dtStart.getDateTimePartString(false));
        if (!allDay) {
            String tzName = dtStart.getTZName();
            if (tzName != null)
                dtStartElem.addAttribute(MailConstants.A_CAL_TIMEZONE, tzName);
        }
        return dtStartElem;
    }

    private static Element encodeDtEnd(Element parent, ParsedDateTime dtEnd, boolean allDay, boolean isTodo, boolean forceUtc) {
        if (forceUtc) {
            dtEnd = (ParsedDateTime) dtEnd.clone();
            dtEnd.toUTC();
        }
        // fixup for bug 30121
        if (allDay && dtEnd.hasTime()) {
            // If this is supposed to be an all-day event but DTEND has time part, clear the time part.
            dtEnd = (ParsedDateTime) dtEnd.clone();
            dtEnd.setHasTime(false);
        } else if (!allDay && !dtEnd.hasTime()) {
            // If the event isn't marked all-day but DTEND has no time part, mark the event all-day.
            allDay = true;
        }
        Element dtEndElem = parent.addElement(MailConstants.E_CAL_END_TIME);
        if (!allDay) {
            String tzName = dtEnd.getTZName();
            if (tzName != null)
                dtEndElem.addAttribute(MailConstants.A_CAL_TIMEZONE, tzName);
        } else {
            if (!isTodo) {
                // See CalendarUtils.parseInviteElementCommon, where we parse DTEND
                // for a description of why we add -1d when sending to the client
                dtEnd = dtEnd.add(ParsedDuration.NEGATIVE_ONE_DAY);
            }
        }
        dtEndElem.addAttribute(MailConstants.A_CAL_DATETIME, dtEnd.getDateTimePartString(false));
        return dtEndElem;
    }

    public static void encodeCalendarItemRecur(Element parent, ItemIdFormatter ifmt,
                                               OperationContext octxt, CalendarItem calItem) {
        TimeZoneMap tzmap = calItem.getTimeZoneMap();
        encodeTimeZoneMap(parent, tzmap);
        Invite[] invites = calItem.getInvites();
        for (Invite inv : invites) {
            String elemName;
            if (inv.isCancel())
                elemName = MailConstants.E_CAL_CANCEL;
            else if (inv.hasRecurId())
                elemName = MailConstants.E_CAL_EXCEPT;
            else
                elemName = MailConstants.E_INVITE_COMPONENT;
            Element compElem = parent.addElement(elemName);
            boolean allDay = inv.isAllDayEvent();
            // RECURRENCE-ID
            if (inv.hasRecurId())
                encodeRecurId(compElem, inv.getRecurId(), allDay);
            if (!inv.isCancel()) {
                // DTSTART
                ParsedDateTime dtStart = inv.getStartTime();
                if (dtStart != null)
                    encodeDtStart(compElem, dtStart, allDay, false);
                // DTEND or DURATION
                ParsedDateTime dtEnd = inv.getEndTime();
                if (dtEnd != null) {
                    encodeDtEnd(compElem, dtEnd, allDay, inv.isTodo(), false);
                } else {
                    ParsedDuration dur = inv.getDuration();
                    if (dur != null)
                        dur.toXml(compElem);
                }

                // recurrence definition
                IRecurrence recurrence = inv.getRecurrence();
                if (recurrence != null) {
                    Element recurElem = compElem.addElement(MailConstants.E_CAL_RECUR);
                    recurrence.toXml(recurElem);
                }
            }
        }
    }
    public static void encodeGalContact(Element response, GalContact contact) {
        Element cn = response.addElement(MailConstants.E_CONTACT);
        cn.addAttribute(MailConstants.A_ID, contact.getId());
        Map<String, Object> attrs = contact.getAttrs();
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sa[] = (String[]) value;
                for (int i = 0; i < sa.length; i++)
                    cn.addKeyValuePair(entry.getKey(), sa[i], MailConstants.E_ATTRIBUTE, MailConstants.A_ATTRIBUTE_NAME);
            } else {
                cn.addKeyValuePair(entry.getKey(), (String) value, MailConstants.E_ATTRIBUTE, MailConstants.A_ATTRIBUTE_NAME);
            }
        }
    }
}
