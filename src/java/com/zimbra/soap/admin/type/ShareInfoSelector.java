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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountSelector;

@XmlAccessorType(XmlAccessType.FIELD)
public class ShareInfoSelector {

    @XmlEnum
    public enum PubShareInfoAction {
        // case must match protocol
        add, remove;

        public static PubShareInfoAction fromString(String s)
        throws ServiceException {
            try {
                return PubShareInfoAction.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST(
                        "unknown ShareInfo action: "+s, e);
            }
        }
    }

    @XmlAttribute(name=AdminConstants.A_ACTION, required=true)
    private final PubShareInfoAction action;

    @XmlElement(name=AdminConstants.E_FOLDER, required=true)
    private final PublishFolderInfo folder;

    @XmlElement(name=AdminConstants.E_OWNER, required=false)
    private final AccountSelector owner;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ShareInfoSelector() {
        this((PubShareInfoAction)null, (PublishFolderInfo) null,
                (AccountSelector) null);
    }

    public ShareInfoSelector(PubShareInfoAction action,
                PublishFolderInfo folder) {
        this(action, folder, (AccountSelector) null);
    }

    public ShareInfoSelector(PubShareInfoAction action,
                PublishFolderInfo folder, AccountSelector owner) {
        this.action = action;
        this.folder = folder;
        this.owner = owner;
    }

    public PubShareInfoAction getAction() { return action; }
    public PublishFolderInfo getFolder() { return folder; }
    public AccountSelector getOwner() { return owner; }
}
