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
        <script src="https://cdnjs.cloudflare.com/ajax/libs/qrious/4.0.2/qrious.min.js"></script>
    </head>
    <body>
        <canvas id="gradient-canvas" data-js-darken-top data-transition-in></canvas>
        <body>
            <canvas id="gradient-canvas" data-js-darken-top data-transition-in></canvas>
            <div class="d-flex align-items-stretch flex-column vh-100">
                <nav class="navbar navbar-expand-lg">
                    <div class="container-fluid">
                        <a class="navbar-brand" href="#">Navbar</a>
                        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
                            <span class="navbar-toggler-icon"></span>
                        </button>
                        <div class="collapse navbar-collapse" id="navbarSupportedContent">
                            <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                            <li class="nav-item">
                                <a class="nav-link active" aria-current="page" href="#">Home</a>
                            </li>
                            <li class="nav-item">
                                <a class="nav-link" href="#">Link</a>
                            </li>
                            <li class="nav-item dropdown">
                                <a class="nav-link dropdown-toggle" href="#" id="navbarDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                                Dropdown
                                </a>
                                <ul class="dropdown-menu" aria-labelledby="navbarDropdown">
                                <li><a class="dropdown-item" href="#">Action</a></li>
                                <li><a class="dropdown-item" href="#">Another action</a></li>
                                <li><hr class="dropdown-divider"></li>
                                <li><a class="dropdown-item" href="#">Something else here</a></li>
                                </ul>
                            </li>
                            <li class="nav-item">
                                <a class="nav-link disabled">Disabled</a>
                            </li>
                            </ul>
                        </div>
                    </div>
                </nav>
                <div class="container-fluid text-center text-secondary h-100 bg-light ps-5 pe-5 pt-3">
                    <a href="/selectWallet" class="text-start text-secondary"><h3 class="fw-normal"><i class="fas fa-arrow-left"></i>Back to Wallets</h3></a>
                    <h2 class="text-start fw-normal">${wallet.getName()} | ${wallet.getUsableBalance()} JCR</h2>
                    <ul class="nav nav-tabs" id="myTab" role="tablist">
                        <li class="nav-item" role="presentation">
                            <button class="nav-link text-secondary active" id="balances-tab" data-bs-toggle="tab" data-bs-target="#balances" type="button" role="tab" aria-controls="balances" aria-selected="true">Balances</button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link text-secondary" id="send-tab" data-bs-toggle="tab" data-bs-target="#send" type="button" role="tab" aria-controls="send" aria-selected="false">Send</button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link text-secondary" id="receive-tab" data-bs-toggle="tab" data-bs-target="#receive" type="button" role="tab" aria-controls="receive" aria-selected="false">Receive</button>
                        </li>
                    </ul>
                    <div class="tab-content" id="myTabContent">
                    <div class="tab-pane fade show active text-start rounded-bottom bg-white border-start border-end border-bottom p-3" id="balances" role="tabpanel" aria-labelledby="balances-tab">
                        <h3 class="text-start fw-light">Usable Balance: ${wallet.getUsableBalance()} JCR</h3>
                        <h3 class="text-start fw-light">Borrowed Balance: ${wallet.getBorrowedBalance()} JCR</h3>
                        <h3 class="text-start fw-light">Penalties Incurred: ${wallet.getPenaltyBalance()} JCR</h3>
                        <h3 class="text-start fw-light">Net Borrowed: ${wallet.getBorrowedBalance() - wallet.getPenaltyBalance()} JCR</h3>
                        <h3 class="text-start fw-light">Lent Balance: ${wallet.getLentBalance()} JCR</h3>
                    </div>
                    <div class="tab-pane fade text-start rounded-bottom bg-white border-start border-end border-bottom p-3" id="send" role="tabpanel" aria-labelledby="send-tab">
                        <form action="/sendTxn" method="POST">
                            <div class="d-flex align-items-center">
                                <label for="receiver">Receiver Address: </label>
                                <input class="btn m-2 fs-6 text-start border-secondary flex-fill" placeholder="Receiver Address" id="receiver" type="text" name="receiver" required>
                            </div>
                            <div class="d-flex align-items-center">
                                <label for="amount">Amount: </label>
                                <input class="btn m-2 fs-6 text-start border-secondary flex-fill" placeholder="Amount" id="amount" type="number" step="any" name="amount" required>
                            </div>
                            <div class="d-flex align-items-center">
                                <label for="pword">Wallet Password: </label>
                                <input class="btn m-2 fs-6 text-start border-secondary flex-fill" placeholder="Password" id="pword" type="password" name="pword" required>
                            </div>
                            <div class="w-100 text-center">
                                <input class="btn btn-secondary m-2 fs-3 text-center" type="submit" value="Send"/>
                            </div>
                        </form>
                    </div>
                    <div class="tab-pane fade text-start rounded-bottom bg-white border-start border-end border-bottom p-3" id="receive" role="tabpanel" aria-labelledby="receive-tab">
                        <h4>Your Wallet Address</h4>
                        <p>${wallet.getAddress()}</p>
                        <p>Share this wallet address to receive payments.</p>
                        <h4>QR Code</h4>
                        <div class="w-100 text-center">
                            <canvas id="qrcode"></canvas>
                        </div>
                        <script type="text/javascript">
                            var qrcode = new QRious({
                              element: document.getElementById("qrcode"),
                              background: '#ffffff',
                              backgroundAlpha: 1,
                              foreground: '#000000',
                              foregroundAlpha: 1,
                              level: 'H',
                              padding: 0,
                              size: 256,
                              value: "${wallet.getAddress()}"
                            });
                        </script>
                    </div>
                    </div>
                </div>
            </div>
        </body>
    </body>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-ka7Sk0Gln4gmtz2MlQnikT1wXgYsOg+OMhuP+IlRH9sENBO0LRn5q+8nbTov4+1p" crossorigin="anonymous"></script>
    <script src="static/js/main.js"></script>
</html>