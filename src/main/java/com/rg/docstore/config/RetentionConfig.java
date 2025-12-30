package com.rg.docstore.config;

import com.rg.retention.RetentionPolicy;
import com.rg.retention.RetentionPolicyBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RetentionProperties.class)
public class RetentionConfig {

  @Bean
  public RetentionPolicy retentionPolicy(RetentionProperties props) {
    var b = RetentionPolicyBuilder.builder()
        .keepLastNVersions(props.keepLastNVersions() == null ? 10 : props.keepLastNVersions());

    if (props.tiers() != null) {
      for (var t : props.tiers()) {
        if (t == null) continue;
        b.addTier(t.window(), t.bucket());
      }
    }

    return b.build();
  }
}
