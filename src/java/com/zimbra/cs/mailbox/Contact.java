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
 * Created on Aug 23, 2004
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

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.EntryCacheDataKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.MailboxBlob;

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
        
        public void setPartName(String name)  { mPartName = name; }
        
        public String getContentType()        { return mDataHandler.getContentType(); }
        public String getName()               { return mFieldName; }
        public int getSize()                  { return mSize; }
        
        /**
         * Returns an <tt>InputStream</tt> to this attachment's content, or <tt>null</tt>
         * if there is no content.
         */
        public InputStream getInputStream()
        throws IOException {
            if (mDataHandler != null) {
                return mDataHandler.getInputStream();
            }
            return null;
        }
        
        public OutputStream getOutputStream()  { throw new UnsupportedOperationException(); }

        /**
         * Returns this attachment's content, or <tt>null</tt>.
         */
        public byte[] getContent()
        throws IOException {
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
        
        public String getFilename()  { return mDataHandler.getName(); }
        public String getPartName()  { return mPartName; }
        public DataHandler getDataHandler() { return mDataHandler; }

        private static final String FN_SIZE = "size", FN_NAME = "name", FN_PART = "part", FN_CTYPE = "ctype", FN_FIELD = "field";

        Metadata asMetadata() {
            return new Metadata().put(FN_SIZE, mSize).put(FN_NAME, getFilename()).put(FN_PART, mPartName).put(FN_CTYPE, getContentType()).put(FN_FIELD, mFieldName);
        }

        @Override public String toString() {
            return new StringBuilder(mFieldName).append(" [").append(getContentType()).append(", ").append(mSize).append("B]").toString();
        }
    }


    /** Relates contact fields (<tt>"firstName"</tt>) to this contact's
     *  values (<tt>"John"</tt>). */
    private Map<String, String> mFields;
    private List<Attachment> mAttachments;
    
    // The list of all *simple* "email" fields in the contact's map
    // IMPORTANT NOTE - does not include the Contact Group 'dlist' entry, which is 
    // * a multi-value entry (comma-separated) 
    private static final String[] EMAIL_FIELDS = new String[] { 
        ContactConstants.A_email, 
        ContactConstants.A_email2, 
        ContactConstants.A_email3, 
        ContactConstants.A_workEmail1, 
        ContactConstants.A_workEmail2, 
        ContactConstants.A_workEmail3 };
    
    private String[] mEmailFields; 


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
    
    public Contact(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_CONTACT)
            throw new IllegalArgumentException();
        
        mEmailFields = getEmailFields(getAccount());
    }

    @Override public String getSender() {
        try {
            return getFileAsString();
        } catch (ServiceException e) {
            return "";
        }
    }

    /** Returns a single field from the contact's field/value pairs. */
    public String get(String fieldName) {
        return mFields.get(fieldName);
    }

    /** Returns a new <tt>Map</tt> containing all the contact's
     *  field/value pairs. */
    public Map<String, String> getAllFields() {
        return new HashMap<String, String>(mFields);
    }
    
    /** Returns a new <tt>Map</tt> containing all the visible
     *  field/value pairs in the contact. */
    public Map<String, String> getFields() {
        HashMap<String, String> fields = new HashMap<String, String>(mFields);
        try {
            String hiddenAttrList = Provisioning.getInstance().getLocalServer().getContactHiddenAttributes();
            if (hiddenAttrList != null) {
                for (String attr : hiddenAttrList.split(",")) {
                    fields.remove(attr);
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.mailop.warn("can't get A_zimbraContactHiddenAttributes", e);
        }
        return fields;
    }

    /** Returns a list of all the contact's attachments.  If the contact has
     *  no attachments in its blob, an empty list is returned. */
    public List<Attachment> getAttachments() {
        if (mAttachments == null)
            return Collections.emptyList();
        return new ArrayList<Attachment>(mAttachments);
    }

    /**
     * Returns the <tt>MimeMessage</tt> for this contact.
     * @throws ServiceException if no <tt>MimeMessage</tt> exists or there
     * was an error retrieving it
     */
    public MimeMessage getMimeMessage(boolean runConverters) throws ServiceException {
        return MessageCache.getMimeMessage(this, runConverters);
    }

    /** Returns the "file as" string used for sort and listing purposes.
     *  This value is derived by using the "<tt>fileAs</tt>" contact field
     *  value to select a standard or custom formatting to apply to the
     *  contact's fields.  Supported <tt>fileAs</tt> values are:<ul>
     *    <li><tt>1</tt> - Last, First
     *    <li><tt>2</tt> - First Last
     *    <li><tt>3</tt> - Company
     *    <li><tt>4</tt> - Last, First (Company)
     *    <li><tt>5</tt> - First Last (Company)
     *    <li><tt>6</tt> - Company (Last, First)
     *    <li><tt>7</tt> - Company (First Last)
     *    <li><tt>8:your name here</tt> - The string "your name here"</ul>
     *  When a <tt>fileAs</tt> value is not specified, <tt>1</tt> is used as
     *  the default. */
    public String getFileAsString() throws ServiceException {
        return getFileAsString(mFields);
    }

    public static String getFileAsString(Map<String, String> fields) throws ServiceException {
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

        String company = fields.get(ContactConstants.A_company);
        if (company == null)
            company = "";
        String first = fields.get(ContactConstants.A_firstName);
        if (first == null)
            first = "";
        String last = fields.get(ContactConstants.A_lastName);
        if (last == null)
            last = "";
        
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
        
        //ContactConstants.A_LAST_C_FIRST = 1
        StringBuilder sb = new StringBuilder();
        sb.append(last);
        if (last.length() > 0 && first.length() > 0) sb.append(", ");
        sb.append(first);
        if (sb.toString().equals(fileAs)) {
        	attrs.put(ContactConstants.A_fileAs, new Integer(ContactConstants.FA_LAST_C_FIRST).toString());
        	return;
        }
        
        //ContactConstants.A_FIRST_LAST = 2
        sb = new StringBuilder();
        sb.append(first);
        if (last.length() > 0 && first.length() > 0) sb.append(' ');
        sb.append(last);
        if (sb.toString().equals(fileAs)) {
        	attrs.put(ContactConstants.A_fileAs, new Integer(ContactConstants.FA_FIRST_LAST).toString());
        	return;
        }
        
        //ContactConstants.A_COMPANY = 3
        if (company.equals(fileAs)) {
        	attrs.put(ContactConstants.A_fileAs, new Integer(ContactConstants.FA_COMPANY).toString());
        	return;
        }

        //ContactConstants.A_LAST_C_FIRST_COMPANY = 4
        sb = new StringBuilder();
        sb.append(last);
        if (last.length() > 0 && first.length() > 0) sb.append(", ");
        sb.append(first);
        if (company.length() > 0) sb.append(" (").append(company).append(')');
        if (sb.toString().equals(fileAs)) {
        	attrs.put(ContactConstants.A_fileAs, new Integer(ContactConstants.FA_LAST_C_FIRST_COMPANY).toString());
        	return;
        }

        //ContactConstants.A_FIRST_LAST_COMPANY = 5
        sb = new StringBuilder();
        sb.append(first);
        if (last.length() > 0 && first.length() > 0) sb.append(' ');
        sb.append(last);
        if (company.length() > 0) sb.append(" (").append(company).append(')');
        if (sb.toString().equals(fileAs)) {
        	attrs.put(ContactConstants.A_fileAs, new Integer(ContactConstants.FA_FIRST_LAST_COMPANY).toString());
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
        	attrs.put(ContactConstants.A_fileAs, new Integer(ContactConstants.FA_COMPANY_LAST_C_FIRST).toString());
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
        	attrs.put(ContactConstants.A_fileAs, new Integer(ContactConstants.FA_COMPANY_FIRST_LAST).toString());
        	return;
        }

        //ContactConstants.A_EXPLICIT = 8
        attrs.put(ContactConstants.A_fileAs, new Integer(ContactConstants.FA_EXPLICIT).toString() + ':' + fileAs);
    }

    /** Returns a list of all email address fields for this contact.  This is used
     *  by {@link com.zimbra.cs.index.Indexer#indexContact} to populate the
     *  "To" field with the contact's email addresses. */
    public List<String> getEmailAddresses() {
        return getEmailAddresses(mEmailFields, mFields);
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
    
    public static final List<String> getEmailAddresses(String[] emailFields, Map<String, String> fields) {
        ArrayList<String> result = new ArrayList<String>();
        for (String field : emailFields) {
            String value = fields.get(field);
            if (value != null && !value.trim().equals(""))
                result.add(value);
        }

        // if the dlist is set, return it as a single value
        String dlist = fields.get(ContactConstants.A_dlist);
        if (dlist != null) {
            String addrs[] = dlist.split(",");
            for (String s : addrs) {
                result.add(s.trim());
            }
        }
        return result;
    }

    @Override boolean isTaggable()      { return true; }
    @Override boolean isCopyable()      { return true; }
    @Override boolean isMovable()       { return true; }
    @Override boolean isMutable()       { return true; }
    @Override boolean isIndexed()       { return true; }
    @Override boolean canHaveChildren() { return false; }


    /** Creates a new <tt>Contact</tt> and persists it to the database.
     *  A real nonnegative item ID must be supplied from a previous call to
     *  {@link Mailbox#getNextItemId(int)}.
     * 
     * @param id      The id for the new contact.
     * @param folder  The {@link Folder} to create the contact in.
     * @param mblob   The stored blob containing contact attachments.
     * @param pc      The contact's fields and values, plus attachments.
     * @param flags   Initial flagset
     * @param tags    A serialized version of all {@link Tag}s to apply.
     * @perms {@link ACL#RIGHT_INSERT} on the folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><tt>mail.CANNOT_CONTAIN</tt> - if the target folder can't
     *        contain contacts
     *    <li><tt>service.INVALID_REQUEST</tt> - if no fields are specified
     *        for the contact
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul>
     * @see #canContain(byte) */
    static Contact create(int id, Folder folder, MailboxBlob mblob, ParsedContact pc, int flags, String tags, CustomMetadata custom)
    throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_CONTACT))
            throw MailServiceException.CANNOT_CONTAIN();
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");

        Mailbox mbox = folder.getMailbox();
        mbox.updateContactCount(1);

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = TYPE_CONTACT;
        data.folderId    = folder.getId();
        if (!folder.inSpam() || mbox.getAccount().getBooleanAttr(Provisioning.A_zimbraJunkMessagesIndexingEnabled, false))
            data.indexId = mbox.generateIndexId(id);
        data.imapId      = id;
        data.locator     = mblob == null ? null : mblob.getLocator();
        data.setBlobDigest(pc.getDigest());
        data.size        = pc.getSize();
        data.date        = mbox.getOperationTimestamp();
        data.flags       = flags | (pc.hasAttachment() ? Flag.BITMASK_ATTACHED : 0);
        data.tags        = Tag.tagsToBitmask(tags);
        data.metadata    = encodeMetadata(DEFAULT_COLOR_RGB, 1, custom, pc.getFields(), pc.getAttachments());
        data.contentChanged(mbox);
        
        if (ZimbraLog.mailop.isInfoEnabled()) {
            String email = "null";
            if (pc.getFields() != null)
                email = pc.getFields().get(ContactConstants.A_email);
            ZimbraLog.mailop.info("adding contact %s: id=%d, folderId=%d, folderName=%s.",
                email, data.id, folder.getId(), folder.getName());
        }
        
        DbMailItem.create(mbox, data, getFileAsString(pc.getFields()));

        Contact con = new Contact(mbox, data);
        con.finishCreation(null);
        if (con.mFields.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);
        return con;
    }

    @Override public List<IndexDocument> generateIndexData(boolean doConsistencyCheck) throws TemporaryIndexingException {
        synchronized (mMailbox) {
            try {
                ParsedContact pc = new ParsedContact(this);
                pc.analyze(mMailbox);
                if (pc.hasTemporaryAnalysisFailure())
                    throw new TemporaryIndexingException();
                return pc.getLuceneDocuments(mMailbox);
            } catch (TemporaryIndexingException tie) {
                throw tie;
            } catch (Exception e) {
                return new ArrayList<IndexDocument>(); 
            }
        }
    }

    @Override void reanalyze(Object data, long newSize) throws ServiceException {
        if (!(data instanceof ParsedContact))
            throw ServiceException.FAILURE("cannot reanalyze non-ParsedContact object", null);

        ParsedContact pc = (ParsedContact) data;

        markItemModified(Change.MODIFIED_CONTENT | Change.MODIFIED_DATE | Change.MODIFIED_FLAGS);

        mFields = pc.getFields();
        if (mFields == null || mFields.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);

        mAttachments = pc.getAttachments();

        mData.flags &= ~Flag.BITMASK_ATTACHED;
        if (pc.hasAttachment())
            mData.flags |= Flag.BITMASK_ATTACHED;

        saveData(getFileAsString(mFields));
    }

    /** @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_READ} on the original item */
    @Override MailItem copy(Folder folder, int id, int parentId) throws IOException, ServiceException {
        mMailbox.updateContactCount(1);
        return super.copy(folder, id, parentId);
    }

    /** @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_READ} on the original item */
    @Override MailItem icopy(Folder folder, int copyId) throws IOException, ServiceException {
        mMailbox.updateContactCount(1);
        return super.icopy(folder, copyId);
    }

    /** @perms {@link ACL#RIGHT_DELETE} on the item */
    @Override PendingDelete getDeletionInfo() throws ServiceException {
        PendingDelete info = super.getDeletionInfo();
        info.contacts = 1;
        return info;
    }


    @SuppressWarnings("unchecked")
    @Override void decodeMetadata(Metadata meta) throws ServiceException {
        Metadata metaAttrs;
        if (meta.getVersion() <= 8) {
            // old version: metadata is just the fields
            metaAttrs = meta;
        } else {
            // new version: fields are in their own subhash
            super.decodeMetadata(meta);
            metaAttrs = meta.getMap(Metadata.FN_FIELDS);

            MetadataList mlAttach = meta.getList(Metadata.FN_ATTACHMENTS, true);
            if (mlAttach != null) {
                mAttachments = new ArrayList<Attachment>(mlAttach.size());
                for (int i = 0; i < mlAttach.size(); i++) {
                    Metadata attachMeta = mlAttach.getMap(i);
                    String fieldName = attachMeta.get(Attachment.FN_FIELD);
                    String partName = attachMeta.get(Attachment.FN_PART);
                    int size = (int) attachMeta.getLong(Attachment.FN_SIZE);
                    DataHandler dh = new DataHandler(new AttachmentDataSource(this, partName));
                    Attachment attachment = new Attachment(dh, fieldName, size);
                    attachment.setPartName(partName);
                    mAttachments.add(attachment);
                }

            }
        }

        mFields = new HashMap<String, String>();
        for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) metaAttrs.asMap()).entrySet())
            mFields.put(entry.getKey().toString(), entry.getValue().toString());
    }

    @Override Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mRGBColor, mVersion, mExtendedData, mFields, mAttachments);
    }

    private static String encodeMetadata(Color color, int version, CustomMetadata custom, Map<String, String> fields, List<Attachment> attachments) {
        CustomMetadataList extended = (custom == null ? null : custom.asList());
        return encodeMetadata(new Metadata(), color, version, extended, fields, attachments).toString();
    }

    static Metadata encodeMetadata(Metadata meta, Color color, int version, CustomMetadataList extended,
                                   Map<String, String> fields, List<Attachment> attachments) {
        meta.put(Metadata.FN_FIELDS, new Metadata(fields));
        if (attachments != null && !attachments.isEmpty()) {
            MetadataList mlist = new MetadataList();
            for (Attachment attach : attachments)
                mlist.add(attach.asMetadata());
            meta.put(Metadata.FN_ATTACHMENTS, mlist);
        }
        return MailItem.encodeMetadata(meta, color, version, extended);
    }


    @Override public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("contact: {");
        appendCommonMembers(sb);
        for (Map.Entry<String, String> entry : mFields.entrySet())
            sb.append(", ").append(entry.getKey()).append(": ").append(entry.getValue());
        sb.append("}");
        return sb.toString();
    }
    
    public String getVCardUID() {
        return mFields.get(ContactConstants.A_vCardUID);
    }
    
    public String getXProp(String xprop) {
        return getXProps().get(xprop);
    }
    
    public Map<String,String> getXProps() {
        return decodeXProps(mFields.get(ContactConstants.A_vCardXProps));
    }
    
    public boolean isGroup() {
        return ContactConstants.TYPE_GROUP.equals(get(ContactConstants.A_type));
    }
    
    public static boolean isGroup(Map<String,? extends Object> attrs) {
        return ContactConstants.TYPE_GROUP.equals((String) attrs.get(ContactConstants.A_type));
    }
    
    public static Map<String,String> decodeXProps(String xpropStr) {
        HashMap<String,String> xprops = new HashMap<String,String>();
        if (xpropStr == null || xpropStr.length() == 0)
            return xprops;
        try {
            JSONObject xpropObj = new JSONObject(xpropStr);
            @SuppressWarnings("unchecked")
            Iterator iter = xpropObj.keys();
            while (iter.hasNext()) {
                String key = (String)iter.next();
                xprops.put(key, xpropObj.get(key).toString());
            }
        } catch (JSONException e) {
            ZimbraLog.mailop.debug("can't get xprop %s", xpropStr, e);
        }
        return xprops;
    }
    
    // xprops are not automatically added to the Contact object
    // as a distinct attribute. the xprops are encoded into JSONObject, 
    // then added as ContactConstants.a_vCardXProps attr to the map.
    public static String encodeXProps(Map<String,String> xprops) {
        JSONObject jsonobj = new JSONObject();
        try {
            for (String s : xprops.keySet())
                jsonobj.put(s, xprops.get(s));
        } catch (JSONException e) {
            ZimbraLog.mailop.debug("can't encode xprops to JSONObject", e);
        }
        return jsonobj.toString();
    }
    
    private static final String ZMVAL = "ZMVAL";
    private static final String ZMVALENCODED = "{\"ZMVAL\":";

    public static String encodeMultiValueAttr(String[] attrs) throws JSONException {
		JSONObject jsonobj = new JSONObject();
		for (String s : attrs)
			jsonobj.append(ZMVAL, s);
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
