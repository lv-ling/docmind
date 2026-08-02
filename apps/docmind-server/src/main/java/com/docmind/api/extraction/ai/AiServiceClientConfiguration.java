package com.docmind.api.extraction.ai;

import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "docmind.ai.enabled", havingValue = "true", matchIfMissing = true)
public class AiServiceClientConfiguration {

  @Bean
  RestClient aiServiceRestClient(RestClient.Builder builder, AiServiceProperties properties) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(properties.connectTimeout())
            .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.readTimeout());
    return builder
        .baseUrl(properties.baseUrl())
        .requestFactory(requestFactory)
        .defaultHeader("X-DocMind-Internal-Token", properties.internalToken())
        .build();
  }
}
