package project.domaincrm_v1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import project.domaincrm_v1.config.UiConfig;

@RestController
public class UiController {

  @Autowired
  private UiConfig uiConfig;

  @GetMapping("/appsettings.js")
  public ResponseEntity<?> appSettingsJs() {
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "text/javascript");

    return ResponseEntity.ok()
      .headers(responseHeaders)
      .body("window.__APP_SETTINGS = " + uiConfig.gAppSettingsJson().toJSONString());
  }
}
