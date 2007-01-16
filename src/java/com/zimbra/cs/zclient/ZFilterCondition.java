/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ZFilterCondition {

    public static final String C_ADDRESSBOOK = "addressbook";
    public static final String C_ATTACHMENT = "attachment";
    public static final String C_BODY = "body";
    public static final String C_DATE = "date";
    public static final String C_EXISTS = "exists";
    public static final String C_NOT_EXISTS = "not exists";
    public static final String C_HEADER = "header";
    public static final String C_NOT_ATTACHMENT = "not attachment";
    public static final String C_SIZE = "size";

    public enum HeaderOp {

        IS, NOT_IS, CONTAINS, NOT_CONTAINS, MATCHES, NOT_MATCHES;

        private static String[] mOps = { ":is", "not :is", ":contains", "not :contains", ":matches", "not :matches" };

        public String toProtoOp() { return mOps[ordinal()]; }

        public static HeaderOp fromString(String s) throws ServiceException {
            s = s.trim().toLowerCase();
            for (int i=0; i < mOps.length; i++)
                if (mOps[i].equals(s)) return values()[i];
            throw ZClientException.CLIENT_ERROR("invalid op: "+s+", valid values: "+ Arrays.asList(mOps), null);
        }
    }

    public enum DateOp {

        BEFORE, NOT_BEFORE, AFTER, NOT_AFTER;

        private static String[] mOps = { ":before", "not :before", ":after", "not :after" };

        public String toProtoOp() { return mOps[ordinal()]; }

        public static DateOp fromString(String s) throws ServiceException {
            s = s.trim().toLowerCase();
            for (int i=0; i < mOps.length; i++)
                if (mOps[i].equals(s)) return values()[i];
            throw ZClientException.CLIENT_ERROR("invalid op: "+s+", valid values: "+ Arrays.asList(mOps), null);
        }
    }

    public enum SizeOp {
        UNDER, NOT_UNDER, OVER, NOT_OVER;

        private static String[] mOps = { ":under", "not :under", ":over", "not :over" };

        public String toProtoOp() { return mOps[ordinal()]; }

        public static SizeOp fromString(String s) throws ServiceException {
            s = s.trim().toLowerCase();
            for (int i=0; i < mOps.length; i++)
                if (mOps[i].equals(s)) return values()[i];
            throw ZClientException.CLIENT_ERROR("invalid op: "+s+", valid values: "+ Arrays.asList(mOps), null);
        }
    }

    public enum BodyOp {
        CONTAINS, NOT_CONTAINS;

        private static String[] mOps = { ":contains", "not :contains" };

        public String toProtoOp() { return mOps[ordinal()]; }

        public static BodyOp fromString(String s) throws ServiceException {
            s = s.trim().toLowerCase();
            for (int i=0; i < mOps.length; i++)
                if (mOps[i].equals(s)) return values()[i];
            throw ZClientException.CLIENT_ERROR("invalid op: "+s+", valid values: "+ Arrays.asList(mOps), null);
        }
    }

    public enum AddressBookOp {
        IN, NOT_IN;

        private static String[] mOps = { ":in", "not :in" };

        public String toProtoOp() { return mOps[ordinal()]; }

        public static AddressBookOp fromString(String s) throws ServiceException {
            s = s.trim().toLowerCase();
            for (int i=0; i < mOps.length; i++)
                if (mOps[i].equals(s)) return values()[i];
            throw ZClientException.CLIENT_ERROR("invalid op: "+s+", valid values: "+ Arrays.asList(mOps), null);
        }

    }

    protected String mName;
    protected String mOp;
    protected String mK0;
    protected String mK1;

    private static String getAttrK(Element e, String k) throws ServiceException {
        String value = e.getAttribute(k);
        List<String> slist = StringUtil.parseSieveStringList(value);
        if (slist.size() != 1) {
            throw ZClientException.CLIENT_ERROR(String.format("unable to parse attr(%s) value(%s)", k, value), null);
        }
        return slist.get(0);
    }

    public static String getK0(Element e) throws ServiceException {
        return getAttrK(e, MailConstants.A_LHS);
    }

    public static String getK1(Element e) throws ServiceException {
        return getAttrK(e, MailConstants.A_RHS);
    }

    public static ZFilterCondition getCondition(Element condEl) throws ServiceException {
        String n = condEl.getAttribute(MailConstants.A_NAME);
        if (n.equals(C_HEADER)) {
            return new ZHeaderCondition(
                    getK0(condEl),
                    HeaderOp.fromString(condEl.getAttribute(MailConstants.A_OPERATION)),
                    getK1(condEl));
        } else if (n.equals(C_EXISTS)) {
            return new ZHeaderExistsCondition(
                    getK0(condEl),
                    true);
        } else if (n.equals(C_NOT_EXISTS)) {
            return new ZHeaderExistsCondition(
                    getK0(condEl),
                    true);
        } if (n.equals(C_DATE)) {
            try {
                return new ZDateCondition(
                        new SimpleDateFormat("yyyyMMdd").parse(getK1(condEl)),
                        DateOp.fromString(condEl.getAttribute(MailConstants.A_OPERATION)));
            } catch (ParseException e) {
                throw ZClientException.CLIENT_ERROR("unable to parse filter date: "+e, null);
            }
        } else if (n.equals(C_SIZE)) {
            return new ZSizeCondition(
                    condEl.getAttribute(MailConstants.A_RHS),
                    SizeOp.fromString(condEl.getAttribute(MailConstants.A_OPERATION)));
        } else if (n.equals(C_BODY)) {
            return new ZBodyCondition(BodyOp.fromString(condEl.getAttribute(MailConstants.A_OPERATION)),getK1(condEl));
        } else if (n.equals(C_ATTACHMENT)) {
            return new ZAttachmentExistsCondition(true);
        } else if (n.equals(C_NOT_ATTACHMENT)) {
            return new ZAttachmentExistsCondition(false);
        } else if (n.equals(C_ADDRESSBOOK)) {
            return new ZAddressBookCondition(
                    AddressBookOp.fromString(condEl.getAttribute(MailConstants.A_OPERATION)),
                    getK0(condEl));
        } else {
             throw ZClientException.CLIENT_ERROR("unknown filter condition: "+n, null);
        }
    }

    Element toElement(Element parent) {
        Element c = parent.addElement(MailConstants.E_CONDITION);
        c.addAttribute(MailConstants.A_NAME, mName);
        if (mOp != null) c.addAttribute(MailConstants.A_OPERATION, mOp);
        if (mK0 != null) c.addAttribute(MailConstants.A_LHS, mK0);
        if (mK1 != null) c.addAttribute(MailConstants.A_RHS, mK1);
        return c;
    }

    private ZFilterCondition(String name, String op, String k0, String k1) {
        mName = name;
        mOp = op;
        mK0 = k0;
        mK1 = k1;
    }

    public String getName() { return mName; }

    public String getK0() { return mK0; }
    public String getK1() { return mK1; }
    public String getOp() { return mOp; }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("name", mName);
        sb.add("op", mOp);
        sb.add("k0", mK0);
        sb.add("k1", mK1);
        sb.endStruct();
        return sb.toString();
    }

    public static class ZAddressBookCondition extends ZFilterCondition {
        public ZAddressBookCondition(AddressBookOp op, String header) {
            super(C_ADDRESSBOOK, op.toProtoOp(), "contacts", header);
        }
    }
    public static class ZBodyCondition extends ZFilterCondition {
        public ZBodyCondition(BodyOp op, String text) {
            super(C_BODY, op.toProtoOp(), null, text);
        }
    }

    public static class ZSizeCondition extends ZFilterCondition {
        public ZSizeCondition(String size, SizeOp op) {
            super(C_SIZE, op.toProtoOp(), null, size);
        }
    }
    
    public static class ZDateCondition extends ZFilterCondition {
        public ZDateCondition(Date date, DateOp op) {
            super(C_DATE, op.toProtoOp(), null, new SimpleDateFormat("yyyyMMdd").format(date));
        }
    }

    public static class ZHeaderCondition extends ZFilterCondition {
        public ZHeaderCondition(String headerName, HeaderOp op, String value) {
            super(C_HEADER, op.toProtoOp(), headerName, value);
        }
    }

    public static class ZHeaderExistsCondition extends ZFilterCondition {
        public ZHeaderExistsCondition(String headerName, boolean exists) {
            super(exists ? C_EXISTS : C_NOT_EXISTS, null, null, headerName);
        }
    }

    public static class ZAttachmentExistsCondition extends ZFilterCondition {
        public ZAttachmentExistsCondition(boolean exists) {
            super(exists ? C_ATTACHMENT : C_NOT_ATTACHMENT, null, null, null);
        }
    }
}
