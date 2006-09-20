<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<zm:getMailbox var="mailbox"/>

<div class=Tree>
<table cellpadding=0 cellspacing=0>
   <tr><th>Folders</th></tr>

   <zm:overviewFolder selected='2' folder="${mailbox.inbox}" label="Inbox" icon="Inbox.gif"/>
   <zm:overviewFolder folder="${mailbox.sent}" label="Sent" icon="SentFolder.gif"/>
   <zm:overviewFolder folder="${mailbox.drafts}" label="Drafts" icon="DraftFolder.gif"/>
   <zm:overviewFolder folder="${mailbox.spam}" label="Junk" icon="SpamFolder.gif"/>
   <zm:overviewFolder folder="${mailbox.trash}" label="Trash" icon="Trash.gif"/>

   <tr><td>&nbsp;</td></tr>
    
   <zm:forEachFolder var="folder">
    <c:if test="${!folder.isSystemFolder and (folder.isNullView or folder.isMessageView or folder.isConversationView) and !folder.isSearchFolder}">
      <zm:overviewFolder folder="${folder}"/>
    </c:if>
   </zm:forEachFolder>      
</table>
</div>

<div class=Tree>
<table cellpadding=0 cellspacing=0>
 <tr><th>Searches</th></tr>
   <zm:forEachFolder var="folder">
     <c:if test="${folder.isSearchFolder}">
       <zm:overviewSearchFolder folder="${folder}"/>
     </c:if>
  </zm:forEachFolder>
</table>
</div>

<div class=Tree>
<table cellpadding=0 cellspacing=0>
   <tr><th>Tags</th></tr>
   <zm:forEachTag var="tag">
	<zm:overviewTag tag="${tag}"/>
  </zm:forEachTag>
</table>
</div>
