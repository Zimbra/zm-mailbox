/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 5, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.zimlet.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.cs.zimlet.ZimletConf;
import com.zimbra.cs.zimlet.ZimletConfig;
import com.zimbra.cs.zimlet.ZimletException;
import com.zimbra.cs.zimlet.ZimletHandler;

/**
 * @author schemers
 *
 * Generic object handler that gets its regex from the handler config.
 * 
 */
public class RegexHandler implements ZimletHandler {

    private Pattern mPattern;
	  
	public String[] match(String text, ZimletConf config) throws ZimletException {
	    if (mPattern == null) {
			String handlerConfig = config.getGlobalConf(ZimletConfig.CONFIG_REGEX_VALUE);
			if (handlerConfig == null) {
				throw ZimletException.ZIMLET_HANDLER_ERROR("null regex value");
			}
	        mPattern = Pattern.compile(handlerConfig);
	    }
        Matcher m = mPattern.matcher(text);
        List l = new ArrayList();
        while (m.find()) {
        	l.add(text.substring(m.start(), m.end()));
        }
        return (String[]) l.toArray(new String[0]);
	}
}
