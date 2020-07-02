/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.client;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.zimbra.common.filter.Sieve;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.soap.mail.type.FilterAction;

import org.json.JSONException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class ZFilterAction implements ToZJSONObject {

    public static final String A_DISCARD = "discard";
    public static final String A_FILEINTO = "fileinto";
    public static final String A_FLAG = "flag";
    public static final String A_KEEP = "keep";
    public static final String A_REDIRECT = "redirect";
    public static final String A_REPLY = "reply";
    public static final String A_NOTIFY = "notify";
    public static final String A_RFCCOMPLIANTNOTIFY = "rfccompliantnotify";
    public static final String A_STOP = "stop";
    public static final String A_TAG = "tag";

    public enum MarkOp {
        READ, FLAGGED, PRIORITY;

        public static MarkOp fromString(String s) throws ServiceException {
            try {
                return valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: " + s + ", valid values: " + Arrays.asList(values()),
                        null);
            }
        }
    }

    private final String name;
    protected final List<String> args;

    private ZFilterAction(String name, String... args) {
        this.name = name;
        this.args = args != null ? ImmutableList.copyOf(args) : Collections.<String>emptyList();
    }

    public static ZFilterAction of(FilterAction action) throws ServiceException {
        if (action instanceof FilterAction.DiscardAction) {
            return new ZDiscardAction();
        } else if (action instanceof FilterAction.FileIntoAction) {
            return new ZFileIntoAction((FilterAction.FileIntoAction) action);
        } else if (action instanceof FilterAction.FlagAction) {
            return new ZMarkAction((FilterAction.FlagAction) action);
        } else if (action instanceof FilterAction.KeepAction) {
            return new ZKeepAction();
        } else if (action instanceof FilterAction.RedirectAction) {
            return new ZRedirectAction((FilterAction.RedirectAction) action);
        } else if (action instanceof FilterAction.ReplyAction) {
            return new ZReplyAction((FilterAction.ReplyAction) action);
        } else if (action instanceof FilterAction.NotifyAction) {
            return new ZNotifyAction((FilterAction.NotifyAction) action);
        } else if (action instanceof FilterAction.RFCCompliantNotifyAction) {
            return new ZRFCCompliantNotifyAction((FilterAction.RFCCompliantNotifyAction) action);
        } else if (action instanceof FilterAction.StopAction) {
            return new ZStopAction();
        } else if (action instanceof FilterAction.TagAction) {
            return new ZTagAction((FilterAction.TagAction) action);
        }
        throw new IllegalArgumentException(action.getClass().getName());
    }

    public String getName() {
        return name;
    }

    public List<String> getArgs() {
        return args;
    }

    public abstract String toActionString();
    abstract FilterAction toJAXB();

    public static final class ZKeepAction extends ZFilterAction {
        public ZKeepAction() {
            super(A_KEEP);
        }

        @Override
        public String toActionString() {
            return "keep";
        }

        @Override
        FilterAction.KeepAction toJAXB() {
            return new FilterAction.KeepAction();
        }
    }

    public static final class ZDiscardAction extends ZFilterAction {
        public ZDiscardAction() {
            super(A_DISCARD);
        }

        @Override
        public String toActionString() {
            return "discard";
        }

        @Override
        FilterAction.DiscardAction toJAXB() {
            return new FilterAction.DiscardAction();
        }
    }

    public static final class ZStopAction extends ZFilterAction {
        public ZStopAction() {
            super(A_STOP);
        }

        @Override
        public String toActionString() {
            return "stop";
        }

        @Override
        FilterAction.StopAction toJAXB() {
            return new FilterAction.StopAction();
        }
    }

    public static final class ZFileIntoAction extends ZFilterAction {
        public ZFileIntoAction(String folderPath) {
            super(A_FILEINTO, folderPath);
        }

        public ZFileIntoAction(FilterAction.FileIntoAction fileinto) {
            this(fileinto.getFolder());
        }

        public String getFolderPath() {
            return args.get(0);
        }

        @Override
        public String toActionString() {
            return "fileinto " + ZFilterRule.quotedString(getFolderPath());
        }

        @Override
        FilterAction.FileIntoAction toJAXB() {
            return new FilterAction.FileIntoAction(getFolderPath());
        }
    }

    public static final class ZTagAction extends ZFilterAction {
        public ZTagAction(String tagName) {
            super(A_TAG, tagName);
        }

        public ZTagAction(FilterAction.TagAction action) {
            this(action.getTag());
        }

        public String getTagName() {
            return args.get(0);
        }

        @Override
        public String toActionString() {
            return "tag " + ZFilterRule.quotedString(getTagName());
        }

        @Override
        FilterAction.TagAction toJAXB() {
            return new FilterAction.TagAction(getTagName());
        }
    }

    public static class ZMarkAction extends ZFilterAction {
        private MarkOp op;

        public ZMarkAction(MarkOp op) {
            super(A_FLAG, op.name().toLowerCase());
            this.op = op;
        }

        public ZMarkAction(FilterAction.FlagAction flag) throws ServiceException {
            this(MarkOp.fromString(flag.getFlag()));
        }

        public String getMarkOp() {
            return op.name();
        }

        @Override
        public String toActionString() {
            return "mark " + op.name().toLowerCase();
        }

        @Override
        FilterAction.FlagAction toJAXB() {
            switch (op) {
                case FLAGGED:
                    return new FilterAction.FlagAction(Sieve.Flag.flagged.name());
                case READ:
                    return new FilterAction.FlagAction(Sieve.Flag.read.name());
                case PRIORITY:
                    return new FilterAction.FlagAction(Sieve.Flag.priority.name());
                default:
                    throw new IllegalStateException(op.toString());
            }
        }
    }

    public static final class ZRedirectAction extends ZFilterAction {
        public ZRedirectAction(String address) {
            super(A_REDIRECT, address);
        }

        public ZRedirectAction(FilterAction.RedirectAction action) {
            this(action.getAddress());
        }

        public String getAddress() {
            return args.get(0);
        }

        @Override
        public String toActionString() {
            return "redirect " + ZFilterRule.quotedString(getAddress());
        }

        @Override
        FilterAction.RedirectAction toJAXB() {
            return new FilterAction.RedirectAction(getAddress());
        }
    }

    public static final class ZReplyAction extends ZFilterAction {
        public ZReplyAction(String bodyTemplate) {
            super(A_REPLY, bodyTemplate);
        }

        public ZReplyAction(FilterAction.ReplyAction action) {
            this(action.getContent());
        }

        public String getBodyTemplate() {
            return args.get(0);
        }

        @Override
        public String toActionString() {
            return "reply " + getBodyTemplate();
        }

        @Override
        FilterAction.ReplyAction toJAXB() {
            return new FilterAction.ReplyAction(getBodyTemplate());
        }
    }

    public static final class ZNotifyAction extends ZFilterAction {
        public ZNotifyAction(String emailAddr, String subjectTemplate, String bodyTemplate) {
            this(emailAddr, subjectTemplate, bodyTemplate, -1);
        }

        public ZNotifyAction(String emailAddr, String subjectTemplate, String bodyTemplate, int maxBodyBytes) {
            this(emailAddr, subjectTemplate, bodyTemplate, maxBodyBytes, "");
        }

        public ZNotifyAction(String emailAddr, String subjectTemplate, String bodyTemplate, int maxBodyBytes,
                String origHeaders) {
            super(A_NOTIFY, emailAddr, Strings.nullToEmpty(subjectTemplate), Strings.nullToEmpty(bodyTemplate),
                    Integer.toString(maxBodyBytes), Strings.nullToEmpty(origHeaders));
        }

        public ZNotifyAction(FilterAction.NotifyAction action) {
            this(action.getAddress(), action.getSubject(), action.getContent(), action.getMaxBodySize(),
                    action.getOrigHeaders());
        }

        public String getEmailAddr() {
            return args.get(0);
        }

        public String getSubjectTemplate() {
            return args.get(1);
        }

        public String getBodyTemplate() {
            return args.get(2);
        }

        public int getMaxBodyBytes() {
            return Integer.valueOf(args.get(3));
        }

        public String getOrigHeaders() {
            return args.get(4);
        }

        @Override
        public String toActionString() {
            return "notify " + getEmailAddr() + " " +
                    (StringUtil.enclose(getSubjectTemplate(), '"')) + " " +
                    (StringUtil.enclose(getBodyTemplate(), '"')) +
                    (getMaxBodyBytes() == -1 ? "" : " " + getMaxBodyBytes());
        }

        @Override
        FilterAction.NotifyAction toJAXB() {
            FilterAction.NotifyAction action = new FilterAction.NotifyAction();
            action.setAddress(getEmailAddr());
            action.setSubject(getSubjectTemplate());
            action.setContent(getBodyTemplate());
            action.setMaxBodySize(getMaxBodyBytes());
            action.setOrigHeaders(getOrigHeaders());
            return action;
        }
    }
 
    public static final class ZRFCCompliantNotifyAction extends ZFilterAction {
        public ZRFCCompliantNotifyAction(String method) {
            this("", "", "", "", method);
        }

        public ZRFCCompliantNotifyAction(String from, String message, String method) {
            this(from, "", "", message, method);
        }

        public ZRFCCompliantNotifyAction(String from, String importance, String options, String message, String method) {
            super(A_RFCCOMPLIANTNOTIFY, Strings.nullToEmpty(from), Strings.nullToEmpty(importance),
                    Strings.nullToEmpty(options), Strings.nullToEmpty(message),
                    Strings.nullToEmpty(method));
        }

        public ZRFCCompliantNotifyAction(FilterAction.RFCCompliantNotifyAction action) {
            this(action.getFrom(), action.getImportance(), action.getOptions(),
                    action.getMessage(), action.getMethod());
        }

        public String getFrom() {
            return args.get(0);
        }

        public String getImportance() {
            return args.get(1);
        }

        public String getOptions() {
            return args.get(2);
        }

        public String getMessage() {
            return args.get(3);
        }

        public String getMethod() {
            return args.get(4);
        }

        @Override
        public String toActionString() {
            StringBuilder str = new StringBuilder("notify ");
            str.append(getFrom()).append(" ");
            if (!getImportance().isEmpty()) {
                str.append(StringUtil.enclose(getImportance(), '"')).append(" ");
            }
            if (!getOptions().isEmpty()) {
                str.append(StringUtil.enclose(getOptions(), '"')).append(" ");
            }
            str.append(StringUtil.enclose(getMessage(), '"')).append(" ")
               .append(StringUtil.enclose(getMethod(), '"'));
            return str.toString();
        }

        @Override
        FilterAction.RFCCompliantNotifyAction toJAXB() {
            FilterAction.RFCCompliantNotifyAction action = new FilterAction.RFCCompliantNotifyAction();
            action.setFrom(getFrom());
            action.setImportance(getImportance());
            action.setOptions(getOptions());
            action.setMessage(getMessage());
            action.setMethod(getMethod());
            return action;
        }
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("name", name);
        jo.putList("args", args);
        return jo;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("name", name).toString();
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}
