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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public abstract class ZFilterCondition {

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

        public static HeaderOp fromString(String s) throws ServiceException {
            try {
                return HeaderOp.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: "+s+", value values: "+Arrays.asList(HeaderOp.values()), null);
            }
        }

        public String toProtoOp() { return mOps[ordinal()]; }

        public static HeaderOp fromProtoString(String s) throws ServiceException {
            s = s.trim().toLowerCase();
            for (int i=0; i < mOps.length; i++)
                if (mOps[i].equals(s)) return values()[i];
            throw ZClientException.CLIENT_ERROR("invalid op: "+s+", valid values: "+ Arrays.asList(mOps), null);
        }
    }

    public enum DateOp {

        BEFORE, NOT_BEFORE, AFTER, NOT_AFTER;

        private static String[] mOps = { ":before", "not :before", ":after", "not :after" };

        public static DateOp fromString(String s) throws ServiceException {
            try {
                return DateOp.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: "+s+", value values: "+Arrays.asList(DateOp.values()), null);
            }
        }

        public String toProtoOp() { return mOps[ordinal()]; }

        public static DateOp fromProtoString(String s) throws ServiceException {
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
            try {
                return SizeOp.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: "+s+", value values: "+Arrays.asList(SizeOp.values()), null);
            }
        }

        public static SizeOp fromProtoString(String s) throws ServiceException {
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
            try {
                return BodyOp.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: "+s+", value values: "+Arrays.asList(BodyOp.values()), null);
            }
        }

        public static BodyOp fromProtoString(String s) throws ServiceException {
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

        public static AddressBookOp fromProtoString(String s) throws ServiceException {
            s = s.trim().toLowerCase();
            for (int i=0; i < mOps.length; i++)
                if (mOps[i].equals(s)) return values()[i];
            throw ZClientException.CLIENT_ERROR("invalid op: "+s+", valid values: "+ Arrays.asList(mOps), null);
        }

        public static AddressBookOp fromString(String s) throws ServiceException {
            try {
                return AddressBookOp.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: "+s+", value values: "+Arrays.asList(AddressBookOp.values()), null);
            }
        }

    }

    protected String mName;
    protected String mOp;
    protected String mK0;
    protected String mK1;

    private static String getAttrK(Element e, String k, boolean defaultValue) throws ServiceException {
        String value = defaultValue  ? e.getAttribute(k, "[\"\"]") : e.getAttribute(k);
        List<String> slist = StringUtil.parseSieveStringList(value);
        if (slist.size() != 1) {
            throw ZClientException.CLIENT_ERROR(String.format("unable to parse attr(%s) value(%s)", k, value), null);
        }
        return slist.get(0);
    }

    private static String getK0(Element e) throws ServiceException {
        return getAttrK(e, MailConstants.A_LHS, false);
    }

    private static String getK1(Element e) throws ServiceException {
        return getAttrK(e, MailConstants.A_RHS, false);
    }

    private static String getK0(Element e, boolean defaultValue) throws ServiceException {
        return getAttrK(e, MailConstants.A_LHS, defaultValue);
    }

    private static String getK1(Element e, boolean defaultValue) throws ServiceException {
        return getAttrK(e, MailConstants.A_RHS, defaultValue);
    }

    public static ZFilterCondition getCondition(Element condEl) throws ServiceException {
        String n = condEl.getAttribute(MailConstants.A_NAME).toLowerCase();
        if (n.equals(C_HEADER)) {
            return new ZHeaderCondition(
                    getK0(condEl, true),
                    HeaderOp.fromProtoString(condEl.getAttribute(MailConstants.A_OPERATION, ":is")),
                    getK1(condEl, true));
        } else if (n.equals(C_EXISTS)) {
            return new ZHeaderExistsCondition(
                    getK0(condEl, true),
                    true);
        } else if (n.equals(C_NOT_EXISTS)) {
            return new ZHeaderExistsCondition(
                    getK0(condEl, true),
                    true);
        } if (n.equals(C_DATE)) {
            try {
                return new ZDateCondition(
                        DateOp.fromProtoString(condEl.getAttribute(MailConstants.A_OPERATION)),
                        new SimpleDateFormat("yyyyMMdd").parse(getK1(condEl)));
            } catch (ParseException e) {
                throw ZClientException.CLIENT_ERROR("unable to parse filter date: "+e, null);
            }
        } else if (n.equals(C_SIZE)) {
            return new ZSizeCondition(
                    SizeOp.fromProtoString(condEl.getAttribute(MailConstants.A_OPERATION)),
                    condEl.getAttribute(MailConstants.A_RHS, ""));
        } else if (n.equals(C_BODY)) {
            return new ZBodyCondition(BodyOp.fromProtoString(condEl.getAttribute(MailConstants.A_OPERATION)),getK1(condEl, true));
        } else if (n.equals(C_ATTACHMENT)) {
            return new ZAttachmentExistsCondition(true);
        } else if (n.equals(C_NOT_ATTACHMENT)) {
            return new ZAttachmentExistsCondition(false);
        } else if (n.equals(C_ADDRESSBOOK)) {
            return new ZAddressBookCondition(
                    AddressBookOp.fromProtoString(condEl.getAttribute(MailConstants.A_OPERATION)),
                    getK0(condEl, true));
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

    public final String getName() { return mName; }
    public final String getK0() { return mK0; }
    public final String getK1() { return mK1; }
    public final String getOp() { return mOp; }

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

    public abstract String toConditionString();


    public static class ZAddressBookCondition extends ZFilterCondition {
        private AddressBookOp mAddressBookOp;

        public ZAddressBookCondition(AddressBookOp op, String header) {
            super(C_ADDRESSBOOK, op.toProtoOp(), header, "contacts");
            mAddressBookOp = op;
        }

        public AddressBookOp getAddressBookOp() { return mAddressBookOp; }
        public String getFolder() { return mK1; }
        public String getHeader() { return mK0; }

        public String toConditionString() {
            return (mAddressBookOp == AddressBookOp.IN ? "addressbook in " : "addressbook not_in ") + ZFilterRule.quotedString(getHeader());
        }
    }
    
    public static class ZBodyCondition extends ZFilterCondition {
        private BodyOp mBodyOp;

        public ZBodyCondition(BodyOp op, String text) {
            super(C_BODY, op.toProtoOp(), null, text);
            mBodyOp = op;
        }

        public BodyOp getBodyOp() { return mBodyOp; }
        public String getText() { return mK1; }

        public String toConditionString() {
            return (mBodyOp == BodyOp.CONTAINS ? "body contains " : "body not_contains ") +ZFilterRule.quotedString(getText());
        }
    }

    public static class ZSizeCondition extends ZFilterCondition {
        private SizeOp mSizeOp;

        public ZSizeCondition(SizeOp op, String size) {
            super(C_SIZE, op.toProtoOp(), null, size);
            mSizeOp = op;
        }

        public SizeOp getSizeOp() { return mSizeOp; }
        public String getSize() { return mK1; }

        public String getUnits() {
            String val = getSize();
            if (val != null) {
                if (val.endsWith("M")) return "M";
                else if (val.endsWith("K")) return "K";
                else if (val.endsWith("G")) return "G";
            }
            return "B";
        }

        public String getSizeNoUnits() {
            String val = getSize();
            if (val != null) {
                if (val.endsWith("M")) return val.substring(0, val.length()-1);
                else if (val.endsWith("K")) return val.substring(0, val.length()-1);
                else if (val.endsWith("G")) return val.substring(0, val.length()-1);
            }
            return val;
        }

        public String toConditionString() {
            return "size " + mSizeOp.name().toLowerCase() + " " + ZFilterRule.quotedString(getSize());
        }
    }
    
    public static class ZDateCondition extends ZFilterCondition {
        private DateOp mDateOp;
        private Date mDate;

        public ZDateCondition(DateOp op, Date date) {
            super(C_DATE, op.toProtoOp(), null, new SimpleDateFormat("yyyyMMdd").format(date));
            mDateOp = op;
            mDate = date;
        }

        public ZDateCondition(DateOp op, String dateStr) throws ServiceException {
            super(C_DATE, op.toProtoOp(), null, dateStr);
            mDateOp = op;
            try {
                mDate = new SimpleDateFormat("yyyyMMdd").parse(dateStr);
            } catch (ParseException e) {
                throw ZClientException.CLIENT_ERROR("invalid date: "+dateStr+", should be: YYYYMMDD", e);
            }
        }

        public DateOp getDateOp() { return mDateOp; }
        public Date getDate() { return mDate; }
        public String getDateString() { return mK1; }

        public String toConditionString() {
            return "date " + mDateOp.name().toLowerCase() + " "+ ZFilterRule.quotedString(getDateString());
        }
    }

    public static class ZHeaderCondition extends ZFilterCondition {
        private HeaderOp mHeaderOp;

        public ZHeaderCondition(String headerName, HeaderOp op, String value) {
            super(C_HEADER, op.toProtoOp(), headerName, value);
            mHeaderOp = op;
        }

        public HeaderOp getHeaderOp() { return mHeaderOp; }
        public String getHeaderName() { return mK0; }
        public String getHeaderValue()  { return mK1; }

        public String toConditionString() {
            return "header " + ZFilterRule.quotedString(getHeaderName()) + " " + mHeaderOp.name().toLowerCase() + " " + ZFilterRule.quotedString(getHeaderValue());
        }
    }

    public static class ZHeaderExistsCondition extends ZFilterCondition {
        private boolean mExists;

        public ZHeaderExistsCondition(String headerName, boolean exists) {
            super(exists ? C_EXISTS : C_NOT_EXISTS, null, null, headerName);
            mExists = exists;
        }

        public boolean getExists() { return mExists; }
        public String getHeaderName() { return mK1; }

        public String toConditionString() {
            return "header " + ZFilterRule.quotedString(getHeaderName()) + (mExists ? " exists" : " not_exists");
        }
    }

    public static class ZAttachmentExistsCondition extends ZFilterCondition {
        private boolean mExists;

        public ZAttachmentExistsCondition(boolean exists) {
            super(exists ? C_ATTACHMENT : C_NOT_ATTACHMENT, null, null, null);
            mExists = exists;
        }

        public boolean getExists() { return mExists; }

        public String toConditionString() {
            return mExists ? "attachment exists" : "attachment not_exists";
        }
    }
}
