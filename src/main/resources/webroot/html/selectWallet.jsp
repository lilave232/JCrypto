<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta2/css/all.min.css" integrity="sha512-YWzhKL2whUzgiheMoBFwW8CKV4qpHQAEuvilg9FAn5VJUDwKZZxkJNuGM4XkWuk94WCrrwslk8yWNGmY1EduTA==" crossorigin="anonymous" referrerpolicy="no-referrer" />
    </head>
    <body>
        <canvas id="gradient-canvas" data-js-darken-top data-transition-in></canvas>
        <div class="d-flex flex-column align-items-stretch w-100 h-100">
            <!-- As a heading -->
            <nav class="navbar navbar-light ps-3">
                <span class="navbar-brand mb-0 h1 text-light">JCrpyto</span>
            </nav>

            <div class="container-fluid bg-light h-100 ps-5 pe-5 pt-2">
                <div class="container">
                    <div class="row">
                        <div class="col-6">
                            <h3 class="fw-normal text-secondary">My Wallets</h3>
                        </div>
                        <div class="col-6 text-end">
                            <div class="btn-group">
                                <button type="button" class="btn btn-secondary dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
                                  Add Wallet
                                </button>
                                <ul class="dropdown-menu dropdown-menu-end">
                                  <li><a class="dropdown-item text-secondary" href="/createWallet">Create Wallet</a></li>
                                  <li><a class="dropdown-item text-secondary" href="/importWallet">Import Wallet</a></li>
                                </ul>
                              </div>
                        </div>
                        
                    </div>
                </div>
                <div class="container pt-2">
                    <div class="d-grid gap-3">
                        <c:forEach items="${wallets}" var="wallet">
                            <div class="container p-2 bg-light rounded border" onClick="reply_click('${wallet.getName()}')">
                                <div class="row">
                                    <div class="col-6 border-end text-center"><h4 class="fw-light text-secondary">${wallet.getName()}</h4></div>
                                    <div class="col-6 text-center"><h4 class="fw-light text-secondary">${wallet.getUsableBalance()} JCR</h4></div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>
        </div>
    </body>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-ka7Sk0Gln4gmtz2MlQnikT1wXgYsOg+OMhuP+IlRH9sENBO0LRn5q+8nbTov4+1p" crossorigin="anonymous"></script>
    <script src="static/js/main.js"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js" integrity="sha256-/xUj+3OJU5yExlq6GSYGSHk7tPXikynS7ogEvDej/m4=" crossorigin="anonymous"></script>
    <script type="text/javascript">
        function reply_click(wallet)
        {
            $.ajax({
                type: "POST",
                url: "/loadWallet",
                data: {"wallet":wallet},
                dataType: "json",
                complete: function(data, textStatus) {
                    // data.redirect contains the string URL to redirect to
                    window.location.href = "/main";
                }
            });
        }
      </script>
</html>