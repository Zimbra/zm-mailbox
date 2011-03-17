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

import java.io.StringReader;

import org.junit.Test;
import static org.junit.Assert.*;

public class HtmlTextExtractorTest {

    @Test
    public void extract()
    throws Exception {
        String html = "<html><head>" +
            "<title>Where It's At</title>" +
            "<style>style</style>" +
            "<script>script</script>" +
            "</head>" +
            "<body>I got two turntables and a microphone.</body></html>";
        String text = HtmlTextExtractor.extract(new StringReader(html), Integer.MAX_VALUE);
        assertTrue(text.contains("Where"));
        assertTrue(text.contains("microphone"));
        assertFalse(text.contains("script"));
        assertFalse(text.contains("style"));
    }
}
