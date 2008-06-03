/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.SystemUtil;

import java.io.IOException;

/**
 * Common base class for both ImapImport and Pop3Import.
 */
public abstract class AbstractMailItemImport implements MailItemImport {
    protected final DataSource dataSource;

    protected AbstractMailItemImport(DataSource ds) {
        dataSource = ds;
    }

    protected void validateDataSource() throws ServiceException {
        DataSource ds = getDataSource();
        if (ds.getHost() == null) {
            throw ServiceException.FAILURE(ds + ": host not set", null);
        }
        if (ds.getPort() == null) {
            throw ServiceException.FAILURE(ds + ": port not set", null);
        }
        if (ds.getConnectionType() == null) {
            throw ServiceException.FAILURE(ds + ": connectionType not set", null);
        }
        if (ds.getUsername() == null) {
            throw ServiceException.FAILURE(ds + ": username not set", null);
        }
    }

    protected int addMessage(ParsedMessage pm, int folderId, int flags)
        throws ServiceException, IOException {
        Mailbox mbox = dataSource.getMailbox();
        Message msg = mbox.addMessage(null, pm, folderId, false, flags, null);
        return msg.getId();
    }

    public boolean isSslEnabled() {
        return dataSource.getConnectionType() == DataSource.ConnectionType.ssl;
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }
}
