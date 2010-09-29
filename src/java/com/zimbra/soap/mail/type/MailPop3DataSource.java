/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.Pop3DataSource;

@XmlType(propOrder = {})
public class MailPop3DataSource
extends MailDataSource
implements Pop3DataSource {

    @XmlAttribute(name=MailConstants.A_DS_LEAVE_ON_SERVER)
    private Boolean leaveOnServer;

    public MailPop3DataSource() {
    }
    
    public MailPop3DataSource(Pop3DataSource data) {
        super(data);
        leaveOnServer = data.isLeaveOnServer();
    }
    
    @Override
    public Boolean isLeaveOnServer() {
        return leaveOnServer;
    }
    
    public void setLeaveOnServer(Boolean leaveOnServer) {
        this.leaveOnServer = leaveOnServer;
    }
}
