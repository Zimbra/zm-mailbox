/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.BackupConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class BackupHost {

    public BackupHost() {}

    public BackupHost(String name) {
        setHost(name);
    }

    @XmlAttribute(name=BackupConstants.A_BACKUP_HOST_NAME, required=true)
    private String host;

    @XmlElementWrapper(name=BackupConstants.E_ACCOUNTS)
    @XmlElement(name=BackupConstants.E_ACCOUNT, type=Name.class)
    private List<Name> accounts = Lists.newArrayList();

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void addAccount(Name account) {
        accounts.add(account);
    }

    public List<Name> getAccounts() {
        return accounts;
    }
}
