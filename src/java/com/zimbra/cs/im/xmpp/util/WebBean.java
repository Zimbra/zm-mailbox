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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
//
//package com.zimbra.cs.im.xmpp.util;
//
//import javax.servlet.ServletContext;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.HttpSession;
//import javax.servlet.jsp.JspWriter;
//import javax.servlet.jsp.PageContext;
//
//public abstract class WebBean {
//
//    public HttpSession session;
//    public HttpServletRequest request;
//    public HttpServletResponse response;
//    public ServletContext application;
//    public JspWriter out;
//
//    public void init(HttpServletRequest request, HttpServletResponse response,
//            HttpSession session, ServletContext app, JspWriter out)
//    {
//        this.request = request;
//        this.response = response;
//        this.session = session;
//        this.application = app;
//        this.out = out;
//    }
//
//    public void init(HttpServletRequest request, HttpServletResponse response,
//                     HttpSession session, ServletContext app) {
//
//        this.request = request;
//        this.response = response;
//        this.session = session;
//        this.application = app;
//    }
//
//    public void init(PageContext pageContext){
//        this.request = (HttpServletRequest)pageContext.getRequest();
//        this.response = (HttpServletResponse)pageContext.getResponse();
//        this.session = (HttpSession)pageContext.getSession();
//        this.application = (ServletContext)pageContext.getServletContext();
//        this.out = (JspWriter)pageContext.getOut();
//    }
//}