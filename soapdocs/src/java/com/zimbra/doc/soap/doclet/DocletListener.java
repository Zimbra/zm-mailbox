/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.doc.soap.doclet;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Tag;

public abstract class DocletListener {

    private String className = null;

    public DocletListener(String className) {
        this.className = className;
    }

    public String getClassName() {
        return this.className;
    }

    /**
     * Called when a registered class is found.
     */
    public abstract void classEvent(ClassDoc classDoc);

    /**
     * Gets the tag text for a given tag.
     *
     * @param    tags an array of tags
     * @param    tag  the tag
     * @return   the tag text or <code>null</code> if tag does not exist
     */
    protected static String getTagText(Tag[] tags, String tag) {
        if (tags.length > 0) {
            for (int k=0; k < tags.length; k++) {
                if (tags[k].name().equalsIgnoreCase(tag)) {
                    return tags[k].text();
                }
            }
        }
        return null;
    }

    /**
     * Dumps the tags to <code>System.out</code>.
     *
     * @param tags an array of tags
     */
    protected static void dumpTags(Tag[] tags) {
        System.out.println("Dumping tags...");
        System.out.println(String.format("tags.length = %d", tags.length));
        if (tags.length > 0) {
            for (int k=0; k < tags.length; k++) {
                System.out.println(String.format("tags[%d].name = %s", k, tags[k].name()));
            }
        }
    }
}
