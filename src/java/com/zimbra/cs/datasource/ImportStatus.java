/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
