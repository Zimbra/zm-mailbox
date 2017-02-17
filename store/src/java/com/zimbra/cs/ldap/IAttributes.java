/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
