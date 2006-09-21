<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="zm" uri="com.zimbra.zm" %>

<zm:getMailbox var="mailbox"/>

  <table cellspacing=0 class=Top>
   <tr height=40>
    <td style='padding-left:10px; padding-right:10px;'><a href="www.zimbra.com"><img src="images/AppBanner.png" border=0 alt="ZCS by Zimbra"></a></td>
    <td  style='width:100%' height=25 nowrap>
     <form method="get" action="clv.jsp"><input class='searchField' style='width:80%' maxlength=2048 name=query value="${fn:escapeXml(param.query)}">&nbsp;<input type=submit name=search value="Search"></form>
    </td>
    <td style='padding-right:5px'>
     <table>
      <tr> <td align=right valign=top> <b>${mailbox.name}</b> </td></tr>
      <tr>
       <td nowrap align=right valign=bottom >
         <a href="?op=options" target=_blank>Options</a> |
         <a href="?op=help" target=_blank>Help</a> |
         <a href="?logout">Log Off</a>
       </td>
      </tr>
     </table>
    </td>
   </tr>
  </table>
