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

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.soap.ZimbraSoapContext;

public class FreeBusyQuery {
	
	private Account mRequestor;
	private long mStart;
	private long mEnd;
	
	private HashMap<String,Account> mTargets;
	
	// needed for proxying to another mailbox server
	private HttpServletRequest mReq;
	private ZimbraSoapContext mCtxt;
	
	public FreeBusyQuery(HttpServletRequest httpReq, ZimbraSoapContext zsc, Account requestor, long start, long end) {
		this(httpReq, requestor, start, end);
		mCtxt = zsc;
	}
	
	public FreeBusyQuery(HttpServletRequest httpReq, Account requestor, long start, long end) {
		mReq = httpReq;
		mRequestor = requestor;
		mStart = start;
		mEnd = end;
		mTargets = new HashMap<String,Account>();
	}
	
	public void addAccountId(String accountId) {
		addUser(accountId, getAccountFromId(accountId));
	}
	
	public void addEmailAddress(String emailAddr) {
		addUser(emailAddr, getAccountFromName(emailAddr));
	}
	
	public void addId(String id) {
		addUser(id, getAccountFromUid(id));
	}
	
	private void addUser(String id, Account acct) {
		mTargets.put(id, acct);
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
    
    private void prepareRequests(ArrayList<FreeBusy> local, RemoteFreeBusyProvider remote, ArrayList<String> external) {
    	for (String id : mTargets.keySet()) {
    		Account acct = mTargets.get(id);
    		if (acct == null) {
    			external.add(id);
    			continue;
    		}
    		try {
        		if (Provisioning.onLocalServer(acct)) {
        		    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        		    OperationContext octxt = null;
        		    if (mCtxt != null)
        		        octxt = new OperationContext(mCtxt.getAuthToken());
        		    else if (mRequestor != null)
        		        octxt = new OperationContext(mRequestor);
        		    local.add(mbox.getFreeBusy(octxt, id, mStart, mEnd));
        		} else {
        			remote.addFreeBusyRequest(mRequestor, acct, id, mStart, mEnd);
        		}
    		} catch (ServiceException e) {
                ZimbraLog.fb.error("cannot get free/busy for "+id, e);
    		}
    	}
    }
    
    public Collection<FreeBusy> getResults() {
    	RemoteFreeBusyProvider remote = new RemoteFreeBusyProvider(mReq, mCtxt, mStart, mEnd);
    	ArrayList<String> external = new ArrayList<String>();
    	ArrayList<FreeBusy> fbList = new ArrayList<FreeBusy>();
    	prepareRequests(fbList, remote, external);

    	fbList.addAll(remote.getResults());
    	if (external.size() > 0)
    		fbList.addAll(FreeBusyProvider.getRemoteFreeBusy(mRequestor, external, mStart, mEnd));
    	return fbList;
    }
    
	public void getResults(Element response) {
    	RemoteFreeBusyProvider remote = new RemoteFreeBusyProvider(mReq, mCtxt, mStart, mEnd);
    	ArrayList<String> external = new ArrayList<String>();
    	ArrayList<FreeBusy> fbList = new ArrayList<FreeBusy>();
    	prepareRequests(fbList, remote, external);

    	for (FreeBusy fb : fbList)
        	ToXML.encodeFreeBusy(response, fb);
    	remote.addResults(response);
    	if (external.size() > 0)
    		FreeBusyProvider.getRemoteFreeBusy(mRequestor, response, external, mStart, mEnd);
	}
}
