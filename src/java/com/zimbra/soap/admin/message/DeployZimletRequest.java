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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.admin.type.AttachmentIdAttrib;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Deploy Zimlet(s)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_DEPLOY_ZIMLET_REQUEST)
public class DeployZimletRequest {

    /**
     * @zm-api-field-tag action
     * @zm-api-field-description Action - valid values : deployAll|deployLocal|status
     */
    @XmlAttribute(name=AdminConstants.A_ACTION /* action */, required=true)
    private final String action;

    /**
     * @zm-api-field-tag flush-cache
     * @zm-api-field-description Flag whether to flush the cache
     */
    @XmlAttribute(name=AdminConstants.A_FLUSH /* flush */, required=false)
    private final ZmBoolean flushCache;

    /**
     * @zm-api-field-description Synchronous flag
     */
    @XmlAttribute(name=AdminConstants.A_SYNCHRONOUS /* synchronous */, required=false)
    private final ZmBoolean synchronous;

    /**
     * @zm-api-field-description Content
     */
    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=true)
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
        this.flushCache = ZmBoolean.fromBool(flushCache);
        this.synchronous = ZmBoolean.fromBool(synchronous);
        this.content = content;
    }

    public String getAction() { return action; }
    public Boolean getFlushCache() { return ZmBoolean.toBool(flushCache); }
    public Boolean getSynchronous() { return ZmBoolean.toBool(synchronous); }
    public AttachmentIdAttrib getContent() { return content; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("action", action)
            .add("flushCache", flushCache)
            .add("synchronous", synchronous)
            .add("content", content);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
