package com.zimbra.cs.mailbox;

import java.util.Set;

import com.zimbra.common.service.ServiceException;

/**
 * Interface providing the names of SmartFolders that should be present for the account
 */
abstract class SmartFolderProvider {

    abstract Set<String> getSmartFolderNames() throws ServiceException;
}
