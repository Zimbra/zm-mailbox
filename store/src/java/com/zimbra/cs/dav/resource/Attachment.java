/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;

import com.google.common.io.Closeables;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;

/**
 * Attachment is a DAV resource that represents email attachments.
 *
 * Using WebDAV client, email attachments in the mailbox can be browsed in
 * file manager like interface.  There are predefined phantom folders like
 * /attachment/by-type/image/today whose contents are dynamically updated
 * just like smart folders.  In those phantom folders the email attachments
 * are listed as individual files.
 *
 * @author jylee
 *
 */
public class Attachment extends PhantomResource {

    private byte[] mContent;

    public Attachment(String uri, String owner, long date, int contentLength) {
        this(uri, owner, parseUri(uri));
        setCreationDate(date);
        setLastModifiedDate(date);
        setProperty(DavElements.P_GETCONTENTLENGTH, Integer.toString(contentLength));
    }

    public Attachment(String uri, String owner, List<String> tokens, DavContext ctxt) throws DavException {
        this(uri, owner, tokens);
        String user = ctxt.getUser();
        Provisioning prov = Provisioning.getInstance();

        String name = tokens.get(tokens.size()-1);
        StringBuilder query = new StringBuilder();
        boolean needQuotes = (name.indexOf(' ') > 0);

        // XXX filename: query won't match the attachments in winmail.dat
        // bug 11406
        query.append("filename:");
        if (needQuotes)
            query.append("'");
        query.append(name);
        if (needQuotes)
            query.append("'");
        boolean found = false;
        try {
            Account account = prov.get(AccountBy.name, user);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            // if more than one attachments with the same name, take the first one.
            try (ZimbraQueryResults zqr = mbox.index.search(ctxt.getOperationContext(),
                query.toString(), SEARCH_TYPES, SortBy.NAME_ASC, 10)) {
                if (zqr.hasNext()) {
                    ZimbraHit hit = zqr.getNext();
                    if (hit instanceof MessageHit) {
                        Message message = ((MessageHit) hit).getMessage();
                        setCreationDate(message.getDate());
                        setLastModifiedDate(message.getChangeDate());
                        MimeMessage msg = message.getMimeMessage();
                        List<MPartInfo> parts = Mime.getParts(msg);
                        for (MPartInfo p : parts) {
                            String fname = p.getFilename();
                            if (name.equals(fname)) {
                                String partName = p.getPartName();
                                mContent = ByteUtil.getContent(Mime.getMimePart(msg, partName).getInputStream(), 0);
                                setProperty(DavElements.P_GETCONTENTLENGTH, Integer.toString(mContent.length));
                                setProperty(DavElements.P_GETCONTENTTYPE, p.getContentType());
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            ZimbraLog.dav.error("can't search for: attachment=" + name, e);
        }
        if (!found) {
            throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, null);
        }
    }

    public Attachment(String uri, String owner, List<String> tokens) {
        super(uri, owner, tokens);
    }

    @Override
    public InputStream getContent(DavContext ctxt) {
        return new ByteArrayInputStream(mContent);
    }

    @Override
    public boolean isCollection() {
        return false;
    }

}
