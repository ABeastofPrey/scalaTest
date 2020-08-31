var globalConn = new MCConnection();
var worker = this;
var intervals = [];

onmessage = function(e) {
  if (e.data.serverMsg) {
    switch (e.data.msg) {
      case 0: // CONNECT REQUEST
        globalConn.connect();
        break;
      case 1: // RESET REQUEST
        globalConn.reset();
        break;
      case 2: // STOP INTERVAL
        if (e.data.id)
          intervals[e.data.id] = null;
        break;
    }
    return;
  }
  if (e.data.interval) { // INTERVAL
    intervals[e.data.id] = true;
    timer(e.data.id, e.data.interval, e.data.msg);
  } else
    globalConn.sendData(e.data.msg);
}

function timer(id, interval, msg) {
  var now = new Date().getTime();
  var timeout;
  if (intervals[id]) {
    timeout = setTimeout(function(){
      timer(id, interval, msg);
    },interval);
    globalConn.sendData(msg);
    if (new Date().getTime() - now > interval) {
      clearTimeout(timeout);
      setTimeout(function(){
        timer(id, interval);
      },interval);
    }
  } else {
    clearTimeout(timeout);
  }
}

function MCConnection() {
  
  var conn = this;
  
  // Private Variables
  var ws = null;
  var MCPORT = 3010;
  var IP = self.location.hostname;
  //var IP = '10.4.20.65';
  var reset = true;
  
  this.connect = function(){
    //Initiate a websocket connection
    try {
      ws = new WebSocket("ws://" + IP + ":" + MCPORT);
      ws.onopen = function(){
        setTimeout(()=>{
          worker.postMessage({serverMsg:true,msg:0}); // ONOPEN MESSAGE
        },100);
        console.log("WEBSOCKET OPENED");
        reset = false;
      };
      ws.onerror = function(e) {
        worker.postMessage({serverMsg:true,msg:1}); // ONERROR MESSAGE
        console.log("WEBSOCKET ERROR: ");
        console.log(e);
      };
      ws.onclose = function(event) {
        worker.postMessage({serverMsg:true,msg:2,clean:event.wasClean}); // ONCLOSE MESSAGE
        console.log("WEBSOCKET CLOSED");
      };
      ws.onmessage = function (msg) {
        worker.postMessage({serverMsg:false,msg:msg.data});
      };
      setTimeout(function(){
        if (ws && ws.readyState !== ws.OPEN) {
          ws.close();
          worker.postMessage({serverMsg:true,msg:3}); // TIMEOUT MESSAGE
        }
      },5000);
    } catch (err) {
      console.log(err.message);
    }
  };

  this.sendData = function(jsonData){
    if (ws && ws.readyState === ws.OPEN) {
      try {
        ws.send(jsonData);
      } catch (err) {
        console.log(err);
        alert('MC Connection terminated unexpectedly, trying to reconnect...');
        conn.connect();
      }
    }
  };
  
  this.reset = function() {
    if (reset)
      return;
    console.log('RESET CONNECTION');
    for (var interval in intervals) {
      intervals[interval] = false;
      intervalCounter = 0;
    }
    if (ws !== null)
      ws.close();
    ws = null;
    reset = true;
  };
  
  this.startLBN = function() {
    
  };

}
