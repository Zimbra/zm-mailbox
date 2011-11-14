/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.mailbox.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;

public class TagUtil {
    private static final String[] NO_TAGS = new String[0];

    @Deprecated
    public static String[] tagIdStringToNames(Mailbox mbox, OperationContext octxt, String tagIdString) throws ServiceException {
        if (Strings.isNullOrEmpty(tagIdString)) {
            return NO_TAGS;
        }

        List<String> tags = Lists.newArrayList();
        for (String tagId : Splitter.on(',').omitEmptyStrings().trimResults().split(tagIdString)) {
            try {
                tags.add(mbox.getTagById(octxt, Integer.parseInt(tagId)).getName());
            } catch (NumberFormatException nfe) {
                throw ServiceException.INVALID_REQUEST("invalid tag ID string: " + tagIdString, nfe);
            }
        }
        return tags.toArray(new String[tags.size()]);
    }

    @Deprecated
    public static String getTagIdString(MailItem item) {
        return getTagIdString(item.getMailbox(), item.getTags());
    }

    @Deprecated
    public static String getTagIdString(Mailbox mbox, String[] tags) {
        return Joiner.on(',').join(getTagIds(mbox, tags));
    }

    @Deprecated
    public static List<Integer> getTagIds(MailItem item) {
        return getTagIds(item.getMailbox(), item.getTags());
    }

    @Deprecated
    public static List<Integer> getTagIds(Mailbox mbox, String[] tags) {
        if (tags == null || tags.length == 0) {
            return Collections.emptyList();
        }

        List<Integer> tagIds = new ArrayList<Integer>(tags.length);
        for (String tag : tags) {
            try {
                tagIds.add(mbox.getTagByName(tag).getId());
            } catch (ServiceException e) { }
        }
        if (tagIds.size() > 1) {
            Collections.sort(tagIds);
        }
        return tagIds;
    }

    @Deprecated
    public static String[] tagBitmaskToNames(Mailbox mbox, OperationContext octxt, long tagBitmask) throws ServiceException {
        if (tagBitmask == 0) {
            return NO_TAGS;
        }

        List<String> tags = Lists.newArrayList();
        for (int i = 0; i <= 31; i++) {
            long mask = 1L << i;
            if ((tagBitmask & mask) != 0) {
                tags.add(mbox.getTagById(octxt, 64 + i).getName());
            }
        }
        return tags.toArray(new String[tags.size()]);
    }

    @Deprecated
    public static String tagIdToName(Mailbox mbox, OperationContext octxt, int tagId) throws ServiceException {
        return mbox.getTagById(octxt, tagId).getName();
    }

    public static String encodeTags(String[] tags) {
        return tags == null || tags.length == 0 ? "" : TagUtil.encodeTags(Arrays.asList(tags));
    }

    public static String encodeTags(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
    
        StringBuilder encoded = new StringBuilder();
        for (String tag : tags) {
            encoded.append(encoded.length() == 0 ? "" : ",").append(tag.replace("\\", "\\\\").replace(",", "\\,"));
        }
        return encoded.toString();
    }

    private static final Splitter TAG_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

    public static String[] decodeTags(String encoded) {
        if (Strings.isNullOrEmpty(encoded)) {
            return NO_TAGS;
        } else if (encoded.indexOf('\\') == -1) {
            List<String> tags = Lists.newArrayList(TAG_SPLITTER.split(encoded));
            return tags.toArray(new String[tags.size()]);
        } else {
            List<String> tags = Lists.newArrayList();
            StringBuilder sb = new StringBuilder(encoded.length());
            boolean escaped = false;
            for (int i = 0, len = encoded.length(); i < len; i++) {
                char c = encoded.charAt(i);
                if (escaped) {
                    sb.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == ',') {
                    String tag = sb.toString().trim();
                    if (!tag.isEmpty()) {
                        tags.add(tag);
                    }
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
            String tag = sb.toString().trim();
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
            return tags.toArray(new String[tags.size()]);
        }
    }

    public static boolean tagsMatch(String[] tags1, String[] tags2) {
        if (tags1 == tags2) {
            return true;
        } else if (ArrayUtil.isEmpty(tags1)) {
            return ArrayUtil.isEmpty(tags2);
        } else if (ArrayUtil.isEmpty(tags2)) {
            return false;
        } else {
            Set<String> s1 = Sets.newHashSet(tags1), s2 = Sets.newHashSet(tags2);
            return s1.size() == s2.size() && !s1.retainAll(s2);
        }
    }

    public static String[] parseTags(Element elt, Mailbox mbox, OperationContext octxt) throws ServiceException {
        String tags = elt.getAttribute(MailConstants.A_TAG_NAMES, null);
        if (tags != null) {
            return decodeTags(tags);
        }

        String tagIds = elt.getAttribute(MailConstants.A_TAGS, null);
        if (tagIds != null) {
            return tagIdStringToNames(mbox, octxt, tagIds);
        }

        return null;
    }
}
