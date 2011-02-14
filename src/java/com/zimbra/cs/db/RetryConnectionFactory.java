/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp.DriverManagerConnectionFactory;

/**
 * ConnectionFactory implementation which allows for retry on exception
 *
 */
public class RetryConnectionFactory extends DriverManagerConnectionFactory {

    public RetryConnectionFactory(String connectUri, Properties props) {
        super(connectUri, props);
    }

    @Override
    public Connection createConnection() throws SQLException {
        AbstractRetry<Connection> exec = new AbstractRetry<Connection>() {
            @Override
            public ExecuteResult<Connection> execute() throws SQLException {
                Connection conn = superCreateConnection();
                Db.getInstance().postCreate(conn);
                return new ExecuteResult<Connection>(new RetryConnection(conn));
            }

            @Override
            protected boolean retryException(SQLException sqle) {
                //TODO: add new error codes in Db and consult those instead. Currently tightly coupled to SQLite
                return (super.retryException(sqle) || sqle.getMessage().contains("SQLITE_CANTOPEN"));
            }

        };
        return exec.doRetry().getResult();
    }

    private Connection superCreateConnection() throws SQLException {
        return super.createConnection();
    }
}
