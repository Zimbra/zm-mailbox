/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.calendar.cache;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for {@link LocalCtagResponseCache}.
 */
public final class LocalCtagResponseCacheTest extends AbstractCtagResponseCacheTest {

    @Override
    protected CtagResponseCache constructCache() throws ServiceException {
        CtagResponseCache cache = new LocalCtagResponseCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(cache);
        Zimbra.getAppContext().getAutowireCapableBeanFactory().initializeBean(cache, "ctagResponseCache");
        return cache;
    }

    @Override
    protected boolean isExternalCacheAvailableForTest() throws Exception {
        return true;
    }

    @Override
    protected void flushCacheBetweenTests() throws Exception {
        ((LocalCtagResponseCache)cache).flush();
    }
}
