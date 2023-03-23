/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2025 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import com.zimbra.common.soap.MailConstants;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = MailConstants.E_MAIL_RECALL_RESPONSE)
public class MailRecallResponse {

    @XmlElement(name = "successfulRecall", required = true)
    private int successfulRecall;

    @XmlElement(name = "unsuccessfulRecall", required = true)
    private int unsuccessfulRecall;

    @XmlElement(name = "allMailRecalled", required = true)
    private boolean allMailRecalled;

    // Getters and Setters
    public int getSuccessfulRecall() {
        return successfulRecall;
    }

    public void setSuccessfulRecall(int successfulRecall) {
        this.successfulRecall = successfulRecall;
    }

    public int getUnsuccessfulRecall() {
        return unsuccessfulRecall;
    }

    public void setUnsuccessfulRecall(int unsuccessfulRecall) {
        this.unsuccessfulRecall = unsuccessfulRecall;
    }

    public boolean isAllMailRecalled() {
        return allMailRecalled;
    }

    public void setAllMailRecalled(boolean allMailRecalled) {
        this.allMailRecalled = allMailRecalled;
    }
}

