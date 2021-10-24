<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="parts/header.jsp" %> 
    <body>
        <canvas id="gradient-canvas" data-js-darken-top data-transition-in></canvas>
        <div class="d-flex flex-column align-items-stretch w-100 h-100">
            <%@ include file="parts/navbar_1.jsp" %> 
            <div class="container-fluid bg-light h-100 ps-5 pe-5 pt-2">
                <h3 class="text-secondary">Connection Settings</h3>
                <c:choose>
                    <c:when test="${peerConnected}">
                        <div class="form-check form-switch">
                            <div class="row align-items-center">
                                <div class="col-auto">
                                    <label for="address">Host Address:</label>
                                </div>
                                <div class="col-auto">
                                  <input type="text" readonly class="form-control" id="address" value="${localAddress}">
                                </div>
                                <div class="col-auto">
                                    <label for="port">Port:</label>
                                </div>
                                <div class="col-auto">
                                  <input type="text" readonly class="form-control" id="port" value="${peerPort}">
                                </div>
                                <div class="col-auto">
                                    <button type="submit" class="btn btn-primary" onclick="setDisconnected()">Stop</button>
                                </div>
                            </div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="container">
                            <div class="row align-items-center">
                                <div class="col-auto">
                                    <label for="address">Host Address:</label>
                                </div>
                                <div class="col-auto">
                                  <input type="text" readonly class="form-control" id="address" value="${localAddress}">
                                </div>
                                <div class="col-auto">
                                    <label for="port">Port:</label>
                                </div>
                                <div class="col-auto">
                                  <input type="text" class="form-control" id="port" value="7777" required>
                                </div>
                                <div class="col-auto">
                                    <c:choose>
                                        <c:when test="${walletActive}">
                                            <button type="submit" class="btn btn-primary" onclick="setConnected()">Start</button>
                                        </c:when>
                                        <c:otherwise>
                                            <button type="submit" disabled class="btn btn-primary" onclick="setConnected()">Start</button>
                                            <h3 class="Must Select Wallet First Go Back To Wallets and Click!"></h3>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
                <h3 class="text-secondary">Outgoing Threads</h3>
                <div class="container border rounded">
                    <c:forEach items="${outgoingThreads}" var="thread">
                        <div class="row">
                            <div class="col-6 border-end">
                                ${thread.getHostAddress()}
                            </div>
                            <div class="col-6">
                                ${thread.getPort()}
                            </div>
                        </div>
                    </c:forEach>
                </div>
                <h3 class="text-secondary">Incoming Threads</h3>
                <div class="container border rounded">
                    <c:forEach items="${incomingThreads}" var="thread">
                        <div class="row">
                            <div class="col-6 border-end">
                                ${thread.getSocket().getInetAddress().getHostName()}
                            </div>
                            <div class="col-6">
                                ${thread.getSocket().getPort()}
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </div>
    </body>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js" integrity="sha256-/xUj+3OJU5yExlq6GSYGSHk7tPXikynS7ogEvDej/m4=" crossorigin="anonymous"></script>
    <script type="text/javascript">
        function setConnected()
        {
            $.ajax({
                type: "POST",
                url: "/peer",
                data: {"action":"Connect Peer", "port":$('#port').val()},
                dataType: "json",
                complete: function(data, textStatus) {
                    // data.redirect contains the string URL to redirect to
                    window.location.href = "/peer";
                }
            });
        }

        function setDisconnected()
        {
            $.ajax({
                type: "POST",
                url: "/peer",
                data: {"action":"Disconnect Peer"},
                dataType: "json",
                complete: function(data, textStatus) {
                    // data.redirect contains the string URL to redirect to
                    window.location.href = "/peer";
                }
            });
        }
    </script>
<%@ include file="parts/footer.jsp" %>