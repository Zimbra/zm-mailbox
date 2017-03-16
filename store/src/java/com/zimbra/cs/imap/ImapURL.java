/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.mailbox.ZimbraMailItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.InputStreamWithSize;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.imap.ImapPartSpecifier.BinaryDecodingException;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.util.JMSession;

/**
 * See https://tools.ietf.org/html/rfc5092 - IMAP URL Scheme
 */
final class ImapURL {
    private static class ImapUrlException extends ImapParseException {
        private static final long serialVersionUID = 174398702563521440L;

        ImapUrlException(String tag, String url, String message) {
            super(tag, "BADURL \"" + url.replace("\\", "\\\\").replace("\"", "\\\"") + '"', "APPEND failed: " + message, false);
        }
    }

    private final String mURL;

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
            mPath = handler.getCurrentImapListener().getPath();
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
                mHostname = acct.getMailHost();
            } catch (ServiceException e) {
                throw new ImapUrlException(tag, url, "could not look up user: " + mUsername);
            }
        }
    }

    String getURL() {
        return mURL;
    }

    private void parse(String tag, ImapCredentials creds, String url) throws ImapParseException {
        String lcurl = url.toLowerCase();
        int pos = 0;
        // check to see if it's an absolute URL or a relative one...
        // imapurl = "imap://" iserver "/" [ icommand ]
        if (url.length() > 7 && lcurl.startsWith("imap://")) {
            // iserver = [iuserauth "@"] hostport
            pos += 7;
            int slash = url.indexOf('/', pos), ampersand = url.indexOf('@', pos);
            if (slash == -1 || slash == pos) {
                throw new ImapUrlException(tag, url, "malformed IMAP URL");
            }
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
            if (colon != -1 && colon < slash) {
                try {
                    mPort = Short.parseShort(url.substring(colon + 1, slash));
                } catch (NumberFormatException nfe) {
                    throw new ImapUrlException(tag, url, "invalid port: " + url.substring(colon + 1, slash));
                }
            }
            mHostname = url.substring(pos, colon != -1 && colon < slash ? colon : slash);
            pos = slash + 1;
        } else {
            if (url.charAt(0) != '/') {
                throw new ImapUrlException(tag, url, "relative IMAP URLs must begin with '/'");
            }
        }

        // icommand     = imailboxlist / imessagelist / imessagepart
        // imessagepart = enc_mailbox [uidvalidity] iuid [isection]
        // uidvalidity  = ";UIDVALIDITY=" nz_number
        // iuid         = "/;UID=" nz_number
        // isection     = "/;SECTION=" enc_section
        int iuid = lcurl.indexOf("/;uid=", pos), uvv = lcurl.indexOf(";uidvalidity=", pos);
        if (iuid == -1) {
            throw new ImapUrlException(tag, url, "only \"imessagepart\"-type IMAP URLs supported");
        }
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
                    if (!req.eof()) {
                        throw new ImapUrlException(tag, url, "extra chars at end of IMAP URL SECTION");
                    }
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
            this.tag = tag;
            addPart(section);
        }

        private Literal getNextBuffer() throws ImapParseException {
            if ((index + 1) >= parts.size()) {
                throw new ImapParseException(tag, "no next literal");
            }
            Literal literal = parts.get(index + 1).getLiteral();
            index += 2;
            offset = 0;
            return literal;
        }

        @Override
        Literal readLiteral() throws ImapParseException {
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

    public byte[] getContent(ImapHandler handler, ImapCredentials creds, String tag) throws ImapException {
        InputStreamWithSize content = getContentAsStream(handler, creds, tag);
        try {
            return ByteUtil.getContent(content.stream, (int) Math.min(content.size, Integer.MAX_VALUE));
        } catch (IOException e) {
            ZimbraLog.imap.info("error reading content from IMAP URL", e);
        }
        throw new ImapUrlException(tag, mURL, "error fetching IMAP URL content");
    }

    public InputStreamWithSize getContentAsStream(ImapHandler handler, ImapCredentials creds, String tag)
            throws ImapException {
        ImapHandler.State state = handler.getState();
        if (state == ImapHandler.State.NOT_AUTHENTICATED) {
            throw new ImapUrlException(tag, mURL, "must be in AUTHENTICATED state");
        }
        try {
            Account acct = Provisioning.getInstance().get(AccountBy.name, mUsername);
            if (acct == null) {
                throw new ImapUrlException(tag, mURL, "cannot find user: " + mUsername);
            }
            ImapListener i4session = handler.getCurrentImapListener();
            OperationContext octxt = creds.getContext().setSession(i4session);
            InputStreamWithSize content = null;
            // special-case the situation where the relevant folder is already SELECTed
            ImapFolder i4folder = handler.getSelectedFolder();
            if (state == ImapHandler.State.SELECTED && i4session != null && i4folder != null) {
                if (acct.getId().equals(i4session.getTargetAccountId()) && mPath.isEquivalent(i4folder.getPath())) {
                    ImapMessage i4msg = i4folder.getByImapId(mUid);
                    if (i4msg == null || i4msg.isExpunged()) {
                        throw new ImapUrlException(tag, mURL, "no such message");
                    }
                    MailboxStore i4Mailbox = i4folder.getMailbox();
                    ZimbraMailItem item = i4Mailbox.getItemById(octxt, ItemIdentifier.fromAccountIdAndItemId(i4Mailbox.getAccountId(), i4msg.msgId), i4msg.getMailItemType());
                    content = ImapMessage.getContent(item);
                }
            }
            // if not, have to fetch by IMAP UID if we're local or handle off-server URLs
            if (content == null) {
                ImapMailboxStore mbox = mPath.getOwnerImapMailboxStore();
                content = mbox.getByImapId(octxt, mUid, mPath.getFolder().getFolderIdAsString(), mPath.asResolvedPath());
                if (null == content) {
                    throw new ImapUrlException(tag, mURL, "no such message");
                }
            }

            // fetch the content of the message
            if (mPart == null) {
                return content;
            }
            // and return the appropriate subpart of the selected message
            MimeMessage mm;
            try {
                mm = new Mime.FixedMimeMessage(JMSession.getSession(), content.stream);
            } finally {
                content.stream.close();
            }
            InputStreamWithSize part = mPart.getContent(mm);
            if (part == null) {
                throw new ImapUrlException(tag, mURL, "no such part");
            }
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

    @Override
    public String toString() {
        try {
            return "imap://" + URLEncoder.encode(mUsername, "utf-8") + '@' + mHostname + (mPort > 0 ? ":" + mPort : "") +
                   '/' + URLEncoder.encode(mPath.asImapPath(), "utf-8") + "/;UID=" + mUid +
                   (mPart != null ? "/;SECTION=" + URLEncoder.encode(mPart.getSectionSpec(), "utf-8") : "");
        } catch (UnsupportedEncodingException e) {
            return "imap://" + mUsername + '@' + mHostname + (mPort > 0 ? ":" + mPort : "") +
                   '/' + mPath + "/;UID=" + mUid + (mPart != null ? "/;SECTION=" + mPart.getSectionSpec() : "");
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mHostname == null) ? 0 : mHostname.hashCode());
        result = prime * result + ((mPart == null) ? 0 : mPart.hashCode());
        result = prime * result + ((mPath == null) ? 0 : mPath.hashCode());
        result = prime * result + mPort;
        result = prime * result + ((mURL == null) ? 0 : mURL.hashCode());
        result = prime * result + mUid;
        result = prime * result + ((mUsername == null) ? 0 : mUsername.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ImapURL other = (ImapURL) obj;
        if (mHostname == null) {
            if (other.mHostname != null) {
                return false;
            }
        } else if (!mHostname.equals(other.mHostname)) {
            return false;
        }
        if (mPart == null) {
            if (other.mPart != null) {
                return false;
            }
        } else if (!mPart.equals(other.mPart)) {
            return false;
        }
        if (mPath == null) {
            if (other.mPath != null) {
                return false;
            }
        } else if (!mPath.equals(other.mPath)) {
            return false;
        }
        if (mPort != other.mPort) {
            return false;
        }
        if (mURL == null) {
            if (other.mURL != null) {
                return false;
            }
        } else if (!mURL.equals(other.mURL)) {
            return false;
        }
        if (mUid != other.mUid) {
            return false;
        }
        if (mUsername == null) {
            if (other.mUsername != null) {
                return false;
            }
        } else if (!mUsername.equals(other.mUsername)) {
            return false;
        }
        return true;
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
