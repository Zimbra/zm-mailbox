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

package com.zimbra.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

public class FileUtilTest {

    /**
     * Overrides {@code lastModified} to return system time.  {@code File.lastModified()} may
     * return values in second-level granularity, which causes the unit test to fail.
     */
    @SuppressWarnings("serial")
    class VirtualFile extends File {
        final long lastModified = System.currentTimeMillis();

        VirtualFile(String name) {
            super(name);
        }

        @Override
        public long lastModified() {
            return lastModified;
        }
    }

    @Test
    public void sortByMtime() throws InterruptedException {
        List<File> files = Lists.newArrayList();
        File temp1 = new VirtualFile("temp1");
        files.add(temp1);
        Thread.sleep(50);

        File temp2 = new VirtualFile("temp2");
        files.add(temp2);

        assertTrue(temp1.lastModified() != temp2.lastModified());

        FileUtil.sortFilesByModifiedTime(files, true);
        assertEquals(temp2, files.get(0));
        FileUtil.sortFilesByModifiedTime(files);
        assertEquals(temp1, files.get(0));
    }
}
