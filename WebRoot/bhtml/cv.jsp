<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
 <head>
  <title>Zimbra: Inbox</title>
  <style type="text/css">
    @import url("style.css");
  </style>
 </head>

 <body>
  <jsp:directive.include file="top.jsp" />
  <zm:appTabs selected='mail'/>
  <zm:search var="searchResult" limit="25" query="${param.query}" offset="${param.offset}" conv="${param.id}"/>
  <c:set var="toolbar">
  <table cellspacing=0 class='Tb'>
   <tr>
    <td align=left class=TbBt><a href="">New</a><a href="">Reply</a><a href="">Reply All</a></td>
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
        <th width=1% nowrap>
        <th width=1% nowrap><img src="images/FlagRed.gif" width=16 height=16 border=0 alt="">
        <th width=15%>From
        <th width=1% nowrap><img src="images/Attachment.gif" width=16 height=16 border=0 alt="">
        <th width=78%>Fragment
        <th width=1% nowrap>Size
        <th width=1% nowrap>Received
       </tr>

       <c:set value="${searchResult.hits[0].id}" var="mid"/>
       <c:forEach items="${searchResult.hits}" var="mess">
       <tr class='Row${mess.isUnread ? ' Unread':''}${mess.id == mid ? ' RowSelected' : ''}'>
        <td width=1% nowrap><input type=checkbox name=t value="1"></td>
        <td width=1% nowrap align=center><img src="images/${mess.isFlagged? 'FlagRed.gif' : 'Blank_16.gif'}" width=16 height=16 border=0 alt=Starred></td>
        <td>${fn:escapeXml(mess.displaySender)}</td>
        <td width=1% nowrap align=center><img src="images/${mess.hasAttachment? 'Attachment.gif' : 'Blank_16.gif'}" width=16 height=16 border=0 alt=Attachment></td>
        <td><a href="cv.jsp?id=${mess.id}">
            ${fn:escapeXml(empty mess.fragment ? '(none)' : zm:truncate(mess.fragment,100, true))}
         </a>
        </td>
        <td nowrap>${mess.size}
        <td nowrap>Feb&nbsp;27
       </tr>   
       </c:forEach>
       <tr><td colspan=7>&nbsp;</td></tr>
      </table>
     </div> <%-- list --%>
     <zm:getMessage var="msg" id="${mid}" markread="true" wanthtml="false" neuterimages="false"/>
     <zm:message message="${msg}"/>
     <div class='RightTbBottom'>${toolbar}</div>
     <jsp:directive.include file="footer.jsp" />
    </td>
   </tr>
  </table>
 </body>
</html>
