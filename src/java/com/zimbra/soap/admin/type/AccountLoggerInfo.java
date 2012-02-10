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

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.LoggerInfo;

@XmlAccessorType(XmlAccessType.NONE)
public class AccountLoggerInfo {

    /**
     * @zm-api-field-tag account-name
     * @zm-api-field-description Account name
     */
    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account ID
     */
    @XmlAttribute(name=AdminConstants.A_ID /* id */, required=true)
    private String id;

    /**
     * @zm-api-field-description Logger information
     */
    @XmlElement(name=AdminConstants.E_LOGGER /* logger */, required=true)
    private List<LoggerInfo> loggers = Lists.newArrayList();

    public AccountLoggerInfo() {
        this((String) null, (String) null, (Collection<LoggerInfo>) null);
    }

    public AccountLoggerInfo(String name, String id) {
        this(name, id, (Collection<LoggerInfo>) null);
    }

    public AccountLoggerInfo(String name, String id,
            Collection<LoggerInfo> loggers) {
        this.name = name;
        this.id = id;
        setLoggers(loggers);
    }

    public AccountLoggerInfo setLoggers(Collection<LoggerInfo> loggers) {
        this.loggers.clear();
        if (loggers != null) {
            this.loggers.addAll(loggers);
        }
        return this;
    }

    public AccountLoggerInfo addLogger(LoggerInfo logger) {
        loggers.add(logger);
        return this;
    }

    public List<LoggerInfo> getLoggers() {
        return Collections.unmodifiableList(loggers);
    }
    public String getName() { return name; }
    public String getId() { return id; }
}
