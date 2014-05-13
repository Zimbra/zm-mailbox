/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.util.security.Constraint;



/**
 * @author zimbra
 *
 */
public class EwsAuthenticator extends ZimbraAuthenticator {

    protected String urlPattern = "";

    /**
     * @return the urlPattern
     */
    @Override
    public String getUrlPattern() {
        return urlPattern;
    }

    /**
     * @param urlPattern the urlPattern to set
     */
    @Override
    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern == null ? "/service/extension/zimbraews/*" : urlPattern.replace("//","/");
    }

    /* (non-Javadoc)
     * @see org.eclipse.jetty.security.Authenticator#getAuthMethod()
     */
    @Override
    public String getAuthMethod() {
        return Constraint.NONE;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jetty.security.Authenticator#validateRequest(javax.servlet.ServletRequest, javax.servlet.ServletResponse, boolean)
     */
    @Override
    public Authentication validateRequest(ServletRequest request, ServletResponse response,
        boolean mandatory) throws ServerAuthException {


        HttpServletRequest httpReq = (HttpServletRequest) request;
        if (PathMap.match(urlPattern, httpReq.getRequestURI())) {
            //We want the Authentication to be set to Unauthenticated so that Spengo Service is not
            // invoked for EWS, returning null will set it to UnAuthenticated.
            return null;
        } else {
            return super.validateRequest(request, response, mandatory);
        }

    }

}
