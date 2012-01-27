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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.NamedElement;

/**
 * @zm-api-command-description Get Free/Busy provider information
 * <br />
 * If the optional element <b>&lt;provider/></b> is present in the request, the response contains the requested
 * provider only.  if no provider is supplied in the request, the response contains all the providers.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_FREE_BUSY_QUEUE_INFO_REQUEST)
public class GetFreeBusyQueueInfoRequest {

    /**
     * @zm-api-field-description Provider
     */
    @XmlElement(name=AdminConstants.E_PROVIDER, required=false)
    private NamedElement provider;

    public GetFreeBusyQueueInfoRequest() {
    }

    public void setProvider(NamedElement provider) { this.provider = provider; }
    public NamedElement getProvider() { return provider; }
}
