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
 * @zm-api-command-description Set current volume.
 * <br />
 * Notes: Each SetCurrentVolumeRequest can set only one current volume type.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SET_CURRENT_VOLUME_REQUEST)
public final class SetCurrentVolumeRequest {

    /**
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final short id;

    /**
     * @zm-api-field-tag volume-type
     * @zm-api-field-description Volume type: 1 (primary message), 2 (secondary message) or 10 (index)
     */
    @XmlAttribute(name=AdminConstants.A_VOLUME_TYPE, required=true)
    private final short type;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SetCurrentVolumeRequest() {
        this((short) -1, (short) -1);
    }

    public SetCurrentVolumeRequest(short id, short type) {
        this.id = id;
        this.type = type;
    }

    public short getType() {
        return type;
    }

    public short getId() {
        return id;
    }
}
