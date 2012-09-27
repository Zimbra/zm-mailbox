/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.cs.servlet;

import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;

import com.zimbra.common.localconfig.LC;


public class DoSFilter extends org.eclipse.jetty.servlets.DoSFilter {
    
    @Override
    public void init(FilterConfig filterConfig) {
        super.init(filterConfig);
        _maxRequestsPerSec = LC.zimbra_dos_filter_max_requests_per_sec.intValue();
    }
    
    @Override
    protected String extractUserId(ServletRequest request) {
        return ZimbraQoSFilter.extractUserId(request);
    }
}
