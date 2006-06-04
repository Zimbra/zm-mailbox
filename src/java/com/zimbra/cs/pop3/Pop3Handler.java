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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 25, 2004
 */
package com.zimbra.cs.pop3;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.tcpserver.TcpServerInputStream;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author schemers
 */
public class Pop3Handler extends ProtocolHandler {

    static final int MIN_EXPIRE_DAYS = 31;
    static final int MAX_RESPONSE = 512;
    static final int MAX_RESPONSE_TEXT = MAX_RESPONSE - 7; // "-ERR" + " " + "\r\n"
    
    private static final byte[] LINE_SEPARATOR = { '\r', '\n'};
    private static final String TERMINATOR = ".";
    private static final int TERMINATOR_C = '.';    
    private static final byte[] TERMINATOR_BYTE = { '.' };
    
    // Connection specific data
    private Socket mConnection;
    private SSLSocket mTlsConnection;    
    private Pop3Server mServer;
    private TcpServerInputStream mInputStream;
    private OutputStream mOutputStream;    
    private String mRemoteAddress;
    private String mUser;
    private String mQuery;
    private String mAccountId;
    private String mAccountName;
    private Pop3Mailbox mMbx;
    
    private int mState;

    private boolean dropConnection;
    
    private static final int STATE_INIT = 0;
    private static final int STATE_AUTHORIZATION = 1;
    private static final int STATE_TRANSACTION = 2;    
    private static final int STATE_UPDATE = 3;

    // Message specific data
    private String mCurrentCommandLine;
    private int mExpire;

    Pop3Handler(Pop3Server server) {
        super(server);
        mServer = server;
    }
    /* (non-Javadoc)
     * @see com.zimbra.cs.tcpserver.ProtocolHandler#setupConnection(java.net.Socket)
     */
    protected boolean setupConnection(Socket connection) throws IOException {
        // TODO Auto-generated method stub
        mConnection = connection;
        mRemoteAddress = connection.getInetAddress().getHostAddress();
        INFO("connected");

        mInputStream = new TcpServerInputStream(connection.getInputStream());
        
        mOutputStream = new BufferedOutputStream(connection.getOutputStream());
        
        if (!Config.userServicesEnabled()) {
            dropConnection();
            return false;
        }

        sendOK(mServer.getBanner());

        mState = STATE_AUTHORIZATION;
        dropConnection = false;        
        return true;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.tcpserver.ProtocolHandler#authenticate()
     */
    protected boolean authenticate() {
        // we auth with the USER/PASS commands
        return true;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.tcpserver.ProtocolHandler#processCommand()
     */
    protected boolean processCommand() throws IOException {
        // TODO: catch IOException too?
        ZimbraLog.clearContext();
        long start = ZimbraPerf.STOPWATCH_POP.start();
        
        try {
            if (mAccountName != null) ZimbraLog.addAccountNameToContext(mAccountName);
            ZimbraLog.addIpToContext(mRemoteAddress);
            boolean result = processCommandInternal();
            ZimbraPerf.STOPWATCH_POP.stop(start);
            return result;
        } catch (Pop3CmdException e) {
            sendERR(e.getResponse());
            DEBUG(e.getMessage(), e);
            return dropConnection == false;
        } catch (ServiceException e) {
            sendERR(Pop3CmdException.getResponse(e.getMessage()));
            DEBUG(e.getMessage(), e);
            return dropConnection == false;
        } finally {
            if (dropConnection)
                dropConnection();
            ZimbraLog.clearContext();
        }
    }
    
    protected boolean processCommandInternal() throws Pop3CmdException, IOException, ServiceException {
        mCurrentCommandLine = mInputStream.readLine();
        //INFO("command("+mCurrentCommandLine+")");
        String cmd = mCurrentCommandLine;
        String arg = null;

        if (cmd == null) {
            dropConnection = true;
            INFO("disconnected without quit");
            //dropConnection();
            return false;
        }

        if (!Config.userServicesEnabled()) {
            dropConnection = true;
            sendERR("Temporarily unavailable");
            //dropConnection();
            return false;
        }

        setIdle(false);

        int space = cmd.indexOf(" ");
        if (space > 0) {
            arg = cmd.substring(space + 1); 
            cmd = cmd.substring(0, space);
        }
        
        if (ZimbraLog.pop.isDebugEnabled()) {
            String darg = "PASS".equals(cmd) ? "<BLOCKED>" : arg;
            DEBUG("command=" + cmd + " arg=" + darg);
        }
                
        if (cmd.length() < 1)
            throw new Pop3CmdException("invalid request. please specify a command");

        // check account status before executing command
        if (mAccountId != null)
            try {
                Account acct = Provisioning.getInstance().get(AccountBy.id, mAccountId);
                if (acct == null || !acct.getAccountStatus().equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
                    return false;
            } catch (ServiceException e) {
                return false;
            }

        int ch = cmd.charAt(0);
        
        // Breaking out of this switch causes a syntax error to be returned
        // So if you process a command then return immediately (even if the
        // command handler reported a syntax error or failed otherwise)
        
        switch (ch) {
        case 'c':
        case 'C':
            if ("CAPA".equalsIgnoreCase(cmd)) {
                doCAPA();
                return true;
            }
            break;
        case 'd':
        case 'D':
            if ("DELE".equalsIgnoreCase(cmd)) {
                doDELE(arg);
                return true;
            }
            break;            
        case 'l':
        case 'L':
            if ("LIST".equalsIgnoreCase(cmd)) {
                doLIST(arg);
                return true;
            }
            break;                                    
        case 'n':
        case 'N':
            if ("NOOP".equalsIgnoreCase(cmd)) {
                doNOOP();
                return true;
            }
            break;
        case 'p':
        case 'P':
            if ("PASS".equalsIgnoreCase(cmd)) {
                doPASS(arg);
                return true;
            }
            break;
        case 'q':
        case 'Q':
            if ("QUIT".equalsIgnoreCase(cmd)) {
                doQUIT();
                return false;
            }
            break;
        case 'r':
        case 'R':
            if ("RETR".equalsIgnoreCase(cmd)) {
                doRETR(arg);
                return true;
            } else if ("RSET".equalsIgnoreCase(cmd)) {
                doRSET();
                return true;
            }
            break;            
        case 's':
        case 'S':
            if ("STAT".equalsIgnoreCase(cmd)) {
                doSTAT();
                return true;
            } else if ("STLS".equalsIgnoreCase(cmd)) {
                doSTLS();
                return true;
            }
            break;            
        case 't':
        case 'T':
            if ("TOP".equalsIgnoreCase(cmd)) {
                doTOP(arg);
                return true;
            }
            break;                        
        case 'u':
        case 'U':
            if ("UIDL".equalsIgnoreCase(cmd)) {
                doUIDL(arg);
                return true;
            } else if ("USER".equalsIgnoreCase(cmd)) {
                doUSER(arg);
                return true;            
            }
            break;
            
        default:
            break;
        }
        throw new Pop3CmdException("unknown command");        
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.tcpserver.ProtocolHandler#dropConnection()
     */
    protected void dropConnection() {
        try {
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            if (mOutputStream != null) {
                mOutputStream.close();
                mOutputStream = null;
            }
        } catch (IOException e) {
            INFO("exception while closing connection", e);
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.tcpserver.ProtocolHandler#notifyIdleConnection()
     */
    protected void notifyIdleConnection() {
        // according to RFC 1939 we aren't supposed to snd a response on idle timeout
        DEBUG("idle connection");

    }

    private void INFO(String message, Throwable e) {
        if (ZimbraLog.pop.isInfoEnabled()) ZimbraLog.pop.info(message, e); 
    }
    
    private void INFO(String message) {
        if (ZimbraLog.pop.isInfoEnabled()) ZimbraLog.pop.info(message);
    }

    private void DEBUG(String message, Throwable e) {
        if (ZimbraLog.pop.isDebugEnabled()) ZimbraLog.pop.debug(message, e);
    }

    private void DEBUG(String message) {
        if (ZimbraLog.pop.isDebugEnabled()) ZimbraLog.pop.debug(message);
    }

    private void WARN(String message, Throwable e) {
        if (ZimbraLog.pop.isWarnEnabled()) ZimbraLog.pop.warn(message, e);
    }

    private void WARN(String message) {
        if (ZimbraLog.pop.isWarnEnabled()) ZimbraLog.pop.warn(message);
    }

    private void sendERR(String response) throws IOException {
        sendResponse("-ERR", response, true);
    }

    private void sendOK(String response) throws IOException {
        sendResponse("+OK", response, true);        
    }
    
    private void sendOK(String response, boolean flush) throws IOException {
        sendResponse("+OK", response, flush);        
    }

    private void sendResponse(String status, String msg, boolean flush) throws IOException {
        String cl = mCurrentCommandLine != null ? mCurrentCommandLine : "<none>";
        String response = (msg == null || msg.length() == 0) ? status : status+" "+msg;
        if (ZimbraLog.pop.isDebugEnabled()) {
            DEBUG(response + " (" + cl + ")");
        } else {
            // only log errors if not debugging...
            if (status.charAt(0) == '-')
                INFO(response + " (" + cl + ")");
        }        
        sendLine(response, flush);
    }

    private void sendLine(String line) throws IOException {
        sendLine(line, true);
    }
    
    private void sendLine(String line, boolean flush) throws IOException {
        mOutputStream.write(line.getBytes());
        mOutputStream.write(LINE_SEPARATOR);
        if (flush)
            mOutputStream.flush();
    }

    private void flush() throws IOException {
        mOutputStream.flush();
    }

    /*
     * state:
     *   in a line
     *   hit end of a line
     */
    private void sendMessage(InputStream is, int maxNumBodyLines) throws IOException {
        
        boolean inBody = false;
        int numBodyLines = 0;
        
        PushbackInputStream stream = new PushbackInputStream(is);
        int c;
        
        boolean startOfLine = true;
        int lineLength = 0;

        while ((c = stream.read()) != -1) {
            if (c == '\r' || c == '\n') {
                if (c == '\r') {
                    int peek = stream.read();
                    if (peek != '\n' && peek != -1) stream.unread(peek);
                }
    
                if (!inBody) {
                    if (lineLength == 0)
                        inBody = true;
                } else {
                    numBodyLines++;
                }                         
                if (inBody && numBodyLines >= maxNumBodyLines)
                    break;
                startOfLine = true;
                lineLength = 0;
                mOutputStream.write(LINE_SEPARATOR);
                continue;
            } else if (c == TERMINATOR_C && startOfLine) {
                mOutputStream.write(c); // we'll end up writing it twice
            }
            if (startOfLine) startOfLine = false;
            lineLength++;
            mOutputStream.write(c);
        }     
        if (lineLength != 0) mOutputStream.write(LINE_SEPARATOR);
        mOutputStream.write(TERMINATOR_BYTE);
        mOutputStream.write(LINE_SEPARATOR);
        mOutputStream.flush();
    }

    private void doQUIT() throws IOException, ServiceException, Pop3CmdException {
        dropConnection = true;
        if (mMbx != null && mMbx.getNumDeletedMessages() > 0) {
            mState = STATE_UPDATE;
            // TODO: hard/soft could be a user/cos pref
            int count = mMbx.deleteMarked(true);
            sendOK("deleted "+count+" message(s)");
        } else {
            sendOK(mServer.getGoodbye());
        }
        INFO("quit from client");
        //dropConnection();
    }
    
    private void doNOOP() throws IOException {
        sendOK("yawn");
    }
    
    private void doRSET() throws Pop3CmdException, IOException {
        if (mState != STATE_TRANSACTION) 
            throw new Pop3CmdException("this command is only valid after a login");
        int numUndeleted = mMbx.undeleteMarked();
        sendOK(numUndeleted+ " message(s) undeleted");
    }    
    
    private void doUSER(String user) throws Pop3CmdException, IOException {
        if ((mTlsConnection == null) && !mServer.allowCleartextLogins())
            throw new Pop3CmdException("only valid after entering TLS mode");
        
        if (mState != STATE_AUTHORIZATION)
            throw new Pop3CmdException("this command is only valid in authorization state");
        
        if (user == null)
            throw new Pop3CmdException("please specify a user");

        if (user.length() > 1024)
            throw new Pop3CmdException("username length too long");

        if (user.endsWith("}")) {
            int p = user.indexOf('{');
            if (p != -1) {
                mUser = user.substring(0, p);
                mQuery = user.substring(p+1, user.length()-1);
                //mLog.info("mUser("+mUser+") mQuery("+mQuery+")");
            } else {
                mUser = user;
            }
        } else {
            mUser = user;
        }

        sendOK("hello "+mUser+", please enter your password");
    }
    
    private void doPASS(String password) throws Pop3CmdException, IOException, ServiceException {
        if ((mTlsConnection == null) && !mServer.allowCleartextLogins())
            throw new Pop3CmdException("only valid after entering TLS mode");        
        
        if (mState != STATE_AUTHORIZATION)
            throw new Pop3CmdException("this command is only valid in authorization state");
        
        if (mUser == null)
            throw new Pop3CmdException("please specify username first with the USER command");

        if (password == null)
            throw new Pop3CmdException("please specify a password");

        if (password.length() > 1024)
            throw new Pop3CmdException("password length too long");
        
        try {
            Provisioning prov = Provisioning.getInstance();            
            Account acct = prov.get(AccountBy.name, mUser);
            if (acct == null)
                throw new Pop3CmdException("invalid username/password");
            if (!acct.getBooleanAttr(Provisioning.A_zimbraPop3Enabled, false))
                throw new Pop3CmdException("pop access not enabled for account");                
            prov.authAccount(acct, password);
            mAccountId = acct.getId();
            mAccountName = acct.getName();
            Mailbox mailbox = Mailbox.getMailboxByAccountId(mAccountId);
            mMbx = new Pop3Mailbox(mailbox, acct, mQuery);
            mState = STATE_TRANSACTION;
            mExpire = (int) (acct.getTimeInterval(Provisioning.A_zimbraMailMessageLifetime, 0) / (1000*60*60*24));
            if (mExpire > 0 && mExpire < MIN_EXPIRE_DAYS) mExpire = MIN_EXPIRE_DAYS;
            sendOK("server ready");
        } catch (AccountServiceException e) {
            // need to catch and mask these two
            if (e.getCode().equals(AccountServiceException.NO_SUCH_ACCOUNT) ||
                e.getCode().equals(AccountServiceException.AUTH_FAILED)) {
                throw new Pop3CmdException("invalid username/password");
            } else if (e.getCode().equals(AccountServiceException.CHANGE_PASSWORD)) {
                throw new Pop3CmdException("your password has expired");
            } else if (e.getCode().equals(AccountServiceException.MAINTENANCE_MODE)) {
                throw new Pop3CmdException("your account is having maintenance peformed. please try again later");
            } else {
                throw new Pop3CmdException(e.getMessage());
            }
        }
    }
    
    private void doSTAT() throws Pop3CmdException, IOException {
        if (mState != STATE_TRANSACTION) 
            throw new Pop3CmdException("this command is only valid after a login");
        sendOK(mMbx.getNumMessages()+" "+mMbx.getSize());
    }
    
    private void doSTLS() throws Pop3CmdException, IOException {
        if (mServer.isConnectionSSL())
            throw new Pop3CmdException("command not valid over TLS");

        if (mState != STATE_AUTHORIZATION)
            throw new Pop3CmdException("this command is only valid prior to login");
        if (mTlsConnection != null)
            throw new Pop3CmdException("command not valid while in TLS mode");
        SSLSocketFactory fac = (SSLSocketFactory) SSLSocketFactory.getDefault();
        sendOK("Begin TLS negotiation");        
        mTlsConnection = (SSLSocket) fac.createSocket(mConnection, mConnection.getInetAddress().getHostName(), mConnection.getPort(), true);
        mTlsConnection.setUseClientMode(false);
        mTlsConnection.startHandshake();
        ZimbraLog.pop.debug("suite: "+mTlsConnection.getSession().getCipherSuite());
        mInputStream = new TcpServerInputStream(mTlsConnection.getInputStream());
        mOutputStream = new BufferedOutputStream(mTlsConnection.getOutputStream());        
    }
    
    private void doLIST(String msg) throws Pop3CmdException, IOException {
        if (mState != STATE_TRANSACTION) 
            throw new Pop3CmdException("this command is only valid after a login");
        if (msg != null) {
            Pop3Message pm = mMbx.getPop3Msg(msg);
            sendOK(msg+" "+pm.getSize());
        } else {
            sendOK(mMbx.getNumMessages()+" messages", false);
            int totNumMsgs = mMbx.getTotalNumMessages();
            for (int n=0; n < totNumMsgs; n++) {
                Pop3Message pm = mMbx.getMsg(n);                
                if (!pm.isDeleted())
                    sendLine((n+1)+" "+pm.getSize(), false);
            }
            sendLine(TERMINATOR);
        }
    }
    
    private void doUIDL(String msg) throws Pop3CmdException, IOException {
        if (mState != STATE_TRANSACTION) 
            throw new Pop3CmdException("this command is only valid after a login");
        if (msg != null) {
            Pop3Message pm = mMbx.getPop3Msg(msg);
            sendOK(msg+" "+pm.getId()+"."+pm.getDigest());
        } else {
            sendOK(mMbx.getNumMessages()+" messages", false);
            int totNumMsgs = mMbx.getTotalNumMessages();
            for (int n=0; n < totNumMsgs; n++) {
                Pop3Message pm = mMbx.getMsg(n);                
                if (!pm.isDeleted())
                    sendLine((n+1)+" "+pm.getId()+"."+pm.getDigest(), false);
            }
            sendLine(TERMINATOR);
        }
    }            

    private int parseInt(String s, String message) throws Pop3CmdException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new Pop3CmdException(message);
        }
    }

    private void doRETR(String msg) throws Pop3CmdException, IOException, ServiceException {
        if (mState != STATE_TRANSACTION) 
            throw new Pop3CmdException("this command is only valid after a login");

        if (msg == null)
            throw new Pop3CmdException("please specify a message");

        Message m = mMbx.getMessage(msg);
        InputStream is = null;        
        try {
            is = m.getRawMessage();
            sendOK("message follows", false);
            sendMessage(is, Integer.MAX_VALUE);
        } finally {
            if (is != null)
                is.close();
        }
    }

    private void doTOP(String arg) throws Pop3CmdException, IOException, ServiceException {
        if (mState != STATE_TRANSACTION) 
            throw new Pop3CmdException("this command is only valid after a login");
        
        int space = arg == null ? -1 : arg.indexOf(" ");
        if (space == -1)
            throw new Pop3CmdException("please specify a message and number of lines");
        
        String msg = arg.substring(0, space);
        int n = parseInt(arg.substring(space + 1), "unable to parse number of lines");
        
        if (n < 0) 
            throw new Pop3CmdException("please specify a non-negative value for number of lines");

        if (msg == null || msg.equals(""))
            throw new Pop3CmdException("please specify a message");

        Message m = mMbx.getMessage(msg);
        InputStream is = null;        
        try {
            is = m.getRawMessage();
            sendOK("message top follows", false);
            sendMessage(is, n);
        } finally {
            if (is != null)
                is.close();
        }        
    }

    private void doDELE(String msg) throws Pop3CmdException, IOException {
        if (mState != STATE_TRANSACTION) 
            throw new Pop3CmdException("this command is only valid after a login");

        if (msg == null)
            throw new Pop3CmdException("please specify a message");

        Pop3Message pm = mMbx.getPop3Msg(msg);
        mMbx.delete(pm);
        sendOK("message "+msg+" marked for deletion");
    }    

    private void doCAPA() throws IOException {
        sendOK("Capability list follows", false);
        sendLine("TOP", false);
        sendLine("USER", false);
        sendLine("UIDL", false);
        if (!mServer.isConnectionSSL())
            sendLine("STLS", false);        
        if (mState != STATE_TRANSACTION) {
            sendLine("EXPIRE "+MIN_EXPIRE_DAYS+" USER", false);
        } else {
            if (mExpire == 0)
                sendLine("EXPIRE NEVER", false);                
            else
                sendLine("EXPIRE "+mExpire, false);
        }
        // TODO: VERSION INFO
        sendLine("IMPLEMENTATION ZimbraInc", false);
        sendLine(TERMINATOR);
    }
}
