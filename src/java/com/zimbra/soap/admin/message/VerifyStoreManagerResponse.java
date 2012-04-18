/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
