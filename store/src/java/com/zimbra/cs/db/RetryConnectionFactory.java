/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
                return (super.retryException(sqle) || Db.errorMatches(sqle, Db.Error.CANTOPEN));
            }

        };
        return exec.doRetry().getResult();
    }

    private Connection superCreateConnection() throws SQLException {
        return super.createConnection();
    }
}
