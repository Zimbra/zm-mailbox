/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Aug 23, 2004
 */
package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.session.PendingModifications.Change;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;


/**
 * @author dkarp
 */
public class Contact extends MailItem {

    /** "File as" setting: &nbsp;<tt>Last, First</tt> */
    public static final int FA_LAST_C_FIRST = 1;
    /** "File as" setting: &nbsp;<tt>First Last</tt> */
    public static final int FA_FIRST_LAST = 2;
    /** "File as" setting: &nbsp;<tt>Company</tt> */
    public static final int FA_COMPANY = 3;
    /** "File as" setting: &nbsp;<tt>Last, First (Company)</tt> */
    public static final int FA_LAST_C_FIRST_COMPANY = 4;
    /** "File as" setting: &nbsp;<tt>First Last (Company)</tt> */
    public static final int FA_FIRST_LAST_COMPANY = 5;
    /** "File as" setting: &nbsp;<tt>Company (Last, First)</tt> */
    public static final int FA_COMPANY_LAST_C_FIRST = 6;
    /** "File as" setting: &nbsp;<tt>Company (First Last)</tt> */
    public static final int FA_COMPANY_FIRST_LAST = 7;
    /** "File as" setting: <i>[explicitly specified "file as" string]</i> */
    public static final int FA_EXPLICIT = 8;

    /** The default "file as" setting: {@link #FA_LAST_C_FIRST}. */
    public  static final int FA_DEFAULT = FA_LAST_C_FIRST;
    private static final int FA_MAXIMUM = FA_EXPLICIT;

    // these are the "well known attrs". keep in sync with Attr enum below
    public static final String A_birthday = "birthday";
    public static final String A_callbackPhone = "callbackPhone";
    public static final String A_carPhone = "carPhone";
    public static final String A_company = "company";
    public static final String A_companyPhone = "companyPhone";
    public static final String A_department = "department";
    public static final String A_dlist = "dlist";
    public static final String A_email = "email";
    public static final String A_email2 = "email2";
    public static final String A_email3 = "email3";
    public static final String A_fileAs = "fileAs";
    public static final String A_firstName = "firstName";
    public static final String A_fullName = "fullName";
    public static final String A_homeCity = "homeCity";
    public static final String A_homeCountry = "homeCountry";
    public static final String A_homeFax = "homeFax";
    public static final String A_homePhone = "homePhone";
    public static final String A_homePhone2 = "homePhone2";
    public static final String A_homePostalCode = "homePostalCode";
    public static final String A_homeState = "homeState";
    public static final String A_homeStreet = "homeStreet";
    public static final String A_homeURL = "homeURL";
    public static final String A_image = "image";
    public static final String A_initials = "initials";
    public static final String A_isMyCard = "isMyCard";
    public static final String A_jobTitle = "jobTitle";
    public static final String A_lastName = "lastName";
    public static final String A_middleName = "middleName";
    public static final String A_mobilePhone = "mobilePhone";
    public static final String A_namePrefix = "namePrefix";
    public static final String A_nameSuffix = "nameSuffix";
    public static final String A_nickname = "nickname";
    public static final String A_notes = "notes";
    public static final String A_office = "office";
    public static final String A_otherCity = "otherCity";
    public static final String A_otherCountry = "otherCountry";
    public static final String A_otherFax = "otherFax";
    public static final String A_otherPhone = "otherPhone";
    public static final String A_otherPostalCode = "otherPostalCode";
    public static final String A_otherState = "otherState";
    public static final String A_otherStreet = "otherStreet";
    public static final String A_otherURL = "otherURL";
    public static final String A_pager = "pager";
    public static final String A_workCity = "workCity";
    public static final String A_workCountry = "workCountry";
    public static final String A_workFax = "workFax";
    public static final String A_workPhone = "workPhone";
    public static final String A_workPhone2 = "workPhone2";
    public static final String A_workPostalCode = "workPostalCode";
    public static final String A_workState = "workState";
    public static final String A_workStreet = "workStreet";
    public static final String A_workURL = "workURL";
    public static final String A_type = "type";
    // Comcast specific fields
    public static final String A_homeAddress = "homeAddress";
    public static final String A_imAddress1 = "imAddress1";
    public static final String A_imAddress2 = "imAddress2";
    public static final String A_workAddress = "workAddress";
    public static final String A_workEmail1 = "workEmail1";
    public static final String A_workEmail2 = "workEmail2";
    public static final String A_workEmail3 = "workEmail3";
    public static final String A_workMobile = "workMobile";
    public static final String A_workIM1 = "workIM1";
    public static final String A_workIM2 = "workIM2";
    public static final String A_workAltPhone = "workAltPhone";
    public static final String A_otherDepartment = "otherDepartment";
    public static final String A_otherOffice = "otherOffice";
    public static final String A_otherProfession = "otherProfession";
    public static final String A_otherAddress = "otherAddress";
    public static final String A_otherMgrName = "otherMgrName";
    public static final String A_otherAsstName = "otherAsstName";
    public static final String A_otherAnniversary = "otherAnniversary";
    public static final String A_otherCustom1 = "otherCustom1";
    public static final String A_otherCustom2 = "otherCustom2";
    public static final String A_otherCustom3 = "otherCustom3";
    public static final String A_otherCustom4 = "otherCustom4";
    // end

    public static final String TYPE_GROUP = "group";
 
    // these are the "well known attrs". keep in sync with A_* above.
    public enum Attr {
        assistantPhone,
        birthday,
        callbackPhone,
        carPhone,
        company,
        companyPhone,
        description,
        department,
        dlist,
        email,
        email2,
        email3,
        fileAs,
        firstName,
        fullName,
        homeCity,
        homeCountry,
        homeFax,
        homePhone,
        homePhone2,
        homePostalCode,
        homeState,
        homeStreet,
        homeURL,
        image,
        initials,
        jobTitle,
        lastName,
        middleName,
        mobilePhone,
        namePrefix,
        nameSuffix,
        nickname,
        notes,
        office,
        otherCity,
        otherCountry,
        otherFax,
        otherPhone,
        otherPostalCode,
        otherState,
        otherStreet,
        otherURL,
        pager,
        workCity,
        workCountry,
        workFax,
        workPhone,
        workPhone2,
        workPostalCode,
        workState,
        workStreet,
        workURL,
        type,
        homeAddress,
        imAddress1,
        imAddress2,
        workAddress,
        workEmail1,
        workEmail2,
        workEmail3,
        workMobile,
        workIM1,
        workIM2,
        workAltPhone,
        otherDepartment,
        otherOffice,
        otherProfession,
        otherAddress,
        otherMgrName,
        otherAsstName,
        otherAnniversary,
        otherCustom1,
        otherCustom2,
        otherCustom3,
        otherCustom4;
      
        public static Attr fromString(String s) throws ServiceException {
            try {
                return Attr.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid attr: "+s+", valid values: "+Arrays.asList(Attr.values()), e);
            }
        }

    }

    public static class Attachment implements DataSource {
        private byte[] mContent;
        private int mSize;
        private String mContentType;
        private String mFilename;
        private String mFieldName;
        private String mPartName;

        public Attachment(byte[] content, String ctype, String field, String filename) {
            mContent = content;
            mSize = content == null ? 0 : mContent.length;
            mContentType = ctype == null ? Mime.CT_APPLICATION_OCTET_STREAM : ctype.toLowerCase();
            mFieldName = field;
            mFilename = filename == null ? "unknown" : filename;
        }

        public Attachment(byte[] content, String ctype, String field, String filename, String part) {
            this(content, ctype, field, filename);
            setPartName(part);
        }

        Attachment(Metadata meta) throws ServiceException {
            this(null, meta.get("ctype"), meta.get("field"), meta.get("name"));
            setPartName(meta.get("part"));
            mSize = (int) meta.getLong("size");
        }

        public void setPartName(String name)  { mPartName = name; }
        public void clearContent()            { mContent = null; }

        public String getContentType()         { return mContentType; }
        public String getName()                { return mFieldName; }
        public InputStream getInputStream()    { return new ByteArrayInputStream(mContent); }
        public OutputStream getOutputStream()  { throw new UnsupportedOperationException(); }

        public byte[] getContent()   { return mContent; }
        public int getSize()         { return mSize; }
        public String getFilename()  { return mFilename; }
        public String getPartName()  { return mPartName; }

        public byte[] getContent(Contact con) throws ServiceException, IOException, MessagingException {
            if (mContent != null)
                return mContent;
            return ByteUtil.getContent(Mime.getMimePart(con.getMimeMessage(false), mPartName).getInputStream(), mSize);
        }

        Metadata asMetadata() {
            return new Metadata().put("size", mSize).put("name", mFilename).put("part", mPartName).put("ctype", mContentType).put("field", mFieldName);
        }

        @Override public String toString() {
            return new StringBuilder(mFieldName).append(" [").append(mContentType).append(", ").append(mSize).append("B]").toString();
        }
    }


    /** Relates contact fields (<tt>"firstName"</tt>) to this contact's
     *  values (<tt>"John"</tt>). */
    private Map<String, String> mFields;
    private List<Attachment> mAttachments;

    public Contact(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_CONTACT)
            throw new IllegalArgumentException();
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
    public Map<String, String> getFields() {
        return new HashMap<String, String>(mFields);
    }

    /** Returns a list of all the contact's attachments.  If the contact has
     *  no attachments in its blob, an empty list is returned. */
    public List<Attachment> getAttachments() {
        if (mAttachments == null)
            return Collections.emptyList();
        return new ArrayList<Attachment>(mAttachments);
    }

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
        String fileAs = fields.get(A_fileAs);
        String[] fileParts = (fileAs == null ? null : fileAs.split(":", 2));
        int fileAsInt = FA_DEFAULT;
        if (fileParts != null) {
            try {
                fileAsInt = Integer.parseInt(fileParts[0]);
                if (fileAsInt < 0 || fileAsInt > FA_MAXIMUM)
                    throw ServiceException.INVALID_REQUEST("invalid fileAs value: " + fileAs, null);
            } catch (NumberFormatException e) {
                throw ServiceException.INVALID_REQUEST("invalid fileAs value: " + fileAs, null);
            }
        }

        String company = fields.get(A_company);
        if (company == null)
            company = "";
        String first = fields.get(A_firstName);
        if (first == null)
            first = "";
        String last = fields.get(A_lastName);
        if (last == null)
            last = "";
        
        StringBuilder result = new StringBuilder();
        switch (fileAsInt) {
            case FA_EXPLICIT:
                if (fileParts.length == 2 && !fileParts[1].trim().equals("")) {
                    result.append(fileParts[1].trim());
                    break;
                }
                throw ServiceException.INVALID_REQUEST("invalid fileAs value: " + fileAs, null);
            default:
            case FA_LAST_C_FIRST:
                result.append(last);
                if (first.length() > 0 && last.length() > 0)
                    result.append(", ");
                result.append(first);
                break;
            case FA_FIRST_LAST:
                result.append(first);
                if (first.length() > 0 && last.length() > 0)
                    result.append(' ');
                result.append(last);
                break;
            case FA_COMPANY:
                result.append(company);
                break;
            case FA_LAST_C_FIRST_COMPANY:
                result.append(last);
                if (first.length() > 0 && last.length() > 0)
                    result.append(", ");
                result.append(first);
                if (company.length() > 0)
                    result.append(" (").append(company).append(')');
                break;
            case FA_FIRST_LAST_COMPANY:
                result.append(first);
                if (first.length() > 0 && last.length() > 0)
                    result.append(' ');
                result.append(last);
                if (company.length() > 0)
                    result.append(" (").append(company).append(')');
                break;
            case FA_COMPANY_LAST_C_FIRST:
                result.append(company);
                if (first.length() > 0 || last.length() > 0) {
                    result.append(" (").append(last);
                    if (first.length() > 0 && last.length() > 0)
                        result.append(", ");
                    result.append(first).append(')');
                }
                break;
            case FA_COMPANY_FIRST_LAST:
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
		String fileAs = attrs.get(A_fullName);
		if (fileAs == null || fileAs.trim().length() == 0)
			return;

        String last = attrs.get(A_lastName);
        last = last == null ? "" : last;
        String first = attrs.get(A_firstName);
        first = first == null ? "" : first;
        String company = attrs.get(A_company);
        company = company == null ? "" : company;
        
        //FA_LAST_C_FIRST = 1
        StringBuilder sb = new StringBuilder();
        sb.append(last);
        if (last.length() > 0 && first.length() > 0) sb.append(", ");
        sb.append(first);
        if (sb.toString().equals(fileAs)) {
        	attrs.put(A_fileAs, new Integer(FA_LAST_C_FIRST).toString());
        	return;
        }
        
        //FA_FIRST_LAST = 2
        sb = new StringBuilder();
        sb.append(first);
        if (last.length() > 0 && first.length() > 0) sb.append(' ');
        sb.append(last);
        if (sb.toString().equals(fileAs)) {
        	attrs.put(A_fileAs, new Integer(FA_FIRST_LAST).toString());
        	return;
        }
        
        //FA_COMPANY = 3
        if (company.equals(fileAs)) {
        	attrs.put(A_fileAs, new Integer(FA_COMPANY).toString());
        	return;
        }

        //FA_LAST_C_FIRST_COMPANY = 4
        sb = new StringBuilder();
        sb.append(last);
        if (last.length() > 0 && first.length() > 0) sb.append(", ");
        sb.append(first);
        if (company.length() > 0) sb.append(" (").append(company).append(')');
        if (sb.toString().equals(fileAs)) {
        	attrs.put(A_fileAs, new Integer(FA_LAST_C_FIRST_COMPANY).toString());
        	return;
        }

        //FA_FIRST_LAST_COMPANY = 5
        sb = new StringBuilder();
        sb.append(first);
        if (last.length() > 0 && first.length() > 0) sb.append(' ');
        sb.append(last);
        if (company.length() > 0) sb.append(" (").append(company).append(')');
        if (sb.toString().equals(fileAs)) {
        	attrs.put(A_fileAs, new Integer(FA_FIRST_LAST_COMPANY).toString());
        	return;
        }
        
        //FA_COMPANY_LAST_C_FIRST = 6
        sb = new StringBuilder();
        sb.append(company);
        if (last.length() > 0 || first.length() > 0) {
            sb.append(" (").append(last);
            if (last.length() > 0 && first.length() > 0) sb.append(", ");
            sb.append(first).append(')');
        }
        if (sb.toString().equals(fileAs)) {
        	attrs.put(A_fileAs, new Integer(FA_COMPANY_LAST_C_FIRST).toString());
        	return;
        }
        
        //FA_COMPANY_FIRST_LAST = 7
        sb = new StringBuilder();
        sb.append(company);
        if (last.length() > 0 || first.length() > 0) {
            sb.append(" (").append(first);
            if (last.length() > 0 && first.length() > 0) sb.append(' ');
            sb.append(last).append(')');
        }
        if (sb.toString().equals(fileAs)) {
        	attrs.put(A_fileAs, new Integer(FA_COMPANY_FIRST_LAST).toString());
        	return;
        }

        //FA_EXPLICIT = 8
        attrs.put(A_fileAs, new Integer(FA_EXPLICIT).toString() + ':' + fileAs);
    }

    /** The list of all "email" fields in the contact's map. */
    private static final String[] EMAIL_FIELDS = new String[] { A_email, A_email2, A_email3 };

    /** Returns a list of all email address fields for this contact.  This is used
     *  by {@link com.zimbra.cs.index.Indexer#indexContact} to populate the
     *  "To" field with the contact's email addresses. */
    public List<String> getEmailAddresses() {
        ArrayList<String> result = new ArrayList<String>();
        for (String field : EMAIL_FIELDS) {
            String value = mFields.get(field);
            if (value != null && !value.trim().equals(""))
                result.add(value);
        }

        // if the dlist is set, return it as a single value
        String dlist = get(A_dlist);
        if (dlist != null)
            result.add(dlist);
        
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
     * @param id        The id for the new contact.
     * @param folder    The {@link Folder} to create the contact in.
     * @param volumeId  The volume to persist any attachments to.
     * @param pc        The contact's fields and values, plus attachments.
     * @param tags      A serialized version of all {@link Tag}s to apply.
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
    static Contact create(int id, Folder folder, short volumeId, ParsedContact pc, String tags) throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_CONTACT))
            throw MailServiceException.CANNOT_CONTAIN();
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");

        Mailbox mbox = folder.getMailbox();
        mbox.updateContactCount(1);

        // XXX: should maintain size on contacts with attachments
        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = TYPE_CONTACT;
        data.folderId    = folder.getId();
        if (!folder.inSpam() || mbox.getAccount().getBooleanAttr(Provisioning.A_zimbraJunkMessagesIndexingEnabled, false))
            data.indexId = id;
        data.imapId      = id;
        data.volumeId    = volumeId;
        data.setBlobDigest(pc.getDigest());
        data.date        = mbox.getOperationTimestamp();
        data.flags       = pc.hasAttachment() ? Flag.BITMASK_ATTACHED : 0;
        data.tags        = Tag.tagsToBitmask(tags);
        data.sender      = getFileAsString(pc.getFields());
        data.metadata    = encodeMetadata(DEFAULT_COLOR, 1, pc.getFields(), pc.getAttachments());
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        Contact con = new Contact(mbox, data);
        con.finishCreation(null);
        if (con.mFields.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);
        return con;
    }

    @Override public List<org.apache.lucene.document.Document> generateIndexData(boolean doConsistencyCheck) throws ServiceException {
        synchronized(mMailbox) {
            ParsedContact pc = new ParsedContact(this);
            return pc.getLuceneDocuments(mMailbox);
        }
    }

    @Override void reanalyze(Object data) throws ServiceException {
        if (!(data instanceof ParsedContact))
            throw ServiceException.FAILURE("cannot reanalyze non-ParsedContact object", null);

        ParsedContact pc = (ParsedContact) data;

        markItemModified(Change.MODIFIED_CONTENT | Change.MODIFIED_DATE | Change.MODIFIED_FLAGS);

        mFields = pc.getFields();
        if (mFields == null || mFields.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);

        mAttachments = pc.getAttachments();
        if (mAttachments != null) {
            // don't hold onto attachment content in memory
            for (Attachment attach : mAttachments)
                attach.clearContent();
        }

        mData.flags &= ~Flag.BITMASK_ATTACHED;
        if (pc.hasAttachment())
            mData.flags |= Flag.BITMASK_ATTACHED;

        saveData(getFileAsString(mFields));
    }

    /** Alters an existing contact's fields.  Depending on the value of the
     *  <tt>replace</tt> parameter, will either modify the existing fields
     *  or completely replace the old ones with the supplied <tt>Map</tt>.
     *  Blank field values cause the field to be deleted from a contact.
     *  The resulting contact must have at least one non-blank field value.
     * 
     * @param fields   The set of contact fields.
     * @perms {@link ACL#RIGHT_WRITE} on the contact
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><tt>service.INVALID_REQUEST</tt> - if the resulting set of
     *        contact fields is empty
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul> */
    void setFields(ParsedContact pc) throws ServiceException {
        if (!canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the contact");

        markItemModified(Change.MODIFIED_CONTENT | Change.MODIFIED_DATE);

        mFields = pc.getFields();
        if (mFields == null || mFields.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);

    	// XXX: should update mData.size and Mailbox.size and folder.size
        addRevision(false);

        mData.date = mMailbox.getOperationTimestamp();
        mData.contentChanged(mMailbox);
        saveData(getFileAsString());
	}

    /** @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_READ} on the original item */
    @Override MailItem copy(Folder folder, int id, int parentId, short destVolumeId) throws IOException, ServiceException {
        mMailbox.updateContactCount(1);
        return super.copy(folder, id, parentId, destVolumeId);
    }

    /** @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_READ} on the original item */
    @Override MailItem icopy(Folder folder, int copyId, short destVolumeId) throws IOException, ServiceException {
        mMailbox.updateContactCount(1);
        return super.icopy(folder, copyId, destVolumeId);
    }

    /** @perms {@link ACL#RIGHT_DELETE} on the item */
    @Override PendingDelete getDeletionInfo() throws ServiceException {
        PendingDelete info = super.getDeletionInfo();
        info.contacts = 1;
        return info;
    }


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
                for (int i = 0; i < mlAttach.size(); i++)
                    mAttachments.add(new Attachment(mlAttach.getMap(i)));
            }
        }

        mFields = new HashMap<String, String>();
        for (Iterator it = metaAttrs.asMap().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            mFields.put(entry.getKey().toString(), entry.getValue().toString());
        }
    }

    @Override Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor, mVersion, mFields, mAttachments);
    }

    private static String encodeMetadata(byte color, int version, Map<String, String> fields, List<Attachment> attachments) {
        return encodeMetadata(new Metadata(), color, version, fields, attachments).toString();
    }

    static Metadata encodeMetadata(Metadata meta, byte color, int version, Map<String, String> fields, List<Attachment> attachments) {
        meta.put(Metadata.FN_FIELDS, new Metadata(fields));
        if (attachments != null && !attachments.isEmpty()) {
            MetadataList mlist = new MetadataList();
            for (Attachment attach : attachments)
                mlist.add(attach.asMetadata());
            meta.put(Metadata.FN_ATTACHMENTS, mlist);
        }
        return MailItem.encodeMetadata(meta, color, version);
    }


    @Override public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("contact: {");
        appendCommonMembers(sb);
        for (Map.Entry entry : mFields.entrySet())
            sb.append(", ").append(entry.getKey()).append(": ").append(entry.getValue());
        sb.append("}");
        return sb.toString();
    }
}
