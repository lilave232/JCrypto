<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="parts/header.jsp" %> 
    <body>
        <canvas id="gradient-canvas" data-js-darken-top data-transition-in></canvas>
        <body>
            <canvas id="gradient-canvas" data-js-darken-top data-transition-in></canvas>
            <div class="d-flex align-items-stretch flex-column vh-100">
                <%@ include file="parts/navbar_1.jsp" %> 
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
                        <h3 class="text-start fw-light">Previous Transactions</h3>
                        <div class="container-fluid border rounded pt-3 pb-3">
                            <div class="row">
                                <div class="col-1 border-end text-center"><p class="fw-light text-secondary">Sent/Received</p></div>
                                <div class="col-2 border-end text-center"><p class="fw-light text-secondary">Date</p></div>
                                <div class="col-7 border-end text-center"><p class="fw-light text-secondary">Transaction ID</p></div>
                                <div class="col-2 border-end text-center"><p class="fw-light text-secondary">Amount</p></div>
                            </div>
                            <c:forEach items="${transactions}" var="transaction">
                                <div class="row">
                                    <c:choose> 
                                        <c:when test="${transaction.getType() == 0}">
                                            <div class="col-1 border-end text-center"><h4 class="fw-light text-success"><i class="fas fa-sign-in-alt"></i></h4></div>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="col-1 border-end text-center"><h4 class="fw-light text-danger"><i class="fas fa-sign-out-alt"></i></h4></div>
                                        </c:otherwise>
                                    </c:choose>
                                    <div class="col-2 border-end text-center"><p class="fw-light text-secondary">${transaction.getDateToString()}</p></div>
                                    <div class="col-7 border-end text-center"><p class="fw-light text-secondary text-truncate">${transaction.getHash()}</p></div>
                                    <div class="col-2 text-center"><p class="fw-light text-secondary">${transaction.getAmount()} JCR</p></div>
                                </div>
                            </c:forEach>
                        </div>
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
                                <label for="fee">Fee: </label>
                                <input class="btn m-2 fs-6 text-start border-secondary flex-fill" placeholder="Fee" id="fee" type="number" step="any" name="fee" required>
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
<%@ include file="parts/footer.jsp" %> 