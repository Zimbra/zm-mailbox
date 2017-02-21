<!--
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
-->

<%@ page isErrorPage="true" import="com.zimbra.common.util.L10nUtil,com.zimbra.common.util.L10nUtil.MsgKey" %>
<HTML>
<HEAD>
    <%
        Object redirectUrl = request.getAttribute("spnego.redirect.url");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Object autoRedirect = request.getAttribute("spnego.auto.redirect");
        if (autoRedirect != null && redirectUrl != null && (Boolean) autoRedirect) {
    %>
        <meta http-equiv="refresh" content="0; URL=<%=redirectUrl%>"/>
    <%
        } else {
    %>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <%
        }
    %>
    <title>Spnego Authentication Failed.</title>
    <style type="text/css">
        .unstyled {
            list-style: none;
        }
        .box {
            border: 1px solid #000;
        }
        .box > SPAN:first-child {
            background-size: 2rem 1.7rem;
            float: left;
            width: 100%;
            color: #ff0000;
            font-size: 1.5rem;
        }
        .container {
            text-align: center;
            padding: 1.5rem;
        }
        .header {
            font-weight: bold;
        }
        SPAN > STRONG, EM {
            font-size: 0.9rem;
        }
        .newLine {
            float: left;
            width: 100%;
        }
    </style>
</HEAD>
<BODY>
    <div>
        <div class="box container">
            <%=L10nUtil.getMessage(MsgKey.spnego_401_error_message, request)%>
        </div>
        <div class="container">
            <span class="newLine">
                <a href="<%= redirectUrl %>">
                    <%=L10nUtil.getMessage(MsgKey.spnego_redirect_message, request)%>
                </a>
            </span>
            <span class="newLine"><br /><br /></span>
            <span class="newLine">
                <a href="<%=L10nUtil.getMessage(MsgKey.spnego_browser_setup_wiki, request)%>">
                    <%=L10nUtil.getMessage(MsgKey.spnego_browser_setup_message, request)%>
                </a>
            </span>
        </div>
    </div>
</BODY>
</HTML>
