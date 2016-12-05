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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.soap.admin.message.AdminCreateWaitSetRequest;
import com.zimbra.soap.admin.message.AdminCreateWaitSetResponse;
import com.zimbra.soap.admin.message.AdminDestroyWaitSetRequest;
import com.zimbra.soap.type.WaitSetAddSpec;

public class ImapServerListener {
    private final String server;
    private String wsID = null;
    private final ConcurrentMap<String, ImapRemoteSession> accountToSessionMap = new ConcurrentHashMap<String, ImapRemoteSession>();
    private final SoapProvisioning soapProv = new SoapProvisioning();

    ImapServerListener(String svr) throws ServiceException {
        this.server = svr;
        soapProv.soapSetURI(URLUtil.getAdminURL(Provisioning.getInstance().getServerByName(server)));
    }

    private void checkAuth() throws ServiceException {
        if(soapProv.isExpired()) {
            soapProv.soapZimbraAdminAuthenticate();
        }
    }

    public void shutdown() {
        synchronized(soapProv) {
            try {
                soapProv.soapLogOut();
            } catch (ServiceException e) {
                ZimbraLog.imap.error("Failed to log out from admin SOAP session", e);
            }
        }
    }

    public void addListener(ImapRemoteSession listener) throws ServiceException {
        accountToSessionMap.put(listener.getTargetAccountId(), listener);
        if(wsID == null) {
            //create a waitset
            createWaitSet(listener.getTargetAccountId());
        } else {
            //add to existing waitset
        }
    }

    public void removeListener(ImapRemoteSession listener) throws ServiceException {
        accountToSessionMap.remove(listener.getTargetAccountId());
        if(accountToSessionMap.isEmpty()) {
            //cancell waitset
            deleteWaitSet();
        }
    }

    public boolean isListeningOn(String accountId) {
        return accountToSessionMap.containsKey(accountId);
    }

    private void createWaitSet(String accountId) throws ServiceException {
        synchronized(soapProv) {
            if(wsID == null) {
                AdminCreateWaitSetRequest req = new AdminCreateWaitSetRequest("all", false);
                WaitSetAddSpec add = new WaitSetAddSpec();
                add.setId(accountId);
                req.addAccount(add);
                checkAuth();
                AdminCreateWaitSetResponse resp = soapProv.invokeJaxb(req);
                wsID = resp.getWaitSetId();
            }
        }
    }

    private void deleteWaitSet() throws ServiceException {
        synchronized(soapProv) {
            if(wsID != null) {
                AdminDestroyWaitSetRequest req = new AdminDestroyWaitSetRequest(wsID);
                checkAuth();
                soapProv.invokeJaxb(req);
                wsID = null;
            }
        }
    }

    @VisibleForTesting
    public String getWSId() {
        return wsID;
    }
}
