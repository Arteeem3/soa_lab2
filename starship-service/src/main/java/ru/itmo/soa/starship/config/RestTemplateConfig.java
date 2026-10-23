package ru.itmo.soa.starship.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;

@Slf4j
@Configuration
public class RestTemplateConfig {

    @Value("${client.ssl.truststore:classpath:ssl/client-truststore.p12}")
    private Resource truststoreResource;

    @Value("${client.ssl.truststore-password:password}")
    private String truststorePassword;

    @Value("${client.http.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${client.http.read-timeout-ms:5000}")
    private int readTimeoutMs;

    @Bean
    public RestTemplate restTemplate() throws Exception {
        SSLContextBuilder sslBuilder = SSLContextBuilder.create();

        if (truststoreResource != null && truststoreResource.exists()) {
            log.info("Loading SSL TrustStore from {}", truststoreResource.getDescription());
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = truststoreResource.getInputStream()) {
                trustStore.load(is, truststorePassword.toCharArray());
            }
            sslBuilder.loadTrustMaterial(trustStore, null);
        } else {
            log.warn("TrustStore resource not found at {}, falling back to default JVM truststore", truststoreResource);
        }

        SSLContext sslContext = sslBuilder.build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        return new RestTemplate(factory);
    }
}
