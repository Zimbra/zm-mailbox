/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;

/**
 * Cache of folders and tags for multiple mailboxes. Loading folders/tags from database is expensive,
 * so we cache them.  The cached data must be kept up to date as changes occur to a folder or
 * a tag.  Folder changes occur very frequently because creating/deleting an item in a folder updates the
 * folder state.
 */
public interface FoldersAndTagsCache {

	/** Returns cached list of all folders and tags for a given mailbox */
    public FoldersAndTags get(Mailbox mbox) throws ServiceException;

    /** Caches list of all folders and tags for a given mailbox */
    public void put(Mailbox mbox, FoldersAndTags foldersAndTags) throws ServiceException;

    /** Clears cache of folders and tags for a given mailbox */
    public void remove(Mailbox mbox) throws ServiceException;



    @Aspect
    public static class Disable {
        @Around("this(com.zimbra.cs.mailbox.FoldersAndTagsCache)")
        public Object invoke(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
            if (DebugConfig.disableFoldersTagsCache) {
                return null;
            }
            return proceedingJoinPoint.proceed();
        }
    }
}
