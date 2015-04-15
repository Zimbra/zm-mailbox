/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.acl.EffectiveACLCache;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.util.ZimbraConfig;

/**
 * Spring Configuration used by unit tests that will use local (in-process) caching.
 */
@Configuration
public class LocalCachingZimbraConfig extends ZimbraConfig {

    @Override
    @Bean
    public CalendarCacheManager calendarCacheManager() throws ServiceException {
        return super.calendarCacheManager(); // TODO need a Local adapter
    }

    @Override
    @Bean
    public EffectiveACLCache effectiveACLCache() throws ServiceException {
        return super.effectiveACLCache(); // TODO need a Local adapter
    }

    @Override
    @Bean
    public FoldersAndTagsCache foldersAndTagsCache() throws ServiceException {
        return new LocalFoldersAndTagsCache();
    }

    @Override
    public boolean isMemcachedAvailable() {
        return false;
    }

    @Override
    public boolean isRedisAvailable() throws ServiceException {
        return false;
    }

    @Override
    public List<MailboxListenerTransport> externalMailboxListeners() throws Exception {
        return Collections.emptyList();
    }

    @Override
    @Bean
    public SharedDeliveryCoordinator sharedDeliveryCoordinator() throws Exception {
        return new LocalSharedDeliveryCoordinator();
    }
}
