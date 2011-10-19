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
