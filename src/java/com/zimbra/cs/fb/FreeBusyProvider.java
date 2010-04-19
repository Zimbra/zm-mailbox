/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.fb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.mail.ToXML;

public abstract class FreeBusyProvider {
	protected static class Request {
		public Request(Account req, String em, long s, long e, int f) {
			requestor = req; email = em; 
    		Calendar cal = GregorianCalendar.getInstance();
    		cal.setTimeInMillis(s);
    		cal.set(Calendar.SECOND, 0);
    		start = cal.getTimeInMillis();
    		cal.setTimeInMillis(e);
    		cal.set(Calendar.SECOND, 0);
    		end = cal.getTimeInMillis();
    		folder = f;
		}
		Account requestor;
		String email;
		long start;
		long end;
		int folder;
		Object data;
	}
	@SuppressWarnings("serial")
	protected static class FreeBusyUserNotFoundException extends Exception {
	}
	
	public abstract FreeBusyProvider getInstance();
	public abstract String getName();
	
	// free/busy lookup from 3rd party system
	public abstract void addFreeBusyRequest(Request req) throws FreeBusyUserNotFoundException;
	public abstract List<FreeBusy> getResults();

	// propagation of Zimbra users free/busy to 3rd party system
	public abstract boolean registerForMailboxChanges();
	public abstract int registerForItemTypes();
	public abstract boolean handleMailboxChange(String accountId);
	public abstract long cachedFreeBusyStartTime();
	public abstract long cachedFreeBusyEndTime();
	public abstract String foreignPrincipalPrefix();
	
	public static void register(FreeBusyProvider p) {
		synchronized (sPROVIDERS) {
			sPROVIDERS.add(p);
		}
	}
	
	private static FreeBusySyncQueue startConsumerThread(FreeBusyProvider p) {
		String name = p.getName();
		FreeBusySyncQueue queue = sPUSHQUEUES.get(name);
		if (queue != null) {
			ZimbraLog.fb.warn("free/busy provider "+name+" has been already registered.");
		}
		queue = new FreeBusySyncQueue(p);
		sPUSHQUEUES.put(name, queue);
		new Thread(queue).start();
		return queue;
	}
	
	public static void mailboxChanged(String accountId) {
		mailboxChanged(accountId, MailItem.typeToBitmask(MailItem.TYPE_APPOINTMENT));
	}
	public static void mailboxChanged(String accountId, int changedType) {
		for (FreeBusyProvider prov : sPROVIDERS)
			if (prov.registerForMailboxChanges() && (changedType & prov.registerForItemTypes()) > 0) {
				FreeBusySyncQueue queue = sPUSHQUEUES.get(prov.getName());
				if (queue == null)
					queue = startConsumerThread(prov);
				synchronized (queue) {
					if (queue.contains(accountId))
						continue;
					queue.addLast(accountId);
					try {
						queue.writeToDisk();
					} catch (IOException e) {
						ZimbraLog.fb.error("can't write to the queue "+queue.getFilename());
					}
					queue.notify();
				}
			}
	}
	
	public void addResults(Element response) {
		for (FreeBusy fb : getResults())
			ToXML.encodeFreeBusy(response, fb);
	}
	
	public static List<FreeBusy> getRemoteFreeBusy(Account requestor, List<String> remoteIds, long start, long end, int folder) {
		Set<FreeBusyProvider> providers = getProviders();
		ArrayList<FreeBusy> ret = new ArrayList<FreeBusy>();
		for (String emailAddr : remoteIds) {
			Request req = new Request(requestor, emailAddr, start, end, folder);
			boolean succeed = false;
			for (FreeBusyProvider prov : providers) {
				try {
					prov.addFreeBusyRequest(req);
					succeed = true;
					break;
				} catch (FreeBusyUserNotFoundException e) {
				}
			}
			if (!succeed) {
				ZimbraLog.fb.error("can't find free/busy provider for user "+emailAddr);
				ret.add(FreeBusy.emptyFreeBusy(emailAddr, start, end));
			}
		}
		
		for (FreeBusyProvider prov : providers) {
			ret.addAll(prov.getResults());
		}
		return ret;
	}
	
	public static void getRemoteFreeBusy(Account requestor, Element response, List<String> remoteIds, long start, long end, int folder) {
		for (FreeBusy fb : getRemoteFreeBusy(requestor, remoteIds, start, end, folder)) {
			ToXML.encodeFreeBusy(response, fb);
		}
	}
	
	protected FreeBusy getFreeBusy(String accountId, int folderId) throws ServiceException {
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(accountId);
		if (mbox == null)
			return null;
		return mbox.getFreeBusy(null, cachedFreeBusyStartTime(), cachedFreeBusyEndTime(), folderId);
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
	
	static {
		sPROVIDERS = new HashSet<FreeBusyProvider>();
		sPUSHQUEUES = new HashMap<String,FreeBusySyncQueue>();
		new ExchangeFreeBusyProvider();  // load the class
	}
	
	public String getQueueFilename() {
		if (!registerForMailboxChanges()) {
			return "(none)";
		}
		return LC.freebusy_queue_directory.value() + "queue-" + getName();
	}
	
	@SuppressWarnings("serial")
	public static class FreeBusySyncQueue extends LinkedList<String> implements Runnable {

		FreeBusySyncQueue(FreeBusyProvider prov) {
			mProvider = prov;
			mLastFailed = 0;
			mShutdown = false;
			mFilename = prov.getQueueFilename();
			File f = new File(mFilename);
			if (!f.exists()) {
				f.getParentFile().mkdirs();
			}
			try {
				readFromDisk();
			} catch (IOException e) {
				ZimbraLog.fb.error("error reading from the queue", e);
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
							long retryInterval = DEFAULT_RETRY_INTERVAL;
							try {
								retryInterval = Provisioning.getInstance().getLocalServer().getFreebusyPropagationRetryInterval();
							} catch (Exception e) {
							}
							if (now < mLastFailed + retryInterval) {
								wait(retryInterval);
								continue;
							}
							acctId = getFirst();
						} else
							wait();

					}
					if (acctId == null)
						continue;

					boolean success = mProvider.handleMailboxChange(acctId);
					
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
					mLastFailed = System.currentTimeMillis();
					ZimbraLog.fb.error("error while syncing freebusy for "+mProvider.getName(), e);
				}
			}
		}
		public void shutdown() {
			mShutdown = true;
		}
		
		private boolean mShutdown;
		private long mLastFailed;
		private static final int DEFAULT_RETRY_INTERVAL = 60 * 1000; // 1m
		private static final int MAX_FILE_SIZE = 10240;  // for sanity check
		private String mFilename;
		private FreeBusyProvider mProvider;
		
		public synchronized void writeToDisk() throws IOException {
			StringBuilder buf = new StringBuilder(Integer.toString(size()+1));
			for (String id : this)
				buf.append("\n").append(id);
			if (buf.length() > MAX_FILE_SIZE) {
				ZimbraLog.fb.error("The free/busy replication queue is too large. #elem="+size());
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
				ZimbraLog.fb.error("The free/busy replication queue is too large: "+mFilename+" ("+len+")");
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
				ZimbraLog.fb.error("The free/busy replication queue is inconsistent: "
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
