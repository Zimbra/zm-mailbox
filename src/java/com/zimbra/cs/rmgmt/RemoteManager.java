/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.rmgmt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.CliUtil;

public class RemoteManager {

    private static final int DEFAULT_REMOTE_MANAGEMENT_PORT = 22;
    private static final String DEFAULT_REMOTE_MANAGEMENT_USER = "zimbra";
    private static final String DEFAULT_REMOTE_MANAGEMENT_COMMAND = "/opt/zimbra/libexec/zmrcd";

    private File mPrivateKey;

    private final String mUser;
    private final String mHost;
    private final int mPort;
    private final String mShimCommand;
    private final String mDescription;

    private RemoteManager(Server remote) throws ServiceException {
        mHost = remote.getAttr(Provisioning.A_zimbraServiceHostname, null);
        if (mHost == null) throw ServiceException.FAILURE("server " + remote.getName() + " does not have a service host name", null);

        mPort = remote.getIntAttr(Provisioning.A_zimbraRemoteManagementPort, DEFAULT_REMOTE_MANAGEMENT_PORT);
        if (mPort < 0) throw ServiceException.FAILURE("server " + remote.getName() + " has invalid " + Provisioning.A_zimbraRemoteManagementPort, null);

        mUser = remote.getAttr(Provisioning.A_zimbraRemoteManagementUser, DEFAULT_REMOTE_MANAGEMENT_USER);
        if (mUser == null) throw ServiceException.FAILURE("server " + remote.getName() + " has no " + Provisioning.A_zimbraRemoteManagementUser, null);

        mShimCommand = remote.getAttr(Provisioning.A_zimbraRemoteManagementCommand, DEFAULT_REMOTE_MANAGEMENT_COMMAND);
        if (mShimCommand == null) throw ServiceException.FAILURE("server " + remote.getName() + " has no " + Provisioning.A_zimbraRemoteManagementCommand, null);

        Server local = Provisioning.getInstance().getLocalServer();
        String localName = local.getName();
        String privateKey = local.getAttr(Provisioning.A_zimbraRemoteManagementPrivateKeyPath, null);
        if (privateKey == null) {
            throw ServiceException.FAILURE("server " + localName + " has no " + Provisioning.A_zimbraRemoteManagementPrivateKeyPath, null);
        }

        File key = new File(privateKey);
        if (!key.exists()) {
            throw ServiceException.FAILURE("server " + localName + " " + Provisioning.A_zimbraRemoteManagementPrivateKeyPath + " (" + key + ") does not exist", null);
        }
        if (!key.canRead()) {
            throw ServiceException.FAILURE("server " + localName + " " + Provisioning.A_zimbraRemoteManagementPrivateKeyPath + " (" + key + ") is not readable", null);
        }
        mPrivateKey = key;

        mDescription = "{RemoteManager: " + localName + "->" + mUser + "@" + mHost + ":" + mPort + "}";
    }

    public String toString() {
        return mDescription;
    }

    private synchronized void executeBackground0(String command, RemoteBackgroundHandler handler) {
        Session s = null;
        try {
            s = getSession();
            if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("(bg) executing shim command  '" + mShimCommand + "' on " + this);
            s.execCommand(mShimCommand);
            OutputStream os = s.getStdin();
            String send = "HOST:" + mHost + " " + command;
            if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("(bg) sending mgmt command '" + send + "' on " + this);
            os.write(send.getBytes());
            os.close();
            InputStream stdout = new StreamGobbler(s.getStdout());
            InputStream stderr = new StreamGobbler(s.getStderr());
            handler.read(stdout, stderr);
        } catch (OutOfMemoryError e) {
            Zimbra.halt("out of memory", e);
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

    public synchronized RemoteResult execute(String command) throws ServiceException {
        Session s = null;
        try {
            s = getSession();
            if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("executing shim command  '" + mShimCommand + "' on " + this);
            s.execCommand(mShimCommand);
            OutputStream os = s.getStdin();
            String send = "HOST:" + mHost + " " + command;
            if (ZimbraLog.rmgmt.isDebugEnabled()) ZimbraLog.rmgmt.debug("sending mgmt command '" + send + "' on " + this);
            os.write(send.getBytes());
            os.close();

            RemoteResult result = new RemoteResult();

            InputStream stdout = new StreamGobbler(s.getStdout());
            InputStream stderr = new StreamGobbler(s.getStderr());
            result.mStdout = ByteUtil.getContent(stdout, -1);
            result.mStderr = ByteUtil.getContent(stderr, -1);
            if (false) {
                System.out.println("#### STDOUT");
                System.out.println(new String(result.mStdout));
                System.out.println("#### STDERR");
                System.out.println(new String(result.mStderr));
            }
            try {
                result.mExitStatus = s.getExitStatus();
            } catch (NullPointerException npe) {
                // wow this is strange - on hold command we hit NPE here.  TODO file a bug against ganymed
            }
            if (result.mExitStatus != 0) {
                throw new IOException(
                        "command failed: exit status=" + result.mExitStatus +
                        ", stdout=" + new String(result.mStdout) +
                        ", stderr=" + new String(result.mStderr));
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

    private Connection mConnection;

    private void releaseSession(Session sess) {
        try {
            sess.close();
        } finally {
            mConnection.close();
            mConnection = null;
        }
    }

    private Session getSession() throws ServiceException {
        try {
            mConnection = new Connection(mHost, mPort);
            mConnection.connect();
            if (!mConnection.authenticateWithPublicKey(mUser, mPrivateKey, null)) {
                throw new IOException("auth failed");
            }
            return mConnection.openSession();
        } catch (IOException ioe) {
            if (mConnection != null) {
                mConnection.close();
            }
            throw ServiceException.FAILURE("exception during auth " + this, ioe);
        }
    }

    public static RemoteManager getRemoteManager(Server server) throws ServiceException {
        return new RemoteManager(server);
    }

    public static void main(String[] args) throws Exception {
        int iterations = Integer.parseInt(args[0]);
        String serverName = args[1];
        String command = args[2];

        CliUtil.toolSetup("DEBUG");
        Provisioning prov = Provisioning.getInstance();
        Server remote = prov.get(ServerBy.name, serverName);

        for (int i = 0; i < iterations; i++) {
            RemoteManager rm = RemoteManager.getRemoteManager(remote);
            RemoteResult rr = rm.execute(command);
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
}
