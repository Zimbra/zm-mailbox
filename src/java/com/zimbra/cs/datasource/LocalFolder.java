package com.zimbra.cs.datasource;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.db.DbImapMessage;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.Log;

import java.util.Set;
import java.util.HashSet;
import java.util.List;

final class LocalFolder {
    private final Mailbox mbox;
    private final String path;
    private Folder folder;

    private static final Log LOG = ZimbraLog.datasource;

    public static LocalFolder fromId(Mailbox mbox, int id)
        throws ServiceException {
        try {
            return new LocalFolder(mbox, mbox.getFolderById(null, id));
        } catch (MailServiceException.NoSuchItemException e) {
            return null;
        }
    }
    
    LocalFolder(Mailbox mbox, String path) {
        this.mbox = mbox;
        this.path = path;
    }

    LocalFolder(Mailbox mbox, Folder folder) throws ServiceException {
        this.mbox = mbox;
        this.path = folder.getPath();
        this.folder = folder;
    }

    public void delete() throws ServiceException {
        debug("deleting folder");
        Folder folder;
        try {
            folder = getFolder();
        } catch (MailServiceException.NoSuchItemException e) {
            return;
        }
        mbox.delete(null, folder.getId(), folder.getType());
    }

    public void create() throws ServiceException {
        debug("creating folder");
        folder = mbox.createFolder(null, path, (byte) 0, MailItem.TYPE_UNKNOWN);
    }

    public void alterTag(int flagId) throws ServiceException {
        mbox.alterTag(null, getFolder().getId(), MailItem.TYPE_FOLDER, flagId, true);
    }

    public void setMessageFlags(int id, int flagMask) throws ServiceException {
        mbox.setTags(null, id, MailItem.TYPE_MESSAGE, flagMask, MailItem.TAG_UNCHANGED);
    }
    
    public boolean exists() throws ServiceException {
        try {
            getFolder();
        } catch (MailServiceException.NoSuchItemException e) {
            return false;
        }
        return true;
    }
    
    public Message getMessage(int id) throws ServiceException {
        try {
            return mbox.getMessageById(null, id);
        } catch (MailServiceException.NoSuchItemException e) {
            return null;
        }
    }

    public void deleteMessage(int id) throws ServiceException {
        debug("deleting message with id %d", id);
        try {
            mbox.delete(null, id, MailItem.TYPE_UNKNOWN);
        } catch (MailServiceException.NoSuchItemException e) {
            debug("message with id %d not found", id);
        }
        DbImapMessage.deleteImapMessage(mbox, getId(), id);
    }

    public void emptyFolder() throws ServiceException {
        mbox.emptyFolder(null, getId(), false);
        DbImapMessage.deleteImapMessages(mbox, getId());
    }
    
    public Set<Integer> getMessageIds() throws ServiceException {
        return new HashSet<Integer>(
            mbox.listItemIds(null, MailItem.TYPE_MESSAGE, folder.getId()));
    }

    public List<Integer> getNewMessageIds() throws ServiceException {
        return DbImapMessage.getNewLocalMessageIds(mbox, getId());    
    }

    public Folder getFolder() throws ServiceException {
        if (folder == null) {
            folder = mbox.getFolderByPath(null, path);
        }
        return folder;
    }

    public int getId() throws ServiceException {
        return getFolder().getId();
    }

    public String getPath() {
        return folder != null ? folder.getPath() : path;
    }

    public void debug(String fmt, Object... args) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(errmsg(String.format(fmt, args)));
        }
    }

    public void info(String fmt, Object... args) {
        LOG.info(errmsg(String.format(fmt, args)));
    }

    public void warn(String msg, Throwable e) {
        LOG.error(errmsg(msg), e);
    }

    public void error(String msg, Throwable e) {
        LOG.error(errmsg(msg), e);
    }

    private String errmsg(String s) {
        return String.format("Local folder '%s': %s", getPath(), s);
    }
}
