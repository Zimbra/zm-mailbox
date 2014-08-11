<!--
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
-->
<%@ page buffer="8kb" autoFlush="true" %>
<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ page session="false" %>
<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="com.zimbra.i18n" %>
<%@ taglib prefix="app" uri="com.zimbra.htmlclient" %>
<app:skinAndRedirect />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<fmt:setLocale value='${pageContext.request.locale}' scope='request' />
<fmt:setBundle basename="/messages/ZhMsg" scope="request"/>
<fmt:setBundle basename="/messages/ZmMsg" var="zmmsg" scope="request"/>

<%
    Object errorCode = request.getAttribute("javax.servlet.error.status_code");
    String errorTitle = errorCode+"Title";
    String errorMsg = errorCode+"Msg";
%>

<c:set var="errCode" value="<%=errorCode%>"/>
<c:set var="errTitle" value="<%=errorTitle%>"/>
<c:set var="errMsg" value="<%=errorMsg%>"/>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8">
    <title>400 - Invalid Request</title>
    <meta name="viewport" content="width=300; initial-scale=1.0; maximum-scale=8.0; user-scalable=1;">
    <meta name="description" content="Zimbra provides open source server and client software for messaging and collaboration. To find out more visit http://www.zimbra.com.">
    <link  rel="stylesheet" type="text/css" href="/css/common,login,zhtml,skin.css">
    <c:param name="skin" value="${param.skin}" />
        <c:param name="v" value="${version}" />
    </c:url>">
    <zm:getFavIcon request="${pageContext.request}" var="favIconUrl" />
    <c:if test="${empty favIconUrl}">
        <fmt:message key="favIconUrl" var="favIconUrl"/>
    </c:if>
    <link rel="SHORTCUT ICON" href="/img/logo/favicon.ico">
</head>
<c:set value="/img" var="iconPath" scope="request"/>
<body>
    <div class="ErrorScreen">
        <div class="center">
            <div class="contentBox">
                <div class="InlineErrorPanel">
                    <table width="100%">
                        <tr>
                            <td width="1%">
                                <img src="/img/dwt/ImgWarning_32.png?v=${version}"
                                     title="Error" alt="Error" id="ZErrorIcon">
                            </td>
                            <td><h2 style="margin:0;">Invalid Request</h2></td>
                        </tr>
                        <tr>
                            <td></td>
                            <td style="border-top:1px solid #333;">
                                <p style="margin:1em 0 2em;">Missing required parameter.<br/>
                                    Please try again.</p>
                                <p style="margin-bottom:2em;">ERROR: 400</p>
                                <p style="font-size:1.2em;font-weight:bold;margin-bottom:1em;">
                                    <a href="/">
                                        <span style="font-size:1.5em;">&laquo;</span>
                                        <span>Go back</span>
                                    </a>
                                </p>
                            </td>
                        </tr>
                    </table>
                </div>
            </div>
            <div class="decor1"></div>
        </div>
        <div class="decor2"></div>
    </div>
</body>
</html>
