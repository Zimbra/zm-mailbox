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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ReindexProgressInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_REINDEX_RESPONSE)
public class ReIndexResponse {

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
     * @zm-api-field-tag status
     * @zm-api-field-description Status - one of
     *                           <b>started|running|cancelled|idle</b>
     */
    @XmlAttribute(name = AdminConstants.A_STATUS, required = true)
    private final String status;

    /**
     * @zm-api-field-description Information about reindexing progress
     */
    @XmlElement(name = AdminConstants.E_PROGRESS, required = false)
    private final ReindexProgressInfo progress;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ReIndexResponse() {
        this((Integer) null, (String) null, (ReindexProgressInfo) null);
    }

    public ReIndexResponse(Integer statusCode) {
        this(statusCode, (String) null, (ReindexProgressInfo) null);
    }

    public ReIndexResponse(Integer statusCode, String status) {
        this(statusCode, status, (ReindexProgressInfo) null);
    }

    public ReIndexResponse(Integer statusCode, String status, ReindexProgressInfo progress) {
        this.statusCode = statusCode;
        this.status = status;
        this.progress = progress;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getStatus() {
        return status;
    }

    public ReindexProgressInfo getProgress() {
        return progress;
    }
}
