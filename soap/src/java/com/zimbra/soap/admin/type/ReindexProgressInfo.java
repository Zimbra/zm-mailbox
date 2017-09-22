/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ReindexProgressInfo {

    /**
     * @zm-api-field-tag statusCode
     * @zm-api-field-description Status code - one of:
      <ul>
      <li>-3 - re-indexing failed</li>
      <li>-2 - re-indexing was interrupted, because indexing queue is full</li>
      <li>-1 - re-indexing was aborted with a "cancel" action </li>
      <li>0 - re-indexing is not running/has not been started</li>
      <li>1 - re-indexing is actively running</li>
      <li>2 - re-indexing task is complete</li>
     */
    @XmlAttribute(name = AdminConstants.A_STATUS_CODE, required = true)
    private final Integer statusCode;

    /**
     * @zm-api-field-tag succeeded
     * @zm-api-field-description Number of reindexes that succeeded
     */
    @XmlAttribute(name=AdminConstants.A_NUM_SUCCEEDED /* numSucceeded */, required=true)
    private final int numSucceeded;

    /**
     * @zm-api-field-tag failed
     * @zm-api-field-description Number of reindexes that failed
     */
    @XmlAttribute(name=AdminConstants.A_NUM_FAILED /* numFailed */, required=true)
    private final int numFailed;

    /**
     * @zm-api-field-tag remaining
     * @zm-api-field-description Number of reindexes that remaining
     */
    @XmlAttribute(name=AdminConstants.A_NUM_REMAINING /* numRemaining */, required=true)
    private final int numRemaining;

    /**
     * @zm-api-field-tag accountId
     * @zm-api-field-description ID of the account bing reindexed
     */
    @XmlAttribute(name=AdminConstants.A_ACCOUNTID /* numRemaining */, required=true)
    private final String accountId;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ReindexProgressInfo() {
        this(-1, -1, -1, -1, "");
    }

    public ReindexProgressInfo(int statusCode,
            int numSucceeded, int numFailed, int numRemaining, String accountId) {
        this.statusCode = statusCode;
        this.numSucceeded = numSucceeded;
        this.numFailed = numFailed;
        this.numRemaining = numRemaining;
        this.accountId = accountId;
    }

    public int getNumSucceeded() { return numSucceeded; }
    public int getNumFailed() { return numFailed; }
    public int getNumRemaining() { return numRemaining; }
    public int getStatusCode() { return statusCode; }
    public String getAccountId() { return accountId; }
}
