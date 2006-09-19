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

  <! --------- TOP ------>
  <jsp:directive.include file="top.jsp" />

  <! --------- TABS ------>
  <table cellspacing=0 class=Tabs>
   <tr> 
    <td class='TabFiller'>
&nbsp;
    </td>
    <td class='Tab'>
<a href="clv.jsp">Mail</a>
    </td>

    <td class='TabSelected'>

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


<c:set var="toolbar">
 <table cellspacing=0 class='Tb'>
   <tr>
<td align=left class=TbBt>
 <a href="">Edit</a>
 <a href="">Delete</a>
 <a href="">Print</a>
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

<div class='ZmContactInfoView'>

 <table cellspacing=0 cellpadding=0>
  <tr class='contactHeaderRow'>
	<td width=20><center><img src="images/Contact.gif"/></center></td>
	<td><div class='contactHeader'>${fn:escapeXml(contact.displayFileAs)}</div></td>
	<td align='right'><!-- tags--> </td>
  </tr>
 </table>


 <table border="0" cellpadding="2" cellspacing="2" width="100%"><tbody>
   <tr>

     <td class="companyName" width="100%">
       <c:if test="${zm:anySet(contact,'jobTitle company')}">
          ${fn:escapeXml(contact.jobTitle)}
          <c:if test="${!((empty contact.jobTitle) or (empty contact.company))}">,</c:if>
          ${fn:escapeXml(contact.company)}
       </c:if>           
     </td><td width="20"><img src="images/ContactsFolder.gif"></div></td><td class="companyFolder">Contacts</td>
   </tr>
 </tbody></table>

 <table border="0" cellpadding="3" cellspacing="3" width="100%">
  <tbody>
  
  <c:if test="${zm:anySet(contact,'email email2 email3')}">
   <tr><td colspan="4" class="sectionLabel" valign="top">Email</td></tr>
   <tr>
     <td width="5">&nbsp;</td>
     <td class="contactOutput">
        <c:if test="${!empty contact.email}">${fn:escapeXml(contact.email)}<br/></c:if>
        <c:if test="${!empty contact.email2}">${fn:escapeXml(contact.email2)}<br/></c:if>
        <c:if test="${!empty contact.email3}">${fn:escapeXml(contact.email3)}<br/></c:if>
     </td>
   </tr>
   <tr><td><br></td></tr>
  </c:if>

  <c:if test="${zm:anySet(contact,'workStreet workCity workState workPostalCode workCountry workURL workPhone workPhone2 workFax assistantPhone companyPhone callbackPhone')}">
   <tr><td colspan="4" class="sectionLabel" valign="top">Work</td></tr>
   <tr>
     <td width="5">&nbsp;</td>
     <td valign="top" width="385">
         <div class="contactOutput">
            <c:if test="${!empty contact.workStreet}">${fn:escapeXml(contact.workStreet)}<br/></c:if>
            <c:if test="${!empty contact.workCity}">${fn:escapeXml(contact.workCity)}<br/></c:if>
            <c:if test="${!empty contact.workState}">${fn:escapeXml(contact.workState)}<br/></c:if>
            <c:if test="${!empty contact.workPostalCode}">${fn:escapeXml(contact.workPostalCode)}<br/></c:if>            
            <c:if test="${!empty contact.workCountry}">${fn:escapeXml(contact.workCountry)}<br/></c:if>                        
            <c:if test="${!empty contact.workURL}">
              <a target=_new href="<c:url value="${contact.workURL}"/>">${fn:escapeXml(contact.workURL)}</a>
            </c:if>
         </div>
     </td>
     <td valign="top">
       <table border="0">
        <tbody>
         <c:if test="${!empty contact.workPhone}">
          <tr><td class="contactLabel">Phone:</td><td class="contactOutput">${fn:escapeXml(contact.workPhone)}</td></tr>
         </c:if>
         <c:if test="${!empty contact.workPhone2}">
          <tr><td class="contactLabel">Phone 2:</td><td class="contactOutput">${fn:escapeXml(contact.workPhone2)}</td></tr>
         </c:if>         
         <c:if test="${!empty contact.workFax}">
          <tr><td class="contactLabel">Fax:</td><td class="contactOutput">${fn:escapeXml(contact.workFax)}</td></tr>
         </c:if>         
         <c:if test="${!empty contact.assistantPhone}">
          <tr><td class="contactLabel">Assistant:</td><td class="contactOutput">${fn:escapeXml(contact.assistantPhone)}</td></tr>
         </c:if>         
         <c:if test="${!empty contact.companyPhone}">
          <tr><td class="contactLabel">Company:</td><td class="contactOutput">${fn:escapeXml(contact.companyPhone)}</td></tr>
         </c:if>
         <c:if test="${!empty contact.callbackPhone}">
          <tr><td class="contactLabel">Callback:</td><td class="contactOutput">${fn:escapeXml(contact.callbackPhone)}</td></tr>
         </c:if>
        </tbody>
       </table>
     </td>
   </tr>
   <tr>
     <td><br></td>
   </tr>
  </c:if>
   
   
  <c:if test="${zm:anySet(contact,'homeStreet homeCity homeState homePostalCode homeCountry homeURL homePhone homePhone2 homeFax mobilePhone pager carPhone')}">
   <tr>
     <td colspan="4" class="sectionLabel" valign="top">Home</td>
   </tr>
   <tr>
     <td width="5">&nbsp;</td>
     <td valign="top" width="385">
         <div class="contactOutput">
            <c:if test="${!empty contact.homeStreet}">${fn:escapeXml(contact.homeStreet)}<br/></c:if>
            <c:if test="${!empty contact.homeCity}">${fn:escapeXml(contact.homeCity)}<br/></c:if>
            <c:if test="${!empty contact.homeState}">${fn:escapeXml(contact.homeState)}<br/></c:if>
            <c:if test="${!empty contact.homePostalCode}">${fn:escapeXml(contact.homePostalCode)}<br/></c:if>
            <c:if test="${!empty contact.homeCountry}">${fn:escapeXml(contact.homeCountry)}<br/></c:if>
            <c:if test="${!empty contact.homeURL}">
              <a target=_new href="<c:url value="${contact.workURL}"/>">${fn:escapeXml(contact.homeURL)}</a>
            </c:if>
         </div>
     </td>
     <td valign="top">
       <table border="0">
         <tbody>
         <c:if test="${!empty contact.homePhone}">
          <tr><td class="contactLabel">Phone:</td><td class="contactOutput">${fn:escapeXml(contact.homePhone)}</td></tr>
         </c:if>
         <c:if test="${!empty contact.homePhone2}">
          <tr><td class="contactLabel">Phone 2:</td><td class="contactOutput">${fn:escapeXml(contact.homePhone2)}</td></tr>
         </c:if>         
         <c:if test="${!empty contact.homeFax}">
          <tr><td class="contactLabel">Fax:</td><td class="contactOutput">${fn:escapeXml(contact.homeFax)}</td></tr>
         </c:if>         
         <c:if test="${!empty contact.mobilePhone}">
          <tr><td class="contactLabel">Mobile:</td><td class="contactOutput">${fn:escapeXml(contact.mobilePhone)}</td></tr>
         </c:if>         
         <c:if test="${!empty contact.pager}">
          <tr><td class="contactLabel">Pager:</td><td class="contactOutput">${fn:escapeXml(contact.pager)}</td></tr>
         </c:if>
         <c:if test="${!empty contact.carPhone}">
          <tr><td class="contactLabel">Car:</td><td class="contactOutput">${fn:escapeXml(contact.carPhone)}</td></tr>
         </c:if>
         </tbody>
       </table>
     </td>
   </tr>
   <tr>
     <td><br></td>
   </tr>
  </c:if>
   
  <c:if test="${zm:anySet(contact,'otherStreet otherCity otherState otherPostalCode otherCountry otherURL otherPhone otherFax')}">
   <tr>
     <td colspan="4" class="sectionLabel" valign="top">Other</td>
   </tr>
   <tr>   
     <td width="5">&nbsp;</td>
     <td valign="top" width="385">
         <div class="contactOutput">
            <c:if test="${!empty contact.otherStreet}">${fn:escapeXml(contact.otherStreet)}<br/></c:if>
            <c:if test="${!empty contact.otherCity}">${fn:escapeXml(contact.otherCity)}<br/></c:if>
            <c:if test="${!empty contact.otherState}">${fn:escapeXml(contact.otherState)}<br/></c:if>
            <c:if test="${!empty contact.otherPostalCode}">${fn:escapeXml(contact.otherPostalCode)}<br/></c:if>
            <c:if test="${!empty contact.otherCountry}">${fn:escapeXml(contact.otherCountry)}<br/></c:if>
            <c:if test="${!empty contact.otherURL}">
              <a target=_new href="<c:url value="${contact.workURL}"/>">${fn:escapeXml(contact.otherURL)}</a>
            </c:if>
         </div>
     </td>
     <td valign="top">
       <table border="0">
         <tbody>
         <c:if test="${!empty contact.otherPhone}">
          <tr><td class="contactLabel">Other Phone:</td><td class="contactOutput">${fn:escapeXml(contact.otherPhone)}</td></tr>
         </c:if>
         <c:if test="${!empty contact.otherFax}">
          <tr><td class="contactLabel">Other Fax:</td><td class="contactOutput">${fn:escapeXml(contact.otherFax)}</td></tr>
         </c:if>         
         </tbody>
       </table>
     </td>
   </tr>
   <tr>
     <td><br></td>
   </tr>
  </c:if>
   
  <c:if test="${!empty contact.notes}">
   <tr>
     <td colspan="4" class="sectionLabel" valign="top">Notes</td>
   </tr>
   <tr>
     <td colspan="4" class="contactOutput">${fn:escapeXml(contact.notes)}<br><br></td>
   </tr>
   </c:if>
   </tbody>
  </table>
 </div>

<div class='RightTbBottom'>${toolbar}</div>
<jsp:directive.include file="footer.jsp" />

    </td>
   </tr>
  </table>

 </body>

</html>




