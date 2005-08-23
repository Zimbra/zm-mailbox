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
 * Created on Aug 23, 2004
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collections;
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

    private static final int FA_LAST_C_FIRST = 1;         /* last, first */
    private static final int FA_FIRST_LAST = 2;           /* first last */
    private static final int FA_COMPANY = 3;              /* company */ 
    private static final int FA_LAST_C_FIRST_COMPANY = 4; /* last, first (company)*/
    private static final int FA_FIRST_LAST_COMPANY = 5;   /* first last (company) */
    private static final int FA_COMPANY_LAST_C_FIRST = 6; /* company (last, first) */
    private static final int FA_COMPANY_FIRST_LAST = 7;   /* company (first last) */
    private static final int FA_EXPLICIT = 8;             /* explicitly specified */

    private static final int FA_DEFAULT = FA_LAST_C_FIRST;
    private static final int FA_MAXIMUM = FA_EXPLICIT;

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

    private Map mAttributes;
	private Map mUnmodifiableAttributes;
	
    public Contact(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_CONTACT)
            throw new IllegalArgumentException();
    }

    public Map getAttrs() {
        if (mUnmodifiableAttributes == null)
            mUnmodifiableAttributes = Collections.unmodifiableMap(mAttributes);
        return mUnmodifiableAttributes;
    }

    public String getFileAsString() throws ServiceException {
        return getFileAsString(mAttributes);
    }
    public static String getFileAsString(Map attrs) throws ServiceException {
        String fileAs = (String) attrs.get(A_fileAs);
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
        
        String company = (String) attrs.get(A_company);
        if (company == null)
            company = "";
        String first = (String) attrs.get(A_firstName);
        if (first == null)
            first = "";
        String last = (String) attrs.get(A_lastName);
        if (last == null)
            last = "";
        
        StringBuffer result = new StringBuffer();
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
                if (first.length() > 0 && last.length() > 0) {
                    result.append(" (").append(last);
                    if (first.length() > 0 && last.length() > 0)
                        result.append(", ");
                    result.append(first).append(')');
                }
                break;
            case FA_COMPANY_FIRST_LAST:
                result.append(company);
                if (first.length() > 0 && last.length() > 0) {
                    result.append(" (").append(first);
                    if (first.length() > 0 && last.length() > 0)
                        result.append(' ');
                    result.append(last).append(')');
                }
                break;
        }
        return result.toString().trim();
    }

    private static final String[] EMAIL_FIELDS = new String[] { A_email, A_email2, A_email3 };
    /**
     * @return A list of all email addresses for this contact.  Used by the indexer, so it can populate
     * the "To" field with the contact's email addresses 
     */
    public List /*<String>*/ getEmailAddresses() {
        ArrayList result = new ArrayList();
        for (int i = 0; i < EMAIL_FIELDS.length; i++) {
            String value = (String) mAttributes.get(EMAIL_FIELDS[i]);
            if (value != null)
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

    boolean canParent(MailItem child) { return (child instanceof Document); }


    /**
     * Create a new contact.
     * @param id Use this value to set mail_item.id in database.
     * @param folder
     * @param attrs
     * @param tags
     * @return
     * @throws ServiceException
     */
    static Contact create(int id, Folder folder, Map attrs, String tags) throws ServiceException {
        if (folder == null || !folder.canContain(TYPE_CONTACT))
            throw MailServiceException.CANNOT_CONTAIN();
        if (attrs == null || attrs.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have attributes", null);

        Mailbox mbox = folder.getMailbox();
        mbox.updateContactCount(1);

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = TYPE_CONTACT;
        data.folderId    = folder.getId();
        data.indexId     = id;
        data.date        = mbox.getOperationTimestamp();
        data.tags        = Tag.tagsToBitmask(tags);
        data.sender      = getFileAsString(attrs);
        data.metadata    = encodeMetadata(attrs);
        data.modMetadata = mbox.getOperationChangeID();
        data.modContent  = mbox.getOperationChangeID();
        DbMailItem.create(mbox, data);

        Contact con = new Contact(mbox, data);
        con.finishCreation(null);
        if (con.mAttributes.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have attributes", null);
        return con;
    }

    public void reindex(IndexItem redo, Object indexData) throws ServiceException {
        // FIXME: need to note this as dirty so we can reindex if things fail
        if (!DebugConfig.disableIndexing)
            Indexer.GetInstance().indexContact(redo, getMailboxId(), mId, this);
    }

    void modify(Map attrs, boolean replace) throws ServiceException {
        if (attrs == null || attrs.isEmpty()) {
            if (replace)
                throw ServiceException.INVALID_REQUEST("contact must have attributes", null);
            return;
        }
        markItemModified(Change.MODIFIED_CONTENT | Change.MODIFIED_DATE);

        if (replace)
            mAttributes.clear();
        for (Iterator it = attrs.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            String key   = StringUtil.stripControlCharacters((String) entry.getKey());
            String value = StringUtil.stripControlCharacters((String) entry.getValue());
            if (key != null && !key.equals("")) {
                if (value != null && !value.equals(""))
                    mAttributes.put(key, value);
                else
                    mAttributes.remove(key);
            }
        }
        if (mAttributes.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have attributes", null);

    	// XXX: should update mData.size and Mailbox.size and folder.size
        mData.date       = mMailbox.getOperationTimestamp();
        mData.modContent = mMailbox.getOperationChangeID();
        saveData(getFileAsString());
	}

    PendingDelete getDeletionInfo() throws ServiceException {
        PendingDelete info = super.getDeletionInfo();
        info.contacts = 1;
        return info;
    }


    Metadata decodeMetadata(String metadata) throws ServiceException {
        Metadata meta = new Metadata(metadata, this);
        mAttributes = new HashMap();
        for (Iterator it = meta.asMap().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            mAttributes.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return meta;
    }

    String encodeMetadata() {
        return encodeMetadata(mAttributes);
    }
    static String encodeMetadata(Map attributes) {
        return new Metadata(attributes).toString();
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("contact: {");
        appendCommonMembers(sb);
        for (Iterator it = mAttributes.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            sb.append(", ").append(entry.getKey()).append(": ").append(entry.getValue());
        }
        sb.append("}");
        return sb.toString();
    }
}
