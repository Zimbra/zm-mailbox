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

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class CompletedTestInfo {

    /**
     * @zm-api-field-tag test-name
     * @zm-api-field-description Test name
     */
    @XmlAttribute(name="name", required=true)
    private final String name;

    /**
     * @zm-api-field-tag exec-seconds
     * @zm-api-field-description Number of seconds to execute the test
     */
    @XmlAttribute(name="execSeconds", required=true)
    private final String execSeconds;

    /**
     * @zm-api-field-tag test-class
     * @zm-api-field-description Test class
     */
    @XmlAttribute(name="class", required=true)
    private final String className;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CompletedTestInfo() {
        this((String) null, (String) null, (String) null);
    }

    public CompletedTestInfo(String name, String execSeconds, String className) {
        this.name = name;
        this.execSeconds = execSeconds;
        this.className = className;
    }

    public String getName() { return name; }
    public String getExecSeconds() { return execSeconds; }
    public String getClassName() { return className; }
}
