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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_PROV_ZULIP_ACCOUNTS_RESPONSE)
public class ProvZulipAccountsResponse {
    /**
     * @zm-api-field-tag numSucceeded
     * @zm-api-field-description Number of accounts that created successfully
     */
    @XmlElement(name=AdminConstants.A_NUM_SUCCEEDED /* number of success */, required=true)
    private int numSucceeded;

    /**
     * @zm-api-field-tag numFailed
     * @zm-api-field-description Number of accounts that could not be created
     */
    @XmlElement(name=AdminConstants.A_NUM_FAILED /* number of failure */, required=true)
    private int numFailed;

    /**
     * @return the numSucceeded
     */
    public int getNumSucceeded() {
        return numSucceeded;
    }

    /**
     * @param numSucceeded the numSucceeded to set
     */
    public void setNumSucceeded(int numSucceeded) {
        this.numSucceeded = numSucceeded;
    }

    /**
     * @return the numFailed
     */
    public int getNumFailed() {
        return numFailed;
    }

    /**
     * @param numFailed the numFailed to set
     */
    public void setNumFailed(int numFailed) {
        this.numFailed = numFailed;
    }
}