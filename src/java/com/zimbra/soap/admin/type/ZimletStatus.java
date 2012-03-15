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

import java.util.Arrays;

import com.zimbra.common.service.ServiceException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class ZimletStatus {

    @XmlEnum
    public enum ZimletStatusSetting {
        // case must match protocol
        enabled, disabled;

        public static ZimletStatusSetting fromString(String s)
        throws ServiceException {
            try {
                return ZimletStatusSetting.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid status setting: " + s + ", valid values: "
                        + Arrays.asList(values()), null);
            }
        }
    }

    /**
     * @zm-api-field-tag zimlet-name
     * @zm-api-field-description Zimlet name
     */
    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag status
     * @zm-api-field-descriptio Status
     */
    @XmlAttribute(name=AdminConstants.A_STATUS /* status */, required=true)
    private final ZimletStatusSetting status;

    /**
     * @zm-api-field-tag extension
     * @zm-api-field-description Extension
     */
    @XmlAttribute(name=AdminConstants.A_EXTENSION /* extension */, required=true)
    private final ZmBoolean extension;

    /**
     * @zm-api-field-tag priority
     * @zm-api-field-description Priority
     */
    @XmlAttribute(name=AdminConstants.A_PRIORITY /* priority */, required=false)
    private final Integer priority;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ZimletStatus() {
        this((String) null, (ZimletStatusSetting) null, false, (Integer) null);
    }

    public ZimletStatus(String name, ZimletStatusSetting status, boolean extension, Integer priority) {
        this.name = name;
        this.status = status;
        this.extension = ZmBoolean.fromBool(extension);
        this.priority = priority;
    }

    public String getName() { return name; }
    public ZimletStatusSetting getStatus() { return status; }
    public boolean getExtension() { return ZmBoolean.toBool(extension); }
    public Integer getPriority() { return priority; }
}
