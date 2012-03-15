/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
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
    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("leaveOnServer", leaveOnServer);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
