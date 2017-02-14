/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.Pop3DataSource;
import com.zimbra.soap.type.ZmBoolean;

@XmlType(propOrder = {})
public class AccountPop3DataSource
extends AccountDataSource
implements Pop3DataSource {

    /**
     * @zm-api-field-description Specifies whether imported POP3 messages should be left on the server or deleted.
     */
    @XmlAttribute(name=MailConstants.A_DS_LEAVE_ON_SERVER)
    private ZmBoolean leaveOnServer;

    public AccountPop3DataSource() {
    }

    public AccountPop3DataSource(Pop3DataSource data) {
        super(data);
        leaveOnServer = ZmBoolean.fromBool(data.isLeaveOnServer());
    }

    @Override
    public Boolean isLeaveOnServer() { return ZmBoolean.toBool(leaveOnServer); }

    @Override
    public void setLeaveOnServer(Boolean leaveOnServer) { this.leaveOnServer = ZmBoolean.fromBool(leaveOnServer); }
}
