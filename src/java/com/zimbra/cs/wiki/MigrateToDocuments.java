/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.wiki;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.util.Zimbra;

public class MigrateToDocuments {

    private Mailbox mbox;
    private OperationContext octxt;
    
    public void handleAccount(Account account) throws ServiceException {
        mbox = MailboxManager.getInstance().getMailboxByAccount(account, true);
        octxt = new OperationContext(account);
        Folder root = mbox.getFolderByPath(octxt, "/");
        String path = "/migrate-";
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        path += fmt.format(new Date());
        Folder destRoot = mbox.createFolder(octxt, path, (byte)0, MailItem.TYPE_DOCUMENT);
        handleFolder(root, destRoot);
    }
    
    private void handleFolder(Folder from, Folder to) throws ServiceException {
        for (Folder source : from.getSubfolders(octxt)) {
            if (source.getDefaultView() != MailItem.TYPE_WIKI)
                continue;
            String path = to.getPath() + "/" + source.getName();
            try {
                Folder dest = mbox.createFolder(octxt, path, (byte)0, MailItem.TYPE_DOCUMENT);
                handleFolder(source, dest);
            } catch (Exception e) {
                ZimbraLog.misc.warn("Can't migrate folder " + source.getName(), e);
            }
        }
        for (MailItem item : mbox.getItemList(octxt, MailItem.TYPE_DOCUMENT, from.getId())) {
            Document doc = (Document) item;
            Document main = null;
            for (int rev = 1; rev < doc.getVersion(); rev++) {
                Document revision = null;
                try {
                    revision = (Document)mbox.getItemRevision(octxt, item.getId(), MailItem.TYPE_DOCUMENT, rev);
                } catch (Exception e) {
                    ZimbraLog.misc.warn("Can't get revision " + rev + " for item " + doc.getName(), e);
                }
                if (revision == null) {
                    ZimbraLog.misc.warn("Empty revision " + rev + " for item " + doc.getName());
                    continue;
                }
                main = addRevision(main, revision, to);
            }
            // add the current revision
            addRevision(main, doc, to);
        }
    }

    private Document addRevision(Document main, Document revision, Folder to) {
        InputStream in = null;
        try {
            in = getContentStream(revision);
            String contentType = revision.getContentType();
            if (revision.getType() == MailItem.TYPE_WIKI)
                contentType = "application/x-zimbra-doc; charset=utf-8";
            if (main == null) {
                try {
                    ParsedDocument pd = new ParsedDocument(in, revision.getName(), contentType, revision.getDate(), revision.getCreator(), revision.getDescription());
                    main = mbox.createDocument(octxt, to.getId(), pd, MailItem.TYPE_DOCUMENT);
                } catch (Exception e) {
                    ZimbraLog.misc.warn("Can't create document " + revision.getName() + " in " + to.getName(), e);
                }
                return main;
            }
            try {
                ParsedDocument pd = new ParsedDocument(in, revision.getName(), contentType, revision.getDate(), revision.getCreator(), revision.getDescription());
                mbox.addDocumentRevision(octxt, main.getId(), pd);
            } catch (Exception e) {
                ZimbraLog.misc.warn("Can't create revision " + revision.getName() + " in " + to.getName(), e);
            }
        } catch (Exception e) {
            ZimbraLog.misc.warn("Can't get content for " + revision.getName(), e);
            return main;
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (Exception e) {}
        }
        return main;
    }
    
    private InputStream getContentStream(Document item) throws IOException, ServiceException {
        if (item.getType() == MailItem.TYPE_DOCUMENT)
            return item.getContentStream();
        String contents = new String(item.getContent(), "UTF-8");
        WikiTemplate wt = new WikiTemplate(contents, mbox.getAccountId(), Integer.toString(item.getFolderId()), item.getName());
        WikiTemplate.Context wctxt = new WikiTemplate.Context(new WikiPage.WikiContext(octxt, null), item, wt);
        return new ByteArrayInputStream(wt.toString(wctxt).getBytes("UTF-8"));
    }

    private static void usage() {
        System.out.println("zmwikimigrate [accountId]+");
        System.exit(0);
    }
    
    public static void main(String[] args) throws Exception {
        CliUtil.toolSetup();
        DbPool.startup();
        Zimbra.startupCLI();
        if (args.length == 0) {
            usage();
        }
        MigrateToDocuments migrate = new MigrateToDocuments();
        for (String arg : args) {
            Account account = Provisioning.getInstance().getAccountByName(arg);
            if (account == null) {
                System.out.println("Can't get account " + arg);
                continue;
            }
            migrate.handleAccount(account);
        }
    }

}
