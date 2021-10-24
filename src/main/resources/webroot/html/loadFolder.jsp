<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="parts/header.jsp" %> 
    <body>
        <canvas id="gradient-canvas" data-js-darken-top data-transition-in></canvas>
        <div class="container text-center text-light position-absolute top-50 start-50 translate-middle">
            <form action="/loadFolder" method="post"> 
                <h1>Welcome to JCrpyto Peer!</h1>
                <h2>Select Folder:</h2>
                <c:forEach items="${chainFolders}" var="folder">
                    <input class="btn btn-outline-light m-2 fs-3" type="submit" name="folder" value="${folder}" />
                </c:forEach>
            </form>
        </div>
    </body>
<%@ include file="parts/footer.jsp" %> 