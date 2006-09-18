/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.2
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimlets
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
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
			handlerConfig.replaceAll("&amp;", "&");
			handlerConfig.replaceAll("&lt;", "<");
			handlerConfig.replaceAll("&gt;", ">");
			handlerConfig.replaceAll("&apos;", "'");
			handlerConfig.replaceAll("&quot;", "\"");
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
