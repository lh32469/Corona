package org.gpc4j.corona.beans;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.annotation.ApplicationScope;
import org.springframework.web.context.annotation.SessionScope;

import javax.inject.Named;
import javax.inject.Singleton;

@Named("rBean")
@Singleton
public class RavenBean {

  private static final Logger LOG
      = LoggerFactory.getLogger(RavenBean.class);

  final IDocumentStore docStore;

  public RavenBean() {
    docStore = new DocumentStore(
        "http://dell-4290.local:5050", "Corona");
    docStore.initialize();
    LOG.info("Created...");
  }

  public IDocumentSession getSession() {
    return docStore.openSession();
  }

}
