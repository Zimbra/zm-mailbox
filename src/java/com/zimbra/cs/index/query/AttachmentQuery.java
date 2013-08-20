/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.zimbra.cs.index.LuceneFields;

/**
 * Query by attachment type.
 *
 * @author tim
 * @author ysasaki
 */
public final class AttachmentQuery extends LuceneQuery {
    // See http://blogs.msdn.com/b/vsofficedeveloper/archive/2008/05/08/office-2007-open-xml-mime-types.aspx
    // NOTE: Must use lowercase
    private static final ImmutableList<String> MS_WORD_TYPES = ImmutableList.of(
        "application/msword",                                                                 // .doc .dot
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",            // .docx
        "application/vnd.openxmlformats-officedocument.wordprocessingml.template",            // .dotx
        "application/vnd.ms-word.document.macroenabled.12",                                   // .docm
        "application/vnd.ms-word.template.macroenabled.12"                                    // .dotm
    );

    private static final ImmutableList<String> MS_EXCEL_TYPES = ImmutableList.of(
        "application/vnd.ms-excel",                                                           // .xls .xlt .xla
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",                  // .xlsx
        "application/vnd.openxmlformats-officedocument.spreadsheetml.template",               // .xltx
        "application/vnd.ms-excel.sheet.macroenabled.12",                                     // .xlsm
        "application/vnd.ms-excel.template.macroenabled.12",                                  // .xltm
        "application/vnd.ms-excel.addin.macroenabled.12",                                     // .xlam
        "application/vnd.ms-excel.sheet.binary.macroenabled.12"                               // .xlsb
    );

    private static final ImmutableList<String> MS_POWERPOINT_TYPES = ImmutableList.of(
        "application/vnd.ms-powerpoint",                                                      // .ppt .pot .pps .ppa
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",          // .pptx
        "application/vnd.openxmlformats-officedocument.presentationml.template",              // .potx
        "application/vnd.openxmlformats-officedocument.presentationml.slideshow",             // .ppsx
        "application/vnd.ms-powerpoint.addin.macroenabled.12",                                // .ppam
        "application/vnd.ms-powerpoint.presentation.macroenabled.12",                         // .pptm .potm
        "application/vnd.ms-powerpoint.slideshow.macroenabled.12"                             // .ppsm
    );

    protected static final Multimap<String, String> CONTENT_TYPES_MULTIMAP =
            new ImmutableListMultimap.Builder<String, String>()
            .put("any", LuceneFields.L_ATTACHMENT_ANY)
            .put("application", "application")
            .put("application/*", "application")
            .put("bmp", "image/bmp")
            .put("image/bmp", "image/bmp")
            .put("gif", "image/gif")
            .put("image/gif", "image/gif")
            .put("image", "image")
            .put("image/*", "image")
            .put("jpeg", "image/jpeg")
            .put("image/jpeg", "image/jpeg")
            .putAll("excel", MS_EXCEL_TYPES)
            .putAll("application/vnd.ms-excel", MS_EXCEL_TYPES)
            .putAll("xls", MS_EXCEL_TYPES)
            .putAll("ppt", MS_POWERPOINT_TYPES)
            .putAll("powerpoint", MS_POWERPOINT_TYPES)
            .putAll("application/vnd.ms-powerpoint", MS_POWERPOINT_TYPES)
            .put("ms-tnef", "application/ms-tnef")
            .put("application/ms-tnef", "application/ms-tnef")
            .putAll("word", MS_WORD_TYPES)
            .putAll("application/msword", MS_WORD_TYPES)
            .putAll("msword", MS_WORD_TYPES)
            .put("none", LuceneFields.L_ATTACHMENT_NONE)
            .put("pdf", "application/pdf")
            .put("application/pdf", "application/pdf")
            .put("text", "text")
            .put("text/*", "text")
            .build();

    private AttachmentQuery(String what) {
        super("attachment:", LuceneFields.L_ATTACHMENTS, what);
    }

    /**
     * Note: returns either a {@link AttachmentQuery} or a {@link SubQuery}
     * @return
     */
    public static Query createQuery(String what) {
        Collection<String> types = lookup(CONTENT_TYPES_MULTIMAP, what);
        if (types.size() == 1) {
            return new AttachmentQuery(Lists.newArrayList(types).get(0));
        } else {
            List<Query> clauses = new ArrayList<Query>(types.size() * 2 - 1);
            for (String contentType : types) {
                if (!clauses.isEmpty()) {
                    clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
                }
                clauses.add(new AttachmentQuery(contentType));
            }
            return new SubQuery(clauses);
        }
    }

    @Override
    public final void setModifier(Modifier mod) {
        // translate not "any" to "none"
        if (mod == Modifier.MINUS && LuceneFields.L_ATTACHMENT_ANY.equals(term)) {
            term = LuceneFields.L_ATTACHMENT_NONE;
            return;
        }
        super.setModifier(mod);
    }

}
