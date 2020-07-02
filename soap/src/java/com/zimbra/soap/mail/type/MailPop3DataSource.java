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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.Pop3DataSource;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class MailPop3DataSource
extends MailDataSource
implements Pop3DataSource {

    /**
     * @zm-api-field-tag leave-on-server
     * @zm-api-field-description Leave messages on the server
     */
    @XmlAttribute(name=MailConstants.A_DS_LEAVE_ON_SERVER /* leaveOnServer */, required=false)
    private ZmBoolean leaveOnServer;

    public MailPop3DataSource() {
        super();
    }

    public MailPop3DataSource(Pop3DataSource data) {
        super(data);
        setLeaveOnServer(data.isLeaveOnServer());
    }

    @Override
    public void setLeaveOnServer(Boolean leaveOnServer) {
        this.leaveOnServer = ZmBoolean.fromBool(leaveOnServer);
    }
    @Override
    public Boolean isLeaveOnServer() { return ZmBoolean.toBool(leaveOnServer); }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("leaveOnServer", leaveOnServer);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
