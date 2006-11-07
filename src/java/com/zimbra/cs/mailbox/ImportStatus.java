/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
/**
 * 
 */
package com.zimbra.cs.mailbox;

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
            "ImportStatus: { dataSourceId=%d, isRunning=%b, success=%b, error=%s, hasRun=%b }",
            mDataSourceId, mIsRunning, mSuccess, mError, mHasRun);
    }
}
