package com.docmind.api.identity.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "docmind.bootstrap", name = "enabled", havingValue = "true")
public class LocalBootstrapInitializer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(LocalBootstrapInitializer.class);

  private final BootstrapProperties properties;
  private final LocalBootstrapService bootstrapService;

  public LocalBootstrapInitializer(
      BootstrapProperties properties, LocalBootstrapService bootstrapService) {
    this.properties = properties;
    this.bootstrapService = bootstrapService;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    bootstrapService.initialize(properties);
    log.info("local_identity_bootstrap_ready");
  }
}
