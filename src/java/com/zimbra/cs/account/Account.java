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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.Map;

import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
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
     * @return the domain name for this account (foo.com), or null if an admin account. 
     */
    public String getDomainName();
    
    /**
     * @return the domain this account, or null if an admin account. 
     * @throws ServiceException
     */    
    public Domain getDomain() throws ServiceException;    
    
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
    public Map getAttrs(boolean prefsOnly, boolean applyCos) throws ServiceException;    
    

    /**
     * @return the COS object for this account, or null if account has no COS
     * 
     * @throws ServiceException
     */
    public Cos getCOS() throws ServiceException;
   
    public String[] getAliases() throws ServiceException;
    
    /**
     * check whether this account's mailbox is supposed to be on this host
     * 
     * @throws ServiceException
     */
    public boolean isCorrectHost() throws ServiceException;

    /**
     * @return the Server object where this account's mailbox is homed
     * @throws ServiceException
     */
    public Server getServer() throws ServiceException;

    /**
     * Returns account's time zone
     * @return
     * @throws ServiceException
     */
    public ICalTimeZone getTimeZone() throws ServiceException;

    /**
     * Returns calendar user type
     * @return USER (default) or RESOURCE
     * @throws ServiceException
     */
    public CalendarUserType getCalendarUserType() throws ServiceException;

    public boolean saveToSent() throws ServiceException;
}
