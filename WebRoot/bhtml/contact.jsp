<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<zm:getContact id="${param.id}" var="contact"/>

<html>
 <head>
  <title>Zimbra: ${fn:escapeXml(contact.displayFileAs)}</title>
  <style type="text/css">
    @import url("style.css");
  </style>
 </head>

 <body>
  <jsp:directive.include file="top.jsp" />

  <zm:appTabs selected='contacts'/>  

  <c:set var="toolbar">
   <table cellspacing=0 class='Tb'>
    <tr>
     <td align=left class=TbBt><a href="">Edit</a><a href="">Delete</a><a href="">Print</a></td>
    </tr>
   </table>
  </c:set>     

  <table>
   <tr>
    <td valign=top class='Left'>
      <zm:overviewTree contacts="true" tags="true"/>
    </td>
    <td valign='top' class='Right'>
     <div class='RightTbTop'>${toolbar}</div>
     <zm:contact contact="${contact}"/>
     <div class='RightTbBottom'>${toolbar}</div>
     <jsp:directive.include file="footer.jsp" />
    </td>
   </tr>
  </table>

 </body>
</html>


