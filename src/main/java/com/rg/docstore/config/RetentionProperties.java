package com.rg.docstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "retention")
public record RetentionProperties(
    Integer keepLastNVersions,
    String bucketZone,
    List<TierSpec> tiers
) {
  public record TierSpec(String window, String bucket) {}
}
