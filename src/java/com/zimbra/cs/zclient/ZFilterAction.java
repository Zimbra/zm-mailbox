/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class ZFilterAction {

    public static final String A_DISCARD = "discard";
    public static final String A_FILEINTO = "fileinto";
    public static final String A_FLAG = "flag";
    public static final String A_KEEP = "keep";
    public static final String A_REDIRECT = "redirect";
    public static final String A_STOP = "stop";
    public static final String A_TAG = "tag";

    public enum MarkOp {

        READ, FLAGGED;

        private static String[] mArgs = { "read", "flagged" };

        public static MarkOp fromString(String s) throws ServiceException {
            try {
                return valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: "+s+", value values: "+Arrays.asList(values()), null);
            }
        }

        public String toProtoArg() { return mArgs[ordinal()]; }

        public static MarkOp fromProtoString(String s) throws ServiceException {
            s = s.trim().toLowerCase();
            for (int i=0; i < mArgs.length; i++)
                if (mArgs[i].equals(s)) return values()[i];
            throw ZClientException.CLIENT_ERROR("invalid arg "+s+", valid values: "+ Arrays.asList(mArgs), null);
        }
    }

    private String mName;
    protected List<String> mArgs;

    private ZFilterAction(String name, String... args) {
        mName = name;
        mArgs = Collections.unmodifiableList(args != null ? Arrays.asList(args) : new ArrayList<String>());
    }

    public String getName() {
        return mName;
    }

    public List<String> getArgs() {
        return mArgs;
    }

    public abstract String toActionString();

    Element toElement(Element parent) {
        Element a = parent.addElement(MailConstants.E_ACTION);
        a.addAttribute(MailConstants.A_NAME, mName);
        for (String arg : mArgs) {
            a.addElement(MailConstants.E_FILTER_ARG).setText(arg);
        }
        return a;
    }


    private static String getArg(Element e) throws ServiceException {
        String value = e.getElement(MailConstants.E_FILTER_ARG).getText();
        List<String> slist = StringUtil.parseSieveStringList(value);
        if (slist.size() != 1) {
            throw ZClientException.CLIENT_ERROR(String.format("unable to parse arg value(%s)", value), null);
        }
        return slist.get(0);
    }

    public static ZFilterAction getAction(Element actionElement) throws ServiceException {
        String n = actionElement.getAttribute(MailConstants.A_NAME).toLowerCase();
        if (n.equals(A_KEEP))
            return new ZKeepAction();
        else if (n.equals(A_DISCARD))
            return new ZDiscardAction();
        else if (n.equals(A_STOP))
            return new ZStopAction();
        else if (n.equals(A_FILEINTO))
            return new ZFileIntoAction(getArg(actionElement));
        else if (n.equals(A_TAG))
            return new ZTagAction(getArg(actionElement));
        else if (n.equals(A_REDIRECT))
            return new ZRedirectAction(getArg(actionElement));
        else if (n.equals(A_FLAG))
            return new ZMarkAction(MarkOp.fromProtoString(getArg(actionElement)));
        else
            throw ZClientException.CLIENT_ERROR("unknown filter action: "+n, null);
    }

    public static class ZKeepAction extends ZFilterAction {
        public ZKeepAction() { super(A_KEEP); }

        public String toActionString() { return "keep"; }
    }

    public static class ZDiscardAction extends ZFilterAction {
        public ZDiscardAction() { super(A_DISCARD); }

        public String toActionString() { return "discard"; }
    }

    public static class ZStopAction extends ZFilterAction {
        public ZStopAction() { super(A_STOP); }

        public String toActionString() { return "stop"; }
    }

    public static class ZFileIntoAction extends ZFilterAction {
        public ZFileIntoAction(String folderPath) { super(A_FILEINTO, folderPath); }

        public String getFolderPath() { return mArgs.get(0); }

        public String toActionString() { return "fileinto " + ZFilterRule.quotedString(getFolderPath()); }
    }

    public static class ZTagAction extends ZFilterAction {
        public ZTagAction(String tagName) { super(A_TAG, tagName); }
        public String getTagName() { return mArgs.get(0); }

        public String toActionString() { return "tag " + ZFilterRule.quotedString(getTagName()); }
    }

    public static class ZMarkAction extends ZFilterAction {
        private MarkOp mMarkOp;
        public ZMarkAction(MarkOp op) {
            super(A_FLAG, op.toProtoArg());
            mMarkOp = op;
        }

        public String getMarkOp() { return mMarkOp.name(); }
        public String toActionString() { return "mark " + mMarkOp.name().toLowerCase(); }
    }

    public static class ZRedirectAction extends ZFilterAction {
        public ZRedirectAction(String address) { super(A_REDIRECT, address); }
        public String getAddress() { return mArgs.get(0); }
        public String toActionString() { return "redirect " + ZFilterRule.quotedString(getAddress()); }
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("name", mName);
        sb.add("args", mArgs, false, false);
        sb.endStruct();
        return sb.toString();
    }
}
