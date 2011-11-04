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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.SimpleSessionInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_SESSIONS_RESPONSE)
public class GetSessionsResponse {

    @XmlAttribute(name=AdminConstants.A_MORE, required=true)
    private final ZmBoolean more;

    @XmlAttribute(name=AdminConstants.A_TOTAL, required=true)
    private final int total;

    @XmlElement(name="s", required=false)
    private List<SimpleSessionInfo> sessions = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetSessionsResponse() {
        this(false, -1);
    }

    public GetSessionsResponse(boolean more, int total) {
        this.more = ZmBoolean.fromBool(more);
        this.total = total;
    }

    public void setSessions(Iterable <SimpleSessionInfo> sessions) {
        this.sessions.clear();
        if (sessions != null) {
            Iterables.addAll(this.sessions,sessions);
        }
    }

    public GetSessionsResponse addSession(SimpleSessionInfo session) {
        this.sessions.add(session);
        return this;
    }

    public boolean getMore() { return ZmBoolean.toBool(more); }
    public int getTotal() { return total; }
    public List<SimpleSessionInfo> getSessions() {
        return Collections.unmodifiableList(sessions);
    }
}
