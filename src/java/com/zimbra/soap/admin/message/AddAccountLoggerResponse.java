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

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.LoggerInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_ADD_ACCOUNT_LOGGER_RESPONSE)
public class AddAccountLoggerResponse {

    @XmlElement(name=AdminConstants.E_LOGGER, required=false)
    private List <LoggerInfo> loggers = Lists.newArrayList();

    public AddAccountLoggerResponse() {
        this((Collection <LoggerInfo>) null);
    }

    public AddAccountLoggerResponse(Collection<LoggerInfo> loggers) {
        setLoggers(loggers);
    }

    public AddAccountLoggerResponse setLoggers(Collection<LoggerInfo> loggers) {
        this.loggers.clear();
        if (loggers != null) {
            this.loggers.addAll(loggers);
        }
        return this;
    }

    public AddAccountLoggerResponse addLogger(LoggerInfo logger) {
        loggers.add(logger);
        return this;
    }

    public List<LoggerInfo> getLoggers() {
        return Collections.unmodifiableList(loggers);
    }
}
