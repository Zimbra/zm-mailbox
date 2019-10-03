/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.fb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.http.HttpException;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.cs.httpclient.URLUtil;

public class FbCli {
    public static class FbProvider {
        public String name;
        public boolean propagate;
        public String queue;
        public String prefix;
        public long fbstart;
        public long fbend;
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(name).append("\n");
            buf.append("\t").append("propagate: ").append(propagate).append("\n");
            buf.append("\t").append("queue:     ").append(queue).append("\n");
            buf.append("\t").append("prefix:    ").append(prefix).append("\n");
            buf.append("\t").append("start:     ").append(new Date(fbstart)).append("\n");
            buf.append("\t").append("end:       ").append(new Date(fbend)).append("\n");
            return buf.toString();
        }
    }
    public static class FbQueue {
        public String name;
        Collection<String> accounts;
        public FbQueue() {
            accounts = new ArrayList<String>();
        }
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(name).append("\n");
            for (String acct : accounts) {
                buf.append("\t").append(acct).append("\n");
            }
            return buf.toString();
        }
    }
    public FbCli() throws ServiceException {
        setServer(LC.zimbra_zmprov_default_soap_server.value());
    }

    public void setServer(String server) {
        String adminUrl = URLUtil.getAdminURL(server);
        mTransport = new SoapHttpTransport(adminUrl);
    }

    public Collection<FbProvider> getAllFreeBusyProviders() throws ServiceException, IOException, HttpException {
        ArrayList<FbProvider> providers = new ArrayList<FbProvider>();
        try {
            auth();
            XMLElement req = new XMLElement(AdminConstants.GET_ALL_FREE_BUSY_PROVIDERS_REQUEST);
            Element resp = mTransport.invoke(req);
            for (Element provElem : resp.listElements(AdminConstants.E_PROVIDER)) {
                FbProvider prov = new FbProvider();
                prov.name = provElem.getAttribute(AdminConstants.A_NAME, null);
                prov.propagate = provElem.getAttributeBool(AdminConstants.A_PROPAGATE, false);
                prov.fbstart = provElem.getAttributeLong(AdminConstants.A_START, 0);
                prov.fbend = provElem.getAttributeLong(AdminConstants.A_END, 0);
                prov.queue = provElem.getAttribute(AdminConstants.A_QUEUE, null);
                prov.prefix = provElem.getAttribute(AdminConstants.A_PREFIX, null);
                providers.add(prov);
            }
        } finally {
            mTransport.shutdown();
        }
        return providers;
    }

    public Collection<FbQueue> getFreeBusyQueueInfo(String provider) throws ServiceException, IOException, HttpException {
        ArrayList<FbQueue> queues = new ArrayList<FbQueue>();
        try {
            auth();
            XMLElement req = new XMLElement(AdminConstants.GET_FREE_BUSY_QUEUE_INFO_REQUEST);
            if (provider != null)
                req.addElement(AdminConstants.E_PROVIDER).addAttribute(AdminConstants.A_NAME, provider);
            Element resp = mTransport.invoke(req);
            for (Element provElem : resp.listElements(AdminConstants.E_PROVIDER)) {
                FbQueue queue = new FbQueue();
                queue.name = provElem.getAttribute(AdminConstants.A_NAME, null);
                for (Element acctElem : provElem.listElements(AdminConstants.E_ACCOUNT)) {
                    queue.accounts.add(acctElem.getAttribute(AdminConstants.A_ID, null));
                }
                queues.add(queue);
            }
        } finally {
            mTransport.shutdown();
        }
        return queues;
    }

    public void pushFreeBusyForDomain(String domain) throws ServiceException, IOException, HttpException {
        try {
            auth();
            XMLElement req = new XMLElement(AdminConstants.PUSH_FREE_BUSY_REQUEST);
            req.addElement(AdminConstants.E_DOMAIN).addAttribute(AdminConstants.A_NAME, domain);
            mTransport.invoke(req);
        } finally {
            mTransport.shutdown();
        }
    }

    public void pushFreeBusyForAccounts(Collection<String> accounts) throws ServiceException, IOException, HttpException {
        try {
            auth();
            XMLElement req = new XMLElement(AdminConstants.PUSH_FREE_BUSY_REQUEST);
            for (String acct : accounts)
                req.addElement(AdminConstants.E_ACCOUNT).addAttribute(AdminConstants.A_ID, acct);
            mTransport.invoke(req);
        } finally {
            mTransport.shutdown();
        }
    }

    public void purgeFreeBusyQueue(String provider) throws ServiceException, IOException, HttpException {
        try {
            auth();
            XMLElement req = new XMLElement(AdminConstants.PURGE_FREE_BUSY_QUEUE_REQUEST);
            if (provider != null)
                req.addElement(AdminConstants.E_PROVIDER).addAttribute(AdminConstants.A_NAME, provider);
            mTransport.invoke(req);
        } finally {
            mTransport.shutdown();
        }
    }

    private void auth() throws ServiceException, IOException, HttpException {
        XMLElement req = new XMLElement(AdminConstants.AUTH_REQUEST);
        req.addElement(AdminConstants.E_NAME).setText(LC.zimbra_ldap_user.value());
        req.addElement(AdminConstants.E_PASSWORD).setText(LC.zimbra_ldap_password.value());
        Element resp = mTransport.invoke(req);
        mTransport.setAuthToken(resp.getElement(AccountConstants.E_AUTH_TOKEN).getText());
    }

    private SoapHttpTransport mTransport;

}
