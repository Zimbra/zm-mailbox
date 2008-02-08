/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.soap.SoapServlet;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.fb.FreeBusyProvider;
import com.zimbra.cs.fb.LocalFreeBusyProvider;
import com.zimbra.cs.fb.RemoteFreeBusyProvider;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.soap.ZimbraSoapContext;

public class GetFreeBusy extends MailDocumentHandler {

    
//    <GetFreeBusyRequest s="date" e="date" [uid="id,..."]/>
//    <GetFreeBusyResponse>
//      <usr id="id">
//        <f s="date" e="date"/>*
//        <b s="date" e="date"/>*
//        <t s="date" e="date"/>*
//        <o s="date" e="date"/>*
//      </usr>  
//    <GetFreeBusyResponse>
//
//    (f)ree (b)usy (t)entative and (o)ut-of-office
    
    private static final long MSEC_PER_DAY = 1000*60*60*24;
    
    private static final long MAX_PERIOD_SIZE_IN_DAYS = 200;

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zc = getZimbraSoapContext(context);
        
        long rangeStart = request.getAttributeLong(MailConstants.A_CAL_START_TIME);
        long rangeEnd = request.getAttributeLong(MailConstants.A_CAL_END_TIME);
        
        if (rangeEnd < rangeStart)
            throw ServiceException.INVALID_REQUEST("End time must be after Start time", null);

        long days = (rangeEnd - rangeStart) / MSEC_PER_DAY;
        if (days > MAX_PERIOD_SIZE_IN_DAYS)
            throw ServiceException.INVALID_REQUEST("Requested range is too large (Maximum "+MAX_PERIOD_SIZE_IN_DAYS+" days)", null);

        Element response = getResponseElement(zc);
        
        // MailConstants.A_UID should be deprecated at some point, bug 21776, comment #14
        String uidParam = request.getAttribute(MailConstants.A_UID, null);    // comma-separated list of account emails or zimbraId GUIDs that *must* match UUID format
        String idParam = request.getAttribute(MailConstants.A_ID, null);    // comma-separated list of account zimbraId GUIDs
        String nameParam = request.getAttribute(MailConstants.A_NAME, null); // comma-separated list of account emails

    	RemoteFreeBusyProvider remote = new RemoteFreeBusyProvider((HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST), zc, rangeStart, rangeEnd);
    	ArrayList<String> nonZimbraIds = new ArrayList<String>();

        String[] idStrs = null;
    	Account acct = null;

        // uidParam should be deprecated at some point, bug 21776 comment #14
    	if (uidParam != null) {
    		idStrs = uidParam.split(",");
    		for (String idStr : idStrs) {
    			acct = getAccountFromUid(idStr);
    			if (acct == null)
    				nonZimbraIds.add(idStr);
    			else
    				getFreeBusyForZimbraUser(response, acct, idStr, rangeStart, rangeEnd, remote);
    		}
    	}
    	if (idParam != null) {
    		idStrs = idParam.split(",");
    		for (String idStr : idStrs) {
    			acct = getAccountFromId(idStr);
    			if (acct == null)
    				nonZimbraIds.add(idStr);
    			else
    				getFreeBusyForZimbraUser(response, acct, idStr, rangeStart, rangeEnd, remote);
    		}
    	}
    	if (nameParam != null) {
    		idStrs = nameParam.split(",");
    		for (String idStr : idStrs) {
    			acct = getAccountFromName(idStr);
    			if (acct == null)
    				nonZimbraIds.add(idStr);
    			else
    				getFreeBusyForZimbraUser(response, acct, idStr, rangeStart, rangeEnd, remote);
    		}
    	}
    	remote.addResults(response);
        FreeBusyProvider.getRemoteFreeBusy(response, nonZimbraIds, rangeStart, rangeEnd);
        return response;
    }
    
    private Account getAccountFromUid(String uid) {
    	Provisioning prov = Provisioning.getInstance();
    	Account acct = null;
    	try {
    		if (Provisioning.isUUID(uid))
    			acct = prov.get(AccountBy.id, uid);
    		else
    			acct = prov.get(AccountBy.name, uid);
    	} catch (ServiceException e) {
    		acct = null;
    	}
    	return acct;
    }
    private Account getAccountFromId(String id) {
    	try {
    		return Provisioning.getInstance().get(AccountBy.id, id);
    	} catch (ServiceException e) {
    	}
    	return null;
    }
    private Account getAccountFromName(String name) {
    	try {
    		return Provisioning.getInstance().get(AccountBy.name, name);
    	} catch (ServiceException e) {
    	}
    	return null;
    }
    private void getFreeBusyForZimbraUser(Element response, Account acct, String user, long start, long end, RemoteFreeBusyProvider remote) {
    	// acct in LDAP is either local or remote.
    	try {
    		if (Provisioning.onLocalServer(acct)) {
    			FreeBusy fb = LocalFreeBusyProvider.getFreeBusy(acct, start, end);
    			ToXML.encodeFreeBusy(response, fb);
    			return;
    		} else {
    			remote.addFreeBusyRequest(acct, user, start, end);
    		}
    	} catch (ServiceException e) {
            ZimbraLog.misc.error("cannot get free/busy for "+user, e);
    	}
    }
}
