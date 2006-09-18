<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<body>
  <zm:hello/>
  <hr>
  My name is : ${param.hello}<br>
  My tags are: ${1+2}<br>
  <zm:forEachTag var="tag">
    name: ${tagName}, id: ${tagId},    color: <c:out value="${tagColor}" /><br>
  </zm:forEachTag>
</body>
</html>
