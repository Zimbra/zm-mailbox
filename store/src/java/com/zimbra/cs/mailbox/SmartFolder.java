package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbTag;

/**
 * Subclass of Tag used for filing messages into one or more categories based on the output of
 * a {@link ClassificationTask}.
 * We need a subclass for this because it should be possible for SmartFolders and Tags
 * to share a name.
 * @author iraykin
 *
 */
public class SmartFolder extends Tag {

    SmartFolder(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        super(mbox, ud);
        init();
    }

    SmartFolder(Mailbox mbox, UnderlyingData ud, boolean skipCache) throws ServiceException {
        super(mbox, ud, skipCache);
        init();
    }

    SmartFolder(Account acc, UnderlyingData data, int mailboxId) throws ServiceException {
        super(acc, data, mailboxId);
        init();
    }

    private void init() {
        if (type != Type.SMARTFOLDER.toByte()) {
            throw new IllegalArgumentException();
        }
    }


    static SmartFolder create(Mailbox mbox, int id, String requestedName) throws ServiceException {
        if (requestedName == null || requestedName != StringUtil.stripControlCharacters(requestedName) || requestedName.contains(FLAG_NAME_PREFIX)) {
            throw MailServiceException.INVALID_NAME(requestedName);
        }
        String name = getInternalTagName(requestedName);
        UnderlyingData data = new UnderlyingData();
        data.id = id;
        data.type = Type.SMARTFOLDER.toByte();
        data.folderId = Mailbox.ID_FOLDER_TAGS;
        data.name = name;
        data.setSubject(name);
        data.contentChanged(mbox);
        ZimbraLog.mailop.info("Adding SmartFolder %s: id=%d.", name, data.id);
        DbTag.createTag(mbox, data, null, false);
        SmartFolder smartFolder = new SmartFolder(mbox, data);
        smartFolder.finishCreation(null);
        return smartFolder;
    }

    @Override
    /**
     * This returns the name of the tag that represents this SmartFolder.
     */
    public String getName() {
        return state.getName();
    }

    public String getSmartFolderName() {
        return getName().substring(2);
    }

    public static String getInternalTagName(String externalName) {
        return SMARTFOLDER_NAME_PREFIX + externalName;
    }
}
