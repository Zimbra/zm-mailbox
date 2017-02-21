/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.filter.jsieve;

import org.apache.jsieve.mail.Action;

import java.util.List;

/**
 */
public class ActionNotify implements Action {

    private String emailAddr;
    private String subjectTemplate;
    private String bodyTemplate;
    // -1 implies no limit
    private int maxBodyBytes;
    private List<String> origHeaders;

    public ActionNotify(
            String emailAddr, String subjectTemplate, String bodyTemplate, int maxBodyBytes, List<String> origHeaders) {
        this.emailAddr = emailAddr;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.maxBodyBytes = maxBodyBytes;
        this.origHeaders = origHeaders;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public String getEmailAddr() {
        return emailAddr;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public int getMaxBodyBytes() {
        return maxBodyBytes;
    }

    public List<String> getOrigHeaders() {
        return origHeaders;
    }
}
