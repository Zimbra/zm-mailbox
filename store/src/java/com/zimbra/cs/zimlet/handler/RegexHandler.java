/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.zimlet.handler;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.zimbra.common.util.ZimbraLog;
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

    @Override
    public String[] match(String text, ZimletConf config) throws ZimletException {
        if (mPattern == null) {
            String handlerConfig = config.getGlobalConf(ZimletConfig.CONFIG_REGEX_VALUE);
            if (handlerConfig == null) {
                throw ZimletException.ZIMLET_HANDLER_ERROR("null regex value");
            }
            mPattern = Pattern.compile(handlerConfig);
            ZimbraLog.zimlet.debug("RegexHandler %s=%s (for config=%s)", ZimletConfig.CONFIG_REGEX_VALUE,
                    handlerConfig, config.getClass().getName());
        }
        Matcher m = mPattern.matcher(text);
        List<String> l = Lists.newArrayList();
        while (m.find()) {
            l.add(text.substring(m.start(), m.end()));
            ZimbraLog.zimlet.trace("RegexHandler matcher found match=[%s] for pattern=[%s]",
                    text.substring(m.start(), m.end()), mPattern.pattern());
        }
        return l.toArray(new String[0]);
    }
}
