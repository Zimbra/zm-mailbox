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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.LoggerInfo;
import com.zimbra.soap.type.LoggingLevel;

@XmlAccessorType(XmlAccessType.NONE)
public final class LoggerInfo {

    /**
     * @zm-api-field-tag category-name
     * @zm-api-field-description name of the logger category
     */
    @XmlAttribute(name=AdminConstants.A_CATEGORY, required=true)
    private String category;
    /**
     * @zm-api-field-description level of the logging.
     */
    @XmlAttribute(name=AdminConstants.A_LEVEL, required=false)
    private LoggingLevel level;

    /**
     * no-argument constructor wanted by JAXB
     */
    private LoggerInfo() {
        this((String) null, (LoggingLevel) null);
    }

    private LoggerInfo(String category, LoggingLevel level) {
        this.category = category;
        this.level = level;
    }

    public static LoggerInfo createForCategoryAndLevel(String category, LoggingLevel level) {
        return new LoggerInfo(category, level);
    }

    public static LoggerInfo createForCategoryAndLevelString(String category, String level) throws ServiceException {
        return new LoggerInfo(category, LoggingLevel.fromString(level));
    }

    public String getCategory() { return category; }
    public LoggingLevel getLevel() { return level; }
}
