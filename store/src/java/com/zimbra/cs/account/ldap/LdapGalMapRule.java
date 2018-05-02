/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.ldap;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AttributeManager.IDNType;
import com.zimbra.cs.account.AttributeType;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.ldap.IAttributes;

/*
 * maps LDAP attrs into contact attrs. 
 */
public class LdapGalMapRule {

    private String[] mLdapAttrs;
    private String[] mContactAttrs;
    
    // parallel array with mContactAttrs
    private LdapGalValueMap[] mContactAttrsValueMaps;
    
    private boolean mIsBinary;
    private boolean mIsBinaryTransfer;
    private boolean mIsSMIMECertificate;
    
    private static final Pattern typedRule = Pattern.compile("\\((.*)\\)\\s(.*)");
        
    public LdapGalMapRule(String rule, Map<String, LdapGalValueMap> valueMaps) {
        
        Matcher matcher = typedRule.matcher(rule);
        if (matcher.matches()) {
            String type = matcher.group(1);
            AttributeType attrType = AttributeType.getType(type);
            if (attrType == null) {
                ZimbraLog.gal.warn("Unrecognized type in attr map: " + type + ", type is ignore for rule " + rule);
            } else {
                if (AttributeManager.isBinaryType(attrType)) {
                    mIsBinary = true;
                } else if (AttributeManager.isBinaryTransferType(attrType)) {
                    mIsBinaryTransfer = true;
                }
                // no special treatment for all other types
                
                rule = matcher.group(2);
            }
        }
        
        int p = rule.indexOf('=');
        if (p != -1) {
            String ldapAttr = rule.substring(0, p);
            String contactAttr = rule.substring(p+1);

            mLdapAttrs = (ldapAttr.indexOf(',') != -1) ? ldapAttr.split(",") : new String[] { ldapAttr };
            mContactAttrs = (contactAttr.indexOf(',') != -1) ? contactAttr.split(",") : new String[] { contactAttr };    
            
            mContactAttrsValueMaps = new LdapGalValueMap[mContactAttrs.length];
            if (valueMaps != null) {
                for (int i = 0; i < mContactAttrs.length; i++) {
                    mContactAttrsValueMaps[i] = valueMaps.get(mContactAttrs[i]);
                }
            }
        }
        
        for (String contactAttr : mContactAttrs) {
        	if (Contact.isSMIMECertField(contactAttr)) {
        		mIsSMIMECertificate = true;
        		break;
        	}
        }
    }
    
    public boolean isBinary() {
        return mIsBinary;
    }
    
    public boolean isBinaryTransfer() {
        return mIsBinaryTransfer;
    }
    
    public boolean containsBinaryData() {
        return mIsBinary || mIsBinaryTransfer;
    }
    
    // return if this rule is the SMIME certificate rule
    public boolean isSMIMECertificate() {
        return mIsSMIMECertificate;
    }
    
    public String[] getLdapAttrs() {
        return mLdapAttrs;
    }

    public String[] getContactAttrs() {
        return mContactAttrs;
    }

    // add contact attr, and also make sure its value is unique for any other 
    // contact attrs in this rule.
    private int addToContactAttrs(Map<String,Object> contactAttrs, Object value, int index) {
        if (index >= mContactAttrs.length) return index;
        for (int i=0; i < index; i++) {
            Object v = contactAttrs.get(mContactAttrs[i]);
                    
            if (v != null) {
                value = mapValue(i, value);
                if (v.equals(value))
                    return index;
            }
        }
        
        contactAttrs.put(mContactAttrs[index], mapValue(index, value));
        index++;
        return index;
    }
    
    private Object mapValue(int index, Object value) {
        LdapGalValueMap valueMap = mContactAttrsValueMaps[index];
        if (valueMap != null)
            return valueMap.apply(value);
        else
            return value;
    }
    
    void apply(IAttributes ldapAttrs, Map<String,Object> contactAttrs) {
        AttributeManager attrMgr = AttributeManager.getInst();
        
        int index = 0; // index into mContactAttrs
        for (String ldapAttr: mLdapAttrs) {
            if (index >= mContactAttrs.length) return;
            String val[];
            try { 
                val = ldapAttrs.getMultiAttrString(ldapAttr, containsBinaryData(), isBinaryTransfer()); 
            } catch (ServiceException e) { 
                return; 
            }
            
            IDNType idnType = AttributeManager.idnType(attrMgr, ldapAttr);
            
            if (val.length == 1) {
                index = addToContactAttrs(contactAttrs, IDNUtil.toUnicode(val[0], idnType), index);
            } else if (val.length > 1) {
                if (mContactAttrs.length == 1) {
                    index = addToContactAttrs(contactAttrs, val, index);
                    return;
                } else {
                    for (int i=0; i < val.length; i++) {
                        if (index >= mContactAttrs.length) return;
                        index = addToContactAttrs(contactAttrs, IDNUtil.toUnicode(val[i], idnType), index);                        
                    }
                }
            }
        }
    }
    
}
