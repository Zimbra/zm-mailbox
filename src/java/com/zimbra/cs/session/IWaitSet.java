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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.session;

import java.util.List;

import com.zimbra.common.service.ServiceException;

/**
 * WaitSet: scalable mechanism for listening for changes to one or many accounts */
public interface IWaitSet {

    /**
     * WaitMultipleAccounts:  optionally modifies the wait set and checks
     * for any notifications.  If block=1 and there are no notificatins, then
     * this API will BLOCK until there is data.
     *
     * Client should always set 'seq' to be the highest known value it has
     * received from the server.  The server will use this information to
     * retransmit lost data.
     *
     * If the client sends a last known sync token then the notification is
     * calculated by comparing the accounts current token with the client's
     * last known.
     *
     * If the client does not send a last known sync token, then notification
     * is based on change since last Wait (or change since <add> if this
     * is the first time Wait has been called with the account)
     * 
     * @param cb
     * @param lastKnownSeqNo
     * @param block
     * @param addAccounts
     * @param updateAccounts
     * @param removeAccounts
     * @return
     * @throws ServiceException
     */
    public List<WaitSetError> doWait(WaitSetCallback cb, String lastKnownSeqNo, boolean block,
        List<WaitSetAccount> addAccounts, List<WaitSetAccount> updateAccounts, 
        List<String> removeAccounts) throws ServiceException;
        
    /**
     * Just a helper: the 'default interest' is set when the WaitSet is created,
     * and subsequent requests can access it when creating/updating WaitSetAccounts
     * if the client didn't specify one with the update.
     */
    public int getDefaultInterest();
    
    /**
     * @return The accountID of the owner/creator
     */
    public String getOwnerAccountId();
    
    /**
     * @return the id of this wait set
     */
    public String getWaitSetId();
    
    /** @return true if this is an "all accounts" waitset */
    public boolean isIncludeAllAccounts();
    
}
