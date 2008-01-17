/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.provider;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.jivesoftware.wildfire.user.User;
import org.jivesoftware.wildfire.user.UserAlreadyExistsException;
import org.jivesoftware.wildfire.user.UserNotFoundException;
import org.jivesoftware.wildfire.user.UserProvider;
import org.xmpp.packet.JID;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;


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
            
            if (acct == null) 
                throw new UserNotFoundException("Unknown user: "+username);
            
            if (!Provisioning.onLocalServer(acct)) 
                throw new UserNotFoundException("User "+username+" is not local to this server");
            
            String un = acct.getName();
//            int atSign = un.indexOf('@');
//            if (atSign >= 0)
//                un = un.substring(0, atSign);
            
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

    public Collection<User> findUsers(Set<String> fields, String query) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Collection<User> findUsers(Set<String> fields, String query, int startIndex, int numResults) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Set<String> getSearchFields() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Collection<String> getUsernames() {
        // TODO Auto-generated method stub
        return null;
    }
}
