/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.rmgmt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Constants;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

public class RemoteManager {

	private static File mPrivateKey;
	
	private final String mUser;
	private final String mHost;
	private final int mPort;
	private final String mShimCommand;
	private final String mDescription;
	
	public RemoteManager(Server remote) throws ServiceException {
	    mHost = remote.getAttr(Provisioning.A_zimbraServiceHostname, null);
	    if (mHost == null) throw ServiceException.FAILURE("server " + remote.getName() + " does not have a service host name", null);
	    
	    mPort = remote.getIntAttr(Provisioning.A_zimbraRemoteManagementPort, -1);
	    if (mPort < 0) throw ServiceException.FAILURE("server " + remote.getName() + " has invalid " + Provisioning.A_zimbraRemoteManagementPort, null);
	    
	    mUser = remote.getAttr(Provisioning.A_zimbraRemoteManagementUser, null);
	    if (mUser == null) throw ServiceException.FAILURE("server " + remote.getName() + " has no " + Provisioning.A_zimbraRemoteManagementUser, null);

	    mShimCommand = remote.getAttr(Provisioning.A_zimbraRemoteManagementCommand, null);
	    if (mShimCommand == null) throw ServiceException.FAILURE("server " + remote.getName() + " has no " + Provisioning.A_zimbraRemoteManagementCommand, null);
		
	    String localName = null;
	    synchronized (RemoteManager.class) {
			if (mPrivateKey == null) {
				Server local = Provisioning.getInstance().getLocalServer();
				localName = local.getName(); 
				String privateKey = local.getAttr(Provisioning.A_zimbraRemoteManagementPrivateKeyPath, null);
				if (privateKey == null) {
					throw ServiceException.FAILURE("server " + localName + " has no " + Provisioning.A_zimbraRemoteManagementPrivateKeyPath, null);
				}
				
				File key = new File(privateKey);
				if (!key.exists()) {
					throw ServiceException.FAILURE("server " + localName + " " + Provisioning.A_zimbraRemoteManagementPrivateKeyPath + " does not exist", null);
				}
				if (!key.canRead()) {
					throw ServiceException.FAILURE("server " + localName + " " + Provisioning.A_zimbraRemoteManagementPrivateKeyPath + " is not readable", null);
				}
				mPrivateKey = key;
			}
		}

	    mDescription = "{RemoteManager: " + localName + "->" + mUser + "@" + mHost + ":" + mPort + "}";
	}

    public String toString() {
    	return mDescription; 
    }
    
    private void executeBackground0(String command, RemoteBackgroundHandler handler) {
        Session s = null;
        try {
            s = getSession();
            s.execCommand(mShimCommand);
            OutputStream os = s.getStdin();
            String send = "HOST:" + mHost + " " + command; 
            os.write(send.getBytes());
            os.close();
            InputStream stdout = new StreamGobbler(s.getStdout());
            InputStream stderr = new StreamGobbler(s.getStderr());
            handler.read(stdout, stderr);
        } catch (Throwable t) {
            handler.error(t);
        } finally {
            if (s != null) {
                releaseSession(s);
            }
        }
    }
    
    public void executeBackground(final String command, final RemoteBackgroundHandler handler) {
        Runnable r = new Runnable() {
            public void run() {
                executeBackground0(command, handler);
            }
        };
        
        Thread t = new Thread(r);
        t.setName(this + "-" + command);
        t.setDaemon(true);
        t.start();
    }
	
    public RemoteResult execute(String command) throws ServiceException {
		Session s = null;
		try {
		    s = getSession();
			s.execCommand(mShimCommand);
            OutputStream os = s.getStdin();
            String send = "HOST:" + mHost + " " + command; 
            os.write(send.getBytes());
            os.close();

            RemoteResult result = new RemoteResult();

            InputStream stdout = new StreamGobbler(s.getStdout());
            InputStream stderr = new StreamGobbler(s.getStderr());
            result.mStdout = ByteUtil.getContent(stdout, -1);
            result.mStderr = ByteUtil.getContent(stderr, -1);
            result.mExitStatus = s.getExitStatus();
            if (result.mExitStatus != 0) {
                throw new IOException("command failed" + result.mExitStatus);
            }
            result.mExitSignal = s.getExitSignal();
			return result;
		} catch (IOException ioe) {
            throw ServiceException.FAILURE("exception executing command: " + command + " with " + this, ioe);
		} finally {
            if (s != null) {
                releaseSession(s);
            }
		}
	}
	
    private static class ConnectionReaperThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable runnable) {
            Thread t = new Thread(runnable, "RemoteManager-ConnectionReaper");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    private class ConnectionReaper implements Runnable {
        public void run() {
        	synchronized (mConnectionLock) {
        		if (mConnection == null) {
        			if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("reaper: connection is null " + this);
        			return;
        		}
        		
        		if (mSessionsActive > 0) {
        			// Session release will take care of scheduling idle check
        			if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("sessions (" + mSessionsActive + ") are active " + this);
        			return;
        		}
        		
        		long now = System.currentTimeMillis();
        		long elapsed = now - mLastSessionTimestamp;
        		if (elapsed < IDLE_CONNECTION_LIFETIME_MILLIS) {
        			if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("check for idle in " + (IDLE_CONNECTION_LIFETIME_MILLIS - elapsed) + " millis " + this);
        			mConnectionReaperExecutor.schedule(this, IDLE_CONNECTION_LIFETIME_MILLIS - elapsed, TimeUnit.MILLISECONDS);
        			return;
        		}
        				
    			if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("connection reaped " + this);
        		mConnection.close();
        		mConnection = null;
        	}
        }
    }

	private Object mConnectionLock = new Object();

    private static final ScheduledThreadPoolExecutor mConnectionReaperExecutor = new ScheduledThreadPoolExecutor(1, new ConnectionReaperThreadFactory());

    private ConnectionReaper mConnectionReaper = new ConnectionReaper();
    
    private long mLastSessionTimestamp;
    
    private static final long IDLE_CONNECTION_LIFETIME_MILLIS = 5 * Constants.MILLIS_PER_MINUTE;
    	
	private int mSessionsActive;
	
	private Connection mConnection;
	
	private void releaseSession(Session sess) {
		try {
			sess.close();
		} finally {
			synchronized (mConnectionLock) {
				mSessionsActive--;
				mLastSessionTimestamp = System.currentTimeMillis();
				if (mSessionsActive == 0) {
					mConnectionReaperExecutor.schedule(mConnectionReaper, IDLE_CONNECTION_LIFETIME_MILLIS, TimeUnit.MILLISECONDS);
				}
			}
		}
	}
	
	private Session getSession() throws ServiceException {
		synchronized (mConnectionLock) {
			try {
				if (mConnection == null) {
					Connection c = new Connection(mHost, mPort);
                    c.connect();
					if (!c.authenticateWithPublicKey(mUser, mPrivateKey, null)) {
						throw new IOException("auth failed");
					}
					mConnection = c;
				}
				
				Session sess = mConnection.openSession();
				mSessionsActive++;
				return sess;
			} catch (IOException ioe) {
				throw ServiceException.FAILURE("exception during auth " + this, ioe);
			}
		}
	}
	
	private static Map<String,RemoteManager> mRemoteManagerCache = new HashMap<String,RemoteManager>();
	
	public static RemoteManager getRemoteManager(Server server) throws ServiceException {
		synchronized (mRemoteManagerCache) {
			RemoteManager rm = mRemoteManagerCache.get(server.getId());
			if (rm != null) {
				return rm;
			}
			
			rm = new RemoteManager(server);
			mRemoteManagerCache.put(server.getId(), rm);
			return rm;
		}
	}
    
    public static void main(String[] args) throws Exception {
        Zimbra.toolSetup("DEBUG");
        Provisioning prov = Provisioning.getInstance();
        Server remote = prov.getServerByName(args[0]);
        RemoteManager rm = RemoteManager.getRemoteManager(remote);
        RemoteResult rr = rm.execute(args[1]);
        Map<String,String> m = RemoteResultParser.parseSingleMap(rr); 
        if (m == null) {
            System.out.println("NO RESULT RETURNED");
        } else {
            for (String k : m.keySet()) {
                System.out.println(k + "=" + m.get(k));
            }
        }
    }
}
