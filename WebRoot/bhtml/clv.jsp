<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<html>
 <head>
  <title>Zimbra: ${fn:escapeXml(empty param.query ? 'Inbox' : param.query)}</title>
  <style type="text/css">
    @import url("style.css");
  </style>
 </head>

 <body>

  <jsp:directive.include file="top.jsp" />
  <zm:appTabs selected='mail'/>

<zm:search var="searchResult" limit="25" query="${param.query}" offset="${param.offset}"/>

<c:set var="toolbar">
 <table cellspacing=0 class='Tb'>
   <tr>
<td align=left class=TbBt>
 <a href="">New</a>
 <a href="">Reply</a>
 <a href="">Reply All</a>
</td>
<td align=right>
 <c:if test="${param.offset gt 0}">
   <a href="?offset=${searchResult.previousOffset}"><img src="images/arrows/LeftArrow.gif" border=0></a>
 </c:if>
 <c:if test="${searchResult.size gt 0}">
 <span>${param.offset+1} - ${param.offset+searchResult.size}</span>
 </c:if>
 <c:if test="${searchResult.hasMore}">
   <a href="?offset=${searchResult.nextOffset}"><img src="images/arrows/RightArrow.gif" border=0></a>
 </c:if>         	
</td>
</tr>
</table>
</c:set>     



  <table>
   <tr>
    <td valign=top class='Left'>
      <zm:overviewTree folders="true" tags="true" searches="true"/>
    </td>
    <td valign='top' class='Right'>
     <div class='RightTbTop'>${toolbar}</div>

<div class=List>
<table cellpadding=2 cellspacing=0 >

<tr>
<th width=1% nowrap>&nbsp;
<th width=1% nowrap><img src="images/FlagRed.gif" width=16 height=16 border=0 alt="">
<th width=30%>From
<th width=1% nowrap><img src="images/Attachment.gif" width=16 height=16 border=0 alt="">
<th width=68%>Subject
<th width=1% nowrap>&nbsp;
<th width=1% nowrap>Received
</tr>

<c:set value="${searchResult.hits[0].id}" var="cid"/>

<c:forEach items="${searchResult.hits}" var="conv">
<tr class='Row ${conv.isUnread ? ' Unread':''}${conv.id == cid ? ' RowSelected' : ''}'>
<td width=1% nowrap><input type=checkbox name=t value="1"></td>
<td width=1% nowrap align=center><img src="images/${conv.isFlagged? 'FlagRed.gif' : 'Blank_16.gif'}" width=16 height=16 border=0 alt=Starred></td>
<td><c:out value="${conv.displayRecipients}" default="<Unknown>"/></td>
<td width=1% nowrap align=center><img src="images/${conv.hasAttachment? 'Attachment.gif' : 'Blank_16.gif'}" width=16 height=16 border=0 alt=Attachment></td>


<td><a href="<c:url value="cv.jsp">
  <c:param name='id' value='${conv.id}'/>
  <c:param name='query' value='${param.query}'/></c:url>"><c:out value="${empty conv.subject ? '<No Subject>' : conv.subject}"/> 
</a></td>
<td nowrap><c:if test="${conv.messageCount > 1}">(${conv.messageCount})&nbsp;</c:if><c:if test="${conv.messageCount < 2}">&nbsp</c:if>
<td nowrap>Feb&nbsp;27
</tr>   
</c:forEach>

</table>
 <c:if test="${searchResult.size == 0}">
 <div class='NoResults'>No results found.</div>
 </c:if>
</div> <%-- list --%>

<div class='RightTbBottom'>${toolbar}</div>
<jsp:directive.include file="footer.jsp" />

    </td>
   </tr>
  </table>

 </body>

</html>
