<%@ tag body-content="empty" %>

<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<div class=Tree>
<table cellpadding=0 cellspacing=0>
 <tr><th>Searches</th></tr>
   <zm:forEachFolder var="folder">
     <c:if test="${folder.isSearchFolder}">
       <zm:overviewSearchFolder folder="${folder}"/>
     </c:if>
  </zm:forEachFolder>
   <tr><td>&nbsp;</td></tr>  
</table>
</div>
