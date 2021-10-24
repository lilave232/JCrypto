<%@ include file="parts/header.jsp" %> 
    <body>
        <canvas id="gradient-canvas" data-js-darken-top data-transition-in></canvas>
        <div class="container text-center text-light position-absolute top-50 start-50 translate-middle">
            <form action="/" method="post"> 
                <h1>Welcome to JCrpyto Peer!</h1>
                <h2>First Step What Would You Like To:</h2>
                <input class="btn btn-outline-light m-2 fs-3" type="submit" name="action" value="Create Folder" />
                <br>
                <input class="btn btn-outline-light m-2 fs-3" type="submit" name="action" value="Load Folder" />
            </form>
        </div>
    </body>
<%@ include file="parts/footer.jsp" %> 