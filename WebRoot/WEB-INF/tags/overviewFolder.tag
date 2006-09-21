<%@ tag body-content="empty" %>
<%@ attribute name="folder" rtexprvalue="true" required="true" type="com.zimbra.cs.jsp.bean.ZFolderBean"%>
<%@ attribute name="selected" rtexprvalue="true" required="false" %>
<%@ attribute name="label" rtexprvalue="true" required="false" %>
<%@ attribute name="icon" rtexprvalue="true" required="false" %>
<%@ attribute name="base" rtexprvalue="true" required="false" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<tr><td class='Folder<c:if test="${folder.hasUnread}"> Unread</c:if><c:if test="${folder.id eq selected}"> Selected</c:if>' >
 <a href='${empty base ? "clv.jsp" : base}?query=in:"${folder.pathUrlEncoded}"'>
   <img src="images/${empty icon ? 'Folder.gif' : icon}"/>
   <span>${fn:escapeXml(empty label ? folder.name : label)} <c:if test="${folder.hasUnread}"> (${folder.unreadCount}) </c:if></span>
 </a>

</td></tr>

