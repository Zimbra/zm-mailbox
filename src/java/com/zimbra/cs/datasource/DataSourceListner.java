/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.cs.datasource;

import java.util.HashSet;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;

public abstract class DataSourceListner {

    public static final String CREATE_DATASOURCE = "CreateDataSource";
    public static final String DELETE_DATASOURCE = "DeleteDataSource";

    private static final HashSet<DataSourceListner> listeners;

    static {
        listeners = new HashSet<DataSourceListner>();
        reset();
    }

    public enum DataSourceAction {
        CREATE(CREATE_DATASOURCE), DELETE(DELETE_DATASOURCE);

        private String action;

        /**
         * @param name
         * @param ordinal
         */
        private DataSourceAction(String name) {
            this.action = name;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return action;
        }

    }

    static void reset() {
        synchronized (listeners) {
            listeners.clear();
        }
    }

    public abstract void notify(Account account, DataSource dataSource, DataSourceAction action);

    public static void register(DataSourceListner listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public static void unregister(DataSourceListner listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public static void createDataSource(Account account, DataSource dataSource) {
        for (DataSourceListner dataSourceListener : listeners) {
            dataSourceListener.notify(account, dataSource, DataSourceAction.CREATE);
        }
    }

    public static void deleteDataSource(Account account, DataSource dataSource) {
        for (DataSourceListner dataSourceListener : listeners) {
            dataSourceListener.notify(account, dataSource, DataSourceAction.DELETE);
        }
    }

}
