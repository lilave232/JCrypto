<html>
    <head>
        <title>Stripe Gradient</title>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-1BmE4kWBq78iYhFldvKuhfTAU6auU8tT94WrHftjDbrCEXSU1oBoqyl2QvZ6jIW3" crossorigin="anonymous">
        <link rel="stylesheet" href="static/css/background.css">
        <link rel="stylesheet" href="static/css/main.css">
        <link rel="preconnect" href="https://fonts.googleapis.com">
        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
        <link href="https://fonts.googleapis.com/css2?family=Ubuntu:wght@300;400;500;700&display=swap" rel="stylesheet">
    </head>
    <body>
        <canvas id="gradient-canvas" data-js-darken-top data-transition-in></canvas>
        <div class="container text-center text-light position-absolute top-50 start-50 translate-middle">
            <form action="/walletCreated" method="post"> 
                <h1>Welcome to JCrpyto Peer!</h1>
                <label>Wallet Name: </label>
                <input class="btn btn-outline-light m-2 fs-3" type="text" name="name" required>
                <br>
                <label>Wallet Password: </label>
                <input class="btn btn-outline-light m-2 fs-3" type="password" name="pword" id="password" required>
                <br>
                <label>Confirm Wallet Password: </label>
                <input class="btn btn-outline-light m-2 fs-3" type="password" id="confirm_password" required>
                <br>
                <input class="btn btn-outline-light m-2 fs-3" type="submit" name="action" value="Create Wallet" />
            </form>
        </div>
    </body>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-ka7Sk0Gln4gmtz2MlQnikT1wXgYsOg+OMhuP+IlRH9sENBO0LRn5q+8nbTov4+1p" crossorigin="anonymous"></script>
    <script src="static/js/main.js"></script>
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
</html>