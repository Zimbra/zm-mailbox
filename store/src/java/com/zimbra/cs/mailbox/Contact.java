/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.EntryCacheDataKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.mailbox.MailItem.TemporaryIndexingException;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;

/**
 * @since Aug 23, 2004
 */
public class Contact extends MailItem {

    public static class Attachment implements DataSource {
        private DataHandler mDataHandler;
        private int mSize;
        private String mFieldName;
        private String mPartName;

        /**
         * Creates a new attachment.
         *
         * @param content attachment content
         * @param ctype content type
         * @param field field name
         * @param filename filename
         */
        public Attachment(byte[] content, String ctype, String field, String filename) {
            if (ctype == null) {
                ctype = MimeConstants.CT_APPLICATION_OCTET_STREAM;
            } else {
                ctype = ctype.toLowerCase();
            }
            if (content == null) {
                content = new byte[0];
            }
            ByteArrayDataSource ds = new ByteArrayDataSource(content, ctype);
            if (filename != null) {
                ds.setName(filename);
            } else {
                ds.setName("unknown");
            }
            init(new DataHandler(ds), field, content.length);
        }

        public Attachment(DataHandler dataHandler, String field)
        throws IOException {
            int size = (int) ByteUtil.getDataLength(dataHandler.getInputStream());
            init(dataHandler, field, size);
        }

        public Attachment(DataHandler dataHandler, String field, int size) {
            init(dataHandler, field, size);
        }

        private void init(DataHandler dataHandler, String field, int size) {
            if (dataHandler == null) {
                throw new NullPointerException("dataHandler cannot be null");
            }
            if (StringUtil.isNullOrEmpty(field)) {
                throw new NullPointerException("field cannot be null or empty");
            }
            mDataHandler = dataHandler;
            mFieldName = field;
            mSize = size;
        }

        public void setPartName(String name) {
            mPartName = name;
        }

        @Override
        public String getContentType() {
            return mDataHandler.getContentType();
        }

        @Override
        public String getName() {
            return mFieldName;
        }

        public int getSize() {
            return mSize;
        }

        /**
         * Returns an <tt>InputStream</tt> to this attachment's content, or <tt>null</tt>
         * if there is no content.
         */
        @Override
        public InputStream getInputStream() throws IOException {
            if (mDataHandler != null) {
                return mDataHandler.getInputStream();
            }
            return null;
        }

        @Override
        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns this attachment's content, or <tt>null</tt>.
         */
        public byte[] getContent() throws IOException {
            InputStream in = null;
            byte[] content = null;
            try {
                in = getInputStream();
                if (in != null) {
                    content = ByteUtil.getContent(in, mSize);
                }
            } finally {
                ByteUtil.closeStream(in);
            }
            return content;
        }

        public String getFilename() {
            return StringUtil.sanitizeFilename(mDataHandler.getName());
        }

        public String getPartName()  { return mPartName; }
        public DataHandler getDataHandler() { return mDataHandler; }

        private static final String FN_SIZE = "size", FN_NAME = "name", FN_PART = "part", FN_CTYPE = "ctype", FN_FIELD = "field";

        Metadata asMetadata() {
            return new Metadata().put(FN_SIZE, mSize).put(FN_NAME, getFilename()).put(FN_PART, mPartName).put(FN_CTYPE, getContentType()).put(FN_FIELD, mFieldName);
        }

        @Override
        public String toString() {
            return new StringBuilder(mFieldName).append(" [").append(getContentType()).append(", ").append(mSize).append("B]").toString();
        }
    }

    public static enum DerefGroupMembersOption {
        // ALL,      // deref all group members - not yet supported
        NONE,        // do not deref any group members
        INLINE_ONLY; // deref only inline group members (see ContactGroup.Member.Type)
    };

    /** Relates contact fields ({@code firstName}) to this contact's values ({@code John}). */
    private Map<String, String> fields;
    private List<Attachment> attachments;

    // The list of all *simple* "email" fields in the contact's map
    // IMPORTANT NOTE - does not include the Contact Group 'dlist' entry, which is a multi-value entry (comma-separated)
    private static final String[] EMAIL_FIELDS = new String[] {
        ContactConstants.A_email, ContactConstants.A_email2, ContactConstants.A_email3,
        ContactConstants.A_workEmail1, ContactConstants.A_workEmail2, ContactConstants.A_workEmail3
    };

    private String[] emailFields;

    /**
     * Returns the email fields in contact for the account.
     *
     * This gets called whenever a Contact object is constructed.
     * With the high call rate, we cache it on the account to avoid
     * repeated computing from the attr value.
     *
     * @param acct
     * @return
     */
    public static String[] getEmailFields(Account acct) {
        String[] emailFields = null;

        // see if it is in cache
        emailFields = (String[])acct.getCachedData(EntryCacheDataKey.ACCOUNT_EMAIL_FIELDS);

        if (emailFields == null) {
            String[] fields = null;
            String emailFieldsStr = acct.getAttr(Provisioning.A_zimbraContactEmailFields);
            if (emailFieldsStr != null)
                fields = emailFieldsStr.split(",");

            if (fields != null) {
                // remove dlist if it is there
                List<String> temp = new ArrayList<String>(fields.length);
                for (String field : fields) {
                    if (!ContactConstants.A_dlist.equals(field))
                        temp.add(field);
                }
                if (temp.size() > 0)
                    emailFields = temp.toArray(new String[0]);
            }

            if (emailFields == null)
                emailFields = EMAIL_FIELDS;

            // we now have a non empty emailFields, cache it on the acccount
            acct.setCachedData(EntryCacheDataKey.ACCOUNT_EMAIL_FIELDS, emailFields);
        }

        return emailFields;
    }

    Contact(Mailbox mbox, UnderlyingData data) throws ServiceException {
        this(mbox, data, false);
    }

    Contact(Mailbox mbox, UnderlyingData data, boolean skipCache) throws ServiceException {
        super(mbox, data, skipCache);
        init();
    }

    Contact(Account acc, UnderlyingData data, int mailboxId) throws ServiceException {
        super(acc, data, mailboxId);
        init();
    }

    private void init () throws ServiceException {
        if (mData.type != Type.CONTACT.toByte()) {
            throw new IllegalArgumentException();
        }
        emailFields = getEmailFields(getAccount());
    }

    @Override
    public String getSender() {
        try {
            return getFileAsString();
        } catch (ServiceException e) {
            return "";
        }
    }

    @Override
    public String getSortSender() {
        String sender = getSender();
        // remove surrogate characters and trim to DbMailItem.MAX_SENDER_LENGTH
        return DbMailItem.normalize(sender, DbMailItem.MAX_SENDER_LENGTH);
    }

    /** Returns a single field from the contact's field/value pairs. */
    public String get(String fieldName) {
        return fields.get(fieldName);
    }

    /** Returns a new <tt>Map</tt> containing all the contact's field/value pairs. */
    public Map<String, String> getAllFields() {
        return new HashMap<String, String>(fields);
    }

    /** Returns a new <tt>Map</tt> containing all the visible field/value pairs in the contact. */
    public Map<String, String> getFields() {
        HashMap<String, String> result = new HashMap<String, String>(fields);
        try {
            String hiddenAttrList = Provisioning.getInstance().getLocalServer().getContactHiddenAttributes();
            if (hiddenAttrList != null) {
                for (String attr : hiddenAttrList.split(",")) {
                    result.remove(attr);
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.mailop.warn("can't get A_zimbraContactHiddenAttributes", e);
        }
        return result;
    }

    /**
     * Returns a list of all the contact's attachments.
     * <p>
     * If the contact has no attachments in its blob, an empty list is returned.
     */
    public List<Attachment> getAttachments() {
        if (attachments == null) {
            return Collections.emptyList();
        }
        return new ArrayList<Attachment>(attachments);
    }

    /**
     * Returns the <tt>MimeMessage</tt> for this contact.
     * @throws ServiceException if no <tt>MimeMessage</tt> exists or there
     * was an error retrieving it
     */
    public MimeMessage getMimeMessage(boolean runConverters) throws ServiceException {
        return MessageCache.getMimeMessage(this, runConverters);
    }

    /**
     * Returns the contact name used for sorting.
     *
     * @return file-as string honoring phonetic fields
     */
    public String getSortName() throws ServiceException {
        return getFileAsString(fields, true);
    }

    /**
     * Returns the "file as" string used for listing purposes.
     * <p>
     * This value is derived by using the {@code fileAs} contact field value to
     * select a standard or custom formatting to apply to the contact's fields.
     * Supported {@code fileAs} values are:
     * <ul>
     *  <li>{@code 1} - Last, First
     *  <li>{@code 2} - First Last
     *  <li>{@code 3} - Company
     *  <li>{@code 4} - Last, First (Company)
     *  <li>{@code 5} - First Last (Company)
     *  <li>{@code 6} - Company (Last, First)
     *  <li>{@code 7} - Company (First Last)
     *  <li>{@code 8:your name here} - The string "your name here"
     * </ul>
     * When a {@code fileAs} value is not specified, {@code 1} is used as the
     * default.
     */
    public String getFileAsString() throws ServiceException {
        return getFileAsString(fields, false);
    }

    public static String getFileAsString(Map<String, String> fields) throws ServiceException {
        return getFileAsString(fields, false);
    }

    public static boolean isUrlField(String field) {
        return ContactConstants.A_homeURL.equalsIgnoreCase(field)
            || ContactConstants.A_otherURL.equalsIgnoreCase(field)
            || ContactConstants.A_workURL.equalsIgnoreCase(field);
    }

    private static String getFileAsString(Map<String, String> fields,
            boolean phonetic) throws ServiceException {

        String fileAs = fields.get(ContactConstants.A_fileAs);
        String[] fileParts = (fileAs == null ? null : fileAs.split(":", 2));
        int fileAsInt = ContactConstants.FA_DEFAULT;
        if (fileParts != null) {
            try {
                fileAsInt = Integer.parseInt(fileParts[0]);
                if (fileAsInt < 0 || fileAsInt > ContactConstants.FA_MAXIMUM)
                    throw ServiceException.INVALID_REQUEST("invalid fileAs value: " + fileAs, null);
            } catch (NumberFormatException e) {
                throw ServiceException.INVALID_REQUEST("invalid fileAs value: " + fileAs, null);
            }
        }

        String company = null;
        if (phonetic) {
            company = fields.get(ContactConstants.A_phoneticCompany);
        }
        if (Strings.isNullOrEmpty(company)) {
            company = fields.get(ContactConstants.A_company);
        }
        company = Strings.nullToEmpty(company);

        String first = null;
        if (phonetic) {
            first = fields.get(ContactConstants.A_phoneticFirstName);
        }
        if (Strings.isNullOrEmpty(first)) {
            first = fields.get(ContactConstants.A_firstName);
        }
        first = Strings.nullToEmpty(first);

        String last = null;
        if (phonetic) {
            last = fields.get(ContactConstants.A_phoneticLastName);
        }
        if (Strings.isNullOrEmpty(last)) {
            last = fields.get(ContactConstants.A_lastName);
        }
        last = Strings.nullToEmpty(last);

        StringBuilder result = new StringBuilder();
        switch (fileAsInt) {
            case ContactConstants.FA_EXPLICIT:
                if (fileParts.length == 2 && !fileParts[1].trim().equals("")) {
                    result.append(fileParts[1].trim());
                    break;
                }
                throw ServiceException.INVALID_REQUEST("invalid fileAs value: " + fileAs, null);
            default:
            case ContactConstants.FA_LAST_C_FIRST:
                result.append(last);
                if (first.length() > 0 && last.length() > 0)
                    result.append(", ");
                result.append(first);
                break;
            case ContactConstants.FA_FIRST_LAST:
                result.append(first);
                if (first.length() > 0 && last.length() > 0)
                    result.append(' ');
                result.append(last);
                break;
            case ContactConstants.FA_COMPANY:
                result.append(company);
                break;
            case ContactConstants.FA_LAST_C_FIRST_COMPANY:
                result.append(last);
                if (first.length() > 0 && last.length() > 0)
                    result.append(", ");
                result.append(first);
                if (company.length() > 0)
                    result.append(" (").append(company).append(')');
                break;
            case ContactConstants.FA_FIRST_LAST_COMPANY:
                result.append(first);
                if (first.length() > 0 && last.length() > 0)
                    result.append(' ');
                result.append(last);
                if (company.length() > 0)
                    result.append(" (").append(company).append(')');
                break;
            case ContactConstants.FA_COMPANY_LAST_C_FIRST:
                result.append(company);
                if (first.length() > 0 || last.length() > 0) {
                    result.append(" (").append(last);
                    if (first.length() > 0 && last.length() > 0)
                        result.append(", ");
                    result.append(first).append(')');
                }
                break;
            case ContactConstants.FA_COMPANY_FIRST_LAST:
                result.append(company);
                if (first.length() > 0 || last.length() > 0) {
                    result.append(" (").append(first);
                    if (first.length() > 0 && last.length() > 0)
                        result.append(' ');
                    result.append(last).append(')');
                }
                break;
        }
        return result.toString().trim();
    }

    /**
     * Convert from a fileAs string to the internal fileAs format.
     *
     * @param attrs
     */
    public static void normalizeFileAs(Map<String, String> attrs) {
        String fileAs = attrs.get(ContactConstants.A_fullName);
        if (fileAs == null || fileAs.trim().length() == 0)
            return;

        String last = attrs.get(ContactConstants.A_lastName);
        last = last == null ? "" : last;
        String first = attrs.get(ContactConstants.A_firstName);
        first = first == null ? "" : first;
        String company = attrs.get(ContactConstants.A_company);
        company = company == null ? "" : company;
        String middle = attrs.get(ContactConstants.A_middleName);
        middle = middle == null ? "" : middle;

        //remove middle name as format is not defined ContactConstants
        if (!middle.isEmpty()) {
            String lastFirstMiddle = last + ", " + first + " " + middle;
            String firstMiddleLast = first + " " + middle + " " + last;
            if (fileAs.equals(lastFirstMiddle) || fileAs.equals(firstMiddleLast)) {
        	    fileAs = last + ", " + first;
            }
        }

        //ContactConstants.A_LAST_C_FIRST = 1
        StringBuilder sb = new StringBuilder();
        sb.append(last);
        if (last.length() > 0 && first.length() > 0) sb.append(", ");
        sb.append(first);
        if (sb.toString().equals(fileAs)) {
            attrs.put(ContactConstants.A_fileAs, Integer.valueOf(ContactConstants.FA_LAST_C_FIRST).toString());
            return;
        }

        //ContactConstants.A_FIRST_LAST = 2
        sb = new StringBuilder();
        sb.append(first);
        if (last.length() > 0 && first.length() > 0) sb.append(' ');
        sb.append(last);
        if (sb.toString().equals(fileAs)) {
            attrs.put(ContactConstants.A_fileAs, Integer.valueOf(ContactConstants.FA_FIRST_LAST).toString());
            return;
        }

        //ContactConstants.A_COMPANY = 3
        if (company.equals(fileAs)) {
            attrs.put(ContactConstants.A_fileAs, Integer.valueOf(ContactConstants.FA_COMPANY).toString());
            return;
        }

        //ContactConstants.A_LAST_C_FIRST_COMPANY = 4
        sb = new StringBuilder();
        sb.append(last);
        if (last.length() > 0 && first.length() > 0) sb.append(", ");
        sb.append(first);
        if (company.length() > 0) sb.append(" (").append(company).append(')');
        if (sb.toString().equals(fileAs)) {
            attrs.put(ContactConstants.A_fileAs, Integer.valueOf(ContactConstants.FA_LAST_C_FIRST_COMPANY).toString());
            return;
        }

        //ContactConstants.A_FIRST_LAST_COMPANY = 5
        sb = new StringBuilder();
        sb.append(first);
        if (last.length() > 0 && first.length() > 0) sb.append(' ');
        sb.append(last);
        if (company.length() > 0) sb.append(" (").append(company).append(')');
        if (sb.toString().equals(fileAs)) {
            attrs.put(ContactConstants.A_fileAs, Integer.valueOf(ContactConstants.FA_FIRST_LAST_COMPANY).toString());
            return;
        }

        //ContactConstants.A_COMPANY_LAST_C_FIRST = 6
        sb = new StringBuilder();
        sb.append(company);
        if (last.length() > 0 || first.length() > 0) {
            sb.append(" (").append(last);
            if (last.length() > 0 && first.length() > 0) sb.append(", ");
            sb.append(first).append(')');
        }
        if (sb.toString().equals(fileAs)) {
            attrs.put(ContactConstants.A_fileAs, Integer.valueOf(ContactConstants.FA_COMPANY_LAST_C_FIRST).toString());
            return;
        }

        //ContactConstants.A_COMPANY_FIRST_LAST = 7
        sb = new StringBuilder();
        sb.append(company);
        if (last.length() > 0 || first.length() > 0) {
            sb.append(" (").append(first);
            if (last.length() > 0 && first.length() > 0) sb.append(' ');
            sb.append(last).append(')');
        }
        if (sb.toString().equals(fileAs)) {
            attrs.put(ContactConstants.A_fileAs, Integer.valueOf(ContactConstants.FA_COMPANY_FIRST_LAST).toString());
            return;
        }

        //ContactConstants.A_EXPLICIT = 8
        attrs.put(ContactConstants.A_fileAs, Integer.valueOf(ContactConstants.FA_EXPLICIT).toString() + ':' + fileAs);
    }

    /**
     * Returns a list of all email address fields for this contact.
     */
    public List<String> getEmailAddresses() {
        return getEmailAddresses(emailFields, fields, DerefGroupMembersOption.INLINE_ONLY);
    }

    public List<String> getEmailAddresses(DerefGroupMembersOption derefGroupMemberOpt) {
        return getEmailAddresses(emailFields, fields, derefGroupMemberOpt);
    }

    public static final boolean isEmailField(String[] emailFields, String fieldName) {
        if (fieldName == null)
            return false;
        String lcField = fieldName.toLowerCase();
        for (String e : emailFields) {
            if (lcField.equals(e))
                return true;
        }
        if (lcField.equals(ContactConstants.A_dlist))
            return true;
        return false;
    }

    public static final List<String> getEmailAddresses(String[] fieldNames,
            Map<String, String> fields, DerefGroupMembersOption derefGroupMemberOpt) {
        List<String> result = new ArrayList<String>();
        for (String name : fieldNames) {
            String addr = fields.get(name);
            if (addr != null && !addr.trim().isEmpty()) {
                result.add(addr);
            }
        }

        if (derefGroupMemberOpt != DerefGroupMembersOption.NONE) {
            String encodedGroupMembers = fields.get(ContactConstants.A_groupMember);
            if (encodedGroupMembers != null) {
                try {
                    ContactGroup contactGroup = ContactGroup.init(encodedGroupMembers);
                    List<String> emailAddrs = contactGroup.getInlineEmailAddresses();
                    for (String addr : emailAddrs) {
                        result.add(addr);
                    }
                } catch (ServiceException e) {
                    ZimbraLog.contact.warn("unable to decode contact group", e);
                }
            }
        }

        return result;
    }

    @Override
    boolean isTaggable() {
        return true;
    }

    @Override
    boolean isCopyable() {
        return true;
    }

    @Override
    boolean isMovable() {
        return true;
    }

    @Override
    boolean isMutable() {
        return true;
    }

    @Override
    boolean canHaveChildren() {
        return false;
    }

    /**
     * Creates a new {@link Contact} and persists it to the database.
     * <p>
     * A real nonnegative item ID must be supplied from a previous call to {@link Mailbox#getNextItemId(int)}.
     *
     * @param id      The id for the new contact.
     * @param folder  The {@link Folder} to create the contact in.
     * @param mblob   The stored blob containing contact attachments.
     * @param pc      The contact's fields and values, plus attachments.
     * @param flags   Initial flagset
     * @param ntags    A serialized version of all {@link Tag}s to apply.
     * @perms {@link ACL#RIGHT_INSERT} on the folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><tt>mail.CANNOT_CONTAIN</tt> - if the target folder can't
     *        contain contacts
     *    <li><tt>service.INVALID_REQUEST</tt> - if no fields are specified
     *        for the contact
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul>
     * @see #canContain(byte)
     */
    static Contact create(int id, Folder folder, MailboxBlob mblob, ParsedContact pc, int flags, Tag.NormalizedTags ntags, CustomMetadata custom)
    throws ServiceException {
        if (folder == null || !folder.canContain(Type.CONTACT)) {
            throw MailServiceException.CANNOT_CONTAIN();
        }
        if (!folder.canAccess(ACL.RIGHT_INSERT)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        }
        Mailbox mbox = folder.getMailbox();
        mbox.updateContactCount(1);

        UnderlyingData data = new UnderlyingData();
        data.id = id;
        data.type = Type.CONTACT.toByte();
        data.folderId = folder.getId();
        if (!folder.inSpam() || mbox.getAccount().getBooleanAttr(Provisioning.A_zimbraJunkMessagesIndexingEnabled, false)) {
            data.indexId = IndexStatus.DEFERRED.id();
        }
        data.imapId = id;
        data.locator = mblob == null ? null : mblob.getLocator();
        data.setBlobDigest(pc.getDigest());
        data.size = pc.getSize();
        data.date = mbox.getOperationTimestamp();
        data.setFlags(flags | (pc.hasAttachment() ? Flag.BITMASK_ATTACHED : 0));
        data.setTags(ntags);
        data.metadata = encodeMetadata(DEFAULT_COLOR_RGB, 1, 1, custom, pc.getFields(), pc.getAttachments());
        data.contentChanged(mbox);

        if (ZimbraLog.mailop.isInfoEnabled()) {
            String email = "null";
            if (pc.getFields() != null) {
                email = pc.getFields().get(ContactConstants.A_email);
            }
            ZimbraLog.mailop.info("adding contact %s: id=%d, folderId=%d, folderName=%s.",
                email, data.id, folder.getId(), folder.getName());
        }

        new DbMailItem(mbox).setSender(getFileAsString(pc.getFields())).create(data);

        Contact contact = new Contact(mbox, data);
        contact.finishCreation(null);
        if (contact.fields.isEmpty()) {
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);
        }
        return contact;
    }

    @Override
    MailboxBlob setContent(StagedBlob staged, Object content) throws ServiceException, IOException {
        ZimbraLog.mailop.info("modifying contact %s: id=%d, folderId=%d, folderName=%s.",
                    get(ContactConstants.A_email), getId(), getFolderId(), getFolder().getName());
        return super.setContent(staged, content);
    }

    @Override
    public List<IndexDocument> generateIndexDataAsync(boolean indexAttachments) throws TemporaryIndexingException {
        try {
            ParsedContact pc = new ParsedContact(this);
            pc.analyze(getAccount(),indexAttachments);
            if (pc.hasTemporaryAnalysisFailure()) {
                throw new TemporaryIndexingException();
            }
            return pc.getLuceneDocuments(getAccount(), indexAttachments);
        } catch (ServiceException e) {
            ZimbraLog.index.error("Failed to index contact id=%d", getId());
            return Collections.emptyList();
        }
    }

    @Override
    void reanalyze(Object data, long newSize) throws ServiceException {
        if (!(data instanceof ParsedContact)) {
            throw ServiceException.FAILURE("cannot reanalyze non-ParsedContact object", null);
        }
        ParsedContact pc = (ParsedContact) data;

        markItemModified(Change.CONTENT | Change.DATE | Change.FLAGS);
        fields = pc.getFields();
        if (fields == null || fields.isEmpty()) {
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);
        }
        attachments = pc.getAttachments();
        if (attachments != null && attachments.size() > 0) {
            for (Attachment attach: attachments) {
                //refresh the data handler. the content rev changes which can change the blob filename/locator.
                //not safe to keep old data handler that may point to old blob
                attach.mDataHandler = new DataHandler(new AttachmentDataSource(this, attach.mPartName));
            }
        }
        mData.unsetFlag(Flag.FlagInfo.ATTACHED);
        if (pc.hasAttachment()) {
            mData.setFlag(Flag.FlagInfo.ATTACHED);
        }
        saveData(new DbMailItem(mMailbox).setSender(getFileAsString(fields)));
    }

    /** @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_READ} on the original item */
    @Override
    MailItem copy(Folder folder, int id, String uuid, MailItem parent) throws IOException, ServiceException {
        mMailbox.updateContactCount(1);
        return super.copy(folder, id, uuid, parent);
    }

    /** @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_READ} on the original item */
    @Override
    MailItem icopy(Folder folder, int copyId, String copyUuid) throws IOException, ServiceException {
        mMailbox.updateContactCount(1);
        return super.icopy(folder, copyId, copyUuid);
    }

    /** @perms {@link ACL#RIGHT_DELETE} on the item */
    @Override
    PendingDelete getDeletionInfo() throws ServiceException {
        PendingDelete info = super.getDeletionInfo();
        info.contacts = 1;
        return info;
    }

    @Override
    void decodeMetadata(Metadata meta) throws ServiceException {
        Metadata metaAttrs;
        if (meta.containsKey(Metadata.FN_FIELDS)) {
            // new version: fields are in their own subhash
            super.decodeMetadata(meta);
            metaAttrs = meta.getMap(Metadata.FN_FIELDS);

            MetadataList mlAttach = meta.getList(Metadata.FN_ATTACHMENTS, true);
            if (mlAttach != null) {
                attachments = new ArrayList<Attachment>(mlAttach.size());
                for (int i = 0; i < mlAttach.size(); i++) {
                    Metadata attachMeta = mlAttach.getMap(i);
                    String fieldName = attachMeta.get(Attachment.FN_FIELD);
                    String partName = attachMeta.get(Attachment.FN_PART);
                    int size = (int) attachMeta.getLong(Attachment.FN_SIZE);
                    DataHandler dh = new DataHandler(new AttachmentDataSource(this, partName));
                    Attachment attachment = new Attachment(dh, fieldName, size);
                    attachment.setPartName(partName);
                    attachments.add(attachment);
                }

            }
        } else {
            // version 8 or earlier; metadata is just the fields
            metaAttrs = meta;
        }

        fields = new HashMap<String, String>();
        for (Map.Entry<String, ?> entry : metaAttrs.asMap().entrySet()) {
            fields.put(entry.getKey(), entry.getValue().toString());
        }
    }

    @Override
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mRGBColor, mMetaVersion, mVersion, mExtendedData, fields, attachments);
    }

    private static String encodeMetadata(Color color, int metaVersion, int version, CustomMetadata custom, Map<String, String> fields, List<Attachment> attachments) {
        CustomMetadataList extended = (custom == null ? null : custom.asList());
        return encodeMetadata(new Metadata(), color, metaVersion, version, extended, fields, attachments).toString();
    }

    static Metadata encodeMetadata(Metadata meta, Color color, int metaVersion, int version, CustomMetadataList extended,
                                   Map<String, String> fields, List<Attachment> attachments) {
        meta.put(Metadata.FN_FIELDS, new Metadata(fields));
        if (attachments != null && !attachments.isEmpty()) {
            MetadataList mlist = new MetadataList();
            for (Attachment attach : attachments)
                mlist.add(attach.asMetadata());
            meta.put(Metadata.FN_ATTACHMENTS, mlist);
        }
        return MailItem.encodeMetadata(meta, color, null, metaVersion, version, extended);
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
        appendCommonMembers(helper);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            helper.add(entry.getKey(), entry.getValue());
        }
        return helper.toString();
    }

    public String getVCardUID() {
        return fields.get(ContactConstants.A_vCardUID);
    }

    public ListMultimap<String, VCardParamsAndValue> getUnknownVCardProps() {
        return decodeUnknownVCardProps(fields.get(ContactConstants.A_vCardXProps));
    }

    public void setUnknownVCardProps(ListMultimap<String, VCardParamsAndValue> xprops) {
        fields.put(ContactConstants.A_vCardXProps, encodeUnknownVCardProps(xprops));
    }

    // could be a GAL group (for GAL sync account) or a contact group
    // GAL group: members are stored in the member field, encoded as a json array.
    // contact group: members are stored in the groupMember field, encoded as Metadata
    public boolean isGroup() {
        return ContactConstants.TYPE_GROUP.equals(get(ContactConstants.A_type));
    }

    public static boolean isGroup(Map<String,? extends Object> attrs) {
        return ContactConstants.TYPE_GROUP.equals(attrs.get(ContactConstants.A_type));
    }

    public boolean isContactGroup() {
        return isGroup() && get(ContactConstants.A_groupMember) != null;
    }

    private static Set<String> SMIME_FIELDS = ImmutableSet.of(
            ContactConstants.A_userCertificate, ContactConstants.A_userSMIMECertificate);

    public static Set<String> getSMIMECertFields() {
        return SMIME_FIELDS;
    }

    public static boolean isSMIMECertField(String fieldName) {
        return SMIME_FIELDS.contains(fieldName);
    }

    private static final String ZMVAL = "ZMVAL";
    private static final String ZMVALENCODED = "{\"ZMVAL\":";
    // Use this when VCARD parameters are involved
    private static final String ZMPROP = "ZMPROP";
    private static final String ZMPROPENCODED = "{\"ZMPROP\":";
    private static final String ZMPARAM = "PAR";

    // xprops and other unknown VCARD properties are not automatically added to the Contact object
    // as a distinct attribute. the xprops are encoded into JSONObject,
    // then added as ContactConstants.a_vCardXProps attr to the map.

    public static ListMultimap<String, VCardParamsAndValue> decodeUnknownVCardProps(String xpropStr) {
        ListMultimap<String, VCardParamsAndValue> xprops = ArrayListMultimap.create();
        if (xpropStr == null || xpropStr.length() == 0) {
            return xprops;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(xpropStr);
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                String fieldValue = node.get(fieldName).asText();
                if (fieldValue.startsWith(ZMVALENCODED)) {
                    JsonNode zmVal = mapper.readTree(fieldValue);
                    JsonNode zmValValue = zmVal.get(ZMVAL);
                    if (null == zmValValue) {
                        ZimbraLog.mailop.info("Field %s has corrupt value='%s'", fieldName, fieldValue);
                        continue;
                    }
                    Iterator<JsonNode> values = zmValValue.iterator();
                    while (values.hasNext()) {
                        JsonNode value = values.next();
                        xprops.put(fieldName, new VCardParamsAndValue(value.asText()));
                    }
                } else if (fieldValue.startsWith(ZMPROPENCODED)) {
                    JsonNode zmProp = mapper.readTree(fieldValue);
                    JsonNode props = zmProp.get(ZMPROP);
                    if (null == props) {
                        ZimbraLog.mailop.info("Field %s has corrupt value='%s'", fieldName, fieldValue);
                        continue;
                    }
                    Iterator<JsonNode> values = props.iterator();
                    while (values.hasNext()) {
                        JsonNode value = values.next();
                        JsonNode zmValValue = value.get(ZMVAL);
                        if (null == zmValValue) {
                            ZimbraLog.mailop.info("Field %s has corrupt value='%s'", fieldName, fieldValue);
                            continue;
                        }
                        String val = zmValValue.asText();  // Can't be an array if using ZMPROP wrapper
                        Set<String> params = Sets.newHashSet();
                        JsonNode paramNode = value.get(ZMPARAM);
                        if (null != paramNode) {
                            Iterator<JsonNode> paramsIter = paramNode.iterator();
                            while (paramsIter.hasNext()) {
                                JsonNode paramValue = paramsIter.next();
                                params.add(paramValue.asText());
                            }
                        }
                        xprops.put(fieldName, new VCardParamsAndValue(val, params));
                    }
                } else {
                    xprops.put(fieldName, new VCardParamsAndValue(fieldValue));
                }
            }
        } catch (IOException e) {
            ZimbraLog.mailop.debug("can't get xprop %s", xpropStr, e);
        }
        return xprops;
    }

    public static String encodeUnknownVCardProps(ListMultimap<String, VCardParamsAndValue> xprops) {
        JSONObject jsonobj = new JSONObject();
        try {
            for (String propName : xprops.keySet()) {
                List<VCardParamsAndValue> paramsAndValueList = xprops.get(propName);
                if ((paramsAndValueList.size() == 1) && paramsAndValueList.get(0).getParams().isEmpty()) {
                    VCardParamsAndValue paramsAndValue = paramsAndValueList.get(0);
                    jsonobj.put(propName, paramsAndValue.getValue());
                } else {
                    boolean hasParams = false;
                    for (VCardParamsAndValue paramsAndValue : paramsAndValueList) {
                        if (!paramsAndValue.getParams().isEmpty()) {
                            hasParams = true;
                            break;
                        }
                    }
                    if (hasParams) {
                        jsonobj.put(propName, encodeAttrWithParamsAndValue(paramsAndValueList));
                    } else {
                        List<String> values = Lists.newArrayListWithCapacity(paramsAndValueList.size());
                        for (VCardParamsAndValue paramsAndValue : paramsAndValueList) {
                            values.add(paramsAndValue.getValue());
                        }
                        jsonobj.put(propName, Contact.encodeMultiValueAttr(values.toArray(new String[0])));
                    }
                }
            }
        } catch (JSONException e) {
            ZimbraLog.mailop.debug("can't encode xprops to JSONObject", e);
        }
        return jsonobj.toString();
    }

    public static String encodeAttrWithParamsAndValue(List<VCardParamsAndValue> paramsAndValueList)
    throws JSONException {
        JSONObject jsonObj = new JSONObject();
        for (VCardParamsAndValue paramsAndValue : paramsAndValueList) {
            JSONObject jsonProp = new JSONObject();
            jsonProp.put(ZMVAL, paramsAndValue.getValue());
            for (String param : paramsAndValue.getParams()) {
                jsonProp.append(ZMPARAM, param);
            }
            jsonObj.append(ZMPROP, jsonProp);
        }
        return jsonObj.toString();
    }

    public static String encodeMultiValueAttr(String[] attrs) throws JSONException {
        JSONObject jsonobj = new JSONObject();
        for (String s : attrs) {
            jsonobj.append(ZMVAL, s);
        }
        return jsonobj.toString();
    }
    public static boolean isMultiValueAttr(String attr) {
        return attr.startsWith(ZMVALENCODED);
    }
    public static String[] parseMultiValueAttr(String attr) throws JSONException {
        if (!isMultiValueAttr(attr))
            return new String[] { attr };
        JSONObject jsonobj = new JSONObject(attr);
        JSONArray array = jsonobj.getJSONArray(ZMVAL);
        String[] mv = new String[array.length()];
        for (int i = 0; i < mv.length; i++)
            mv[i] = array.getString(i);
        return mv;
    }
    public static JSONArray getMultiValueAttrArray(String attr) throws JSONException {
        if (!isMultiValueAttr(attr)) {
            JSONObject jsonobj = new JSONObject();
            jsonobj.append(ZMVAL, attr);
            return jsonobj.getJSONArray(ZMVAL);
        } else {
            JSONObject jsonobj = new JSONObject(attr);
            JSONArray array = jsonobj.getJSONArray(ZMVAL);
            return array;
        }
    }
}
