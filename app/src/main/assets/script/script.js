console.log("robotWebUi.js : server IP= " + document.location.host);
var sock = new WebSocket('ws://' + document.location.host, "protocolOne");

sock.onopen = function(event) {
    console.log("I am connected to server.....");
};

sock.onmessage = function(event) {
    console.log("on message" + event.data);
    document.getElementById('TEXT').innerHTML = event.data;
}

sock.onerror = function(error) {
    console.log('WebSocket Error', error);
};

function send(message) {
    console.log('WebSocket try to send', message);
    sock.send(message);
}