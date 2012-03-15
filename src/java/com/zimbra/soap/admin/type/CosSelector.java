/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class CosSelector {

    @XmlEnum
    public static enum CosBy {
        // case must match protocol
        id, name;

        public static CosBy fromString(String s) throws ServiceException {
            try {
                return CosBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    /**
     * @zm-api-field-tag cos-selector-by
     * @zm-api-field-description Select the meaning of <b>{cos-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY) private final CosBy cosBy;

    /**
     * @zm-api-field-tag cos-selector-key
     * @zm-api-field-description The key used to identify the COS. Meaning determined by <b>{cos-selector-by}</b>
     */
    @XmlValue private final String key;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CosSelector() {
        this.cosBy = null;
        this.key = null;
    }

    public CosSelector(CosBy by, String key) {
        this.cosBy = by;
        this.key = key;
    }

    public String getKey() { return key; }

    public CosBy getBy() { return cosBy; }
}
