/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.fb;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.soap.ProxyTarget;
import com.zimbra.soap.ZimbraSoapContext;

public class RemoteFreeBusyProvider {
	
	public RemoteFreeBusyProvider(HttpServletRequest httpReq, ZimbraSoapContext zsc, long start, long end) {
		mRemoteAccountMap = new HashMap<String,StringBuilder>();
		mHttpReq = httpReq;
		mSoapCtxt = zsc;
		mStart = start;
		mEnd = end;
	}
	
	private Map<String,StringBuilder> mRemoteAccountMap;
	private HttpServletRequest mHttpReq;
	private ZimbraSoapContext mSoapCtxt;
	private long mStart;
	private long mEnd;
	private StringBuilder mFailedAccounts;
	
	public void addRemoteAccount(Account account) {
		String hostname = account.getAttr(Provisioning.A_zimbraMailHost);
		String id = account.getId();
		StringBuilder buf = mRemoteAccountMap.get(hostname);
		if (buf == null)
			buf = new StringBuilder(id);
		else
			buf.append(",").append(id);
	}
	
	public void addResults(Element response) {
        Provisioning prov = Provisioning.getInstance();
        for (Map.Entry<String, StringBuilder> entry : mRemoteAccountMap.entrySet()) {
            // String server = entry.getKey();
            String paramStr = entry.getValue().toString();
            String[] idStrs = paramStr.split(",");

            try {
                Element req = mSoapCtxt.getRequestProtocol().getFactory().createElement(MailConstants.GET_FREE_BUSY_REQUEST);
                req.addAttribute(MailConstants.A_CAL_START_TIME, mStart);
                req.addAttribute(MailConstants.A_CAL_END_TIME, mEnd);
                req.addAttribute(MailConstants.A_UID, paramStr);

                // hack: use the ID of the first user
                Account acct = prov.get(AccountBy.name, idStrs[0]);
                if (acct == null)
                    acct = prov.get(AccountBy.id, idStrs[0]);
                if (acct != null) {
                    Element remoteResponse = proxyRequest(req, acct.getId(), mSoapCtxt);
                    for (Element thisElt : remoteResponse.listElements())
                        response.addElement(thisElt.detach());
                } else {
                    ZimbraLog.calendar.debug("Account " + idStrs[0] + " not found while searching free/busy");
                }
            } catch (SoapFaultException e) {
            	addFailedAccounts(paramStr);
            } catch (ServiceException e) {
            	addFailedAccounts(paramStr);
            }
        }
	}

	private void addFailedAccounts(String accts) {
		if (mFailedAccounts == null)
			mFailedAccounts = new StringBuilder(accts);
		else
			mFailedAccounts.append(",").append(accts);
	}
	
	public String getFailedAccounts() {
		if (mFailedAccounts == null)
			return "";
		return mFailedAccounts.toString();
	}
    
    protected Element proxyRequest(Element request, String acctId, ZimbraSoapContext zsc) throws ServiceException {
        // new context for proxied request has a different "requested account"
        ZimbraSoapContext zscTarget = new ZimbraSoapContext(zsc, acctId);
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(Provisioning.AccountBy.id, acctId);
        Server server = prov.getServer(acct);

        Element response = null;
        request.detach();

        // executing remotely; find out target and proxy there
        ProxyTarget proxy = new ProxyTarget(server.getId(), zsc.getRawAuthToken(), mHttpReq);
        response = proxy.dispatch(request, zscTarget).detach();

        return response;
    }
}
