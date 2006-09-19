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

</table>
</div> <%-- list --%>

<zm:getMessage var="msg" id="526" markread="true" wanthtml="false" neuterimages="false"/>
 
<div width=100% height=100% class=Message>
 <div class=MessageHeader>
   <table width=100% cellpadding=2 cellspacing=0 border=0 class=Tabs>
   <tr><td align=right><b>Subject:</b></td><td><c:out value="${msg.subject}"/></td></tr>
   <tr><td align=right><b>From:</b></td><td>"Apple Computer" &lt;support@mac.com&gt;</td></tr>
   <tr><td align=right><b>To:</b></td><td>.Mac user &lt;noreply@mac.com&gt;</td></tr>
   <tr><td align=right><b>Date:</b></td><td><c:out value="${msg.displaySentDate}"/></td></tr>
  </table>
 </div>
 <div class=MessageBody>
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
