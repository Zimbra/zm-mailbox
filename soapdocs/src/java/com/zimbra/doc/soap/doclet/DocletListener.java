/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
