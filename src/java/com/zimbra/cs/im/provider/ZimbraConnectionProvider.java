/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.im.provider;

import java.sql.Connection;
import java.sql.SQLException;

import org.jivesoftware.database.ConnectionProvider;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbPool;

public class ZimbraConnectionProvider implements ConnectionProvider {

    public boolean isPooled() {
        return true;
    }

    
    public Connection getConnection() throws SQLException {
        try {
            return DbPool.getConnection().getConnection();
        } catch (ServiceException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof SQLException) 
                throw (SQLException)cause;
            throw new SQLException("Caught ServiceException: "+ex.toString());
        }
    }

    public void restart() {
    }
    public void start() {
    }
    public void destroy() {
    }
}
