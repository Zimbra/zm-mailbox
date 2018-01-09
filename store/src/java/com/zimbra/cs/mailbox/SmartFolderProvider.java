package com.zimbra.cs.mailbox;

import java.util.Set;

import com.zimbra.common.service.ServiceException;

/**
 * Interface providing the names of SmartFolders that should be present for the account
 */
public abstract class SmartFolderProvider {

    public abstract Set<String> getSmartFolderNames() throws ServiceException;
}
