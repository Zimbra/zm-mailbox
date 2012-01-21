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

    public static LoggerInfo createForCategoryAndLevelString(String category, String level) {
        return new LoggerInfo(category, LoggingLevel.valueOf(level));
    }

    public String getCategory() { return category; }
    public LoggingLevel getLevel() { return level; }
}
