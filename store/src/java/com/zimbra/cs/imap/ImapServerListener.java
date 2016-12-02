/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;


public class ImapServerListener {
    protected static ImapServerListener instance;
    private final Map<String, List<ImapListener>> listeners;
    private final Map<String, String> accountsToWaitsets;
    private final Map<String, String> serversToWaitsets;
    public static synchronized ImapServerListener getInstance() {
        if(instance == null) {
            instance = new ImapServerListener();
        }
        return instance;
    }

    ImapServerListener() {
        listeners = new ConcurrentHashMap<String, List<ImapListener>>();
        accountsToWaitsets = new ConcurrentHashMap<String, String>();
        serversToWaitsets = new ConcurrentHashMap<String, String>();
    }

    public void addListener(ImapListener listener) throws ServiceException {
        String accountId = listener.getTargetAccountId();
        Account acc = Provisioning.getInstance().getAccountById(accountId);
        if(acc != null) {
            if(!listeners.containsKey(accountId)) {
                listeners.put(accountId, Collections.synchronizedList(new ArrayList<ImapListener>()));
            }
            listeners.get(accountId).add(listener);
            
        } else {
            ZimbraLog.imap.error("Tried registering IMAP listener for a non-existent account %s", accountId);
        }
    }

    public void removeListener(ImapListener listener) {
        if(listeners.containsKey(listener.getTargetAccountId())) {
            listeners.get(listener.getTargetAccountId()).remove(listener);
            if(listeners.get(listener.getTargetAccountId()).isEmpty()) {
                //remove this account from waitset for the server
            }
        }
    }

    public boolean isListeningOn(String accountId) {
        return listeners.containsKey(accountId);
    }

    private void addAccountToWaitset(Account acc) {
        if(!accountsToWaitsets.containsKey(acc.getId())) {
            //we don't have a waitset for this account yet
            if(!serversToWaitsets.containsKey(acc.getMailHost())) {
                //we are not listening on this server yet
            }
                
        }
    }
    /**
     * Deletes any remaining waitsets to release resources on remote servers. ImapServer should call this method before dying.
     */
    public void shutdown() {
       
    }
}
