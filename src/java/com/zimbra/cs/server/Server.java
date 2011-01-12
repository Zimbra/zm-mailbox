/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.server;

import com.zimbra.common.service.ServiceException;

/**
 * Common interface for servers based on either {@link TcpServer} or {@link NioServer}.
 */
public interface Server {
    /**
     * Returns the server name (e.g. ImapServer), which is used for thread name, logging, statistics and etc.
     */
    String getName();
    ServerConfig getConfig();
    void start() throws ServiceException;
    void stop() throws ServiceException;
    void stop(int graceSecs) throws ServiceException;
}
