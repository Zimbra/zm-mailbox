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
             <div id='ZloginFormPanel'>
              <form method='post'>
              <table cellpadding=4>
               <tr>
                 <td class='zLoginLabelContainer'>Username:</td>
                 <td colspan=2 class='zLoginFieldContainer'>
                        <input class='zLoginField' name='username' type='text' autocomplete='OFF'></input>
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
                    <td><input type=checkbox></input></td>
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