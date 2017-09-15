/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.type;

import com.zimbra.soap.account.type.AccountCalDataSource;
import com.zimbra.soap.account.type.AccountDataSource;
import com.zimbra.soap.account.type.AccountImapDataSource;
import com.zimbra.soap.account.type.AccountPop3DataSource;
import com.zimbra.soap.account.type.AccountRssDataSource;

public class DataSources {

    public static AccountDataSource newDataSource(DataSource data) {
        return new AccountDataSource(data);
    }

    public static AccountDataSource newDataSource() {
        return new AccountDataSource();
    }

    public static Pop3DataSource newPop3DataSource() {
        return new AccountPop3DataSource();
    }

    public static Pop3DataSource newPop3DataSource(Pop3DataSource data) {
        return new AccountPop3DataSource(data);
    }

    public static ImapDataSource newImapDataSource() {
        return new AccountImapDataSource();
    }

    public static ImapDataSource newImapDataSource(ImapDataSource data) {
        return new AccountImapDataSource(data);
    }

    public static RssDataSource newRssDataSource() {
        return new AccountRssDataSource();
    }

    public static RssDataSource newRssDataSource(RssDataSource data) {
        return new AccountRssDataSource(data);
    }

    public static CalDataSource newCalDataSource() {
        return new AccountCalDataSource();
    }

    public static CalDataSource newCalDataSource(CalDataSource data) {
        return new AccountCalDataSource(data);
    }
}
