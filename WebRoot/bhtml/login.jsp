<%@ page buffer="8kb" autoFlush="true" %>
<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:if test="${!(empty param.username) && !(empty param.password)}">
  <c:catch var="loginException">
    <zm:login username="${param.username}" password="${param.password}" rememberme="${param.rememberme == 'on'}"/>
    <jsp:forward page="clv.jsp"/>
  </c:catch>
  <c:if test="${loginException != null}">
  	<zm:getException var="error" exception="${loginException}"/>
  </c:if>
</c:if>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
 <head>
  <title>Zimbra</title>
  <style type="text/css">
    @import url("style.css");
  </style>
 </head>
 <body>
   <table width=100% height=100%>
    <tr>
     <td align=center valign=middle>
       <div id="ZloginPanel">
         <table>
          <tr>
           <td>
            <table>
             <tr>           
              <td align=center valign=middle>          
               <img src="images/LoginBanner.png"/>
              </td>
             </tr>
             <tr>
              <td>
                <div id='ZLoginAppName'>Collaboration Suite</div>
              </td>
             </tr>
            </table>
           </td>
           <td>
          </tr>
          <tr> 
           <td id='ZloginBodyContainer'>
             <c:if test="${error != null}">
             <div id='ZloginErrorPanel'>
              <table>
               <tr>
                <td valign='top' width='40'>
                  <img src="images/Critical_32.gif"/>
                </td>
                <td class='errorText'>
                  ${error.displayMessage}
                </td>
              </tr>
              </table>
             </div>
             </c:if>           
           
             <div id='ZloginFormPanel'>
              <form method='post'>
              <table cellpadding=4>
               <tr>
                 <td class='zLoginLabelContainer'>Username:</td>
                 <td colspan=2 class='zLoginFieldContainer'>
                   <input class='zLoginField' name='username' type='text' autocomplete='OFF' value='${fn:escapeXml(param.username)}'></input>
                 </td>
               </tr>
               <tr>
                 <td class='zLoginLabelContainer'>Password:</td>
                 <td colspan=2 class='zLoginFieldContainer'>
                    <input class='zLoginField' name='password' type='password' autocomplete='OFF'></input>
                 </td>
               </tr>
               <tr>
                 <td class='zLoginLabelContainer'></td>
                 <td>
                  <table>
                   <tr>
                    <td><input type=checkbox name='rememberme'></input></td>
                    <td class='zLoginCheckboxLabelContainer'>Remember me on this computer</td>                    
                   </tr>                   
                  </table>                 
                 </td>
                 <td><input type=submit class='zLoginButton' value="Log In"></input></td>
               </tr>               
              </table>
              <table>
                <tr>
                 <td id='ZloginLicenseContainer'>
                   Copyright &copy;2006 Zimbra, Inc. 'Zimbra' and the Zimbra logos are trademarks of Zimbra, Inc.
                  </td>
                </tr>
              </table>              
             </div>
           </td>
          </tr>
         </table>
      </form>         
       </div>
     </td>
    </tr>
   </table>
 </body>
</html>