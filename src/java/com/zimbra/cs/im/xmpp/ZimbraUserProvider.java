/* ***** BEGIN LICENSE BLOCK *****
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

package com.zimbra.cs.im.xmpp;

import java.util.Collection;
import java.util.Date;

import org.xmpp.packet.JID;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.im.xmpp.srv.user.User;
import com.zimbra.cs.im.xmpp.srv.user.UserAlreadyExistsException;
import com.zimbra.cs.im.xmpp.srv.user.UserNotFoundException;
import com.zimbra.cs.im.xmpp.srv.user.UserProvider;
import com.zimbra.cs.service.ServiceException;


public class ZimbraUserProvider implements UserProvider {
    
    static ZimbraUserProvider sInstance = null;

    static ZimbraUserProvider getInstance() { return sInstance; }
    
    public ZimbraUserProvider() {
        sInstance = this;
    }
    
    public User loadUser(String username) throws UserNotFoundException {
        // Un-escape username.
        username = JID.unescapeNode(username);
        
        try {
            Account acct = lookupAccount(username);
            
            String un = acct.getName();
            int atSign = un.indexOf('@');
            if (atSign >= 0)
                un = un.substring(0, atSign);
            
            // return new User(username, name, email, creationDate, modificationDate)
            return new User(un, acct.getAttr(Provisioning.A_displayName, null), acct.getAttr(Provisioning.A_zimbraMailDeliveryAddress),  new Date(0), new Date(0));
            
        } catch (ServiceException e) {
            throw new UserNotFoundException(e);
        }
    }
    
    Account lookupAccount(String username) throws ServiceException { 
        Account acct = Provisioning.getInstance().get(AccountBy.name, username);
        return acct;
    }

    public User createUser(String username, String password, String name, String email)  throws UserAlreadyExistsException {
        throw new UnsupportedOperationException();
    }
    
    public void deleteUser(String username) {
        throw new UnsupportedOperationException();
    }
    
    public int getUserCount() {
        throw new UnsupportedOperationException();
    }
    
    public Collection<User> getUsers() { 
        throw new UnsupportedOperationException();
    }
    
    public Collection<User> getUsers(int startIndex, int numResults) {
        throw new UnsupportedOperationException();
    }

    public String getPassword(String username) throws UserNotFoundException, UnsupportedOperationException { 
        throw new UnsupportedOperationException();
    }

    public void setPassword(String username, String password) throws UserNotFoundException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
    
    public void setName(String username, String name) throws UserNotFoundException {
        throw new UnsupportedOperationException();
    }
    
    public void setEmail(String username, String email) throws UserNotFoundException {
        throw new UnsupportedOperationException();
    }
    
    public void setCreationDate(String username, Date creationDate) throws UserNotFoundException {
        throw new UnsupportedOperationException();
    }
    
    public void setModificationDate(String username, Date modificationDate) throws UserNotFoundException {
        throw new UnsupportedOperationException();
    }
    
    public boolean isReadOnly() { return true; }
    
    public boolean supportsPasswordRetrieval() { return false; }
}
