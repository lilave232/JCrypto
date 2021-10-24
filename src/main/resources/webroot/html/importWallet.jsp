<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="parts/header.jsp" %> 
    <body>
        <canvas id="gradient-canvas" data-js-darken-top data-transition-in></canvas>
        <div class="container text-center text-light position-absolute top-50 start-50 translate-middle">
            <form action="/importWallet" method="post"> 
                <h1>Welcome to JCrpyto Peer!</h1>
                <label>Wallet Name: </label>
                <input class="btn btn-outline-light m-2 fs-3" type="text" name="name" required>
                <br>
                <label>Wallet Mnemonic: </label>
                <input class="btn btn-outline-light m-2 fs-3" type="text" name="mnemonic" required>
                <br>
                <label>Wallet Password: </label>
                <input class="btn btn-outline-light m-2 fs-3" type="password" name="pword" id="password" required>
                <br>
                <label>Confirm Wallet Password: </label>
                <input class="btn btn-outline-light m-2 fs-3" type="password" id="confirm_password" required>
                <br>
                <input class="btn btn-outline-light m-2 fs-3" type="submit" name="action" value="Import Wallet" />
            </form>
        </div>
    </body>
    <script>
        var password = document.getElementById("password"), confirm_password = document.getElementById("confirm_password");

        function validatePassword(){
        if(password.value != confirm_password.value) {
            confirm_password.setCustomValidity("Passwords Don't Match");
        } else {
            confirm_password.setCustomValidity('');
        }
        }

        password.onchange = validatePassword;
        confirm_password.onkeyup = validatePassword;
    </script>
<%@ include file="parts/footer.jsp" %> 