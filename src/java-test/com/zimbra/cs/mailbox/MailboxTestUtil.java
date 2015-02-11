/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import java.io.File;
import java.io.FileFilter;
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
import org.apache.solr.common.SolrException;

import com.google.common.base.Strings;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.IndexingQueueAdapter;
import com.zimbra.cs.index.IndexingService;
import com.zimbra.cs.index.solr.EmbeddedSolrIndex;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.http.HttpStoreManagerTest.MockHttpStoreManager;
import com.zimbra.cs.store.http.MockHttpStore;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraConfig;
import com.zimbra.soap.DefaultSoapSessionFactory;
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
        zimbraServerDir = Strings.nullToEmpty(zimbraServerDir);
        System.setProperty("log4j.configuration", "log4j-test.properties");
        // Don't load from /opt/zimbra/conf
        System.setProperty("zimbra.config", zimbraServerDir + "src/java-test/localconfig-test.xml");
        LC.reload();
        LC.zimbra_attrs_directory.setDefault(zimbraServerDir + "conf/attrs");
        LC.zimbra_rights_directory.setDefault(zimbraServerDir + "conf/rights");
        LC.timezone_file.setDefault(zimbraServerDir + "conf/timezones.ics");

        // default MIME handlers are now set up in MockProvisioning constructor
        Provisioning.setInstance(new MockProvisioning());
        Provisioning.getInstance().getLocalServer().setIndexThreads(1);
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

    public static void initServer(Class<? extends StoreManager> storeManagerClass, String zimbraServerDir) throws Exception {
        initServer(storeManagerClass, zimbraServerDir, ZimbraConfig.class);
    }

    public static void initServer(Class<? extends StoreManager> storeManagerClass, String zimbraServerDir, Class configClass) throws Exception {
        initProvisioning(zimbraServerDir);
        LC.zimbra_mailbox_groups.setDefault(1);
        DebugConfig.setNumMailboxGroup(1);
        DebugConfig.setDisableShareExpirationListener(true);
        LC.zimbra_class_database.setDefault(HSQLDB.class.getName());
        DbPool.startup();
        HSQLDB.createDatabase(zimbraServerDir, false);
        
        //use EmbeddedSolrIndex for indexing, because Solr webapp is nor running
        LC.zimbra_class_index_store_factory.setDefault(EmbeddedSolrIndex.Factory.class.getName());
        IndexStore.setFactory(LC.zimbra_class_index_store_factory.value());
        LC.zimbra_class_store.setDefault(storeManagerClass.getName());
        LC.zimbra_class_soapsessionfactory.setDefault(DefaultSoapSessionFactory.class.getName());
        deleteAllIndexFolders();
        setupEmbeddedSolrDirs(true);
        
        Zimbra.startupTest(configClass);
        MailboxManager.setInstance(Zimbra.getAppContext().getBean(MailboxManager.class));
        StoreManager.getInstance().startup();
        
        //set server into synchronous indexing mode
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);
        
        //disable indexing queue
        Provisioning.getInstance().getLocalServer().setIndexingQueueProvider("");
        
        //stop indexing service, it does not do anything without an indexing queue
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();
        
    }

    private static void setupEmbeddedSolrDirs(boolean recreateIfExists) throws Exception {
        File solrDir = new File("../ZimbraServer/build/test/solr/");
        if (recreateIfExists && solrDir.exists()) {
            try {
                FileUtils.deleteDirectory(solrDir);
            } catch (IOException e) {
                Thread.sleep(1000);
                try {
                    FileUtils.cleanDirectory(solrDir);
                    FileUtils.deleteDirectory(solrDir);
                } catch (IOException e2) {
                    ZimbraLog.test.error("cannot delete SOLR directory " + solrDir.getAbsolutePath());
                }
            }
        }
        if (!solrDir.exists()) {
            solrDir.mkdirs();
        }
        File solrXml = new File("../ZimbraServer/build/test/solr/solr.xml");
        if (solrXml.exists()) {
            solrXml.delete();
        }
        FileUtils.copyFile(new File("../ZimbraServer/src/test/resources/solr.xml"), solrXml);
        File configSets = new File("../ZimbraServer/build/test/solr/configsets/");
        if (configSets.exists()) {
            FileUtils.cleanDirectory(configSets);
        }
        else {
            configSets.mkdirs();
        }
        FileUtils.copyDirectory(new File("../ZimbraServer/conf/solr/configsets/"), configSets, new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return !pathname.getName().equalsIgnoreCase("solrconfig.xml");
            }

        });
        FileUtils.copyFile(new File("../ZimbraServer/src/test/resources/solrconfig.xml"), new File("../ZimbraServer/build/test/solr/configsets/zimbra/conf/solrconfig.xml"));
        File libsDir = new File("../ZimbraServer/build/test/solr/custom/");
        if (libsDir.exists()) {
            FileUtils.cleanDirectory(libsDir);
        } else {
            libsDir.mkdirs();
        }
        copytoSolrLibsDir(locateFile(new File("../SolrPlugins/target/"), "solrplugins-.*-SNAPSHOT\\.jar"), libsDir);
        copytoSolrLibsDir(locateFile(new File("../ZimbraCommon/ZimbraStoreCommon/target/"), "zimbracommon-.*\\.jar"), libsDir);
        File commonJarsDir = new File("../ZimbraCommon/jars/");
        copytoSolrLibsDir(locateFile(commonJarsDir, "guava-.*\\.jar"), libsDir);
        copytoSolrLibsDir(locateFile(commonJarsDir, "mail-.*\\.jar"), libsDir);
        copytoSolrLibsDir(locateFile(commonJarsDir, "lucene-core-.*\\.jar"), libsDir);
        copytoSolrLibsDir(locateFile(commonJarsDir, "lucene-analyzers-common-.*\\.jar"), libsDir);
        copytoSolrLibsDir(locateFile(commonJarsDir, "lucene-queryparser-.*\\.jar"), libsDir);
    }

    private static File locateFile(File directory, String regex) throws ServiceException {
        File[] matchingFiles = directory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.matches(regex);
            }
        });
        if (matchingFiles == null || matchingFiles.length == 0) {
            //The can happen in certain build scenarios; when the required projects aren't built yet.
            //In this case, we want to silently fall through, since the embedded solr directory will have been created by maven.
            return null;
        }
        if (matchingFiles.length > 1) {
            throw ServiceException.FAILURE(String.format("multiple files matching pattern found", regex), new Throwable());
        } else {
            return matchingFiles[0];
        }

    }
    private static void copytoSolrLibsDir(File from, File toDir) throws IOException {
        if (from == null) {
            return;
        }
        File dest = new File(toDir, from.getName());
        FileUtils.copyFile(from, dest);
    }

    /**
     * Clears the database and index.
     */
    public static void clearData() throws Exception {
        clearData("");
    }

    /**
     * Clears the database and index.
     * @param zimbraServerDir the directory that contains the ZimbraServer project
     */
    public static void clearData(String zimbraServerDir) throws Exception {
        HSQLDB.clearDatabase(zimbraServerDir);
        MailboxManager.getInstance().clearCache();
        IndexStore.getFactory().destroy();
        try {
            cleanupAllIndexStores();
        } catch (SolrException ex) {
            //ignore. We are deleting the folders anyway
        }
        if(Zimbra.getAppContext().getBean(IndexingQueueAdapter.class) != null) {
            Zimbra.getAppContext().getBean(IndexingQueueAdapter.class).drain();
        }
        
        StoreManager sm = StoreManager.getInstance();
        if (sm instanceof MockStoreManager) {
            ((MockStoreManager) sm).purge();
        } else if (sm instanceof MockHttpStoreManager) {
            MockHttpStore.purge();
        }
        DocumentHandler.resetLocalHost();
        setupEmbeddedSolrDirs(true);
    }

    public static void deleteAllIndexFolders() throws Exception {
        File[] cores = new File("../ZimbraServer/build/test/solr/").listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return (!name.equals("configsets") &&
                        !name.equals("custom") &&
                        !name.equals("solr.xml"));
            }
        });

        if(cores != null) {
            for (File core : cores) {
                try {
                    FileUtils.cleanDirectory(core);
                    FileUtils.deleteDirectory(core);
                } catch (IOException e2) {
                    ZimbraLog.test.error("cannot delete SOLR directory " + core.getAbsolutePath());
                }
            }
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

    public static void waitForIndexing(Mailbox mbox) throws ServiceException {

        int timeWaited = 0;
        int waitIncrement  = 100;
        int maxWaitTime = 5000;

        IndexStore indexStore = mbox.index.getIndexStore();
        
        //wait for index to be initialized
        while(!indexStore.indexExists() && timeWaited < maxWaitTime) {
            try {
                Thread.sleep(waitIncrement);
                timeWaited += waitIncrement;
            } catch (InterruptedException e) {
            }
        }

        //time check
        if (timeWaited >= maxWaitTime) {
            throw ServiceException.FAILURE(String.format("Mailbox %s is taking longer than %d ms waiting for IndexStore to get initialized.", mbox.getAccountId(), maxWaitTime), new Throwable());
        }
        
        //wait for the indexing queue to be empty (should be empty at this point, unless we got here before IndexingService got the head of the queue)
        if(Zimbra.getAppContext().getBean(IndexingQueueAdapter.class) != null) {
            //wait until all indexing threads are done
            while (Zimbra.getAppContext().getBean(IndexingService.class).isRunning() && 
                    Zimbra.getAppContext().getBean(IndexingService.class).getNumActiveTasks() > 0 && 
                        timeWaited < maxWaitTime) {
                try {
                    Thread.sleep(waitIncrement);
                    timeWaited += waitIncrement;
                } catch (InterruptedException e) {
                }
            }
            
            //time check
            if (timeWaited >= maxWaitTime) {
                throw ServiceException.FAILURE(String.format("Mailbox %s is taking longer than %d ms waiting for IndexingService to finish all tasks.", mbox.getAccountId(), maxWaitTime), new Throwable());
            }
            
            //wait for indexing queue to be emptied
            while(Zimbra.getAppContext().getBean(IndexingService.class).isRunning() &&
                    Zimbra.getAppContext().getBean(IndexingQueueAdapter.class).peek() != null && 
                        timeWaited < maxWaitTime) {
                try {
                    Thread.sleep(waitIncrement);
                    timeWaited += waitIncrement;
                } catch (InterruptedException e) {
                }
            }
            
            //time check
            if (timeWaited >= maxWaitTime) {
                throw ServiceException.FAILURE(String.format("Mailbox %s is taking longer than %d ms waiting for indexing queue to be emptied.", mbox.getAccountId(), maxWaitTime), new Throwable());
            }
            
            //wait for batch re-index counter to go to 0
            int completed = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class).getSucceededMailboxTaskCount(mbox.getAccountId());
            int failed = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class).getFailedMailboxTaskCount(mbox.getAccountId());
            int total = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class).getTotalMailboxTaskCount(mbox.getAccountId()); 
            while(Zimbra.getAppContext().getBean(IndexingService.class).isRunning() && completed + failed < total && total > 0 && timeWaited < maxWaitTime) {
                try {
                    Thread.sleep(waitIncrement);
                    timeWaited += waitIncrement;
                    failed = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class).getFailedMailboxTaskCount(mbox.getAccountId());
                    completed = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class).getSucceededMailboxTaskCount(mbox.getAccountId());
                    total = Zimbra.getAppContext().getBean(IndexingQueueAdapter.class).getTotalMailboxTaskCount(mbox.getAccountId()); 
                } catch (InterruptedException e) {
                }
            }
            
            //time check
            if (timeWaited >= maxWaitTime) {
                throw ServiceException.FAILURE(String.format("Mailbox %s is taking longer than %d ms waiting for indexing task counter to go to 0. Current task counter: %d", mbox.getAccountId(), maxWaitTime,  Zimbra.getAppContext().getBean(IndexingQueueAdapter.class).getSucceededMailboxTaskCount(mbox.getAccountId())), new Throwable());
            }
        }
        
        //now wait for EmbeddedSolrServer to finish processing update requests
        ((EmbeddedSolrIndex)(mbox.index.getIndexStore())).waitForIndexCommit(((EmbeddedSolrIndex)mbox.index.getIndexStore()).getSolrServer());
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
