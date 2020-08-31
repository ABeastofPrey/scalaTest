/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MCJCommLib;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;


public class MCJSocketClient {

    final int BUFFERSIZE = 1024 * 20;
    byte[] buffer = new byte[BUFFERSIZE];
    Socket socket = null;
    DataInputStream inStream = null;
    DataOutputStream outStream = null;
    boolean bContinue = true;
    MCJCallbacks cbTgt = null;
    Thread readerThread = null;
    private int port;

  public MCJSocketClient(MCJCallbacks cbTgt) {
    this.cbTgt = cbTgt;
  }

  public MCJRetBoolExt Connect(String hostName, int port, int timeOut) {
    this.port = port;
    //IPAddress addr;
    if (socket != null && socket.isConnected()) {
      return (new MCJRetBoolExt(false));
    }
    try {
      socket = new Socket(hostName, port);
      socket.setTcpNoDelay(true);
    } catch (Exception ex) {
      return (new MCJRetBoolExt(false, "Connection failed"));
    }
    if (!socket.isConnected()) {
      return (new MCJRetBoolExt(false));
    }
    try {
      inStream = new DataInputStream(socket.getInputStream());
      outStream = new DataOutputStream(socket.getOutputStream());
    } catch (Exception ex) {
      try {
        socket.close();
      } catch (Exception ex2) {
      }
      return (new MCJRetBoolExt(false, "Error getting streams"));
    }
    bContinue = true;
    readerThread = new Thread("Read incomming messages") {
      public void run() {
        int bRead = 0;
        while (bContinue) {
          try {
            bRead = inStream.read(buffer);
            if (cbTgt != null) {
              cbTgt.OnNewBufferCallback(buffer, bRead);
            }
          } catch (Exception ex) {
            Disconnect();
            return;
          }
        }
      }
    };
    readerThread.start();
    return (new MCJRetBoolExt(true));
  }

  public void Disconnect() {
    if (!bContinue)
      return;
    bContinue = false;
    try {
      inStream.close();
      outStream.close();
      readerThread.join(500);
      socket.close();
    } catch (Exception ex) {
      System.out.println("MC Socket Client disconnect exception: " + ex.getMessage());
    } finally {
      //System.out.println("MC Socket Client disconnect on port " + port);
    }
  }

  public boolean Write(byte[] data) {
    if (socket == null || !socket.isConnected() || data.length == 0) {
      return false;
    }
    try {
      outStream.write(data);
    } catch (Exception ex) {
      return false;
    }
    return true;
  }
}
