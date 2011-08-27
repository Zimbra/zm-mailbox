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
