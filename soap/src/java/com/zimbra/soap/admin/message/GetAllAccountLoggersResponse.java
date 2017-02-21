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

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AccountLoggerInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ALL_ACCOUNT_LOGGERS_RESPONSE)
public class GetAllAccountLoggersResponse {

    /**
     * @zm-api-field-description Account loggers that have been created on the given server since the last server start
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT_LOGGER, required=false)
    private List <AccountLoggerInfo> loggers = Lists.newArrayList();

    public GetAllAccountLoggersResponse() {
        this((Collection <AccountLoggerInfo>) null);
    }

    public GetAllAccountLoggersResponse(
            Collection <AccountLoggerInfo> loggers) {
        setLoggers(loggers);
    }

    public GetAllAccountLoggersResponse setLoggers(
            Collection <AccountLoggerInfo> loggers) {
        this.loggers.clear();
        if (loggers != null) {
            this.loggers.addAll(loggers);
        }
        return this;
    }

    public GetAllAccountLoggersResponse addLogger(
            AccountLoggerInfo logger) {
        loggers.add(logger);
        return this;
    }

    public List<AccountLoggerInfo> getLoggers() {
        return Collections.unmodifiableList(loggers);
    }
}
