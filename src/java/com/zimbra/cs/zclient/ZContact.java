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

import java.util.Arrays;
import java.util.Map;

import com.zimbra.cs.service.ServiceException;

public interface ZContact  {
    
    public enum Attr {

        assitantPhone,
        birthday,
        callbackPhone,
        carPhone,
        company,
        companyPhone,
        description,
        department,
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

    public String getId();
    
    public String getTagIds();
    
    public String getFlags();
    
    public boolean hasFlags();
    
    public boolean hasTags();
    
    public boolean isFlagged();
    
    public boolean hasAttachment();
    
    public String getFolderId();

    public String getRevision();

    /**
     * @return time in msecs
     */
    public long getMetaDataChangedDate();
    
    public Map<String, String> getAttrs();

}
