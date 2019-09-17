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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class PurgeMessagesStatus extends MailboxWithMailboxId {

    /**
     * @zm-api-field-tag all
     * @zm-api-field-description True if all messages were deleted
     */
    @XmlAttribute(name=AdminConstants.A_ALL /* all */, required=false)
    public ZmBoolean all;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private PurgeMessagesStatus() {
        this(0, null, null, false);
    }

    public PurgeMessagesStatus(int mbxid, String accountId, Long size, boolean all) {
        super(mbxid, accountId, size);
        this.all = ZmBoolean.fromBool(all);
    }

    @XmlTransient
    public boolean getAllSetting() { return ZmBoolean.toBool(all); }

    public PurgeMessagesStatus setAllSetting(boolean all) {
        this.all = ZmBoolean.fromBool(all);
        return this;
    }
}
