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
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class FailedTestInfo {

    /**
     * @zm-api-field-tag failed-test-name
     * @zm-api-field-description Failed test name
     */
    @XmlAttribute(name="name", required=true)
    private final String name;

    /**
     * @zm-api-field-tag failed-test-exec-seconds
     * @zm-api-field-description Failed test execution time
     */
    @XmlAttribute(name="execSeconds", required=true)
    private final String execSeconds;

    /**
     * @zm-api-field-tag failed-test-class
     * @zm-api-field-description Failed test class name
     */
    @XmlAttribute(name="class", required=true)
    private final String className;

    /**
     * @zm-api-field-tag throwable
     * @zm-api-field-description Text of any exception thrown during the test
     */
    @XmlValue
    private final String throwable;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private FailedTestInfo() {
        this((String) null, (String) null, (String) null, (String) null);
    }

    public FailedTestInfo(String name, String execSeconds,
                    String className, String throwable) {
        this.name = name;
        this.execSeconds = execSeconds;
        this.className = className;
        this.throwable = throwable;
    }

    public String getName() { return name; }
    public String getExecSeconds() { return execSeconds; }
    public String getClassName() { return className; }
    public String getThrowable() { return throwable; }
}
