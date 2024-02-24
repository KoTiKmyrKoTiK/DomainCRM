package project.domaincrm_v1.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import net.minidev.json.JSONObject;

@ConfigurationProperties(prefix = "app.config.ui")
@Component
public class UiConfig {
  private Map<String, String> appSettingsMap = new HashMap<>();

  public String getBaseUrl() {
    return appSettingsMap.get("baseUrl");
  }

  public void setBaseUrl(String baseUrl) {
    this.appSettingsMap.put("baseUrl", baseUrl);
  }

  public JSONObject gAppSettingsJson() {
    return new JSONObject(appSettingsMap);
  }
}