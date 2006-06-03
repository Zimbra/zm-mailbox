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

import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public interface Account extends NamedEntry {

    public static enum CalendarUserType {
        USER,       // regular person account
        RESOURCE    // calendar resource
    }

    public String getUid();

    /**
     * Returns the *account's* COSId, that is, returns the zimbraCOSId directly set on the account, or null if not set.
     * Use Provisioning.getCos(account) to get the actual COS object.
     * @return 
     */
    public String getAccountCOSId();
    
    /**
     * @return the domain name for this account (foo.com), or null if an admin account. 
     */
    public String getDomainName();
    
    public String getAccountStatus();
    
    /**
     * combines all zimbraPref* attributes from the Account and the COS and returns as a map. Account preferences
     * override COS preferences.
     *  
     * @param prefsOnly return only zimbraPref* attrs.
     * @param applyCos apply COS attributes
     * @return
     * @throws ServiceException
     */
    public Map<String, Object> getAttrs(boolean prefsOnly, boolean applyCos) throws ServiceException;    

    public String[] getAliases() throws ServiceException;

    /**
     * Returns calendar user type
     * @return USER (default) or RESOURCE
     * @throws ServiceException
     */
    public CalendarUserType getCalendarUserType() throws ServiceException;

    public boolean saveToSent() throws ServiceException;
}
