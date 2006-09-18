<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class=Tree>
<table>
   <tr><th class=TreeHeading>Folders</th></tr>
   <tr><td class=Selected><a href="clv.jsp?query=in:inbox">Inbox</a></td></tr>   
   <tr><td><a href="?">Sent</a></td></tr>
   <tr><td><a href="?">Drafts</a></td></tr>      
   <tr><td><a href="?">Junk</a></td></tr>
   <tr><td><a href="?">Trash</a></td></tr>
</table>
</div>

<div class=Tree>
<table>
 <tr><th class=TreeHeading>Searches</th></tr>
   <zm:forEachFolder var="folder">
     <c:if test="${folder.isSearchFolder}">
       <tr><td><a href="clv.jsp?query=<c:out value="${folder.query}"/>"><c:out value="${folder.name}"/></a></td></tr>
     </c:if>
  </zm:forEachFolder>
</table>
</div>

<div class=Tree>
<table>
   <tr><th class=TreeHeading>Tags</th></tr>
   <zm:forEachTag var="tag">
     <tr><td class='${tag.hasUnread ? ' Unread':''}'>
     <a href="clv.jsp?query=tag:<c:out value="${tag.name}"/>"><c:out value="${tag.name}"/>
     <c:if test="${tag.hasUnread}"> (${tag.unreadCount}) </c:if>
     </a>
  </zm:forEachTag>
</table>
</div>
