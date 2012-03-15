/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
