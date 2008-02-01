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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.mail.ToXML;

public abstract class FreeBusyProvider {
	protected static class Request {
		public Request(String em, long s, long e) {
			email = em; start = s; end = e;
		}
		String email;
		long start;
		long end;
		Object data;
	}
	@SuppressWarnings("serial")
	protected static class FreeBusyUserNotFoundException extends Exception {
	}
	public abstract FreeBusyProvider getInstance();
	public abstract void addFreeBusyRequest(Request req) throws FreeBusyUserNotFoundException;
	public abstract Set<FreeBusy> getResults();
	
	public abstract boolean canCacheZimbraUserFreeBusy();
	public abstract long cachedFreeBusyStartTime();
	public abstract long cachedFreeBusyEndTime();
	public abstract void setFreeBusyForZimbraUser(String email, FreeBusy fb);
	
	public static void register(FreeBusyProvider p) {
		sPROVIDERS.add(p);
		if (p.canCacheZimbraUserFreeBusy())
			sNEEDSPUSH = true;
	}
	
	public static void mailboxChanged(Mailbox mbox) {
		if (!sNEEDSPUSH)
			return;
		// XXX use publish / subscribe persistent queue
		for (FreeBusyProvider prov : sPROVIDERS)
			if (prov.canCacheZimbraUserFreeBusy())
				try {
					FreeBusy fb = mbox.getFreeBusy(prov.cachedFreeBusyStartTime(), prov.cachedFreeBusyEndTime());
					String name = mbox.getAccount().getName();
					prov.setFreeBusyForZimbraUser(name, fb);
				} catch (ServiceException se) {
					ZimbraLog.misc.error("can't get free/busy for user "+mbox.getAccountId(), se);
				}
	}
	
	public void addResults(Element response) {
		for (FreeBusy fb : getResults())
			ToXML.encodeFreeBusy(response, fb);
	}
	
	public static void getRemoteFreeBusy(Element response, List<String> remoteIds, long start, long end) {
		Set<FreeBusyProvider> providers = getProviders();
		for (String emailAddr : remoteIds) {
			Request req = new Request(emailAddr, start, end);
			FreeBusyProvider p = null;
			for (FreeBusyProvider prov : providers) {
				try {
					prov.addFreeBusyRequest(req);
					p = prov;
					break;
				} catch (FreeBusyUserNotFoundException e) {
				}
			}
			if (p == null) {
				ZimbraLog.misc.error("can't find free/busy provider for user "+emailAddr);
				ToXML.encodeFreeBusy(response, FreeBusy.emptyFreeBusy(emailAddr, start, end));
			}
		}
		
		for (FreeBusyProvider prov : providers) {
			prov.addResults(response);
		}
	}
	
	private static Set<FreeBusyProvider> getProviders() {
		HashSet<FreeBusyProvider> ret = new HashSet<FreeBusyProvider>();
		for (FreeBusyProvider p : sPROVIDERS)
			ret.add(p.getInstance());
		return ret;
	}
	private static HashSet<FreeBusyProvider> sPROVIDERS;
	private static boolean sNEEDSPUSH;
	
	static {
		sPROVIDERS = new HashSet<FreeBusyProvider>();
		sNEEDSPUSH = false;
		register(new ExchangeFreeBusyProvider());
	}
}
