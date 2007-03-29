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
package com.zimbra.cs.imap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.imap.ImapMessage.ImapMessageSet;
import com.zimbra.cs.imap.ImapCredentials.EnabledHack;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.mail.ItemActionHelper;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.stats.StatsFile;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

/**
 * @author dkarp
 */
public abstract class ImapHandler extends ProtocolHandler {

    abstract class Authenticator {
        String mTag;
        boolean mContinue = true;
        Authenticator(String tag)  { mTag = tag; }
        abstract boolean handle(byte[] data) throws IOException;
    }
    private class AuthPlain extends Authenticator {
        AuthPlain(String tag)  { super(tag); }
        boolean handle(byte[] response) throws IOException {
            // RFC 2595 6: "Non-US-ASCII characters are permitted as long as they are
            //              represented in UTF-8 [UTF-8]."
            String message = new String(response, "utf-8");

            // RFC 2595 6: "The client sends the authorization identity (identity to
            //              login as), followed by a US-ASCII NUL character, followed by the
            //              authentication identity (identity whose password will be used),
            //              followed by a US-ASCII NUL character, followed by the clear-text
            //              password.  The client may leave the authorization identity empty to
            //              indicate that it is the same as the authentication identity."
            int nul1 = message.indexOf('\0'), nul2 = message.indexOf('\0', nul1 + 1);
            if (nul1 == -1 || nul2 == -1) {
                sendNO(mTag, "malformed authentication message");
                return true;
            }
            String authorizeId = message.substring(0, nul1);
            String authenticateId = message.substring(nul1 + 1, nul2);
            String password = message.substring(nul2 + 1);
            if (authorizeId.equals(""))
                authorizeId = authenticateId;

            mContinue = login(authorizeId, authenticateId, password, "AUTHENTICATE", mTag);
            return true;
        }
    }

    enum State { NOT_AUTHENTICATED, AUTHENTICATED, SELECTED, LOGOUT }

    private static final long MAXIMUM_IDLE_PROCESSING_MILLIS = 15 * Constants.MILLIS_PER_SECOND;

    static final char[] LINE_SEPARATOR       = { '\r', '\n' };
    static final byte[] LINE_SEPARATOR_BYTES = { '\r', '\n' };

    private DateFormat mTimeFormat   = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z", Locale.US);
    private DateFormat mDateFormat   = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
    private DateFormat mZimbraFormat = DateFormat.getDateInstance(DateFormat.SHORT);

    protected Authenticator   mAuthenticator;
    protected ImapCredentials mCredentials;
    protected boolean         mStartedTLS;
    protected String          mLastCommand;
    protected ImapFolder      mSelectedFolder;
    private   String          mIdleTag;
    protected boolean         mGoodbyeSent;

    ImapHandler() {
        super(null);
    }

    ImapHandler(ImapServer server) {
        super(server);
    }

    abstract void dumpState(Writer w);
    abstract void encodeState(Element parent);

    abstract Object getServer();

    DateFormat getTimeFormat()   { return mTimeFormat; }
    DateFormat getDateFormat()   { return mDateFormat; }
    DateFormat getZimbraFormat() { return mZimbraFormat; }

    ImapCredentials getCredentials()  { return mCredentials; }

    static final boolean STOP_PROCESSING = false, CONTINUE_PROCESSING = true;
    
    static StatsFile STATS_FILE = new StatsFile("perf_imap", new String[] { "command" }, true);

    void checkEOF(String tag, ImapRequest req) throws ImapParseException {
        if (!req.eof())
            throw new ImapParseException(tag, "excess characters at end of command");
    }

    boolean continueAuthentication(ImapRequest req) throws IOException {
        String tag = mAuthenticator.mTag;
        boolean authFinished = true;

        try {
            // use the tag from the original AUTHENTICATE command
            req.setTag(tag);

            // 6.2.2: "If the client wishes to cancel an authentication exchange, it issues a line
            //         consisting of a single "*".  If the server receives such a response, it MUST
            //         reject the AUTHENTICATE command by sending a tagged BAD response."
            if (req.peekChar() == '*') {
                req.skipChar('*');
                if (req.eof())
                    sendBAD(tag, "AUTHENTICATE aborted");
                else
                    sendBAD(tag, "AUTHENTICATE failed; invalid base64 input");
                return CONTINUE_PROCESSING;
            }

            byte[] response = req.readBase64(false);
            checkEOF(tag, req);
            authFinished = mAuthenticator.handle(response);
            return mAuthenticator.mContinue;
        } catch (ImapParseException ipe) {
            sendBAD(tag, ipe.getMessage());
            return CONTINUE_PROCESSING;
        } finally {
            if (authFinished)
                mAuthenticator = null;
        }
    }

    boolean isIdle() {
        return mIdleTag != null;
    }

    boolean executeRequest(ImapRequest req) throws IOException, ImapParseException {
        if (isIdle())
            return doIDLE(null, IDLE_STOP, req.readATOM().equals("DONE") && req.eof());

        String tag = req.readTag();

        boolean byUID = false;
        req.skipSpace();
        String command = mLastCommand = req.readATOM();
        do {
            switch (command.charAt(0)) {
                case 'A':
                    if (command.equals("AUTHENTICATE")) {
                        req.skipSpace();  String mechanism = req.readATOM();
                        byte[] response = null;
                        if (req.peekChar() == ' ' && extensionEnabled("SASL-IR")) {
                            req.skipSpace();  response = req.readBase64(true);
                        }
                        checkEOF(tag, req);
                        return doAUTHENTICATE(tag, mechanism, response);
                    } else if (command.equals("APPEND")) {
                        List<String> flags = null;  Date date = null;
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        req.skipSpace();
                        if (req.peekChar() == '(') {
                            flags = req.readFlags();  req.skipSpace();
                        }
                        if (req.peekChar() == '"') {
                            date = req.readDate(mTimeFormat, true);  req.skipSpace();
                        }
                        if ((req.peekChar() == 'c' || req.peekChar() == 'C') && extensionEnabled("CATENATE")) {
                            List<Object> parts = new LinkedList<Object>();
                            req.skipAtom("CATENATE");  req.skipSpace();  req.skipChar('(');
                            while (req.peekChar() != ')') {
                                if (!parts.isEmpty())
                                    req.skipSpace();
                                String type = req.readATOM();  req.skipSpace();
                                if (type.equals("TEXT"))      parts.add(req.readLiteral());
                                else if (type.equals("URL"))  parts.add(new ImapURL(tag, this, req.readAstring()));
                                else throw new ImapParseException(tag, "unknown CATENATE cat-part: " + type);
                            }
                            req.skipChar(')');  checkEOF(tag, req);
                            return doCATENATE(tag, path, flags, date, parts);
                        } else {
                            byte[] content = req.readLiteral8();
                            checkEOF(tag, req);
                            return doAPPEND(tag, path, flags, date, content);
                        }
                    }
                    break;
                case 'C':
                    if (command.equals("CAPABILITY")) {
                        checkEOF(tag, req);
                        return doCAPABILITY(tag);
                    } else if (command.equals("COPY")) {
                        req.skipSpace();  String sequence = req.readSequence();
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        checkEOF(tag, req);
                        return doCOPY(tag, sequence, path, byUID);
                    } else if (command.equals("CLOSE")) {
                        checkEOF(tag, req);
                        return doCLOSE(tag);
                    } else if (command.equals("CREATE")) {
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        checkEOF(tag, req);
                        return doCREATE(tag, path);
                    } else if (command.equals("CHECK")) {
                        checkEOF(tag, req);
                        return doCHECK(tag);
                    }
                    break;
                case 'D':
                    if (command.equals("DELETE")) {
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        checkEOF(tag, req);
                        return doDELETE(tag, path);
                    }
                    break;
                case 'E':
                    if (command.equals("EXPUNGE")) {
                        String sequence = null;
                        if (byUID) {
                            req.skipSpace();  sequence = req.readSequence();
                        }
                        checkEOF(tag, req);
                        return doEXPUNGE(tag, byUID, sequence);
                    } else if (command.equals("EXAMINE")) {
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        checkEOF(tag, req);
                        return doEXAMINE(tag, path);
                    }
                    break;
                case 'F':
                    if (command.equals("FETCH")) {
                        List<ImapPartSpecifier> parts = new ArrayList<ImapPartSpecifier>();
                        req.skipSpace();  String sequence = req.readSequence();
                        req.skipSpace();  int attributes = req.readFetch(parts);
                        checkEOF(tag, req);
                        return doFETCH(tag, sequence, attributes, parts, byUID);
                    }
                    break;
                case 'G':
                    if (command.equals("GETQUOTA") && extensionEnabled("QUOTA")) {
                        req.skipSpace();  ImapPath qroot = new ImapPath(req.readAstring(), mCredentials);
                        checkEOF(tag, req);
                        return doGETQUOTA(tag, qroot);
                    } else if (command.equals("GETQUOTAROOT") && extensionEnabled("QUOTA")) {
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        checkEOF(tag, req);
                        return doGETQUOTAROOT(tag, path);
                    }
                    break;
                case 'I':
                    if (command.equals("ID") && extensionEnabled("ID")) {
                        req.skipSpace();  Map<String, String> params = req.readParameters(true);
                        checkEOF(tag, req);
                        return doID(tag, params);
                    } else if (command.equals("IDLE") && extensionEnabled("IDLE")) {
                        checkEOF(tag, req);
                        return doIDLE(tag, IDLE_START, true);
                    }
                    break;
                case 'L':
                    if (command.equals("LOGIN")) {
                        req.skipSpace();  String user = req.readAstring();
                        req.skipSpace();  String pass = req.readAstring();
                        checkEOF(tag, req);
                        return doLOGIN(tag, user, pass);
                    } else if (command.equals("LOGOUT")) {
                        checkEOF(tag, req);
                        return doLOGOUT(tag);
                    } else if (command.equals("LIST")) {
                        req.skipSpace();  String base = req.readEscapedFolder();
                        req.skipSpace();  String pattern = req.readFolderPattern();
                        checkEOF(tag, req);
                        return doLIST(tag, base, pattern);
                    } else if (command.equals("LSUB")) {
                        req.skipSpace();  String base = req.readEscapedFolder();
                        req.skipSpace();  String pattern = req.readFolderPattern();
                        checkEOF(tag, req);
                        return doLSUB(tag, base, pattern);
                    }
                    break;
                case 'N':
                    if (command.equals("NOOP")) {
                        checkEOF(tag, req);
                        return doNOOP(tag);
                    } else if (command.equals("NAMESPACE") && extensionEnabled("NAMESPACE")) {
                        checkEOF(tag, req);
                        return doNAMESPACE(tag);
                    }
                    break;
                case 'R':
                    if (command.equals("RENAME")) {
                        req.skipSpace();  ImapPath folder = new ImapPath(req.readFolder(), mCredentials);
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        checkEOF(tag, req);
                        return doRENAME(tag, folder, path);
                    }
                    break;
                case 'S':
                    if (command.equals("STORE")) {
                        StoreAction operation = StoreAction.REPLACE;  boolean silent = false;
                        req.skipSpace();  String sequence = req.readSequence();
                        req.skipSpace();
                        switch (req.peekChar()) {
                            case '+':  req.skipChar('+');  operation = StoreAction.ADD;     break;
                            case '-':  req.skipChar('-');  operation = StoreAction.REMOVE;  break;
                        }
                        String cmd = req.readATOM();
                        if (cmd.equals("FLAGS.SILENT"))  silent = true;
                        else if (!cmd.equals("FLAGS"))   throw new ImapParseException(tag, "invalid store-att-flags");
                        req.skipSpace();  List<String> flags = req.readFlags();
                        checkEOF(tag, req);
                        return doSTORE(tag, sequence, flags, operation, silent, byUID);
                    } else if (command.equals("SEARCH")) {
                        Integer options = null;
                        req.skipSpace();
                        if ("RETURN".equals(req.peekATOM()) && extensionEnabled("ESEARCH")) {
                            options = 0;
                            req.skipAtom("RETURN");  req.skipSpace();  req.skipChar('(');
                            while (req.peekChar() != ')') {
                                if (options != 0)
                                    req.skipSpace();
                                String option = req.readATOM();
                                if (option.equals("MIN"))         options |= RETURN_MIN;
                                else if (option.equals("MAX"))    options |= RETURN_MAX;
                                else if (option.equals("ALL"))    options |= RETURN_ALL;
                                else if (option.equals("COUNT"))  options |= RETURN_COUNT;
                                else if (option.equals("SAVE") && extensionEnabled("X-DRAFT-I05-SEARCHRES"))  options |= RETURN_SAVE;
                                else
                                    throw new ImapParseException(tag, "unknown RETURN option \"" + option + '"');
                            }
                            req.skipChar(')');  req.skipSpace();
                            if (options == 0)
                                options = RETURN_ALL;
                        }
                        ImapSearch i4search = req.readSearch();
                        checkEOF(tag, req);
                        return doSEARCH(tag, i4search, byUID, options);
                    } else if (command.equals("SELECT")) {
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        checkEOF(tag, req);
                        return doSELECT(tag, path);
                    } else if (command.equals("STARTTLS") && extensionEnabled("STARTTLS")) {
                        checkEOF(tag, req);
                        return doSTARTTLS(tag);
                    } else if (command.equals("STATUS")) {
                        int status = 0;
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        req.skipSpace();  req.skipChar('(');
                        while (req.peekChar() != ')') {
                            if (status != 0)
                                req.skipSpace();
                            String flag = req.readATOM();
                            if (flag.equals("MESSAGES"))          status |= STATUS_MESSAGES;
                            else if (flag.equals("RECENT"))       status |= STATUS_RECENT;
                            else if (flag.equals("UIDNEXT"))      status |= STATUS_UIDNEXT;
                            else if (flag.equals("UIDVALIDITY"))  status |= STATUS_UIDVALIDITY;
                            else if (flag.equals("UNSEEN"))       status |= STATUS_UNSEEN;
                            else
                                throw new ImapParseException(tag, "unknown STATUS attribute \"" + flag + '"');
                        }
                        req.skipChar(')');
                        checkEOF(tag, req);
                        return doSTATUS(tag, path, status);
                    } else if (command.equals("SUBSCRIBE")) {
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        checkEOF(tag, req);
                        return doSUBSCRIBE(tag, path);
                    } else if (command.equals("SETQUOTA") && extensionEnabled("QUOTA")) {
                        Map<String, String> limits = new HashMap<String, String>();
                        req.skipSpace();  String qroot = req.readAstring();
                        req.skipSpace();  req.skipChar('(');
                        while (req.peekChar() != ')') {
                            if (!limits.isEmpty())
                                req.skipSpace();
                            String resource = req.readATOM();  req.skipSpace();
                            limits.put(resource, req.readNumber());
                        }
                        req.skipChar(')');
                        checkEOF(tag, req);
                        return doSETQUOTA(tag, qroot, limits);
                    }
                    break;
                case 'U':
                    if (command.equals("UID")) {
                        req.skipSpace();  command = req.readATOM();
                        if (command.equals("FETCH") || command.equals("SEARCH") || command.equals("COPY") || command.equals("STORE") || (command.equals("EXPUNGE") && extensionEnabled("UIDPLUS"))) {
                            byUID = true;
                            continue;
                        }
                        throw new ImapParseException(tag, "command not permitted with UID");
                    } else if (command.equals("UNSUBSCRIBE")) {
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        checkEOF(tag, req);
                        return doUNSUBSCRIBE(tag, path);
                    } else if (command.equals("UNSELECT") && extensionEnabled("UNSELECT")) {
                        checkEOF(tag, req);
                        return doUNSELECT(tag);
                    }
                    break;
            }
        } while (byUID);

        throw new ImapParseException(tag, "command not implemented");
    }

    State getState() {
        if (mGoodbyeSent)
            return State.LOGOUT;
        else if (mSelectedFolder != null)
            return State.SELECTED;
        else if (mCredentials != null)
            return State.AUTHENTICATED;
        else
            return State.NOT_AUTHENTICATED;
    }

    boolean checkState(String tag, State required) throws IOException {
        State state = getState();
        if (required == State.NOT_AUTHENTICATED && state != State.NOT_AUTHENTICATED) {
            sendNO(tag, "must be in NOT AUTHENTICATED state");
            return false;
        } else if (required == State.AUTHENTICATED && (state == State.NOT_AUTHENTICATED || state == State.LOGOUT)) {
            sendNO(tag, "must be in AUTHENTICATED or SELECTED state");
            return false;
        } else if (required == State.SELECTED && state != State.SELECTED) {
            sendNO(tag, "must be in SELECTED state");
            return false;
        } else {
            return true;
        }
    }

    ImapFolder getSelectedFolder() {
        return getState() == State.LOGOUT ? null : mSelectedFolder;
    }

    void unsetSelectedFolder() {
        if (mSelectedFolder != null)
            mSelectedFolder.unregister();
        mSelectedFolder = null;
    }

    void setSelectedFolder(ImapFolder i4folder) throws ServiceException {
        if (i4folder == mSelectedFolder)
            return;

        unsetSelectedFolder();
        if (i4folder == null)
            return;

        try {
            i4folder.register();
        } catch (ServiceException e) {
            i4folder.unregister();
            throw e;
        }
        mSelectedFolder = i4folder;
    }

    private boolean canContinue(ServiceException e) {
        return e.getCode().equals(MailServiceException.MAINTENANCE) ? STOP_PROCESSING : CONTINUE_PROCESSING;
    }

    private OperationContext getContext() throws ServiceException {
        if (mCredentials == null)
            throw ServiceException.AUTH_REQUIRED();
        return mCredentials.getContext();
    }


    boolean doCAPABILITY(String tag) throws IOException {
        sendCapability();
        sendOK(tag, "CAPABILITY completed");
        return CONTINUE_PROCESSING;
    }

    private static final String[] SUPPORTED_EXTENSIONS = new String[] {
        "BINARY", "CATENATE", "CHILDREN", "ESEARCH", "ID", "IDLE",
        "LITERAL+", "LOGIN-REFERRALS", "NAMESPACE", "QUOTA", "SASL-IR",
        "UIDPLUS", "UNSELECT", "WITHIN", "X-DRAFT-I05-SEARCHRES"
    };

    private void sendCapability() throws IOException {
        // [IMAP4rev1]        RFC 3501: Internet Message Access Protocol - Version 4rev1
        // [LOGINDISABLED]    RFC 3501: Internet Message Access Protocol - Version 4rev1
        // [STARTTLS]         RFC 3501: Internet Message Access Protocol - Version 4rev1
        // [AUTH=PLAIN]       RFC 2595: Using TLS with IMAP, POP3 and ACAP
        // [BINARY]           RFC 3516: IMAP4 Binary Content Extension
        // [CATENATE]         RFC 4469: Internet Message Access Protocol (IMAP) CATENATE Extension
        // [CHILDREN]         RFC 3348: IMAP4 Child Mailbox Extension
        // [ESEARCH]          RFC 4731: IMAP4 Extension to SEARCH Command for Controlling What Kind of Information Is Returned
        // [ID]               RFC 2971: IMAP4 ID Extension
        // [IDLE]             RFC 2177: IMAP4 IDLE command
        // [LITERAL+]         RFC 2088: IMAP4 non-synchronizing literals
        // [LOGIN-REFERRALS]  RFC 2221: IMAP4 Login Referrals
        // [NAMESPACE]        RFC 2342: IMAP4 Namespace
        // [QUOTA]            RFC 2087: IMAP4 QUOTA extension
        // [SASL-IR]          draft-siemborski-imap-sasl-initial-response-06: IMAP Extension for SASL Initial Client Response
        // [UIDPLUS]          RFC 4315: Internet Message Access Protocol (IMAP) - UIDPLUS extension
        // [UNSELECT]         RFC 3691: IMAP UNSELECT command
        // [WITHIN]           draft-ietf-lemonade-search-within-03: WITHIN Search extension to the IMAP Protocol
        // [X-DRAFT-I05-SEARCHRES]  draft-melnikov-imap-search-res-05: IMAP extension for referencing the last SEARCH result

        boolean authenticated = mCredentials != null;
        String nologin  = mStartedTLS || authenticated || ImapServer.allowCleartextLogins() ? "" : " LOGINDISABLED";
        String starttls = mStartedTLS || authenticated || !extensionEnabled("STARTTLS")     ? "" : " STARTTLS";
        String plain    = !mStartedTLS || authenticated || !extensionEnabled("AUTH=PLAIN")  ? "" : " AUTH=PLAIN";

        StringBuilder capability = new StringBuilder("CAPABILITY IMAP4rev1" + nologin + starttls + plain);
        for (String extension : SUPPORTED_EXTENSIONS)
            if (extensionEnabled(extension))
                capability.append(' ').append(extension);

        sendUntagged(capability.toString());
    }

    boolean extensionEnabled(String extension) {
        // check whether the extension is explicitly disabled on the server
        if (ImapServer.isExtensionDisabled(getServer(), extension))
            return false;
        // check whether one of the extension's prerequisites is disabled on the server
        if (extension.equalsIgnoreCase("X-DRAFT-I05-SEARCHRES"))
            return extensionEnabled("ESEARCH");
        // see if the user's session has disabled the extension
        if (extension.equalsIgnoreCase("IDLE") && mCredentials != null && mCredentials.isHackEnabled(EnabledHack.NO_IDLE))
            return false;
        // everything else is enabled
        return true;
    }

    boolean doNOOP(String tag) throws IOException {
        sendNotifications(true, false);
        sendOK(tag, "NOOP completed");
        return CONTINUE_PROCESSING;
    }

    // RFC 2971 3: "The sole purpose of the ID extension is to enable clients and servers
    //              to exchange information on their implementations for the purposes of
    //              statistical analysis and problem determination."
    boolean doID(String tag, Map<String, String> attrs) throws IOException {
        if (attrs != null)
            ZimbraLog.imap.info("IMAP client identified as: " + attrs);

        sendUntagged("ID (\"NAME\" \"Zimbra\" \"VERSION\" \"" + BuildInfo.VERSION + "\" \"RELEASE\" \"" + BuildInfo.RELEASE + "\")");
        sendOK(tag, "ID completed");
        return CONTINUE_PROCESSING;
    }

    abstract boolean doSTARTTLS(String tag) throws IOException;

    boolean doLOGOUT(String tag) throws IOException {
        sendUntagged(ImapServer.getGoodbye());
        mGoodbyeSent = true;
        sendOK(tag, "LOGOUT completed");
        return STOP_PROCESSING;
    }

    boolean doAUTHENTICATE(String tag, String mechanism, byte[] response) throws IOException {
        if (!checkState(tag, State.NOT_AUTHENTICATED))
            return CONTINUE_PROCESSING;

        boolean finished = false;

        if (mechanism.equals("PLAIN") && extensionEnabled("AUTH=PLAIN")) {
            // RFC 2595 6: "The PLAIN SASL mechanism MUST NOT be advertised or used
            //              unless a strong encryption layer (such as the provided by TLS)
            //              is active or backwards compatibility dictates otherwise."
            if (!mStartedTLS) {
                sendNO(tag, "cleartext logins disabled");
                return CONTINUE_PROCESSING;
            }
            mAuthenticator = new AuthPlain(tag);
        } else {
            // no other AUTHENTICATE mechanisms are supported yet
            sendNO(tag, "mechanism not supported");
            return CONTINUE_PROCESSING;
        }

        // draft-siemborski-imap-sasl-initial-response:
        //      "This extension adds an optional second argument to the AUTHENTICATE
        //       command that is defined in Section 6.2.2 of [IMAP4].  If this second
        //       argument is present, it represents the contents of the "initial
        //       client response" defined in section 5.1 of [SASL]."
        if (response != null)
            finished = mAuthenticator.handle(response);

        if (finished)
            mAuthenticator = null;
        else
            sendContinuation();
        return CONTINUE_PROCESSING;
    }

    boolean doLOGIN(String tag, String username, String password) throws IOException {
        if (!checkState(tag, State.NOT_AUTHENTICATED))
            return CONTINUE_PROCESSING;
        else if (!mStartedTLS && !ImapServer.allowCleartextLogins()) {
            sendNO(tag, "cleartext logins disabled");
            return CONTINUE_PROCESSING;
        }

        return login(username, "", password, "LOGIN", tag);
    }

    boolean login(String username, String authenticateId, String password, String command, String tag) throws IOException {
        // the Windows Mobile 5 hacks are enabled by appending "/wm" to the username, etc.
        EnabledHack enabledHack = EnabledHack.NONE;
        for (EnabledHack hack : EnabledHack.values()) {
            if (hack.toString() != null && username.endsWith(hack.toString())) {
                enabledHack = hack;
                username = username.substring(0, username.length() - hack.toString().length());
                break;
            }
        }

        try {
            Account account = authenticate(authenticateId, username, password, command, tag);
            ImapCredentials session = startSession(account, enabledHack, command, tag);
            if (session == null)
                return CONTINUE_PROCESSING;
            disableUnauthConnectionAlarm();
        } catch (ServiceException e) {
            ZimbraLog.imap.warn(command + " failed", e);
            if (e.getCode().equals(AccountServiceException.CHANGE_PASSWORD))
                sendNO(tag, "[ALERT] password must be changed before IMAP login permitted");
            else if (e.getCode().equals(AccountServiceException.MAINTENANCE_MODE))
                sendNO(tag, "[ALERT] account undergoing maintenance; please try again later");
            else
                sendNO(tag, command + " failed");
            return canContinue(e);
        }

        sendCapability();
        sendOK(tag, command + " completed");
        return CONTINUE_PROCESSING;
    }

    abstract void disableUnauthConnectionAlarm() throws ServiceException;

    private Account authenticate(String authenticateId, String username, String password, String command, String tag) throws ServiceException, IOException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, username);
        Account authacct = authenticateId.equals("") ? account : prov.get(AccountBy.name, authenticateId);
        if (account == null || authacct == null) {
            sendNO(tag, command + " failed");
            return null;
        }
        // authenticate the authentication principal
        prov.authAccount(authacct, password, "imap");
        // authorize as the target user
        if (!account.getId().equals(authacct.getId())) {
            // check domain/global admin if auth credentials != target account
            if (!AccessManager.getInstance().canAccessAccount(authacct, account)) {
                sendNO(tag, command + " failed");
                return null;
            }
        }
        return account;
    }

    private ImapCredentials startSession(Account account, EnabledHack hack, String command, String tag) throws ServiceException, IOException {
        if (account == null)
            return null;

        // make sure we can actually login via IMAP on this host
        if (!account.getBooleanAttr(Provisioning.A_zimbraImapEnabled, false)) {
            sendNO(tag, "account does not have IMAP access enabled");
            return null;
        } else if (!Provisioning.onLocalServer(account)) { 
            String correctHost = account.getAttr(Provisioning.A_zimbraMailHost);
            ZimbraLog.imap.info(command + " failed; should be on host " + correctHost);
            if (correctHost == null || correctHost.trim().equals("") || !extensionEnabled("LOGIN_REFERRALS"))
                sendNO(tag, command + " failed [wrong host]");
            else
                sendNO(tag, "[REFERRAL imap://" + URLEncoder.encode(account.getName(), "utf-8") + '@' + correctHost + "/] " + command + " failed");
            return null;
        }

        mCredentials = new ImapCredentials(account, hack);
        return mCredentials;
    }

    boolean doSELECT(String tag, ImapPath path) throws IOException {
        return selectFolder(tag, "SELECT", path);
    }

    boolean doEXAMINE(String tag, ImapPath path) throws IOException {
        return selectFolder(tag, "EXAMINE", path);
    }

    private boolean selectFolder(String tag, String command, ImapPath path) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        ImapFolder i4oldFolder = mSelectedFolder;
        boolean writable = command.equals("SELECT");
        List<String> permflags = Collections.emptyList();
        try {
            Object mboxobj = path.getOwnerMailbox();
            if (!(mboxobj instanceof Mailbox)) {
                // 6.3.1: "The SELECT command automatically deselects any currently selected mailbox 
                //         before attempting the new selection.  Consequently, if a mailbox is selected
                //         and a SELECT command that fails is attempted, no mailbox is selected."
                unsetSelectedFolder();

                ZimbraLog.imap.info("cannot select folder on other server: " + path);
                sendNO(tag, command + " failed: cannot open mailbox on other server");
                return CONTINUE_PROCESSING;
            }

            ImapFolder i4folder = null;
            if (i4oldFolder != null && !i4oldFolder.isVirtual() && path.equals(i4oldFolder.getPath())) {
                try {
                    i4oldFolder.reopen(writable);
                    i4folder = i4oldFolder;
                } catch (ServiceException e) {
                    ZimbraLog.imap.warn("error quick-reopening folder " + path + "; proceeding with manual reopen", e);
                }
            }

            if (i4folder == null)
                i4folder = new ImapFolder(path, writable, this, mCredentials);

        	writable = i4folder.isWritable();
        	setSelectedFolder(i4folder);

            if (writable) {
                // RFC 4314 5.1.1: "Any server implementing an ACL extension MUST accurately reflect the
                //                  current user's rights in FLAGS and PERMANENTFLAGS responses."
                permflags = i4folder.getFlagList(true);
                if (!path.isWritable(ACL.RIGHT_DELETE))
                    permflags.remove("\\Deleted");
                if (path.belongsTo(mCredentials.getAccountId()))
                    permflags.add("\\*");
            }
        } catch (ServiceException e) {
            // 6.3.1: "The SELECT command automatically deselects any currently selected mailbox 
            //         before attempting the new selection.  Consequently, if a mailbox is selected
            //         and a SELECT command that fails is attempted, no mailbox is selected."
            unsetSelectedFolder();

            if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER))
                ZimbraLog.imap.info(command + " failed: no such folder: " + path);
            else
                ZimbraLog.imap.warn(command + " failed", e);
            sendNO(tag, command + " failed");
            return canContinue(e);
        }

        // note: not sending back a "* OK [UIDNEXT ....]" response for search folders
        //    6.3.1: "If this is missing, the client can not make any assumptions about the
        //            next unique identifier value."
        // FIXME: hardcoding "* 0 RECENT"
        sendUntagged(mSelectedFolder.getSize() + " EXISTS");
        sendUntagged(0 + " RECENT");
        if (mSelectedFolder.getFirstUnread() > 0)
        	sendUntagged("OK [UNSEEN " + mSelectedFolder.getFirstUnread() + ']');
        sendUntagged("OK [UIDVALIDITY " + mSelectedFolder.getUIDValidity() + ']');
        if (!mSelectedFolder.isVirtual())
            sendUntagged("OK [UIDNEXT " + mSelectedFolder.getInitialUIDNEXT() + ']');
        sendUntagged("FLAGS (" + StringUtil.join(" ", mSelectedFolder.getFlagList(false)) + ')');
        sendUntagged("OK [PERMANENTFLAGS (" + StringUtil.join(" ", permflags) + ")]");
        sendOK(tag, (writable ? "[READ-WRITE] " : "[READ-ONLY] ") + command + " completed");
        return CONTINUE_PROCESSING;
    }

    boolean doCREATE(String tag, ImapPath path) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        if (!path.isCreatable()) {
            ZimbraLog.imap.info("CREATE failed: hidden folder or parent: " + path);
            sendNO(tag, "CREATE failed");
            return CONTINUE_PROCESSING;
        }

        try {
            Object mboxobj = path.getOwnerMailbox();
            if (mboxobj instanceof Mailbox) {
                ((Mailbox) mboxobj).createFolder(getContext(), path.asZimbraPath(), (byte) 0, MailItem.TYPE_MESSAGE);
            } else if (mboxobj instanceof ZMailbox) {
                ((ZMailbox) mboxobj).createFolder(null, path.asZimbraPath(), ZFolder.View.message, ZFolder.Color.defaultColor, null, null);
            } else {
                throw AccountServiceException.NO_SUCH_ACCOUNT(path.getOwner());
            }
        } catch (ServiceException e) {
            String cause = "CREATE failed";
            if (e.getCode().equals(MailServiceException.CANNOT_CONTAIN))
                cause += ": superior mailbox has \\Noinferiors set";
            else if (e.getCode().equals(MailServiceException.ALREADY_EXISTS))
                cause += ": mailbox already exists";
            else if (e.getCode().equals(MailServiceException.INVALID_NAME))
                cause += ": invalid mailbox name";
            else if (e.getCode().equals(ServiceException.PERM_DENIED))
                cause += ": permission denied";
            ZimbraLog.imap.warn(cause, e);
            sendNO(tag, cause);
            return canContinue(e);
        }

        sendNotifications(true, false);
        sendOK(tag, "CREATE completed");
        return CONTINUE_PROCESSING;
    }

    boolean doDELETE(String tag, ImapPath path) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        try {
            Object mboxobj = path.getOwnerMailbox();
            if (mboxobj instanceof Mailbox) {
                ImapDeleteOperation op = new ImapDeleteOperation(mSelectedFolder, getContext(), (Mailbox) mboxobj, path);
                op.schedule();
            } else if (mboxobj instanceof ZMailbox) {
                ZMailbox zmbx = (ZMailbox) mboxobj;
                ZFolder zfolder = zmbx.getFolderByPath(path.asZimbraPath());
                if (zfolder == null)
                    throw MailServiceException.NO_SUCH_FOLDER(path.asImapPath());

                if (zfolder.getSubFolders().isEmpty())
                    zmbx.deleteFolder(zfolder.getId());
                else
                    zmbx.emptyFolder(zfolder.getId(), false);
            } else {
                throw AccountServiceException.NO_SUCH_ACCOUNT(path.getOwner());
            }

            if (mSelectedFolder != null && path.equals(mSelectedFolder.getPath()))
                unsetSelectedFolder();
        } catch (ServiceException e) {
            if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER))
                ZimbraLog.imap.info("DELETE failed: no such folder: " + path);
            else
                ZimbraLog.imap.warn("DELETE failed", e);
            sendNO(tag, "DELETE failed");
            return canContinue(e);
        }

        sendNotifications(true, false);
        sendOK(tag, "DELETE completed");
        return CONTINUE_PROCESSING;
    }

    boolean doRENAME(String tag, ImapPath oldPath, ImapPath newPath) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        try {
            Account source = oldPath.getOwnerAccount(), target = newPath.getOwnerAccount();
            if (source == null || target == null) {
                ZimbraLog.imap.info("RENAME failed: no such account for " + oldPath + " or " + newPath);
                sendNO(tag, "RENAME failed: no such account");
                return CONTINUE_PROCESSING;
            } else if (!source.getId().equalsIgnoreCase(target.getId())) {
                ZimbraLog.imap.info("RENAME failed: cannot move folder between mailboxes");
                sendNO(tag, "RENAME failed: cannot rename mailbox to other user's namespace");
                return CONTINUE_PROCESSING;
            }

            Object mboxobj = oldPath.getOwnerMailbox();
            if (mboxobj instanceof Mailbox) {
                Mailbox mbox = (Mailbox) mboxobj;
                int folderId = mbox.getFolderByPath(getContext(), oldPath.asZimbraPath()).getId();
                if (folderId == Mailbox.ID_FOLDER_INBOX)
                    throw ImapServiceException.CANT_RENAME_INBOX();
                mbox.rename(getContext(), folderId, MailItem.TYPE_FOLDER, newPath.asZimbraPath());
            } else if (mboxobj instanceof ZMailbox) {
                ZMailbox zmbx = (ZMailbox) mboxobj;
                ZFolder zfolder = zmbx.getFolderByPath(oldPath.asZimbraPath());
                if (zfolder == null)
                    throw MailServiceException.NO_SUCH_FOLDER(oldPath.asZimbraPath());
                else if (new ItemId(zfolder.getId(), (String) null).getId() == Mailbox.ID_FOLDER_INBOX)
                    throw ImapServiceException.CANT_RENAME_INBOX();
                zmbx.renameFolder(zfolder.getId(), newPath.asZimbraPath());
            } else {
                ZimbraLog.imap.info("RENAME failed: cannot get mailbox for path: " + oldPath);
                sendNO(tag, "RENAME failed");
                return CONTINUE_PROCESSING;
            }
        } catch (ServiceException e) {
        	if (e instanceof ImapServiceException && e.getCode().equals(ImapServiceException.CANT_RENAME_INBOX)) {
        		ZimbraLog.imap.info("RENAME failed: RENAME of INBOX not supported");
        		sendNO(tag, "RENAME failed: RENAME of INBOX not supported");
        		return CONTINUE_PROCESSING;
        	} else if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER)) {
        		ZimbraLog.imap.info("RENAME failed: no such folder: " + oldPath);
            } else {
        		ZimbraLog.imap.warn("RENAME failed", e);
            }
        	sendNO(tag, "RENAME failed");
        	return canContinue(e);
        }

        // note: if ImapFolder contains a pathname, we may need to update mSelectedFolder
        sendNotifications(true, false);
        sendOK(tag, "RENAME completed");
        return CONTINUE_PROCESSING;
    }

    boolean doSUBSCRIBE(String tag, ImapPath path) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        try {
            if (path.belongsTo(mCredentials.getAccountId())) {
                Mailbox mbox = mCredentials.getMailbox();
                Folder folder = mbox.getFolderByPath(getContext(), path.asZimbraPath());
                if (!path.isVisible())
                    throw ImapServiceException.FOLDER_NOT_VISIBLE(path.asImapPath());
                if (!folder.isTagged(mbox.mSubscribeFlag))
                    mbox.alterTag(getContext(), folder.getId(), MailItem.TYPE_FOLDER, Flag.ID_FLAG_SUBSCRIBED, true);
            } else {
                mCredentials.subscribe(path);
            }
        } catch (ImapServiceException e) {
            if (e.getCode().equals(ImapServiceException.FOLDER_NOT_VISIBLE)) {
                ZimbraLog.imap.info("SUBSCRIBE failed: "+e.toString());
                sendNO(tag, "SUBSCRIBE failed");
                return CONTINUE_PROCESSING;
            } else 
                ZimbraLog.imap.warn("SUBSCRIBE failed", e);
        } catch (ServiceException e) {
            if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER))
                ZimbraLog.imap.info("SUBSCRIBE failed: no such folder: " + path);
            else
                ZimbraLog.imap.warn("SUBSCRIBE failed", e);
            sendNO(tag, "SUBSCRIBE failed");
            return canContinue(e);
        }
        
        sendNotifications(true, false);
        sendOK(tag, "SUBSCRIBE completed");
        return CONTINUE_PROCESSING;
    }

    boolean doUNSUBSCRIBE(String tag, ImapPath path) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        try {
            if (path.belongsTo(mCredentials.getAccountId())) {
                try {
                    Mailbox mbox = mCredentials.getMailbox();
                    Folder folder = mbox.getFolderByPath(getContext(), path.asZimbraPath());
                    if (folder.isTagged(mbox.mSubscribeFlag))
                        mbox.alterTag(getContext(), folder.getId(), MailItem.TYPE_FOLDER, Flag.ID_FLAG_SUBSCRIBED, false);
                } catch (NoSuchItemException nsie) { }
            } else {
                mCredentials.unsubscribe(path);
            }
        } catch (MailServiceException.NoSuchItemException nsie) {
            ZimbraLog.imap.info("UNSUBSCRIBE failure skipped: no such folder: " + path);
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("UNSUBSCRIBE failed", e);
            sendNO(tag, "UNSUBSCRIBE failed");
            return canContinue(e);
        }

        sendNotifications(true, false);
        sendOK(tag, "UNSUBSCRIBE completed");
        return CONTINUE_PROCESSING;
    }

    boolean doLIST(String tag, String referenceName, String mailboxName) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        if (mailboxName.equals("")) {
            // 6.3.8: "An empty ("" string) mailbox name argument is a special request to return
            //         the hierarchy delimiter and the root name of the name given in the reference."
            sendUntagged("LIST (\\Noselect) \"/\" \"\"");
            sendOK(tag, "LIST completed");
            return CONTINUE_PROCESSING;
        }

        List<String> matches = null;
        try {
            String pattern = mailboxName;
            if (!mailboxName.startsWith("/")) {
                if (referenceName.endsWith("/"))  pattern = referenceName + mailboxName;
                else                              pattern = referenceName + '/' + mailboxName;
            }
            ImapPath patternPath = new ImapPath(pattern, mCredentials);
            pattern = patternPath.asImapPath().toUpperCase();

            if (patternPath.getOwner() != null && patternPath.getOwner().indexOf('*') != -1) {
                // RFC 2342 5: "Alternatively, a server MAY return NO to such a LIST command,
                //              requiring that a user name be included with the Other Users'
                //              Namespace prefix before listing any other user's mailboxes."
                ZimbraLog.imap.info("LIST failed: wildcards not permitted in username " + pattern);
                sendNO(tag, "LIST failed: wildcards not permitted in username");
                return CONTINUE_PROCESSING;
            }

            // make sure we can do a  LIST "" "/home/user1"
            if (patternPath.getOwner() != null && (ImapPath.NAMESPACE_PREFIX + patternPath.getOwner()).toUpperCase().equals(pattern)) {
                matches = new ArrayList<String>();
                matches.add("LIST (\\Noselect) \"/\" " + patternPath.asUtf7String());
            } else {
                Object mboxobj = patternPath.getOwnerMailbox();
                if (mboxobj instanceof Mailbox) {
                	ImapListOperation op = new ImapListOperation(mSelectedFolder, getContext(), (Mailbox) mboxobj, patternPath, mCredentials, extensionEnabled("CHILDREN"));
                	op.schedule();
                	matches = op.getMatches();
                } else if (mboxobj instanceof ZMailbox) {
                    matches = new ArrayList<String>();
                    ZMailbox zmbx = (ZMailbox) mboxobj;
                    for (ZFolder zfolder : zmbx.getAllFolders()) {
                        ImapPath zpath = new ImapPath(patternPath.getOwner(), zmbx, zfolder, mCredentials);
                        if (zpath.asImapPath().toUpperCase().matches(pattern) && zpath.isVisible())
                            matches.add("LIST () \"/\" " + zpath.asUtf7String());
                    }
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("LIST failed", e);
            sendNO(tag, "LIST failed");
            return canContinue(e);
        }

        if (matches != null) {
        	for (String match : matches)
        		sendUntagged(match);
        }

        sendNotifications(true, false);
        sendOK(tag, "LIST completed");
        return CONTINUE_PROCESSING;
    }

    boolean doLSUB(String tag, String referenceName, String mailboxName) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        String pattern = mailboxName;
        if (!mailboxName.startsWith("/")) {
            if (referenceName.endsWith("/"))  pattern = referenceName + mailboxName;
            else                              pattern = referenceName + '/' + mailboxName;
        }
        ImapPath patternPath = new ImapPath(pattern, mCredentials);

        try {
        	ImapLSubOperation op = new ImapLSubOperation(mSelectedFolder, getContext(), mCredentials.getMailbox(), patternPath, mCredentials, extensionEnabled("CHILDREN"));
        	op.schedule();
        	List<String> subscriptions = op.getSubs();

            if (subscriptions != null) {
            	for (String sub : subscriptions)
            		sendUntagged(sub);
            }
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("LSUB failed", e);
            sendNO(tag, "LSUB failed");
            return canContinue(e);
        }

        sendNotifications(true, false);
        sendOK(tag, "LSUB completed");
        return CONTINUE_PROCESSING;
    }

    static final int STATUS_MESSAGES    = 0x01;
    static final int STATUS_RECENT      = 0x02;
    static final int STATUS_UIDNEXT     = 0x04;
    static final int STATUS_UIDVALIDITY = 0x08;
    static final int STATUS_UNSEEN      = 0x10;

    boolean doSTATUS(String tag, ImapPath path, int status) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        StringBuilder data = new StringBuilder();
        try {
            if (!path.isVisible()) {
                ZimbraLog.imap.info("STATUS failed: folder not visible: " + path);
                sendNO(tag, "STATUS failed");
                return CONTINUE_PROCESSING;
            }

            int messages, recent, uidnext, uvv, unread;
            Object mboxobj = path.getOwnerMailbox();
            if (mboxobj instanceof Mailbox) {
                Folder folder = ((Mailbox) mboxobj).getFolderByPath(getContext(), path.asZimbraPath());

                messages = folder.getSize();
                recent = 0;
                uidnext = folder instanceof SearchFolder ? -1 : folder.getImapUIDNEXT();
                uvv = ImapFolder.getUIDValidity(folder);
                unread = folder.getUnreadCount();
            } else if (mboxobj instanceof ZMailbox) {
                ZFolder zfolder = ((ZMailbox) mboxobj).getFolderByPath(path.asZimbraPath());
                if (zfolder == null)
                    throw MailServiceException.NO_SUCH_FOLDER(path.asImapPath());

                messages = zfolder.getMessageCount();
                recent = 0;
                uidnext = -1;
                uvv = ImapFolder.getUIDValidity(zfolder);
                unread = zfolder.getUnreadCount();
            } else {
                throw AccountServiceException.NO_SUCH_ACCOUNT(path.getOwner());
            }

            if (messages >= 0 && (status & STATUS_MESSAGES) != 0)
                data.append(data.length() > 0 ? " " : "").append("MESSAGES ").append(messages);
            // FIXME: hardcoded "RECENT 0"
            if (recent >= 0 && (status & STATUS_RECENT) != 0)
                data.append(data.length() > 0 ? " " : "").append("RECENT ").append(recent);
            // note: we're not supporting UIDNEXT for search folders; see the comments in selectFolder()
            if (uidnext > 0 && (status & STATUS_UIDNEXT) != 0)
                data.append(data.length() > 0 ? " " : "").append("UIDNEXT ").append(uidnext);
            if (uvv > 0 && (status & STATUS_UIDVALIDITY) != 0)
                data.append(data.length() > 0 ? " " : "").append("UIDVALIDITY ").append(uvv);
            if (unread >= 0 && (status & STATUS_UNSEEN) != 0)
                data.append(data.length() > 0 ? " " : "").append("UNSEEN ").append(unread);
        } catch (ServiceException e) {
            if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER))
                ZimbraLog.imap.info("STATUS failed: no such folder: " + path);
            else
                ZimbraLog.imap.warn("STATUS failed", e);
            sendNO(tag, "STATUS failed");
            return canContinue(e);
        }

        sendUntagged("STATUS " + path.asUtf7String() + " (" + data + ')');
        sendNotifications(true, false);
        sendOK(tag, "STATUS completed");
        return CONTINUE_PROCESSING;
    }

    boolean doCATENATE(String tag, ImapPath path, List<String> flagNames, Date date, List<Object> parts) throws IOException, ImapParseException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int size = 0;
        for (Object part : parts) {
            byte[] buffer;
            if (part instanceof byte[])
                buffer = (byte[]) part;
            else
                buffer = ((ImapURL) part).getContent(this, mCredentials, tag);

            size += buffer.length;
            if (size > ImapRequest.getMaxRequestLength())
                throw new ImapParseException(tag, "TOOBIG", "request too long", false);
            baos.write(buffer);
        }
        return doAPPEND(tag, path, flagNames, date, baos.toByteArray());
    }

    boolean doAPPEND(String tag, ImapPath path, List<String> flagNames, Date date, byte[] content) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        // if we're using Thunderbird, try to set INTERNALDATE to the message's Date: header
        if (date == null && mCredentials.isHackEnabled(EnabledHack.THUNDERBIRD)) {
            try {
                // inefficient, but must be done before creating the ParsedMessage
                date = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(content)).getSentDate();
            } catch (MessagingException e) { }
        }

        // server uses UNIX time, so range-check specified date (is there a better place for this?)
        if (date != null && date.getTime() > Integer.MAX_VALUE * 1000L) {
            ZimbraLog.imap.info("APPEND failed: date out of range");
            sendNO(tag, "APPEND failed: date out of range");
            return CONTINUE_PROCESSING;
        }

        ArrayList<Tag> newTags = new ArrayList<Tag>();
        StringBuilder appendHint = extensionEnabled("UIDPLUS") ? new StringBuilder() : null;
        try {
            if (!path.isVisible())
                throw ImapServiceException.FOLDER_NOT_VISIBLE(path.asImapPath());
            else if (!path.isWritable(ACL.RIGHT_INSERT))
                throw ImapServiceException.FOLDER_NOT_WRITABLE(path.asImapPath());

            Object mboxobj = path.getOwnerMailbox();
            if (mboxobj instanceof Mailbox) {
            	ImapAppendOperation op = new ImapAppendOperation(mSelectedFolder, getContext(), (Mailbox) mboxobj,
            				this, path, flagNames, date, content, newTags, appendHint);
            	op.schedule();
            } else if (mboxobj instanceof ZMailbox) {
                ZMailbox zmbx = (ZMailbox) mboxobj;
                ZFolder zfolder = zmbx.getFolderByPath(path.asZimbraPath());
                int uvv = ImapFolder.getUIDValidity(zfolder);

                int flagMask = Flag.BITMASK_UNREAD;
                for (ImapFlag i4flag : getSystemFlags(flagNames)) {
                    if (!i4flag.mPermanent)     continue;
                    else if (i4flag.mPositive)  flagMask |= i4flag.mBitmask;
                    else                        flagMask &= ~i4flag.mBitmask;
                }

                // FIXME: set APPENDUID if possible
                String createdId = zmbx.addMessage(zfolder.getId(), Flag.bitmaskToFlags(flagMask), null, date.getTime(), content, true);
                if (appendHint != null && uvv > 0 && createdId != null)
                    appendHint.append("[APPENDUID ").append(uvv).append(' ').append(new ItemId(createdId, mCredentials.getAccountId()).getId()).append("] ");
            } else {
                throw AccountServiceException.NO_SUCH_ACCOUNT(path.getOwner());
            }
        } catch (ServiceException e) {
            deleteTags(newTags);
            String msg = "APPEND failed";
            if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER)) {
                ZimbraLog.imap.info("APPEND failed: no such folder: " + path);
                // 6.3.11: "Unless it is certain that the destination mailbox can not be created,
                //          the server MUST send the response code "[TRYCREATE]" as the prefix
                //          of the text of the tagged NO response."
                if (path.isCreatable())
                    msg = "[TRYCREATE] APPEND failed: no such mailbox";
            } else {
                ZimbraLog.imap.warn("APPEND failed", e);
            }
            sendNO(tag, msg);
            return canContinue(e);
        }

        sendNotifications(true, false);
        sendOK(tag, (appendHint == null ? "" : appendHint.toString()) + "APPEND completed");
        return CONTINUE_PROCESSING;
    }

    List<ImapFlag> getSystemFlags(List<String> flagNames) throws ServiceException {
        if (flagNames == null || flagNames.isEmpty())
            return Collections.emptyList();

        Mailbox mbox = mSelectedFolder != null ? mSelectedFolder.getMailbox() : mCredentials.getMailbox();
        ImapFlagCache i4cache = ImapFlagCache.getSystemFlags(mbox);
        List<ImapFlag> i4flags = new ArrayList<ImapFlag>();
        for (String name : flagNames) {
            ImapFlag i4flag = i4cache.getByName(name);
            if (i4flag != null)
                i4flags.add(i4flag);
        }
        return i4flags;
    }

    List<ImapFlag> findOrCreateTags(Mailbox mbox, List<String> tagNames, List<Tag> newTags) throws ServiceException {
        if (tagNames == null || tagNames.size() == 0)
            return Collections.emptyList();
        ArrayList<ImapFlag> flags = new ArrayList<ImapFlag>();
        for (String name : tagNames) {
            ImapFlag i4flag = mSelectedFolder.getFlagByName(name);
            if (i4flag == null) {
                if (name.startsWith("\\"))
                    throw MailServiceException.INVALID_NAME(name);
                try {
                    i4flag = mSelectedFolder.cacheTag(mbox.getTagByName(name));
                } catch (MailServiceException.NoSuchItemException nsie) {
                    if (newTags == null)
                        continue;
                    // notification will update mTags hash
                    Tag ltag = mbox.createTag(getContext(), name, MailItem.DEFAULT_COLOR);
                    newTags.add(ltag);
                    i4flag = mSelectedFolder.getFlagByName(name);
                }
            }
            flags.add(i4flag);
        }
        return flags;
    }

    private void deleteTags(List<Tag> ltags) {
        if (ltags == null || ltags.isEmpty())
            return;

        for (Tag ltag : ltags) {
            try {
                // notification will update mTags hash
                ltag.getMailbox().delete(getContext(), ltag, null);
            } catch (ServiceException e) {
                ZimbraLog.imap.warn("failed to delete tag: " + ltag.getName(), e);
            }
        }
    }

    static final boolean IDLE_START = true;
    static final boolean IDLE_STOP  = false;

    // RFC 2177 3: "The IDLE command is sent from the client to the server when the client is
    //              ready to accept unsolicited mailbox update messages.  The server requests
    //              a response to the IDLE command using the continuation ("+") response.  The
    //              IDLE command remains active until the client responds to the continuation,
    //              and as long as an IDLE command is active, the server is now free to send
    //              untagged EXISTS, EXPUNGE, and other messages at any time."
    boolean doIDLE(String tag, boolean begin, boolean success) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        if (begin == IDLE_START) {
            mIdleTag = tag;
            sendNotifications(true, false);
            sendContinuation();
        } else {
            tag = mIdleTag;
            mIdleTag = null;
            if (success)  sendOK(tag, "IDLE completed");
            else          sendBAD(tag, "IDLE stopped without DONE");
        }
        return CONTINUE_PROCESSING;
    }

    boolean doSETQUOTA(String tag, String qroot, Map<String, String> limits) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        // cannot set quota from IMAP at present
        sendNO(tag, "SETQUOTA failed");
        return CONTINUE_PROCESSING;
    }

    boolean doGETQUOTA(String tag, ImapPath qroot) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        try {
            if (!qroot.belongsTo(mCredentials.getAccountId())) {
                ZimbraLog.imap.info("GETQUOTA failed: cannot get quota for other user's mailbox: " + qroot);
                sendNO(tag, "GETQUOTA failed: permission denied");
                return CONTINUE_PROCESSING;
            }

            long quota = mCredentials.getAccount().getIntAttr(Provisioning.A_zimbraMailQuota, 0);
            if (qroot == null || !qroot.equals("") || quota <= 0) {
                ZimbraLog.imap.info("GETQUOTA failed: unknown quota root: '" + qroot + "'");
                sendNO(tag, "GETQUOTA failed: unknown quota root");
                return CONTINUE_PROCESSING;
            }
            // RFC 2087 3: "STORAGE  Sum of messages' RFC822.SIZE, in units of 1024 octets"
            sendUntagged("QUOTA \"\" (STORAGE " + (mCredentials.getMailbox().getSize() / 1024) + ' ' + (quota / 1024) + ')');
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("GETQUOTA failed", e);
            sendNO(tag, "GETQUOTA failed");
            return canContinue(e);
        }

        sendNotifications(true, false);
        sendOK(tag, "GETQUOTA completed");
        return CONTINUE_PROCESSING;
    }

    boolean doGETQUOTAROOT(String tag, ImapPath qroot) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        try {
            if (!qroot.belongsTo(mCredentials.getAccountId())) {
                ZimbraLog.imap.info("GETQUOTAROOT failed: cannot get quota root for other user's mailbox: " + qroot);
                sendNO(tag, "GETQUOTAROOT failed: permission denied");
                return CONTINUE_PROCESSING;
            }

            // make sure the folder exists and is visible
            if (!qroot.isVisible()) {
                ZimbraLog.imap.info("GETQUOTAROOT failed: folder not visible: '" + qroot + "'");
                sendNO(tag, "GETQUOTAROOT failed");
                return CONTINUE_PROCESSING;
            }

            // see if there's any quota on the account
            long quota = mCredentials.getAccount().getIntAttr(Provisioning.A_zimbraMailQuota, 0);
            sendUntagged("QUOTAROOT " + qroot.asUtf7String() + (quota > 0 ? " \"\"" : ""));
            if (quota > 0)
                sendUntagged("QUOTA \"\" (STORAGE " + (mCredentials.getMailbox().getSize() / 1024) + ' ' + (quota / 1024) + ')');
        } catch (ServiceException e) {
            if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER))
                ZimbraLog.imap.info("GETQUOTAROOT failed: no such folder: " + qroot);
            else
                ZimbraLog.imap.warn("GETQUOTAROOT failed", e);
            sendNO(tag, "GETQUOTAROOT failed");
            return canContinue(e);
        }

        sendNotifications(true, false);
        sendOK(tag, "GETQUOTAROOT completed");
        return CONTINUE_PROCESSING;
    }

    boolean doNAMESPACE(String tag) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        sendUntagged("NAMESPACE ((\"\" \"/\")) ((\"" + ImapPath.NAMESPACE_PREFIX + "\" \"/\")) NIL");
        sendNotifications(true, false);
        sendOK(tag, "NAMESPACE completed");
        return CONTINUE_PROCESSING;
    }

    boolean doCHECK(String tag) throws IOException {
        if (!checkState(tag, State.SELECTED))
            return CONTINUE_PROCESSING;

        sendNotifications(true, false);
        sendOK(tag, "CHECK completed");
        return CONTINUE_PROCESSING;
    }

    boolean doCLOSE(String tag) throws IOException {
        if (!checkState(tag, State.SELECTED))
            return CONTINUE_PROCESSING;

        try {
            // 6.4.2: "The CLOSE command permanently removes all messages that have the \Deleted
            //         flag set from the currently selected mailbox, and returns to the authenticated
            //         state from the selected state.  No untagged EXPUNGE responses are sent.
            //
            //         No messages are removed, and no error is given, if the mailbox is
            //         selected by an EXAMINE command or is otherwise selected read-only."
            if (mSelectedFolder.isWritable() && mSelectedFolder.getPath().isWritable(ACL.RIGHT_DELETE))
                expungeMessages(mSelectedFolder, null);
        } catch (ServiceException e) {
            // log the error but keep going...
            ZimbraLog.imap.warn("error during CLOSE", e);
        }

        unsetSelectedFolder();

        sendOK(tag, "CLOSE completed");
        return CONTINUE_PROCESSING;
    }

    // RFC 3691 2: "The UNSELECT command frees server's resources associated with the selected
    //              mailbox and returns the server to the authenticated state.  This command
    //              performs the same actions as CLOSE, except that no messages are permanently
    //              removed from the currently selected mailbox."
    boolean doUNSELECT(String tag) throws IOException {
        if (!checkState(tag, State.SELECTED))
            return CONTINUE_PROCESSING;

        unsetSelectedFolder();

        sendOK(tag, "UNSELECT completed");
        return CONTINUE_PROCESSING;
    }
    
    private final int SUGGESTED_DELETE_BATCH_SIZE = 30;

    boolean doEXPUNGE(String tag, boolean byUID, String sequenceSet) throws IOException {
        if (!checkState(tag, State.SELECTED)) {
            return CONTINUE_PROCESSING;
        } else if (!mSelectedFolder.isWritable()) {
            sendNO(tag, "mailbox selected READ-ONLY");
            return CONTINUE_PROCESSING;
        }

        String command = (byUID ? "UID EXPUNGE" : "EXPUNGE");
        try {
            if (!mSelectedFolder.getPath().isWritable(ACL.RIGHT_DELETE))
                throw ServiceException.PERM_DENIED("you do not have permission to delete messages from this folder");

            expungeMessages(mSelectedFolder, sequenceSet);
        } catch (ServiceException e) {
            ZimbraLog.imap.warn(command + " failed", e);
            sendNO(tag, command + " failed");
            return canContinue(e);
        }

        sendNotifications(true, false);
        sendOK(tag, command + " completed");
        return CONTINUE_PROCESSING;
    }

    void expungeMessages(ImapFolder i4folder, String sequenceSet) throws ServiceException, IOException {
        Set<ImapMessage> i4set;
        synchronized (mSelectedFolder.getMailbox()) {
            i4set = (sequenceSet == null ? null : i4folder.getSubsequence(sequenceSet, true));
        }
        List<Integer> ids = new ArrayList<Integer>(SUGGESTED_DELETE_BATCH_SIZE);

        long checkpoint = System.currentTimeMillis();
        for (int i = 1, max = i4folder.getSize(); i <= max; i++) {
            ImapMessage i4msg = i4folder.getBySequence(i);
            if (i4msg != null && !i4msg.isExpunged() && (i4msg.flags & Flag.BITMASK_DELETED) > 0)
                if (i4set == null || i4set.contains(i4msg))
                    ids.add(i4msg.msgId);

            if (ids.size() >= (i == max ? 1 : SUGGESTED_DELETE_BATCH_SIZE)) {
                try {
                    ZimbraLog.imap.debug("  ** deleting: " + ids);
                    
                    int[] idsArray = new int[ids.size()];
                    int counter  = 0;
                    for (int id : ids)
                        idsArray[counter++] = id;
                    mSelectedFolder.getMailbox().delete(getContext(), idsArray, MailItem.TYPE_UNKNOWN, null);
                    
                } catch (MailServiceException.NoSuchItemException e) {
                    // something went wrong, so delete *this* batch one at a time
                    for (int id : ids) {
                        try {
                            ZimbraLog.imap.debug("  ** fallback deleting: " + id);
                            mSelectedFolder.getMailbox().delete(getContext(), new int[] {id}, MailItem.TYPE_UNKNOWN, null);
                        } catch (MailServiceException.NoSuchItemException nsie) {
                            i4msg = i4folder.getById(id);
                            if (i4msg != null)
                                i4msg.setExpunged(true);
                        }
                    }
                }
                ids.clear();

                // send a gratuitous untagged response to keep pissy clients from closing the socket from inactivity
                long now = System.currentTimeMillis();
                if (now - checkpoint > MAXIMUM_IDLE_PROCESSING_MILLIS) {
                    sendIdleUntagged();  checkpoint = now;
                }
            }
        }
    }

    private static final int RETURN_MIN   = 0x01;
    private static final int RETURN_MAX   = 0x02;
    private static final int RETURN_ALL   = 0x04;
    private static final int RETURN_COUNT = 0x08;
    private static final int RETURN_SAVE  = 0x10;

    private static final int LARGEST_FOLDER_BATCH = 600;
    static final byte[] ITEM_TYPES = new byte[] { MailItem.TYPE_MESSAGE, MailItem.TYPE_CONTACT };

    boolean doSEARCH(String tag, ImapSearch i4search, boolean byUID, Integer options) throws IOException {
        if (!checkState(tag, State.SELECTED))
            return CONTINUE_PROCESSING;

        boolean saveResults = (options != null && (options & RETURN_SAVE) != 0);
        ImapMessageSet hits;

        try {
            Mailbox mbox = mSelectedFolder.getMailbox();
            synchronized (mbox) {
                if (i4search.canBeRunLocally()) {
                    hits = i4search.evaluate(mSelectedFolder);
                } else {
                    String search = i4search.toZimbraSearch(mSelectedFolder);
                    if (!mSelectedFolder.isVirtual())
                        search = "in:" + mSelectedFolder.getQuotedPath() + ' ' + search;
                    else if (mSelectedFolder.getSize() <= LARGEST_FOLDER_BATCH)
                        search = ImapSearch.sequenceAsSearchTerm(mSelectedFolder, mSelectedFolder.getSubsequence("1:*", false), false) + ' ' + search;
                    else
                        search = '(' + mSelectedFolder.getQuery() + ") " + search;
                    ZimbraLog.imap.info("[ search is: " + search + " ]");

                    SearchParams params = new SearchParams();
                    params.setQueryStr(search);
                    params.setTypes(ITEM_TYPES);
                    params.setSortBy(MailboxIndex.SortBy.DATE_ASCENDING);
                    params.setChunkSize(2000);
                    params.setPrefetch(false);
                    params.setMode(Mailbox.SearchResultMode.IDS);
                    ZimbraQueryResults zqr = mbox.search(SoapProtocol.Soap12, getContext(), params);

                    hits = new ImapMessageSet();
                    try {
                        for (ZimbraHit hit = zqr.getFirstHit(); hit != null; hit = zqr.getNext())
                            hits.add(mSelectedFolder.getById(hit.getItemId()));
                    } finally {
                        zqr.doneWithSearchResults();
                    }
                }

                hits.remove(null);
        	}
        } catch (ParseException pe) {
            ZimbraLog.imap.warn("SEARCH failed (bad query)", pe);
            sendNO(tag, "SEARCH failed");
            return CONTINUE_PROCESSING;
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("SEARCH failed", e);
            sendNO(tag, "SEARCH failed");
            return CONTINUE_PROCESSING;
        }

        StringBuilder result = null;
        if (options == null) {
            result = new StringBuilder("SEARCH");
            for (ImapMessage i4msg : hits)
                result.append(' ').append(byUID ? i4msg.imapUid : i4msg.sequence);
        } else if (options != RETURN_SAVE) {
            result = new StringBuilder("ESEARCH (TAG \"").append(tag).append("\")");
            if (!hits.isEmpty() && (options & RETURN_MIN) != 0)
                result.append(" MIN ").append(byUID ? hits.first().imapUid : hits.first().sequence);
            if (!hits.isEmpty() && (options & RETURN_MAX) != 0)
                result.append(" MAX ").append(byUID ? hits.last().imapUid : hits.last().sequence);
            if ((options & RETURN_COUNT) != 0)
                result.append(" COUNT ").append(hits.size());
            if (!hits.isEmpty() && (options & RETURN_ALL) != 0)
                result.append(" ALL ").append(ImapFolder.encodeSubsequence(hits, byUID));
        }

        if (saveResults) {
            if (hits.isEmpty() || options == RETURN_SAVE || (options & (RETURN_COUNT | RETURN_ALL)) != 0) {
                mSelectedFolder.saveSearchResults(hits);
            } else {
                ImapMessageSet saved = new ImapMessageSet();
                if ((options & RETURN_MIN) != 0)
                    saved.add(hits.first());
                if ((options & RETURN_MAX) != 0)
                    saved.add(hits.last());
                mSelectedFolder.saveSearchResults(saved);
            }
        }

        if (result != null)
            sendUntagged(result.toString());
        sendNotifications(false, false);
        sendOK(tag, "SEARCH completed");
        return CONTINUE_PROCESSING;
    }

    static final int FETCH_BODY          = 0x0001;
    static final int FETCH_BODYSTRUCTURE = 0x0002;
    static final int FETCH_ENVELOPE      = 0x0004;
    static final int FETCH_FLAGS         = 0x0008;
    static final int FETCH_INTERNALDATE  = 0x0010;
    static final int FETCH_RFC822_SIZE   = 0x0020;
    static final int FETCH_BINARY_SIZE   = 0x0040;
    static final int FETCH_UID           = 0x0080;
    static final int FETCH_MARK_READ     = 0x1000;
    private static final int FETCH_FROM_CACHE = FETCH_FLAGS | FETCH_UID;
    private static final int FETCH_FROM_MIME  = FETCH_BODY | FETCH_BODYSTRUCTURE | FETCH_ENVELOPE;

    static final int FETCH_FAST = FETCH_FLAGS | FETCH_INTERNALDATE | FETCH_RFC822_SIZE;
    static final int FETCH_ALL  = FETCH_FAST  | FETCH_ENVELOPE;
    static final int FETCH_FULL = FETCH_ALL   | FETCH_BODY;

    abstract OutputStream getFetchOutputStream();

    boolean doFETCH(String tag, String sequenceSet, int attributes, List<ImapPartSpecifier> parts, boolean byUID) throws IOException, ImapParseException {
        if (!checkState(tag, State.SELECTED))
            return CONTINUE_PROCESSING;

        // 6.4.8: "However, server implementations MUST implicitly include the UID message
        //         data item as part of any FETCH response caused by a UID command, regardless
        //         of whether a UID was specified as a message data item to the FETCH."
        if (byUID)
            attributes |= FETCH_UID;
        String command = (byUID ? "UID FETCH" : "FETCH");
        boolean markRead = mSelectedFolder.isWritable() && (attributes & FETCH_MARK_READ) != 0;

        List<ImapPartSpecifier> fullMessage = new ArrayList<ImapPartSpecifier>();
        if (parts != null && !parts.isEmpty()) {
            for (Iterator<ImapPartSpecifier> it = parts.iterator(); it.hasNext(); ) {
                ImapPartSpecifier pspec = it.next();
                if (pspec.isEntireMessage()) {
                    it.remove();  fullMessage.add(pspec);
                }
            }
        }

        Set<ImapMessage> i4set;
        Mailbox mbox = mSelectedFolder.getMailbox();
        synchronized (mbox) {
            i4set = mSelectedFolder.getSubsequence(sequenceSet, byUID);
        }
        boolean allPresent = byUID || !i4set.contains(null);
        i4set.remove(null);

        for (ImapMessage i4msg : i4set) {
            OutputStream os = getFetchOutputStream();
            ByteArrayOutputStream baosDebug = ZimbraLog.imap.isDebugEnabled() ? new ByteArrayOutputStream() : null;
	        PrintStream result = new PrintStream(new ByteUtil.TeeOutputStream(os, baosDebug), false, "utf-8");
        	try {
                boolean markMessage = markRead && (i4msg.flags & Flag.BITMASK_UNREAD) != 0;
                boolean empty = true;
                byte[] raw = null;
                MailItem item = null;
                MimeMessage mm = null;

                result.print("* " + i4msg.sequence + " FETCH (");

                if (!fullMessage.isEmpty() || !parts.isEmpty() || (attributes & ~FETCH_FROM_CACHE) != 0) {
                    item = mbox.getItemById(getContext(), i4msg.msgId, i4msg.getType());
                }

                if ((attributes & FETCH_UID) != 0) {
                    result.print((empty ? "" : " ") + "UID " + i4msg.imapUid);  empty = false;
                }
                if ((attributes & FETCH_INTERNALDATE) != 0) {
                    result.print((empty ? "" : " ") + "INTERNALDATE \"" + mTimeFormat.format(new Date(item.getDate())) + '"');  empty = false;
                }
                if ((attributes & FETCH_RFC822_SIZE) != 0) {
                    result.print((empty ? "" : " ") + "RFC822.SIZE " + i4msg.getSize(item));  empty = false;
                }
                if ((attributes & FETCH_BINARY_SIZE) != 0) {
                    result.print((empty ? "" : " ") + "BINARY.SIZE[] " + i4msg.getSize(item));  empty = false;
                }

                if (!fullMessage.isEmpty()) {
                    raw = ImapMessage.getContent(item);
                    for (ImapPartSpecifier pspec : fullMessage) {
                        result.print(empty ? "" : " ");  pspec.write(result, os, raw);  empty = false;
                    }
                }

                if (!parts.isEmpty() || (attributes & FETCH_FROM_MIME) != 0) {
                    mm = ImapMessage.getMimeMessage(item, raw);
                    if ((attributes & FETCH_BODY) != 0) {
                        result.print(empty ? "" : " ");  result.print("BODY ");
                        i4msg.serializeStructure(result, mm, false);  empty = false;
                    }
                    if ((attributes & FETCH_BODYSTRUCTURE) != 0) {
                        result.print(empty ? "" : " ");  result.print("BODYSTRUCTURE ");
                        i4msg.serializeStructure(result, mm, true);  empty = false;
                    }
                    if ((attributes & FETCH_ENVELOPE) != 0) {
                        result.print(empty ? "" : " ");  result.print("ENVELOPE ");
                        i4msg.serializeEnvelope(result, mm);  empty = false;
                    }
                    for (ImapPartSpecifier pspec : parts) {
                        result.print(empty ? "" : " ");  pspec.write(result, os, mm);  empty = false;
                    }
                }

                // 6.4.5: "The \Seen flag is implicitly set; if this causes the flags to
                //         change, they SHOULD be included as part of the FETCH responses."
                // FIXME: optimize by doing a single mark-read op on multiple messages
                if (markMessage) {
                    mbox.alterTag(getContext(), i4msg.msgId, i4msg.getType(), Flag.ID_FLAG_UNREAD, false, null);
                }
                if ((attributes & FETCH_FLAGS) != 0 || markMessage) {
                    mSelectedFolder.undirtyMessage(i4msg);
                    result.print(empty ? "" : " ");  result.print(i4msg.getFlags(mSelectedFolder));  empty = false;
                }
            } catch (ImapPartSpecifier.BinaryDecodingException e) {
                // don't write this response line if we're returning NO
                os = baosDebug = null;
                throw new ImapParseException(tag, "UNKNOWN-CTE", command + "failed: unknown content-type-encoding", false);
            } catch (ServiceException e) {
                ZimbraLog.imap.warn("ignoring error during " + command + ": ", e);
                continue;
            } catch (MessagingException e) {
                ZimbraLog.imap.warn("ignoring error during " + command + ": ", e);
                continue;
            } finally {
                if (os != null) {
                    result.write(')');
                    os.write(LINE_SEPARATOR_BYTES, 0, LINE_SEPARATOR_BYTES.length);
                    os.flush();
                }
                if (baosDebug != null)
                    ZimbraLog.imap.debug("  S: " + baosDebug);
            }
        }

        sendNotifications(false, false);
        if (allPresent)
        	sendOK(tag, command + " completed");
        else {
        	// RFC 2180 4.1.2: "The server MAY allow the EXPUNGE of a multi-accessed mailbox,
            //                  and on subsequent FETCH commands return FETCH responses only
		    //                  for non-expunged messages and a tagged NO."
        	sendNO(tag, "some of the requested messages no longer exist");
        }
        return CONTINUE_PROCESSING;
    }

    enum StoreAction { REPLACE, ADD, REMOVE };
    
    private final int SUGGESTED_BATCH_SIZE = 100;

    boolean doSTORE(String tag, String sequenceSet, List<String> flagNames, StoreAction operation, boolean silent, boolean byUID) throws IOException {
        if (!checkState(tag, State.SELECTED))
            return CONTINUE_PROCESSING;
        else if (!mSelectedFolder.isWritable()) {
            sendNO(tag, "mailbox selected READ-ONLY");
            return CONTINUE_PROCESSING;
        }

        String command = (byUID ? "UID STORE" : "STORE");
        List<Tag> newTags = (operation != StoreAction.REMOVE ? new ArrayList<Tag>() : null);
        Mailbox mbox = mSelectedFolder.getMailbox();

        Set<ImapMessage> i4set;
        synchronized (mbox) {
            i4set = mSelectedFolder.getSubsequence(sequenceSet, byUID);
        }
        boolean allPresent = byUID || !i4set.contains(null);
        i4set.remove(null);

        try {
            // get set of relevant tags
            List<ImapFlag> i4flags;
            synchronized (mbox) {
                i4flags = findOrCreateTags(mbox, flagNames, newTags);
            }

            if (operation != StoreAction.REMOVE) {
                for (ImapFlag i4flag : i4flags) {
                    if (i4flag.mId == Flag.ID_FLAG_DELETED && !mSelectedFolder.getPath().isWritable(ACL.RIGHT_DELETE))
                        throw ServiceException.PERM_DENIED("you do not have permission to set the \\Deleted flag");
                }
            }

            // if we're doing a STORE FLAGS (i.e. replace), precompute the new set of flags for all the affected messages
            long tags = 0;  int flags = Flag.BITMASK_UNREAD;  short sflags = 0;
            if (operation == StoreAction.REPLACE) {
                for (ImapFlag i4flag : i4flags) {
                    if (Tag.validateId(i4flag.mId))
                        tags = (i4flag.mPositive ? tags | i4flag.mBitmask : tags & ~i4flag.mBitmask);
                    else if (!i4flag.mPermanent)
                        sflags = (byte) (i4flag.mPositive ? sflags | i4flag.mBitmask : sflags & ~i4flag.mBitmask);
                    else
                        flags = (int) (i4flag.mPositive ? flags | i4flag.mBitmask : flags & ~i4flag.mBitmask);
                }
            }

            long checkpoint = System.currentTimeMillis();

            int i = 0;
            List<ImapMessage> i4list = new ArrayList<ImapMessage>(SUGGESTED_BATCH_SIZE);
            List<Integer> idlist = new ArrayList<Integer>(SUGGESTED_BATCH_SIZE);
            for (ImapMessage msg : i4set) {
                // we're sending 'em off in batches of 100
                i4list.add(msg);  idlist.add(msg.msgId);
                if (++i % SUGGESTED_BATCH_SIZE != 0 && i != i4set.size())
                    continue;

                try {
                    // if it was a STORE [+-]?FLAGS.SILENT, temporarily disable notifications
                    if (silent)
                        mSelectedFolder.disableNotifications();

                    if (operation == StoreAction.REPLACE) {
                        // replace real tags and flags on all messages
                        mbox.setTags(getContext(), ArrayUtil.toIntArray(idlist), MailItem.TYPE_UNKNOWN, flags, tags, null);
                        // replace session tags on all messages
                        for (ImapMessage i4msg : i4list)
                            i4msg.setSessionFlags(sflags, mSelectedFolder);
                    } else if (!i4flags.isEmpty()) {
                        for (ImapFlag i4flag : i4flags) {
                            boolean add = operation == StoreAction.ADD ^ !i4flag.mPositive;
                            if (i4flag.mPermanent) {
                                // real tag; do a batch update to the DB
                                mbox.alterTag(getContext(), ArrayUtil.toIntArray(idlist), MailItem.TYPE_UNKNOWN, i4flag.mId, add, null);
                            } else {
                                // session tag; update one-by-one in memory only
                                for (ImapMessage i4msg : i4list)
                                    i4msg.setSessionFlags((short) (add ? i4msg.sflags | i4flag.mBitmask : i4msg.sflags & ~i4flag.mBitmask), mSelectedFolder);
                            }
                        }
                    }
                } finally {
                    // if it was a STORE [+-]?FLAGS.SILENT, reenable notifications
                    mSelectedFolder.enableNotifications();
                }

                if (!silent) {
                    for (ImapMessage i4msg : i4list) {
                        mSelectedFolder.undirtyMessage(i4msg);
                        StringBuilder ntfn = new StringBuilder();
                        ntfn.append(i4msg.sequence).append(" FETCH (").append(i4msg.getFlags(mSelectedFolder));
                        // 6.4.8: "However, server implementations MUST implicitly include
                        //         the UID message data item as part of any FETCH response
                        //         caused by a UID command..."
                        if (byUID)
                            ntfn.append(" UID ").append(i4msg.imapUid);
                        sendUntagged(ntfn.append(')').toString());
                    }
                } else {
                    // send a gratuitous untagged response to keep pissy clients from closing the socket from inactivity
                    long now = System.currentTimeMillis();
                    if (now - checkpoint > MAXIMUM_IDLE_PROCESSING_MILLIS) {
                        sendIdleUntagged();  checkpoint = now;
                    }
                }

                i4list.clear();  idlist.clear();
            }
        } catch (ServiceException e) {
            deleteTags(newTags);
            ZimbraLog.imap.warn(command + " failed", e);
            sendNO(tag, command + " failed");
            return canContinue(e);
        }

        sendNotifications(false, false);
        // RFC 2180 4.2.1: "If the ".SILENT" suffix is used, and the STORE completed successfully for
        //                  all the non-expunged messages, the server SHOULD return a tagged OK."
        // RFC 2180 4.2.3: "If the ".SILENT" suffix is not used, and a mixture of expunged and non-
        //                  expunged messages are referenced, the server MAY set the flags and return
        //                  a FETCH response for the non-expunged messages along with a tagged NO."
        if (silent || allPresent)
            sendOK(tag, command + " completed");
        else
            sendNO(tag, command + " completed");
        return CONTINUE_PROCESSING;
    }
    
    private final int SUGGESTED_COPY_BATCH_SIZE = 50;

    boolean doCOPY(String tag, String sequenceSet, ImapPath path, boolean byUID) throws IOException {
        if (!checkState(tag, State.SELECTED))
            return CONTINUE_PROCESSING;

        String command = (byUID ? "UID COPY" : "COPY");
        String copyuid = "";
        List<MailItem> copies = new ArrayList<MailItem>();
        Mailbox mbox = mSelectedFolder.getMailbox();

        Set<ImapMessage> i4set;
        synchronized (mbox) {
            i4set = mSelectedFolder.getSubsequence(sequenceSet, byUID);
        }
        // RFC 2180 4.4.1: "The server MAY disallow the COPY of messages in a multi-
        //                  accessed mailbox that contains expunged messages."
        if (!byUID && i4set.contains(null)) {
            sendNO(tag, "COPY rejected because some of the requested messages were expunged");
            return CONTINUE_PROCESSING;
        }
        i4set.remove(null);

        try {
            if (!path.isVisible())
                throw ImapServiceException.FOLDER_NOT_VISIBLE(path.asImapPath());
            else if (!path.isWritable(ACL.RIGHT_INSERT))
                throw ImapServiceException.FOLDER_NOT_WRITABLE(path.asImapPath());

            Object mboxobj = path.getOwnerMailbox();
            ItemId iidTarget = null;
            boolean sameMailbox = false;
            int uvv = -1;

            // check target folder permissions before attempting the copy
            if (mboxobj instanceof Mailbox) {
                Mailbox mboxTarget = (Mailbox) mboxobj;
                sameMailbox = mbox.getAccountId().equalsIgnoreCase(mboxTarget.getAccountId());
                Folder folder = mboxTarget.getFolderByPath(getContext(), path.asZimbraPath());
                iidTarget = new ItemId(folder);
                uvv = ImapFolder.getUIDValidity(folder);
            } else if (mboxobj instanceof ZMailbox) {
                ZMailbox zmbxTarget = (ZMailbox) mboxobj;
                ZFolder zfolder = zmbxTarget.getFolderByPath(path.asZimbraPath());
                iidTarget = new ItemId(zfolder.getId(), path.getOwnerAccount().getId());
                uvv = ImapFolder.getUIDValidity(zfolder);
            } else {
                throw AccountServiceException.NO_SUCH_ACCOUNT(path.getOwner());
            }

            long checkpoint = System.currentTimeMillis();
            List<Integer> srcUIDs = extensionEnabled("UIDPLUS") ? new ArrayList<Integer>() : null;
            List<Integer> copyUIDs = extensionEnabled("UIDPLUS") ? new ArrayList<Integer>() : null;

            int i = 0;
            List<ImapMessage> i4list = new ArrayList<ImapMessage>(SUGGESTED_COPY_BATCH_SIZE);
            List<Integer> idlist = new ArrayList<Integer>(SUGGESTED_COPY_BATCH_SIZE);
            List<Integer> createdList = new ArrayList<Integer>(SUGGESTED_COPY_BATCH_SIZE);
            for (ImapMessage i4msg : i4set) {
                // we're sending 'em off in batches of 50
                i4list.add(i4msg);  idlist.add(i4msg.msgId);
                if (++i % SUGGESTED_COPY_BATCH_SIZE != 0 && i != i4set.size())
                    continue;

                if (sameMailbox) {
                    
                    List<MailItem> copyMsgs;
                    try {
                        byte type = MailItem.TYPE_UNKNOWN;
                        int[] mItemIds = new int[i4list.size()];
                        int counter  = 0;
                        for (ImapMessage curMsg : i4set) {
                            mItemIds[counter++] = i4msg.msgId;
                            if (counter == 1)
                                type = curMsg.getType();
                            else if (i4msg.getType() != type)
                                type = MailItem.TYPE_UNKNOWN;
                        }
                        copyMsgs = mbox.imapCopy(getContext(), mItemIds, type, iidTarget.getId());
                    } catch (IOException e) {
                        throw ServiceException.FAILURE("Caught IOException execiting " + this, e);
                    }
                    
                    copies.addAll(copyMsgs);
                    for (MailItem target : copyMsgs)
                        createdList.add(target.getImapUid());
                } else {
                    ItemActionHelper op = ItemActionHelper.COPY(getContext(), mbox, idlist, MailItem.TYPE_UNKNOWN, null, iidTarget);
                    for (String target : op.getCreatedIds())
                        createdList.add(new ItemId(target, mSelectedFolder.getAuthenticatedAccountId()).getId());
                }

                if (createdList.size() != i4list.size())
                    throw ServiceException.FAILURE("mismatch between original and target count during IMAP COPY", null);
                if (srcUIDs != null) {
                    for (ImapMessage source : i4list)
                        srcUIDs.add(source.imapUid);
                    for (Integer target : createdList)
                        copyUIDs.add(target);
                }

                i4list.clear();  idlist.clear();  createdList.clear();

                // send a gratuitous untagged response to keep pissy clients from closing the socket from inactivity
                long now = System.currentTimeMillis();
                if (now - checkpoint > MAXIMUM_IDLE_PROCESSING_MILLIS) {
                    sendIdleUntagged();  checkpoint = now;
                }
            }

            if (uvv > 0 && srcUIDs != null && srcUIDs.size() > 0)
                copyuid = "[COPYUID " + uvv + ' ' +
                          ImapFolder.encodeSubsequence(srcUIDs) + ' ' +
                          ImapFolder.encodeSubsequence(copyUIDs) + "] ";
        } catch (IOException e) {
            // 6.4.7: "If the COPY command is unsuccessful for any reason, server implementations
            //         MUST restore the destination mailbox to its state before the COPY attempt."
//            deleteMessages(copies);

            ZimbraLog.imap.warn(command + " failed", e);
            sendNO(tag, command + " failed");
            return CONTINUE_PROCESSING;
        } catch (ServiceException e) {
            // 6.4.7: "If the COPY command is unsuccessful for any reason, server implementations
            //         MUST restore the destination mailbox to its state before the COPY attempt."
//            deleteMessages(copies);

            String rcode = "";
            if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER)) {
                ZimbraLog.imap.info(command + " failed: no such folder: " + path);
                if (path.isCreatable())
                    rcode = "[TRYCREATE] ";
            } else {
                ZimbraLog.imap.warn(command + " failed", e);
            }
            sendNO(tag, rcode + command + " failed");
            return canContinue(e);
        }

        // RFC 2180 4.4: "COPY is the only IMAP4 sequence number command that is safe to allow
        //                an EXPUNGE response on.  This is because a client is not permitted
        //                to cascade several COPY commands together."
        sendNotifications(true, false);
    	sendOK(tag, copyuid + command + " completed");
        return CONTINUE_PROCESSING;
    }

    private void deleteMessages(List<MailItem> messages) {
        if (messages == null || messages.isEmpty())
            return;

        for (MailItem item : messages) {
            try {
                item.getMailbox().delete(getContext(), item, null);
            } catch (ServiceException e) {
                ZimbraLog.imap.warn("could not roll back creation of message", e);
            }
        }
    }


    public void sendNotifications(boolean notifyExpunges, boolean flush) throws IOException {
        if (mSelectedFolder == null)
            return;

        // is this the right thing to synchronize on?
        synchronized (mSelectedFolder.getMailbox()) {
            // FIXME: notify untagged NO if close to quota limit

            boolean removed = false, received = mSelectedFolder.checkpointSize();
            if (notifyExpunges) {
                for (Integer index : mSelectedFolder.collapseExpunged()) {
                    sendUntagged(index + " EXPUNGE");  removed = true;
                }
            }
            mSelectedFolder.checkpointSize();

            // notify of any message flag changes
            for (Iterator<ImapMessage> it = mSelectedFolder.dirtyIterator(); it.hasNext(); ) {
                ImapMessage i4msg = it.next();
                if (i4msg.isAdded())
                    i4msg.setAdded(false);
                else
                	sendUntagged(i4msg.sequence + " FETCH (" + i4msg.getFlags(mSelectedFolder) + ')');
            }
            mSelectedFolder.clearDirty();

            // FIXME: not handling RECENT

            if (received || removed)
                sendUntagged(mSelectedFolder.getSize() + " EXISTS");

            if (flush)
                flushOutput();
        }
    }

    @Override
    public void dropConnection() {
        dropConnection(true);
    }
    
    abstract void dropConnection(boolean sendBanner);

    abstract void flushOutput() throws IOException;


    void sendIdleUntagged() throws IOException                   { sendUntagged("NOOP", true); }

    void sendOK(String tag, String response) throws IOException  { sendResponse(tag, response.equals("") ? "OK" : "OK " + response, true); }
    void sendNO(String tag, String response) throws IOException  { sendResponse(tag, response.equals("") ? "NO" : "NO " + response, true); }
    void sendBAD(String tag, String response) throws IOException { sendResponse(tag, response.equals("") ? "BAD" : "BAD " + response, true); }
    void sendUntagged(String response) throws IOException        { sendResponse("*", response, false); }
    void sendUntagged(String response, boolean flush) throws IOException { sendResponse("*", response, flush); }
    void sendContinuation() throws IOException                   { sendResponse("+", null, true); }
    void sendContinuation(String response) throws IOException    { sendResponse("+", response, true); }
    
    private void sendResponse(String status, String msg, boolean flush) throws IOException {
        String response = status + ' ' + (msg == null ? "" : msg);
        if (ZimbraLog.imap.isDebugEnabled())
            ZimbraLog.imap.debug("  S: " + response);
        else if (status.startsWith("BAD"))
            ZimbraLog.imap.info("  S: " + response);
        sendLine(response, flush);
    }

    abstract void sendLine(String line, boolean flush) throws IOException;
}
