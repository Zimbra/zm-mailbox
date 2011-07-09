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
package com.zimbra.cs.ldap;

import java.util.List;

import com.zimbra.common.service.ServiceException;

// TODO deprecate after all legacy stuff is remvoed
public interface IAttributes {
    
    /**
     * - If a method does not have a CheckBinary parameter, it will *not* check 
     *   for binary data and binary transfer based on AttributeManager.
     *   It will assume all attributes are *not* binary.
     *   
     * - If a method has a CheckBinary parameter, it will check for binary data 
     *   and binary transfer based on AttributeManager if CheckBinary is CHECK.
     *   It will assume all attributes are *not* binary if CheckBinary is NOCHECK.
     */
    
    
    public String getAttrString(String attrName) throws ServiceException;
    
    public String[] getMultiAttrString(String attrName) throws ServiceException;
    
    public String[] getMultiAttrString(String attrName, 
            boolean containsBinaryData, boolean isBinaryTransfer) throws ServiceException;
    
    
    public static enum CheckBinary {
        CHECK,
        NOCHECK;
    }
    
    public List<String> getMultiAttrStringAsList(String attrName, CheckBinary checkBinary) 
    throws ServiceException;
    
    /**
     * Whether the specified attribute is present.
     * 
     * @param attrName
     * @return
     */
    public abstract boolean hasAttribute(String attrName);
    
    /**
     * Whether it contains the specified attribute with the specified value.
     *  
     * @param attrName
     * @param value
     * @return
     */
    public abstract boolean hasAttributeValue(String attrName, String value);

}
