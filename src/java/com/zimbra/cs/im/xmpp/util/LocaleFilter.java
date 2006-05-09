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
//
//package com.zimbra.cs.im.xmpp.util;
//
//import javax.servlet.*;
//import javax.servlet.jsp.jstl.core.Config;
//import java.io.IOException;
//
///**
// * Sets the locale context-wide.
// */
//public class LocaleFilter implements Filter {
//
//    private ServletContext context;
//
//    public void init(FilterConfig config) throws ServletException {
//        this.context = config.getServletContext();
//    }
//
//    /**
//     * Ssets the locale context-wide based on a call to {@link JiveGlobals#getLocale()}.
//     */
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException
//    {
//        // Note, putting the locale in the application at this point is a little overkill
//        // (ie, every user who hits this filter will do this). Eventually, it might make
//        // sense to just set the locale in the user's session and if that's done we might
//        // want to honor a preference to get the user's locale based on request headers.
//        // For now, this is just a convenient place to set the locale globally.
//        Config.set(context, Config.FMT_LOCALE, JiveGlobals.getLocale());
//
//        // Move along:
//        chain.doFilter(request, response);
//    }
//
//    /** Does nothing */
//    public void destroy() {
//    }
//}
