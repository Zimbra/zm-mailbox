/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import com.zimbra.common.localconfig.LC;


public class MariaDB extends MySQL {

    @Override
    DbPool.PoolConfig getPoolConfig() {
        return new MariaDBConfig();
    }

    protected class MariaDBConfig extends MySQLConfig {

        @Override
        protected String getDriverClassName() {
            return "org.mariadb.jdbc.Driver";
        }

        @Override
        protected String getRootUrl() {
            String bindAddress = LC.mysql_bind_address.value();
            if (bindAddress.indexOf(':') > -1) {
                bindAddress = "[" + bindAddress + "]";
            }

            return "jdbc:mysql://" + bindAddress + ":" + LC.mysql_port.value() + "/";
        }

    }
}
