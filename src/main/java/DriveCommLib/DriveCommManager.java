package DriveCommLib;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.servotronix.mcwebserver.Utils;

import org.apache.commons.net.telnet.TelnetClient;
import org.java_websocket.WebSocket;
import org.json.JSONObject;

public class DriveCommManager {
  
  // CONSTANTS
  private static final int PORT = 4000;
  private static final int DEFAULT_READ_TIMEOUT = 6000; // 6 seconds
  
  // Manager Variables
  private TelnetClient tc;
  private InputStream in;
  private PrintStream out;
  private String promptRegex = "(.|\\s)*-(-|[1-9])>$";
  private Thread watchdog = null;
  private boolean connected = false;
  private long lastTime = 0;
  private boolean isSending = false;
  private int timeout = DEFAULT_READ_TIMEOUT;
  private boolean luaMode = false;
  private String luaCommand = null;
  private int luaCommandID = -1;
  private WebSocket ws = null;
  
  public DriveCommManager(WebSocket ws)
  {
    this.tc = new TelnetClient();
    this.ws = ws;
  }
  
  public void disconnect() {
    connected = false;
    try {
      tc.disconnect();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    System.out.println("Drive Comm Mngr Disconnected");
  }
  
  public boolean connect(String ip) {
    if (connected)
      return true;
    try {
      tc.connect(ip, PORT);
      if (tc.isConnected()) {
        connected = true;
        System.out.println("Connected to Drive Telnet!");
        // Get input and output stream references
        in = tc.getInputStream();
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
        readUntil(null);
        return true;
      }
      return false;
    } catch (IOException e) {
      return false;
    }
  }
  
  private void emitLine(String line) {
    JSONObject result = new JSONObject();
    result.put("cmd_id", luaCommandID);
    result.put("cmd", luaCommand);
    result.put("msg",line);
    result.put("type","sub");
    if (ws != null && ws.isOpen()) {
      try {
        ws.send(result.toString());
      } catch (Exception e) {
        System.out.println("CAN'T SEND ANSWER");
      }
    }
  }
  
  private void handleLua() {
    if (in != null) {
      StringBuffer sb = new StringBuffer();
      char lastChar = '>';
      char ch;
      while (true) {
        try {
          int readByte = in.read();
          if (readByte != -1) {
            ch = (char) readByte;
          } else {
            disconnect();
            throw new IOException( "TelnetConnection: End of stream reached. Maybe remote end crashed " );
          }
        } catch (IOException e) {
          disconnect();
          break;
        }
        sb.append(ch);
        if (ch == '\n') {
          emitLine(sb.toString());
          sb.setLength(0);
          ch = '>';
          continue;
        }
        if (ch == lastChar) {
          String[] lines = sb.toString().trim().split("\n");
          if (lines[lines.length-1].matches(promptRegex))
            break;
        }
      }
      
    }
  }

  public TelnetResult send(String command, Integer timeout, int cmd_id) {
    //System.out.println("sending " + command);
    TelnetResult result = new TelnetResult();
    if ( connected && command != null )
    {
      luaMode = command.startsWith("lua");
      luaCommand = luaMode ? command : null;
      luaCommandID = luaMode ? cmd_id : -1;
      write(command);
      if (luaMode) {
        handleLua();
        result.setAns(command);
        return result;
      }
      try {
        result = readUntil(timeout);
      } catch (IOException e) {
        result.setAns("N/A");
        disconnect();
      }
    }
    return result;
  }

  private synchronized TelnetResult readUntil(Integer timeout) throws IOException
  {
    //System.out.println("Read until " + pattern + "...");
    lastTime = Utils.currentTimeMillis();
    this.timeout = timeout == null ? DEFAULT_READ_TIMEOUT : timeout;
    isSending = true;
    String retVal = null;
    TelnetResult result = new TelnetResult();
    if (in != null) {
      StringBuffer sb = new StringBuffer();
      StringBuffer currLine = new StringBuffer();
      char lastChar = '>';
      char ch;
      while (true) {
        try {
          int readByte = in.read();
          if (readByte != -1) {
            ch = (char) readByte;
          } else {
            disconnect();
            throw new IOException( "TelnetConnection: End of stream reached. Maybe remote end crashed " );
          }
        } catch (IOException e) {
          disconnect();
          throw new IOException( e );
        }
        sb.append(ch);
        currLine.append(ch);
        if (ch == '\n')
          currLine.setLength(0);
        else if (ch == lastChar) {
          Pattern r = Pattern.compile(promptRegex);
          Matcher m = r.matcher(currLine.toString());
          if (m.find()) {
            String string = sb.toString();
            retVal = string.substring( 0, string.length() - 3 );
            result.setAns(retVal);
            result.setPrompt(m.group(2));
            break;
          }
        }
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
