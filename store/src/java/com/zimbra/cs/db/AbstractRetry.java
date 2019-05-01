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

import java.sql.SQLException;

import com.zimbra.common.util.ZimbraLog;
/**
 * Abstract class used to wrap a SQL execute command with retry logic 
 */
public abstract class AbstractRetry<T> {
    
    /**
     * Child class must implement SQL execute in this method
     * @throws SQLException
     */
    public abstract ExecuteResult<T> execute() throws SQLException;

    protected boolean retryException(SQLException sqle)
    {
        return Db.errorMatches(sqle, Db.Error.BUSY) || Db.errorMatches(sqle, Db.Error.LOCKED);
    }

    private static final int RETRY_LIMIT = 10;
    private static final int RETRY_DELAY = 1000;
    
    private static int retryCount = 0;
    public static int getTotalRetries() {
        return retryCount;
    }
    
    private synchronized void incrementTotalRetries() {
        retryCount++;
    }

    /**
     * Invoke the execute method inside a retry loop
     * @throws SQLException
     */
    public ExecuteResult<T> doRetry() throws SQLException {
        int tries = 0;
        SQLException sqle = null;
        while (tries < RETRY_LIMIT) {
            try {
                ExecuteResult<T> executeResult = execute();
                if (tries > 0) {
                    ZimbraLog.dbconn.info("connection succeeded after %s attempts", tries + 1);
                }
                return executeResult;
            } catch (SQLException e) {
                if (retryException(e)) {
                    sqle = e;
                    tries++;
                    ZimbraLog.dbconn.warnQuietly(
                            String.format("retrying connection attempt %s of %s due to possibly recoverable exception",
                            tries, RETRY_LIMIT), e);
                    incrementTotalRetries();
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                    }
                } else {
                    throw e;
                }
            }
        }
        ZimbraLog.dbconn.warn("DB retry gave up after %s attempts.", tries, sqle);
        throw new SQLException("DB retry gave up after "+tries+" attempts.",sqle);
    }

}
