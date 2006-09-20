<%@ tag body-content="empty" %>
<%@ attribute name="line" rtexprvalue="true" required="true"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:if test="${!empty line}">${fn:escapeXml(line)}<br/></c:if>
