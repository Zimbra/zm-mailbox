/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.callback.CallbackUtil;
import com.zimbra.cs.mailbox.Folder.FolderOptions;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.Config;
import com.zimbra.cs.util.Zimbra;

public class ContactBackupThread extends Thread {
    private static final String OPERATION = "ContactBackup";
    private static volatile ContactBackupThread backupThread = null;
    private static Object THREAD_LOCK = new Object();
    private static boolean shutdownRequested = false;
    private static final String CT_TYPE = "application/x-compressed-tar";
    private static final String CONTACT_RES_URL = "?fmt=tgz&types=contact";
    private static final String FILE_NAME = "Contacts";
    private static final String FILE_DESC = "Contact Backup at ";
    private static final String DATE_FORMAT = "yyyy-MM-dd-HHmmss";
    private boolean success = true;

    private ContactBackupThread() {
        setName(OPERATION);
    }

    public static void startup() {
        synchronized(THREAD_LOCK) {
            if (isRunning()) {
                ZimbraLog.contactbackup.warn("can not start another thread");
                return;
            }
            backupThread = new ContactBackupThread();
            shutdownRequested = false;
            backupThread.start();
        }
    }

    public static void shutdown() {
        synchronized(THREAD_LOCK) {
            if (backupThread != null) {
                shutdownRequested = true;
                backupThread.interrupt();
                backupThread = null;
                ZimbraLog.contactbackup.debug("shutdown done");
            } else {
                ZimbraLog.contactbackup.debug("shutdown requested but %s is not running", OPERATION);
            }
        }
    }

    public static synchronized boolean isRunning() {
        synchronized(THREAD_LOCK) {
            return backupThread != null;
        }
    }

    private static void setContactBackupLastMailboxId(int id) {
        try {
            Config.setInt(Config.CONTACT_BACKUP_LAST_MAILBOX_ID, id);
            ZimbraLog.contactbackup.debug("setting contact backup last mailbox id with %d", id);
        } catch (ServiceException se) {
            ZimbraLog.contactbackup.warn("exception occured while setting contact backup last mailbox id with %d", id);
            ZimbraLog.contactbackup.debug(se);
        }
    }

    /**
     * Iterate over list of mailbox ids and start backup on each one of them.
     * Sleep for zimbraFeatureContactBackupFrequency once the thread cycle is over.
     */
    @Override
    public void run() {
        List<Integer> mailboxIds = new ArrayList<Integer>();
        try {
            mailboxIds = CallbackUtil.getSortedMailboxIdList();
        } catch (ServiceException e) {
            ZimbraLog.contactbackup.warn("can not get list of mailboxes, shutting down thread.");
            ZimbraLog.contactbackup.debug(e);
            backupThread = null;
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("mailboxId,email,contact backup").append("\n");
        Date startTime = new Date();
        ZimbraLog.contactbackup.debug("starting iteration on mailboxes at %s", startTime.toString());
        for (Integer mailboxId : mailboxIds) {
            if (shutdownRequested) {
                ZimbraLog.contactbackup.info("shutting down thread.");
                return;
            }
            ZimbraLog.contactbackup.debug("starting to work with mailbox %d", mailboxId);
            ZimbraLog.addMboxToContext(mailboxId);
            success = true;
            try {
                Mailbox mbox = MailboxManager.getInstance().getMailboxById(mailboxId);
                sb.append(mailboxId).append(",");
                Account account = mbox.getAccount();
                sb.append(account.getMail()).append(",");
                ZimbraLog.addAccountNameToContext(account.getName());
                if (account.isFeatureContactBackupEnabled() && !account.isIsSystemAccount() && !account.isIsSystemResource() && account.isAccountStatusActive()) {
                    OperationContext octxt = new OperationContext(account);
                    Folder folder = getContactBackupFolder(octxt, mbox, true);
                    if (folder != null) {
                        createBackup(octxt, mbox, account, folder, startTime);
                        purgeOldBackups(octxt, mbox, folder, startTime);
                    } else {
                        success = false;
                        ZimbraLog.contactbackup.info("contact backup folder not found for %d, continuing to next mailbox", mailboxId);
                    }
                    // set current mailbox id as last processed mailbox
                    if (success) {
                        setContactBackupLastMailboxId(mailboxId);
                        sb.append("success");
                    }
                } else {
                    ZimbraLog.contactbackup.debug("contact backup skipped for %d: feature is disabled/account is inactive/it's a system account", mailboxId);
                    sb.append("skipped");
                }
            } catch (Exception e) {
                ZimbraLog.contactbackup.warn("backup/purge failed for mailbox %d, continuing to next mailbox", mailboxId);
                ZimbraLog.contactbackup.debug(e);
                sb.append("failed");
            }
            sb.append("\n");
            ZimbraLog.clearContext();
        }
        setContactBackupLastMailboxId(0);
        createReport(sb, startTime);
        long endTime = System.currentTimeMillis();
        long diff = endTime - startTime.getTime();
        ZimbraLog.contactbackup.debug("finished iteration on mailboxes, iteration took %d ms", diff);
        ContactBackupThread.shutdown();
    }

    private void createBackup(OperationContext octxt, Mailbox mbox, Account account, Folder folder, Date startTime) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        StringBuilder filename = new StringBuilder();
        filename.append(FILE_NAME).append("-").append(sdf.format(date));
        InputStream is = null;
        ZimbraAuthToken token = null;
        try {
            token = new ZimbraAuthToken(account);
            ZMailbox.Options zoptions = new ZMailbox.Options(token.toZAuthToken(), AccountUtil.getSoapUri(account));
            zoptions.setNoSession(true);
            zoptions.setTargetAccount(account.getId());
            zoptions.setTargetAccountBy(AccountBy.id);
            ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
            is = zmbx.getRESTResource(CONTACT_RES_URL);
            ParsedDocument pd = new ParsedDocument(is, filename.toString(), CT_TYPE, startTime.getTime(), OPERATION, FILE_DESC + startTime.toString());
            Document doc = mbox.createDocument(octxt, folder.getId(), pd, MailItem.Type.DOCUMENT, 0);
            ZimbraLog.contactbackup.debug("contact backup created size %d bytes", doc.getSize());
        } catch (UnsupportedOperationException | IOException | ServiceException exception) {
            success = false;
            ZimbraLog.contactbackup.warn("contact export failed, continuing to next mailbox");
            ZimbraLog.contactbackup.debug(exception);
        } catch (OutOfMemoryError e) {
            Zimbra.halt("OutOfMemoryError while creating contact backup", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    ZimbraLog.contactbackup.debug("IOExcepion occured while closing stream", ioe);
                }
            }
            if (token != null) {
                try {
                    token.deRegister();
                 } catch (AuthTokenException e) {
                     ZimbraLog.contactbackup.warn("failed to deregister token");
                     ZimbraLog.contactbackup.debug(e);
                 }
             }
        }
    }

    private void purgeOldBackups(OperationContext octxt, Mailbox mbox, Folder folder, Date startTime) {
        long lifeTime = CallbackUtil.getTimeInterval(Provisioning.A_zimbraFeatureContactBackupLifeTime, 0);
        long cutoff = startTime.getTime() - lifeTime;
        TypedIdList list = null;
        try {
            list = mbox.getItemIds(octxt, folder.getId());
        } catch (ServiceException se) {
            ZimbraLog.contactbackup.warn("exception occured while getting list of contact backups");
            ZimbraLog.contactbackup.debug(se);
            success = false;
            return;
        }
        if (!list.isEmpty()) {
            int counter = 0;
            for (Integer id : list.getAllIds()) {
                try (final MailboxLock l = mbox.lock(true);
                     final Mailbox.MailboxTransaction t = mbox.new MailboxTransaction(OPERATION, octxt, l)) {
                    MailItem item = mbox.getItemById(id, MailItem.Type.DOCUMENT);
                    t.commit();
                    if (item.getDate() < cutoff) {
                        try {
                            ZimbraLog.contactbackup.debug("deleting item with id: %s", item.getId());
                            mbox.delete(octxt, item.getId(), MailItem.Type.DOCUMENT);
                            counter++;
                        } catch (ServiceException se) {
                            success = false;
                            ZimbraLog.contactbackup.warn("failed to delete item (id=%d) from contact backup folder", item.getId());
                            ZimbraLog.contactbackup.debug(se);
                        }
                    }
                } catch (ServiceException se) {
                    success = false;
                    ZimbraLog.contactbackup.warn("exception occured while getting document from contact backup folder");
                    ZimbraLog.contactbackup.debug(se);
                    continue;
                }
            }
            ZimbraLog.contactbackup.debug("%d items deleted", counter);
        } else {
            ZimbraLog.contactbackup.debug("No items found in contact backup folder");
        }
    }

    private Folder getContactBackupFolder(OperationContext octxt, Mailbox mbox, boolean createIfDontExist) {
        Folder folder = null;
        try {
            folder = mbox.getFolderByName(octxt, Mailbox.ID_FOLDER_BRIEFCASE,
                    MailConstants.A_CONTACTS_BACKUP_FOLDER_NAME);
        } catch (ServiceException se) {
            if (se.getCode().equals(MailServiceException.NO_SUCH_FOLDER) && createIfDontExist) {
                ZimbraLog.contactbackup.debug("contact backup folder does not exist, trying to create new one");
                FolderOptions opts = new FolderOptions();
                opts.setDefaultView(Type.FOLDER);
                byte hidden = Folder.FOLDER_IS_IMMUTABLE | Folder.FOLDER_DONT_TRACK_COUNTS;
                opts.setAttributes(hidden);
                try {
                    folder = mbox.createFolder(octxt, MailConstants.A_CONTACTS_BACKUP_FOLDER_NAME, Mailbox.ID_FOLDER_BRIEFCASE, opts);
                    ZimbraLog.contactbackup.debug("contact backup folder created");
                } catch (ServiceException se2) {
                    ZimbraLog.contactbackup.warn("failed to create contact backup folder");
                    ZimbraLog.contactbackup.debug(se2);
                }
            } else {
                ZimbraLog.contactbackup.warn("exception occured while getting contact backup folder");
                ZimbraLog.contactbackup.debug(se);
            }
        }
        return folder;
    }

    private void createReport(StringBuilder builder, Date startTime) {
        File folderLog = new File(LC.zimbra_log_directory.value());
        if (!folderLog.exists() || !folderLog.isDirectory() || !folderLog.canWrite()) {
            ZimbraLog.contactbackup.debug("Failed to find log folder to save contact backup report.");
            return;
        }
        File reportFile = new File(folderLog, "ContactBackupReport.csv." + new SimpleDateFormat("yyyyMMddHHmmss").format(startTime));
        FileWriter writer = null;
        boolean success = true;
        try {
            writer = new FileWriter(reportFile);
            writer.write(builder.toString());
        } catch (IOException ioe) {
            ZimbraLog.contactbackup.debug("Failed to write in report file.");
            ZimbraLog.contactbackup.debug(ioe);
            success = false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ioe) {
                    ZimbraLog.contactbackup.debug("Error occurred while closing writer.");
                    ZimbraLog.contactbackup.debug(ioe);
                    success = false;
                }
            }
        }
        if (success) {
            ZimbraLog.contactbackup.debug("Contact backup report stored at %s", reportFile.getAbsolutePath());
        }
    }
}
