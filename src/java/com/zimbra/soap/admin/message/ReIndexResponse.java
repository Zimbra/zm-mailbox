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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ReindexProgressInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_REINDEX_RESPONSE)
public class ReIndexResponse {

    /**
     * @zm-api-field-tag status
     * @zm-api-field-description Status - one of <b>started|running|cancelled|idle</b>
     */
    @XmlAttribute(name=AdminConstants.A_STATUS, required=true)
    private final String status;

    /**
     * @zm-api-field-description Information about reindexing progress
     */
    @XmlElement(name=AdminConstants.E_PROGRESS, required=false)
    private final ReindexProgressInfo progress;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private ReIndexResponse() {
        this((String) null, (ReindexProgressInfo)null);
    }

    public ReIndexResponse(String status) {
        this(status, (ReindexProgressInfo)null);
    }

    public ReIndexResponse(String status, ReindexProgressInfo progress) {
        this.status = status;
        this.progress = progress;
    }

    public String getStatus() { return status; }
    public ReindexProgressInfo getProgress() { return progress; }
}
