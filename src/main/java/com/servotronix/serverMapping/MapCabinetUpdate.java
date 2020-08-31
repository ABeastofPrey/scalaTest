package com.servotronix.serverMapping;

import com.servotronix.MCInfo.MCInfo;
import com.servotronix.mcwebserver.FreeMarkerEngine;

import org.apache.commons.io.IOUtils;

import MCAuth.TokenManager;
import spark.ModelAndView;

import static spark.Spark.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MapCabinetUpdate {

  public static void map(FreeMarkerEngine engine, MCInfo info) {

    after("/cabinet-update/*", (req,res)->{
      if (res.type() != null || res.body() != null)
        return;
      String token = TokenManager.generateToken(); // TO AUTHENTICATE THIS SESSION
      if (token == null)
        return;
      Map<String, Object> attributes = new HashMap<>();
      req.session().attribute("securityToken",token);
      res.cookie("token", token);
      attributes.put("token", token);
      res.body(engine.render(new ModelAndView(attributes, "cabinet_update.html")));
      res.status(200);
    });

    get("/cabinet-update/", (req, res) -> {
      Map<String, Object> attributes = new HashMap<>();
      String token = TokenManager.generateToken();
      req.session().attribute("securityToken",token);
      res.cookie("token", token);
      attributes.put("token", token);
      return engine.render(new ModelAndView(attributes, "cabinet_update.html"));
    });

  }
  
}