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

package com.zimbra.cs.account;

import java.util.Map;

import com.zimbra.cs.mailbox.Contact;

/**
 * @author schemers
 */
public class GalContact implements Comparable {
    
    private Map<String, Object> mAttrs;
    private String mId;
    private String mSortField;

    public GalContact(String dn, Map<String,Object> attrs) {
        mId = dn;
        mAttrs = attrs;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.GalContact#getId()
     */
    public String getId() {
        return mId;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.GalContact#getAttrs()
     */
    public Map<String, Object> getAttrs() {
        return mAttrs;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("LdapGalContact: { ");
        sb.append("id="+mId);
        sb.append("}");
        return sb.toString();
    }

    private String getSingleAttr(String name) {
        Object val = mAttrs.get(name);
        if (val instanceof String) return (String) val;
        else if (val instanceof String[]) return ((String[])val)[0];
        else return null;
    }
    
    private String getSortField() {
        if (mSortField != null) return mSortField;
        
        mSortField = getSingleAttr(Contact.A_fullName);
        if (mSortField != null) return mSortField;

        String first = getSingleAttr(Contact.A_firstName);
        String last = getSingleAttr(Contact.A_lastName);
        
        if (first != null || last != null) {
            StringBuilder sb = new StringBuilder();
            if (first != null) {
                sb.append(first);
            }
            if (last != null) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(last);
            }
            mSortField = sb.toString();
        } else {
            mSortField = getSingleAttr(Contact.A_email);
            if (mSortField == null) mSortField = "";
        }
        return mSortField;
    }
    
    public int compareTo(Object obj) {
        if (!(obj instanceof GalContact))
            return 0;
        GalContact other = (GalContact) obj;
        return getSortField().compareTo(other.getSortField());
    }
}
