/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
