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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class AccountSessionInfo {

    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;

    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final String id;

    @XmlElement(name="s", required=false)
    private List<SessionInfo> sessions = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountSessionInfo() {
        this((String) null, (String) null);
    }

    public AccountSessionInfo(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public void setSessions(Iterable <SessionInfo> sessions) {
        this.sessions.clear();
        if (sessions != null) {
            Iterables.addAll(this.sessions,sessions);
        }
    }

    public AccountSessionInfo addSession(SessionInfo session) {
        this.sessions.add(session);
        return this;
    }

    public String getName() { return name; }
    public String getId() { return id; }
    public List<SessionInfo> getSessions() {
        return Collections.unmodifiableList(sessions);
    }
}
