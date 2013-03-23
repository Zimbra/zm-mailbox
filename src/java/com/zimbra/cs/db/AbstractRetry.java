/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
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
                return execute();
            } catch (SQLException e) {
                if (retryException(e)) {
                    sqle = e;
                    tries++;
                    ZimbraLog.dbconn.warn("retrying connection attempt:"+tries+" due to possibly recoverable exception: ",e);
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
        throw new SQLException("DB retry gave up after "+tries+" attempts.",sqle);
    }

}
