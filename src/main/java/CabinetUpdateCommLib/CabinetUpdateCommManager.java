package CabinetUpdateCommLib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import com.servotronix.mcwebserver.Utils;

import org.apache.commons.net.telnet.TelnetClient;
import org.java_websocket.WebSocket;

public class CabinetUpdateCommManager {
  
  // CONSTANTS
  private static final int PORT = 12373;
  private static final int DEFAULT_READ_TIMEOUT = 60000; // 60 seconds
  
  // Manager Variables
  private TelnetClient tc;
  private InputStream in;
  private BufferedReader inFromServer;
  private PrintStream out;
  private Thread watchdog = null;
  private boolean connected = false;
  private long lastTime = 0;
  private boolean isSending = false;
  private int timeout = DEFAULT_READ_TIMEOUT;
  private WebSocket ws = null;
  
  public CabinetUpdateCommManager(WebSocket ws)
  {
    this.tc = new TelnetClient();
    this.ws = ws;
  }
  
  public void disconnect() {
    System.out.println("Disconnecting cabinet update...");
    connected = false;
    try {
      if (tc != null)
        tc.disconnect();
    } catch (Exception ex) {
      System.out.println("Disconnect error!");
      ex.printStackTrace();
    }
    System.out.println("Cabinet Update Comm Mngr Disconnected");
  }
  
  public boolean connect() {
    if (connected)
      return true;
    try {
      tc.connect("127.0.0.1", PORT);
      if (tc.isConnected()) {
        connected = true;
        System.out.println("Connected to Cabinet Update Telnet!");
        // Get input and output stream references
        in = tc.getInputStream();
        inFromServer = new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));
        out = new PrintStream(tc.getOutputStream());
        out.flush();
        watchdog = new Thread(new Runnable() {
          @Override
          public void run() {
            while (connected) {
              if (isSending && Utils.currentTimeMillis() - lastTime > timeout)
                disconnect();
              try {Thread.sleep(3000);}catch(Exception e){}
            }
          }
        },"java_watchdog");
        watchdog.start();
        return true;
      }
      return false;
    } catch (IOException e) {
      return false;
    }
  }

  public String send(String command, Integer timeout, int cmd_id) {
    String result = "";
    if ( connected && command != null )
    {
      write(command);
      try {
        result = readUntil(timeout);
      } catch (IOException e) {
        e.printStackTrace();
        result = "";
        disconnect();
      }
    }
    return result;
  }

  private synchronized String readUntil(Integer timeout) throws IOException
  {
    lastTime = Utils.currentTimeMillis();
    this.timeout = timeout == null ? DEFAULT_READ_TIMEOUT : timeout;
    isSending = true;
    String result = "";
    String line;
    if (in != null) {
      while ((line = inFromServer.readLine()) != null) {
        result = line;
        break;
      }
    }
    isSending = false;
    return result;
  }

  /**
   * Sends actual Telnet command.
   *
   * @param value
   */
  private synchronized void write(String value) {
    if ( null != out ) {
      out.println(value);
      out.flush();
    }
  }
}
