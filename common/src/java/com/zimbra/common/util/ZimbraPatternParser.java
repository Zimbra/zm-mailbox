/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.util;

import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.pattern.PatternParser;

/**
 * Formats the <tt>%z</tt> pattern as all the keys and values passed
 * to {@link ZimbraLog#addToContext}.
 *  
 * @author bburtin
 */
public class ZimbraPatternParser {

    ZimbraPatternLayout mLayout;
    PatternParser patterParser;
    
    ZimbraPatternParser(String pattern, ZimbraPatternLayout layout) {
        patterParser = new PatternParser(pattern);
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
