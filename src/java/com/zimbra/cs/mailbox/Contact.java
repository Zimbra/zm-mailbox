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
 * Created on Aug 23, 2004
 */
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.StringUtil;


/**
 * @author dkarp
 */
public class Contact extends MailItem {

    /** "File as" setting: &nbsp;<code>Last, First</code> */
    private static final int FA_LAST_C_FIRST = 1;
    /** "File as" setting: &nbsp;<code>First Last</code> */
    private static final int FA_FIRST_LAST = 2;
    /** "File as" setting: &nbsp;<code>Company</code> */
    private static final int FA_COMPANY = 3;
    /** "File as" setting: &nbsp;<code>Last, First (Company)</code> */
    private static final int FA_LAST_C_FIRST_COMPANY = 4;
    /** "File as" setting: &nbsp;<code>First Last (Company)</code> */
    private static final int FA_FIRST_LAST_COMPANY = 5;
    /** "File as" setting: &nbsp;<code>Company (Last, First)</code> */
    private static final int FA_COMPANY_LAST_C_FIRST = 6;
    /** "File as" setting: &nbsp;<code>Company (First Last)</code> */
    private static final int FA_COMPANY_FIRST_LAST = 7;
    /** "File as" setting: <i>[explicitly specified "file as" string]</i> */
    private static final int FA_EXPLICIT = 8;

    /** The default "file as" setting: {@link #FA_LAST_C_FIRST}. */
    private static final int FA_DEFAULT = FA_LAST_C_FIRST;
    private static final int FA_MAXIMUM = FA_EXPLICIT;

    public static final String A_birthday = "birthday";
    public static final String A_callbackPhone = "callbackPhone";
    public static final String A_carPhone = "carPhone";
    public static final String A_company = "company";
    public static final String A_companyPhone = "companyPhone";
    public static final String A_department = "department"; 
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
    public static final String A_initials = "initials";
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

    /** Relates contact fields (<code>"firstName"</code>) to this contact's
     *  values (<code>"John"</code>). */
    private Map<String, String> mFields;


    public Contact(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_CONTACT)
            throw new IllegalArgumentException();
    }
    
    /**
     * Returns a single field from the contact's field/value pairs
     * @param fieldName
     * @return 
     */
    public String get(String fieldName) {
        return mFields.get(fieldName);
    }

    /** Returns a new <code>Map</code> containing all the contact's
     *  field/value pairs. */
    public Map<String, String> getFields() {
        return new HashMap<String, String>(mFields);
    }

    public String getFileAsString() throws ServiceException {
        return getFileAsString(mFields);
    }
    public static String getFileAsString(Map<String, String> fields) throws ServiceException {
        String fileAs = fields.get(A_fileAs);
        String[] fileParts = (fileAs == null ? null : fileAs.split(":", 2));
        int fileAsInt = FA_DEFAULT;
        if (fileParts != null)
            try {
                fileAsInt = Integer.parseInt(fileParts[0]);
                if (fileAsInt < 0 || fileAsInt > FA_MAXIMUM)
                    throw ServiceException.INVALID_REQUEST("invalid fileAs value: " + fileAs, null);
            } catch (NumberFormatException e) {
                throw ServiceException.INVALID_REQUEST("invalid fileAs value: " + fileAs, null);
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

    /** Returns a list of all email addresses for this contact.  This is used
     *  by {@link com.zimbra.cs.index.Indexer#indexContact} to populate the
     *  "To" field with the contact's email addresses. */
    public List<String> getEmailAddresses() {
        ArrayList<String> result = new ArrayList<String>();
        for (String field : EMAIL_FIELDS) {
            String value = mFields.get(field);
            if (value != null && !value.trim().equals(""))
                result.add(value);
        }
        return result;
    }
    
    boolean isTaggable()      { return true; }
    boolean isCopyable()      { return true; }
    boolean isMovable()       { return true; }
    boolean isMutable()       { return true; }
    boolean isIndexed()       { return true; }
    boolean canHaveChildren() { return false; }


    /** Creates a new <code>Contact</code> and persists it to the database.
     *  A real nonnegative item ID must be supplied from a previous call to
     *  {@link Mailbox#getNextItemId(int)}.
     * 
     * @param id        The id for the new contact.
     * @param folder    The {@link Folder} to create the contact in.
     * @param volumeId  The volume to persist any attachments to.
     * @param fields    The complete set of contact fields and values.
     * @param tags      A serialized version of all {@link Tag}s to apply.
     * @perms {@link ACL#RIGHT_INSERT} on the folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>mail.CANNOT_CONTAIN</code> - if the target folder
     *        can't contain contacts
     *    <li><code>service.INVALID_REQUEST</code> - if no fields are
     *        specified for the contact
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul>
     * @see #canContain(byte) */
    static Contact create(int id, Folder folder, short volumeId, Map<String, String> fields, String tags) throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_CONTACT))
            throw MailServiceException.CANNOT_CONTAIN();
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");

        if (fields == null)
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);
        for (Iterator<Map.Entry<String, String>> it = fields.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String> entry = it.next();
            String key   = StringUtil.stripControlCharacters(entry.getKey());
            String value = StringUtil.stripControlCharacters(entry.getValue());
            if (key == null || key.trim().equals("") || value == null || value.equals(""))
                it.remove();
        }
        if (fields.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);

        Mailbox mbox = folder.getMailbox();
        mbox.updateContactCount(1);

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = TYPE_CONTACT;
        data.folderId    = folder.getId();
        data.indexId     = id;
        data.imapId      = id;
        data.volumeId    = volumeId;
        data.date        = mbox.getOperationTimestamp();
        data.tags        = Tag.tagsToBitmask(tags);
        data.sender      = getFileAsString(fields);
        data.metadata    = encodeMetadata(DEFAULT_COLOR, fields);
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        Contact con = new Contact(mbox, data);
        con.finishCreation(null);
        if (con.mFields.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);
        return con;
    }

    public void reindex(IndexItem redo, Object indexData) throws ServiceException {
        // FIXME: need to note this as dirty so we can reindex if things fail
        if (!DebugConfig.disableIndexing)
            Indexer.GetInstance().indexContact(redo, mMailbox.getMailboxIndex(), mId, this);
    }

    void reanalyze(Object data) throws ServiceException {
        saveData(getFileAsString(mFields));
    }

    /** Alters an existing contact's fields.  Depending on the value of the
     *  <code>replace</code> parameter, will either modify the existing fields
     *  or completely replace the old ones with the supplied <code>Map</code>.
     *  Blank field values cause the field to be deleted from a contact.
     *  The resulting contact must have at least one non-blank field value.
     * 
     * @param fields   The set of contact fields.
     * @param replace  <code>true</code> to discard the old field values,
     *                 <code>false</code> to merge the new fields in.
     * @perms {@link ACL#RIGHT_WRITE} on the contact
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>service.INVALID_REQUEST</code> - if the resulting set of
     *        contact fields is empty
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul> */
    void modify(Map<String, String> fields, boolean replace) throws ServiceException {
        if (fields == null || fields.isEmpty()) {
            if (replace)
                throw ServiceException.INVALID_REQUEST("contact must have fields", null);
            return;
        }
        if (!canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the contact");

        markItemModified(Change.MODIFIED_CONTENT | Change.MODIFIED_DATE);

        if (replace)
            mFields.clear();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key   = StringUtil.stripControlCharacters(entry.getKey());
            String value = StringUtil.stripControlCharacters(entry.getValue());
            if (key != null && !key.trim().equals("")) {
                if (value != null && !value.equals(""))
                    mFields.put(key, value);
                else
                    mFields.remove(key);
            }
        }
        if (mFields.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);

    	// XXX: should update mData.size and Mailbox.size and folder.size
        mData.date = mMailbox.getOperationTimestamp();
        mData.contentChanged(mMailbox);
        saveData(getFileAsString());
	}

    /** @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_READ} on the original item */
    MailItem copy(Folder folder, int id, short destVolumeId) throws IOException, ServiceException {
        mMailbox.updateContactCount(1);
        return super.copy(folder, id, destVolumeId);
    }

    /** @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_READ} on the original item */
    MailItem icopy(Folder folder, int id, short destVolumeId, int imapId) throws IOException, ServiceException {
        mMailbox.updateContactCount(1);
        return super.icopy(folder, id, destVolumeId, imapId);
    }

    /** @perms {@link ACL#RIGHT_DELETE} on the item */
    PendingDelete getDeletionInfo() throws ServiceException {
        PendingDelete info = super.getDeletionInfo();
        info.contacts = 1;
        return info;
    }


    void decodeMetadata(Metadata meta) throws ServiceException {
        Metadata metaAttrs;
        if (meta.getVersion() <= 8) {
            // old version: metadata is just the fields
            metaAttrs = meta;
        } else {
            // new version: fields are in their own subhash
            super.decodeMetadata(meta);
            metaAttrs = meta.getMap(Metadata.FN_FIELDS);
        }

        mFields = new HashMap<String, String>();
        for (Iterator it = metaAttrs.asMap().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            mFields.put(entry.getKey().toString(), entry.getValue().toString());
        }
    }

    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor, mFields);
    }
    private static String encodeMetadata(byte color, Map<String, String> fields) {
        return encodeMetadata(new Metadata(), color, fields).toString();
    }
    static Metadata encodeMetadata(Metadata meta, byte color, Map<String, String> fields) {
        meta.put(Metadata.FN_FIELDS, new Metadata(fields));
        return MailItem.encodeMetadata(meta, color);
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("contact: {");
        appendCommonMembers(sb);
        for (Map.Entry entry : mFields.entrySet())
            sb.append(", ").append(entry.getKey()).append(": ").append(entry.getValue());
        sb.append("}");
        return sb.toString();
    }
}
