/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.ContactGroup;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;
import com.zimbra.cs.service.util.ItemIdFormatter;

public class ContactFolderFormatter extends Formatter {

    private static final byte FIELD_DELIMITER   = '\u001D';  // group separator
    private static final byte CONTACT_DELIMITER = '\u001E';  // record separator

    private enum Delimiter { Field, Contact };


    @Override
    public void formatCallback(UserServletContext context) throws UserServletException,
            ServiceException, IOException, ServletException {
        if (!(context.target instanceof Folder))
            throw UserServletException.notImplemented("can only handle Folders");

        Folder f = (Folder)context.target;
        if (f.getDefaultView() != MailItem.Type.CONTACT) {
            throw UserServletException.notImplemented("can only handle Contact Folders");
        }
        String v = context.params.get("t");
        Delimiter d = Delimiter.Field;
        if (v != null && v.equals("2"))
            d = Delimiter.Contact;
        v = context.params.get("all");
        boolean allContacts = false;
        if (v != null)
            allContacts = true;

        ItemIdFormatter ifmt = new ItemIdFormatter(context.getAuthAccount(), context.targetAccount, false);
        OutputStream out = new BufferedOutputStream(context.resp.getOutputStream());

        Iterator<? extends MailItem> contacts = null;
        contacts = this.getMailItems(context, 0, 0, 0);
        while (contacts.hasNext())
            printContact(contacts.next(), out, ifmt, d);
        if (allContacts) {
            for (Folder folder : context.targetMailbox.getFolderList(context.opContext, SortBy.NONE)) {
                // local contact folders only
                if (folder == context.target || folder.getType() == MailItem.Type.MOUNTPOINT ||
                        folder.getDefaultView() != MailItem.Type.CONTACT) {
                    continue;
                }
                for (MailItem item : this.getMailItemsFromFolder(context, folder, 0, 0, 0)) {
                    printContact(item, out, ifmt, d);
                }
            }
        }
        out.flush();
    }

    private void printContact(MailItem item, OutputStream out, ItemIdFormatter ifmt, Delimiter d) throws IOException {
        if (!(item instanceof Contact))
            return;
        // send metadata of the Contact
        // itemId
        out.write(MailConstants.A_ID.getBytes("UTF-8"));
        out.write(FIELD_DELIMITER);
        out.write(ifmt.formatItemId(item).getBytes("UTF-8"));
        out.write(FIELD_DELIMITER);
        // folderId
        out.write(MailConstants.A_FOLDER.getBytes("UTF-8"));
        out.write(FIELD_DELIMITER);
        out.write(ifmt.formatItemId(item.getFolderId()).getBytes("UTF-8"));
        out.write(FIELD_DELIMITER);
        // date
        out.write(MailConstants.A_DATE.getBytes("UTF-8"));
        out.write(FIELD_DELIMITER);
        out.write(Long.toString(item.getDate()).getBytes("UTF-8"));
        out.write(FIELD_DELIMITER);
        // revision
        out.write(MailConstants.A_REVISION.getBytes("UTF-8"));
        out.write(FIELD_DELIMITER);
        out.write(Long.toString(item.getSavedSequenceLong()).getBytes("UTF-8"));
        out.write(FIELD_DELIMITER);
        // fileAsStr
        try {
            String fileAsStr = ((Contact)item).getFileAsString();
            out.write(MailConstants.A_FILE_AS_STR.getBytes("UTF-8"));
            out.write(FIELD_DELIMITER);
            out.write(fileAsStr.getBytes("UTF-8"));
        } catch (ServiceException se) {
        }

        Map<String,String> fields = ((Contact) item).getFields();
        for (String k : fields.keySet()) {
            if (ContactConstants.A_groupMember.equals(k)) {
                printContactGroup(fields.get(k), out);
            } else {
                out.write(FIELD_DELIMITER);
                out.write(k.getBytes("UTF-8"));
                out.write(FIELD_DELIMITER);
                out.write(fields.get(k).getBytes("UTF-8"));
            }
        }

        // return the image part number required for Extensible Universal Contact Card (bug 73146)
        List<Contact.Attachment> attachments = ((Contact) item).getAttachments();
        for (Contact.Attachment attachment : attachments) {
            if (attachment.getName().equals(ContactConstants.A_image)) {
                out.write(FIELD_DELIMITER);
                out.write((ContactConstants.A_image + MailConstants.A_PART).getBytes("UTF-8"));
                out.write(FIELD_DELIMITER);
                out.write(attachment.getPartName().getBytes("UTF-8"));
                break;
            }
        }

        switch (d) {
        case Field:
            out.write(FIELD_DELIMITER);
            break;
        case Contact:
            out.write(CONTACT_DELIMITER);
            break;
        }
    }

    private void printContactGroup(String encodedContactGroup, OutputStream out) throws IOException {
        ContactGroup contactGroup = null;

        try {
            contactGroup = ContactGroup.init(encodedContactGroup);
        } catch (ServiceException e) {
            ZimbraLog.contact.warn("unable to init contact group", e);
        }

        if (contactGroup == null) {
            return;
        }

        for (ContactGroup.Member member : contactGroup.getMembers()) {
            ContactGroup.Member.Type type = member.getType();
            out.write(FIELD_DELIMITER);
            out.write(type.getDelimittedFieldsEncoded().getBytes("UTF-8"));
            out.write(FIELD_DELIMITER);
            out.write(member.getValue().getBytes("UTF-8"));
        }
    }

    @Override
    public FormatType getType() {
        return FormatType.CONTACT_FOLDER;
    }



}
