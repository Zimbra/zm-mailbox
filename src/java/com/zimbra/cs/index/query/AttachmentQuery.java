/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.index.query;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.index.LuceneFields;

/**
 * Query by attachment type.
 *
 * @author tim
 * @author ysasaki
 */
public class AttachmentQuery extends LuceneQuery {
    private static final Map<String, String> mMap;

    static {
        mMap = new HashMap<String, String>();

        addMapping(mMap, new String[] {"any"}, "any");
        addMapping(mMap, new String[] {"application", "application/*"}, "application");
        addMapping(mMap, new String[] {"bmp", "image/bmp"}, "image/bmp");
        addMapping(mMap, new String[] {"gif", "image/gif"}, "image/gif");
        addMapping(mMap, new String[] {"image", "image/*"}, "image");
        addMapping(mMap, new String[] {"jpeg", "image/jpeg"}, "image/jpeg");
        addMapping(mMap, new String[] {"excel", "application/vnd.ms-excel", "xls"}, "application/vnd.ms-excel");
        addMapping(mMap, new String[] {"ppt", "application/vnd.ms-powerpoint"}, "application/vnd.ms-powerpoint");
        addMapping(mMap, new String[] {"ms-tnef", "application/ms-tnef"}, "application/ms-tnef");
        addMapping(mMap, new String[] {"word", "application/msword", "msword"}, "application/msword");
        addMapping(mMap, new String[] {"none"}, "none");
        addMapping(mMap, new String[] {"pdf", "application/pdf"}, "application/pdf");
        addMapping(mMap, new String[] {"text", "text/*"}, "text");
    }

    public AttachmentQuery(String what) {
        super("type:", LuceneFields.L_ATTACHMENTS, lookup(mMap, what));
    }

    AttachmentQuery(String luceneField, String what) {
        super("type:", luceneField, lookup(mMap, what));
    }
}
