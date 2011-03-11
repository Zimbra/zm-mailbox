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

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class WaitSetAddSpec {

    @XmlAttribute(name=MailConstants.A_NAME, required=false)
    private final String name;

    @XmlAttribute(name=MailConstants.A_ID, required=false)
    private final String id;

    @XmlAttribute(name=MailConstants.A_TOKEN, required=false)
    private final String token;

    @XmlAttribute(name=MailConstants.A_TYPES, required=false)
    private final String interests;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private WaitSetAddSpec() {
        this((String) null, (String) null, (String) null, (String) null);
    }

    public WaitSetAddSpec(String name, String id, String token,
                    String interests) {
        this.name = name;
        this.id = id;
        this.token = token;
        this.interests = interests;
    }

    public String getName() { return name; }
    public String getId() { return id; }
    public String getToken() { return token; }
    public String getInterests() { return interests; }
}
