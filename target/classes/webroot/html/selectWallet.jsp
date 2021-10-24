<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="parts/header.jsp" %> 
    <body>
        <canvas id="gradient-canvas" data-js-darken-top data-transition-in></canvas>
        <div class="d-flex flex-column align-items-stretch w-100 h-100">
            <%@ include file="parts/navbar.jsp" %> 
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
<%@ include file="parts/footer.jsp" %>