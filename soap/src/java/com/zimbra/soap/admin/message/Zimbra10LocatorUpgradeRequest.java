/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.MailboxByAccountIdSelector;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE) @XmlRootElement(name = AdminConstants.E_ZIMBRA10_LOCATOR_UPGRADE_REQUEST)
public final class Zimbra10LocatorUpgradeRequest {
    @XmlElement(name = AdminConstants.E_ACCOUNT_NAME, required = false)
    private List<String> accounts = new ArrayList<>();
    @XmlElement(name = AdminConstants.E_MAIL_BOXES, required = false)
    private List<Integer> mboxNumbers = new ArrayList<>();
    @XmlElement(name = AdminConstants.E_IS_UPDATE_ALL, required = false)
    private boolean isUpdateAllMailBoxes;
    private Zimbra10LocatorUpgradeRequest() {
        this((List<String>) null, (List<Integer>) null, Boolean.FALSE);
    }
    public Zimbra10LocatorUpgradeRequest(List<String> accounts, List<Integer> mboxNumbers,
            boolean isUpdateAllMailBoxes) {
        this.accounts = accounts;
        this.mboxNumbers = mboxNumbers;
        this.isUpdateAllMailBoxes = isUpdateAllMailBoxes;
    }
    public List<String> getAccounts() {
        return accounts;
    }
    public List<Integer> getMboxNumbers() {
        return mboxNumbers;
    }
    public boolean isUpdateAllMailBoxes() {
        return isUpdateAllMailBoxes;
    }
}