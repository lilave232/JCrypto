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
                            <button class="nav-link text-secondary active" id="owned-tab" data-bs-toggle="tab" data-bs-target="#owned" type="button" role="tab" aria-controls="owned aria-selected="true">Owned</button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link text-secondary" id="mint-tab" data-bs-toggle="tab" data-bs-target="#mint" type="button" role="tab" aria-controls="send" aria-selected="false">Mint</button>
                        </li>
                    </ul>
                    <div class="tab-content" id="myTabContent">
                        <div class="tab-pane show active text-start rounded-bottom bg-white border-start border-end border-bottom p-3" id="owned" role="tabpanel" aria-labelledby="owned-tab">
                            <c:forEach items="${nfts}" var="nft">
                                <div class="container border text-center p-3">
                                    <div class="d-flex align-items-center justify-content-center">
                                        <img class="img-fluid w-50 h-50" src="${nft.getBase64()}"/>
                                    </div>
                                    <h2 class="m-0">${nft.getTitle()}</h2>
                                    <h4 class="m-0">${nft.getDescription()}</h4>
                                    <p class="m-0">${nft.getInceptionDateFormatted()}</p>
                                </div>
                            </c:forEach>
                        </div>
                        <div class="tab-pane fade text-start rounded-bottom bg-white border-start border-end border-bottom p-3" id="mint" role="tabpanel" aria-labelledby="mint-tab">
                            <form action="/nft" method="POST"  enctype="multipart/form-data">
                                <div class="d-flex align-items-center">
                                    <label for="title">Title: </label>
                                    <input class="btn m-2 fs-6 text-start border-secondary flex-fill" placeholder="Title" id="title" type="text" name="title" required>
                                </div>
                                <div class="d-flex align-items-center">
                                    <label for="description">Description: </label>
                                    <input class="btn m-2 fs-6 text-start border-secondary flex-fill" placeholder="Description" id="description" type="text" name="description" required>
                                </div>
                                <div class="d-flex align-items-center">
                                    <label for="file">File: </label>
                                    <input class="btn m-2 fs-6 text-start border-secondary flex-fill" id="file" type="file" name="file" accept="image/*" onchange="loadFile(event)" required>
                                </div>
                                <div class="d-flex align-items-center justify-content-center">
                                    <img id="output" class="img-fluid w-50 h-50"/>
                                    <script>
                                        var loadFile = function(event) {
                                          var output = document.getElementById('output');
                                          output.src = URL.createObjectURL(event.target.files[0]);
                                          output.onload = function() {
                                            URL.revokeObjectURL(output.src) // free memory
                                          }
                                        };
                                    </script>
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
                                    <input class="btn btn-secondary m-2 fs-3 text-center" type="submit" value="Mint"/>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </body>
    </body>
<%@ include file="parts/footer.jsp" %> 