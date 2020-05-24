<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %><%@
taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %><%@
taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %><%@
taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib prefix="clay" uri="http://liferay.com/tld/clay" %>

<liferay-theme:defineObjects />

<portlet:defineObjects />



<span data-toggle="tooltip" data-placement="bottom" data-html="true" title='<div><h3>${quotainuse}</h3>
<div>
<c:forEach var = "line" items = "${lines}">
         <c:if test="${line.enabled}">
             <div>
             <b>${line.label}: </b> <span><span>${line.used}</span> / <span>${line.total}</span></span>
             </div>
         </c:if>
</c:forEach>
</div>
</div>
'>
<clay:icon symbol="repository" />
</span>

<style>
</style>

<script>
AUI().ready(function(){
$(function () {
  $('[data-toggle="tooltip"]').tooltip()
})
});
</script>