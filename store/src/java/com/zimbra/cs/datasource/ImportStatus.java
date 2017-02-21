/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
/**
 * 
 */
package com.zimbra.cs.datasource;

import com.zimbra.cs.account.DataSource;

/**
 * Keeps track of the status of an import from a {@link DataSource}.
 * 
 * @author bburtin
 */
public class ImportStatus {
    private String mDataSourceId;
    boolean mIsRunning = false;
    boolean mSuccess = false;
    String mError = null;
    boolean mHasRun = false;

    ImportStatus(String dataSourceId) {
        mDataSourceId = dataSourceId;
    }
    
    ImportStatus(ImportStatus status) {
        mDataSourceId = status.mDataSourceId;
        mIsRunning = status.isRunning();
        mSuccess = status.getSuccess();
        mError = status.getError();
        mHasRun = status.hasRun();
    }
    
    public String getDataSourceId() { return mDataSourceId; }
    public boolean isRunning() { return mIsRunning; }
    public boolean getSuccess() { return mSuccess; }
    public String getError() { return mError; }
    
    /**
     * Returns <code>true</code> if an import process has ever started on this data source.
     */
    public boolean hasRun() { return mHasRun; }
    
    @Override
    public String toString() {
        return String.format(
            "ImportStatus: { dataSourceId=%s, isRunning=%b, success=%b, error=%s, hasRun=%b }",
            mDataSourceId, mIsRunning, mSuccess, mError, mHasRun);
    }
}
