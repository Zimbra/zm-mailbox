/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Formats the <tt>%z</tt> pattern as all the keys and values passed
 * to {@link ZimbraLog#addToContext}.
 *  
 * @author bburtin
 */
public class ZimbraPatternParser
extends PatternParser {

    ZimbraPatternLayout mLayout;
    
    ZimbraPatternParser(String pattern, ZimbraPatternLayout layout) {
        super(pattern);
        mLayout = layout;
    }
      
    public void finalizeConverter(char c) {
        if (c == 'z') {
            addConverter(new ZimbraPatternConverter(formattingInfo));
            currentLiteral.setLength(0);
        } else {
            super.finalizeConverter(c);
        }
    }

    private class ZimbraPatternConverter extends PatternConverter {
        ZimbraPatternConverter(FormattingInfo formattingInfo) {
            super(formattingInfo);
        }

        public String convert(LoggingEvent event) {
            return ZimbraLog.getContextString();
        }
    }  
}
