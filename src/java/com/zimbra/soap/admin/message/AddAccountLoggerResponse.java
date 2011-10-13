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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.LoggerInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_ADD_ACCOUNT_LOGGER_RESPONSE)
public class AddAccountLoggerResponse {

    @XmlElement(name=AdminConstants.E_LOGGER /* logger */, required=false)
    private List<LoggerInfo> loggers = Lists.newArrayList();

    private AddAccountLoggerResponse() {
    }

    private AddAccountLoggerResponse(Iterable <LoggerInfo> loggers) {
        setLoggers(loggers);
    }

    public static AddAccountLoggerResponse create(Iterable <LoggerInfo> loggers) {
        return new AddAccountLoggerResponse(loggers);
    }

    public void setLoggers(Iterable <LoggerInfo> loggers) {
        this.loggers.clear();
        if (loggers != null) {
            Iterables.addAll(this.loggers,loggers);
        }
    }

    public void addLogger(LoggerInfo logger) {
        this.loggers.add(logger);
    }

    public List<LoggerInfo> getLoggers() {
        return Collections.unmodifiableList(loggers);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("loggers", loggers);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
