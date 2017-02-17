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

package com.zimbra.common.jetty;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.rewrite.handler.Rule;

import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;

public class PortRule extends Rule {

    private int _port;
    protected Pattern _regex; 
    
    private Integer _httpErrorStatusRegexNotmatched;
    private String _httpErrorReasonRegexNotMatched;  // a value on L10nUtil.MsgKey

    /* ------------------------------------------------------------ */
    public PortRule()
    {
        _handling = true;
        _terminating = true;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Sets the port.
     * 
     * @param port the port
     */
    public void setPort(int port)
    {
        _port = port;
    }

    /* ------------------------------------------------------------ */
    /**
     * Sets the regular expression string used to match with string URI.
     * 
     * @param regex the regular expression.
     */
    public void setRegex(String regex)
    {
        _regex=Pattern.compile(regex);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * If the regex is not matched, handle the request with the 
     * http status and reason.
     * 
     * @param value the replacement string.
     */
    public void setHttpErrorStatusRegexNotMatched(int status)
    {
        _httpErrorStatusRegexNotmatched = status;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * If the regex is not matched, handle the request with the 
     * http status and reason.
     *  
     * @param value the replacement string.
     */
    public void setHttpErrorReasonRegexNotMatched(String reason)
    {
        _httpErrorReasonRegexNotMatched = reason;
    }
    
    @Override
    public String matchAndApply(String target, HttpServletRequest request, HttpServletResponse response) 
    throws IOException {
        int port = request.getLocalPort();
        
        if (port == _port) {
            Matcher matcher=_regex.matcher(target);
            if (!matcher.matches()) {
                return apply(target, request, response);
            }
        }
        return null;
    }

    private String apply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        String reason = null;
        if (_httpErrorReasonRegexNotMatched != null) {
            try {
                L10nUtil.MsgKey reasonKey = L10nUtil.MsgKey.valueOf(_httpErrorReasonRegexNotMatched);
                reason = L10nUtil.getMessage(reasonKey);
            } catch (IllegalArgumentException e) {
                ZimbraLog.misc.debug("invalid msg key: " + _httpErrorReasonRegexNotMatched);
            }
        }
        
        if (reason == null) {
            response.sendError(_httpErrorStatusRegexNotmatched);
        } else {
            response.sendError(_httpErrorStatusRegexNotmatched, reason);
        }
        return target;
    }
    
    
    /**
     * Returns the rule pattern.
     */
    public String toString()
    {
        return super.toString()+"["+_port+"]"+"["+ ((_regex != null) ? _regex.toString() : "") +"]"+"["+_httpErrorStatusRegexNotmatched+"]";                
    }

}
