/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.gal.GalSearchConfig.GalType;
import com.zimbra.cs.mailbox.Contact;

/**
 * @author schemers
 */
public class GalContact implements Comparable {
    
    public interface Visitor  {
        public void visit(GalContact gc) throws ServiceException;
    }
   
    private GalType mGalType;
    private Map<String, Object> mAttrs;
    private String mId;
    private String mSortField;

    public GalContact(String dn, Map<String,Object> attrs) {
        mId = dn;
        mAttrs = attrs;
    }
    
    public GalContact(GalType galType, String dn, Map<String,Object> attrs) {
        mGalType = galType;
        mId = dn;
        mAttrs = attrs;
    }

    public boolean isZimbraGal() {
        return GalType.zimbra == mGalType;
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

    public String getSingleAttr(String name) {
        Object val = mAttrs.get(name);
        String strValue = null;
        if (val instanceof String) {
            strValue = (String) val;
            try {
                String [] vals = Contact.parseMultiValueAttr(strValue);
                if(vals.length >= 1) {
                    strValue = vals[0];
                }
            } catch (JSONException jex) {
                ZimbraLog.mailop.debug("Not a valid JSON format : %s", strValue, jex);
            }
        } else if (val instanceof String[]) {
            strValue = ((String[])val)[0];
        }
        return strValue;
    }
    
    public boolean isGroup() {
        return ContactConstants.TYPE_GROUP.equals(getSingleAttr(ContactConstants.A_type));
    }
    
    private String getSortField() {
        if (mSortField != null) return mSortField;
        
        mSortField = getSingleAttr(ContactConstants.A_fullName);
        if (mSortField != null) return mSortField;

        String first = getSingleAttr(ContactConstants.A_firstName);
        String last = getSingleAttr(ContactConstants.A_lastName);
        
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
            mSortField = getSingleAttr(ContactConstants.A_email);
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
    
    public static GalContact fromElement(Element elm) throws ServiceException {
        String dn = elm.getAttribute(AccountConstants.A_REF);
        Map<String,Object> attrs = new HashMap<String,Object>();
        for (Element attr : elm.listElements(MailConstants.E_ATTRIBUTE)) {
            String name = attr.getAttribute(MailConstants.A_ATTRIBUTE_NAME);
            String value = attr.getText();
            StringUtil.addToMultiMap(attrs, name, value);
        }
        GalContact galContact = new GalContact(dn , attrs);
        return galContact;
    }
}
