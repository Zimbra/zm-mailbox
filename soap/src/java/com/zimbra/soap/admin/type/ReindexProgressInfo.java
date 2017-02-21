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

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ReindexProgressInfo {

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
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ReindexProgressInfo() {
        this(-1, -1, -1);
    }

    public ReindexProgressInfo(
            int numSucceeded, int numFailed, int numRemaining) {
        this.numSucceeded = numSucceeded;
        this.numFailed = numFailed;
        this.numRemaining = numRemaining;
    }

    public int getNumSucceeded() { return numSucceeded; }
    public int getNumFailed() { return numFailed; }
    public int getNumRemaining() { return numRemaining; }
}
