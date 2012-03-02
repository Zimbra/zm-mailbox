/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.zimbra.common.util.Constants;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.imap.ImapMessage.ImapMessageSet;

abstract class ImapSearch {
    abstract boolean canBeRunLocally();
    abstract String toZimbraSearch(ImapFolder i4folder) throws ImapParseException;
    abstract ImapMessageSet evaluate(ImapFolder i4folder) throws ImapParseException;
    boolean requiresMODSEQ()  { return false; }

    static boolean isAllMessages(ImapFolder i4folder, Set<ImapMessage> i4set) {
        int size = i4set.size() - (i4set.contains(null) ? 1 : 0);
        return size == i4folder.getSize();
    }

    static String sequenceAsSearchTerm(ImapFolder i4folder, TreeSet<ImapMessage> i4set, boolean abbreviateAll) {
        i4set.remove(null);
        if (i4set.isEmpty()) {
            return "item:none";
        } else if (abbreviateAll && isAllMessages(i4folder, i4set)) {
            return "item:all";
        }

        StringBuilder sb = new StringBuilder("item:{");
        for (ImapMessage i4msg : i4set) {
            sb.append(sb.length() == 6 ? "" : ",").append(i4msg.msgId);
        }
        return sb.append('}').toString();
    }

    static String stringAsSearchTerm(String content) {
        return stringAsSearchTerm(content, true);
    }

    static String stringAsSearchTerm(String content, boolean wildcard) {
        String sanitized = content.replace('*', ' ').replace('"', ' ');
        if (wildcard && (sanitized.length() == 0 || !Character.isWhitespace(sanitized.charAt(sanitized.length() - 1)))) {
            sanitized += '*';
        }
        return '"' + sanitized + '"';
    }


    static abstract class LogicalOperation extends ImapSearch {
        List<ImapSearch> mChildren = new ArrayList<ImapSearch>();

        LogicalOperation addChild(ImapSearch i4search) {
            mChildren.add(i4search);  return this;
        }

        @Override boolean canBeRunLocally() {
            for (ImapSearch i4search : mChildren) {
                if (!i4search.canBeRunLocally()) {
                    return false;
                }
            }
            return true;
        }

        @Override boolean requiresMODSEQ() {
            for (ImapSearch i4search : mChildren) {
                if (i4search.requiresMODSEQ()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((mChildren == null) ? 0 : mChildren.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            LogicalOperation other = (LogicalOperation) obj;
            if (mChildren == null) {
                if (other.mChildren != null) {
                    return false;
                }
            } else if (!mChildren.equals(other.mChildren)) {
                return false;
            }
            return true;
        }
    }

    static class AndOperation extends LogicalOperation {
        AndOperation(ImapSearch... children)  { super();  for (ImapSearch i4search : children) addChild(i4search); }

        @Override String toZimbraSearch(ImapFolder i4folder) throws ImapParseException {
            StringBuilder search = new StringBuilder("(");
            for (ImapSearch i4search : mChildren) {
                search.append(search.length() == 1 ? "" : " ").append(i4search.toZimbraSearch(i4folder));
            }
            return search.append(')').toString();
        }

        @Override ImapMessageSet evaluate(ImapFolder i4folder) throws ImapParseException {
            ImapMessageSet matched = null;
            for (ImapSearch i4search : mChildren) {
                if (matched == null) {
                    matched = i4search.evaluate(i4folder);
                } else {
                    matched.retainAll(i4search.evaluate(i4folder));
                }

                if (matched.isEmpty())
                    break;
            }
            return matched;
        }
    }

    static class OrOperation extends LogicalOperation {
        OrOperation(ImapSearch... children)  { super();  for (ImapSearch i4search : children) addChild(i4search); }

        @Override String toZimbraSearch(ImapFolder i4folder) throws ImapParseException {
            StringBuilder search = new StringBuilder("(");
            for (ImapSearch i4search : mChildren) {
                search.append(search.length() == 1 ? "(" : " or (").append(i4search.toZimbraSearch(i4folder)).append(')');
            }
            return search.append(')').toString();
        }

        @Override ImapMessageSet evaluate(ImapFolder i4folder) throws ImapParseException {
            ImapMessageSet matched = null;
            for (ImapSearch i4search : mChildren) {
                if (matched == null) {
                    matched = i4search.evaluate(i4folder);
                } else {
                    matched.addAll(i4search.evaluate(i4folder));
                }

                if (isAllMessages(i4folder, matched))
                    break;
            }
            return matched;
        }
    }

    static class NotOperation extends LogicalOperation {
        NotOperation()                  { super(); }
        NotOperation(ImapSearch child)  { super();  addChild(child); }

        @Override String toZimbraSearch(ImapFolder i4folder) throws ImapParseException {
            return '-' + mChildren.get(0).toZimbraSearch(i4folder);
        }

        @Override ImapMessageSet evaluate(ImapFolder i4folder) throws ImapParseException {
            ImapMessageSet matches = i4folder.getAllMessages();
            matches.removeAll(mChildren.get(0).evaluate(i4folder));
            return matches;
        }
    }

    static class AllSearch extends ImapSearch {
        @Override boolean canBeRunLocally()                     { return true; }
        @Override String toZimbraSearch(ImapFolder i4folder)    { return "item:all"; }
        @Override ImapMessageSet evaluate(ImapFolder i4folder)  { return i4folder.getAllMessages(); }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AllSearch;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    static class NoneSearch extends ImapSearch {
        @Override boolean canBeRunLocally()                     { return true; }
        @Override String toZimbraSearch(ImapFolder i4folder)    { return "item:none"; }
        @Override ImapMessageSet evaluate(ImapFolder i4folder)  { return new ImapMessageSet(); }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof NoneSearch;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    static class SequenceSearch extends ImapSearch {
        private String mTag;
        private String mSubSequence;
        private boolean mIsUidSearch;
        SequenceSearch(String tag, String subSequence, boolean byUID)  { mTag = tag;  mSubSequence = subSequence;  mIsUidSearch = byUID; }

        @Override boolean canBeRunLocally()  { return true; }

        @Override String toZimbraSearch(ImapFolder i4folder) throws ImapParseException {
            return sequenceAsSearchTerm(i4folder, evaluate(i4folder), true);
        }

        @Override ImapMessageSet evaluate(ImapFolder i4folder) throws ImapParseException {
            return i4folder.getSubsequence(mTag, mSubSequence, mIsUidSearch, true);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (mIsUidSearch ? 1231 : 1237);
            result = prime * result + ((mSubSequence == null) ? 0 : mSubSequence.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SequenceSearch other = (SequenceSearch) obj;
            if (mIsUidSearch != other.mIsUidSearch) {
                return false;
            }
            if (mSubSequence == null) {
                if (other.mSubSequence != null) {
                    return false;
                }
            } else if (!mSubSequence.equals(other.mSubSequence)) {
                return false;
            }
            return true;
        }
    }

    static class FlagSearch extends ImapSearch {
        private String mFlagName;
        FlagSearch(String flagName)  { mFlagName = flagName; }

        @Override boolean canBeRunLocally()  { return true; }

        @Override String toZimbraSearch(ImapFolder i4folder) {
            ImapFlag i4flag = i4folder.getFlagByName(mFlagName);
            if (i4flag == null) {
                return "item:none";
            }
            String prefix = i4flag.mPositive ? "" : "(-", suffix = i4flag.mPositive ? "" : ")";
            if (i4flag.mPermanent) {
                return prefix + "tag:" + i4flag.mName + suffix;
            }
            return prefix + sequenceAsSearchTerm(i4folder, i4folder.getFlaggedMessages(i4flag), true) + suffix;
        }

        @Override ImapMessageSet evaluate(ImapFolder i4folder) {
            ImapFlag i4flag = i4folder.getFlagByName(mFlagName);
            if (i4flag == null) {
                return new ImapMessageSet();
            } else if (i4flag.mPositive) {
                return i4folder.getFlaggedMessages(i4flag);
            } else {
                ImapMessageSet matched = i4folder.getAllMessages();
                matched.removeAll(i4folder.getFlaggedMessages(i4flag));
                return matched;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((mFlagName == null) ? 0 : mFlagName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            FlagSearch other = (FlagSearch) obj;
            if (mFlagName == null) {
                if (other.mFlagName != null) {
                    return false;
                }
            } else if (!mFlagName.equals(other.mFlagName)) {
                return false;
            }
            return true;
        }
    }

    static class DateSearch extends ImapSearch {
        enum Relation {
            before("before:"), after("date:>="), date("date:");

            String query;
            Relation(String rep)                { query = rep; }
            @Override public String toString()  { return query; }
        };
        private Relation mRelation;
        private Date mDate;
        private long mTimestamp;
        DateSearch(Relation relation, Date date)  { mDate = date;  mTimestamp = date.getTime();  mRelation = relation; }

        @Override boolean canBeRunLocally() {
            return mTimestamp < 0 || mTimestamp > System.currentTimeMillis() + 36 * Constants.MILLIS_PER_MONTH;
        }

        @Override String toZimbraSearch(ImapFolder i4folder)  {
            if (mTimestamp < 0) {
                return (mRelation == Relation.after ? "item:all" : "item:none");
            } else if (mTimestamp > System.currentTimeMillis() + 36 * Constants.MILLIS_PER_MONTH) {
                return (mRelation == Relation.before ? "item:all" : "item:none");
            } else {
                return mRelation + DateFormat.getDateInstance(DateFormat.SHORT).format(mDate);
            }
        }

        @Override ImapMessageSet evaluate(ImapFolder i4folder) {
            if (mTimestamp < 0) {
                return (mRelation == Relation.after ? i4folder.getAllMessages() : new ImapMessageSet());
            } else if (mTimestamp > System.currentTimeMillis() + 36 * Constants.MILLIS_PER_MONTH) {
                return (mRelation == Relation.before ? i4folder.getAllMessages() : new ImapMessageSet());
            } else {
                throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((mDate == null) ? 0 : mDate.hashCode());
            result = prime * result + ((mRelation == null) ? 0 : mRelation.hashCode());
            result = prime * result + (int) (mTimestamp ^ (mTimestamp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            DateSearch other = (DateSearch) obj;
            if (mDate == null) {
                if (other.mDate != null) {
                    return false;
                }
            } else if (!mDate.equals(other.mDate)) {
                return false;
            }
            if (mRelation == null) {
                if (other.mRelation != null) {
                    return false;
                }
            } else if (!mRelation.equals(other.mRelation)) {
                return false;
            }
            if (mTimestamp != other.mTimestamp) {
                return false;
            }
            return true;
        }
    }

    static class RelativeDateSearch extends ImapSearch {
        private DateSearch.Relation mRelation;
        private int mOffset;
        RelativeDateSearch(DateSearch.Relation relation, int offset)  { mOffset = offset;  mRelation = relation; }

        @Override boolean canBeRunLocally()                   { return false; }
        @Override String toZimbraSearch(ImapFolder i4folder)  { return mRelation.toString() + (System.currentTimeMillis() - mOffset * Constants.MILLIS_PER_SECOND); }
        @Override ImapMessageSet evaluate(ImapFolder i4folder) {
            throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + mOffset;
            result = prime * result + ((mRelation == null) ? 0 : mRelation.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            RelativeDateSearch other = (RelativeDateSearch) obj;
            if (mOffset != other.mOffset) {
                return false;
            }
            if (mRelation == null) {
                if (other.mRelation != null) {
                    return false;
                }
            } else if (!mRelation.equals(other.mRelation)) {
                return false;
            }
            return true;
        }
    }

    static class ModifiedSearch extends ImapSearch {
        private int mChangedSince;
        ModifiedSearch(int changeId)  { mChangedSince = changeId; }

        @Override boolean requiresMODSEQ()                    { return true; }
        @Override boolean canBeRunLocally()                   { return false; }
        @Override String toZimbraSearch(ImapFolder i4folder)  { return "(modseq:>" + mChangedSince + ")"; }
        @Override ImapMessageSet evaluate(ImapFolder i4folder) {
            throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + mChangedSince;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ModifiedSearch other = (ModifiedSearch) obj;
            if (mChangedSince != other.mChangedSince) {
                return false;
            }
            return true;
        }
    }

    static class SizeSearch extends ImapSearch {
        enum Relation { larger, smaller };
        private Relation mRelation;
        private long mSize;
        SizeSearch(Relation relation, long size)  { mSize = size;  mRelation = relation; }

        @Override boolean canBeRunLocally()                   { return false; }
        @Override String toZimbraSearch(ImapFolder i4folder)  { return mRelation + ":" + mSize; }
        @Override ImapMessageSet evaluate(ImapFolder i4folder) {
            throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((mRelation == null) ? 0 : mRelation.hashCode());
            result = prime * result + (int) (mSize ^ (mSize >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SizeSearch other = (SizeSearch) obj;
            if (mRelation == null) {
                if (other.mRelation != null) {
                    return false;
                }
            } else if (!mRelation.equals(other.mRelation)) {
                return false;
            }
            if (mSize != other.mSize) {
                return false;
            }
            return true;
        }
    }

    static class ContentSearch extends ImapSearch {
        private String mValue;
        ContentSearch(String value)  { mValue = value; }

        @Override boolean canBeRunLocally()                   { return mValue.trim().equals(""); }
        @Override String toZimbraSearch(ImapFolder i4folder)  { return stringAsSearchTerm(mValue); }
        @Override ImapMessageSet evaluate(ImapFolder i4folder) {
            if (canBeRunLocally())
                return i4folder.getAllMessages();
            throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((mValue == null) ? 0 : mValue.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ContentSearch other = (ContentSearch) obj;
            if (mValue == null) {
                if (other.mValue != null) {
                    return false;
                }
            } else if (!mValue.equals(other.mValue)) {
                return false;
            }
            return true;
        }
    }

    static class HeaderSearch extends ImapSearch {
        static class Header {
            static final Header SUBJECT = new Header("subject", "subject");
            static final Header FROM = new Header("from", "from");
            static final Header TO = new Header("to", "to");
            static final Header CC = new Header("cc", "cc");
            static final Header BCC = new Header("bcc", "#bcc");
            static final Header MSGID = new Header("message-id", "msgid");

            private static final Header[] SPECIAL_HEADERS = new Header[] { SUBJECT, FROM, TO, CC, BCC, MSGID };

            private final String mField, mKey;
            private Header(String fieldName, String searchKey) {
                mField = fieldName;  mKey = searchKey;
            }

            static Header parse(String field) {
                field = field.toLowerCase();
                for (Header syshdr : SPECIAL_HEADERS) {
                    if (field.equals(syshdr.mField))
                        return syshdr;
                }
                String key = field.replaceAll("[ \t\":()]", "");
                while (key.startsWith("-"))
                    key = key.substring(1);
                if (key.equals(""))
                    key = "*";
                return new Header(field, '#' + key);
            }

            @Override public String toString()  { return mKey; }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((mField == null) ? 0 : mField.hashCode());
                result = prime * result + ((mKey == null) ? 0 : mKey.hashCode());
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                Header other = (Header) obj;
                if (mField == null) {
                    if (other.mField != null) {
                        return false;
                    }
                } else if (!mField.equals(other.mField)) {
                    return false;
                }
                if (mKey == null) {
                    if (other.mKey != null) {
                        return false;
                    }
                } else if (!mKey.equals(other.mKey)) {
                    return false;
                }
                return true;
            }
        };

        private Header mHeader;
        private String mValue;
        private boolean mPrefixSearch = true;
        HeaderSearch(Header header, String value) {
            while (value.startsWith("<") || value.startsWith(">") || value.startsWith("=")) {
                value = value.substring(1);
            }
            if (header == Header.MSGID && value.endsWith(">")) {
                value = value.substring(0, value.length() - 1);
                mPrefixSearch = false;
            }
            mValue = value;  mHeader = header;
        }

        @Override boolean canBeRunLocally()  { return false; }
        @Override String toZimbraSearch(ImapFolder i4folder) {
            String value = stringAsSearchTerm(mValue, mPrefixSearch);
            if ((mHeader == Header.FROM || mHeader == Header.TO || mHeader == Header.CC) && mValue.indexOf('@') == -1) {
                value += " or " + stringAsSearchTerm('@' + mValue);
            }
            return mHeader + ":(" + value + ")";
        }
        @Override ImapMessageSet evaluate(ImapFolder i4folder) {
            throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((mHeader == null) ? 0 : mHeader.hashCode());
            result = prime * result + (mPrefixSearch ? 1231 : 1237);
            result = prime * result + ((mValue == null) ? 0 : mValue.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            HeaderSearch other = (HeaderSearch) obj;
            if (mHeader == null) {
                if (other.mHeader != null) {
                    return false;
                }
            } else if (!mHeader.equals(other.mHeader)) {
                return false;
            }
            if (mPrefixSearch != other.mPrefixSearch) {
                return false;
            }
            if (mValue == null) {
                if (other.mValue != null) {
                    return false;
                }
            } else if (!mValue.equals(other.mValue)) {
                return false;
            }
            return true;
        }
    }
}
