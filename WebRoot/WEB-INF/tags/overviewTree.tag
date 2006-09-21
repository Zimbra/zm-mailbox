<%@ tag body-content="empty" %>
<%@ attribute name="folders" rtexprvalue="true" required="false" %>
<%@ attribute name="searches" rtexprvalue="true" required="false" %>
<%@ attribute name="contacts" rtexprvalue="true" required="false" %>
<%@ attribute name="calendars" rtexprvalue="true" required="false" %>
<%@ attribute name="tags" rtexprvalue="true" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="zm" uri="com.zimbra.zm" %>

<zm:getMailbox var="mailbox"/>
<c:if test="${folders}"><zm:folderTree/></c:if>
<c:if test="${contacts}"><zm:contactFolderTree/></c:if>
<c:if test="${searches}"><zm:searchFolderTree/></c:if>
<c:if test="${tags}"><zm:tagTree/></c:if>
