package org.gpc4j.corona.beans;

import org.springframework.web.context.annotation.SessionScope;

import javax.inject.Named;

@Named("sBean")
@SessionScope
public class SessionBean {

  private String include;
  private String exclude;

  public String getInclude() {
    return include;
  }

  public void setInclude(String include) {
    this.include = include;
  }

  public String getExclude() {
    return exclude;
  }

  public void setExclude(String exclude) {
    this.exclude = exclude;
  }
}
