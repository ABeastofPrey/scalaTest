package com.servotronix;

import org.json.JSONObject;

public class ErrorResponse extends JSONObject {
  
  public ErrorResponse(String err) {
    super();
    this.put("error", err);
  }
  
}
