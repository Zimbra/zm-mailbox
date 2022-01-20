/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.index;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxIndex;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.server.ProtocolHandler;
import com.zimbra.cs.server.TcpServer;
import com.zimbra.common.io.TcpServerInputStream;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.*;

/**
 * @since Jul 20, 2004
 */
public final class IndexEditor {

    static final int SEARCH_RETURN_CONVERSATIONS = 1;
    static final int SEARCH_RETURN_MESSAGES = 2;
    static final int SEARCH_RETURN_DOCUMENTS = 3;

    private static SortBy sortOrder = SortBy.DATE_DESC;
    private BufferedReader inputReader = null;
    private PrintStream outputStream = null;

    private static Log mLog = LogFactory.getLog(IndexEditor.class);

    public void deleteIndex(int mailboxId) throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mailboxId);
        try {
            mbox.index.deleteIndex();
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException", e);
        }
    }

    public void reIndexAll() {
        MailboxManager mmgr;
        try {
            mmgr = MailboxManager.getInstance();
        } catch (ServiceException e) {
            ZimbraLog.index.error("could not retrieve mailbox manager; aborting reindex", e);
            return;
        }
        int ids[] = mmgr.getMailboxIds();
        for (int i = 0; i < ids.length; i++) {
            mLog.info("Mailbox "+ids[i]+"\n");
            try {
                Mailbox mbx = mmgr.getMailboxById(ids[i]);
                mbx.index.startReIndex();
            } catch (ServiceException e) {
                mLog.info("Exception ReIndexing " + ids[i], e);
            }
        }
    }

    public void reIndex(int mailboxId) {
        try {
            Mailbox mbx = MailboxManager.getInstance().getMailboxById(mailboxId);
            mbx.index.startReIndex();
        } catch(Exception e) {
            outputStream.println("Re-index FAILED with " + ExceptionToString.ToString(e));
        }
    }

    public interface QueryRunner {
        ZimbraQueryResults runQuery(String qstr, Set<MailItem.Type> types, SortBy sortBy)
            throws IOException, MailServiceException, ServiceException;
    }

    public class SingleQueryRunner implements QueryRunner {
        int mMailboxId;

        SingleQueryRunner(int mailboxId) {
            mMailboxId = mailboxId;
        }

        @Override
        public ZimbraQueryResults runQuery(String qstr, Set<MailItem.Type> types, SortBy sortBy)
                throws IOException, MailServiceException, ServiceException {
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mMailboxId);
            SearchParams params = new SearchParams();
            params.setQueryString(qstr);
            params.setTypes(types);
            params.setSortBy(sortBy);
            params.setOffset(0);
            params.setLimit(100);
            params.setPrefetch(true);
            params.setFetchMode(SearchParams.Fetch.NORMAL);
            ZimbraQuery zq = new ZimbraQuery(null, SoapProtocol.Soap12, mbox, params);
            return zq.execute();
        }
    }

    public void doQuery(QueryRunner runner, boolean dump, int groupBy)
        throws MailServiceException, IOException, ServiceException {

        while (true) {
            outputStream.print("Query> ");
            String qstr = inputReader.readLine();
            if (qstr.equals("")) {
                return;
            }
            outputStream.println("\n\nTest 1: "+qstr);
            long startTime = System.currentTimeMillis();

            Set<MailItem.Type> types;
            switch (groupBy) {
            case SEARCH_RETURN_CONVERSATIONS:
                types = EnumSet.of(MailItem.Type.CONVERSATION);
                break;
            case SEARCH_RETURN_MESSAGES:
                types = EnumSet.of(MailItem.Type.MESSAGE);
                break;
            default:
                types = EnumSet.noneOf(MailItem.Type.class);
                break;
            }
            try (ZimbraQueryResults res = runner.runQuery(qstr, types, sortOrder)) {
                long endTime = System.currentTimeMillis();
                int HITS_PER_PAGE = 20;
                int totalShown = 0;

                res.resetIterator();
                ZimbraHit hit = res.getNext();
                while (hit != null) {
                    for (int i = 0; (hit != null) && (i < HITS_PER_PAGE); i++) {
                        displayHit(hit, groupBy);
                        totalShown++;
                        hit = res.getNext();
                    }
                    if (hit != null) {
                        outputStream.print("more (y/n) ? ");
                        String line = inputReader.readLine();
                        if (line.length() == 0 || line.charAt(0) == 'n')
                            break;
                    }
                }
                outputStream.println("Query ran in " + (endTime-startTime) + " ms");
                outputStream.println("Displayed a total of " + totalShown + " Hits");
            }
        }
    }

    public void displayHit(ZimbraHit hit, int groupBy) {
        outputStream.print("HIT: ");
        if (groupBy == SEARCH_RETURN_CONVERSATIONS) {
            ConversationHit ch = (ConversationHit) hit;
            outputStream.println(ch.toString() + " \"  (" + ch.getNumMessageHits() + ")");
            for (MessageHit mh : ch.getMessageHits()) {
                outputStream.println("\t" + mh.toString());
            }
        } else {
            if (hit instanceof MessageHit) {
                MessageHit mh = (MessageHit)hit;
                outputStream.println(mh.toString());
            } else if (hit instanceof MessagePartHit){
                MessagePartHit mph = (MessagePartHit)hit;
                outputStream.println(mph.toString());
            } else {
                outputStream.println(hit.toString());
            }
        }
    }

    public void dumpFields(int mailboxId) throws IOException, ServiceException {
    }

    public boolean confirm(String confirmString) {
        outputStream.println(confirmString);
        outputStream.print("Type YES to confirm: ");
        try {
            String s = inputReader.readLine();
            if (s.equals("YES")) {
                return true;
            }
        } catch(Exception e) {
            outputStream.print("Caught exception: "+ExceptionToString.ToString(e));
            // nothing
        }
        return false;
    }

    public static String Format(String s, int len) {
        StringBuffer toRet = new StringBuffer(len+1);
        int curOff = 0;
        if (s.length() < len) {
            for (curOff = 0; curOff < (len-s.length()); curOff++) {
                toRet.append(" ");
            }
        }

        int sOff = 0;
        for (; curOff < len; curOff++) {
            toRet.append(s.charAt(sOff));
            sOff++;
        }
        toRet.append("  ");
        return toRet.toString();
    }

    public void dumpDocument(Document d, boolean isDeleted) {
        if (isDeleted) {
            outputStream.print("DELETED ");
        }
        String subj, blobId;
        Field f;
        f = d.getField(LuceneFields.L_H_SUBJECT);
        if (f!=null) {
            subj = f.stringValue();
        } else {
            subj = "MISSING_SUBJECT";
        }
        f = d.getField(LuceneFields.L_MAILBOX_BLOB_ID);
        if (f!=null) {
            blobId = f.stringValue();
        } else {
            blobId = "MISSING";
        }
        String dateStr = d.get(LuceneFields.L_SORT_DATE);
        if (dateStr == null) {
            dateStr = "";
        } else {
            try {
                Date dt = DateTools.stringToDate(dateStr);
                dateStr = dt.toString() +" (" + dt.getTime() + ")";
            } catch (java.text.ParseException e) {
                assert false;
            }
        }
        String sizeStr = d.get(LuceneFields.L_SORT_SIZE);
        if (sizeStr == null) {
            sizeStr = "";
        }
        String part = d.get(LuceneFields.L_PARTNAME);
        if (part == null) {
            part = "NO_PART";
        }

        outputStream.println(Format(blobId, 10) + Format(dateStr, 45) +
                Format(part, 10) + Format(sizeStr, 10) + "\"" + subj + "\"");

        Field content = d.getField(LuceneFields.L_CONTENT);
        if (content != null) {
            outputStream.println("\t"+content.toString());
        }
    }

    public void dumpAll(int mailboxId) throws IOException, ServiceException {
    }

    public void dumpDocumentByMailItemId(int mailboxId, int mailItemId) throws ServiceException, IOException {
    }

    public void dumpTerms(int mailboxId) throws IOException, ServiceException {
    }

    public void getTerms(int mailboxId, String field, int minNum, int maxNum, Collection<?> ret)
    throws IOException, ServiceException {
    }

    public static class TwoTerms implements Comparable<TwoTerms> {
        @Override public int compareTo(TwoTerms other) {
            if (other.mCount == mCount) {
                if (other.s1.equals(s1)) {
                    return -(other.s2.compareTo(s2));
                }
                return -(other.s1.compareTo(s1));
            }
            return -(other.mCount-mCount);
        }
        public int mCount;
        public String s1;
        public String s2;
    }

    public void spanTest(int mailboxId) throws IOException, ServiceException {
    }

    static IndexEditorTcpServer sTcpServer = null;
    static IndexEditorProtocolhandler sIndexEditorProtocolHandler;
    static int sPortNo = 7035;
    static IndexEditorTcpThread tcpServerThread;
    static Thread sThread;

    public static void StartTcpEditor() throws ServiceException {
        ServerSocket serverSocket = NetUtil.getTcpServerSocket(null, sPortNo);
        sTcpServer = new IndexEditorTcpServer(3, serverSocket);
        sIndexEditorProtocolHandler = new IndexEditorProtocolhandler(sTcpServer);
        sTcpServer.addActiveHandler(sIndexEditorProtocolHandler);
        sThread = new Thread(new IndexEditorTcpThread(), "IndexEditor-TcpServer");
        sThread.start();
    }

    public static void EndTcpEditor() {
        for (Object cur : inputs) {
            try {
                if (cur instanceof InputStream) {
                    ((InputStream)cur).close();
                } else {
                    ((OutputStream)cur).close();
                }
            } catch (IOException e) {
                mLog.error("Caught "+ExceptionToString.ToString(e));
            }
        }

        if (sTcpServer != null) {
            try {
                sTcpServer.removeActiveHandler(sIndexEditorProtocolHandler);
            } finally {
                sTcpServer.stop(0);
                sTcpServer = null;
            }
        }
    }

    public static List<Object> inputs = new ArrayList<Object>();

    private static class IndexEditorTcpThread implements Runnable {
        @Override
        public void run() {
            sTcpServer.run();
        }
    }

    private static class IndexEditorTcpServer extends TcpServer {
        IndexEditorTcpServer(int numThreads, ServerSocket serverSocket) {
            super(numThreads, serverSocket);
        }

        @Override
        public String getName() {
            return "IndexEditorTcpServer";
        }

        @Override
        protected ProtocolHandler newProtocolHandler() {
            return new IndexEditorProtocolhandler(this);
        }

        @Override
        public int getConfigMaxIdleMilliSeconds() {
            return 0;
        }

    }


    private static class IndexEditorProtocolhandler extends ProtocolHandler {
        private InputStream mInputStream;
        private OutputStream mOutputStream;
//      private String mRemoteAddress;
        private IndexEditor mEditor = null;

        private String logLayoutPattern = "%d %-5p [%t] [%x] %c{1} - %m%n";


        public IndexEditorProtocolhandler(TcpServer server) {
            super(server);
        }

        /**
         * Performs any necessary setup steps upon connection from client.
         * @throws IOException
         */
        @Override
        protected boolean setupConnection(Socket connection) throws IOException {
//          mRemoteAddress = connection.getInetAddress().getHostAddress();

            mInputStream = new TcpServerInputStream(connection.getInputStream());
            mOutputStream = new BufferedOutputStream(connection.getOutputStream());

            inputs.add(mInputStream);
            inputs.add(mOutputStream);
            return true;
        }

        /**
         * Authenticates the client.
         * @return true if authenticated, false if not authenticated
         * @throws IOException
         */
        @Override
        protected boolean authenticate() throws IOException {
            return true;
        }

        private org.apache.logging.log4j.core.appender.WriterAppender mAppender;

        public boolean enableLogging() {
            if (mAppender == null) {
                /*
                 * Layout layout = new PatternLayout(logLayoutPattern ); 
                 * mAppender = new org.apache.logging.log4j.core.appender.WriterAppender(layout, mOutputStream);
                 * Logger root = Logger.getRootLogger(); 
                 * root.addAppender(mAppender);
                 */
                final LoggerContext context = LoggerContext.getContext(false);
                final Configuration config = context.getConfiguration();
                final PatternLayout layout = PatternLayout.newBuilder().withPattern(logLayoutPattern).build();
                final Appender appender = OutputStreamAppender.createAppender(layout, null, mOutputStream, mAppender.getName(), false, true);
                appender.start();
                config.addAppender(appender);
                return true;
            } else {
                return false;   
            }
        }

        public boolean disableLogging() {
            if (mAppender != null) {
                final LoggerContext context = LoggerContext.getContext(false);
                final Configuration config = context.getConfiguration();
                config.getRootLogger().removeAppender(mAppender.getName());
                return true;
            }
            return false;
        }

        /**
         * Reads and processes one command sent by client.
         * @return true if expecting more commands, false if QUIT command was
         *         received and server disconnected the connection
         * @throws Exception
         */
        @Override
        protected boolean processCommand() throws Exception {
            mAppender = null;
            try {
                mEditor = new IndexEditor(this);
                mEditor.run(mInputStream, mOutputStream);
            } finally {
                disableLogging();
            }
            return false;
        }

        /**
         * Closes any input/output streams with the client.  May get called
         * multiple times.
         */
        @Override
        protected void dropConnection() {
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch(IOException e) {
                    mLog.warn("While closing output stream, Caught "+ExceptionToString.ToString(e));
                }
                mInputStream = null;
            }
            if (mOutputStream != null) {
                try {
                    mOutputStream.close();
                } catch(IOException e) {
                    mLog.warn("While closing output stream, Caught "+ExceptionToString.ToString(e));
                }
                mOutputStream = null;
            }
        }

        /**
         * Called when a connection has been idle for too long.  Sends
         * protocol-specific message to client notifying it that the
         * connection is being dropped due to idle timeout.
         */
        @Override
        protected void notifyIdleConnection() {
        }

    }

    public void run(InputStream input, OutputStream output)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        PrintStream printer = new PrintStream(output, true);
        run(reader, printer);
    }

    public void run() {
        run(new BufferedReader(new InputStreamReader(System.in)), System.out);
    }

    public void run(BufferedReader _inputReader, PrintStream _outputStream) {
        inputReader = _inputReader;
        outputStream = _outputStream;

        String mailboxIdStr = null;
        int mailboxId = 0;
        boolean quit = false;

        while(!quit) {
            try {
                outputStream.print("> ");

                String command = null;
                try {
                    command = inputReader.readLine();
                } catch (IOException e) {
                    // an IOException here means the input stream is closed.  We should quit.
                    quit=true;
                    continue;
                }

                if (command==null || command.equals("exit") || command.equals("quit")) {
                    quit = true;
                } else if (command.equals("?")) {
                    help();
                } else if (command.equals("re-index")) {
                    reIndex(mailboxId);
                } else if (command.equals("re-index-all")) {
                    reIndexAll();
                } else if (command.equals("re-index-msg")) {
                    outputStream.print("MSGID> THIS_FUNCTION_CURRENTLY_UNIMPLEMENTED");
//                    String msg = inputReader.readLine();
//                    int msgId = Integer.parseInt(msg);
//                    reIndexMsg(mailboxId, msgId);
                } else if (command.equals("sort da")) {
                    sortOrder = SortBy.DATE_ASC;
                    outputStream.println("---->Search order = DATE_ASCENDING");
                } else if (command.equals("sort dd")) {
                    sortOrder = SortBy.DATE_DESC;
                    outputStream.println("---->Search order = DATE_DESCENDING");
                } else if (command.equals("sort sa")) {
                    sortOrder = SortBy.SUBJ_ASC;
                    outputStream.println("---->Search order = SUBJ_ASCENDING");
                } else if (command.equals("sort sd")) {
                    sortOrder = SortBy.SUBJ_DESC;
                    outputStream.println("---->Search order = SUBJ_DESCENDING");
                } else if (command.equals("sort na")) {
                    sortOrder = SortBy.NAME_ASC;
                    outputStream.println("---->Search order = NAME_ASCENDING");
                } else if (command.equals("sort nd")) {
                    sortOrder = SortBy.NAME_DESC;
                    outputStream.println("---->Search order = NAME_DESCENDING");
                } else if (command.equals("sort za")) {
                    sortOrder = SortBy.SIZE_ASC;
                    outputStream.println("---->Search order = SIZE_ASCENDING");
                } else if (command.equals("sort zd")) {
                    sortOrder = SortBy.SIZE_DESC;
                    outputStream.println("---->Search order = SIZE_DESCENDING");
                } else if (command.equals("q") || command.equals("query")) {
                    QueryRunner runner = new SingleQueryRunner(mailboxId);
                    doQuery(runner,false, SEARCH_RETURN_MESSAGES);
                } else if (command.equals("qd") || command.equals("querydump")) {
                    QueryRunner runner = new SingleQueryRunner(mailboxId);
                    doQuery(runner,true, SEARCH_RETURN_DOCUMENTS);
                } else if (command.equals("qc") || command.equals("queryconv")) {
                    QueryRunner runner = new SingleQueryRunner(mailboxId);
                    doQuery(runner,true, SEARCH_RETURN_CONVERSATIONS);
//                  } else if (command.equals("nq") || command.equals("newquery")) {
//                  newQuery(mailboxId);
                } else if (command.equals("qp") || command.equals("queryconv")) {
                    QueryRunner runner = new SingleQueryRunner(mailboxId);
                    doQuery(runner,true, SEARCH_RETURN_DOCUMENTS);
                } else if (command.equals("mbox")) {
                    outputStream.print("Enter New Mailbox ID: ");
                    mailboxIdStr = inputReader.readLine();
                    mailboxId = getMailboxIdFromString(mailboxIdStr);
                    outputStream.println("\tMailboxID set to "+mailboxId);
                } else if (command.equals("fields")) {
                    dumpFields(mailboxId);
                } else if (command.equals("terms")) {
                    dumpTerms(mailboxId);
                } else if (command.equals("s")) {
                    spanTest(mailboxId);
                } else if (command.equals("delete_index")) {
                    if (confirm("Are you sure you want to delete the index for mailbox "+ mailboxId+"?"))
                    {
                        deleteIndex(mailboxId);
                    }
                } else if (command.equals("dumpmi")) {
                    outputStream.print("Enter Mail-Item-ID:");
                    String midStr = inputReader.readLine();

                    if (!midStr.equals("")) {
                        int id = Integer.parseInt(midStr);
                        dumpDocumentByMailItemId(mailboxId, id);
                    }
                } else if (command.equals("dumpall")) {
                    dumpAll(mailboxId);
                } else if (command.equals("size")) {
                    getSize(mailboxId);
                } else if (command.equals("loglevel")) {
                    logLevel();
                } else if (command.equals("snoop")) {
                    if (mHandler == null) {
                        outputStream.println("Log Snooping only available in remote mode");
                    } else {
                        if (mHandler.enableLogging()) {
                            outputStream.println("Log Snooping ENABLED");
                        } else {
                            outputStream.println("Log Snooping already active.");
                        }
                    }
                } else if (command.equals("nosnoop")) {
                    if (mHandler == null) {
                        outputStream.println("Log Snooping only available in remote mode");
                    } else {
                        if (mHandler.disableLogging()) {
                            outputStream.println("Log Snooping DISABLED");
                        } else {
                            outputStream.println("Log Snooping not active.");
                        }
                    }
                }
            } catch(Exception e) {
                outputStream.println("Caught Exception "+ExceptionToString.ToString(e));
            }
        }

    }


    int getMailboxIdFromString(String str) throws ServiceException {
        if (str != null && !str.equals("")) {
            if (str.indexOf('@') >= 0) {
                // account
                Account acct = Provisioning.getInstance().get(AccountBy.name, str);
                Mailbox mbx = MailboxManager.getInstance().getMailboxByAccount(acct);
                return mbx.getId();

            } else {
                return Integer.parseInt(str);
            }
        }
        return 0;
    }

    public void logLevel()
    {
        String logLevel = null;
        try {
            outputStream.print("Enter logging level> ");
            logLevel = inputReader.readLine();
        } catch(Exception e) {
            outputStream.print("Caught exception: "+e.toString());
        }

        Logger root = LoggerContext.getContext().getRootLogger();

        if (logLevel != null && !logLevel.equals("")) {

            Level newLevel = null;

            if (logLevel.equalsIgnoreCase("ALL")) {
                newLevel = Level.ALL;
            } else if (logLevel.equalsIgnoreCase("DEBUG")) {
                newLevel = Level.DEBUG;
            } else if (logLevel.equalsIgnoreCase("ERROR")) {
                newLevel = Level.ERROR;
            } else if (logLevel.equalsIgnoreCase("FATAL")) {
                newLevel = Level.FATAL;
            } else if (logLevel.equalsIgnoreCase("INFO")) {
                newLevel = Level.INFO;
            } else if (logLevel.equalsIgnoreCase("OFF")) {
                newLevel = Level.OFF;
            } else if (logLevel.equalsIgnoreCase("WARN")) {
                newLevel = Level.WARN;
            }

            if (newLevel == null) {
                outputStream.println("Unknown level - must be ALL/DEBUG/ERROR/FATAL/INFO/OFF/WARN");
                return;
            }

            root.setLevel(newLevel);
        }
        Level cur = root.getLevel();
        outputStream.println("Current level is: "+cur);
    }

    private IndexEditorProtocolhandler mHandler;
    public IndexEditor(IndexEditorProtocolhandler handler) {
        mHandler = handler;
    }
    public IndexEditor() {
        mHandler = null;
    }

    public static void main(String[] args) {
        CliUtil.toolSetup("DEBUG");

        MailboxIndex.startup();
        IndexEditor editor = new IndexEditor();
        editor.run();
        MailboxIndex.shutdown();
    }

    void getSize(int mailboxId) throws ServiceException {
        Mailbox mbx = MailboxManager.getInstance().getMailboxById(mailboxId);
        long size = mbx.getSize();
        outputStream.println("Mailbox "+mailboxId+" has size "+size+" ("+(size/1024)+"kb)");
    }

    void help() {
        outputStream.println("\nHELP (updated)");
        outputStream.println("----");
        outputStream.println("exit-- exit this program");
        outputStream.println("re-index -- re-index mailbox from message store");
        outputStream.println("re-index-all -- re-index ALL MAILBOXES (!) in the message store");
        outputStream.println("re-index-msg -- re-index mailbox from message store");
        outputStream.println("sort na|nd|sa|sd|da|dd -- set sort order (name asc/desc, subj asc/desc or date asc/desc)");
        outputStream.println("query -- run a query group_by_message");
        outputStream.println("queryconv -- run a query group_by_conv");
//      outputStream.println("newquery -- run new query");
        outputStream.println("mbox -- change mailbox");
        outputStream.println("fields -- dump all known fields");
        outputStream.println("terms -- dump all known terms for a field");
//      outputStream.println("spans -- spantest");
        outputStream.println("delete_index -- deletes the index");
        outputStream.println("dumpmi -- dump document by mail_item");
        outputStream.println("dumpall -- dump all documents");
        outputStream.println("unit -- run unit tests");
//      outputStream.println("archive -- archive from recent idx to stable idx");
        outputStream.println("hack -- hacked test code to make a copy of index");
        outputStream.println("size -- Return the (uncompressed) size of this mailbox");
        outputStream.println("verify -- Verify that all messages in this mailbox are indexed");
        outputStream.println("loglevel -- Change the default global logging level (affects all appenders!)");
        outputStream.println("snoop -- copy log4j root logger to local output (snoop logs)");
        outputStream.println("nosnoop -- stop copying log4j logger");
    }
}
