package MCAuth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.servotronix.mcwebserver.MCDefs;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class MCUserManager {
    
    private static Connection connection = null;
    private static List<String> loggedInUserTokens = new ArrayList<>();
    
    public static List<String> getTokens() {
        return loggedInUserTokens;
    }
    
    public static void addToken(String token) {
        if (!loggedInUserTokens.contains(token))
            loggedInUserTokens.add(token);
    }
    
    public static void removeToken(String token) {
        loggedInUserTokens.remove(token);
    }
    
    public static void init() {
      // Load driver
      try {
        Class.forName("org.hsqldb.jdbcDriver");
      } catch (Exception e) {
        System.out.println("FAILED TO LOAD DRIVER.");
        return;
      }
      // Connect to DB (creates it if doesn't exist)
      try {
        connection = DriverManager.getConnection("jdbc:hsqldb:file:" + MCDefs.MCDB_FOLDER + "MCUsers", "SA", "Servotronix");
      } catch (SQLException e) {
        System.out.println("Can't connect to DB:");
        e.printStackTrace();
        return;
      }
      // Create user table (will fail if already exists, and go to the catch section)
      try {
        Statement statement = connection.createStatement();
        statement.execute("CREATE TABLE users (" +
                          "Username varchar(32)," +
                          "Password varchar(300)," +
                          "Salt varchar(64)," +
                          "Data varchar(300)," +
                          "FullName varchar(32)," +
                          "License Boolean" +
                          ")");
        statement.close();
        boolean b = signup("admin","ADMIN","0","Admin User");
        if (b) {
          System.out.println("Created system admin");
        }
        b = signup("super","1-super99","99","Supervisor User");
        if (b)
          System.out.println("Created system supervisor");
      } catch (SQLException e) { // TABLE ALREADY EXISTS
        
        /* 
            IF FullName coloumn DOESN'T EXIST (Webserver older than v3.0) then add it.
            (will fail if already exists)
          */
        try { 
          Statement statement = connection.createStatement();
          statement.execute("ALTER TABLE users ADD FullName varchar(32) DEFAULT 'N/A' NOT NULL");
          statement.close();
          System.out.println("FullName added to table users...");
        } catch (SQLException e2) {
        }
        
        // For MCs without License coloumn (older than v3.1.4)
        try { 
          Statement statement = connection.createStatement();
          statement.execute("ALTER TABLE users ADD License Boolean DEFAULT 0");
          statement.close();
          System.out.println("License coloumn added to table users...");
        } catch (SQLException e2) {
        }
        
        // For MCs with older web server (older than v3.1.1)
        if (signup("super","1-super99","99","Supervisor User"))
          System.out.println("Created system supervisor");
      }
      // Create logger table (will fail if already exists)
      try {
        Statement loggerStatement = connection.createStatement();
        loggerStatement.execute("CREATE TABLE log (" +
                          "Username varchar(32)," +
                          "Message varchar(300)," +
                          "Time TIMESTAMP," +
                          "Uuid UUID" + 
                          ")");
        loggerStatement.close();
      } catch (SQLException e) { // DATABASE EXISTS
        /* 
            IF UUID coloumn DOESN'T EXIST (Webserver older than v3.2.9) then add it.
            (will fail if already exists)
          */
          try { 
            Statement statement = connection.createStatement();
            statement.execute("ALTER TABLE log ADD Uuid UUID");
            statement.close();
            System.out.println("UUID added to table log...");
          } catch (SQLException e2) {
          }
      }
      
      // Create safety table and add the default password (will fail if already exists)
      try {
        System.out.print("Init safety data...");
        Statement loggerStatement = connection.createStatement();
        loggerStatement.execute("CREATE TABLE safety (pass varchar(300))");
        loggerStatement.close();
        changeSafetyPass("Safety-User");
        System.out.println("initialized table...done.");
      } catch (SQLException e) { // DATABASE EXISTS
        System.out.println("done.");
      }
    }

    public static boolean changeSafetyPass(String pass) {
      try {
        // FIRST, DELETE CURRENT PASSWORD
        Statement statement = connection.createStatement();
        statement.executeUpdate("DELETE FROM safety");
        statement.close();

        // ENCODE PASSWORD TO JWT
        Algorithm algorithm = Algorithm.HMAC256("Servotronix");
        String encoded = JWT.create().withIssuer("auth0").withSubject(pass).sign(algorithm);
        String query = "INSERT INTO safety (pass) VALUES (?)";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, encoded);
        ps.execute();
        ps.close();
        return true;
      } catch (Exception exception){ }
      return false;
    }
    
    public static void closeDBConnection() {
        try {
            if(connection != null)
                connection.close();
        } catch(SQLException e) {
          System.err.println("Can't close DB connection:" + e.getMessage());
        }
    }
    
    public static boolean signup(String name, String password, String permission, String fullName) {    
      Base64.Encoder enc = Base64.getEncoder();
      byte[] salt = MCAuth.getNextSalt();
      try {
        String query = "SELECT * FROM users WHERE Username = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, name);
        // check if username exists
        ResultSet rs = statement.executeQuery();
        if (rs.next()) // username found
            return false;
        // create JWT
        JSONObject data = new JSONObject();
        data.put("username", name);
        int iPermission = Integer.parseInt(permission);
        if (iPermission < 0 || (iPermission > 3 && iPermission != 99))
          return false;
        data.put("permission",iPermission);
        String token = null;
        try {
            Algorithm algorithm = Algorithm.HMAC256("Servotronix");
            token = JWT.create()
                .withIssuer("auth0")
                .withSubject(data.toString())
                .sign(algorithm);

        } catch (UnsupportedEncodingException exception){
            return false;
        } catch (JWTCreationException exception){
            return false;
        }
        query = "INSERT INTO users (Username,Password,Salt,Data,FullName,License) VALUES (?,'" +
                            enc.encodeToString(MCAuth.hash(password.toCharArray(),salt)) + "','" +
                            enc.encodeToString(salt) + "',?,?,0)";
        statement = connection.prepareStatement(query);
        statement.setString(1, name);
        statement.setString(2, token);
        statement.setString(3, fullName);
        statement.execute();
        statement.close();
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    }
    
    public static boolean setLicense(String name) {
      if (name == null)
        return false;
      String query = "SELECT * FROM users WHERE Username = ?";
      try {
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, name);
        ResultSet rs = statement.executeQuery();
        if (rs.next()) { // username found
          query = "UPDATE users SET License = 1 WHERE Username = ?";
          statement = connection.prepareStatement(query);
          statement.setString(1, name);
          statement.execute();
          statement.close();
          return true;
        }
        return false;
      } catch (SQLException e) {
        System.out.println("SQL ERROR!");
        e.printStackTrace();
        return false;
      }
    }
    
    public static boolean edit(String name, String password, String permission, String fullName) {
      Base64.Encoder enc = Base64.getEncoder();
      byte[] salt = MCAuth.getNextSalt();
      try {
        // Make sure ADMIN permission stays 0
        if (name.equals("admin") && Integer.parseInt(permission) != 0)
          return false;
        String query = "SELECT * FROM users WHERE Username = ?";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, name);
        // check if username exists
        ResultSet rs = statement.executeQuery();
        if (rs.next()) { // username found          
          // create JWT
          JSONObject data = new JSONObject();
          data.put("username", name);
          int iPermission = Integer.parseInt(permission);
          if (iPermission < 0 || (iPermission > 3 && iPermission != 99))
            return false;
          data.put("permission",iPermission);
          String token = null;
          try {
              Algorithm algorithm = Algorithm.HMAC256("Servotronix");
              token = JWT.create()
                  .withIssuer("auth0")
                  .withSubject(data.toString())
                  .sign(algorithm);

          } catch (UnsupportedEncodingException exception){
              return false;
          } catch (JWTCreationException exception){
              return false;
          }
          if (password == null || password.length() == 0) { // Don't update password
            query = "UPDATE users SET Data = ?, FullName = ? WHERE Username = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, token);
            statement.setString(2, fullName);
            statement.setString(3, name);
          } else {
            query = "UPDATE users SET Password = ?, Salt = ?, Data = ?, FullName = ? WHERE Username = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, enc.encodeToString(MCAuth.hash(password.toCharArray(),salt)));
            statement.setString(2, enc.encodeToString(salt));
            statement.setString(3, token);
            statement.setString(4, fullName);
            statement.setString(5, name);
          }
          statement.execute();
          statement.close();
          return true;
        } else {
          return false;
        }
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    }

    public static boolean authSafety(String pass) {
      if (pass == null)
        return false;
      // search for jwt pass in table
      try {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM safety");
        if (rs.next()) {
          String currJWT = rs.getString("pass");
          rs.close();
          statement.close();
          Algorithm algorithm = Algorithm.HMAC256("Servotronix");
          JWTVerifier verifier = JWT.require(algorithm).withIssuer("auth0").build();
          DecodedJWT jwt = verifier.verify(currJWT);
          return pass.equals(jwt.getSubject());
        } else {
          rs.close();
          statement.close();
          System.out.println("No safety pass stored on this database!");
          return false;
        }
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    }
    
    public static JSONObject login(String token) {
      String username = getUsernameFromToken(token);
      if (username == null)
        return null;
      // search for username in table
      try {
        JSONObject user = null;
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM users WHERE Username = '" + username + "'");
        while (rs.next() && user == null) {
          user = new JSONObject();
          user.put("username", rs.getString("Username"));
          user.put("fullName", rs.getString("FullName"));
          user.put("license", rs.getBoolean("License"));
          int permission;
          Algorithm algorithm = Algorithm.HMAC256("Servotronix");
          JWTVerifier verifier = JWT.require(algorithm).withIssuer("auth0").build();
          String userData = rs.getString("Data");
          DecodedJWT jwt = verifier.verify(userData);
          JSONObject data = new JSONObject(jwt.getSubject());
          permission = data.getInt("permission");
          user.put("permission", permission);
          rs.close();
          statement.close();
          return user;
        }
      } catch (Exception e) {
        
      }
      return null;
    }
    
    public static JSONObject login(String name, String password) {
      JSONObject user = null;
      Base64.Decoder dec = Base64.getDecoder();
      try {
        // search for username in table
        Statement statement = connection.createStatement();
        System.out.println("SEARCHING FOR " + name);
        ResultSet rs = statement.executeQuery("SELECT * FROM users WHERE Username = '" + name + "'");
        boolean passwordOK = false;
        while (rs.next() && user == null) {
          System.out.println("FOUND USERNAME...");
          passwordOK = MCAuth.isExpectedPassword(
            password.toCharArray(),
            dec.decode(rs.getString("Salt")),
            dec.decode(rs.getString("Password"))
          );
          if (passwordOK) {
            System.out.println("password OK!");
            String userData = rs.getString("Data");
            user = new JSONObject();
            user.put("username", rs.getString("Username"));
            user.put("fullName", rs.getString("FullName"));
            user.put("license", rs.getBoolean("License"));
            int permission;
            try {
              Algorithm algorithm = Algorithm.HMAC256("Servotronix");
              JWTVerifier verifier = JWT.require(algorithm).withIssuer("auth0").build();
              DecodedJWT jwt = verifier.verify(userData);
              JSONObject data = new JSONObject(jwt.getSubject());
              permission = data.getInt("permission");
            } catch (UnsupportedEncodingException exception){
              System.out.println("UnsupportedEncodingException");
              permission = -1;
            } catch (JWTVerificationException exception){
              System.out.println("JWTVerificationException");
              permission = -1;
            }
            user.put("permission", permission);
          } else {
            System.out.println("password WRONG!");
          }
        }
        rs.close();
        statement.close();
      } catch (SQLException e) {
        System.out.println("SQLException while logging in!");
        e.printStackTrace();
      } finally {
        return user;
      }
    }
    
    public static boolean delete(String name) {
      if (name.equals("admin") || name.equals("super"))
        return false;
      try {
        // search for username in table
        Statement statement = connection.createStatement();
        boolean deleted = (statement.executeUpdate("DELETE FROM users WHERE Username = '" + name + "'") > 0);
        statement.close();
        return deleted;
      } catch (SQLException e) {
        return false;
      }
    }
    
    public static int getPermission(String username) {    
      int permission = -1;
      try {
        // search for username in table
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM users WHERE Username = '" + username + "'");
        while (rs.next()) {
          String userData = rs.getString("Data");
          try {
            Algorithm algorithm = Algorithm.HMAC256("Servotronix");
            JWTVerifier verifier = JWT.require(algorithm).withIssuer("auth0").build();
            DecodedJWT jwt = verifier.verify(userData);
            JSONObject data = new JSONObject(jwt.getSubject());
            permission = data.getInt("permission");
          } catch (UnsupportedEncodingException exception){
             permission = -1;
          } catch (JWTVerificationException exception){
             permission = -1;
          }
        }
        rs.close();
        statement.close();
      } finally {
          return permission;
      }
    }
    
    public static JSONArray getAllUsers(int reqPerm, String reqName) throws Exception {    
      JSONArray users = new JSONArray();
      try {
        PreparedStatement statement;
        String query = "SELECT * FROM users";
        if (reqPerm != 0 && reqPerm != 99) {
          query += " WHERE Username = ?";
          statement = connection.prepareStatement(query);
          statement.setString(1, reqName);
        } else {
          statement = connection.prepareStatement(query);
        }
        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
          JSONObject user = new JSONObject();
          String userData = rs.getString("Data");
          try {
            Algorithm algorithm = Algorithm.HMAC256("Servotronix");
            JWTVerifier verifier = JWT.require(algorithm)
              .withIssuer("auth0")
              .build(); //Reusable verifier instance
            DecodedJWT jwt = verifier.verify(userData);
            JSONObject data = new JSONObject(jwt.getSubject());
            int p = data.getInt("permission");
            if (p == 99)
              continue;
            user.put("username", rs.getString("Username"));
            user.put("fullName", rs.getString("FullName"));
            user.put("permission", p);
            users.put(user);
          } catch (Exception e){
            e.printStackTrace();
          }
        }
        rs.close();
        statement.close();
        return users;
      } catch (SQLException e) {
          throw new Exception("Databse Error");
      }
    }
    
    public static String getUsernameFromToken(String token) {
    if (token == null)
      return null;
    String username = null;
    try {
      Algorithm algorithm = Algorithm.HMAC256("Servotronix");
      JWTVerifier verifier = JWT.require(algorithm)
          .withIssuer("auth0")
          .build(); //Reusable verifier instance
      DecodedJWT jwt = verifier.verify(token);
      JSONObject data = new JSONObject(jwt.getSubject());
      username = data.getString("username");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return username;
    }
  }
    
}
