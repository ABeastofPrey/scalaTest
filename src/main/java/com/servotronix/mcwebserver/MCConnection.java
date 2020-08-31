package com.servotronix.mcwebserver;

import MCJCommLib.GenDefs;
import MCJCommLib.MCJCommManager;
import static com.servotronix.mcwebserver.WebsocketErrorCode.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

public class MCConnection implements MCJCommLib.MCJCallbacks{
    
    private WebSocket ws;
    private MCJCommManager mngr;
    private int entryStationPort;
    private int wsPort;
    private boolean auth = false;
    
    private int localPort = -1; // THE PORT OF THE ENTRYSTATION CONNECTION
    
    public void setAuth() {
      this.auth = true;
    }
    public boolean isAuth() {
      return this.auth;
    }
    public int getPort() {
      return localPort;
    }
    public boolean isConnected() {
      boolean wsConn = ws != null && ws.isOpen();
      boolean esConn = mngr != null && mngr.isConnected();
      return wsConn || esConn;
    }
    
    public MCConnection(WebSocket ws, int entryStationPort) {
      mngr = new MCJCommManager(this);
      mngr.Init("mgr_" + entryStationPort, "127.0.0.1", entryStationPort, 1000);
      this.ws = ws;
      this.entryStationPort = entryStationPort;
      this.wsPort = ws.getRemoteSocketAddress().getPort();
    }
    
    public MCConnection(int entryStationPort) {
        mngr = new MCJCommManager(this);
        mngr.Init("mgr_" + entryStationPort, "127.0.0.1", entryStationPort, 1000);
        this.ws = null;
        this.entryStationPort = entryStationPort;
    }
    
    public int getWSPort() {
        return wsPort;
    }
    
    public MCJCommManager getMngr() {
      return mngr;
    }
    
    public boolean connectToMC() {
      if (mngr.Connect()) {
        localPort = mngr.getPort();
        return true;
      }
      return false;
    }
    
    public void disconnect(int reason) {
      new Thread(new Runnable(){
        @Override
        public void run() {
          if (mngr.isConnected()) {
            if (reason == WS_NORMAL_CLOSE) {
              System.out.println("?tp_exit >>> " + mngr.QueryMCSynced("?tp_exit", 0));
            }
            System.out.println("Disconnecting mngr...");
            mngr.Disconnect();
          }
        }
      },"MCConn Disconnect").start();
      if (ws != null && !ws.isClosed() && !ws.isClosing())
        ws.close(reason);
      MCConnectionManager.instance.removeMCConnection(this, false);
    }
    
    public void send(String msg) {
      if (ws!= null && ws.isOpen()) {
        ws.send(msg);
      }
    }
    
    public int getEntryStationPort() {
      return entryStationPort;
    }

    @Override
    public void OnNewBufferCallback(byte[] buffer, int filledSize) {
        
    }

    @Override
    public void OnNewMessageCAllback(String newMsg) {
        
    }

    @Override
    public void OnMultiPurpDataCallback(GenDefs.FrameTypeEnum frameType, String msg, int id) {
      if (frameType == GenDefs.FrameTypeEnum.ASYNC_FRAME)
        sendAsyncMsg(msg);
    }

    @Override
    public void OnConnectionStateChangeCallback(boolean connected) {
      if (!connected && isConnected()) { // entry station closed while connection is active
        System.out.println("ENTRYSTATION CLOSED ON PORT " + getEntryStationPort());
        disconnect(WS_ENTRYSTATION_CLOSED);
      }
    }
    
    private void sendAsyncMsg(String msg) {
      JSONObject tpMsg = new JSONObject();
      tpMsg.put("cmd_id", -1); // THIS MEANS IT'S A SERVER-ORIGINATED MESSAGE
      tpMsg.put("msg", msg);
      send(tpMsg.toString());
      //MCConnectionManager.instance.sendToAll(tpMsg.toString());
    }
    
    public boolean test() {
      boolean wsOpen = false, esOpen = mngr.isConnected(), wsNull = false;
      System.out.println("TESTING CONN " + getPort() + "...");
      wsNull = ws == null;
      System.out.println("WS NULL: " + wsNull);
      if (!wsNull) {
        wsOpen = ws.isOpen();
      }
      System.out.println("WS OPEN:" + wsOpen);
      System.out.println("ES OPEN:" + esOpen);
      boolean result = (wsOpen && esOpen || !wsOpen && !esOpen);
      System.out.println("TEST RESULT:" + result);
      System.out.println("----------------------");
      return result;
    }
    
}
