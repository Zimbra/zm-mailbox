/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
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
package com.zimbra.common.mime;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class MimeDetectTest {

    @Test
    public void testFileName() throws IOException {
        MimeDetect.getMimeDetect().addGlob("image/jpeg", "*.jpg", 50);
        Assert.assertEquals("image/jpeg", MimeDetect.getMimeDetect().detect("2011.07.19 089+.JPG"));
        Assert.assertEquals("image/jpeg", MimeDetect.getMimeDetect().detect("2011.07.18 706+.jpg"));
        Assert.assertEquals("image/jpeg", MimeDetect.getMimeDetect().detect("2011.07.18 706+.jPg"));
    }
}
