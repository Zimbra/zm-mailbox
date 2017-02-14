/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-description Verify Store Manager
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_VERIFY_STORE_MANAGER_RESPONSE)
public class VerifyStoreManagerResponse {

    @XmlAttribute(required=false)
    private String storeManagerClass;

    @XmlAttribute(required=false)
    private Long incomingTime;

    @XmlAttribute(required=false)
    private Long stageTime;

    @XmlAttribute(required=false)
    private Long linkTime;

    @XmlAttribute(required=false)
    private Long fetchTime;

    @XmlAttribute(required=false)
    private Long deleteTime;

    public String getStoreManagerClass() {
        return storeManagerClass;
    }

    public void setStoreManagerClass(String storeManagerClass) {
        this.storeManagerClass = storeManagerClass;
    }

    public Long getIncomingTime() {
        return incomingTime;
    }

    public void setIncomingTime(Long incomingTime) {
        this.incomingTime = incomingTime;
    }

    public Long getStageTime() {
        return stageTime;
    }

    public void setStageTime(Long stageTime) {
        this.stageTime = stageTime;
    }

    public Long getLinkTime() {
        return linkTime;
    }

    public void setLinkTime(Long linkTime) {
        this.linkTime = linkTime;
    }

    public Long getFetchTime() {
        return fetchTime;
    }

    public void setFetchTime(Long fetchTime) {
        this.fetchTime = fetchTime;
    }

    public Long getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(Long deleteTime) {
        this.deleteTime = deleteTime;
    }

    /**
     * no-argument constructor wanted by JAXB
     */
    private VerifyStoreManagerResponse() {
    }
}
