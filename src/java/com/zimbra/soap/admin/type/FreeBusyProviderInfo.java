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
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class FreeBusyProviderInfo {

    /**
     * @zm-api-field-tag provider-name
     * @zm-api-field-description Provider name
     */
    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag propagate-flag
     * @zm-api-field-description Propagate flag
     */
    @XmlAttribute(name=AdminConstants.A_PROPAGATE /* propagate */, required=true)
    private final ZmBoolean propagate;

    /**
     * @zm-api-field-tag fb-cache-start-time-secs-since-epoch
     * @zm-api-field-description Free/Busy cache start time in seconds since the epoch
     */
    @XmlAttribute(name=AdminConstants.A_START /* start */, required=true)
    private final long start;

    /**
     * @zm-api-field-tag fb-cache-end-time-secs-since-epoch
     * @zm-api-field-description Free/Busy cache end time in seconds since the epoch
     */
    @XmlAttribute(name=AdminConstants.A_END /* end */, required=true)
    private final long end;

    /**
     * @zm-api-field-tag queue location
     * @zm-api-field-description Queue location
     */
    @XmlAttribute(name=AdminConstants.A_QUEUE /* queue */, required=true)
    private final String queue;

    /**
     * @zm-api-field-tag prefix-used-in-zimbra-ForeignPrincipal
     * @zm-api-field-description Prefix used in Zimbra ForeignPrincipal
     */
    @XmlAttribute(name=AdminConstants.A_PREFIX /* prefix */, required=true)
    private final String prefix;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private FreeBusyProviderInfo() {
        this((String) null, false, -1L, -1L, (String) null, (String) null);
    }

    public FreeBusyProviderInfo(String name, boolean propagate,
                    long start, long end, String queue, String prefix) {
        this.name = name;
        this.propagate = ZmBoolean.fromBool(propagate);
        this.start = start;
        this.end = end;
        this.queue = queue;
        this.prefix = prefix;
    }

    public String getName() { return name; }
    public boolean getPropagate() { return ZmBoolean.toBool(propagate); }
    public long getStart() { return start; }
    public long getEnd() { return end; }
    public String getQueue() { return queue; }
    public String getPrefix() { return prefix; }
}
