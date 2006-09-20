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
  <zm:appTabs selected='contacts'/>  

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
    <td valign=top class='Left'>
      <jsp:directive.include file="left.jsp" />
    </td>
    <!-- right -->
    <td valign='top' class='Right'>
     <div class='RightTbTop'>${toolbar}</div>

<div class='ZmContactInfoView'>

 <table cellspacing=0 cellpadding=0>
  <tr class='contactHeaderRow'>
	<td width=20><center><img src="images/Contact.gif"/></center></td>
	<td><div class='contactHeader'>${fn:escapeXml(empty contact.displayFileAs ? '<None>' : contact.displayFileAs)}</div></td>
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
       <zm:contactLine line="${contact.email}"/>
       <zm:contactLine line="${contact.email2}"/>
	   <zm:contactLine line="${contact.email3}"/>
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
            <zm:contactLine line="${contact.workStreet}"/>
            <zm:contactLine line="${contact.workCity}"/>
            <zm:contactLine line="${contact.workState}"/>
            <zm:contactLine line="${contact.workPostalCode}"/>
            <zm:contactLine line="${contact.workCountry}"/>            
            <c:if test="${!empty contact.workURL}">
              <a target=_new href="<c:url value="${contact.workURL}"/>">${fn:escapeXml(contact.workURL)}</a>
            </c:if>
         </div>
     </td>
     <td valign="top">
       <table border="0">
        <tbody>
        <zm:contactPhone label="Phone" phone="${contact.workPhone}"/>
        <zm:contactPhone label="Phone 2" phone="${contact.workPhone2}"/>       
        <zm:contactPhone label="Fax" phone="${contact.workFax}"/>
        <zm:contactPhone label="Assistant" phone="${contact.assistantPhone}"/>
        <zm:contactPhone label="Company" phone="${contact.companyPhone}"/>
        <zm:contactPhone label="Callback" phone="${contact.callbackPhone}"/>
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
            <zm:contactLine line="${contact.homeStreet}"/>
            <zm:contactLine line="${contact.homeCity}"/>
            <zm:contactLine line="${contact.homeState}"/>
            <zm:contactLine line="${contact.homePostalCode}"/>
            <zm:contactLine line="${contact.homeCountry}"/>
            <c:if test="${!empty contact.homeURL}">
              <a target=_new href="<c:url value="${contact.workURL}"/>">${fn:escapeXml(contact.homeURL)}</a>
            </c:if>
         </div>
     </td>
     <td valign="top">
       <table border="0">
        <tbody>
        <zm:contactPhone label="Phone" phone="${contact.homePhone}"/>
        <zm:contactPhone label="Phone 2" phone="${contact.homePhone2}"/>
        <zm:contactPhone label="Fax" phone="${contact.homeFax}"/>
        <zm:contactPhone label="Mobile" phone="${contact.mobilePhone}"/>
        <zm:contactPhone label="Pager" phone="${contact.pager}"/>
        <zm:contactPhone label="Car" phone="${contact.carPhone}"/>
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
            <zm:contactLine line="${contact.otherStreet}"/>
            <zm:contactLine line="${contact.otherCity}"/>
            <zm:contactLine line="${contact.otherState}"/>
            <zm:contactLine line="${contact.otherPostalCode}"/>
            <zm:contactLine line="${contact.otherCountry}"/>
            <c:if test="${!empty contact.otherURL}">
              <a target=_new href="<c:url value="${contact.workURL}"/>">${fn:escapeXml(contact.otherURL)}</a>
            </c:if>
         </div>
     </td>
     <td valign="top">
       <table border="0">
        <tbody>
         <zm:contactPhone label="Other Phone" phone="${contact.otherPhone}"/>
         <zm:contactPhone label="Other Fax" phone="${contact.otherFax}"/>
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




