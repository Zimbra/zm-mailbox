/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.imap.ImapPartSpecifier.BinaryDecodingException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.util.JMSession;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;

class ImapURL {
    private static class ImapUrlException extends ImapParseException {
        private static final long serialVersionUID = 174398702563521440L;

        ImapUrlException(String tag, String url, String message) {
            super(tag, "BADURL \"" + url.replace("\\", "\\\\").replace("\"", "\\\"") + '"', "APPEND failed: " + message, false);
        }
    }

    private String mURL;

    private String mUsername;
    private String mHostname;
    private short mPort;

    private ImapPath mPath;
    // private long mUidValidity;
    private int mUid;
    private ImapPartSpecifier mPart;

    ImapURL(String tag, ImapHandler handler, String url) throws ImapParseException {
        if (url == null || url.length() == 0)
            throw new ImapUrlException(tag, url, "blank/null IMAP URL");
        parse(tag, handler.getCredentials(), url);
        mURL = url;

        if (mPath == null || mPath.asZimbraPath().length() == 0) {
            if (handler.getState() != ImapHandler.State.SELECTED)
                throw new ImapUrlException(tag, url, "IMAP URL must specify folder if session not SELECTED");
            mPath = handler.getCurrentSession().getPath();
        }

        if (mUsername == null || mUsername.length() == 0) {
            if (handler.getState() == ImapHandler.State.NOT_AUTHENTICATED)
                throw new ImapUrlException(tag, url, "IMAP URL must specify user if session not AUTHENTICATED");
            mUsername = handler.getCredentials().getUsername();
            if (mPath != null && mPath.getOwner() != null) {
                try {
                    Account owner = mPath.getOwnerAccount();
                    if (owner != null)
                        mUsername = owner.getName();
                } catch (ServiceException e) {
                    throw new ImapUrlException(tag, url, "could not look up user: " + mPath.getOwner());
                }
            }
        }

        if (mHostname == null || mHostname.length() == 0) {
            try {
                Account acct = Provisioning.getInstance().get(AccountBy.name, mUsername);
                if (acct == null)
                    throw new ImapUrlException(tag, url, "unknown user: " + mUsername);
                mHostname = acct.getAttr(Provisioning.A_zimbraMailHost);
            } catch (ServiceException e) {
                throw new ImapUrlException(tag, url, "could not look up user: " + mUsername);
            }
        }
    }

    String getURL() { return mURL; }

    private void parse(String tag, ImapCredentials creds, String url) throws ImapParseException {
        String lcurl = url.toLowerCase();
        int pos = 0;
        // check to see if it's an absolute URL or a relative one...
        // imapurl = "imap://" iserver "/" [ icommand ]
        if (url.length() > 7 && lcurl.startsWith("imap://")) {
            // iserver = [iuserauth "@"] hostport
            pos += 7;
            int slash = url.indexOf('/', pos), ampersand = url.indexOf('@', pos);
            if (slash == -1 || slash == pos)
                throw new ImapUrlException(tag, url, "malformed IMAP URL");
            if (ampersand != -1 && ampersand < slash) {
                // iuserauth = enc_user [iauth] / [enc_user] iauth
                // iauth     = ";AUTH=" ( "*" / enc_auth_type )
                int semicolon = url.indexOf(';', pos);
                mUsername = urlDecode(url.substring(pos, semicolon != -1 && semicolon < ampersand ? semicolon : ampersand));
                // FIXME: need to support "iauth" production
                pos = ampersand + 1;
            }
            // hostport = host [ ":" port ]
            int colon = url.indexOf(':', pos);
            if (colon != -1 && colon < slash)
                try {
                    mPort = Short.parseShort(url.substring(colon + 1, slash));
                } catch (NumberFormatException nfe) { throw new ImapUrlException(tag, url, "invalid port: " + url.substring(colon + 1, slash)); }
            mHostname = url.substring(pos, colon != -1 && colon < slash ? colon : slash);
            pos = slash + 1;
        } else {
            if (url.charAt(0) != '/')
                throw new ImapUrlException(tag, url, "relative IMAP URLs must begin with '/'");
        }

        // icommand     = imailboxlist / imessagelist / imessagepart
        // imessagepart = enc_mailbox [uidvalidity] iuid [isection]
        // uidvalidity  = ";UIDVALIDITY=" nz_number
        // iuid         = "/;UID=" nz_number
        // isection     = "/;SECTION=" enc_section
        int iuid = lcurl.indexOf("/;uid=", pos), uvv = lcurl.indexOf(";uidvalidity=", pos);
        if (iuid == -1)
            throw new ImapUrlException(tag, url, "only \"imessagepart\"-type IMAP URLs supported");
        mPath = new ImapPath(urlDecode(url.substring(pos, uvv != -1 && uvv < iuid ? uvv : iuid)), creds);
        pos = iuid + 6;

        int isection = lcurl.indexOf("/;section=", pos);
        String uid = url.substring(pos, isection != -1 ? isection : url.length());
        try {
            mUid = Integer.parseInt(uid);
        } catch (NumberFormatException nfe) { throw new ImapUrlException(tag, url, "invalid uid in IMAP URL: " + uid); }

        if (isection != -1) {
            String section = urlDecode(url.substring(isection + 10));
            if (section.length() > 0) {
                try {
                    ImapRequest req = new ParserImapRequest(tag, section);
                    mPart = req.readPartSpecifier(false, false);
                    if (!req.eof())
                        throw new ImapUrlException(tag, url, "extra chars at end of IMAP URL SECTION");
                } catch (ImapParseException ipe) {
                    throw new ImapUrlException(tag, url, ipe.getMessage());
                } catch (IOException ioe) {
                    throw new ImapUrlException(tag, url, ioe.getMessage());
                }
            }
        }
    }

    private static class ParserImapRequest extends ImapRequest {
        public ParserImapRequest(String tag, String section) {
            super(null);
            mTag = tag;
            addPart(section);
        }

        private Literal getNextBuffer() throws ImapParseException {
            if ((mIndex + 1) >= mParts.size()) {
                throw new ImapParseException(mTag, "no next literal");
            }
            Literal literal = mParts.get(mIndex + 1).getLiteral();
            mIndex += 2;
            mOffset = 0;
            return literal;
        }

        @Override Literal readLiteral() throws ImapParseException {
            return getNextBuffer();
        }
    }

    private String urlDecode(String raw) {
        try {
            return URLDecoder.decode(raw, "utf-8");
        } catch (UnsupportedEncodingException uee) {
            return raw;
        }
    }

    public byte[] getContent(ImapHandler handler, ImapCredentials creds, String tag) throws ImapParseException {
        Pair<Long, InputStream> content = getContentAsStream(handler, creds, tag);
        try {
            return ByteUtil.getContent(content.getSecond(), (int) Math.min(content.getFirst(), Integer.MAX_VALUE));
        } catch (IOException e) {
            ZimbraLog.imap.info("error reading content from IMAP URL", e);
        }
        throw new ImapUrlException(tag, mURL, "error fetching IMAP URL content");
    }
    
    public Pair<Long, InputStream> getContentAsStream(ImapHandler handler, ImapCredentials creds, String tag) throws ImapParseException {
        ImapHandler.State state = handler.getState();
        if (state == ImapHandler.State.NOT_AUTHENTICATED)
            throw new ImapUrlException(tag, mURL, "must be in AUTHENTICATED state");

        try {
            Account acct = Provisioning.getInstance().get(AccountBy.name, mUsername);
            if (acct == null)
                throw new ImapUrlException(tag, mURL, "cannot find user: " + mUsername);

            ImapSession i4session = handler.getCurrentSession();
            OperationContext octxt = creds.getContext().setSession(i4session);
            Pair<Long, InputStream> content = null;
            // special-case the situation where the relevant folder is already SELECTed
            ImapFolder i4folder = handler.getSelectedFolder();
            if (state == ImapHandler.State.SELECTED && i4session != null && i4folder != null) {
                if (acct.getId().equals(i4session.getTargetAccountId()) && mPath.isEquivalent(i4folder.getPath())) {
                    ImapMessage i4msg = i4folder.getByImapId(mUid);
                    if (i4msg == null || i4msg.isExpunged())
                        throw new ImapUrlException(tag, mURL, "no such message");
                    MailItem item = i4folder.getMailbox().getItemById(octxt, i4msg.msgId, i4msg.getType());
                    content = ImapMessage.getContent(item);
                }
            }
            // if not, have to fetch by IMAP UID if we're local
            if (content == null && mPath.onLocalServer()) {
                Mailbox mbox = (Mailbox) mPath.getOwnerMailbox();
                MailItem item = mbox.getItemByImapId(octxt, mUid, mPath.asItemId().getId());
                if (!ImapMessage.SUPPORTED_TYPES.contains(item.getType()))
                    throw new ImapUrlException(tag, mURL, "no such message");
                content = ImapMessage.getContent(item);
            }
            // last option: handle off-server URLs
            if (content == null) {
                Account authacct = Provisioning.getInstance().get(AccountBy.id, creds.getAccountId());
                AuthToken auth = AuthProvider.getAuthToken(authacct, System.currentTimeMillis() + 60 * 1000);
                HashMap<String, String> params = new HashMap<String, String>();
                params.put(UserServlet.QP_IMAP_ID, Integer.toString(mUid));
                UserServlet.HttpInputStream is = UserServlet.getRemoteContentAsStream(auth, acct, mPath.asResolvedPath(), params);
                content = new Pair<Long, InputStream>((long) is.getContentLength(), is);
            }

            // fetch the content of the message
            if (mPart == null)
                return content;

            // and return the appropriate subpart of the selected message
            MimeMessage mm;
            try {
                mm = new Mime.FixedMimeMessage(JMSession.getSession(), content.getSecond());
            } finally {
                content.getSecond().close();
            }
            Pair<Long, InputStream> part = mPart.getContent(mm);
            if (part == null)
                throw new ImapUrlException(tag, mURL, "no such part");
            return part;

        } catch (NoSuchItemException e) {
            ZimbraLog.imap.info("no such message", e);
        } catch (ServiceException e) {
            ZimbraLog.imap.info("can't fetch content from IMAP URL", e);
        } catch (MessagingException e) {
            ZimbraLog.imap.info("can't fetch content from IMAP URL", e);
        } catch (IOException e) {
            ZimbraLog.imap.info("error reading content from IMAP URL", e);
        } catch (BinaryDecodingException e) {
            ZimbraLog.imap.info("can't fetch content from IMAP URL", e);
        }
        throw new ImapUrlException(tag, mURL, "error fetching IMAP URL content");
    }

    @Override public String toString() {
        try {
            return "imap://" + URLEncoder.encode(mUsername, "utf-8") + '@' + mHostname + (mPort > 0 ? ":" + mPort : "") +
                   '/' + URLEncoder.encode(mPath.asImapPath(), "utf-8") + "/;UID=" + mUid +
                   (mPart != null ? "/;SECTION=" + URLEncoder.encode(mPart.getSectionSpec(), "utf-8") : "");
        } catch (UnsupportedEncodingException e) {
            return "imap://" + mUsername + '@' + mHostname + (mPort > 0 ? ":" + mPort : "") +
                   '/' + mPath + "/;UID=" + mUid + (mPart != null ? "/;SECTION=" + mPart.getSectionSpec() : "");
        }
    }

    public static void main(String[] args) throws ImapParseException, ServiceException, IOException {
        Account acct = Provisioning.getInstance().get(AccountBy.name, "user1@macbeth.liquidsys.com");
        ImapCredentials creds = new ImapCredentials(acct, ImapCredentials.EnabledHack.NONE);
        ImapHandler handler = new TcpImapHandler(null).setCredentials(creds);
        handler.setSelectedFolder(new ImapPath("trash", creds), (byte) 0);

        System.out.println(new ImapURL("tag", handler, "/Drafts;UIDVALIDITY=385759045/;UID=20/;section=HEADER"));
        System.out.println(new ImapURL("tag", handler, "/;UID=20/;section=1.mime"));
        System.out.println(new ImapURL("tag", handler, "/INBOX;UIDVALIDITY=785799047/;UID=113330/;section=1.5.9"));
        System.out.println(new ImapURL("tag", handler, "imap://minbari.org:7143/\\\"gray-council\\\";UIDVALIDITY=385759045/;UID=20"));
        System.out.println(new ImapURL("tag", handler, "imap://bester;auth=gssapi@psicorp.org/~peter/%E6%97%A5%E6%9C%AC%E8%AA%9E/%E5%8F%B0%E5%8C%97/;UID=11916"));
        System.out.println(new ImapURL("tag", handler, "imap://;AUTH=*@minbari.org/gray-council/;uid=20/;section="));

        System.out.println(new ImapUrlException("tag", "\"\\\"", "msg").mCode);
    }
}
