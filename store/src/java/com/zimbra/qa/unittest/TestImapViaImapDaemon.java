 package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.zimbra.common.account.Key.CacheEntryBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.localconfig.LocalConfig;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.imap.ImapHandler;
import com.zimbra.cs.imap.ImapProxy.ZimbraClientAuthenticator;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.security.sasl.ZimbraAuthenticator;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.soap.admin.type.CacheEntryType;

  /**
   * Test the IMAP server provided by the IMAP daemon, doing the necessary configuration to make it work.
   *
   * Note: Currently bypasses Proxy, the tests connect directly to the IMAP daemon's port
   *
   * The actual tests that are run are in {@link SharedImapTests}
   */
  @Ignore("Need AdminWaitSet support at a minimum - which is not currently working for Zimbra-X")
  public class TestImapViaImapDaemon extends SharedImapTests {
      private static String IMAPD_CSV_FILE = "/opt/zimbra/zmstat/imapd.csv";
      private static String IMAPD_CSV_STATS_FILE = "/opt/zimbra/zmstat/imapd_stats.csv";

      @Before
      public void setUp() throws Exception  {
          getLocalServer();
          TestUtil.assumeTrue("remoteImapServerEnabled false for this server", imapServer.isRemoteImapServerEnabled());
          TestUtil.setLCValue(LC.imap_always_use_remote_store, String.valueOf(false));
          imapServer.setReverseProxyUpstreamImapServers(new String[] {imapServer.getServiceHostname()});
          // As we're connecting to the IMAP daemon's port directly, there is no harm
          // in having the other IMAP daemon running.  Also, currently, changing the value of these
          // settings may result in zmconfigd restarting mailboxd (due to attrs having
          // requiresRestart="mailbox" in zimbra-attrs.xml).  Obviously, that results in RunUnitTestsRequest
          // failing because the process it was talking to has disappeared!
          // imapServer.setImapServerEnabled(false);
          // imapServer.setImapSSLServerEnabled(false);
          super.sharedSetUp();
          TestUtil.flushImapDaemonCache(imapServer);
      }

      @After
      public void tearDown() throws Exception {
          super.sharedTearDown();
          if (imapHostname != null) {
              TestUtil.flushImapDaemonCache(imapServer);
              getAdminConnection().reloadLocalConfig();
          }
      }

      @Override
      protected int getImapPort() {
          return imapServer.getRemoteImapBindPort();
      }

      @Override
      protected void flushCacheIfNecessary() throws Exception {
          TestUtil.flushImapDaemonCache(imapServer);
      }

      @Test
      public void testClearDaemonCacheWrongAuthenticator() throws Exception {
          connection = connect();
          try {
              connection.flushCache(CacheEntryType.config.toString());
              fail("should not be able to flush the cache without authenticating");
          } catch (CommandFailedException cfe) {
              assertEquals("must be in AUTHENTICATED or SELECTED state", cfe.getError());
          }
          connection.login(PASS);
          try {
              connection.flushCache(CacheEntryType.config.toString());
              fail("should not be able to flush the cache without using X-ZIMBRA auth mechanism");
          } catch (CommandFailedException cfe) {
              assertEquals("must be authenticated as admin with X-ZIMBRA auth mechanism", cfe.getError());
          }
      }

      @Test
      public void testAddAccountLoggerWrongAuthenticator() throws Exception {
          connection = connect();
          Account account = TestUtil.getAccount(USER);
          try {
              connection.addAccountLogger(account, "zimbra.imap", "trace");
              fail("should not be able to add an account logger without authenticating");
          } catch (CommandFailedException cfe) {
              assertEquals("must be in AUTHENTICATED or SELECTED state", cfe.getError());
          }
          connection.login(PASS);
          try {
              connection.addAccountLogger(account, "zimbra.imap", "trace");
              fail("should not be able to add an account logger without using X-ZIMBRA auth mechanism");
          } catch (CommandFailedException cfe) {
              assertEquals("must be authenticated as admin with X-ZIMBRA auth mechanism", cfe.getError());
          }
      }

      @Test
      public void testAddAccountLogger() throws Exception {
          connection = getAdminConnection();
          Account account = TestUtil.getAccount(USER);
          try {
              connection.addAccountLogger(account, "zimbra.imap", "trace");
          } catch (CommandFailedException cfe) {
              fail("Couldn't add an account logger - " + cfe.getMessage());
          }
      }

      private void tryConnect(boolean shouldSucceed, String message) throws Exception {
          try {
              connection = connect(USER);
              connection.login(PASS);
              if (!shouldSucceed) {
                  fail(message);
              }
          } catch (CommandFailedException e) {
              if (shouldSucceed) {
                  fail(message);
              }
          }
      }

      private void enableImapAndCheckCachedValue(Account acct) throws Exception {
          acct.setImapEnabled(true);
          tryConnect(false, "should not be able to log in since imapd has old value of zimbraImapEnabled=FALSE");
      }

      private void disableImapAndCheckCachedValue(Account acct) throws Exception {
          acct.setImapEnabled(false);
          tryConnect(true, "should be able to log in since imapd has old value of zimbraImapEnabled=TRUE");
      }

    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")  // checking done in called methods
      @Test
      public void testClearDaemonCache() throws Exception {
          tryConnect(true, "should be able to log in initially"); //loads the account into the imapd cache

          Account acct = TestUtil.getAccount(USER);
          CacheEntry acctByName = new CacheEntry(CacheEntryBy.name, acct.getName());
          CacheEntry acctById = new CacheEntry(CacheEntryBy.id, acct.getId());
          ImapConnection adminConn = getAdminConnection();

          //verify that we can flush an account cache by name
          disableImapAndCheckCachedValue(acct);
          adminConn.flushCache("account", new CacheEntry[] { acctByName });
          tryConnect(false, "should not be able to log in after flushing LDAP cache by account name");

          //verify that we can flush an account cache by ID
          enableImapAndCheckCachedValue(acct);
          adminConn.flushCache("account", new CacheEntry[] { acctById });
          tryConnect(true, "should be able to log in after flushing LDAP cache by account ID");

          //verify that we can flush an account cache with multiple CacheEntries
          disableImapAndCheckCachedValue(acct);
          adminConn.flushCache("account", new CacheEntry[] { acctByName, acctById });
          tryConnect(false, "should not be able to log in after flushing LDAP cache by account name and ID");

          //verify that we can flush an account cache without a CacheEntry value
          enableImapAndCheckCachedValue(acct);
          adminConn.flushCache("account");
          tryConnect(true, "should be able to log in after flushing entire LDAP account cache");

          //verify that we can flush multiple cache types at once
          disableImapAndCheckCachedValue(acct);
          adminConn.flushCache("config,account");
          tryConnect(false, "should not be able to log in after flushing multiple cache types");

          //verify that we can flush all caches
          enableImapAndCheckCachedValue(acct);
          adminConn.flushCache("all");
          tryConnect(true, "should be able to log in after flushing all caches");
      }

      @Test
      public void testReloadLocalConfig() throws Exception {
          //to test this, we set imap_max_consecutive_error to 1 and make sure imapd disconnects after
          //the first failure
          ImapConnection adminConn = getAdminConnection();
          int savedMaxConsecutiveError = LC.imap_max_consecutive_error.intValue();

          //don't use TestUtil.setLCValue, since it issues a ReloadLocalConfigRequest, which will
          //send out its own RELOADLC request
          LocalConfig lc = new LocalConfig(null);
          lc.set(LC.imap_max_consecutive_error.key(), "1");
          lc.save();

          connection = connect(USER);
          connection.login(PASS);
          try {
              //sanity check: even though the LC value is changed, imapd should still be using the old value
              for (int i = 0; i < savedMaxConsecutiveError; i++) {
                  try {
                      connection.select("BAD");
                  } catch (CommandFailedException e) {
                      assertEquals("expected 'SELECT failed' error", "SELECT failed", e.getError());
                  }
              }
              try {
                  connection.select("INBOX");
                  fail("session should be disconnected due to too many consecutive errors");
              } catch (CommandFailedException e) {}

              //reload LC and reconnect; imapd should now be using the new value
              adminConn.reloadLocalConfig();
              connection = connect(USER);
              connection.login(PASS);
              try {
                  connection.select("BAD");
              } catch (CommandFailedException e) {
                  assertEquals("expected 'SELECT failed' error", "SELECT failed", e.getError());
              }
              try {
                  connection.select("INBOX");
                  fail("session should be disconnected after 1 error");
              } catch (CommandFailedException e) {}
          } finally {
              TestUtil.setLCValue(LC.imap_max_consecutive_error, String.valueOf(savedMaxConsecutiveError));
              adminConn.reloadLocalConfig();
          }
      }

      @Test
      public void testZimbraCommandsNonAdminUser() throws Exception {
          Account acct = TestUtil.getAccount(USER);
          AuthenticatorFactory authFactory = new AuthenticatorFactory();
          authFactory.register(ZimbraAuthenticator.MECHANISM, ZimbraClientAuthenticator.class);
          ImapConfig config = new ImapConfig(imapHostname);
          config.setMechanism(ZimbraAuthenticator.MECHANISM);
          config.setAuthenticatorFactory(authFactory);
          config.setPort(imapPort);
          config.setAuthenticationId(acct.getName());
          config.getLogger().setLevel(Log.Level.trace);
          ImapConnection conn = new ImapConnection(config);
          conn.connect();
          conn.authenticate(AuthProvider.getAuthToken(acct).getEncoded());
          try {
              conn.reloadLocalConfig();
              fail("should not be able to run X-ZIMBRA-RELOADLC command with a non-admin auth token");
          } catch (CommandFailedException cfe) {
              assertEquals("must be authenticated as admin with X-ZIMBRA auth mechanism", cfe.getError());
          }
          try {
              conn.flushCache("all");
              fail("should not be able to run X-ZIMBRA-FLUSHCACHE command with a non-admin auth token");
          } catch (CommandFailedException cfe) {
              assertEquals("must be authenticated as admin with X-ZIMBRA auth mechanism", cfe.getError());
          }
      }

      @Test
      public void imapdStatFiles() throws Exception {
          connection = this.connectAndSelectInbox(USER);
          connection.logout();
          connection = null;
          int max_time = 70 * 1000;  // Files are updated every minute, so max wait a little bit longer
          int sofar = 0;
          File testFile = new File(IMAPD_CSV_FILE);
          while (!testFile.exists() && (sofar < max_time)) {
              ZimbraLog.test.debug("%s file does not exist after %s seconds", IMAPD_CSV_FILE, sofar / 1000);
              Thread.sleep(1000);
              sofar += 1000;
          }
          assertTrue(String.format("%s file does not exist", testFile.getCanonicalPath()), testFile.exists());
          assertTrue(String.format("%s file is unexpectedly empty", testFile.getCanonicalPath()),
                  testFile.length()>0);
          testFile = new File(IMAPD_CSV_STATS_FILE);
          if (!testFile.exists()) {
              Thread.sleep(500);
          }
          assertTrue(String.format("%s file does not exist", testFile.getCanonicalPath()), testFile.exists());
          assertTrue(String.format("%s file is unexpectedly empty", testFile.getCanonicalPath()),
                  testFile.length()>0);
      }

      @Test
      public void flushNonImapCacheTypes() throws Exception {
          //Flushing these cache types won't do anything on the imapd server, but
          //we want to make sure that this does not fail;
          ImapConnection adminConn = getAdminConnection();
          Set<CacheEntryType> allTypes = new HashSet<CacheEntryType>(Arrays.asList(CacheEntryType.values()));
          Set<CacheEntryType> nonImapTypes = Sets.difference(allTypes, ImapHandler.IMAP_CACHE_TYPES);
          for (CacheEntryType type: nonImapTypes) {
              try {
                  adminConn.flushCache(CacheEntryType.license.toString());
              } catch (CommandFailedException e) {
                  fail("should be able issue X-ZIMBRA-FLUSHCACHE command for cache type " + type.toString());
              }
          }
      }
  }
