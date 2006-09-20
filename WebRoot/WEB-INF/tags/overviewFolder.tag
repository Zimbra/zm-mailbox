<%@ tag body-content="empty" %>
<%@ attribute name="folder" rtexprvalue="true" required="true" type="com.zimbra.cs.jsp.bean.ZFolderBean"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<tr><td ${folder.hasUnread ? 'class="Unread"' : ''}>
   <a href="conv.jsp?query=in:${folder.name}">${folder.name}</a>
       <c:if test="${folder.hasUnread}"> (${folder.unreadCount}) </c:if>
</td></tr>
