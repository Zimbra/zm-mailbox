<%@ tag body-content="empty" %>
<%@ attribute name="tag" rtexprvalue="true" required="true" type="com.zimbra.cs.jsp.bean.ZTagBean"%>
<%@ attribute name="selected" rtexprvalue="true" required="false" %>
<%@ attribute name="label" rtexprvalue="true" required="false" %>
<%@ attribute name="icon" rtexprvalue="true" required="false" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<tr><td class='Folder ${tag.hasUnread ? ' Unread':''}'>
 <a href='clv.jsp?query=tag:"${fn:escapeXml(tag.name)}"'/>
 <img src="images/TagOrange.gif"/>
 <span>
 <c:out value="${tag.name}"/>
 <c:if test="${tag.hasUnread}"> (${tag.unreadCount}) </c:if>
 </span>
 </a>
 </td></tr>
 