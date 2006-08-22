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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.imap.ImapPartSpecifier.BinaryDecodingException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.ZimbraLog;

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

    private String mFolder;
    // private long mUidValidity;
    private int mUid;
    private ImapPartSpecifier mPart;

    ImapURL(String tag, ImapSession session, String url) throws ImapParseException {
        if (url == null || url.length() == 0)
            throw new ImapUrlException(tag, url, "blank/null IMAP URL");
        parse(tag, url);
        mURL = url;

        if (mUsername == null || mUsername.length() == 0) {
            if (ImapSession.getState(session) == ImapSession.STATE_NOT_AUTHENTICATED)
                throw new ImapUrlException(tag, url, "IMAP URL must specify user if session not AUTHENTICATED");
            mUsername = session.getUsername();
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

        if (mFolder == null || mFolder.length() == 0) {
            if (ImapSession.getState(session) != ImapSession.STATE_SELECTED)
                throw new ImapUrlException(tag, url, "IMAP URL must specify folder if session not SELECTED");
            mFolder = session.getFolder().getPath();
        } else if (mFolder.startsWith("/") && mFolder.length() > 1) {
            mFolder = mFolder.substring(1);
        }
    }

    String getURL() { return mURL; }

    private void parse(String tag, String url) throws ImapParseException {
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
        mFolder = urlDecode(url.substring(pos, uvv != -1 && uvv < iuid ? uvv : iuid));
        pos = iuid + 6;

        int isection = lcurl.indexOf("/;section=", pos);
        String uid = url.substring(pos, isection != -1 ? isection : url.length());
        try {
            mUid = Integer.parseInt(uid);
        } catch (NumberFormatException nfe) { throw new ImapUrlException(tag, url, "invalid uid in IMAP URL: " + uid); }

        if (isection != -1) {
            String section = urlDecode(url.substring(isection + 10));
            if (section.length() > 0) {
                List<Object> list = new ArrayList<Object>();
                list.add(section);
                try {
                    OzImapRequest req = new OzImapRequest(tag, list, null);
                    mPart = req.readPartSpecifier(false, false);
                    if (!req.eof())
                        throw new ImapUrlException(tag, url, "extra chars at end of IMAP URL SECTION");
                } catch (ImapParseException ipe) {
                    throw new ImapUrlException(tag, url, ipe.getMessage());
                }
            }
        }
    }

    private String urlDecode(String raw) {
        try {
            return URLDecoder.decode(raw, "utf-8");
        } catch (UnsupportedEncodingException uee) {
            return raw;
        }
    }

    public byte[] getContent(ImapSession session, String tag) throws ImapParseException {
        byte state = ImapSession.getState(session);
        if (state == ImapSession.STATE_NOT_AUTHENTICATED)
            throw new ImapUrlException(tag, mURL, "must be in AUTHENTICATED state");

        try {
            Account acct = Provisioning.getInstance().get(AccountBy.name, mUsername);
            if (acct == null)
                throw new ImapUrlException(tag, mURL, "cannot find user: " + mUsername);

            OperationContext octxt = session.getContext();
            byte[] content = null;
            // special-case the situation where the relevant folder is already SELECTed
            if (acct.getId().equals(session.getAccountId()) && state == ImapSession.STATE_SELECTED) {
                ImapFolder i4folder = session.getFolder();
                if (i4folder != null && mFolder.toLowerCase().equals(i4folder.getPath().toLowerCase())) {
                    ImapMessage i4msg = i4folder.getByImapId(mUid);
                    if (i4msg == null || i4msg.isExpunged())
                        throw new ImapUrlException(tag, mURL, "no such message");
                    MailItem item = session.getMailbox().getItemById(octxt, i4msg.msgId, i4msg.getType());
                    content = ImapMessage.getContent(item);
                }
            }
            // if not, have to fetch by IMAP UID if we're local
            if (content == null && Provisioning.onLocalServer(acct)) {
                Mailbox mbox = Mailbox.getMailboxByAccount(acct);
                Folder folder = mbox.getFolderByPath(octxt, mFolder);
                MailItem item = mbox.getItemByImapId(octxt, mUid, folder.getId());
                if (item.getType() != MailItem.TYPE_MESSAGE && item.getType() != MailItem.TYPE_CONTACT)
                    throw new ImapUrlException(tag, mURL, "no such message");
                content = ImapMessage.getContent(item);
            }
            // last option: handle off-server URLs
            if (content == null) {
                Account authacct = Provisioning.getInstance().get(AccountBy.id, session.getAccountId());
                AuthToken auth = new AuthToken(authacct, System.currentTimeMillis() + 60 * 1000);
                HashMap<String, String> params = new HashMap<String, String>();
                params.put(UserServlet.QP_IMAP_ID, Integer.toString(mUid));
                content = UserServlet.getRemoteContent(auth, acct, mFolder, params);
            }

            // fetch the content of the message
            if (mPart == null)
                return content;

            // and return the appropriate subpart of the selected message
            MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new ByteArrayInputStream(content));
            byte[] part = mPart.getContent(mm);
            if (part == null)
                throw new ImapUrlException(tag, mURL, "no such part");
            return part;

        } catch (NoSuchItemException e) {
            ZimbraLog.imap.info("no such message", e);
            throw new ImapUrlException(tag, mURL, "error fetching IMAP URL content");
        } catch (ServiceException e) {
            ZimbraLog.imap.info("can't fetch content from IMAP URL", e);
            throw new ImapUrlException(tag, mURL, "error fetching IMAP URL content");
        } catch (MessagingException e) {
            ZimbraLog.imap.info("can't fetch content from IMAP URL", e);
            throw new ImapUrlException(tag, mURL, "error fetching IMAP URL content");
        } catch (BinaryDecodingException e) {
            ZimbraLog.imap.info("can't fetch content from IMAP URL", e);
            throw new ImapUrlException(tag, mURL, "error fetching IMAP URL content");
        }
    }

    public String toString() {
        try {
            return "imap://" + URLEncoder.encode(mUsername, "utf-8") + '@' + mHostname + (mPort > 0 ? ":" + mPort : "") +
                   '/' + URLEncoder.encode(mFolder, "utf-8") + "/;UID=" + mUid +
                   (mPart != null ? "/;SECTION=" + URLEncoder.encode(mPart.getSectionSpec(), "utf-8") : "");
        } catch (UnsupportedEncodingException e) {
            return "imap://" + mUsername + '@' + mHostname + (mPort > 0 ? ":" + mPort : "") +
                   '/' + mFolder + "/;UID=" + mUid + (mPart != null ? "/;SECTION=" + mPart.getSectionSpec() : "");
        }
    }

    public static void main(String[] args) throws ImapParseException, ServiceException {
        Account acct = Provisioning.getInstance().get(AccountBy.name, "user1@macbeth.liquidsys.com");
        ImapSession session = new ImapSession(acct.getId(), "test");
        session.setUsername(acct.getName());
        session.selectFolder(new ImapFolder("trash", true, session.getMailbox(), session));

        System.out.println(new ImapURL("tag", session, "/Drafts;UIDVALIDITY=385759045/;UID=20/;section=HEADER"));
        System.out.println(new ImapURL("tag", session, "/;UID=20/;section=1.mime"));
        System.out.println(new ImapURL("tag", session, "/INBOX;UIDVALIDITY=785799047/;UID=113330/;section=1.5.9"));
        System.out.println(new ImapURL("tag", session, "imap://minbari.org:7143/\\\"gray-council\\\";UIDVALIDITY=385759045/;UID=20"));
        System.out.println(new ImapURL("tag", session, "imap://bester;auth=gssapi@psicorp.org/~peter/%E6%97%A5%E6%9C%AC%E8%AA%9E/%E5%8F%B0%E5%8C%97/;UID=11916"));
        System.out.println(new ImapURL("tag", session, "imap://;AUTH=*@minbari.org/gray-council/;uid=20/;section="));

        System.out.println(new ImapUrlException("tag", "\"\\\"", "msg").mCode);
    }
}
