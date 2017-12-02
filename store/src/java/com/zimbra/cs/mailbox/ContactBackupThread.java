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

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
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
    private static long contactBackupFreq = 0;
    private boolean shutdownRequested = false;
    private static final String CT_TYPE = "application/x-compressed-tar";
    private static final String CONTACT_RES_URL = "?fmt=tgz&types=contact";
    private static final String FILE_NAME = "Contacts";
    private static final String FILE_DESC = "Contact Backup at ";
    private static final String DATE_FORMAT = "yyyy-MM-dd-HHmmss";
    private boolean success = true;

    private ContactBackupThread() {
        setName(OPERATION);
    }

    public static synchronized void startup() {
        synchronized(THREAD_LOCK) {
            if (isRunning()) {
                ZimbraLog.contactbackup.warn("can not start another thread");
                return;
            }
            setContactBackupFrequency();
            if (contactBackupFreq == 0) {
                ZimbraLog.contactbackup.warn("%s is set to 0", Provisioning.A_zimbraFeatureContactBackupFrequency);
                return;
            }
            if (!CallbackUtil.logStartup(Provisioning.A_zimbraFeatureContactBackupFrequency)) {
                return;
            }
            backupThread = new ContactBackupThread();
            backupThread.start();
        }
    }

    public static synchronized void shutdown() {
        synchronized(THREAD_LOCK) {
            if (backupThread != null) {
                backupThread.requestShutdown();
                backupThread.interrupt();
                backupThread = null;
                ZimbraLog.contactbackup.debug("shutdown requested");
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

    // set shutdown = true
    private void requestShutdown() {
        shutdownRequested = true;
    }

    // fetch value of zimbraFeatureContactBackupFrequency from server and update the same to a class variable contactBackupFreq
    private static void setContactBackupFrequency() {
        contactBackupFreq = CallbackUtil.getTimeInterval(Provisioning.A_zimbraFeatureContactBackupFrequency, 0);
        ZimbraLog.contactbackup.debug("set contactBackupFreq to %d", contactBackupFreq);
    }

    // return list of mailbox ids 
    private List<Integer> getMailboxIds() throws ServiceException {
        List<Integer> mailboxIds = CallbackUtil.getSortedMailboxIdList();
        // Remove id's <= last mailbox id, so that we start with the one after the last backed up.
        int lastProcessedId = Config.getInt(Config.CONTACT_BACKUP_LAST_MAILBOX_ID, 0);
        int cutoff = 0;
        if (lastProcessedId > 0) {
            for (int i = 0; i < mailboxIds.size(); i++) {
                int id = mailboxIds.get(i);
                if (id > lastProcessedId) {
                    cutoff = i;
                    ZimbraLog.contactbackup.debug("starting backup with mailbox id: %d and mailboxes after this id", id);
                    break;
                }
            }
            mailboxIds = mailboxIds.subList(cutoff, mailboxIds.size());
        }
        return mailboxIds;
    }

    // make current thread sleep for given time
    private void sleepThread(long time){
        if (time > 0) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                ZimbraLog.contactbackup.debug("thread was interrupted.");
                shutdownRequested = true;
            }
        }
    }

    private void setContactBackupLastMailboxId(int id) {
        try {
            Config.setInt(Config.CONTACT_BACKUP_LAST_MAILBOX_ID, id);
            ZimbraLog.contactbackup.debug("setting contact backup last mailbox id with %d", id);
        } catch (ServiceException se) {
            ZimbraLog.contactbackup.warn("exception occured while setting contact backup last mailbox id with %d", id, se);
        }
    }

    private void setContactBackupNextIterationStartTime(long time) {
        try {
            Config.setLong(Config.CONTACT_BACKUP_NEXT_ITR_START_TIME, time);
            ZimbraLog.contactbackup.debug("setting contact backup next iteration time %d", time);
        } catch (ServiceException se) {
            ZimbraLog.contactbackup.warn("exception occured while setting contact backup next iteration time", se);
        }
    }

    private long getInitalSleepTime() {
        long sleepTime = LC.contact_backup_initial_sleep_ms.longValue();
        long nextItrTime = Config.getLong(Config.CONTACT_BACKUP_NEXT_ITR_START_TIME, 0);
        long waitTime = 0;
        Date now = new Date();
        if (now.getTime() < nextItrTime) {
            waitTime = nextItrTime - now.getTime();
        }
        if (waitTime > sleepTime) {
            sleepTime = waitTime;
        }
        return sleepTime;
    }

    /**
     * Iterate over list of mailbox ids and start backup on each one of them.
     * Sleep for zimbraFeatureContactBackupFrequency once the thread cycle is over.
     */
    @Override
    public void run() {
        long sleepTime = getInitalSleepTime();
        ZimbraLog.contactbackup.info("Thread sleeping for %d ms before doing work.", sleepTime);
        sleepThread(sleepTime);
        if (shutdownRequested) {
            ZimbraLog.contactbackup.info("Shutting down thread.");
            backupThread = null;
            return;
        }
        while (true) {
            List<Integer> mailboxIds = null;
            try {
                mailboxIds = getMailboxIds();
            } catch (ServiceException e) {
                ZimbraLog.contactbackup.warn("can not get list of mailboxes, shutting down thread.", e);
                backupThread = null;
                return;
            }
            Date startTime = new Date();
            ZimbraLog.contactbackup.debug("starting iteration on mailboxes at %s", startTime.toString());
            for (Integer mailboxId : mailboxIds) {
                if (shutdownRequested) {
                    ZimbraLog.contactbackup.info("shutting down thread.");
                    backupThread = null;
                    return;
                }
                ZimbraLog.contactbackup.debug("starting to work with mailbox %d", mailboxId);
                ZimbraLog.addMboxToContext(mailboxId);
                success = true;
                try {
                    Mailbox mbox = MailboxManager.getInstance().getMailboxById(mailboxId);
                    Account account = mbox.getAccount();
                    ZimbraLog.addAccountNameToContext(account.getName());
                    if (account.isFeatureContactBackupEnabled() && !account.isIsSystemAccount() && !account.isIsSystemResource() && account.isAccountStatusActive()) {
                        OperationContext octxt = new OperationContext(account);
                        Folder folder = getContactBackupFolder(octxt, mbox, true);
                        if (folder != null) {
                            createBackup(octxt, mbox, account, folder, startTime);
                            purgeOldBackups(octxt, mbox, folder, startTime);
                        } else {
                            success = false;
                            ZimbraLog.contactbackup.info("contact backup folder not found, continuing to next mailbox");
                        }
                        // set current mailbox id as last processed mailbox
                        if (success) {
                            setContactBackupLastMailboxId(mbox.getId());
                        }
                    } else {
                        ZimbraLog.contactbackup.debug("contact backup skipped: feature is disabled/account is inactive/it's a system account");
                    }
                } catch (Exception e) {
                    ZimbraLog.contactbackup.warn("backup/purge failed for mailbox %d, continuing to next mailbox", mailboxId, e);
                }
                ZimbraLog.clearContext();
            }
            setContactBackupLastMailboxId(0);
            Date endTime = new Date();
            long diff = endTime.getTime() - startTime.getTime();
            ZimbraLog.contactbackup.debug("finished iteration on mailboxes, iteration took %d ms", diff);
            long sleepThreadTime = contactBackupFreq - diff; // find actual diff so that thread will start again at a same time next time
            Date next = new Date(endTime.getTime() + sleepThreadTime);
            setContactBackupNextIterationStartTime(next.getTime());
            ZimbraLog.contactbackup.info("Thread finished iteration, sleeping for %d ms", sleepThreadTime);
            sleepThread(sleepThreadTime);
        }
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
            ZimbraLog.contactbackup.warn("contact export failed, continuing to next mailbox", exception);
        } catch (OutOfMemoryError e) {
            Zimbra.halt("OutOfMemoryError while creating contact backup", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    ZimbraLog.contactbackup.warn("IOExcepion occured while closing stream", ioe);
                }
            }
            if (token != null) {
                try {
                    token.deRegister();
                 } catch (AuthTokenException e) {
                     ZimbraLog.contactbackup.warn("failed to deregister token", e);
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
            ZimbraLog.contactbackup.warn("exception occured while getting list of contact backups", se);
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
                            ZimbraLog.contactbackup.warn("failed to delete item (id=%d) from contact backup folder", item.getId(), se);
                        }
                    }
                } catch (ServiceException se) {
                    success = false;
                    ZimbraLog.contactbackup.warn("exception occured while getting document from contact backup folder", se);
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
                    ZimbraLog.contactbackup.warn("failed to create contact backup folder", se2);
                }
            } else {
                ZimbraLog.contactbackup.warn("exception occured while getting contact backup folder", se);
            }
        }
        return folder;
    }
}
