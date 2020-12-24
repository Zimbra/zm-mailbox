/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.rmgmt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.security.SecurityUtils;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.util.Zimbra;



public class RemoteManager {

    private static final int DEFAULT_REMOTE_MANAGEMENT_PORT = 22;
    private static final String DEFAULT_REMOTE_MANAGEMENT_USER = "zimbra";
    private static final String DEFAULT_REMOTE_MANAGEMENT_COMMAND = "/opt/zimbra/libexec/zmrcd";

    private final File mPrivateKey;

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

    public String getPrivateKeyPath() {
        return mPrivateKey.getAbsolutePath();
    }

    @Override
    public String toString() {
        return mDescription;
    }

    public Integer getPort() {
	return mPort;
    }

    private synchronized void executeBackground0(String command, RemoteBackgroundHandler handler) {
        RemoteResult result = new RemoteResult();
        try {
            result = executeRemoteCommand(mUser,mHost,mPort,mPrivateKey,mShimCommand,command);
            if (ZimbraLog.rmgmt.isTraceEnabled()) {
                try {
                    ZimbraLog.rmgmt.trace("stdout content for cmd:\n%s", new String(result.mStdout, "UTF-8"));
                    ZimbraLog.rmgmt.trace("stderr content for cmd:\n%s", new String(result.mStderr, "UTF-8"));
                } catch (Exception ex) {
                    ZimbraLog.rmgmt.trace("Problem logging stdout or stderr for cmd - probably not UTF-8");
                }
            }
            InputStream stdout = new ByteArrayInputStream(result.mStdout);
            InputStream stderr = new ByteArrayInputStream(result.mStderr);
            handler.read(stdout,stderr);
        }
        catch (OutOfMemoryError e) {
            Zimbra.halt("out of memory", e);
        } catch (Throwable t) {
            handler.error(t);
        }
    }

    public void executeBackground(final String command, final RemoteBackgroundHandler handler) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                executeBackground0(command, handler);
            }
        };

        Thread t = new Thread(r);
        t.setName(this + "-" + command);
        t.setDaemon(true);
        t.start();
    }

    public synchronized RemoteResult execute(String command) throws ServiceException{
        RemoteResult result = new RemoteResult();
        try {
            result = executeRemoteCommand(mUser,mHost,mPort,mPrivateKey,mShimCommand,command);
            if (ZimbraLog.rmgmt.isTraceEnabled()) {
                try {
                    ZimbraLog.rmgmt.trace("stdout content for cmd:\n%s", new String(result.mStdout, "UTF-8"));
                    ZimbraLog.rmgmt.trace("stderr content for cmd:\n%s", new String(result.mStderr, "UTF-8"));
                } catch (Exception ex) {
                    ZimbraLog.rmgmt.trace("Problem logging stdout or stderr for cmd - probably not UTF-8");
                }
            }
        } catch (Exception ioe) {
             throw ServiceException.FAILURE("exception executing command: " + command + " with " + this, ioe);
        }
        return result;
    }

    public static RemoteResult executeRemoteCommand(String username, String host, int port, File privateKey,
			String mShimCommand, String command) throws Exception {
		long defaultTimeoutSeconds = 10l;
		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		String send = "HOST:" + host + " " + command;
		InputStream inputStream = new ByteArrayInputStream(send.getBytes());
		try (ClientSession session = client.connect(username, host, port)
				.verify(defaultTimeoutSeconds, TimeUnit.SECONDS).getSession()) {
			session.addPublicKeyIdentity(loadKeypair(privateKey.getAbsolutePath()));
			session.auth().verify(defaultTimeoutSeconds, TimeUnit.SECONDS);
			if (ZimbraLog.rmgmt.isDebugEnabled()) {
				ZimbraLog.rmgmt.debug("executing shim command '%s'", mShimCommand);
			}
			try (ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
					ByteArrayOutputStream errorResponseStream = new ByteArrayOutputStream();
					ClientChannel channel = session.createChannel("exec", mShimCommand + "\n")) {
				if (ZimbraLog.rmgmt.isDebugEnabled()) {
					ZimbraLog.rmgmt.debug("sending mgmt command '%s'", send);
				}
				channel.setIn(inputStream);
				channel.setOut(responseStream);
				channel.setErr(errorResponseStream);
				channel.open();
				try {
					channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED),
							TimeUnit.SECONDS.toMillis(defaultTimeoutSeconds));
					RemoteResult result = new RemoteResult();
					InputStream stdout = new ByteArrayInputStream(responseStream.toByteArray());
					InputStream stderr = new ByteArrayInputStream(errorResponseStream.toByteArray());
					result.mStdout = ByteUtil.getContent(stdout, -1);
					result.mStderr = ByteUtil.getContent(stderr, -1);
					result.mExitStatus = channel.getExitStatus();
					if (result.mExitStatus != 0) {
						throw new IOException("command failed: exit status=" + result.mExitStatus + ", stdout="
								+ new String(result.mStdout) + ", stderr=" + new String(result.mStderr));
					}
					result.mExitSignal = channel.getExitSignal();
					String errorString = new String(errorResponseStream.toByteArray());
					if (!errorString.isEmpty()) {
						throw new Exception(errorString);
					}
					return result;
				} finally {
					channel.close(false);
					session.close();
				}
			}
		} finally {
			client.stop();
		}
	}
	public static KeyPair loadKeypair(String privateKeyPath) throws IOException, GeneralSecurityException {
        try (InputStream privateKeyStream = new FileInputStream(privateKeyPath)) {
            Iterable<KeyPair> keyPairIterable =
                    SecurityUtils.loadKeyPairIdentities(null, null, privateKeyStream, null);
            KeyPair keyPair = keyPairIterable.iterator().next();
            return keyPair;
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
        Server remote = prov.get(Key.ServerBy.name, serverName);

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
