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
package com.zimbra.cs.mailbox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.wiki.WikiPage;
import com.zimbra.cs.wiki.WikiTemplate;

public class MigrateToDocuments {

    private Mailbox mbox;
    private OperationContext octxt;
    
    public void handleAccount(Account account) throws ServiceException {
        handleMailbox(MailboxManager.getInstance().getMailboxByAccount(account, true));
    }
    public void handleMailbox(Mailbox mbox) throws ServiceException {
        this.mbox = mbox;
        octxt = new OperationContext(mbox);
        Folder root = mbox.getFolderByPath(octxt, "/");
        String path = "/.migrate-wiki";
        Folder destRoot = null;
        try {
            destRoot = mbox.getFolderByPath(octxt, path);
        } catch (Exception e) {
        }
        if (destRoot == null)
            destRoot = mbox.createFolder(octxt, path, (byte)0, MailItem.Type.DOCUMENT);
        if (destRoot == null) {
            ZimbraLog.misc.warn("Can't create folder: %s", path);
            return;
        }
        moveToBackupFolder(root, destRoot);
        migrateFromBackupFolder(octxt, destRoot, root);
        mbox.delete(octxt, destRoot.getId(), MailItem.Type.FOLDER);
    }
    
    private void moveToBackupFolder(Folder from, Folder to) throws ServiceException {
        for (Folder source : from.getSubfolders(octxt)) {
            if (source.getDefaultView() != MailItem.Type.WIKI)
                continue;
            String path = to.getPath() + "/" + source.getName();
            Folder dest = null;
            try {
                dest = mbox.createFolder(octxt, path, (byte)0, MailItem.Type.DOCUMENT);
            } catch (MailServiceException e) {
                if (e.getCode().equals(MailServiceException.ALREADY_EXISTS)) {
                    dest = mbox.getFolderByPath(octxt, path);
                    ZimbraLog.misc.warn("Backup folder already exists: %s", source.getName());
                } else {
                    ZimbraLog.misc.warn("Can't create backup folder: %s", path);
                    continue;
                }
            }
            moveToBackupFolder(source, dest);
        }
        for (MailItem item : mbox.getItemList(octxt, MailItem.Type.WIKI, from.getId())) {
            try {
                mbox.move(octxt, item.getId(), MailItem.Type.WIKI, to.getId());
            } catch (MailServiceException e) {
                if (e.getCode().equals(MailServiceException.ALREADY_EXISTS)) {
                    ZimbraLog.misc.warn("Item already exists: %s", item.getName());
                } else {
                    ZimbraLog.misc.warn("Can't move item %s to backup folder %s", item.getName(), to.getPath());
                }
            }
        }
    }
    
    private void migrateFromBackupFolder(OperationContext octxt, Folder from, Folder to) throws ServiceException {
        for (Folder source : from.getSubfolders(octxt)) {
            String path = to.getPath();
            if (!path.endsWith("/"))
                path += "/";
            path += source.getName();
            Folder sub = mbox.getFolderByPath(octxt, path);
            migrateFromBackupFolder(octxt, source, sub);
        }
        if (to.getDefaultView() == MailItem.Type.WIKI)
            mbox.migrateFolderView(octxt, to, MailItem.Type.DOCUMENT);
        for (MailItem item : mbox.getItemList(octxt, MailItem.Type.WIKI, from.getId())) {
            Document doc = (Document) item;
            Document main = null;
            try {
                main = (Document) mbox.getItemByPath(octxt, to.getPath() + "/" + doc.getName());
            } catch (Exception e) {
                ZimbraLog.misc.info("Creating new item: " + doc.getName());
            }
            for (int rev = 1; rev < doc.getVersion(); rev++) {
                Document revision = null;
                try {
                    revision = (Document)mbox.getItemRevision(octxt, item.getId(), MailItem.Type.DOCUMENT, rev);
                } catch (Exception e) {
                    ZimbraLog.misc.warn("Can't get revision " + rev + " for item " + doc.getName(), e);
                }
                if (revision == null) {
                    ZimbraLog.misc.warn("Empty revision " + rev + " for item " + doc.getName());
                    continue;
                }
                // name comes from the current revision
                main = addRevision(item.getName(), main, revision, to);
            }
            // add the current revision
            addRevision(item.getName(), main, doc, to);
        }
    }
    
    private Document addRevision(String name, Document main, Document revision, Folder to) {
        InputStream in = null;
        try {
            in = getContentStream(revision);
            String contentType = revision.getContentType();
            if (revision.getType() == MailItem.Type.WIKI)
                contentType = "application/x-zimbra-doc; charset=utf-8";
            ParsedDocument pd = new ParsedDocument(in, name, contentType, revision.getDate(), revision.getCreator(), revision.getDescription());
            if (main == null) {
                main = mbox.createDocument(octxt, to.getId(), pd, MailItem.Type.DOCUMENT);
            } else {
                mbox.addDocumentRevision(octxt, main.getId(), pd);
            }
        } catch (Exception e) {
            ZimbraLog.misc.warn("Can't add new revision for " + name + " revision " + revision.getVersion(), e);
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (Exception e) {}
        }
        return main;
    }
    
    private InputStream getContentStream(Document item) throws IOException, ServiceException {
        if (item.getType() == MailItem.Type.DOCUMENT)
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
