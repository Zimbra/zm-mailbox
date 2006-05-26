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
 * Created on Nov 17, 2004
 */
package com.zimbra.cs.account.ldap;

import java.util.Map;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.mailbox.Contact;

/**
 * @author schemers
 */
public class LdapGalContact implements GalContact {

    private Map<String, Object> mAttrs;
    private String mId;
    private String mSortField;

    public LdapGalContact(String dn, Map<String,Object> attrs) {
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
    
    private String getSortField() {
        if (mSortField != null) return mSortField;
        
        mSortField = (String) mAttrs.get(Contact.A_fullName);
        if (mSortField != null) return mSortField;

        String first = (String) mAttrs.get(Contact.A_firstName);
        String last = (String) mAttrs.get(Contact.A_lastName);
        
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
            mSortField = "";
        }
        return mSortField;
    }
    
    public int compareTo(Object obj) {
        if (!(obj instanceof LdapGalContact))
            return 0;
        LdapGalContact other = (LdapGalContact) obj;
        return getSortField().compareTo(other.getSortField());
    }
}
