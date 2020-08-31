package MCAuth;

import static MCAuth.MCUserManager.getUsernameFromToken;

import com.servotronix.mcwebserver.MCConnectionManager;
import com.servotronix.mcwebserver.MCDefs;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

public class DatabaseOperations {
  
  private static Connection connection = null;
  
  public static void log (String token, String msg) {
    String username = getUsernameFromToken(token);
    if (username == null)
      return;
    if (msg == null)
      return;
    // Connect to DB (creates it if doesn't exist)
    try {
      connection = DriverManager.getConnection("jdbc:hsqldb:file:" + MCDefs.MCDB_FOLDER + "MCUsers", "SA", "Servotronix");
      String query = "INSERT INTO log (Username,Message,Time,Uuid) VALUES (?,?,?,?)";
      PreparedStatement statement = connection.prepareStatement(query);
      statement.setString(1, username);
      statement.setString(2, msg);
      statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
      UUID uuid = UUID.randomUUID();
      statement.setObject(4,uuid);
      statement.execute();
      statement.close();
      connection.close();
      JSONObject obj = new JSONObject();
      obj.put("user", username);
      obj.put("msg", msg);
      obj.put("time",System.currentTimeMillis());
      obj.put("uuid",uuid.toString());
      MCConnectionManager.instance.sendToAll("***" + obj.toString());
    } catch (SQLException e) {
      System.out.println("Logging failed:");
      e.printStackTrace();
      return;
    }
  }
  
  public static boolean clearOldLogEntries() {
    String query = "DELETE FROM log WHERE Time < DATEADD(MONTH,-1,CURRENT_TIMESTAMP)";
    try {
      connection = DriverManager.getConnection("jdbc:hsqldb:file:" + MCDefs.MCDB_FOLDER + "MCUsers", "SA", "Servotronix");
      PreparedStatement statement = connection.prepareStatement(query);
      statement.executeUpdate();
      return true;
    } catch (Exception e) {
      
    }
    return false;
  }

  public static boolean clear() {
    String query = "DELETE FROM log";
    try {
      connection = DriverManager.getConnection("jdbc:hsqldb:file:" + MCDefs.MCDB_FOLDER + "MCUsers", "SA", "Servotronix");
      PreparedStatement statement = connection.prepareStatement(query);
      statement.executeUpdate();
      return true;
    } catch (Exception e) {
      
    }
    return false;
  }
  
  public static JSONArray getLog() {
    JSONArray result = new JSONArray();
    try {
      connection = DriverManager.getConnection("jdbc:hsqldb:file:" + MCDefs.MCDB_FOLDER + "MCUsers", "SA", "Servotronix");
      String query = "SELECT * FROM log ORDER BY Time DESC";
      PreparedStatement statement = connection.prepareStatement(query);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        JSONObject log = new JSONObject();
        log.put("username", rs.getString("Username"));
        log.put("time", rs.getTimestamp("Time").getTime());
        log.put("msg", rs.getString("Message"));
        log.put("UUID", rs.getString("Uuid"));
        result.put(log);
      }
      statement.close();
      connection.close();
    } catch (SQLException e) {
      System.out.println("Logging failed:");
      e.printStackTrace();
    } finally {
      return result;
    }
  }
  
  public static boolean destroy() {
    try {
      connection = DriverManager.getConnection("jdbc:hsqldb:file:" + MCDefs.MCDB_FOLDER + "MCUsers", "SA", "Servotronix");
      Statement statement = connection.createStatement();
      statement.execute("DROP TABLE users");
      statement.close();
      statement = connection.createStatement();
      statement.executeUpdate("DROP TABLE log");
      statement.close();
      connection.close();
      System.out.println("Users and Log tables deleted");
      return true;
    } catch (Exception e) {
      System.out.println("Users and Log tables WERE NOT deleted!");
      e.printStackTrace();
      return false;
    }
  }
  
}
