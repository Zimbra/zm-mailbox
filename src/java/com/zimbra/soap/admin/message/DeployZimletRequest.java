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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.admin.type.AttachmentIdAttrib;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_DEPLOY_ZIMLET_REQUEST)
public class DeployZimletRequest {

    @XmlAttribute(name=AdminConstants.A_ACTION, required=true)
    private final String action;

    @XmlAttribute(name=AdminConstants.A_FLUSH, required=false)
    private final Boolean flushCache;

    @XmlAttribute(name=AdminConstants.A_SYNCHRONOUS, required=false)
    private final Boolean synchronous;

    @XmlElement(name=MailConstants.E_CONTENT, required=true)
    private final AttachmentIdAttrib content;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DeployZimletRequest() {
        this((String) null, (Boolean) null, (Boolean) null,
                (AttachmentIdAttrib) null);
    }

    public DeployZimletRequest(String action, Boolean flushCache,
                    Boolean synchronous, AttachmentIdAttrib content) {
        this.action = action;
        this.flushCache = flushCache;
        this.synchronous = synchronous;
        this.content = content;
    }

    public String getAction() { return action; }
    public Boolean getFlushCache() { return flushCache; }
    public Boolean getSynchronous() { return synchronous; }
    public AttachmentIdAttrib getContent() { return content; }
}
