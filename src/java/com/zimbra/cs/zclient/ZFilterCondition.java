/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zclient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.JSONException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.filter.FilterUtil;
import com.zimbra.cs.filter.FilterUtil.DateComparison;
import com.zimbra.cs.filter.FilterUtil.NumberComparison;
import com.zimbra.cs.filter.FilterUtil.StringComparison;

public abstract class ZFilterCondition implements ToZJSONObject {

    public static final String C_ADDRESSBOOK = "addressbook";
    public static final String C_ATTACHMENT = "attachment";
    public static final String C_BODY = "body";
    public static final String C_DATE = "date";
    public static final String C_EXISTS = "exists";
    public static final String C_NOT_EXISTS = "not exists";
    public static final String C_HEADER = "header";
    public static final String C_ATTACHMENT_HEADER = "attachment_header";
    public static final String C_NOT_ATTACHMENT = "not attachment";
    public static final String C_SIZE = "size";
    public static final String C_INVITE = "invite";

    public enum HeaderOp {

        IS, NOT_IS, CONTAINS, NOT_CONTAINS, MATCHES, NOT_MATCHES;

        public static HeaderOp fromString(String s) throws ServiceException {
            try {
                return HeaderOp.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: "+s+", value values: "+Arrays.asList(HeaderOp.values()), null);
            }
        }
        
        public static HeaderOp fromStringComparison(StringComparison comparison, boolean isNegative) {
            if (comparison == StringComparison.is) {
                return (isNegative ? NOT_IS : IS);
            }
            if (comparison == StringComparison.contains) {
                return (isNegative ? NOT_CONTAINS : CONTAINS);
            }
            return (isNegative ? NOT_MATCHES : MATCHES);
        }
        
        public StringComparison toStringComparison() {
            if (this == IS || this == NOT_IS) {
                return StringComparison.is;
            }
            if (this == CONTAINS || this == NOT_CONTAINS) {
                return StringComparison.contains;
            }
            return StringComparison.matches;
        }
        
        public boolean isNegative() {
            return (this == NOT_IS || this == NOT_CONTAINS || this == NOT_MATCHES);
        }
    }

    public enum DateOp {

        BEFORE, NOT_BEFORE, AFTER, NOT_AFTER;

        public static DateOp fromString(String s) throws ServiceException {
            try {
                return DateOp.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: "+s+", value values: "+Arrays.asList(DateOp.values()), null);
            }
        }
        
        public static DateOp fromDateComparison(DateComparison comparison, boolean isNegative) {
            if (comparison == DateComparison.before) {
                return (isNegative ? NOT_BEFORE : BEFORE);
            }
            return (isNegative ? NOT_AFTER : AFTER);
        }
        
        public DateComparison toDateComparison() {
            if (this == BEFORE || this == NOT_BEFORE) {
                return DateComparison.before;
            }
            return DateComparison.after;
        }
        
        public boolean isNegative() {
            return (this == NOT_BEFORE || this == NOT_AFTER);
        }
    }

    public enum SizeOp {
        UNDER, NOT_UNDER, OVER, NOT_OVER;

        public static SizeOp fromString(String s) throws ServiceException {
            try {
                return SizeOp.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: "+s+", value values: "+Arrays.asList(SizeOp.values()), null);
            }
        }
        
        public static SizeOp fromNumberComparison(NumberComparison comparison, boolean isNegative) {
            if (comparison == NumberComparison.over) {
                return (isNegative ? NOT_OVER : OVER);
            }
            return (isNegative ? NOT_UNDER : UNDER);
        }
        
        public NumberComparison toNumberComparison() {
            if (this == UNDER || this == NOT_UNDER) {
                return NumberComparison.under;
            } else {
                return NumberComparison.over;
            }
        }
        
        public boolean isNegative() {
            return (this == NOT_UNDER || this == NOT_OVER);
        }
    }

    public enum BodyOp {
        CONTAINS, NOT_CONTAINS;

        public static BodyOp fromString(String s) throws ServiceException {
            try {
                return BodyOp.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: "+s+", value values: "+Arrays.asList(BodyOp.values()), null);
            }
        }
    }

    public enum AddressBookOp {
        IN, NOT_IN;

        public static AddressBookOp fromString(String s) throws ServiceException {
            try {
                return AddressBookOp.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid op: "+s+", value values: "+Arrays.asList(AddressBookOp.values()), null);
            }
        }
    }

    public static ZFilterCondition getCondition(Element condEl) throws ServiceException {
        String name = condEl.getName();
        boolean isNegative = condEl.getAttributeBool(MailConstants.A_NEGATIVE, false);

        if (name.equals(MailConstants.E_HEADER_TEST)) {
            String header = condEl.getAttribute(MailConstants.A_HEADER);
            String s = condEl.getAttribute(MailConstants.A_STRING_COMPARISON);
            s = s.toLowerCase();
            StringComparison comparison = StringComparison.fromString(s);
            String value = condEl.getAttribute(MailConstants.A_VALUE);
            return new ZHeaderCondition(header, HeaderOp.fromStringComparison(comparison, isNegative), value);
        } else if (name.equals(MailConstants.E_ATTACHMENT_HEADER_TEST)) {
            String header = condEl.getAttribute(MailConstants.A_HEADER);
            String s = condEl.getAttribute(MailConstants.A_STRING_COMPARISON);
            s = s.toLowerCase();
            StringComparison comparison = StringComparison.fromString(s);
            String value = condEl.getAttribute(MailConstants.A_VALUE);
            return new ZAttachmentHeaderCondition(header, HeaderOp.fromStringComparison(comparison, isNegative), value);
        } else if (name.equals(MailConstants.E_HEADER_EXISTS_TEST)) {
            String header = condEl.getAttribute(MailConstants.A_HEADER);
            return new ZHeaderExistsCondition(header, !isNegative);
        } else if (name.equals(MailConstants.E_SIZE_TEST)) {
            String s = condEl.getAttribute(MailConstants.A_NUMBER_COMPARISON);
            s = s.toLowerCase();
            NumberComparison comparison = NumberComparison.fromString(s);
            String size = condEl.getAttribute(MailConstants.A_SIZE);
            return new ZSizeCondition(SizeOp.fromNumberComparison(comparison, isNegative), size);
        } else if (name.equals(MailConstants.E_DATE_TEST)) {
            String s = condEl.getAttribute(MailConstants.A_DATE_COMPARISON);
            s = s.toLowerCase();
            DateComparison comparison = DateComparison.fromString(s);
            Date date = new Date(condEl.getAttributeLong(MailConstants.A_DATE) * 1000);
            return new ZDateCondition(DateOp.fromDateComparison(comparison, isNegative), date);
        } else if (name.equals(MailConstants.E_BODY_TEST)) {
            String value = condEl.getAttribute(MailConstants.A_VALUE);
            BodyOp op = (isNegative ? BodyOp.NOT_CONTAINS : BodyOp.CONTAINS);
            return new ZBodyCondition(op, value);
        } else if (name.equals(MailConstants.E_ADDRESS_BOOK_TEST)) {
            String header = condEl.getAttribute(MailConstants.A_HEADER);
            // String folderPath = condEl.getAttribute(MailConstants.A_FOLDER_PATH);
            // TODO: support path to contacts folder
            AddressBookOp op = (isNegative ? AddressBookOp.NOT_IN : AddressBookOp.IN);
            return new ZAddressBookCondition(op, header);
        } else if (name.equals(MailConstants.E_ATTACHMENT_TEST)) {
            return new ZAttachmentExistsCondition(!isNegative);
        } else if (name.equals(MailConstants.E_INVITE_TEST)) {
            List<Element> eMethods = condEl.listElements(MailConstants.E_METHOD);
            if (eMethods.isEmpty()) {
                return new ZInviteCondition(!isNegative);
            } else {
                List<String> methods = new ArrayList<String>();
                for (Element eMethod : eMethods) {
                    methods.add(eMethod.getText());
                }
                return new ZInviteCondition(!isNegative, methods);
            }
        } else {
             throw ZClientException.CLIENT_ERROR("unknown filter condition: "+name, null);
        }
    }

    /**
     * Adds a new test element to the given parent.
     * @return the new element
     */
    abstract Element toElement(Element parent);

    public abstract String getName();

    public ZJSONObject toZJSONObject() throws JSONException {
        // TODO: Implement in subclasses?
        ZJSONObject jo = new ZJSONObject();
        jo.put("name", getName());
        return jo;
    }

    public String toString() {
        return String.format("[ZFilterCondition %s]", getName());
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public abstract String toConditionString();


    public static class ZAddressBookCondition extends ZFilterCondition {
        private AddressBookOp mAddressBookOp;
        private String mHeader;

        public ZAddressBookCondition(AddressBookOp op, String header) {
            mAddressBookOp = op;
            mHeader = header;
        }

        @Override
        public String getName() {
            return C_ADDRESSBOOK;
        }
        
        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_ADDRESS_BOOK_TEST);
            test.addAttribute(MailConstants.A_HEADER, mHeader);
            test.addAttribute(MailConstants.A_FOLDER_PATH, "contacts");
            if (mAddressBookOp == AddressBookOp.NOT_IN) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            return test;
        }

        public String getHeader() { return mHeader; }
        public AddressBookOp getAddressBookOp() { return mAddressBookOp; }

        public String toConditionString() {
            return (mAddressBookOp == AddressBookOp.IN ? "addressbook in " : "addressbook not_in ") + ZFilterRule.quotedString(getHeader());
        }
    }
    
    public static class ZBodyCondition extends ZFilterCondition {
        private BodyOp mBodyOp;
        private String mText;

        public ZBodyCondition(BodyOp op, String text) {
            mBodyOp = op;
            mText = text;
        }

        @Override
        public String getName() {
            return C_BODY;
        }

        public BodyOp getBodyOp() { return mBodyOp; }
        public String getText() { return mText; }

        public String toConditionString() {
            return (mBodyOp == BodyOp.CONTAINS ? "body contains " : "body not_contains ") +ZFilterRule.quotedString(getText());
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_BODY_TEST);
			if (!StringUtil.isNullOrEmpty(mText)) {
            	test.addAttribute(MailConstants.A_VALUE, mText);
			}
            if (mBodyOp == BodyOp.NOT_CONTAINS) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            return test;
        }

    }

    public static class ZSizeCondition extends ZFilterCondition {
        private SizeOp mSizeOp;
        private String mSize;

        public ZSizeCondition(SizeOp op, String size) {
            mSizeOp = op;
            mSize = size;
        }

        public SizeOp getSizeOp() { return mSizeOp; }
        public String getSize() { return mSize; }

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

        @Override
        public String getName() {
            return C_SIZE;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_SIZE_TEST);
            test.addAttribute(MailConstants.A_NUMBER_COMPARISON, mSizeOp.toNumberComparison().toString());
            if (mSizeOp.isNegative()) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            test.addAttribute(MailConstants.A_SIZE, mSize);
            return test;
        }
    }
    
    public static class ZDateCondition extends ZFilterCondition {
        private DateOp mDateOp;
        private Date mDate;

        public ZDateCondition(DateOp op, Date date) {
            mDateOp = op;
            mDate = date;
        }

        public ZDateCondition(DateOp op, String dateStr) throws ServiceException {
            mDateOp = op;
            try {
                mDate = new SimpleDateFormat("yyyyMMdd").parse(dateStr);
            } catch (ParseException e) {
                throw ZClientException.CLIENT_ERROR("invalid date: "+dateStr+", should be: YYYYMMDD", e);
            }
        }

        public DateOp getDateOp() { return mDateOp; }
        public Date getDate() { return mDate; }
        public String getDateString() { return FilterUtil.SIEVE_DATE_PARSER.format(mDate); }

        public String toConditionString() {
            return "date " + mDateOp.name().toLowerCase() + " "+ ZFilterRule.quotedString(getDateString());
        }

        @Override
        public String getName() {
            return C_DATE;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_DATE_TEST);
            test.addAttribute(MailConstants.A_DATE_COMPARISON, mDateOp.toDateComparison().toString());
            if (mDateOp.isNegative()) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            test.addAttribute(MailConstants.A_DATE, mDate.getTime() / 1000);
            return test;
        }
        
    }

    public static class ZHeaderCondition extends ZFilterCondition {
        private HeaderOp mHeaderOp;
        private String mHeaderName;
        private String mValue;

        public ZHeaderCondition(String headerName, HeaderOp op, String value) {
            mHeaderName = headerName;
            mHeaderOp = op;
            mValue = value;
        }

        public HeaderOp getHeaderOp() { return mHeaderOp; }
        public String getHeaderName() { return mHeaderName; }
        public String getHeaderValue()  { return mValue; }

        public String toConditionString() {
            return "header " + ZFilterRule.quotedString(getHeaderName()) + " " + mHeaderOp.name().toLowerCase() + " " + ZFilterRule.quotedString(getHeaderValue());
        }

        @Override
        public String getName() {
            return C_HEADER;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_HEADER_TEST);
            test.addAttribute(MailConstants.A_HEADER, mHeaderName);
            test.addAttribute(MailConstants.A_STRING_COMPARISON, mHeaderOp.toStringComparison().toString());
            if (mHeaderOp.isNegative()) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
			if (!StringUtil.isNullOrEmpty(mValue)) {
            	test.addAttribute(MailConstants.A_VALUE, mValue);
			}
            return test;
        }
        
    }

    public static class ZAttachmentHeaderCondition extends ZFilterCondition {
        private HeaderOp mHeaderOp;
        private String mHeaderName;
        private String mValue;

        public ZAttachmentHeaderCondition(String headerName, HeaderOp op, String value) {
            mHeaderName = headerName;
            mHeaderOp = op;
            mValue = value;
        }

        public HeaderOp getHeaderOp() { return mHeaderOp; }
        public String getHeaderName() { return mHeaderName; }
        public String getHeaderValue()  { return mValue; }

        public String toConditionString() {
            return "attachment_header " + ZFilterRule.quotedString(getHeaderName()) + " " + mHeaderOp.name().toLowerCase() + " " + ZFilterRule.quotedString(getHeaderValue());
        }

        @Override
        public String getName() {
            return C_ATTACHMENT_HEADER;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_ATTACHMENT_HEADER_TEST);
            test.addAttribute(MailConstants.A_HEADER, mHeaderName);
            test.addAttribute(MailConstants.A_STRING_COMPARISON, mHeaderOp.toStringComparison().toString());
            if (mHeaderOp.isNegative()) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            if (!StringUtil.isNullOrEmpty(mValue)) {
                test.addAttribute(MailConstants.A_VALUE, mValue);
            }
            return test;
        }
    }

    public static class ZHeaderExistsCondition extends ZFilterCondition {
        private boolean mExists;
        private String mHeaderName;

        public ZHeaderExistsCondition(String headerName, boolean exists) {
            mExists = exists;
            mHeaderName = headerName;
        }

        public boolean getExists() { return mExists; }
        public String getHeaderName() { return mHeaderName; }

        public String toConditionString() {
            return "header " + ZFilterRule.quotedString(getHeaderName()) + (mExists ? " exists" : " not_exists");
        }

        @Override
        public String getName() {
            return C_EXISTS;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_HEADER_EXISTS_TEST);
            test.addAttribute(MailConstants.A_HEADER, mHeaderName);
            if (!mExists) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            return test;
        }
    }

    public static class ZAttachmentExistsCondition extends ZFilterCondition {
        private boolean mExists;

        public ZAttachmentExistsCondition(boolean exists) {
            mExists = exists;
        }

        public boolean getExists() { return mExists; }

        public String toConditionString() {
            return mExists ? "attachment exists" : "attachment not_exists";
        }

        @Override
        public String getName() {
            return C_ATTACHMENT;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_ATTACHMENT_TEST);
            if (!mExists) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            return test;
        }
    }

    public static class ZInviteCondition extends ZFilterCondition {
        public static final String METHOD_ANYREQUEST = "anyrequest";
        public static final String METHOD_ANYREPLY = "anyreply";
        public static final String METHOD_PUBLISH = "publish";
        public static final String METHOD_REQUEST = "request";
        public static final String METHOD_REPLY = "reply";
        public static final String METHOD_ADD = "add";
        public static final String METHOD_CANCEL = "cancel";
        public static final String METHOD_REFRESH = "refresh";
        public static final String METHOD_DECLINECOUNTER = "declinecounter";
        
        private boolean mIsInvite;
        private List<String> mMethods = new ArrayList<String>();

        public ZInviteCondition(boolean isInvite) {
            mIsInvite = isInvite;
        }
        
        public ZInviteCondition(boolean isInvite, String method) {
            this(isInvite, Arrays.asList(method));
        }
        
        public ZInviteCondition(boolean isInvite, List<String> methods) {
            mIsInvite = isInvite;
            if (methods != null) {
                mMethods.addAll(methods);
            }
        }
        
        public void setMethods(String ... methods) {
            mMethods.clear();
            Collections.addAll(mMethods, methods);
        }
        
        public boolean isInvite() { return mIsInvite; }

        public String toConditionString() {
            StringBuilder buf = new StringBuilder("invite ");
            if (!mIsInvite) {
                buf.append("not_");
            }
            buf.append("exists");
            if (!mMethods.isEmpty()) {
                buf.append(" method ").append(StringUtil.join(",", mMethods));
            }
            return buf.toString();
        }

        @Override
        public String getName() {
            return C_INVITE;
        }

        @Override
        Element toElement(Element parent) {
            Element test = parent.addElement(MailConstants.E_INVITE_TEST);
            if (!mIsInvite) {
                test.addAttribute(MailConstants.A_NEGATIVE, true);
            }
            for (String method : mMethods) {
                test.addElement(MailConstants.E_METHOD).setText(method);
            }
            return test;
        }
    }
}
