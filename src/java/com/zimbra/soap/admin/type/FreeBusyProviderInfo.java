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

@XmlAccessorType(XmlAccessType.FIELD)
public class FreeBusyProviderInfo {

    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;

    @XmlAttribute(name=AdminConstants.A_PROPAGATE, required=true)
    private final ZmBoolean propagate;

    @XmlAttribute(name=AdminConstants.A_START, required=true)
    private final long start;

    @XmlAttribute(name=AdminConstants.A_END, required=true)
    private final long end;

    @XmlAttribute(name=AdminConstants.A_QUEUE, required=true)
    private final String queue;

    @XmlAttribute(name=AdminConstants.A_PREFIX, required=true)
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
