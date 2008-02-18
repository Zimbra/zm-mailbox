/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.imap.ImapCredentials.EnabledHack;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.imap.ImapMessage.ImapMessageSet;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.security.sasl.*;
import com.zimbra.cs.service.mail.FolderAction;
import com.zimbra.cs.service.mail.ItemActionHelper;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.tcpserver.ProtocolHandler;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZGrant;
import com.zimbra.cs.zclient.ZMailbox;

/**
 * @author dkarp
 */
public abstract class ImapHandler extends ProtocolHandler {
    enum State { NOT_AUTHENTICATED, AUTHENTICATED, SELECTED, LOGOUT }

    enum ActivatedExtension { CONDSTORE, QRESYNC };

    private static final long MAXIMUM_IDLE_PROCESSING_MILLIS = 15 * Constants.MILLIS_PER_SECOND;

    static final char[] LINE_SEPARATOR       = { '\r', '\n' };
    static final byte[] LINE_SEPARATOR_BYTES = { '\r', '\n' };

    private DateFormat mTimeFormat   = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z", Locale.US);
    private DateFormat mDateFormat   = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
    private DateFormat mZimbraFormat = DateFormat.getDateInstance(DateFormat.SHORT);

    protected ImapConfig      mConfig;
    protected OutputStream    mOutputStream;
    protected Authenticator   mAuthenticator;
    protected ImapCredentials mCredentials;
    protected boolean         mStartedTLS;
    protected String          mLastCommand;
    protected ImapFolder      mSelectedFolder;
    private   String          mIdleTag;
    private   String          mOrigRemoteAddress;
    protected boolean         mGoodbyeSent;
    private   Set<ActivatedExtension> mActivatedExtensions;

    ImapHandler(MinaImapServer server) {
        super(null);
        mConfig = (ImapConfig) server.getConfig();
    }

    ImapHandler(ImapServer server) {
        super(server);
        mConfig = (ImapConfig) server.getConfig();
    }

    DateFormat getTimeFormat()   { return mTimeFormat; }
    DateFormat getDateFormat()   { return mDateFormat; }
    DateFormat getZimbraFormat() { return mZimbraFormat; }

    ImapCredentials getCredentials()  { return mCredentials; }

    public boolean isSSLEnabled() { return mConfig.isSSLEnabled(); }

    public ImapConfig getConfig() { return mConfig; }
    
    protected String getOrigRemoteIpAddr() { return mOrigRemoteAddress; }
    
    protected void setOrigRemoteIpAddr(String ip) { mOrigRemoteAddress = ip; }
    
    static final boolean STOP_PROCESSING = false, CONTINUE_PROCESSING = true;

    protected void handleImapParseException(ImapParseException e) throws IOException {
        if (e.mTag == null)
            sendUntagged("BAD " + e.getMessage(), true);
        else if (e.mCode != null)
            sendNO(e.mTag, '[' + e.mCode + "] " + e.getMessage());
        else if (e.mNO)
            sendNO(e.mTag, e.getMessage());
        else
            sendBAD(e.mTag, e.getMessage());
    }

    void checkEOF(String tag, ImapRequest req) throws ImapParseException {
        if (!req.eof())
            throw new ImapParseException(tag, "excess characters at end of command");
    }

    boolean continueAuthentication(ImapRequest req) throws IOException {
        String tag = getTag(mAuthenticator);
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
                mAuthenticator = null;
                return CONTINUE_PROCESSING;
            }

            byte[] response = req.readBase64(false);
            checkEOF(tag, req);
            return continueAuthentication(response);
        } catch (ImapParseException ipe) {
            sendBAD(tag, ipe.getMessage());
            mAuthenticator = null;
            return CONTINUE_PROCESSING;
        }
    }
    
    private boolean continueAuthentication(byte[] response) throws IOException {
        mAuthenticator.handle(response);
        if (mAuthenticator.isComplete()) {
            if (mAuthenticator.isAuthenticated()) {
                // Authentication successful
                completeAuthentication();
                enableInactivityTimer();
                return CONTINUE_PROCESSING;
            }
            // Authentication failed
            boolean canContinue = canContinue(mAuthenticator);
            mAuthenticator = null;
            return canContinue;
        }
        return CONTINUE_PROCESSING;
    }
    
    boolean isIdle() {
        return mIdleTag != null;
    }

    private static String getTag(Authenticator auth) {
        return ((ImapAuthenticatorUser) auth.getAuthenticatorUser()).getTag();
    }

    private static boolean canContinue(Authenticator auth) {
        return ((ImapAuthenticatorUser) auth.getAuthenticatorUser()).canContinue();
    }

    boolean executeRequest(ImapRequest req) throws IOException, ImapParseException {
        if (isIdle()) {
            boolean clean = false;
            try {
                clean = req.readATOM().equals("DONE") && req.eof();
            } catch (ImapParseException ipe) { }
            return doIDLE(null, IDLE_STOP, clean);
        }

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
                        List<Append> appends = new ArrayList<Append>(1);
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        do {
                            List<String> flags = null;  Date date = null;
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
                                req.skipChar(')');
                                appends.add(new Append(flags, date, parts));
                            } else {
                                appends.add(new Append(flags, date, req.readLiteral8()));
                            }
                        } while (!req.eof() && extensionEnabled("MULTIAPPEND"));
                        checkEOF(tag, req);
                        return doAPPEND(tag, path, appends);
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
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials, ImapPath.Scope.NAME);
                        checkEOF(tag, req);
                        return doDELETE(tag, path);
                    } else if (command.equals("DELETEACL") && extensionEnabled("ACL")) {
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        req.skipSpace();  String principal = req.readAstring();
                        checkEOF(tag, req);
                        return doDELETEACL(tag, path, principal);
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
                        byte params = 0;  QResyncInfo qri = null;
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        if (req.peekChar() == ' ') {
                            req.skipSpace();  req.skipChar('(');
                            while (req.peekChar() != ')') {
                                if (params != 0)
                                    req.skipSpace();
                                String param = req.readATOM();
                                if (param.equals("CONDSTORE") && extensionEnabled("CONDSTORE")) {
                                    params |= ImapFolder.SELECT_CONDSTORE;
                                } else if (param.equals("QRESYNC") && sessionActivated(ActivatedExtension.QRESYNC)) {
                                    params |= ImapFolder.SELECT_CONDSTORE;
                                    qri = new QResyncInfo();
                                    req.skipSpace();  req.skipChar('(');
                                    qri.uvv = req.parseInteger(req.readNumber());  req.skipSpace();
                                    qri.modseq = req.parseInteger(req.readNumber());
                                    if (req.peekChar() == ' ') {
                                        req.skipSpace();
                                        if (req.peekChar() != '(')
                                            qri.knownUIDs = req.readSequence(false);
                                        if (qri.knownUIDs == null || req.peekChar() == ' ') {
                                            if (qri.knownUIDs != null)  req.skipSpace();
                                            req.skipChar('(');  qri.seqMilestones = req.readSequence(false);
                                            req.skipSpace();    qri.uidMilestones = req.readSequence(false);
                                            req.skipChar(')');
                                        }
                                    }
                                    req.skipChar(')');
                                } else {
                                    throw new ImapParseException(tag, "unknown EXAMINE parameter \"" + param + '"');
                                }
                            }
                            req.skipChar(')');
                        }
                        checkEOF(tag, req);
                        return doEXAMINE(tag, path, params, qri);
                    } else if (command.equals("ENABLE") && extensionEnabled("ENABLE")) {
                        List<String> extensions = new ArrayList<String>();
                        do {
                            req.skipSpace();  extensions.add(req.readATOM());
                        } while (!req.eof());
                        checkEOF(tag, req);
                        return doENABLE(tag, extensions);
                    }
                    break;
                case 'F':
                    if (command.equals("FETCH")) {
                        List<ImapPartSpecifier> parts = new ArrayList<ImapPartSpecifier>();
                        int modseq = -1;
                        req.skipSpace();  String sequence = req.readSequence();
                        req.skipSpace();  int attributes = req.readFetch(parts);
                        if (req.peekChar() == ' ') {
                            boolean first = true;
                            req.skipSpace();  req.skipChar('(');
                            while (req.peekChar() != ')') {
                                if (!first)
                                    req.skipSpace();
                                String modifier = req.readATOM();
                                if (modifier.equals("CHANGEDSINCE") && extensionEnabled("CONDSTORE")) {
                                    req.skipSpace();  modseq = req.parseInteger(req.readNumber(ImapRequest.ZERO_OK));
                                } else if (modifier.equals("VANISHED") && byUID && sessionActivated(ActivatedExtension.QRESYNC)) {
                                    attributes |= FETCH_VANISHED;
                                } else {
                                    throw new ImapParseException(tag, "bad FETCH modifier: " + modifier);
                                }
                                first = false;
                            }
                            req.skipChar(')');
                        }
                        checkEOF(tag, req);
                        return doFETCH(tag, sequence, attributes, parts, byUID, modseq);
                    }
                    break;
                case 'G':
                    if (command.equals("GETQUOTA") && extensionEnabled("QUOTA")) {
                        req.skipSpace();  ImapPath qroot = new ImapPath(req.readAstring(), mCredentials);
                        checkEOF(tag, req);
                        return doGETQUOTA(tag, qroot);
                    } else if (command.equals("GETACL") && extensionEnabled("ACL")) {
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        checkEOF(tag, req);
                        return doGETACL(tag, path);
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
                        Set<String> patterns = new LinkedHashSet<String>(2);
                        boolean parenthesized = false;
                        byte selectOptions = 0, returnOptions = 0;

                        req.skipSpace();
                        if (req.peekChar() == '(' && extensionEnabled("LIST-EXTENDED")) {
                            req.skipChar('(');
                            while (req.peekChar() != ')') {
                                if (selectOptions != 0)
                                    req.skipSpace();
                                String option = req.readATOM();
                                if (option.equals("RECURSIVEMATCH"))   selectOptions |= SELECT_RECURSIVE;
                                else if (option.equals("SUBSCRIBED"))  selectOptions |= SELECT_SUBSCRIBED;
                                else if (option.equals("REMOTE"))      selectOptions |= SELECT_REMOTE;
                                else
                                    throw new ImapParseException(tag, "unknown LIST select option \"" + option + '"');
                            }
                            if ((selectOptions & (SELECT_SUBSCRIBED | SELECT_RECURSIVE)) == SELECT_RECURSIVE)
                                throw new ImapParseException(tag, "must include SUBSCRIBED when specifying RECURSIVEMATCH", false);
                            req.skipChar(')');  req.skipSpace();
                        }

                        String base = req.readFolder();  req.skipSpace();

                        if (req.peekChar() == '(' && extensionEnabled("LIST-EXTENDED")) {
                            parenthesized = true;  req.skipChar('(');
                        }
                        do {
                            if (!patterns.isEmpty())  req.skipSpace();
                            patterns.add(req.readFolderPattern());
                        } while (parenthesized && req.peekChar() != ')');
                        if (parenthesized)
                            req.skipChar(')');

                        if (req.peekChar() == ' ' && extensionEnabled("LIST-EXTENDED")) {
                            req.skipSpace();  req.skipAtom("RETURN");
                            req.skipSpace();  req.skipChar('(');
                            while (req.peekChar() != ')') {
                                if (returnOptions != 0)
                                    req.skipSpace();
                                String option = req.readATOM();
                                if (option.equals("SUBSCRIBED"))     returnOptions |= RETURN_SUBSCRIBED;
                                else if (option.equals("CHILDREN"))  returnOptions |= RETURN_CHILDREN;
                                else
                                    throw new ImapParseException(tag, "unknown LIST return option \"" + option + '"');
                            }
                            req.skipChar(')');
                        }
                        checkEOF(tag, req);
                        return doLIST(tag, base, patterns, selectOptions, returnOptions);
                    } else if (command.equals("LSUB")) {
                        req.skipSpace();  String base = req.readFolder();
                        req.skipSpace();  String pattern = req.readFolderPattern();
                        checkEOF(tag, req);
                        return doLSUB(tag, base, pattern);
                    } else if (command.equals("LISTRIGHTS") && extensionEnabled("ACL")) {
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        req.skipSpace();  String principal = req.readAstring();
                        checkEOF(tag, req);
                        return doLISTRIGHTS(tag, path, principal);
                    }
                    break;
                case 'M':
                    if (command.equals("MYRIGHTS") && extensionEnabled("ACL")) {
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        checkEOF(tag, req);
                        return doMYRIGHTS(tag, path);
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
                        req.skipSpace();  ImapPath folder = new ImapPath(req.readFolder(), mCredentials, ImapPath.Scope.NAME);
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials, ImapPath.Scope.NAME);
                        checkEOF(tag, req);
                        return doRENAME(tag, folder, path);
                    }
                    break;
                case 'S':
                    if (command.equals("STORE")) {
                        StoreAction operation = StoreAction.REPLACE;
                        boolean silent = false;  int modseq = -1;
                        req.skipSpace();  String sequence = req.readSequence();  req.skipSpace();

                        if (req.peekChar() == '(' && extensionEnabled("CONDSTORE")) {
                            req.skipChar('(');  req.skipAtom("UNCHANGEDSINCE");  req.skipSpace();
                            modseq = req.parseInteger(req.readNumber(ImapRequest.ZERO_OK));
                            req.skipChar(')');  req.skipSpace();
                        }

                        switch (req.peekChar()) {
                            case '+':  req.skipChar('+');  operation = StoreAction.ADD;     break;
                            case '-':  req.skipChar('-');  operation = StoreAction.REMOVE;  break;
                        }
                        String cmd = req.readATOM();
                        if (cmd.equals("FLAGS.SILENT"))  silent = true;
                        else if (!cmd.equals("FLAGS"))   throw new ImapParseException(tag, "invalid store-att-flags");
                        req.skipSpace();  List<String> flags = req.readFlags();

                        checkEOF(tag, req);
                        return doSTORE(tag, sequence, flags, operation, silent, modseq, byUID);
                    } else if (command.equals("SELECT")) {
                        byte params = 0;  QResyncInfo qri = null;
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        if (req.peekChar() == ' ') {
                            req.skipSpace();  req.skipChar('(');
                            while (req.peekChar() != ')') {
                                if (params != 0)
                                    req.skipSpace();
                                String param = req.readATOM();
                                if (param.equals("CONDSTORE") && extensionEnabled("CONDSTORE")) {
                                    params |= ImapFolder.SELECT_CONDSTORE;
                                } else if (param.equals("QRESYNC") && sessionActivated(ActivatedExtension.QRESYNC)) {
                                    params |= ImapFolder.SELECT_CONDSTORE;
                                    qri = new QResyncInfo();
                                    req.skipSpace();  req.skipChar('(');
                                    qri.uvv = req.parseInteger(req.readNumber());  req.skipSpace();
                                    qri.modseq = req.parseInteger(req.readNumber());
                                    if (req.peekChar() == ' ') {
                                        req.skipSpace();
                                        if (req.peekChar() != '(')
                                            qri.knownUIDs = req.readSequence(false);
                                        if (qri.knownUIDs == null || req.peekChar() == ' ') {
                                            if (qri.knownUIDs != null)  req.skipSpace();
                                            req.skipChar('(');  qri.seqMilestones = req.readSequence(false);
                                            req.skipSpace();    qri.uidMilestones = req.readSequence(false);
                                            req.skipChar(')');
                                        }
                                    }
                                    req.skipChar(')');
                                } else {
                                    throw new ImapParseException(tag, "unknown SELECT parameter \"" + param + '"');
                                }
                            }
                            req.skipChar(')');
                        }
                        checkEOF(tag, req);
                        return doSELECT(tag, path, params, qri);
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
                            if (flag.equals("MESSAGES"))            status |= STATUS_MESSAGES;
                            else if (flag.equals("RECENT"))         status |= STATUS_RECENT;
                            else if (flag.equals("UIDNEXT"))        status |= STATUS_UIDNEXT;
                            else if (flag.equals("UIDVALIDITY"))    status |= STATUS_UIDVALIDITY;
                            else if (flag.equals("UNSEEN"))         status |= STATUS_UNSEEN;
                            else if (flag.equals("HIGHESTMODSEQ"))  status |= STATUS_HIGHESTMODSEQ;
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
                    } else if (command.equals("SETACL") && extensionEnabled("ACL")) {
                        StoreAction action = StoreAction.REPLACE;
                        req.skipSpace();  ImapPath path = new ImapPath(req.readFolder(), mCredentials);
                        req.skipSpace();  String principal = req.readAstring();
                        req.skipSpace();  String i4rights = req.readAstring();
                        checkEOF(tag, req);
                        if (i4rights.startsWith("+"))       { action = StoreAction.ADD;  i4rights = i4rights.substring(1); }
                        else if (i4rights.startsWith("-"))  { action = StoreAction.REMOVE;  i4rights = i4rights.substring(1); }
                        return doSETACL(tag, path, principal, i4rights, action);
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
        else if (isAuthenticated())
            return State.AUTHENTICATED;
        else
            return State.NOT_AUTHENTICATED;
    }

    protected boolean isAuthenticated() {
        return mCredentials != null;
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

    void unsetSelectedFolder() throws IOException {
        if (mSelectedFolder != null) {
            mSelectedFolder.unregister();
            if (sessionActivated(ActivatedExtension.QRESYNC))
                sendUntagged("[CLOSED]");
        }
        mSelectedFolder = null;
    }

    void setSelectedFolder(ImapFolder i4folder) throws ServiceException, IOException {
        if (i4folder == mSelectedFolder) {
            if (sessionActivated(ActivatedExtension.QRESYNC))
                sendUntagged("[CLOSED]");
            return;
        }

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
        
        ZimbraLog.imap.info("selected folder " + mSelectedFolder.toString());
    }

    boolean canContinue(ServiceException e) {
        return e.getCode().equals(MailServiceException.MAINTENANCE) ? STOP_PROCESSING : CONTINUE_PROCESSING;
    }

    private OperationContext getContext() throws ServiceException {
        if (!isAuthenticated())
            throw ServiceException.AUTH_REQUIRED();
        return mCredentials.getContext().setSession(mSelectedFolder);
    }


    boolean doCAPABILITY(String tag) throws IOException {
        sendUntagged(getCapabilityString());
        sendOK(tag, "CAPABILITY completed");
        return CONTINUE_PROCESSING;
    }

    private static final Set<String> SUPPORTED_EXTENSIONS = new LinkedHashSet<String>(Arrays.asList(
        "ACL", "BINARY", "CATENATE", "CHILDREN", "CONDSTORE", "ENABLE", "ESEARCH", "ID", "IDLE",
        "LIST-EXTENDED", "LITERAL+", "LOGIN-REFERRALS", "MULTIAPPEND", "NAMESPACE", "QUOTA", "RIGHTS=ektx",
        "SASL-IR", "UIDPLUS", "UNSELECT", "WITHIN", "X-DRAFT-I05-SEARCHRES", "X-DRAFT-W05-QRESYNC"
    ));

    protected String getCapabilityString() {
        // [IMAP4rev1]        RFC 3501: Internet Message Access Protocol - Version 4rev1
        // [LOGINDISABLED]    RFC 3501: Internet Message Access Protocol - Version 4rev1
        // [STARTTLS]         RFC 3501: Internet Message Access Protocol - Version 4rev1
        // [AUTH=PLAIN]       RFC 4616: The PLAIN Simple Authentication and Security Layer (SASL) Mechanism
        // [AUTH=GSSAPI]      RFC 1731: IMAP4 Authentication Mechanisms
        // [ACL]              RFC 4314: IMAP4 Access Control List (ACL) Extension
        // [BINARY]           RFC 3516: IMAP4 Binary Content Extension
        // [CATENATE]         RFC 4469: Internet Message Access Protocol (IMAP) CATENATE Extension
        // [CHILDREN]         RFC 3348: IMAP4 Child Mailbox Extension
        // [CONDSTORE]        RFC 4551: IMAP Extension for Conditional STORE Operation or Quick Flag Changes Resynchronization
        // [ENABLE]           draft-gulbrandsen-imap-enable-03: The IMAP ENABLE Extension
        // [ESEARCH]          RFC 4731: IMAP4 Extension to SEARCH Command for Controlling What Kind of Information Is Returned
        // [ID]               RFC 2971: IMAP4 ID Extension
        // [IDLE]             RFC 2177: IMAP4 IDLE command
        // [LIST-EXTENDED]    draft-ietf-imapext-list-extensions-18: IMAP4 LIST Command Extensions
        // [LITERAL+]         RFC 2088: IMAP4 non-synchronizing literals
        // [LOGIN-REFERRALS]  RFC 2221: IMAP4 Login Referrals
        // [MULTIAPPEND]      RFC 3502: Internet Message Access Protocol (IMAP) - MULTIAPPEND Extension
        // [NAMESPACE]        RFC 2342: IMAP4 Namespace
        // [QUOTA]            RFC 2087: IMAP4 QUOTA extension
        // [RIGHTS=ektx]      RFC 4314: IMAP4 Access Control List (ACL) Extension
        // [SASL-IR]          RFC 4959: IMAP Extension for Simple Authentication and Security Layer (SASL) Initial Client Response
        // [UIDPLUS]          RFC 4315: Internet Message Access Protocol (IMAP) - UIDPLUS extension
        // [UNSELECT]         RFC 3691: IMAP UNSELECT command
        // [WITHIN]           RFC 5032: WITHIN Search Extension to the IMAP Protocol
        // [X-DRAFT-I05-SEARCHRES]  draft-melnikov-imap-search-res-05: IMAP extension for referencing the last SEARCH result
        // [X-DRAFT-W05-QRESYNC]    draft-ietf-lemonade-reconnect-client-06: IMAP4 Extensions for Quick Mailbox Resynchronization

        boolean authenticated = isAuthenticated();
        String nologin  = mStartedTLS || authenticated || mConfig.allowCleartextLogins()  ? "" : " LOGINDISABLED";
        String starttls = mStartedTLS || authenticated || !extensionEnabled("STARTTLS")   ? "" : " STARTTLS";
        String plain    = !allowCleartextLogins() || authenticated || !extensionEnabled("AUTH=PLAIN") ? "" : " AUTH=PLAIN";
        String gss      = authenticated || !isGssAuthEnabled() ? "" : " AUTH=GSSAPI";
        String zimbra   = authenticated || !extensionEnabled("AUTH=ZIMBRA") ? "" : " AUTH=ZIMBRA";
        StringBuilder capability = new StringBuilder("CAPABILITY IMAP4rev1" + nologin + starttls + plain + gss + zimbra);
        for (String extension : SUPPORTED_EXTENSIONS) {
            if (extensionEnabled(extension))
                capability.append(' ').append(extension);
        }

        return capability.toString();
    }

    // TODO Remove this debugging option for final release
    private static final Boolean GSS_ENABLED = Boolean.getBoolean("ZimbraGssEnabled");

    private boolean isGssAuthEnabled() {
        return (GSS_ENABLED || mConfig.isSaslGssapiEnabled()) && extensionEnabled("AUTH=GSSAPI");
    }

    boolean extensionEnabled(String extension) {
        // check whether the extension is explicitly disabled on the server
        if (mConfig.isExtensionDisabled(extension))
            return false;
        // check whether one of the extension's prerequisites is disabled on the server
        if (extension.equalsIgnoreCase("X-DRAFT-I05-SEARCHRES"))
            return extensionEnabled("ESEARCH");
        if (extension.equalsIgnoreCase("RIGHTS=ektx"))
            return extensionEnabled("ACL");
        if (extension.equalsIgnoreCase("X-DRAFT-W05-QRESYNC"))
            return extensionEnabled("CONDSTORE");
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
        if (attrs != null) {
            String origIp = attrs.get("X-ORIGINATING-IP");
            if (origIp != null) {
                String curOrigRemoteIp = getOrigRemoteIpAddr();
                if (curOrigRemoteIp == null) {
                    setOrigRemoteIpAddr(origIp);
                    ZimbraLog.addOrigIpToContext(origIp);
                    ZimbraLog.imap.info("IMAP client identified as: " + attrs);
                } else {
                    if (curOrigRemoteIp.equals(origIp))
                        ZimbraLog.imap.warn("IMAP ID with X-ORIGINATING-IP is allowed only once per session, command ignored");
                    else
                        ZimbraLog.imap.error("IMAP ID with X-ORIGINATING-IP is allowed only once per session, received different IP: " + origIp + ", command ignored");
                }
           } else
                ZimbraLog.imap.info("IMAP client identified as: " + attrs);
        }

        sendNotifications(true, false);
        sendUntagged("ID (\"NAME\" \"Zimbra\" \"VERSION\" \"" + BuildInfo.VERSION + "\" \"RELEASE\" \"" + BuildInfo.RELEASE + "\")");
        sendOK(tag, "ID completed");
        return CONTINUE_PROCESSING;
    }

    boolean doENABLE(String tag, List<String> extensions) throws IOException, ImapParseException {
        List<ActivatedExtension> targets = new ArrayList<ActivatedExtension>(extensions.size());
        for (String ext : extensions) {
            // draft-gulbrandsen-imap-enable-03 3: "If the argument is not an extension known to
            //                                      the server, the server MUST ignore the argument."
            if (!SUPPORTED_EXTENSIONS.contains(ext) || !extensionEnabled(ext))
                continue;

            if (ext.equals("CONDSTORE")) {
                targets.add(ActivatedExtension.CONDSTORE);
            } else if (ext.equals("X-DRAFT-W05-QRESYNC")) {
                targets.add(ActivatedExtension.CONDSTORE);
                targets.add(ActivatedExtension.QRESYNC);
            } else {
                // draft-gulbrandsen-imap-enable-03 3: "If the argument is an extension known to the server,
                //                                      and it is not specifically permitted to enable it
                //                                      using ENABLE, the server MUST respond with BAD."
                throw new ImapParseException(tag, "non-enableable extension: " + ext);
            }
        }

        // draft-gulbrandsen-imap-enable-03 3: "If the argument is an extension is supported by the
        //                                      server and which needs to be enabled, the server MUST
        //                                      enable the extension for the duration of the connection."
        for (ActivatedExtension ax : targets)
            activateExtension(ax);

        sendNotifications(true, false);
        sendOK(tag, "ENABLE completed");
        return CONTINUE_PROCESSING;
    }

    void activateExtension(ActivatedExtension ext) {
        if (ext == null)
            return;
        if (mActivatedExtensions == null)
            mActivatedExtensions = new HashSet<ActivatedExtension>(1);
        mActivatedExtensions.add(ext);
    }

    boolean sessionActivated(ActivatedExtension ext) {
        return mActivatedExtensions != null && mActivatedExtensions.contains(ext);
    }

    abstract boolean doSTARTTLS(String tag) throws IOException;

    boolean doLOGOUT(String tag) throws IOException {
        sendUntagged(mConfig.getGoodbye());
        mGoodbyeSent = true;
        sendOK(tag, "LOGOUT completed");
        return STOP_PROCESSING;
    }

    boolean doAUTHENTICATE(String tag, String mechanism, byte[] initial) throws IOException {
        if (!checkState(tag, State.NOT_AUTHENTICATED))
            return CONTINUE_PROCESSING;

        AuthenticatorUser authUser = new ImapAuthenticatorUser(this, tag);
        if (PlainAuthenticator.MECHANISM.equals(mechanism) && mechanismEnabled(mechanism)) {
            // RFC 2595 6: "The PLAIN SASL mechanism MUST NOT be advertised or used
            //              unless a strong encryption layer (such as the provided by TLS)
            //              is active or backwards compatibility dictates otherwise."
            if (!allowCleartextLogins()) {
                sendNO(tag, "cleartext logins disabled");
                return CONTINUE_PROCESSING;
            }
            mAuthenticator = new PlainAuthenticator(authUser);
        } else if (GssAuthenticator.MECHANISM.equals(mechanism) && isGssAuthEnabled()) {
            mAuthenticator = new GssAuthenticator(authUser);
        } else if (ZimbraAuthenticator.MECHANISM.equals(mechanism) && mechanismEnabled(mechanism)) {
            mAuthenticator = new ZimbraAuthenticator(authUser);
        } else {
            // no other AUTHENTICATE mechanisms are supported yet
            sendNO(tag, "mechanism not supported");
            return CONTINUE_PROCESSING;
        }
        
        if (!mAuthenticator.initialize()) {
            mAuthenticator = null;
            return CONTINUE_PROCESSING;
        }

        // draft-siemborski-imap-sasl-initial-response:
        //      "This extension adds an optional second argument to the AUTHENTICATE
        //       command that is defined in Section 6.2.2 of [IMAP4].  If this second
        //       argument is present, it represents the contents of the "initial
        //       client response" defined in section 5.1 of [SASL]."
        if (initial != null)
            return continueAuthentication(initial);

        sendContinuation("");
        return CONTINUE_PROCESSING;
    }

    private boolean mechanismEnabled(String mechanism) {
        return extensionEnabled("AUTH=" + mechanism);
    }

    private boolean allowCleartextLogins() {
        return mStartedTLS || mConfig.allowCleartextLogins();
    }
    
    boolean doLOGIN(String tag, String username, String password) throws IOException {
        if (!checkState(tag, State.NOT_AUTHENTICATED))
            return CONTINUE_PROCESSING;

        if (!allowCleartextLogins()) {
            sendNO(tag, "cleartext logins disabled");
            return CONTINUE_PROCESSING;
        }

        boolean cont = authenticate(username, "", password, tag, null);
        if (isAuthenticated()) {
            // 6.2.3: "A server MAY include a CAPABILITY response code in the tagged OK
            //         response of a successful LOGIN command in order to send capabilities
            //         automatically."
            sendOK(tag, '[' + getCapabilityString() + "] LOGIN completed");
            enableInactivityTimer();
        }
        return cont;
    }
    
    boolean authenticate(String username, String authenticateId, String password, String tag, String mechanism)
    throws IOException {
        // the Windows Mobile 5 hacks are enabled by appending "/wm" to the username, etc.
        // TODO For GSSAPI, should enabled hack be applied to authenticateId
        // instead?
        EnabledHack enabledHack = EnabledHack.NONE;
        for (EnabledHack hack : EnabledHack.values()) {
            if (hack.toString() != null && username != null && username.endsWith(hack.toString())) {
                enabledHack = hack;
                username = username.substring(0, username.length() - hack.toString().length());
                break;
            }
        }

        String command = mechanism != null ? "AUTHENTICATE" : "LOGIN";
        try {
            Account acct;
            if (GssAuthenticator.MECHANISM.equals(mechanism))
                acct = AuthenticatorUtil.authenticateKrb5(username, authenticateId);
            else if (ZimbraAuthenticator.MECHANISM.equals(mechanism))
                acct = AuthenticatorUtil.authenticateZToken(username, password);
            else
                acct = AuthenticatorUtil.authenticate(username, authenticateId, password, "imap");
            if (acct == null) {
                sendNO(tag, command + " failed");
                return CONTINUE_PROCESSING;
            }

            ImapCredentials creds = startSession(acct, enabledHack, tag, mechanism);
            if (creds == null)
                return CONTINUE_PROCESSING;
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

        return CONTINUE_PROCESSING;
    }

    private ImapCredentials startSession(Account account, EnabledHack hack, String tag, String mechanism)
    throws ServiceException, IOException {
        String command = mechanism != null ? "AUTHENTICATE" : "LOGIN";
        // make sure we can actually login via IMAP on this host
        if (!account.getBooleanAttr(Provisioning.A_zimbraImapEnabled, false)) {
            sendNO(tag, "account does not have IMAP access enabled");
            return null;
        } else if (!ZimbraAuthenticator.MECHANISM.equals(mechanism) && !Provisioning.onLocalServer(account)) { 
            String correctHost = account.getAttr(Provisioning.A_zimbraMailHost);
            ZimbraLog.imap.info(command + " failed; should be on host " + correctHost);
            if (correctHost == null || correctHost.trim().equals("") || !extensionEnabled("LOGIN_REFERRALS"))
                sendNO(tag, command + " failed [wrong host]");
            else
                sendNO(tag, "[REFERRAL imap://" + URLEncoder.encode(account.getName(), "utf-8") + '@' + correctHost + "/] " + command + " failed");
            return null;
        }

        mCredentials = new ImapCredentials(account, hack);
        if (mCredentials.isLocal())
            mCredentials.getMailbox().beginTrackingImap();
        
        ZimbraLog.addAccountNameToContext(mCredentials.getUsername());
        ZimbraLog.imap.info("user " + mCredentials.getUsername() + " authenticated, mechanism=" + (mechanism == null ? "LOGIN" : mechanism));
        
        return mCredentials;
    }

    private static class QResyncInfo {
        int uvv, modseq;
        String knownUIDs, seqMilestones, uidMilestones;
    }

    boolean doSELECT(String tag, ImapPath path, byte params, QResyncInfo qri) throws IOException, ImapParseException {
        return selectFolder(tag, "SELECT", path, params, qri);
    }

    boolean doEXAMINE(String tag, ImapPath path, byte params, QResyncInfo qri) throws IOException, ImapParseException {
        return selectFolder(tag, "EXAMINE", path, (byte) (params | ImapFolder.SELECT_READONLY), qri);
    }

    private boolean selectFolder(String tag, String command, ImapPath path, byte params, QResyncInfo qri) throws IOException, ImapParseException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        ImapFolder i4folder = null;
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

            ImapFolder i4oldFolder = mSelectedFolder;
            if (i4oldFolder != null && !i4oldFolder.isVirtual() && path.isEquivalent(i4oldFolder.getPath())) {
                try {
                    i4oldFolder.reopen(params);
                    i4folder = i4oldFolder;
                } catch (ServiceException e) {
                    ZimbraLog.imap.warn("error quick-reopening folder " + path + "; proceeding with manual reopen", e);
                }
            }

            if (i4folder == null)
                i4folder = new ImapFolder(path, params, this, mCredentials);

        	writable = i4folder.isWritable();
        	setSelectedFolder(i4folder);

            if (writable) {
                // RFC 4314 5.1.1: "Any server implementing an ACL extension MUST accurately reflect the
                //                  current user's rights in FLAGS and PERMANENTFLAGS responses."
                permflags = i4folder.getFlagList(true);
                if (!path.isWritable(ACL.RIGHT_DELETE))
                    permflags.remove("\\Deleted");
                if (path.belongsTo(mCredentials))
                    permflags.add("\\*");
            }
        } catch (ServiceException e) {
            // 6.3.1: "The SELECT command automatically deselects any currently selected mailbox 
            //         before attempting the new selection.  Consequently, if a mailbox is selected
            //         and a SELECT command that fails is attempted, no mailbox is selected."
            unsetSelectedFolder();

            if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER))
                ZimbraLog.imap.info(command + " failed: no such folder: " + path);
            else if (e.getCode().equals(ServiceException.PERM_DENIED))
                ZimbraLog.imap.info(command + " failed: permission denied: " + path);
            else
                ZimbraLog.imap.warn(command + " failed", e);
            sendNO(tag, command + " failed");
            return canContinue(e);
        }

        // note: not sending back a "* OK [UIDNEXT ....]" response for search folders
        //    6.3.1: "If this is missing, the client can not make any assumptions about the
        //            next unique identifier value."
        sendUntagged(i4folder.getSize() + " EXISTS");
        sendUntagged(i4folder.getRecentCount() + " RECENT");
        if (i4folder.getFirstUnread() > 0)
        	sendUntagged("OK [UNSEEN " + i4folder.getFirstUnread() + "] mailbox contains unseen messages");
        sendUntagged("OK [UIDVALIDITY " + i4folder.getUIDValidity() + "] UIDs are valid for this mailbox");
        if (!i4folder.isVirtual())
            sendUntagged("OK [UIDNEXT " + i4folder.getInitialUIDNEXT() + "] next expected UID is " + i4folder.getInitialUIDNEXT());
        sendUntagged("FLAGS (" + StringUtil.join(" ", i4folder.getFlagList(false)) + ')');
        sendUntagged("OK [PERMANENTFLAGS (" + StringUtil.join(" ", permflags) + ")] junk-related flags are not permanent");
        if (!i4folder.isVirtual())
            sendUntagged("OK [HIGHESTMODSEQ " + i4folder.getInitialMODSEQ() + "] modseq tracked on this mailbox");
        else
            sendUntagged("OK [NOMODSEQ] modseq not supported on search folders");

        // handle any QRESYNC stuff if the UVVs match
        if (qri != null && qri.uvv == i4folder.getUIDValidity() && !i4folder.isVirtual()) {
            boolean sentVanished = false;
            String knownUIDs = qri.knownUIDs == null ? "1:" + (i4folder.getInitialUIDNEXT() - 1) : qri.knownUIDs;
            if (qri.seqMilestones != null && qri.uidMilestones != null) {
                int lowwater = 1;
                ImapMessageSet seqset = i4folder.getSubsequence(tag, qri.seqMilestones, false);
                ImapMessageSet uidset = i4folder.getSubsequence(tag, qri.uidMilestones, true);
                seqset.remove(null);  uidset.remove(null);
                for (Iterator<ImapMessage> itseq = seqset.iterator(), ituid = uidset.iterator(); itseq.hasNext() && ituid.hasNext(); ) {
                    ImapMessage i4msg;
                    if ((i4msg = itseq.next()) != ituid.next())
                        break;
                    lowwater = i4msg.imapUid + 1;
                }
                if (lowwater > 1) {
                    String constrainedSet = i4folder.cropSubsequence(knownUIDs, true, lowwater, -1);
                    String vanished = i4folder.invertSubsequence(constrainedSet, true, i4folder.getAllMessages());
                    if (!vanished.equals(""))
                        sendUntagged("VANISHED (EARLIER) " + vanished);
                    sentVanished = true;
                }
            }
            fetch(tag, knownUIDs, FETCH_FLAGS | (sentVanished ? 0 : FETCH_VANISHED), null, true, qri.modseq, false);
        }

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
                ((Mailbox) mboxobj).createFolder(getContext(), path.asResolvedPath(), (byte) 0, MailItem.TYPE_MESSAGE);
            } else if (mboxobj instanceof ZMailbox) {
                ((ZMailbox) mboxobj).createFolder(null, path.asResolvedPath(), ZFolder.View.message, ZFolder.Color.defaultColor, null, null);
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
            if (cause.equals("CREATE failed"))
                ZimbraLog.imap.warn(cause, e);
            else
                ZimbraLog.imap.info(cause);
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
            if (!path.isVisible())
                throw ImapServiceException.FOLDER_NOT_VISIBLE(path.asImapPath());

            Object mboxobj = path.getOwnerMailbox();
            if (mboxobj instanceof Mailbox) {
                Mailbox mbox = (Mailbox) mboxobj;
                Folder folder = (Folder) path.getFolder();
                if (!folder.isDeletable())
                    throw ImapServiceException.CANNOT_DELETE_SYSTEM_FOLDER(folder.getPath());

                // don't want the DELETE to cause *this* connection to drop if the deleted folder is currently selected
                if (mSelectedFolder != null && path.isEquivalent(mSelectedFolder.getPath()))
                    unsetSelectedFolder();

                if (!folder.hasSubfolders()) {
                    mbox.delete(getContext(), folder.getId(), MailItem.TYPE_FOLDER);
                    // deleting the folder also unsubscribes from it...
                    mCredentials.unsubscribe(path);
                } else {
                    // 6.3.4: "It is permitted to delete a name that has inferior hierarchical
                    //         names and does not have the \Noselect mailbox name attribute.
                    //         In this case, all messages in that mailbox are removed, and the
                    //         name will acquire the \Noselect mailbox name attribute."
                    mbox.emptyFolder(getContext(), folder.getId(), false);
                    // FIXME: add \Deleted flag to folder
                }
            } else if (mboxobj instanceof ZMailbox) {
                ZMailbox zmbx = (ZMailbox) mboxobj;
                ZFolder zfolder = (ZFolder) path.getFolder();

                if (zfolder.getSubFolders().isEmpty()) {
                    zmbx.deleteFolder(zfolder.getId());
                    // deleting the folder also unsubscribes from it...
                    mCredentials.unsubscribe(path);
                } else {
                    zmbx.emptyFolder(zfolder.getId(), false);
                }
            } else {
                throw AccountServiceException.NO_SUCH_ACCOUNT(path.getOwner());
            }
        } catch (ServiceException e) {
            if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER))
                ZimbraLog.imap.info("DELETE failed: no such folder: " + path);
            else if (e.getCode().equals(AccountServiceException.NO_SUCH_ACCOUNT))
                ZimbraLog.imap.info("DELETE failed: no such account: " + path);
            else if (e.getCode().equals(ImapServiceException.FOLDER_NOT_VISIBLE))
                ZimbraLog.imap.info("DELETE failed: folder not visible: " + path);
            else if (e.getCode().equals(ImapServiceException.CANT_DELETE_SYSTEM_FOLDER))
                ZimbraLog.imap.info("DELETE failed: system folder cannot be deleted: " + path);
            else if (e.getCode().equals(ServiceException.PERM_DENIED))
                ZimbraLog.imap.info("DELETE failed: permission denied: " + path);
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
            } else if (!newPath.isCreatable()) {
                ZimbraLog.imap.info("RENAME failed: hidden folder or parent: " + newPath);
                sendNO(tag, "RENAME failed");
                return CONTINUE_PROCESSING;
            } else if (!oldPath.isVisible()) {
                throw MailServiceException.NO_SUCH_FOLDER(oldPath.asZimbraPath());
            }

            Object mboxobj = oldPath.getOwnerMailbox();
            if (mboxobj instanceof Mailbox) {
                int folderId = oldPath.asItemId().getId();
                if (folderId == Mailbox.ID_FOLDER_INBOX)
                    throw ImapServiceException.CANT_RENAME_INBOX();
                Mailbox mbox = (Mailbox) mboxobj;
                mbox.rename(getContext(), folderId, MailItem.TYPE_FOLDER, "/" + newPath.asResolvedPath());
            } else if (mboxobj instanceof ZMailbox) {
                if (oldPath.asItemId().getId() == Mailbox.ID_FOLDER_INBOX)
                    throw ImapServiceException.CANT_RENAME_INBOX();
                ZMailbox zmbx = (ZMailbox) mboxobj;
                ZFolder zfolder = (ZFolder) oldPath.getFolder();
                zmbx.renameFolder(zfolder.getId(), newPath.asResolvedPath());
            } else {
                ZimbraLog.imap.info("RENAME failed: cannot get mailbox for path: " + oldPath);
                sendNO(tag, "RENAME failed");
                return CONTINUE_PROCESSING;
            }
        } catch (ServiceException e) {
        	if (e.getCode().equals(ImapServiceException.CANT_RENAME_INBOX)) {
        		ZimbraLog.imap.info("RENAME failed: RENAME of INBOX not supported");
        		sendNO(tag, "RENAME failed: RENAME of INBOX not supported");
        		return CONTINUE_PROCESSING;
            } else if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER)) {
                ZimbraLog.imap.info("RENAME failed: no such folder: " + oldPath);
            } else if (e.getCode().equals(MailServiceException.IMMUTABLE_OBJECT)) {
                ZimbraLog.imap.info("RENAME failed: cannot rename system folder: " + oldPath);
            } else if (e.getCode().equals(MailServiceException.CANNOT_CONTAIN)) {
                ZimbraLog.imap.info("RENAME failed: invalid target folder: " + newPath);
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
            // canonicalizing the path also throws exceptions when the folder doesn't exist
            path.canonicalize();

            if (path.belongsTo(mCredentials)) {
                if (!path.isVisible())
                    throw ImapServiceException.FOLDER_NOT_VISIBLE(path.asImapPath());
                Mailbox mbox = (Mailbox) path.getOwnerMailbox();
                Folder folder = (Folder) path.getFolder();
                if (!folder.isTagged(mbox.mSubscribedFlag))
                    mbox.alterTag(getContext(), folder.getId(), MailItem.TYPE_FOLDER, Flag.ID_FLAG_SUBSCRIBED, true);
            } else {
                mCredentials.subscribe(path);
            }
        } catch (ServiceException e) {
            if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER))
                ZimbraLog.imap.info("SUBSCRIBE failed: no such folder: " + path);
            else if (e.getCode().equals(ServiceException.PERM_DENIED))
                ZimbraLog.imap.info("SUBSCRIBE failed: permission denied on folder: " + path);
            else if (e.getCode().equals(ImapServiceException.FOLDER_NOT_VISIBLE))
                ZimbraLog.imap.info("SUBSCRIBE failed: folder not visible: " + path);
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
            if (path.belongsTo(mCredentials)) {
                try {
                    Mailbox mbox = mCredentials.getMailbox();
                    Folder folder = (Folder) path.getFolder();
                    if (folder.isTagged(mbox.mSubscribedFlag))
                        mbox.alterTag(getContext(), folder.getId(), MailItem.TYPE_FOLDER, Flag.ID_FLAG_SUBSCRIBED, false);
                } catch (NoSuchItemException nsie) { }
            }

            // always check for remote subscriptions -- the path might be an old mountpoint...
            mCredentials.unsubscribe(path);
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

    private static final byte SELECT_SUBSCRIBED = 0x01;
    private static final byte SELECT_REMOTE     = 0x02;
    private static final byte SELECT_RECURSIVE  = 0x04;

    private static final byte RETURN_SUBSCRIBED = 0x01;
    private static final byte RETURN_CHILDREN   = 0x02;

    boolean doLIST(String tag, String referenceName, Set<String> mailboxNames, byte selectOptions, byte returnOptions) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        if (selectOptions == 0 && returnOptions == 0 && mailboxNames.size() == 1 && mailboxNames.contains("")) {
            // 6.3.8: "An empty ("" string) mailbox name argument is a special request to return
            //         the hierarchy delimiter and the root name of the name given in the reference."
            sendUntagged("LIST (\\NoSelect) \"/\" \"\"");
            sendOK(tag, "LIST completed");
            return CONTINUE_PROCESSING;
        }

        // LIST-EXTENDED 4: "The CHILDREN return option is simply an indication that the client
        //                   wants this information; a server MAY provide it even if the option is
        //                   not specified."
        if (extensionEnabled("CHILDREN"))
            returnOptions |= RETURN_CHILDREN;

        // LIST-EXTENDED 3.1: "Note that the SUBSCRIBED selection option implies the SUBSCRIBED
        //                     return option (see below)."
        boolean selectSubscribed = (selectOptions & SELECT_SUBSCRIBED) != 0;
        if (selectSubscribed)
            returnOptions |= RETURN_SUBSCRIBED;
        boolean returnSubscribed = (returnOptions & RETURN_SUBSCRIBED) != 0;
        Set<String> remoteSubscriptions = null;

        boolean selectRecursive = (selectOptions & SELECT_RECURSIVE) != 0;

        Map<ImapPath, String> matches = new LinkedHashMap<ImapPath, String>();
        try {
            for (String mailboxName : mailboxNames) {
                // LIST-EXTENDED 3: "In particular, if an extended LIST command has multiple mailbox
                //                   names and one (or more) of them is the empty string, the empty
                //                   string MUST be ignored for the purpose of matching."
                if (mailboxName.equals(""))
                    continue;

                Pair<String, Pattern> resolved = resolvePath(referenceName, mailboxName);
                String resolvedPath = resolved.getFirst();
                Pattern pattern = resolved.getSecond();

                ImapPath patternPath = new ImapPath(resolvedPath, mCredentials, ImapPath.Scope.UNPARSED);
                String owner = patternPath.getOwner();

                if (owner != null && (owner.indexOf('*') != -1 || owner.indexOf('%') != -1)) {
                    // RFC 2342 5: "Alternatively, a server MAY return NO to such a LIST command,
                    //              requiring that a user name be included with the Other Users'
                    //              Namespace prefix before listing any other user's mailboxes."
                    ZimbraLog.imap.info("LIST failed: wildcards not permitted in username " + patternPath);
                    sendNO(tag, "LIST failed: wildcards not permitted in username");
                    return CONTINUE_PROCESSING;
                }

                // you cannot access your own mailbox via the /home/username mechanism
                if (owner != null && patternPath.belongsTo(mCredentials))
                    continue;

                // make sure we can do a  LIST "" "/home/user1"
                if (owner != null && (ImapPath.NAMESPACE_PREFIX + owner).toUpperCase().equals(resolvedPath)) {
                    matches.put(patternPath, "LIST (\\NoSelect) \"/\" " + patternPath.asUtf7String());
                    continue;
                }

                // if there's no matching account, skip this pattern
                Account acct = patternPath.getOwnerAccount();
                if (acct == null)
                    continue;

                if (returnSubscribed)
                    remoteSubscriptions = mCredentials.listSubscriptions();

                // get the set of *all* folders; we'll iterate over it below to find matches
                Map<ImapPath, ItemId> paths = new LinkedHashMap<ImapPath, ItemId>();
                accumulatePaths(patternPath.getOwnerMailbox(), owner, null, paths);

                // get the set of folders matching the selection criteria (either all folders or selected folders)
                Set<ImapPath> selected = paths.keySet();
                if (selectSubscribed) {
                    selected = new LinkedHashSet<ImapPath>();
                    for (ImapPath path : paths.keySet()) {
                        if (isPathSubscribed(path, remoteSubscriptions))
                            selected.add(path);
                    }
                    // handle nonexistent selected folders by adding them to "selected" but not to "paths"
                    if (remoteSubscriptions != null) {
                        for (String sub : remoteSubscriptions) {
                            ImapPath spath = new ImapPath(sub, mCredentials);
                            if (!selected.contains(spath) && (owner == null) == (spath.getOwner() == null))
                                selected.add(spath);
                        }
                    }
                }

                // return only the selected folders (and perhaps their parents) matching the pattern
                for (ImapPath path : selected) {
                    if (!matches.containsKey(path) && pathMatches(path, pattern))
                        matches.put(path, "LIST (" + getFolderAttrs(path, returnOptions, paths, remoteSubscriptions) + ") \"/\" " + path.asUtf7String());

                    if (!selectRecursive)
                        continue;
                    String folderName = path.asZimbraPath();
                    for (int index = folderName.length() + 1; (index = folderName.lastIndexOf('/', index - 1)) != -1; ) {
                        ImapPath parent = new ImapPath(path.getOwner(), folderName.substring(0, index), mCredentials);
                        if (pathMatches(parent, pattern)) {
                            // use the already-resolved version of the parent ImapPath from the "paths" map if possible
                            for (ImapPath cached : paths.keySet()) {
                                if (cached.equals(parent)) {
                                    parent = cached;  break;
                                }
                            }
                            matches.put(parent, "LIST (" + getFolderAttrs(parent, returnOptions, paths, remoteSubscriptions) + ") \"/\" " + parent.asUtf7String() + " (\"CHILDINFO\" (\"SUBSCRIBED\"))");
                        }
                    }
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("LIST failed", e);
            sendNO(tag, "LIST failed");
            return canContinue(e);
        }

        if (matches != null) {
        	for (String match : matches.values())
        		sendUntagged(match);
        }

        sendNotifications(true, false);
        sendOK(tag, "LIST completed");
        return CONTINUE_PROCESSING;
    }

    private static final boolean[] REGEXP_ESCAPED = new boolean[128];
        static {
            REGEXP_ESCAPED['('] = REGEXP_ESCAPED[')'] = REGEXP_ESCAPED['.'] = true;
            REGEXP_ESCAPED['['] = REGEXP_ESCAPED[']'] = REGEXP_ESCAPED['|'] = true;
            REGEXP_ESCAPED['^'] = REGEXP_ESCAPED['$'] = REGEXP_ESCAPED['?'] = true;
            REGEXP_ESCAPED['{'] = REGEXP_ESCAPED['}'] = REGEXP_ESCAPED['*'] = true;
            REGEXP_ESCAPED['\\'] = true;
        }

    private static Pair<String, Pattern> resolvePath(String referenceName, String mailboxName) {
        int startWildcards = referenceName.length();
        String resolved = mailboxName;
        if (!mailboxName.startsWith("/") && !referenceName.trim().equals("")) {
            if (referenceName.endsWith("/"))  resolved = referenceName + mailboxName;
            else                              resolved = referenceName + '/' + mailboxName;
        } else {
            startWildcards = 0;
        }

        String unescaped = resolved.toUpperCase();
        StringBuffer escaped = new StringBuffer();
        for (int i = 0; i < unescaped.length(); i++) {
            char c = unescaped.charAt(i);
            // 6.3.8: "The character "*" is a wildcard, and matches zero or more characters at this position.
            //         The character "%" is similar to "*", but it does not match a hierarchy delimiter."
            if (c == '*' && i >= startWildcards)       escaped.append(".*");
            else if (c == '%' && i >= startWildcards)  escaped.append("[^/]*");
            else if (c > 0x7f || !REGEXP_ESCAPED[c])   escaped.append(c);
            else                                       escaped.append('\\').append(c);
        }

        return new Pair<String, Pattern>(resolved, Pattern.compile(escaped.toString()));
    }

    private void accumulatePaths(Object mboxobj, String owner, ImapPath relativeTo, Map<ImapPath, ItemId> paths) throws ServiceException {
        String root = relativeTo == null ? "" : "/" + relativeTo.asResolvedPath();
        if (mboxobj instanceof Mailbox) {
            Mailbox mbox = (Mailbox) mboxobj;
            Collection<Folder> folders = mbox.getVisibleFolders(getContext());
            if (folders == null)
                folders = mbox.getFolderById(getContext(), relativeTo == null ? Mailbox.ID_FOLDER_USER_ROOT : relativeTo.asItemId().getId()).getSubfolderHierarchy();
            for (Folder folder : folders) {
                if (!folder.getPath().startsWith(root) || folder.getPath().equals(root))
                    continue;
                ImapPath path = relativeTo == null ? new ImapPath(owner, folder, mCredentials) : new ImapPath(owner, folder, relativeTo);
                if (path.isVisible()) {
                    paths.put(path, path.asItemId());
                    if (folder instanceof Mountpoint)
                        accumulatePaths(path.getOwnerMailbox(), owner, path, paths);
                }
            }
        } else if (mboxobj instanceof ZMailbox) {
            ZMailbox zmbx = (ZMailbox) mboxobj;
            for (ZFolder zfolder : zmbx.getAllFolders()) {
                if (!zfolder.getPath().startsWith(root) || zfolder.getPath().equals(root))
                    continue;
                ImapPath path = relativeTo == null ? new ImapPath(owner, zmbx, zfolder, mCredentials) : new ImapPath(owner, zmbx, zfolder, relativeTo);
                if (path.isVisible())
                    paths.put(path, path.asItemId());
            }
        }
    }

    private static boolean pathMatches(ImapPath path, Pattern pattern) {
        return pattern.matcher(path.asImapPath().toUpperCase()).matches();
    }

    private String getFolderAttrs(ImapPath path, byte returnOptions, Map<ImapPath, ItemId> paths, Set<String> subscriptions)
    throws ServiceException {
        StringBuilder attrs = new StringBuilder();

        ItemId iid = paths.get(path);
        if (iid == null)
            attrs.append(attrs.length() == 0 ? "" : " ").append("\\NonExistent");

        if ((returnOptions & RETURN_SUBSCRIBED) != 0 && isPathSubscribed(path, subscriptions))
            attrs.append(attrs.length() == 0 ? "" : " ").append("\\Subscribed");

        if (iid == null)
            return attrs.toString();

        boolean noinferiors = (iid.getId() == Mailbox.ID_FOLDER_SPAM);
        if (noinferiors)
            attrs.append(attrs.length() == 0 ? "" : " ").append("\\NoInferiors");

        if (!path.isSelectable())
            attrs.append(attrs.length() == 0 ? "" : " ").append("\\NoSelect");

        if (!noinferiors && (returnOptions & RETURN_CHILDREN) != 0) {
            String prefix = path.asZimbraPath().toUpperCase() + '/';
            boolean children = false;
            for (ImapPath other : paths.keySet()) {
                if (other.asZimbraPath().toUpperCase().startsWith(prefix) && other.isVisible()) {
                    children = true;  break;
                }
            }
            attrs.append(attrs.length() == 0 ? "" : " ").append(children ? "\\HasChildren" : "\\HasNoChildren");
        }

        return attrs.toString();
    }

    private boolean isPathSubscribed(ImapPath path, Set<String> subscriptions) throws ServiceException {
        if (path.belongsTo(mCredentials)) {
            Folder folder = (Folder) path.getFolder();
            return folder.isTagged(folder.getMailbox().mSubscribedFlag);
        } else if (subscriptions != null && !subscriptions.isEmpty()) {
            for (String sub : subscriptions) {
                if (sub.equalsIgnoreCase(path.asImapPath()))
                    return true;
            }
        }
        return false;
    }

    boolean doLSUB(String tag, String referenceName, String mailboxName) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        Pair<String, Pattern> resolved = resolvePath(referenceName, mailboxName);
        String resolvedPath = resolved.getFirst();
        Pattern pattern = resolved.getSecond();
        Pattern childPattern = Pattern.compile(pattern.pattern() + "/.*");

        ImapPath patternPath = new ImapPath(resolvedPath, mCredentials, ImapPath.Scope.UNPARSED);

        List<String> subscriptions = null;
        try {
            // you cannot access your own mailbox via the /home/username mechanism
            if (patternPath.getOwner() == null || !patternPath.belongsTo(mCredentials)) {
                Map<ImapPath, Boolean> hits = new HashMap<ImapPath, Boolean>();

                if (patternPath.getOwner() == null) {
                    Mailbox mbox = mCredentials.getMailbox();
                    for (Folder folder : mbox.getFolderById(getContext(), Mailbox.ID_FOLDER_USER_ROOT).getSubfolderHierarchy()) {
                        if (folder.isTagged(mbox.mSubscribedFlag))
                            checkSubscription(new ImapPath(null, folder, mCredentials), pattern, childPattern, hits);
                    }
                }

                Set<String> remoteSubscriptions = mCredentials.listSubscriptions();
                if (remoteSubscriptions != null && !remoteSubscriptions.isEmpty()) {
                    String owner = patternPath.getOwner();
                    for (String sub : remoteSubscriptions) {
                        ImapPath subscribed = new ImapPath(sub, mCredentials);
                        if ((owner == null) == (subscribed.getOwner() == null))
                            checkSubscription(subscribed, pattern, childPattern, hits);
                    }
                }

                subscriptions = new ArrayList<String>(hits.size());
                for (Map.Entry<ImapPath, Boolean> hit : hits.entrySet()) {
                    String attrs = hit.getValue() ? "" : "\\NoSelect";
                    subscriptions.add("LSUB (" + attrs + ") \"/\" " + hit.getKey().asUtf7String());
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("LSUB failed", e);
            sendNO(tag, "LSUB failed");
            return canContinue(e);
        }

        if (subscriptions != null) {
            for (String sub : subscriptions)
                sendUntagged(sub);
        }

        sendNotifications(true, false);
        sendOK(tag, "LSUB completed");
        return CONTINUE_PROCESSING;
    }

    private boolean checkSubscription(ImapPath path, Pattern pattern, Pattern childPattern, Map<ImapPath, Boolean> hits) {
        if (pathMatches(path, pattern)) {
            hits.put(path, Boolean.TRUE);  return true;
        } else if (!pathMatches(path, childPattern)) {
            return false;
        }

        // 6.3.9: "A special situation occurs when using LSUB with the % wildcard. Consider 
        //         what happens if "foo/bar" (with a hierarchy delimiter of "/") is subscribed
        //         but "foo" is not.  A "%" wildcard to LSUB must return foo, not foo/bar, in
        //         the LSUB response, and it MUST be flagged with the \Noselect attribute."

        // figure out the set of unsubscribed mailboxes that match the pattern and are parents of subscribed mailboxes
        boolean matched = false;
        int delimiter = path.asImapPath().lastIndexOf('/');
        while (delimiter > 0) {
            path = new ImapPath(path.asImapPath().substring(0, delimiter), mCredentials);
            if (!hits.containsKey(path) && pathMatches(path, pattern)) {
                hits.put(path, Boolean.FALSE);  matched = true;
            }
            delimiter = path.asImapPath().lastIndexOf('/');
        }
        return matched;
    }

    static final int STATUS_MESSAGES      = 0x01;
    static final int STATUS_RECENT        = 0x02;
    static final int STATUS_UIDNEXT       = 0x04;
    static final int STATUS_UIDVALIDITY   = 0x08;
    static final int STATUS_UNSEEN        = 0x10;
    static final int STATUS_HIGHESTMODSEQ = 0x20;

    boolean doSTATUS(String tag, ImapPath path, int status) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        StringBuilder data = new StringBuilder();
        try {
            path.canonicalize();
            if (!path.isVisible()) {
                ZimbraLog.imap.info("STATUS failed: folder not visible: " + path);
                sendNO(tag, "STATUS failed");
                return CONTINUE_PROCESSING;
            }

            int messages, recent, uidnext, uvv, unread, modseq;
            Object mboxobj = path.getOwnerMailbox();
            if (mboxobj instanceof Mailbox) {
                Mailbox mbox = (Mailbox) mboxobj;
                Folder folder = (Folder) path.getFolder();

                messages = (int) folder.getItemCount();
                if ((status & STATUS_RECENT) == 0)
                    recent = -1;
                else if (messages == 0)
                    recent = 0;
                else if (mSelectedFolder != null && path.isEquivalent(mSelectedFolder.getPath()))
                    recent = mSelectedFolder.getRecentCount();
                else
                    recent = mbox.countImapRecent(getContext(), folder.getId());
                uidnext = folder instanceof SearchFolder ? -1 : folder.getImapUIDNEXT();
                uvv = ImapFolder.getUIDValidity(folder);
                unread = folder.getUnreadCount();
                modseq = folder instanceof SearchFolder ? 0 : folder.getImapMODSEQ();
            } else if (mboxobj instanceof ZMailbox) {
                ZFolder zfolder = (ZFolder) path.getFolder();
                if (zfolder == null)
                    throw MailServiceException.NO_SUCH_FOLDER(path.asImapPath());

                messages = zfolder.getMessageCount();
                recent = 0;
                uidnext = -1;
                uvv = ImapFolder.getUIDValidity(zfolder);
                unread = zfolder.getUnreadCount();
                modseq = -1;
            } else {
                throw AccountServiceException.NO_SUCH_ACCOUNT(path.getOwner());
            }

            if (messages >= 0 && (status & STATUS_MESSAGES) != 0)
                data.append(data.length() > 0 ? " " : "").append("MESSAGES ").append(messages);
            if (recent >= 0 && (status & STATUS_RECENT) != 0)
                data.append(data.length() > 0 ? " " : "").append("RECENT ").append(recent);
            // note: we're not supporting UIDNEXT for search folders; see the comments in selectFolder()
            if (uidnext > 0 && (status & STATUS_UIDNEXT) != 0)
                data.append(data.length() > 0 ? " " : "").append("UIDNEXT ").append(uidnext);
            if (uvv > 0 && (status & STATUS_UIDVALIDITY) != 0)
                data.append(data.length() > 0 ? " " : "").append("UIDVALIDITY ").append(uvv);
            if (unread >= 0 && (status & STATUS_UNSEEN) != 0)
                data.append(data.length() > 0 ? " " : "").append("UNSEEN ").append(unread);
            if (modseq >= 0 && (status & STATUS_HIGHESTMODSEQ) != 0)
                data.append(data.length() > 0 ? " " : "").append("HIGHESTMODSEQ ").append(modseq);
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

    private static class Append {
        Date mDate;

        List<Object> mParts;
        byte[] mContent;

        List<String> mFlagNames;
        int flags = Flag.BITMASK_UNREAD;
        long tags = 0;
        short sflags = 0;

        Append(List<String> flagNames, Date date, byte[] content) {
            mFlagNames = flagNames;  mDate = date;  mContent = content;
        }
        Append(List<String> flagNames, Date date, List<Object> parts) {
            mFlagNames = flagNames;  mDate = date;  mParts = parts;
        }
    }

    boolean doAPPEND(String tag, ImapPath path, List<Append> appends) throws IOException, ImapParseException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        List<Tag> newTags = new ArrayList<Tag>();
        List<Integer> createdIds = new ArrayList<Integer>(appends.size());
        StringBuilder appendHint = extensionEnabled("UIDPLUS") ? new StringBuilder() : null;
        try {
            if (!path.isVisible())
                throw ImapServiceException.FOLDER_NOT_VISIBLE(path.asImapPath());
            else if (!path.isWritable(ACL.RIGHT_INSERT))
                throw ImapServiceException.FOLDER_NOT_WRITABLE(path.asImapPath());

            Object mboxobj = path.getOwnerMailbox();
            Object folderobj = path.getFolder();

            synchronized (mboxobj) {
                ImapFlagCache flagset = ImapFlagCache.getSystemFlags(mboxobj instanceof Mailbox ? (Mailbox) mboxobj : mCredentials.getMailbox());
                ImapFlagCache tagset = mboxobj instanceof Mailbox ? new ImapFlagCache((Mailbox) mboxobj, getContext()) : new ImapFlagCache();

                for (Append append : appends) {
                    if (append.mFlagNames != null && !append.mFlagNames.isEmpty()) {
                        for (String name : append.mFlagNames) {
                            ImapFlag i4flag = flagset.getByName(name);
                            if (i4flag != null && !i4flag.mListed)
                                i4flag = null;
                            else if (i4flag == null && !name.startsWith("\\"))
                                i4flag = tagset.getByName(name);
                            if (i4flag == null)
                                i4flag = tagset.createTag(getContext(), name, newTags);

                            if (i4flag != null) {
                                if (!i4flag.mPermanent)               append.sflags |= i4flag.mBitmask;
                                else if (Tag.validateId(i4flag.mId))  append.tags |= i4flag.mBitmask;
                                else if (i4flag.mPositive)            append.flags |= i4flag.mBitmask;
                                else                                  append.flags &= ~i4flag.mBitmask;
                            }
                        }
                    }
                    append.mFlagNames = null;
                }
            }

            for (Append append : appends) {
                if (append.mContent == null) {
                    // translate CATENATE (...) directives into the resulting byte array
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int size = 0;
                    for (Object part : append.mParts) {
                        byte[] buffer;
                        if (part instanceof byte[])
                            buffer = (byte[]) part;
                        else
                            buffer = ((ImapURL) part).getContent(this, mCredentials, tag);

                        size += buffer.length;
                        if (size > mConfig.getMaxRequestSize())
                            throw new ImapParseException(tag, "TOOBIG", "request too long", false);
                        baos.write(buffer);
                    }
                    append.mContent = baos.toByteArray();
                    append.mParts = null;
                }

                // if we're using Thunderbird, try to set INTERNALDATE to the message's Date: header
                if (append.mDate == null && mCredentials.isHackEnabled(EnabledHack.THUNDERBIRD)) {
                    try {
                        // inefficient, but must be done before creating the ParsedMessage
                        append.mDate = new MimeMessage(JMSession.getSession(), new ByteArrayInputStream(append.mContent)).getSentDate();
                    } catch (MessagingException e) { }
                }

                // server uses UNIX time, so range-check specified date (is there a better place for this?)
                if (append.mDate != null && append.mDate.getTime() > Integer.MAX_VALUE * 1000L) {
                    ZimbraLog.imap.info("APPEND failed: date out of range");
                    sendNO(tag, "APPEND failed: date out of range");
                    return CONTINUE_PROCESSING;
                }

                if (mboxobj instanceof Mailbox) {
                    int id = append((Mailbox) mboxobj, (Folder) folderobj, append);
                    if (id > 0)
                        createdIds.add(id);
                } else {
                    ZMailbox zmbx = (ZMailbox) mboxobj;
                    ZFolder zfolder = (ZFolder) folderobj;

                    String id = zmbx.addMessage(zfolder.getId(), Flag.bitmaskToFlags(append.flags), null, append.mDate.getTime(), append.mContent, true);
                    createdIds.add(new ItemId(id, mCredentials.getAccountId()).getId());
                }
                append.mContent = null;
            }

            int uvv = (folderobj instanceof Folder ? ImapFolder.getUIDValidity((Folder) folderobj) : ImapFolder.getUIDValidity((ZFolder) folderobj));
            if (appendHint != null && uvv > 0)
                appendHint.append("[APPENDUID ").append(uvv).append(' ').append(ImapFolder.encodeSubsequence(createdIds)).append("] ");
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
            } else if (e.getCode().equals(MailServiceException.INVALID_NAME)) {
                ZimbraLog.imap.info("APPEND failed: " + e.getMessage());
            } else if (e.getCode().equals(ImapServiceException.FOLDER_NOT_VISIBLE)) {
                ZimbraLog.imap.info("APPEND failed: folder not visible: " + path);
            } else if (e.getCode().equals(ImapServiceException.FOLDER_NOT_WRITABLE)) {
                ZimbraLog.imap.info("APPEND failed: folder not writable: " + path);
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

    int append(Mailbox mbox, Folder folder, Append append) throws ServiceException {
        synchronized (mbox) {
            try {
                boolean idxAttach = mbox.attachmentsIndexingEnabled();
                ParsedMessage pm = append.mDate != null ? new ParsedMessage(append.mContent, append.mDate.getTime(), idxAttach) :
                                                          new ParsedMessage(append.mContent, idxAttach);
                try {
                    pm.analyze();
                } catch (ServiceException e) {
                    ZimbraLog.imap.warn("could not completely extract text from APPENDed message; continuing", e);
                }

                try {
                    if (!pm.getSender().equals("")) {
                        InternetAddress ia = new InternetAddress(pm.getSender());
                        if (AccountUtil.addressMatchesAccount(mbox.getAccount(), ia.getAddress()))
                            append.flags |= Flag.BITMASK_FROM_ME;
                    }
                } catch (Exception e) { }

                Message msg = mbox.addMessage(getContext(), pm, folder.getId(), true, append.flags, Tag.bitmaskToTags(append.tags));
                if (msg != null && append.sflags != 0 && getState() == ImapHandler.State.SELECTED) {
                    ImapMessage i4msg = getSelectedFolder().getById(msg.getId());
                    if (i4msg != null)
                        i4msg.setSessionFlags(append.sflags, getSelectedFolder());
                }
                return msg == null ? -1 : msg.getId();
            } catch (IOException e) {
                throw ServiceException.FAILURE(e.toString(), e);
            } catch (MessagingException e) {
                throw ServiceException.FAILURE(e.toString(), e);
            }
        }
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
            sendContinuation("idling");
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
            if (!qroot.belongsTo(mCredentials)) {
                ZimbraLog.imap.info("GETQUOTA failed: cannot get quota for other user's mailbox: " + qroot);
                sendNO(tag, "GETQUOTA failed: permission denied");
                return CONTINUE_PROCESSING;
            }

            long quota = mCredentials.getAccount().getLongAttr(Provisioning.A_zimbraMailQuota, 0);
            if (qroot == null || !qroot.asImapPath().equals("") || quota <= 0) {
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
            if (!qroot.belongsTo(mCredentials)) {
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
            long quota = mCredentials.getAccount().getLongAttr(Provisioning.A_zimbraMailQuota, 0);
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

    private static final String IMAP_READ_RIGHTS   = "lr";
    private static final String IMAP_WRITE_RIGHTS  = "sw";
    private static final String IMAP_INSERT_RIGHTS = "ick";
    private static final String IMAP_DELETE_RIGHTS = "xted";
    private static final String IMAP_ADMIN_RIGHTS  = "a";

    /** Returns whether all of a set of <tt>linked</tt> RFC 4314 rights is
     *  contained within a string. */
    private boolean allRightsPresent(final String i4rights, final String linked) {
        for (int i = 0; i < linked.length(); i++)
            if (i4rights.indexOf(linked.charAt(i)) == -1)
                return false;
        return true;
    }

    boolean doSETACL(String tag, ImapPath path, String principal, String i4rights, StoreAction action) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        // RFC 4314 2: "If rights are tied in an implementation, the implementation must be
        //              conservative in granting rights in response to SETACL commands--unless
        //              all rights in a tied set are specified, none of that set should be
        //              included in the ACL entry for that identifier."
        short rights = 0;
        for (int i = 0; i < i4rights.length(); i++) {
            char c = i4rights.charAt(i);
            if (IMAP_READ_RIGHTS.indexOf(c) != -1) {
                if (allRightsPresent(i4rights, IMAP_READ_RIGHTS))    rights |= ACL.RIGHT_READ;
            } else if (IMAP_WRITE_RIGHTS.indexOf(c) != -1) {
                if (allRightsPresent(i4rights, IMAP_WRITE_RIGHTS))   rights |= ACL.RIGHT_WRITE;
            } else if (IMAP_INSERT_RIGHTS.indexOf(c) != -1) {
                if (allRightsPresent(i4rights, IMAP_INSERT_RIGHTS))  rights |= ACL.RIGHT_INSERT;
            } else if (IMAP_DELETE_RIGHTS.indexOf(c) != -1) {
                if (allRightsPresent(i4rights, IMAP_DELETE_RIGHTS))  rights |= ACL.RIGHT_DELETE;
            } else if (IMAP_ADMIN_RIGHTS.indexOf(c) != -1) {
                if (allRightsPresent(i4rights, IMAP_ADMIN_RIGHTS))   rights |= ACL.RIGHT_ADMIN;
            } else {
                // RFC 4314 3.1: "Note that an unrecognized right MUST cause the command to return
                //                the BAD response.  In particular, the server MUST NOT silently
                //                ignore unrecognized rights."
                ZimbraLog.imap.info("SETACL failed: invalid rights string: " + i4rights);
                sendBAD(tag, "SETACL failed: invalid right");
                return CONTINUE_PROCESSING;
            }
        }

        try {
            // make sure the requester has sufficient permissions to make the request
            if ((path.getFolderRights() & ACL.RIGHT_ADMIN) == 0) {
                ZimbraLog.imap.info("SETACL failed: user does not have admin access: " + path);
                sendNO(tag, "SETACL failed");
                return CONTINUE_PROCESSING;
            }

            // detect a no-op early and short-circuit out here
            if (action != StoreAction.REPLACE && rights == 0) {
                sendNotifications(true, false);
                sendOK(tag, "SETACL completed");
                return CONTINUE_PROCESSING;
            }

            // figure out who's being granted permissions
            String granteeId = null;
            byte granteeType = 0;
            if (principal.equals("anyone")) {
                granteeId = ACL.GUID_AUTHUSER;  granteeType = ACL.GRANTEE_AUTHUSER;
            } else {
                granteeType = ACL.GRANTEE_USER;
                NamedEntry entry = Provisioning.getInstance().get(AccountBy.name, principal);
                if (entry == null) {
                    entry = Provisioning.getInstance().get(DistributionListBy.name, principal);
                    granteeType = ACL.GRANTEE_GROUP;
                }
                if (entry != null)
                    granteeId = entry.getId();
            }
            if (granteeId == null) {
                ZimbraLog.imap.info("SETACL failed: cannot resolve principal: " + principal);
                sendNO(tag, "SETACL failed");
                return CONTINUE_PROCESSING;
            }

            // figure out the rights already granted on the folder
            short oldRights = 0, newRights = 0;
            Object folderobj = path.getFolder();
            if (folderobj instanceof Folder) {
                ACL acl = ((Folder) folderobj).getEffectiveACL();
                if (acl != null) {
                    for (ACL.Grant grant : acl.getGrants()) {
                        if (granteeId.equalsIgnoreCase(grant.getGranteeId()) || (granteeType == ACL.GRANTEE_AUTHUSER && (grant.getGranteeType() == ACL.GRANTEE_AUTHUSER || grant.getGranteeType() == ACL.GRANTEE_PUBLIC)))
                            oldRights |= grant.getGrantedRights();
                    }
                }
            } else {
                for (ZGrant zgrant : ((ZFolder) folderobj).getGrants()) {
                    if (granteeId.equalsIgnoreCase(zgrant.getGranteeId()) || (granteeType == ACL.GRANTEE_AUTHUSER && (zgrant.getGranteeType() == ZGrant.GranteeType.all || zgrant.getGranteeType() == ZGrant.GranteeType.pub)))
                        oldRights |= ACL.stringToRights(zgrant.getPermissions());
                }
            }

            // calculate the new rights we want granted on the folder
            if (action == StoreAction.REMOVE)    newRights = (short) (oldRights & ~rights);
            else if (action == StoreAction.ADD)  newRights = (short) (oldRights | rights);
            else                                 newRights = rights;

            // and update the folder appropriately, if necessary
            if (newRights != oldRights) {
                if (folderobj instanceof Folder) {
                    Mailbox mbox = (Mailbox) path.getOwnerMailbox();
                    mbox.grantAccess(getContext(), ((Folder) folderobj).getId(), granteeId, granteeType, newRights, null);
                } else {
                    ZMailbox zmbx = (ZMailbox) path.getOwnerMailbox();
                    ZGrant.GranteeType type = (granteeType == ACL.GRANTEE_AUTHUSER ? ZGrant.GranteeType.all : ZGrant.GranteeType.usr);
                    zmbx.modifyFolderGrant(((ZFolder) folderobj).getId(), type, principal, ACL.rightsToString(newRights), null);
                }
            }
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.PERM_DENIED))
                ZimbraLog.imap.info("SETACL failed: permission denied on folder: " + path);
            else if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER))
                ZimbraLog.imap.info("SETACL failed: no such folder: " + path);
            else if (e.getCode().equals(AccountServiceException.NO_SUCH_ACCOUNT))
                ZimbraLog.imap.info("SETACL failed: no such account: " + principal);
            else
                ZimbraLog.imap.warn("SETACL failed", e);
            sendNO(tag, "SETACL failed");
            return CONTINUE_PROCESSING;
        }

        sendNotifications(true, false);
        sendOK(tag, "SETACL completed");
        return CONTINUE_PROCESSING;
    }

    boolean doDELETEACL(String tag, ImapPath path, String principal) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        try {
            // make sure the requester has sufficient permissions to make the request
            if ((path.getFolderRights() & ACL.RIGHT_ADMIN) == 0) {
                ZimbraLog.imap.info("DELETEACL failed: user does not have admin access: " + path);
                sendNO(tag, "DELETEACL failed");
                return CONTINUE_PROCESSING;
            }
    
            // figure out whose permissions are being revoked
            String granteeId = null;
            if (principal.equals("anyone")) {
                granteeId = ACL.GUID_AUTHUSER;
            } else {
                NamedEntry entry = Provisioning.getInstance().get(AccountBy.name, principal);
                if (entry == null)
                    entry = Provisioning.getInstance().get(DistributionListBy.name, principal);
                if (entry != null)
                    granteeId = entry.getId();
            }
            if (granteeId == null) {
                ZimbraLog.imap.info("DELETEACL failed: cannot resolve principal: " + principal);
                sendNO(tag, "DELETEACL failed");
                return CONTINUE_PROCESSING;
            }

            // and revoke the permissions appropriately
            Object mboxobj = path.getOwnerMailbox();
            if (mboxobj instanceof Mailbox) {
                Mailbox mbox = (Mailbox) mboxobj;
                Folder folder = (Folder) path.getFolder();
                if (folder.getEffectiveACL() != null) {
                    mbox.revokeAccess(getContext(), folder.getId(), granteeId);
                    if (granteeId == ACL.GUID_AUTHUSER)
                        mbox.revokeAccess(getContext(), folder.getId(), ACL.GUID_PUBLIC);
                }
            } else {
                ZMailbox zmbx = (ZMailbox) mboxobj;
                ZFolder zfolder = (ZFolder) path.getFolder();
                if (!zfolder.getGrants().isEmpty()) {
                    zmbx.modifyFolderRevokeGrant(zfolder.getId(), granteeId);
                    if (granteeId == ACL.GUID_AUTHUSER)
                        zmbx.modifyFolderRevokeGrant(zfolder.getId(), ACL.GUID_PUBLIC);
                }
            }
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.PERM_DENIED))
                ZimbraLog.imap.info("DELETEACL failed: permission denied on folder: " + path);
            else if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER))
                ZimbraLog.imap.info("DELETEACL failed: no such folder: " + path);
            else if (e.getCode().equals(AccountServiceException.NO_SUCH_ACCOUNT))
                ZimbraLog.imap.info("DELETEACL failed: no such account: " + principal);
            else
                ZimbraLog.imap.warn("DELETEACL failed", e);
            sendNO(tag, "DELETEACL failed");
            return CONTINUE_PROCESSING;
        }

        sendNotifications(true, false);
        sendOK(tag, "DELETEACL completed");
        return CONTINUE_PROCESSING;
    }

    boolean doGETACL(String tag, ImapPath path) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        StringBuilder i4acl = new StringBuilder("ACL ").append(path.asUtf7String());

        try {
            // make sure the requester has sufficient permissions to make the request
            if ((path.getFolderRights() & ACL.RIGHT_ADMIN) == 0) {
                ZimbraLog.imap.info("GETACL failed: user does not have admin access: " + path);
                sendNO(tag, "GETACL failed");
                return CONTINUE_PROCESSING;
            }

            // the target folder's owner always has full rights
            Account owner = path.getOwnerAccount();
            if (owner != null)
                i4acl.append(" \"").append(owner.getName()).append("\" ").append(IMAP_CONCATENATED_RIGHTS);

            // write out the grants to all users and groups
            Short anyoneRights = null;
            Object folderobj = path.getFolder();
            if (folderobj instanceof Folder) {
                ACL acl = ((Folder) folderobj).getEffectiveACL();
                if (acl != null) {
                    for (ACL.Grant grant : acl.getGrants()) {
                        byte type = grant.getGranteeType();
                        short rights = grant.getGrantedRights();
                        if (type == ACL.GRANTEE_AUTHUSER || type == ACL.GRANTEE_PUBLIC) {
                            anyoneRights = (short) ((anyoneRights == null ? 0 : anyoneRights) | rights);
                        } else if (type == ACL.GRANTEE_USER || type == ACL.GRANTEE_GROUP) {
                            NamedEntry entry = FolderAction.lookupGranteeByZimbraId(grant.getGranteeId(), type);
                            if (entry != null)
                                i4acl.append(" \"").append(entry.getName()).append("\" ").append(exportRights(rights));
                        }
                    }
                }
            } else {
                for (ZGrant zgrant : ((ZFolder) folderobj).getGrants()) {
                    ZGrant.GranteeType ztype = zgrant.getGranteeType();
                    short rights = ACL.stringToRights(zgrant.getPermissions());
                    if (ztype == ZGrant.GranteeType.pub || ztype == ZGrant.GranteeType.all) {
                        anyoneRights = (short) ((anyoneRights == null ? 0 : anyoneRights) | rights);
                    } else if (ztype == ZGrant.GranteeType.usr || ztype == ZGrant.GranteeType.grp) {
                        byte granteeType = ztype == ZGrant.GranteeType.usr ? ACL.GRANTEE_USER : ACL.GRANTEE_GROUP;
                        NamedEntry entry = FolderAction.lookupGranteeByZimbraId(zgrant.getGranteeId(), granteeType);
                        if (entry != null)
                            i4acl.append(" \"").append(entry.getName()).append("\" ").append(exportRights(rights));
                    }
                }
            }

            // aggregate all the "public" and "auth user" grants into the "anyone" IMAP ACL
            if (anyoneRights != null)
                i4acl.append(" anyone ").append(exportRights(anyoneRights));
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.PERM_DENIED))
                ZimbraLog.imap.info("GETACL failed: permission denied on folder: " + path);
            else if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER))
                ZimbraLog.imap.info("GETACL failed: no such folder: " + path);
            else
                ZimbraLog.imap.warn("GETACL failed", e);
            sendNO(tag, "GETACL failed");
            return CONTINUE_PROCESSING;
        }

        sendUntagged(i4acl.toString());
        sendNotifications(true, false);
        sendOK(tag, "GETACL completed");
        return CONTINUE_PROCESSING;
    }

    /** The set of rights required to create a new subfolder in ZCS. */
    private short SUBFOLDER_RIGHTS = ACL.RIGHT_INSERT | ACL.RIGHT_READ;

    /** Converts a Zimbra rights bitmask to an RFC 4314-compatible rights
     *  string. */
    private String exportRights(short rights) {
        StringBuilder imapRights = new StringBuilder(12);
        if ((rights & ACL.RIGHT_READ) == ACL.RIGHT_READ)      imapRights.append("lr");
        if ((rights & ACL.RIGHT_WRITE) == ACL.RIGHT_WRITE)    imapRights.append("sw");
        if ((rights & ACL.RIGHT_INSERT) == ACL.RIGHT_INSERT)  imapRights.append("ic");
        if ((rights & SUBFOLDER_RIGHTS) == SUBFOLDER_RIGHTS)  imapRights.append("k");
        if ((rights & ACL.RIGHT_DELETE) == ACL.RIGHT_DELETE)  imapRights.append("xted");
        if ((rights & ACL.RIGHT_ADMIN) == ACL.RIGHT_ADMIN)    imapRights.append("a");
        return imapRights.length() == 0 ? "\"\"" : imapRights.toString();
    }

    /** All the supported IMAP rights, concatenated together into a single string. */
    private static final String IMAP_CONCATENATED_RIGHTS = IMAP_READ_RIGHTS + IMAP_WRITE_RIGHTS + IMAP_INSERT_RIGHTS + IMAP_DELETE_RIGHTS + IMAP_ADMIN_RIGHTS;
    /** All the supported IMAP rights, with <tt>linked</tt> sets of rights
     *  grouped together and the groups delimited by spaces. */
    private static final String IMAP_DELIMITED_RIGHTS = IMAP_READ_RIGHTS + ' ' + IMAP_WRITE_RIGHTS + ' ' + IMAP_INSERT_RIGHTS + ' ' + IMAP_DELETE_RIGHTS + ' ' + IMAP_ADMIN_RIGHTS;

    boolean doLISTRIGHTS(String tag, ImapPath path, String principal) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        boolean isOwner = false;
        try {
            if (!principal.equals("anyone")) {
                Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.name, principal);
                if (acct == null)
                    throw AccountServiceException.NO_SUCH_ACCOUNT(principal);
                isOwner = path.belongsTo(acct.getId());
            }
            // as a side effect, path.getFolderRights() checks for the existence of the target folder
            if ((path.getFolderRights() & ACL.RIGHT_ADMIN) == 0)
                throw ServiceException.PERM_DENIED("you must have admin privileges to perform LISTRIGHTS");
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.PERM_DENIED))
                ZimbraLog.imap.info("LISTRIGHTS failed: permission denied on folder: " + path);
            else if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER))
                ZimbraLog.imap.info("LISTRIGHTS failed: no such folder: " + path);
            else if (e.getCode().equals(AccountServiceException.NO_SUCH_ACCOUNT))
                ZimbraLog.imap.info("LISTRIGHTS failed: no such account: " + principal);
            else
                ZimbraLog.imap.warn("LISTRIGHTS failed", e);
            sendNO(tag, "LISTRIGHTS failed");
            return canContinue(e);
        }

        if (isOwner)
            sendUntagged("LISTRIGHTS " + path.asUtf7String() + " \"" + principal + "\" " + IMAP_CONCATENATED_RIGHTS);
        else
            sendUntagged("LISTRIGHTS " + path.asUtf7String() + " \"" + principal + "\" \"\" " + IMAP_DELIMITED_RIGHTS);
        sendNotifications(true, false);
        sendOK(tag, "LISTRIGHTS completed");
        return CONTINUE_PROCESSING;
    }

    boolean doMYRIGHTS(String tag, ImapPath path) throws IOException {
        if (!checkState(tag, State.AUTHENTICATED))
            return CONTINUE_PROCESSING;

        short rights;
        try {
            // as a side effect, path.getFolderRights() checks for the existence of the target folder
            rights = path.getFolderRights();
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.PERM_DENIED))
                ZimbraLog.imap.info("MYRIGHTS failed: permission denied on folder: " + path);
            else if (e.getCode().equals(MailServiceException.NO_SUCH_FOLDER))
                ZimbraLog.imap.info("MYRIGHTS failed: no such folder: " + path);
            else if (e.getCode().equals(AccountServiceException.NO_SUCH_ACCOUNT))
                ZimbraLog.imap.info("MYRIGHTS failed: no such account: " + path.getOwner());
            else
                ZimbraLog.imap.warn("MYRIGHTS failed", e);
            sendNO(tag, "MYRIGHTS failed");
            return canContinue(e);
        }

        sendUntagged("MYRIGHTS " + path.asUtf7String() + ' ' + exportRights(rights));
        sendNotifications(true, false);
        sendOK(tag, "MYRIGHTS completed");
        return CONTINUE_PROCESSING;
    }

    boolean doCHECK(String tag) throws IOException {
        if (!checkState(tag, State.SELECTED))
            return CONTINUE_PROCESSING;

        sendNotifications(true, false);
        sendOK(tag, "CHECK completed");
        return CONTINUE_PROCESSING;
    }

    boolean doCLOSE(String tag) throws IOException, ImapParseException {
        if (!checkState(tag, State.SELECTED))
            return CONTINUE_PROCESSING;

        boolean expunged = false;
        try {
            // 6.4.2: "The CLOSE command permanently removes all messages that have the \Deleted
            //         flag set from the currently selected mailbox, and returns to the authenticated
            //         state from the selected state.  No untagged EXPUNGE responses are sent.
            //
            //         No messages are removed, and no error is given, if the mailbox is
            //         selected by an EXAMINE command or is otherwise selected read-only."
            if (mSelectedFolder.isWritable() && mSelectedFolder.getPath().isWritable(ACL.RIGHT_DELETE))
                expunged = expungeMessages(tag, mSelectedFolder, null);
        } catch (ServiceException e) {
            // log the error but keep going...
            ZimbraLog.imap.warn("error during CLOSE", e);
        }

        String status = "";
        try {
            if (expunged && !mSelectedFolder.isVirtual() && sessionActivated(ActivatedExtension.QRESYNC))
                status = "[HIGHESTMODSEQ " + mSelectedFolder.getCurrentMODSEQ() + "] ";
        } catch (ServiceException e) {
            ZimbraLog.imap.info("error while determining HIGHESTMODSEQ of selected folder", e);
        }

        unsetSelectedFolder();

        sendOK(tag, status + "CLOSE completed");
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

    boolean doEXPUNGE(String tag, boolean byUID, String sequenceSet) throws IOException, ImapParseException {
        if (!checkState(tag, State.SELECTED)) {
            return CONTINUE_PROCESSING;
        } else if (!mSelectedFolder.isWritable()) {
            sendNO(tag, "mailbox selected READ-ONLY");
            return CONTINUE_PROCESSING;
        }

        String command = (byUID ? "UID EXPUNGE" : "EXPUNGE");
        boolean expunged = false;
        try {
            if (!mSelectedFolder.getPath().isWritable(ACL.RIGHT_DELETE))
                throw ServiceException.PERM_DENIED("you do not have permission to delete messages from this folder");

            expunged = expungeMessages(tag, mSelectedFolder, sequenceSet);
        } catch (ServiceException e) {
            ZimbraLog.imap.warn(command + " failed", e);
            sendNO(tag, command + " failed");
            return canContinue(e);
        }

        String status = "";
        try {
            if (expunged && byUID && !mSelectedFolder.isVirtual() && sessionActivated(ActivatedExtension.QRESYNC))
                status = "[HIGHESTMODSEQ " + mSelectedFolder.getCurrentMODSEQ() + "] ";
        } catch (ServiceException e) {
            ZimbraLog.imap.info("error while determining HIGHESTMODSEQ of selected folder", e);
        }

        sendNotifications(true, false);
        sendOK(tag, status + command + " completed");
        return CONTINUE_PROCESSING;
    }

    boolean expungeMessages(String tag, ImapFolder i4folder, String sequenceSet) throws ServiceException, IOException, ImapParseException {
        Set<ImapMessage> i4set;
        synchronized (mSelectedFolder.getMailbox()) {
            i4set = (sequenceSet == null ? null : i4folder.getSubsequence(tag, sequenceSet, true));
        }
        List<Integer> ids = new ArrayList<Integer>(SUGGESTED_DELETE_BATCH_SIZE);

        boolean changed = false;
        long checkpoint = System.currentTimeMillis();
        for (int i = 1, max = i4folder.getSize(); i <= max; i++) {
            ImapMessage i4msg = i4folder.getBySequence(i);
            if (i4msg != null && !i4msg.isExpunged() && (i4msg.flags & Flag.BITMASK_DELETED) > 0) {
                if (i4set == null || i4set.contains(i4msg)) {
                    ids.add(i4msg.msgId);  changed = true;
                }
            }

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

        return changed;
    }

    private static final int RETURN_MIN   = 0x01;
    private static final int RETURN_MAX   = 0x02;
    private static final int RETURN_ALL   = 0x04;
    private static final int RETURN_COUNT = 0x08;
    private static final int RETURN_SAVE  = 0x10;

    private static final int LARGEST_FOLDER_BATCH = 600;
    public static final byte[] ITEM_TYPES = ArrayUtil.toByteArray(ImapMessage.SUPPORTED_TYPES);

    boolean doSEARCH(String tag, ImapSearch i4search, boolean byUID, Integer options) throws IOException, ImapParseException {
        if (!checkState(tag, State.SELECTED))
            return CONTINUE_PROCESSING;

        boolean requiresMODSEQ = i4search.requiresMODSEQ();
        if (requiresMODSEQ)
            activateExtension(ActivatedExtension.CONDSTORE);
        // RFC 4551 3.1.2: "A server that returned NOMODSEQ response code for a mailbox,
        //                  which subsequently receives one of the following commands
        //                  while the mailbox is selected:
        //                     -  a FETCH or SEARCH command that includes the MODSEQ
        //                         message data item
        //                  MUST reject any such command with the tagged BAD response."
        if (requiresMODSEQ && !sessionActivated(ActivatedExtension.CONDSTORE))
            throw new ImapParseException(tag, "NOMODSEQ", "cannot SEARCH MODSEQ in this mailbox", true);

        boolean saveResults = (options != null && (options & RETURN_SAVE) != 0);
        ImapMessageSet hits;
        int modseq = 0;

        try {
            Mailbox mbox = mSelectedFolder.getMailbox();
            synchronized (mbox) {
                if (i4search.canBeRunLocally()) {
                    hits = i4search.evaluate(mSelectedFolder);
                    hits.remove(null);
                } else {
                    String search = i4search.toZimbraSearch(mSelectedFolder);
                    if (!mSelectedFolder.isVirtual())
                        search = "in:" + mSelectedFolder.getQuotedPath() + ' ' + search;
                    else if (mSelectedFolder.getSize() <= LARGEST_FOLDER_BATCH)
                        search = ImapSearch.sequenceAsSearchTerm(mSelectedFolder, mSelectedFolder.getAllMessages(), false) + ' ' + search;
                    else
                        search = '(' + mSelectedFolder.getQuery() + ") " + search;
                    ZimbraLog.imap.info("[ search is: " + search + " ]");

                    SearchParams params = new SearchParams();
                    params.setIncludeTagDeleted(true);
                    params.setQueryStr(search);
                    params.setTypes(ITEM_TYPES);
                    params.setSortBy(MailboxIndex.SortBy.DATE_ASCENDING);
                    params.setChunkSize(2000);
                    params.setPrefetch(false);
                    params.setMode(requiresMODSEQ ? Mailbox.SearchResultMode.MODSEQ : Mailbox.SearchResultMode.IDS);
                    ZimbraQueryResults zqr = mbox.search(SoapProtocol.Soap12, getContext(), params);

                    hits = new ImapMessageSet();
                    try {
                        for (ZimbraHit hit = zqr.getFirstHit(); hit != null; hit = zqr.getNext()) {
                            ImapMessage i4msg = mSelectedFolder.getById(hit.getItemId());
                            if (i4msg == null || i4msg.isExpunged())
                                continue;
                            hits.add(i4msg);
                            if (requiresMODSEQ)
                                modseq = Math.max(modseq, Math.max(hit.getModifiedSequence(), i4msg.getFlagModseq(mSelectedFolder.getTagset())));
                        }
                    } finally {
                        zqr.doneWithSearchResults();
                    }
                }
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
            if (byUID)
                result.append(" UID");
            if (!hits.isEmpty() && (options & RETURN_MIN) != 0)
                result.append(" MIN ").append(byUID ? hits.first().imapUid : hits.first().sequence);
            if (!hits.isEmpty() && (options & RETURN_MAX) != 0)
                result.append(" MAX ").append(byUID ? hits.last().imapUid : hits.last().sequence);
            if ((options & RETURN_COUNT) != 0)
                result.append(" COUNT ").append(hits.size());
            if (!hits.isEmpty() && (options & RETURN_ALL) != 0)
                result.append(" ALL ").append(ImapFolder.encodeSubsequence(hits, byUID));
        }

        if (modseq > 0)
            result.append(" (MODSEQ ").append(modseq).append(')');

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
        sendOK(tag, (byUID ? "UID " : "") + "SEARCH completed");
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
    static final int FETCH_MODSEQ        = 0x0100;
    static final int FETCH_VANISHED      = 0x0200;
    static final int FETCH_MARK_READ     = 0x1000;

    private static final int FETCH_FROM_CACHE = FETCH_FLAGS | FETCH_UID;
    private static final int FETCH_FROM_MIME  = FETCH_BODY | FETCH_BODYSTRUCTURE | FETCH_ENVELOPE;

    static final int FETCH_FAST = FETCH_FLAGS | FETCH_INTERNALDATE | FETCH_RFC822_SIZE;
    static final int FETCH_ALL  = FETCH_FAST  | FETCH_ENVELOPE;
    static final int FETCH_FULL = FETCH_ALL   | FETCH_BODY;

    boolean doFETCH(String tag, String sequenceSet, int attributes, List<ImapPartSpecifier> parts, boolean byUID, int changedSince)
    throws IOException, ImapParseException {
        return fetch(tag, sequenceSet, attributes, parts, byUID, changedSince, true);
    }

    boolean fetch(String tag, String sequenceSet, int attributes, List<ImapPartSpecifier> parts, boolean byUID, int changedSince, boolean standalone)
    throws IOException, ImapParseException {
        if (!checkState(tag, State.SELECTED))
            return CONTINUE_PROCESSING;

        ImapFolder i4folder = mSelectedFolder;

        // 6.4.8: "However, server implementations MUST implicitly include the UID message
        //         data item as part of any FETCH response caused by a UID command, regardless
        //         of whether a UID was specified as a message data item to the FETCH."
        if (byUID)
            attributes |= FETCH_UID;
        String command = (byUID ? "UID FETCH" : "FETCH");
        boolean markRead = i4folder.isWritable() && (attributes & FETCH_MARK_READ) != 0;

        // draft-ietf-lemonade-reconnect-client-06 3.2: "The VANISHED UID FETCH modifier MUST
        //                                               only be specified together with
        //                                               the CHANGEDSINCE UID FETCH modifier."
        if ((attributes & FETCH_VANISHED) != 0 && (!byUID || changedSince < 0))
            throw new ImapParseException(tag, "cannot specify VANISHED without CHANGEDSINCE");

        if (changedSince >= 0)
            attributes |= FETCH_MODSEQ;
        if ((attributes & FETCH_MODSEQ) != 0)
            activateExtension(ActivatedExtension.CONDSTORE);
        boolean modseqEnabled = sessionActivated(ActivatedExtension.CONDSTORE);
        // RFC 4551 3.1.2: "A server that returned NOMODSEQ response code for a mailbox,
        //                  which subsequently receives one of the following commands
        //                  while the mailbox is selected:
        //                     -  a FETCH command with the CHANGEDSINCE modifier,
        //                     -  a FETCH or SEARCH command that includes the MODSEQ
        //                         message data item
        //                  MUST reject any such command with the tagged BAD response."
        if (!modseqEnabled && (attributes & FETCH_MODSEQ) != 0)
            throw new ImapParseException(tag, "NOMODSEQ", "cannot FETCH MODSEQ in this mailbox", true);

        List<ImapPartSpecifier> fullMessage = new ArrayList<ImapPartSpecifier>();
        if (parts != null && !parts.isEmpty()) {
            for (Iterator<ImapPartSpecifier> it = parts.iterator(); it.hasNext(); ) {
                ImapPartSpecifier pspec = it.next();
                if (pspec.isEntireMessage()) {
                    it.remove();  fullMessage.add(pspec);
                }
            }
        }

        ImapMessageSet i4set;
        Mailbox mbox = i4folder.getMailbox();
        synchronized (mbox) {
            i4set = i4folder.getSubsequence(tag, sequenceSet, byUID);
        }
        boolean allPresent = byUID || !i4set.contains(null);
        i4set.remove(null);

        // if VANISHED was requested, we need to return the set of UIDs that *don't* exist in the folder
        if (byUID && (attributes & FETCH_VANISHED) != 0) {
            int highwater = Integer.MAX_VALUE;
            try {
                highwater = i4folder.getCurrentMODSEQ();
            } catch (ServiceException e) { }
            if (highwater > changedSince) {
                String vanished = i4folder.invertSubsequence(sequenceSet, true, i4set);
                if (!vanished.equals(""))
                    sendUntagged("VANISHED (EARLIER) " + vanished);
            }
        }

        // if a CHANGEDSINCE sequence number was specified, narrow the message set before iterating over the messages
        if (changedSince >= 0) {
            try {
                // get a list of all the messages modified since the checkpoint
                Set<Integer> folderId = new HashSet<Integer>(Arrays.asList(i4folder.getId()));
                ImapMessageSet modified = new ImapMessageSet();
                for (int id : mbox.getModifiedItems(getContext(), changedSince, MailItem.TYPE_UNKNOWN, folderId).getFirst()) {
                    ImapMessage i4msg = i4folder.getById(id);
                    if (i4msg != null)
                        modified.add(i4msg);
                }
                // add any messages with tags whose names have been changed since the checkpoint
                ImapFlagCache i4cache = i4folder.getTagset();
                if (i4cache.getMaximumModseq() > changedSince) {
                    for (ImapMessage i4msg : i4set) {
                        if (i4msg.getFlagModseq(i4cache) > changedSince)
                            modified.add(i4msg);
                    }
                }
                // and intersect those "modified" messages with the set of requested messages
                i4set.retainAll(modified);
            } catch (ServiceException e) {
                if (standalone) {
                    ZimbraLog.imap.warn(command + " failed", e);
                    sendNO(tag, command + " failed");
                    return canContinue(e);
                }
            }
        }

        synchronized (mbox) {
            if (i4folder.areTagsDirty()) {
                sendUntagged("FLAGS (" + StringUtil.join(" ", i4folder.getFlagList(false)) + ')');
                i4folder.cleanTags();
            }
        }

        for (ImapMessage i4msg : i4set) {
            OutputStream os = mOutputStream;
            ByteArrayOutputStream baosDebug = ZimbraLog.imap.isDebugEnabled() ? new ByteArrayOutputStream() : null;
	        PrintStream result = new PrintStream(new ByteUtil.TeeOutputStream(os, baosDebug), false, "utf-8");
        	try {
                boolean markMessage = markRead && (i4msg.flags & Flag.BITMASK_UNREAD) != 0;
                boolean empty = true;
                MailItem item = null;
                MimeMessage mm = null;

                result.print("* " + i4msg.sequence + " FETCH (");

                if (!fullMessage.isEmpty() || (parts != null && !parts.isEmpty()) || (attributes & ~FETCH_FROM_CACHE) != 0) {
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
                    // FIXME: fetching the entire body into memory even for a partial fetch
                    for (ImapPartSpecifier pspec : fullMessage) {
                        Pair<Long, InputStream> contents = ImapMessage.getContent(item);
                        try {
                            result.print(empty ? "" : " ");  pspec.writeMessage(result, os, contents.getSecond(), contents.getFirst());  empty = false;
                        } finally {
                            ByteUtil.closeStream(contents.getSecond());
                        }
                    }
                }

                if ((parts != null && !parts.isEmpty()) || (attributes & FETCH_FROM_MIME) != 0) {
                    mm = ImapMessage.getMimeMessage(item);
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
                ImapFolder.DirtyMessage unsolicited = i4folder.undirtyMessage(i4msg);
                if ((attributes & FETCH_FLAGS) != 0 || unsolicited != null) {
                    result.print(empty ? "" : " ");  result.print(i4msg.getFlags(i4folder));  empty = false;
                }

                // RFC 4551 3.2: "Once the client specified the MODSEQ message data item in a
                //                FETCH request, the server MUST include the MODSEQ fetch response
                //                data items in all subsequent unsolicited FETCH responses."
                if ((attributes & FETCH_MODSEQ) != 0 || (modseqEnabled && unsolicited != null)) {
                    int modseq = unsolicited == null ? i4msg.getModseq(item, i4folder.getTagset()) : unsolicited.modseq;
                    result.print((empty ? "" : " ") + "MODSEQ (" + modseq + ')');  empty = false;
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

        if (standalone) {
            sendNotifications(false, false);
            if (allPresent) {
                sendOK(tag, command + " completed");
            } else {
                // RFC 2180 4.1.2: "The server MAY allow the EXPUNGE of a multi-accessed mailbox,
                //                  and on subsequent FETCH commands return FETCH responses only
                //                  for non-expunged messages and a tagged NO."
                sendNO(tag, "some of the requested messages no longer exist");
            }
        }
        return CONTINUE_PROCESSING;
    }

    enum StoreAction { REPLACE, ADD, REMOVE }
    
    private final int SUGGESTED_BATCH_SIZE = 100;

    boolean doSTORE(String tag, String sequenceSet, List<String> flagNames, StoreAction operation, boolean silent, int modseq, boolean byUID)
    throws IOException, ImapParseException {
        if (!checkState(tag, State.SELECTED)) {
            return CONTINUE_PROCESSING;
        } else if (!mSelectedFolder.isWritable()) {
            sendNO(tag, "mailbox selected READ-ONLY");
            return CONTINUE_PROCESSING;
        }

        if (modseq >= 0)
            activateExtension(ActivatedExtension.CONDSTORE);
        boolean modseqEnabled = sessionActivated(ActivatedExtension.CONDSTORE);
        // RFC 4551 3.1.2: "A server that returned NOMODSEQ response code for a mailbox,
        //                  which subsequently receives one of the following commands
        //                  while the mailbox is selected:
        //                     -  a STORE command with the UNCHANGEDSINCE modifier
        //                  MUST reject any such command with the tagged BAD response."
        if (!modseqEnabled && modseq >= 0)
            throw new ImapParseException(tag, "NOMODSEQ", "cannot STORE UNCHANGEDSINCE in this mailbox", true);
        ImapMessageSet modifyConflicts = modseqEnabled ? new ImapMessageSet() : null;

        String command = (byUID ? "UID STORE" : "STORE");
        List<Tag> newTags = (operation != StoreAction.REMOVE ? new ArrayList<Tag>() : null);
        Mailbox mbox = mSelectedFolder.getMailbox();

        Set<ImapMessage> i4set;
        synchronized (mbox) {
            i4set = mSelectedFolder.getSubsequence(tag, sequenceSet, byUID);
        }
        boolean allPresent = byUID || !i4set.contains(null);
        i4set.remove(null);

        try {
            // get set of relevant tags
            Set<ImapFlag> i4flags = new HashSet<ImapFlag>(flagNames.size());
            synchronized (mbox) {
                for (String name : flagNames) {
                    ImapFlag i4flag = mSelectedFolder.getFlagByName(name);
                    if (i4flag == null || !i4flag.mListed)
                        i4flag = mSelectedFolder.getTagset().createTag(getContext(), name, newTags);
                    if (i4flag != null)
                        i4flags.add(i4flag);
                }

                if (mSelectedFolder.areTagsDirty()) {
                    sendUntagged("FLAGS (" + StringUtil.join(" ", mSelectedFolder.getFlagList(false)) + ')');
                    mSelectedFolder.cleanTags();
                }
            }

            if (operation != StoreAction.REMOVE) {
                for (ImapFlag i4flag : i4flags) {
                    if (i4flag.mId == Flag.ID_FLAG_DELETED) {
                        if (!mSelectedFolder.getPath().isWritable(ACL.RIGHT_DELETE))
                            throw ServiceException.PERM_DENIED("you do not have permission to set the \\Deleted flag");
                    } else if (i4flag.mPermanent) {
                        if (!mSelectedFolder.getPath().isWritable(ACL.RIGHT_WRITE))
                            throw ServiceException.PERM_DENIED("you do not have permission to set the " + i4flag.mName + " flag");
                    }
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

                synchronized (mbox) {
                    if (modseq >= 0) {
                        MailItem[] items = mbox.getItemById(getContext(), idlist, MailItem.TYPE_UNKNOWN);
                        for (int idx = items.length - 1; idx >= 0; idx--) {
                            ImapMessage i4msg = i4list.get(idx);
                            if (i4msg.getModseq(items[idx], mSelectedFolder.getTagset()) > modseq) {
                                modifyConflicts.add(i4msg);
                                i4list.remove(idx);  idlist.remove(idx);
                                allPresent = false;
                            }
                        }
                    }

                    try {
                        // if it was a STORE [+-]?FLAGS.SILENT, temporarily disable notifications
                        if (silent && !modseqEnabled)
                            mSelectedFolder.disableNotifications();
    
                        if (operation == StoreAction.REPLACE) {
                            // replace real tags and flags on all messages
                            mbox.setTags(getContext(), ArrayUtil.toIntArray(idlist), MailItem.TYPE_UNKNOWN, flags, tags, null);
                            // replace session tags on all messages
                            for (ImapMessage i4msg : i4list)
                                i4msg.setSessionFlags(sflags, mSelectedFolder);
                        } else {
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
                }

                if (!silent || modseqEnabled) {
                    for (ImapMessage i4msg : i4list) {
                        ImapFolder.DirtyMessage dirty = mSelectedFolder.undirtyMessage(i4msg);
                        if (silent && (dirty == null || dirty.modseq <= 0))
                            continue;

                        StringBuilder ntfn = new StringBuilder();
                        boolean empty = true;
                        ntfn.append(i4msg.sequence).append(" FETCH (");
                        if (!silent) {
                            ntfn.append(i4msg.getFlags(mSelectedFolder));  empty = false;
                        }
                        // 6.4.8: "However, server implementations MUST implicitly include
                        //         the UID message data item as part of any FETCH response
                        //         caused by a UID command..."
                        if (byUID) {
                            ntfn.append(empty ? "": " ").append("UID ").append(i4msg.imapUid);  empty = true;
                        }
                        if (dirty != null && dirty.modseq > 0 && modseqEnabled) {
                            ntfn.append(empty ? "": " ").append("MODSEQ (").append(dirty.modseq).append(')');  empty = true;
                        }
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
            if (e.getCode().equals(MailServiceException.INVALID_NAME))
                ZimbraLog.imap.info(command + " failed: " + e.getMessage());
            else
                ZimbraLog.imap.warn(command + " failed", e);
            sendNO(tag, command + " failed");
            return canContinue(e);
        }

        String skipped = modifyConflicts == null || modifyConflicts.isEmpty() ? "" : " [MODIFIED " + ImapFolder.encodeSubsequence(modifyConflicts, byUID) + ']';

        sendNotifications(false, false);
        // RFC 2180 4.2.1: "If the ".SILENT" suffix is used, and the STORE completed successfully for
        //                  all the non-expunged messages, the server SHOULD return a tagged OK."
        // RFC 2180 4.2.3: "If the ".SILENT" suffix is not used, and a mixture of expunged and non-
        //                  expunged messages are referenced, the server MAY set the flags and return
        //                  a FETCH response for the non-expunged messages along with a tagged NO."
        if (silent || allPresent)
            sendOK(tag, command + skipped + " completed");
        else
            sendNO(tag, command + skipped + " completed");
        return CONTINUE_PROCESSING;
    }

    private final int SUGGESTED_COPY_BATCH_SIZE = 50;

    boolean doCOPY(String tag, String sequenceSet, ImapPath path, boolean byUID) throws IOException, ImapParseException {
        if (!checkState(tag, State.SELECTED))
            return CONTINUE_PROCESSING;

        String command = (byUID ? "UID COPY" : "COPY");
        String copyuid = "";
        List<MailItem> copies = new ArrayList<MailItem>();
        Mailbox mbox = mSelectedFolder.getMailbox();

        Set<ImapMessage> i4set;
        synchronized (mbox) {
            i4set = mSelectedFolder.getSubsequence(tag, sequenceSet, byUID);
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
                sameMailbox = mbox.getAccountId().equalsIgnoreCase(((Mailbox) mboxobj).getAccountId());
                Folder folder = (Folder) path.getFolder();
                iidTarget = new ItemId(folder);
                uvv = ImapFolder.getUIDValidity(folder);
            } else if (mboxobj instanceof ZMailbox) {
                ZFolder zfolder = (ZFolder) path.getFolder();
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
                        for (ImapMessage curMsg : i4list) {
                            mItemIds[counter++] = curMsg.msgId;
                            if (counter == 1)
                                type = curMsg.getType();
                            else if (curMsg.getType() != type)
                                type = MailItem.TYPE_UNKNOWN;
                        }
                        copyMsgs = mbox.imapCopy(getContext(), mItemIds, type, iidTarget.getId());
                    } catch (IOException e) {
                        throw ServiceException.FAILURE("Caught IOException executing " + this, e);
                    }
                    
                    copies.addAll(copyMsgs);
                    for (MailItem target : copyMsgs)
                        createdList.add(target.getImapUid());
                } else {
                    ItemActionHelper op = ItemActionHelper.COPY(getContext(), mbox, null, idlist, MailItem.TYPE_UNKNOWN, null, iidTarget);
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
            } else if (e.getCode().equals(ImapServiceException.FOLDER_NOT_VISIBLE)) {
                ZimbraLog.imap.info(command + " failed: folder not visible: " + path);
            } else if (e.getCode().equals(ImapServiceException.FOLDER_NOT_WRITABLE)) {
                ZimbraLog.imap.info(command + " failed: folder not writable: " + path);
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
        ImapFolder i4folder = mSelectedFolder;
        if (i4folder == null)
            return;

        Mailbox mbox = i4folder.getMailbox();
        if (mbox == null)
            return;

        List<String> notifications = new ArrayList<String>();
        // XXX: is this the right thing to synchronize on?
        synchronized (mbox) {
            // FIXME: notify untagged NO if close to quota limit

            if (i4folder.areTagsDirty()) {
                notifications.add("FLAGS (" + StringUtil.join(" ", i4folder.getFlagList(false)) + ')');
                i4folder.cleanTags();
            }

            int oldRecent = i4folder.getRecentCount();
            boolean removed = false, received = i4folder.checkpointSize();
            if (notifyExpunges) {
                List<Integer> expunged = i4folder.collapseExpunged();
                removed = !expunged.isEmpty();
                if (removed) {
                    if (sessionActivated(ActivatedExtension.QRESYNC)) {
                        notifications.add("VANISHED " + ImapFolder.encodeSubsequence(expunged));
                    } else {
                        for (Integer index : expunged)
                            notifications.add(index + " EXPUNGE");
                    }
                }
            }
            i4folder.checkpointSize();

            // notify of any message flag changes
            boolean sendModseq = sessionActivated(ActivatedExtension.CONDSTORE);
            for (Iterator<ImapFolder.DirtyMessage> it = i4folder.dirtyIterator(); it.hasNext(); ) {
                ImapFolder.DirtyMessage dirty = it.next();
                if (dirty.i4msg.isAdded())
                    dirty.i4msg.setAdded(false);
                else
                    notifications.add(dirty.i4msg.sequence + " FETCH (" + dirty.i4msg.getFlags(i4folder) +
                                      (sendModseq && dirty.modseq > 0 ? " MODSEQ (" + dirty.modseq + ')' : "") + ')');
            }
            i4folder.clearDirty();

            if (received || removed)
                notifications.add(i4folder.getSize() + " EXISTS");
            if (received || oldRecent != i4folder.getRecentCount())
                notifications.add(i4folder.getRecentCount() + " RECENT");
        }

        // no I/O while the Mailbox is locked...
        for (String ntfn : notifications)
            sendUntagged(ntfn);
        if (flush)
            flushOutput();
    }

    @Override
    public void dropConnection() {
        dropConnection(true);
    }
    
    abstract protected void dropConnection(boolean sendBanner);

    abstract protected void flushOutput() throws IOException;

    abstract protected void enableInactivityTimer() throws IOException;

    abstract protected void completeAuthentication() throws IOException;

    void sendIdleUntagged() throws IOException                   { sendUntagged("NOOP", true); }

    void sendOK(String tag, String response) throws IOException  { sendResponse(tag, response.equals("") ? "OK" : "OK " + response, true); }
    void sendNO(String tag, String response) throws IOException  { sendResponse(tag, response.equals("") ? "NO" : "NO " + response, true); }
    void sendBAD(String tag, String response) throws IOException { sendResponse(tag, response.equals("") ? "BAD" : "BAD " + response, true); }
    void sendUntagged(String response) throws IOException        { sendResponse("*", response, false); }
    void sendUntagged(String response, boolean flush) throws IOException { sendResponse("*", response, flush); }
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
