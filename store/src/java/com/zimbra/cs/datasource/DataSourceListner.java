/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
