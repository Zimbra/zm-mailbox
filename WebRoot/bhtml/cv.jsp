<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
 <head>
  <title>Zimbra: Inbox</title>
  <style type="text/css">
    @import url("style.css");
  </style>
 </head>

 <body>

  <! --------- TOP ------>
  <jsp:directive.include file="top.jsp" />

  <! --------- TABS ------>
  <table cellspacing=0 class=Tabs>
   <tr> 
    <td class='TabFiller'>
&nbsp;
    </td>
    <td class='TabSelected'>
Mail
    </td>

    <td class='Tab'>
<a href="contacts.jsp">Contacts</a>
    </td>
    <td class='Tab'>
<a href="">Calendar</a>
    </td>
    <td class='TabFiller'>
&nbsp;
    </td>
    <td class='Tab'>
<a href="">Options</a>
    </td>
   </tr>
  </table>

<zm:search var="searchResult" limit="25" query="${param.query}" offset="${param.offset}" conv="${param.id}"/>
<c:set var="toolbar">
 <table cellspacing=0 class='Tb'>
   <tr>
<td align=left class=TbBt>
 <a href="">New</a>
 <a href="">Reply</a>
 <a href="">Reply All</a>
</td>
<td align=right>
 <c:if test="${param.offset > 0}">
   <a href="?offset=${searchResult.previousOffset}"><img src="images/arrows/PreviousPage.gif" border=0></a>
 </c:if>
 <span>${param.offset+1} - ${param.offset+searchResult.size}</span>
 <c:if test="${searchResult.hasMore}">
   <a href="?offset=${searchResult.nextOffset}"><img src="images/arrows/NextPage.gif" border=0></a>
 </c:if>         	
</td>
</tr>
</table>
</c:set>     



  <! --------- LEFT/RIGHT ------>
  <table>
   <tr>
    <!------- LEFT -->
    <td valign=top style='width:150px'>
      <jsp:directive.include file="left.jsp" />
    </td>
    <!-- right -->
    <td valign='top'>
     <div class='RightTbTop'>${toolbar}</div>

<div class=List>
<table cellpadding=2 cellspacing=0 >

<tr>
<th width=1% nowrap>
<th width=1% nowrap><img src="images/FlagRed.gif" width=16 height=16 border=0 alt="">
<th width=20%>From
<th width=1% nowrap><img src="images/Attachment.gif" width=16 height=16 border=0 alt="">
<th width=78%>Fragment
<th width=1% nowrap>Size
<th width=1% nowrap>Received
</tr>

<c:forEach items="${searchResult.hits}" var="mess">
<tr class='ConvRow${mess.isUnread ? ' Unread':''}'>
<td width=1% nowrap><input type=checkbox name=t value="1"></td>
<td width=1% nowrap><img src="images/${mess.isFlagged? 'FlagRed.gif' : 'Blank_16.gif'}" width=16 height=16 border=0 alt=Starred></td>
<td><c:out value="${mess.displaySender}"/></td>
<td width=1% nowrap><img src="images/${mess.hasAttachment? 'Attachment.gif' : 'Blank_16.gif'}" width=16 height=16 border=0 alt=Attachment></td>
<td><a href="cv.jsp?id=${mess.id}"><c:out value="${empty mess.fragment ? '(none)' : mess.fragment}"/> 
<%-- <c:if test="${!(empty mess.fragment)}"><span class='Fragment'> - <c:out value="${mess.fragment}"/></span></c:if>  --%>
</a></td>
<td nowrap>${mess.size}
<td nowrap>Feb&nbsp;27
</tr>   
</c:forEach>
<tr><td colspan=7>&nbsp;</td></tr>
</table>
</div> <%-- list --%>

<c:set value="${searchResult.hits[0].id}" var="mid"/>

<zm:getMessage var="msg" id="${mid}" markread="true" wanthtml="false" neuterimages="false"/>
 
<div width=100% height=100% class=Msg>
 <div class=MsgHdr>
   <table width=100% cellpadding=2 cellspacing=0 border=0 class=Tabs>
   <tr><td class='MsgHdrName MsgHdrSub'>Subject:</td><td class='MsgHdrSub'><c:out value="${msg.subject}"/></td></tr>
   <tr>
      <td class='MsgHdrName'>Sent by:</td>
      <td><c:out value="${msg.displayFrom}" default='<Unknown>'/>; On: <c:out value="${msg.displaySentDate}"/></td></tr>

   <c:set var="to" value="${msg.displayTo}"/>
   <c:if test="${!(empty to)}">
      <tr><td class='MsgHdrName'>To:</td><td><c:out value="${to}"/></td></tr>
   </c:if>

   <c:set var="cc" value="${msg.displayCc}"/>
   <c:if test="${!(empty cc)}">
      <tr><td class='MsgHdrName'>Cc:</td><td><c:out value="${cc}"/></td></tr>
   </c:if>

   <c:set var="bcc" value="${msg.displayBcc}"/>
   <c:if test="${!(empty bcc)}">
      <tr><td class='MsgHdrName'>Bcc:</td><td><c:out value="${bcc}"/></td></tr>
   </c:if>

   <c:set var="replyto" value="${msg.displayReplyTo}"/>   
   <c:if test="${!(empty replyto)}">
      <tr><td class='MsgHdrName'>Reply To:</td><td><c:out value="${replyto}"/></td></tr>
   </c:if>   

  </table>
 </div>
 <div class=MsgBody>
   ${msg.displayBodyHtml}
 </div>
</div>

<div class='RightTbBottom'>${toolbar}</div>
<jsp:directive.include file="footer.jsp" />

    </td>
   </tr>
  </table>

 </body>

</html>
