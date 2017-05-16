/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

import java.util.List;
import java.util.Map;

import org.apache.jsieve.mail.Action;

public class ActionNotifyMailto implements Action {

    private String from;
    private Map<String, String> options;
    private int importance;
    private String message;
    private String mailto;
    private Map<String, List<String>> mailtoParams;

    public ActionNotifyMailto(String from, Map<String, String> options, int importance, String message, String mailto,
            Map<String, List<String>> mailtoParams) {
        this.from = from;
        this.options = options;
        this.importance = importance;
        this.message = message;
        this.mailto = mailto;
        this.mailtoParams = mailtoParams;
    }

    public String getFrom() { return from; }
    public Map<String, String> getOptions() { return options; }
    public int getImportance() { return importance; }
    public String getMessage() { return message; }
    public String getMailto() {     return mailto;    }
    public Map<String, List<String>> getMailtoParams() { return mailtoParams; }
    public void setFrom(String from) { this.from = from; }
    public void setOptions(Map<String, String> options) { this.options = options; }
    public void setImportance(int importance) { this.importance = importance; }
    public void setMessage(String message) { this.message = message; }
    public void setMailto(String mailto) { this.mailto = mailto; }
    public void setMailtoParams(Map<String, List<String>> mailtoParams) { this.mailtoParams = mailtoParams; }
}
