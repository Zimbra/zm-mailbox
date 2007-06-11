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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.MDC;
import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Formats the <tt>%X</tt> pattern without braces as all the keys
 * and values in the {@link MDC}.
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
        if (c == 'X' && (i >= patternLength || (pattern.charAt(i) != '{'))) {
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
            Map context = MDC.getContext();
            if (context == null || context.size() == 0) {
                return "";
            }
            StringBuffer sb = new StringBuffer();
            Set<String> keyOrder = mLayout.getMdcKeyOrder();
            
            // Append ordered keys first
            if (keyOrder != null) {
                for (String key : keyOrder) {
                    Object value = context.get(key);
                    if (key != null && value != null) {
                        ZimbraLog.encodeArg(sb, key, value.toString());
                    }
                }
            }
            
            // Append the rest
            for (Object key : context.keySet()) {
                if (keyOrder == null ||
                    (keyOrder != null && !keyOrder.contains(key))) {
                    Object value = context.get(key);
                    if (key != null && value != null) {
                        ZimbraLog.encodeArg(sb, key.toString(), value.toString());
                    }
                }
            }
            
            return sb.toString();
        }
    }  
}
