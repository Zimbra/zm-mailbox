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

package com.zimbra.soap.account.message;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AccountConstants.E_MAIL_RECALL_RESPONSE)
public class MailRecallResponse extends AdminAttrsImpl {
    @XmlAttribute
    private int successfulRecall;
    @XmlAttribute
    private int unSuccessfulRecall;
    @XmlAttribute
    private boolean isAllMailRecalled;

    public int getSuccessfulRecall() {
        return successfulRecall;
    }

    public void setSuccessfulRecall(int successfulRecall) {
        this.successfulRecall = successfulRecall;
    }

    public int getUnSuccessfulRecall() {
        return unSuccessfulRecall;
    }

    public void setUnSuccessfulRecall(int unSuccessfulRecall) {
        this.unSuccessfulRecall = unSuccessfulRecall;
    }

    public boolean isAllMailRecalled() {
        return isAllMailRecalled;
    }

    public void setAllMailRecalled(boolean allMailRecalled) {
        isAllMailRecalled = allMailRecalled;
    }
}
