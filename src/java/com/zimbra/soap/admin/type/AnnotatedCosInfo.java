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

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class AnnotatedCosInfo extends CosInfo {

    /**
     * @zm-api-field-tag is-default-cos
     * @zm-api-field-description Flag whether is the default Class Of Service (COS)
     */
    @XmlAttribute(name=AdminConstants.A_IS_DEFAULT_COS, required=false)
    private final ZmBoolean isDefaultCos;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AnnotatedCosInfo() {
        this((String) null,(String) null, (Boolean)null,
                (Collection <Attr>) null);
    }

    public AnnotatedCosInfo(String id, String name) {
        this(id, name, (Boolean)null, (Collection <Attr>) null);
    }

    public AnnotatedCosInfo(String id, String name, Boolean isDefaultCos) {
        this(id, name, isDefaultCos, (Collection <Attr>) null);
    }

    public AnnotatedCosInfo(String id, String name, Boolean isDefaultCos,
            Collection <Attr> attrs) {
        super(id, name, attrs);
        this.isDefaultCos = ZmBoolean.fromBool(isDefaultCos);
    }

    public Boolean getIsDefaultCos() { return ZmBoolean.toBool(isDefaultCos); }
}
