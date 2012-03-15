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


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class GalContactInfo extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag gal-contact-id
     * @zm-api-field-description Global Address List contact ID
     */
    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private String id;

    public GalContactInfo() {
        this((String) null);
    }

    public GalContactInfo(String id) { this.id = id; }

    public void setId(String id) { this.id = id; }
    public String getId() { return id; }
}
