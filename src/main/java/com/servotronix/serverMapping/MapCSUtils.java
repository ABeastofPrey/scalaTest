package com.servotronix.serverMapping;

import MCAuth.TokenManager;
import com.servotronix.MCInfo.MCInfo;
import com.servotronix.mcwebserver.FreeMarkerEngine;
import java.util.HashMap;
import java.util.Map;
import spark.ModelAndView;
import static spark.Spark.get;

public class MapCSUtils {
  
  public static void map(FreeMarkerEngine engine, MCInfo info) {

    get("/modbus/", (req, res) -> {
      Map<String, Object> attributes = new HashMap<>();
      String token = TokenManager.generateToken();
      req.session().attribute("securityToken",token);
      res.cookie("token", token);
      attributes.put("token", token);
      return engine.render(new ModelAndView(attributes, "modbus.html"));
    });
  }
  
}
