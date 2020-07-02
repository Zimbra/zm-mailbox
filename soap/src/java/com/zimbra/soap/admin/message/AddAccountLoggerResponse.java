/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import com.google.common.base.MoreObjects;
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

    /**
     * @zm-api-field-description Information on loggers
     */
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("loggers", loggers);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
