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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.Element;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ZContact  {

    /** "File as" setting: &nbsp;<code>Last, First</code> */
    public static final String FA_LAST_C_FIRST = "1";
    /** "File as" setting: &nbsp;<code>First Last</code> */
    public static final String FA_FIRST_LAST = "2";
    /** "File as" setting: &nbsp;<code>Company</code> */
    public static final String FA_COMPANY = "3";
    /** "File as" setting: &nbsp;<code>Last, First (Company)</code> */
    public static final String FA_LAST_C_FIRST_COMPANY = "4";
    /** "File as" setting: &nbsp;<code>First Last (Company)</code> */
    public static final String FA_FIRST_LAST_COMPANY = "5";
    /** "File as" setting: &nbsp;<code>Company (Last, First)</code> */
    public static final String FA_COMPANY_LAST_C_FIRST = "6";
    /** "File as" setting: &nbsp;<code>Company (First Last)</code> */
    public static final String FA_COMPANY_FIRST_LAST = "7";
    /** "File as" setting: <i>[explicitly specified "file as" string]</i> */
    public static final String FA_EXPLICIT = "8";

    private String mId;
    private String mFlags;
    private String mFolderId;
    private String mTagIds;
    private String mRevision;
    private long mMetaDataChangedDate;
    private Map<String, String> mAttrs;

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
        workURL;

        public static Attr fromString(String s) throws ServiceException {
            try {
                return Attr.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid attr: "+s+", valid values: "+Arrays.asList(Attr.values()), e);
            }
        }

    }

    public enum Flag {
        flagged('f'),
        attachment('a');

        private char mFlagChar;
        
        public char getFlagChar() { return mFlagChar; }

        public static String toNameList(String flags) {
            if (flags == null || flags.length() == 0) return "";            
            StringBuilder sb = new StringBuilder();
            for (int i=0; i < flags.length(); i++) {
                String v = null;
                for (Flag f : Flag.values()) {
                    if (f.getFlagChar() == flags.charAt(i)) {
                        v = f.name();
                        break;
                    }
                }
                if (sb.length() > 0) sb.append(", ");
                sb.append(v == null ? flags.substring(i, i+1) : v);
            }
            return sb.toString();
        }
        
        Flag(char flagChar) {
            mFlagChar = flagChar;            
        }
    }

    public ZContact(Element e) throws ServiceException {
        mId = e.getAttribute(MailService.A_ID);
        mFolderId = e.getAttribute(MailService.A_FOLDER);
        mFlags = e.getAttribute(MailService.A_FLAGS, null);
        mTagIds = e.getAttribute(MailService.A_TAGS, null);
        mRevision = e.getAttribute(MailService.A_REVISION, null);
        mMetaDataChangedDate = e.getAttributeLong(MailService.A_MODIFIED_DATE, 0) * 1000;
        mAttrs = new HashMap<String, String>();
        for (Element a : e.listElements(MailService.E_ATTRIBUTE)) {
            mAttrs.put(a.getAttribute(MailService.A_ATTRIBUTE_NAME), a.getText());
        }
    }

    public String getFolderId() {
        return mFolderId;
    }

    public String getId() {
        return mId;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("id", mId);
        sb.add("folder", mFolderId);
        sb.add("flags", mFlags);
        sb.add("tags", mTagIds);
        sb.addDate("metaDataChangedDate", mMetaDataChangedDate);
        sb.add("revision", mRevision);
        sb.beginStruct("attrs");
        for (Map.Entry<String, String> entry : mAttrs.entrySet()) {
            sb.add(entry.getKey(), entry.getValue());
        }
        sb.endStruct();
        sb.endStruct();
        return sb.toString();
    }

    public String getFlags() {
        return mFlags;
    }

    public Map<String, String> getAttrs() {
        return mAttrs;
    }

    public long getMetaDataChangedDate() {
        return mMetaDataChangedDate;
    }

    public String getRevision() {
        return mRevision;
    }

    public String getTagIds() {
        return mTagIds;
    }

    public boolean hasFlags() {
        return mFlags != null && mFlags.length() > 0;        
    }

    public boolean hasTags() {
        return mTagIds != null && mTagIds.length() > 0;
    }

    public boolean hasAttachment() {
        return hasFlags() && mFlags.indexOf(Flag.attachment.getFlagChar()) != -1;
    }

    public boolean isFlagged() {
        return hasFlags() && mFlags.indexOf(Flag.flagged.getFlagChar()) != -1;
    }



}
