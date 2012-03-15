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
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-description Delete a volume
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_DELETE_VOLUME_REQUEST)
public final class DeleteVolumeRequest {

    /**
     * @zm-api-field-tag volume-id
     * @zm-api-field-description Volume ID
     */
    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final short id;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private DeleteVolumeRequest() {
         this((short) -1);
    }

    public DeleteVolumeRequest(short value) {
        id = value;
    }

    public short getId() {
        return id;
    }

}
