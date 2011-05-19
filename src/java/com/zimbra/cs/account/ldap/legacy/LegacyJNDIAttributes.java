/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap.legacy;

import java.util.Arrays;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.IAttributes;

/**
 * An IAttributes tied to ZimbraLdapContext and the legacy ldapUtil methods.
 * 
 * @author pshao
 *
 */
public class LegacyJNDIAttributes implements IAttributes {
    private Attributes attributes;
    
    public LegacyJNDIAttributes(Attributes attrs) {
        this.attributes = attrs;
    }
    
    @Override
    public String getAttrString(String attrName) throws ServiceException {
        try {
            return LegacyLdapUtil.getAttrString(attributes, attrName);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to get attribute " + attrName, e);
        }
    }
    
    @Override
    public String[] getMultiAttrString(String attrName) throws ServiceException {
        try {
            return LegacyLdapUtil.getMultiAttrString(attributes, attrName);
        } catch (NamingException e) {
          throw ServiceException.FAILURE("unable to get attribute " + attrName, e);
        }
    }
    
    @Override
    public String[] getMultiAttrString(String attrName, boolean containsBinaryData, boolean isBinaryTransfer) 
    throws ServiceException {
        try {
            return LegacyLdapUtil.getMultiAttrString(attributes, attrName, containsBinaryData, isBinaryTransfer);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to get attribute " + attrName, e);
        }
    }

    @Override
    /**
     * checkBinary is IGNORED.  This implementation always check binary.
     */
    public List<String> getMultiAttrStringAsList(String attrName, CheckBinary checkBinary) 
    throws ServiceException {
        try {
            return Arrays.asList(LegacyLdapUtil.getMultiAttrString(attributes, attrName));
        } catch (NamingException e) {
          throw ServiceException.FAILURE("unable to get attribute " + attrName, e);
        }
    }

    @Override
    public boolean hasAttribute(String attrName) {
        return attributes.get(attrName) != null;
    }

    @Override
    public boolean hasAttributeValue(String attrName, String value) {
        Attribute attr = attributes.get(attrName);
        return attr.contains(value);
    }

}
