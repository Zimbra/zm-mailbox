<%@ tag body-content="empty" %>
<%@ attribute name="selected" rtexprvalue="true" required="false" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<table cellpadding=0 cellspacing=0 class=Tabs>
   <tr> 
    <td class='TabFiller'>
&nbsp;
    </td>
    <td valign=middle class='Tab${selected=='mail' ? ' Selected' :''}'>
	 <a href="clv.jsp">               
       <img src="images/MailApp.gif"/>
       <span>Mail</span>
      </a>       
    </td>

    <td class='Tab${selected=='contacts' ? ' Selected' :''}'>
       <a href="contacts.jsp"><img src="images/ContactsApp.gif"/><span>Contacts</span></a>
    </td>
    <td class='Tab${selected=='calendar' ? ' Selected' :''}'>
       <img src="images/CalendarApp.gif"/>    
<span>Calendar</span>
    </td>
    <td class='TabFiller'>
&nbsp;
    </td>
   </tr>
  </table>
