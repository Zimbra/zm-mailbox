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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
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
	public abstract String getName();
	public abstract boolean propagateFreeBusy(String email, FreeBusy fb);
	
	public static void register(FreeBusyProvider p) {
		sPROVIDERS.add(p);
		if (p.canCacheZimbraUserFreeBusy()) {
			String name = p.getName();
			FreeBusySyncQueue queue = sPUSHQUEUES.get(name);
			if (queue != null) {
				ZimbraLog.misc.warn("free/busy provider "+name+" has been already registered.");
			}
			queue = new FreeBusySyncQueue(p);
			sPUSHQUEUES.put(name, queue);
			new Thread(queue).start();
		}
	}
	
	public static void mailboxChanged(Mailbox mbox) {
		mailboxChanged(mbox.getAccountId());
	}
	public static void mailboxChanged(String accountId) {
		if (sPUSHQUEUES.size() == 0)
			return;

		for (FreeBusyProvider prov : sPROVIDERS)
			if (prov.canCacheZimbraUserFreeBusy()) {
				FreeBusySyncQueue queue = sPUSHQUEUES.get(prov.getName());
				synchronized (queue) {
					if (queue.contains(accountId))
						continue;
					queue.addLast(accountId);
					try {
						queue.writeToDisk();
					} catch (IOException e) {
						ZimbraLog.misc.error("can't write to the queue "+queue.getFilename());
					}
					queue.notify();
				}
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
	
	protected FreeBusy getFreeBusy(String accountId) throws ServiceException {
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(accountId);
		if (mbox == null)
			return null;
		return mbox.getFreeBusy(cachedFreeBusyStartTime(), cachedFreeBusyEndTime());
	}
	
	protected String getEmailAddress(String accountId) throws ServiceException {
		Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.id, accountId);
		if (acct == null)
			return null;
		return acct.getName();
	}
	
	public FreeBusySyncQueue getSyncQueue() {
		return sPUSHQUEUES.get(getName());
	}
	public static FreeBusyProvider getProvider(String name) {
		for (FreeBusyProvider p : sPROVIDERS)
			if (p.getName().equals(name))
				return p;
		return null;
	}
	public static Set<FreeBusyProvider> getProviders() {
		HashSet<FreeBusyProvider> ret = new HashSet<FreeBusyProvider>();
		for (FreeBusyProvider p : sPROVIDERS)
			ret.add(p.getInstance());
		return ret;
	}
	private static HashSet<FreeBusyProvider> sPROVIDERS;
	private static HashMap<String,FreeBusySyncQueue> sPUSHQUEUES;
	
	public static final String QUEUE_DIR = "/opt/zimbra/fbqueue";
	
	static {
		sPROVIDERS = new HashSet<FreeBusyProvider>();
		sPUSHQUEUES = new HashMap<String,FreeBusySyncQueue>();
		register(new ExchangeFreeBusyProvider());
	}
	
	@SuppressWarnings("serial")
	public static class FreeBusySyncQueue extends LinkedList<String> implements Runnable {

		FreeBusySyncQueue(FreeBusyProvider prov) {
			mProvider = prov;
			mLastFailed = 0;
			mShutdown = false;
			mFilename = QUEUE_DIR + "/" + "queue-" + prov.getName();
			File f = new File(mFilename);
			if (!f.exists()) {
				f.getParentFile().mkdirs();
			}
			try {
				readFromDisk();
			} catch (IOException e) {
				ZimbraLog.misc.error("error reading from the queue", e);
			}
		}
		public void run() {
			Thread.currentThread().setName(mProvider.getName() + " Free/Busy Sync Queue");
			while (!mShutdown) {
				try {
					String acctId = null;
					synchronized (this) {
						if (size() > 0) {
							// wait for some interval when we detect a failure
							// such that we don't spin loop and keep hammering a down server.
							long now = System.currentTimeMillis();
							if (now < mLastFailed + RETRY_INTERVAL) {
								ZimbraLog.misc.debug("wait for a while..");
								wait(RETRY_INTERVAL);
								continue;
							}
							acctId = getFirst();
						} else
							wait();

					}
					if (acctId == null)
						continue;

					String email = mProvider.getEmailAddress(acctId);
					FreeBusy fb = mProvider.getFreeBusy(acctId);
					
					if (email == null || fb == null) {
						ZimbraLog.misc.warn("cannot fetch mailbox and/or free/busy for account "+acctId);
						continue;
					}
					
					boolean success = mProvider.propagateFreeBusy(email, fb);
					
					synchronized (this) {
						removeFirst();
					}
					if (!success) {
						synchronized (this) {
							addLast(acctId);
						}
						mLastFailed = System.currentTimeMillis();
					}
					writeToDisk();

				} catch (Exception e) {
					ZimbraLog.misc.error("error while syncing freebusy for "+mProvider.getName(), e);
				}
			}
		}
		public void shutdown() {
			mShutdown = true;
		}
		
		private boolean mShutdown;
		private long mLastFailed;
		private static final int RETRY_INTERVAL = 10 * 1000; // 10 sec
		private static final int MAX_FILE_SIZE = 10240;  // for sanity check
		private String mFilename;
		private FreeBusyProvider mProvider;
		
		public synchronized void writeToDisk() throws IOException {
			StringBuilder buf = new StringBuilder(Integer.toString(size()+1));
			for (String id : this)
				buf.append("\n").append(id);
			if (buf.length() > MAX_FILE_SIZE) {
				ZimbraLog.misc.error("The free/busy replication queue is too large. #elem="+size());
				return;
			}
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(mFilename);
				out.write(buf.toString().getBytes());
				out.getFD().sync();
			} finally {
				if (out != null)
					out.close();
			}
		}
		
		public synchronized void readFromDisk() throws IOException {
			File f = new File(mFilename);
			if (!f.exists())
				f.createNewFile();
			long len = f.length();
			if (len > MAX_FILE_SIZE) {
				ZimbraLog.misc.error("The free/busy replication queue is too large: "+mFilename+" ("+len+")");
				return;
			}
			FileInputStream in = null;
			String[] tokens = null;
			try {
				in = new FileInputStream(f);
				byte[] buf = ByteUtil.readInput(in, (int)len, MAX_FILE_SIZE);
				tokens = new String(buf, "UTF-8").split("\n");
			} finally {
				if (in != null)
					in.close();
			}
			if (tokens.length < 2)
				return;
			int numTokens = Integer.parseInt(tokens[0]);
			if (numTokens != tokens.length) {
				ZimbraLog.misc.error("The free/busy replication queue is inconsistent: "
						+"numTokens="+numTokens+", actual="+tokens.length);
				return;
			}
			clear();
			Collections.addAll(this, tokens);
			removeFirst();
		}
		
		public String getFilename() {
			return mFilename;
		}
	}
}
