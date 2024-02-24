package project.domaincrm_v1.dao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DatasourceConfig {
  @Value("${spring.datasource.url}")
  private String springDatasourceUrl;

  @Value("${spring.datasource.username}")
  private String springDatasourceUsername;

  @Value("${spring.datasource.password}")
  private String springDatasourcePassword;

  public String gSpringDatasourceUrl() {
    return springDatasourceUrl;
  }
  public String gSpringDatasourceUsername() {
    return springDatasourceUsername;
  }
  public String gSpringDatasourcePassword() {
    return springDatasourcePassword;
  }
}
