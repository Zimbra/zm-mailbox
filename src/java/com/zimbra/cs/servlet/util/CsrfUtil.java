/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra, Inc.
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

package com.zimbra.cs.servlet.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.common.net.HttpHeaders;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author zimbra
 *
 */
public final class CsrfUtil {

    /**
     * Private constructor.
     */
    private CsrfUtil() {

    }

    /**
     *
     * @param req servlet request object
     * @param checkReqForCsrf whether req are to be checked for CSRF attack
     * @param allowedRefHost a list of referer hosts that are allowed
     * @return true if it is a CSRF request elese false
     * @throws MalformedURLException when passed referer URL is invalid
     */
    public static boolean isCsrfRequest(final HttpServletRequest req, final boolean checkReqForCsrf,
        final List<String> allowedRefHost)
        throws MalformedURLException {

        if (!checkReqForCsrf) {
            return false;
        }

        boolean csrfReq = false;
        if (ZimbraLog.soap.isTraceEnabled()) {
            Enumeration<String> hdrNames = req.getHeaderNames();
            ZimbraLog.soap.trace("Soap request headers.");
            while (hdrNames.hasMoreElements()) {
                String name = hdrNames.nextElement();
                // we do not want to print cookie headers for security reasons.
                if (name.contains(HttpHeaders.COOKIE))
                    continue;
                ZimbraLog.soap.trace(name + "=" + req.getHeader(name));
            }
        }

        String host = getRequestHost(req);
        String referrer = req.getHeader(HttpHeaders.REFERER);
        String refHost = null;


        if (!StringUtil.isNullOrEmpty(referrer)) {
            URL refURL = new URL(referrer);
            refHost = refURL.getHost().toLowerCase();
        }

        if (refHost == null) {
            csrfReq = false;
        }  else if (refHost.equalsIgnoreCase(host)) {
           csrfReq = false;
        } else {
            if (allowedRefHost != null && allowedRefHost.contains(refHost)) {
                csrfReq = false;
            } else {
                csrfReq = true;
            }
        }

        if (ZimbraLog.soap.isDebugEnabled()) {
            ZimbraLog.soap.debug("Host : " + host + ", Referrer host :" + refHost + ", Allowed Host:"
                + allowedRefHost + " Soap req is"
                + (csrfReq ? " not allowed." : " allowed.") );
        }

        return csrfReq;
    }



    /**
     *
     * @param host
     * @return
     */
    public static String getRequestHost(final HttpServletRequest req) {

        String host = HttpUtil.getVirtualHost(req);
        if (host == null) {
            return host;
        }
        String temp = host;
        if (temp.indexOf(":") != -1) {
            int endIndex = temp.indexOf(":");
            temp = host.substring(0, endIndex);
        }
        if (ZimbraLog.soap.isTraceEnabled()) {
            ZimbraLog.soap.trace("Original host : " + host + " returning: " + temp);

        }

        temp = temp.toLowerCase();
        return temp;
    }

}
