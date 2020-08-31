package com.servotronix.serverMapping;

import MCAuth.DatabaseOperations;
import static MCAuth.MCAuth.generateToken;
import static MCAuth.MCAuth.verifyToken;
import MCAuth.MCUserManager;
import static MCAuth.MCUserManager.getAllUsers;
import static MCAuth.MCUserManager.getPermission;
import static MCAuth.MCUserManager.getUsernameFromToken;
import MCAuth.TokenManager;
import com.servotronix.ErrorResponse;
import com.servotronix.MCInfo.MCInfo;
import com.servotronix.mcwebserver.FreeMarkerEngine;
import com.servotronix.mcwebserver.MCDefs;
import static com.servotronix.mcwebserver.MCDefs.AVATAR_PATH;
import static com.servotronix.mcwebserver.MCDefs.PROFILE_PICS;
import static com.servotronix.mcwebserver.MCDefs.RECORDFILE;
import com.servotronix.mcwebserver.MCThemeManager;
import com.servotronix.mcwebserver.Utils;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Part;
import org.eclipse.jetty.server.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import static spark.Spark.*;

public class MapCS {

  private static boolean convert(String recName) {
    try {
      File file = new File("/RAM/" + recName.toUpperCase() + ".REC");
      File CSV = new File(RECORDFILE);
      FileWriter writer = new FileWriter(CSV, false);
      FileInputStream fis = new FileInputStream(file);
      byte[] arr = new byte[(int)file.length()];
      fis.read(arr);
      ByteBuffer bb = ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN);
      /* Read the File header (16 bytes):
        [0]o Number of variables;
        [1]o Number of recorded points;
        [2]o Recording gap;
        [3]o Length of the info string
     */
      int varCount = (bb.getInt() & 0x3ff);
      int recordCount = bb.getInt();
      int gap = bb.getInt();
      int len = bb.getInt();
      String pc_infostr;
      byte[] info = new byte[len];
      bb.get(info);
      pc_infostr = new String(info);
      writer.write("" + gap + '\n');
      writer.write(pc_infostr+'\n');
      int count = recordCount;
      while (count > 0) {
        int i = varCount;
        while (i > 0) {
          try {
            double val = bb.getDouble();
            writer.write(String.format("%16.15e",val)+",");
            i--;
          } catch (BufferUnderflowException e) {
            break;
          }
        }
        writer.write("\n");
        count--;
      }
      writer.flush();
      return true;
      
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
  
  private static List<String> getResourceFiles( String path ) throws IOException {
    List<String> filenames = new ArrayList<>();
    try (
      InputStream in = getResourceAsStream( path );  
      BufferedReader br = new BufferedReader( new InputStreamReader( in ) ) ) {
      String resource;
      while( (resource = br.readLine()) != null ) {
        filenames.add( resource );
      }
    }
    return filenames;
  }

  private static InputStream getResourceAsStream( String resource ) {
    final InputStream in
      = getContextClassLoader().getResourceAsStream( resource );

    return in == null ? MapCS.class.getResourceAsStream( resource ) : in;
  }

  private static ClassLoader getContextClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }
  
  public static String getFileContent(String filePath)
  {
      StringBuilder contentBuilder = new StringBuilder();
      try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8)) {
        stream.forEach(s -> contentBuilder.append(s).append("\n"));
      } catch (IOException e) {
        e.printStackTrace();
      }
      return contentBuilder.toString();
  }

  private static void deleteFolder(File folder) {
    if (folder == null || !folder.exists() || !folder.isDirectory()) return;
    File[] files = folder.listFiles();
    if(files!=null) {
      for(File f: files) {
        if(f.isDirectory()) {
          deleteFolder(f);
        } else {
          f.delete();
        }
      }
    }
    folder.delete();
  }
  
  public static void map(FreeMarkerEngine engine, MCInfo info) {
    
    after("/rs/*",(req,res)->{
      if (res.type() != null || res.body() != null)
        return;
      String token = TokenManager.generateToken(); // TO AUTHENTICATE THIS SESSION
      if (token == null)
        return;
      req.session().attribute("securityToken",token);
      res.body(getFileContent(MCDefs.EXT_WWW_FOLDER + "rs/index.html"));
      res.status(200);
    });
    
    /*
      THESE ARE THE NEW API FUNCTIONS, OLD ONES WILL BE DEPRECATED AFTER TESTS.
    */
    get("/cs/api/java-version", (req, res) -> {
      res.type("application/json");
      JSONObject result = new JSONObject();
      result.put("ver", MCDefs.VER);
      return result;
    });
    get("/cs/api/free-space", (req, res) -> {
      return new File(MCDefs.MC_FOLDER).getUsableSpace();
    });
    get("/cs/api/factoryRestore", (req, res) -> {
      res.type("application/json");
      // CHECK PERMISSION
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      if (permission != 0 && permission != 99) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      System.out.println("FACTORY RESTORE IN PROGRESS...");
      System.out.println("REMOVING PROFILE PICTURES...");
      // REMOVE USER PROFILE PICS
      deleteFolder(new File(PROFILE_PICS));
      System.out.println("REMOVING USER DATABASE...");
      // REMOVE DATABASE
      return DatabaseOperations.destroy();
    });
    get("/cs/api/ports", (req, res) -> {
      res.type("application/json");
      JSONObject ret = new JSONObject();
      ret.put("ws", "" + MCDefs.PORT);
      return ret;
    });
    get("/cs/api/license/:name", (req, res) -> {
      res.type("application/json");
      return info.getFeatures().contains(req.params(":name"));
    });
    get("/cs/api/users", (req, res) -> {
      res.type("application/json");
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      String username = getUsernameFromToken(token);
      if (permission == -1) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      return getAllUsers(permission,username);
    });
    post("/cs/api/users", (req, res) -> {
      res.type("application/json");
      JSONObject data = new JSONObject(req.body()).getJSONObject("user");
      /*if (!hasPrivateAPIAccess(req)) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }*/
      String username = data.getString("username");
      String pass = data.getString("password");
      if (username == null || pass == null) {
        res.status(Response.SC_BAD_REQUEST);
        return false;
      }
      JSONObject user = MCUserManager.login(username, pass);
      if (user == null) {
        res.status(422);
        return new ErrorResponse("Invalid username or password");
      }
      String token = generateToken(username,getPermission(username));
      JSONObject response = new JSONObject();
      response.put("user", user);
      response.put("token", token);
      if (token != null) {
        DatabaseOperations.log(token,"login");
      }
      return response;
    });
    
    get("/cs/api/user", (req,res) -> {
      res.type("application/json");
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      JSONObject response = new JSONObject();
      JSONObject user = MCUserManager.login(token);
      if (user == null) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      response.put("user", user);
      response.put("token", token);
      if (token != null) {
        DatabaseOperations.log(token, "login");
      }
      return response;
    });
    
    get("/cs/api/sysinfo", (req, res) -> {
      res.type("application/json");
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      if (verifyToken(token) >= 0)
        return info.getInfoAsJSON();
      res.status(Response.SC_FORBIDDEN);
        return false;
    });

    get("/cs/api/sysBasicInfo", (req, res) -> {
      res.type("application/json");
      JSONObject obj = info.getInfoAsJSON();
      JSONObject basic = new JSONObject();
      basic.put("name",obj.getString("name"));
      basic.put("ip",obj.getString("ip"));
      basic.put("sn",obj.getString("sn"));
      return basic;
    });
    
    get("/cs/api/:username/pic", (req, res) -> {
      String token = req.queryParams("token");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      String currUser = getUsernameFromToken(token);
      int permission = verifyToken(token);
      String username = req.params(":username");
      if (!username.equals(currUser) && permission != 0 && permission != 99) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      File dir = new File(MCDefs.PROFILE_PICS + username);
      if (dir.exists() && dir.isDirectory()) {
        for (File f : dir.listFiles()) {
          if (f.getName().indexOf("profilepic")==0) {
            res.raw().setContentType("image/jpeg");
            try (OutputStream out = res.raw().getOutputStream()) {
              ImageIO.write(ImageIO.read(f),"jpg",out);
              return true;
            } catch (IOException e) {
              res.status(Response.SC_INTERNAL_SERVER_ERROR);
              return false;
            }
          }
        }
      }
      // FILE NOT FOUND
      res.raw().setContentType("image/jpeg");
      try (OutputStream out = res.raw().getOutputStream()) {
        InputStream in = MapFileOperations.class.getResourceAsStream(AVATAR_PATH);
        if (in != null) {
          ImageIO.write(
            ImageIO.read(MapFileOperations.class.getResourceAsStream(AVATAR_PATH)),
            "png",out
          );
          return true;
        }
        return false;
      } catch (IOException e) {
        return false;
      }
    });
    
    post("/cs/api/:username/pic", "multipart/form-data", (request, response) -> {
      boolean validToken = false;
      String token = request.headers("Authorization");
      if (token != null) {
        token = token.split(" ")[1];
        validToken = verifyToken(token) > -1;
      }      
      long maxFileSize = 2000000;       // the maximum size allowed for uploaded files
      long maxRequestSize = 100000000;    // the maximum size allowed for multipart/form-data requests
      int fileSizeThreshold = 1024;       // the size threshold after which files will be written to disk
      MultipartConfigElement multipartConfigElement = new MultipartConfigElement(MCDefs.PROFILE_PICS, maxFileSize, maxRequestSize, fileSizeThreshold);
      request.raw().setAttribute("org.eclipse.jetty.multipartConfig",multipartConfigElement);
      Collection<Part> parts = request.raw().getParts();
      try {
        String username = request.params(":username");
        String reqUsername = validToken ? getUsernameFromToken(token) : null;
        Part uploadedFile = request.raw().getPart("file");
        if (
            username == null ||
            reqUsername == null ||
            username.equals("admin") ||
            username.equals("super") ||
            !username.equals(reqUsername)
        ) {
          response.status(Response.SC_FORBIDDEN);
          uploadedFile.delete();
          uploadedFile = null;
          multipartConfigElement = null;
          parts = null;
          return false;
        }
        String name = uploadedFile.getSubmittedFileName().toUpperCase();
        String ext = name.substring(name.indexOf(".")+1);
        if (!Arrays.asList(MCDefs.ALLOWED_IMG_EXTENSIONS).contains(ext)) {
          return false;
        }
        Path out = Paths.get(MCDefs.PROFILE_PICS + username + "/profilepic.jpg");
        try (final InputStream in = uploadedFile.getInputStream()) {
          // CREATE DIRECTORIES, IF NECESSARY
          File dir = new File(MCDefs.PROFILE_PICS + username);
          if (!dir.exists()) {
            Files.createDirectories(Paths.get(MCDefs.PROFILE_PICS + username));
          }
          BufferedImage image = ImageIO.read(in);
          int w = image.getWidth();
          int h = image.getHeight();
          BufferedImage cropped = null;
          if (w > h) {
            int diff = (w-h) / 2;
            cropped = image.getSubimage(diff,0,h,h);
          } else if (w < h) {
            int diff = (h-w) / 2;
            cropped = image.getSubimage(0,diff,w,w);
          } else 
            cropped = image;
          if (cropped != null) {
            int w2 = cropped.getWidth();
            int h2 = cropped.getHeight();
            int [] pixels = cropped.getRGB(0,0,w2,h2,null,0,w2);
            BufferedImage copy = new BufferedImage(w2,h2,BufferedImage.TYPE_INT_RGB);
            copy.setRGB(0, 0,w2,h2,pixels,0,w2);
            File outputfile = out.toFile();
            ImageIO.write(copy,"jpg",outputfile);
          }
          uploadedFile.delete();
          uploadedFile = null;
        } catch (IOException e) {
          System.out.println("Error with InputStream: " + e.getMessage());
          e.printStackTrace();
          uploadedFile.delete();
          uploadedFile = null;
          multipartConfigElement = null;
          parts = null;
          return false;
        }
      } catch (Exception e) {
        System.out.println("Error while uploading file: " + e.getMessage());
        e.printStackTrace();
        multipartConfigElement = null;
        parts = null;
        return false;
      }
      multipartConfigElement = null;
      parts = null;
      return true;
    });
    
    get("/cs/api/dashboard/recfiles", (req, res) -> {
      res.type("application/json");
      JSONArray result = new JSONArray();
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return result;
      }
      token = token.split(" ")[1];
      if (verifyToken(token) == -1) {
        res.status(Response.SC_FORBIDDEN);
        return result;
      }
      File recFolder = new File("/RAM/");
      int i = 0;
      for (File f : recFolder.listFiles()) {
        String name = f.getName();
        if (name.endsWith("REC")) {
          result.put(i,name);
          i++;
        }
      }
      return result;
    });
    
    get("/cs/api/dashboard/rec/:recName", (req, res) -> {
      res.type("application/json");
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      if (verifyToken(token) == -1) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String recName = req.params(":recName");
      if (recName == null) {
        res.status(Response.SC_BAD_REQUEST);
        return null;
      }
      if (convert(recName)) {
        try {
          String content = new String(Files.readAllBytes(Paths.get(MCDefs.RECORDFILE)));
          JSONObject response = new JSONObject();
          response.put("data", content);
          return response;
        } catch (Exception e) {
          res.status(Response.SC_INTERNAL_SERVER_ERROR);
          return false;
        }
      }
      res.status(Response.SC_INTERNAL_SERVER_ERROR);
      return false;
    });
    
    get("/cs/api/dashboard/recFile/:recName", (req, res) -> {
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      if (verifyToken(token) == -1) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String recName = req.params(":recName");
      if (recName == null) {
        res.status(Response.SC_BAD_REQUEST);
        return false;
      }
      File recFile = new File("/RAM/"+recName+".REC");
      if (!recFile.exists()) {
        res.status(Response.SC_BAD_REQUEST);
        return false;
      }
      try {
        ServletOutputStream out = res.raw().getOutputStream();
        FileInputStream fis = new FileInputStream(recFile);
        res.raw().setContentType("application/octet-stream");
        res.header("Content-Disposition", "attachment; filename=\""+recName+".REC\"");
        byte[] buffer = new byte[1024];
        int len;
        while ((len = fis.read(buffer)) > 0) {
          out.write(buffer,0,len);
        }
        fis.close();
        out.flush();
        out.close();
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    });

    get("/cs/api/log", (req, res) -> {
      res.type("application/json");
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      if (permission == -1) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      return DatabaseOperations.getLog();
    });

    get("/cs/api/logClear", (req, res) -> {
      res.type("application/json");
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      if (permission > 2 && permission != 99) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      return DatabaseOperations.clear();
    });

    post("/cs/api/signup", (req, res) -> {
      res.type("application/json");
      JSONObject data = new JSONObject(req.body());
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      if (permission != 0 && permission != 99) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String username = data.getString("username");
      String password = data.getString("password");
      String fullName = data.getString("fullName");
      String userPermission = data.getString("permission");
      if (username == null || password == null || fullName == null || userPermission == null) {
        res.status(Response.SC_BAD_REQUEST);
        return false;
      }
      if (MCUserManager.signup(username, password, userPermission,fullName)) {
        DatabaseOperations.log(token, "user_new;" + username);
        return true;
      }
      return false;
    });
    put("/cs/api/user", (req, res) -> {
      res.type("application/json");
      JSONObject data = new JSONObject(req.body());
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      if (permission == -1) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      String reqUser = getUsernameFromToken(token);
      if (permission != 0 && permission != 99 && !reqUser.equals(data.getString("username"))) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String username = data.getString("username");
      String password = data.getString("password");
      String fullName = data.getString("fullName");
      String userPermission = data.getString("permission");
      if (username == null || fullName == null || userPermission == null || username.equals("super")) {
        res.status(Response.SC_BAD_REQUEST);
        return false;
      }
      if (MCUserManager.edit(username, password, userPermission,fullName)) {
        DatabaseOperations.log(token, "user_edit;" + username);
        return true;
      }
      return false;
    });
    put("/cs/api/license", (req, res) -> {
      res.type("application/json");
      JSONObject data = new JSONObject(req.body());
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      if (permission == -1) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      String reqUser = getUsernameFromToken(token);
      String username = data.getString("username");
      if (!reqUser.equals(username)) { // trying to modify other user
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      return MCUserManager.setLicense(username);
    });
    delete("/cs/api/user/:username", (req, res) -> {
      res.type("application/json");
      String username = req.params(":username");
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      if (permission != 0 && permission != 99) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String reqUser = MCUserManager.getUsernameFromToken(token);
      if (username.equals(reqUser)) {
        return false;
      }
      if(MCUserManager.delete(username)){
        // delete his profile pic
        deleteFolder(new File(PROFILE_PICS + username));
        DatabaseOperations.log(token, "user_del;" + username);
        return true;
      }
      return false;
    });
    
    get("/cs/api/theme", (req, res) -> {
      res.type("application/json");
      return new JSONObject().put("theme", MCThemeManager.theme);
    });
    put("/cs/api/theme/:value", (req, res) -> {
      res.type("application/json");
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      if (permission == -1)
        return false;
      if (permission != 99) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String theme = req.params(":value");
      if (theme == null) {
        res.status(Response.SC_BAD_REQUEST);
        return false;
      }
      MCThemeManager.createThemeFile(theme);
      return true;
    });

    // SAFETY
    post("/cs/api/safety/auth", (req, res) -> {
      res.type("application/json");
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      if (permission == -1) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      String pass = new JSONObject(req.body()).getString("password");
      if (pass == null) {
        res.status(Response.SC_BAD_REQUEST);
        return false;
      }
      return MCUserManager.authSafety(pass);
    });

    post("/cs/api/safety/reset", (req, res) -> {
      res.type("application/json");
      String token = req.headers("Authorization");
      if (token == null) {
        res.status(Response.SC_UNAUTHORIZED);
        return false;
      }
      token = token.split(" ")[1];
      int permission = verifyToken(token);
      if (permission != 0 && permission != 99) {
        res.status(Response.SC_FORBIDDEN);
        return false;
      }
      JSONObject data = new JSONObject(req.body());
      String newPass = data.getString("newPass");
      if (newPass == null || newPass.trim().length() == 0) {
        res.status(Response.SC_BAD_REQUEST);
        return false;
      }
      return MCUserManager.changeSafetyPass(newPass);
    });

  }
}
