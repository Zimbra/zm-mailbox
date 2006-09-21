<%@ tag body-content="empty" %>

<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<zm:getMailbox var="mailbox"/>

<div class=Tree>
<table cellpadding=0 cellspacing=0>
   <tr><th>Address Books</th></tr>

   <zm:overviewFolder base="contacts.jsp" selected='2' folder="${mailbox.contacts}" label="Contacts" icon="ContactsFolder.gif"/>
   <zm:overviewFolder base="contacts.jsp" selected='2' folder="${mailbox.autoContacts}" label="Emailed Contacts" icon="ContactsFolder.gif"/>   
    
   <zm:forEachFolder var="folder">
    <c:if test="${!folder.isSystemFolder and folder.isContactView and !folder.isSearchFolder}">
      <zm:overviewFolder base="contacts.jsp" folder="${folder}" icon="ContactsFolder.gif"/>
    </c:if>
   </zm:forEachFolder>     
      <tr><td>&nbsp;</td></tr> 
</table>
</div>
