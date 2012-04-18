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
@XmlRootElement(name=AdminConstants.E_VERIFY_STORE_MANAGER_REQUEST)
public class VerifyStoreManagerRequest {

    @XmlAttribute(name=AdminConstants.A_FILE_SIZE, required=false)
    private Integer fileSize;

    @XmlAttribute(name=AdminConstants.A_NUM, required=false)
    private Integer num;

    @XmlAttribute(name=AdminConstants.A_CHECK_BLOBS, required=false)
    private Boolean checkBlobs;

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public Boolean getCheckBlobs() {
        return checkBlobs;
    }

    public void setCheckBlobs(Boolean checkBlobs) {
        this.checkBlobs = checkBlobs;
    }

    /**
     * no-argument constructor wanted by JAXB
     */
    private VerifyStoreManagerRequest() {
    }
}
