<%@ tag body-content="empty" %>
<%@ attribute name="message" rtexprvalue="true" required="true" type="com.zimbra.cs.jsp.bean.ZMessageBean"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
 
<div width=100% height=100% class=Msg>
 <div class=MsgHdr>
   <table width=100% cellpadding=2 cellspacing=0 border=0>
   <tr><td class='MsgHdrName MsgHdrSub'>Subject:</td><td class='MsgHdrValue MsgHdrSub'>${fn:escapeXml(message.subject)}</td></tr>
   <tr>
      <td class='MsgHdrName'>Sent by:</td>
      <td class='MsgHdrValue'><c:out value="${message.displayFrom}" default='<Unknown>'/>; On: <c:out value="${message.displaySentDate}"/></td></tr>

   <c:set var="to" value="${message.displayTo}"/>
   <c:if test="${!(empty to)}">
      <tr><td class='MsgHdrName'>To:</td><td class='MsgHdrValue'><c:out value="${to}"/></td></tr>
   </c:if>

   <c:set var="cc" value="${message.displayCc}"/>
   <c:if test="${!(empty cc)}">
      <tr><td class='MsgHdrName'>Cc:</td><td class='MsgHdrValue'><c:out value="${cc}"/></td></tr>
   </c:if>

   <c:set var="bcc" value="${message.displayBcc}"/>
   <c:if test="${!(empty bcc)}">
      <tr><td class='MsgHdrName'>Bcc:</td><td class='MsgHdrValue'><c:out value="${bcc}"/></td></tr>
   </c:if>

   <c:set var="replyto" value="${message.displayReplyTo}"/>   
   <c:if test="${!(empty replyto)}">
      <tr><td class='MsgHdrName'>Reply To:</td><td class='MsgHdrValue'><c:out value="${replyto}"/></td></tr>
   </c:if>   

  </table>
 </div>
 <div class=MsgBody>
   ${message.displayBodyHtml}
 </div>
</div>
