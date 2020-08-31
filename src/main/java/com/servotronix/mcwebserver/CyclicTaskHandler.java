package com.servotronix.mcwebserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.json.JSONObject;

public class CyclicTaskHandler {
  
  private static Socket clientSocket = null;
  private static DataOutputStream outToServer;
  private static BufferedReader inFromServer;
  private static boolean isConnected = false;
  private static final Lock qryAre = new ReentrantLock(true);
  private static StringBuilder resSB;
  private static final int TIMEOUT = 6000;
  
  public static boolean isConnected() { 
    if (clientSocket != null && clientSocket.isClosed())
      isConnected = false;
    return isConnected;
  }
  
  public static String QueryMC(String cmd){
    long connTime = now();
    if (!isConnected()) {
      return "";
    }
    qryAre.lock();
    if (!send(cmd)) {
      // can't send, probably socket closed, try to reconnect...
      System.out.println("CAN'T SEND CYC QUERY TO MC, TRYING TO RECONNECT...");
      if (!connect() || !send(cmd)) {
        System.out.println("CAN'T SEND QUERY TO MC SOCKET:" + cmd);
        disconnect();
        qryAre.unlock();
        return "";
      } 
    }
    //System.out.println(cmd + " SENT >>> " + (now() - connTime));
    resSB = new StringBuilder();
    // READ STREAM UNTIL AN ANSWER
    boolean gotAnswer = false;
    int lines = 0;
    long diff = 0;
    String ret = null;
    while (!gotAnswer && isConnected && (diff < TIMEOUT)) {
      try {
        String line = inFromServer.readLine(); // this is using the TIMOUT constant
        diff = Math.abs(now() - connTime);
        if (line.endsWith("$$")) {
          resSB.append(line.substring(0, line.length()-2));
          ret = handleMessage(resSB.toString(), cmd);
          if (ret != null) {
            gotAnswer = true;
          }
          break;
        } else {
          if (lines > 0)
            resSB.append("\\n");
          resSB.append(line);
          lines++;
        }
      } catch (Exception e) {
        System.out.println("ERROR READING FROM CYCLIC TASK, DISCONNECTING...");
        disconnect();
        qryAre.unlock();
        System.out.println("DISCONNECTED CYC (SOCKET CLOSED PROBABLY)");
        return "";
      }
    }
    // HANDLE TIMEOUT
    if (diff >= TIMEOUT && !gotAnswer) {
      System.out.println(cmd + " RET >>> TIMEOUT (" + diff + ")");
      qryAre.unlock();
      return "";
    }
    //System.out.println(cmd + " RESULT >>> " + (now() - connTime));
    String result = resSB.toString();
    if (isConnected && gotAnswer) {
      qryAre.unlock();
      return ret;
    } else {
      System.out.println("UNEXPECTED CYC ERROR IN " + cmd + ": isConnected="+isConnected+" gotAnswer="+gotAnswer);
      System.out.println("CURRENT RESULT:" + result);
    }
    qryAre.unlock();
    return "";
  }
  
  private static String handleMessage(String msg, String cmd) {
    try {
      JSONObject data = new JSONObject(msg);
      int id = data.getInt("id");
      if (!cmd.startsWith("{" + id)) {
        System.out.println("WARNING: MISMATCH FOUND! (cmd: " + cmd + "), RESULT WAS:");
        System.out.println(msg);
        return null;
      }
      switch(id) {
        case 0: // tp stat
          return data.get("result").toString();
        default:
          return data.getString("result");
      }
    } catch (Exception e) {
      System.out.println("ERROR: BAD JSON RESULT FROM CYC.PRG:\n------------------------------------------\n" + msg + "\n------------------------------------------");
      e.printStackTrace();
      return null;
    }
  }
  
  private static long now() {
    return System.nanoTime() / 1000000;
  }
  
  
  public static boolean connect() {
    if (isConnected())
      return true;
    try {
      if (clientSocket != null)
        disconnect();
      clientSocket = new Socket("localhost", 8132);
      clientSocket.setSoTimeout(TIMEOUT);
      outToServer = new DataOutputStream(clientSocket.getOutputStream());
      inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),StandardCharsets.UTF_8));
      isConnected = true;
      System.out.println("CYCLIC HANDLER CONNECTED!");
      return true;
    } catch (Exception e) {
      System.out.println("CYCLIC HANDLER COULDN'T CONNECT!");
      isConnected = false;
      return false;
    }
  }
  
  public static boolean send(String msg) {
    try {
      outToServer.writeBytes(msg);
      //System.out.println(now() + "\tQUERY " + msg);
      return true;
    } catch (IOException e) {
      isConnected = false;
      //System.out.println("SEND TO CYCLIC TASK FAILED!");
      //e.printStackTrace();
      return false;
    }
  }
  
  public static void disconnect() {
    if (!isConnected)
      return;
    isConnected = false;
    try { outToServer.close(); } catch (IOException e) { System.out.println("COULD NOT close outToServer"); }
    try { inFromServer.close(); } catch (IOException e) { System.out.println("COULD NOT close inFromServer"); }
    try { clientSocket.close(); } catch (IOException e) { System.out.println("COULD NOT close clientSocket"); }
    System.out.println("CYCLIC HANDLER DISCONNECTED!");
  }
  
}