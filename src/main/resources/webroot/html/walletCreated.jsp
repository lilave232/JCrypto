<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="parts/header.jsp" %> 
    <body>
        <canvas id="gradient-canvas" data-js-darken-top data-transition-in></canvas>
        <div class="container text-center text-light position-absolute top-50 start-50 translate-middle">
            <h2>Wallet Mnemonic is: ${mnemonic}</h2>
            <h4>The only way to recover your password is with this mnemonic</h4>
            <h4>Please remember to securely store your mnemonic offline!</h4>
            <h2><a href="/main"><i class="fas fa-arrow-right"></i></a></h2>
        </div>
    </body>
<%@ include file="parts/footer.jsp" %>