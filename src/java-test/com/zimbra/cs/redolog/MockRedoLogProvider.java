/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.redolog;

import java.io.File;

import com.zimbra.common.service.ServiceException;

/**
 * Mock {@link RedoLogProvider} for unit test.
 *
 * @author ysasaki
 */
public class MockRedoLogProvider extends RedoLogProvider {

    public MockRedoLogProvider() {
        mRedoLogManager = new RedoLogManager(new File("build/test/redo/redo.log"), new File("build/test/redo"), false);
    }

    @Override
    public boolean isMaster() {
        return true;
    }

    @Override
    public boolean isSlave() {
        return false;
    }

    @Override
    public void startup() throws ServiceException {
    }

    @Override
    public void shutdown() throws ServiceException {
    }

    @Override
    public void initRedoLogManager() {
    }

}
