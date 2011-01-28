package com.zimbra.cs.filter.jsieve;

import org.apache.jsieve.mail.Action;

/**
 */
public class ActionNotify implements Action {

    private String emailAddr;
    private String subjectTemplate;
    private String bodyTemplate;
    // -1 implies no limit
    private int maxBodyBytes;

    public ActionNotify(String emailAddr, String subjectTemplate, String bodyTemplate, int maxBodyBytes) {
        this.emailAddr = emailAddr;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.maxBodyBytes = maxBodyBytes;
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
}
