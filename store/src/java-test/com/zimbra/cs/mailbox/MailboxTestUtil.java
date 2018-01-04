/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016, 2017 Synacor, Inc.
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

package com.zimbra.cs.mailbox;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.solr.common.SolrException;

import com.google.common.base.Strings;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.InMemoryEphemeralStore;
import com.zimbra.cs.ephemeral.migrate.InMemoryMigrationInfo;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.solr.EmbeddedSolrIndex;
import com.zimbra.cs.index.solr.MockSolrIndex;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.http.HttpStoreManagerTest.MockHttpStoreManager;
import com.zimbra.cs.store.http.MockHttpStore;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.DocumentHandler;

public final class MailboxTestUtil {

    private MailboxTestUtil() {
    }

    /**
     * Initializes the provisioning.
     */
    public static void initProvisioning() throws Exception {
        initProvisioning("");
    }

    /**
     * Initializes the provisioning.
     *
     * @param zimbraServerDir the directory that contains the ZimbraServer project
     * @throws Exception
     */
    public static void initProvisioning(String zimbraServerDir) throws Exception {
        zimbraServerDir = getZimbraServerDir(zimbraServerDir);
        System.setProperty("log4j.configuration", "log4j-test.properties");
        System.setProperty("zimbra.config", zimbraServerDir + "src/java-test/localconfig-test.xml");
        LC.reload();
        //substitute test TZ file
        String timezonefilePath = zimbraServerDir + "src/java-test/timezones-test.ics";
        File d = new File(timezonefilePath);
        if (!d.exists()) {
            throw new FileNotFoundException("timezones-test.ics not found in " + timezonefilePath);
        }
        LC.timezone_file.setDefault(timezonefilePath);
        LC.zimbra_rights_directory.setDefault(StringUtils.removeEnd(zimbraServerDir, "/") +"-conf" + "/conf/rights");
        LC.zimbra_attrs_directory.setDefault(zimbraServerDir + "conf/attrs");
        LC.zimbra_tmp_directory.setDefault(zimbraServerDir + "tmp");
        //substitute test DS config file
        String dsfilePath = zimbraServerDir + "src/java-test/datasource-test.xml";
        d = new File(dsfilePath);
        if (!d.exists()) {
            throw new FileNotFoundException("datasource-test.xml not found in " + dsfilePath);
        }
        LC.data_source_config.setDefault(dsfilePath);
        // default MIME handlers are now set up in MockProvisioning constructor
        Provisioning.setInstance(new MockProvisioning());
    }

    public static String getZimbraServerDir(String zimbraServerDir) {
        String serverDir = zimbraServerDir;
        if (StringUtil.isNullOrEmpty(serverDir)) {
            serverDir = Strings.nullToEmpty(System.getProperty("server.dir"));
            if (serverDir.isEmpty()) {
                serverDir = Strings.nullToEmpty(System.getProperty("user.dir"));
            }
        }
        if (!serverDir.endsWith("/")) {
            serverDir = serverDir + "/";
        }
        return serverDir;
    }

    /**
     * Initializes the provisioning, database, index and store manager.
     */
    public static void initServer() throws Exception {
        initServer(MockStoreManager.class);
    }

    /**
     * Initializes the provisioning, database, index and store manager.
     * @param zimbraServerDir the directory that contains the ZimbraServer project
     * @throws Exception
     */
    public static void initServer(String zimbraServerDir) throws Exception {
        initServer(MockStoreManager.class, zimbraServerDir);
    }

    public static void initServer(Class<? extends StoreManager> storeManagerClass) throws Exception {
        initServer(storeManagerClass, "");
    }

    public static void initServer(Class<? extends StoreManager> storeManagerClass, String zimbraServerDir, boolean OctopusInstance) throws Exception {
        MigrationInfo.setFactory(InMemoryMigrationInfo.Factory.class);
        EphemeralStore.setFactory(InMemoryEphemeralStore.Factory.class);
        initProvisioning(zimbraServerDir);

        LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
        DbPool.startup();
        HSQLDB.createDatabase(zimbraServerDir, OctopusInstance);

        IndexStore.registerIndexFactory("mock", MockSolrIndex.Factory.class.getName());
        IndexStore.registerIndexFactory("embeddedsolr",EmbeddedSolrIndex.Factory.class.getName());

        //use EmbeddedSolrIndex for indexing, because Solr webapp is nor running
        Provisioning.getInstance().getLocalServer().setIndexURL("embeddedsolr:local");
        IndexStore.setFactory(EmbeddedSolrIndex.Factory.class.getName());

        MailboxManager.setInstance(null);
        IndexStore.setFactory(LC.zimbra_class_index_store_factory.value());

        LC.zimbra_class_store.setDefault(storeManagerClass.getName());
        StoreManager.getInstance().startup();
    }

    public static void initServer(Class<? extends StoreManager> storeManagerClass, String zimbraServerDir) throws Exception {
        MigrationInfo.setFactory(InMemoryMigrationInfo.Factory.class);
        EphemeralStore.setFactory(InMemoryEphemeralStore.Factory.class);
        initProvisioning(zimbraServerDir);

        LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
        DbPool.startup();
        HSQLDB.createDatabase(zimbraServerDir, false);

        IndexStore.registerIndexFactory("mock", MockSolrIndex.Factory.class.getName());
        IndexStore.registerIndexFactory("embeddedsolr",EmbeddedSolrIndex.Factory.class.getName());

        //use EmbeddedSolrIndex for indexing, because Solr webapp is nor running
        Provisioning.getInstance().getLocalServer().setIndexURL("embeddedsolr:local");
        IndexStore.setFactory(EmbeddedSolrIndex.Factory.class.getName());

        MailboxManager.setInstance(null);

        LC.zimbra_class_store.setDefault(storeManagerClass.getName());
        StoreManager.getInstance().startup();

        //set server into synchronous indexing mode
        //Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);

    }

    /**
     * Clears the database and index.
     */
    public static void clearData() throws Exception {
        clearData("");
        MailboxManager.getInstance().clearAdditionalQuotaProviders();
    }

    /**
     * Clears the database and index.
     * @param zimbraServerDir the directory that contains the ZimbraServer project
     */
    public static void clearData(String zimbraServerDir) throws Exception {
        HSQLDB.clearDatabase(zimbraServerDir);
        MailboxManager.getInstance().cacheManager.clearCache();
        try {
            IndexStore.getFactory().destroy();
            cleanupAllIndexStores();
        } catch (SolrException ex) {
            //ignore. We are deleting the folders anyway
        }
        StoreManager sm = StoreManager.getInstance();
        if (sm instanceof MockStoreManager) {
            ((MockStoreManager) sm).purge();
        } else if (sm instanceof MockHttpStoreManager) {
            MockHttpStore.purge();
        }
        DocumentHandler.resetLocalHost();
        EphemeralStore.getFactory().shutdown();
    }

    private static void deleteDirContents(File dir) throws IOException {
        deleteDirContents(dir, 0);
    }

    private static void deleteDirContents(File dir, int recurCount) throws IOException {
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException ioe) {
            if (recurCount > 10) {
                throw new IOException("Gave up after multiple IOExceptions", ioe);
            }
            ZimbraLog.test.info("delete dir='%s' failed due to IOException '%s' (probably files still in use)."
                    + "Waiting a moment and trying again", dir, ioe.getMessage());
            //wait a moment and try again; this can bomb if files still being written by some thread
            try {
                Thread.sleep(2500);
            } catch (InterruptedException ie) {

            }
            deleteDirContents(dir, recurCount+1);
        }
    }

    public static void cleanupAllIndexStores() throws Exception {
        File[] cores = new File("../ZimbraServer/build/test/solr/").listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return (!name.equals("configsets") &&
                        !name.equals("custom") &&
                        !name.equals("solr.xml"));
            }
        });
        if(cores != null) {
            for (int i = 0; i < cores.length; i++) {
                try {
                    cleanupIndexStore(MailboxManager.getInstance().getMailboxByAccountId(cores[i].getName()));
                } catch (ServiceException | IOException e) {
                   Thread.sleep(1000);
                } finally {
                    deleteIndexDir(cores[i].getName());
                }
            }
        }
    }

<<<<<<< HEAD
    public static void cleanupIndexStore(Mailbox mbox) {
        IndexStore index = mbox.index.getIndexStore();
        if (index instanceof ElasticSearchIndex) {
            String key = mbox.getAccountId();
            String indexUrl = String.format("%s%s/", LC.zimbra_index_elasticsearch_url_base.value(), key);
            HttpRequestBase method = new HttpDelete(indexUrl);
            try {
                ElasticSearchConnector connector = new ElasticSearchConnector();
                int statusCode = connector.executeMethod(method);
                if (statusCode == HttpStatus.SC_OK) {
                    boolean ok = connector.getBooleanAtJsonPath(new String[] {"ok"}, false);
                    boolean acknowledged = connector.getBooleanAtJsonPath(new String[] {"acknowledged"}, false);
                    if (!ok || !acknowledged) {
                        ZimbraLog.index.debug("Delete index status ok=%b acknowledged=%b", ok, acknowledged);
                    }
                } else {
                    String error = connector.getStringAtJsonPath(new String[] {"error"});
                    if (error != null && error.startsWith("IndexMissingException")) {
                        ZimbraLog.index.debug("Unable to delete index for key=%s.  Index is missing", key);
                    } else {
                        ZimbraLog.index.error("Problem deleting index for key=%s error=%s", key, error);
                    }
                }
            } catch (IOException e) {
                ZimbraLog.index.error("Problem Deleting index with key=" + key, e);
=======
    private static void deleteIndexDir(String dir) throws IOException, InterruptedException {
        File f = new File("../ZimbraServer/build/test/solr/", dir);
        try {
            FileUtils.deleteDirectory(f);
        } catch (IOException e) {
            Thread.sleep(1000);
            try {
                FileUtils.cleanDirectory(f);
                FileUtils.deleteDirectory(f);
            } catch (IOException e2) {
                ZimbraLog.test.error("cannot delete SOLR directory " + f.getAbsolutePath());
>>>>>>> 4c0efa19ed... updated unit tests with API changes
            }
        }
    }

    public static void cleanupIndexStore(Mailbox mbox) throws ServiceException, IOException {
        if(mbox != null && mbox.index != null) {
            mbox.index.deleteIndex();
        }
    }

    public static void setFlag(Mailbox mbox, int itemId, Flag.FlagInfo flag) throws ServiceException {
        MailItem item = mbox.getItemById(null, itemId, MailItem.Type.UNKNOWN);
        int flags = item.getFlagBitmask() | flag.toBitmask();
        mbox.setTags(null, itemId, item.getType(), flags, null, null);
    }

    public static void unsetFlag(Mailbox mbox, int itemId, Flag.FlagInfo flag) throws ServiceException {
        MailItem item = mbox.getItemById(null, itemId, MailItem.Type.UNKNOWN);
        int flags = item.getFlagBitmask() & ~flag.toBitmask();
        mbox.setTags(null, itemId, item.getType(), flags, null, null);
    }

    public static ParsedMessage generateMessage(String subject) throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", "Bob Evans <bob@example.com>");
        mm.setHeader("To", "Jimmy Dean <jdean@example.com>");
        mm.setHeader("Subject", subject);
        mm.setText("nothing to see here");
        return new ParsedMessage(mm, false);
    }

    public static ParsedMessage generateHighPriorityMessage(String subject) throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", "Hi Bob <bob@example.com>");
        mm.setHeader("To", "Jimmy Dean <jdean@example.com>");
        mm.setHeader("Subject", subject);
        mm.addHeader("Importance", "high");
        mm.setText("nothing to see here");
        return new ParsedMessage(mm, false);
    }

    public static ParsedMessage generateLowPriorityMessage(String subject) throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", "Lo Bob <bob@example.com>");
        mm.setHeader("To", "Jimmy Dean <jdean@example.com>");
        mm.setHeader("Subject", subject);
        mm.addHeader("Importance", "low");
        mm.setText("nothing to see here");
        return new ParsedMessage(mm, false);
    }

    public static ParsedMessage generateMessageWithAttachment(String subject) throws Exception {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        mm.setHeader("From", "Vera Oliphant <oli@example.com>");
        mm.setHeader("To", "Jimmy Dean <jdean@example.com>");
        mm.setHeader("Subject", subject);
        mm.setText("Good as gold");
        MimeMultipart multi = new ZMimeMultipart("mixed");
        ContentDisposition cdisp = new ContentDisposition(Part.ATTACHMENT);
        cdisp.setParameter("filename", "fun.txt");

        ZMimeBodyPart bp = new ZMimeBodyPart();
        // MimeBodyPart.setDataHandler() invalidates Content-Type and CTE if there is any, so make sure
        // it gets called before setting Content-Type and CTE headers.
        try {
            bp.setDataHandler(new DataHandler(new ByteArrayDataSource("Feeling attached.", "text/plain")));
        } catch (IOException e) {
            throw new MessagingException("could not generate mime part content", e);
        }
        bp.addHeader("Content-Disposition", cdisp.toString());
        bp.addHeader("Content-Type", "text/plain");
        bp.addHeader("Content-Transfer-Encoding", MimeConstants.ET_8BIT);
        multi.addBodyPart(bp);

        mm.setContent(multi);
        mm.saveChanges();

        return new ParsedMessage(mm, false);
    }

    public static Invite generateInvite(Account account, String fragment,
                ZVCalendar cals) throws Exception {

        List<Invite> invites = Invite.createFromCalendar(account, fragment, cals,
            true);

        return invites.get(0);
    }
}
