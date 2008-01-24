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
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

public class RemoteFreeBusyProvider {
    public static void proxyRemoteItems(
            Map<String, Object> context, ZimbraSoapContext zc, Element response,
            long rangeStart, long rangeEnd, Map<String, StringBuilder> remote) {
        Provisioning prov = Provisioning.getInstance();
        for (Map.Entry<String, StringBuilder> entry : remote.entrySet()) {
            // String server = entry.getKey();
            String paramStr = entry.getValue().toString();
            String[] idStrs = paramStr.split(",");

            try {
                Element req = zc.getRequestProtocol().getFactory().createElement(MailConstants.GET_FREE_BUSY_REQUEST);
                req.addAttribute(MailConstants.A_CAL_START_TIME, rangeStart);
                req.addAttribute(MailConstants.A_CAL_END_TIME, rangeEnd);
                req.addAttribute(MailConstants.A_UID, paramStr);

                // hack: use the ID of the first user
                Account acct = prov.get(AccountBy.name, idStrs[0]);
                if (acct == null)
                    acct = prov.get(AccountBy.id, idStrs[0]);
                if (acct != null) {
                    Element remoteResponse = proxyRequest(req, context, acct.getId(), zc);
                    for (Element thisElt : remoteResponse.listElements())
                        response.addElement(thisElt.detach());
                } else {
                    ZimbraLog.calendar.debug("Account " + idStrs[0] + " not found while searching free/busy");
                }
            } catch (SoapFaultException e) {
                for (int i = 0; i < idStrs.length; i++)
                    addFailureInfo(response, rangeStart, rangeEnd, idStrs[i], e);
            } catch (ServiceException e) {
                for (int i = 0; i < idStrs.length; i++)
                    addFailureInfo(response, rangeStart, rangeEnd, idStrs[i], e);
            }
        }
    }

    protected static void addFailureInfo(Element response, long rangeStart, long rangeEnd, String idStr, Exception e) {
        //sLog.debug("Could not get FreeBusy data for id " + idStr, e);
        Element usr = response.addElement(MailConstants.E_FREEBUSY_USER);
        usr.addAttribute(MailConstants.A_ID, idStr);
        usr.addElement(MailConstants.E_FREEBUSY_NO_DATA)
           .addAttribute(MailConstants.A_CAL_START_TIME, rangeStart)
           .addAttribute(MailConstants.A_CAL_END_TIME, rangeEnd);
    }
    
    protected static Element proxyRequest(Element request, Map<String, Object> context, String acctId, ZimbraSoapContext zsc) throws ServiceException {
        // new context for proxied request has a different "requested account"
        ZimbraSoapContext zscTarget = new ZimbraSoapContext(zsc, acctId);
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.get(Provisioning.AccountBy.id, acctId);
        Server server = prov.getServer(acct);

        Element response = null;
        request.detach();

        // executing remotely; find out target and proxy there
        HttpServletRequest httpreq = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
        ProxyTarget proxy = new ProxyTarget(server.getId(), zsc.getRawAuthToken(), httpreq);
        response = proxy.dispatch(request, zsc).detach();

        return response;
    }
}
