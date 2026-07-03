package cc.synx.config;

import cc.synx.api.AuthApiClient;
import cc.synx.api.LogApiClient;
import cc.synx.api.MessageAdminApiClient;
import cc.synx.api.MessageApiClient;
import cc.synx.api.UserAdminApiClient;
import cc.synx.api.UserClient;
import cc.synx.api.ValidateClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class WebClientConfig {

    @Bean
    public UserClient initWebClient(
            @Value("${topbiz.clients.legacy-user-url:http://localhost:8080}") String baseUrl) {
        return client(baseUrl, UserClient.class);
    }

    @Bean
    public ValidateClient initValidateClient(
            @Value("${topbiz.clients.validate-url:http://localhost:8081}") String baseUrl) {
        return client(baseUrl, ValidateClient.class);
    }

    @Bean
    public UserAdminApiClient userAdminApiClient(
            @Value("${topbiz.clients.user-service-url:http://localhost:18081}") String baseUrl) {
        return client(baseUrl, UserAdminApiClient.class);
    }

    @Bean
    public AuthApiClient authApiClient(
            @Value("${topbiz.clients.user-service-url:http://localhost:18081}") String baseUrl) {
        return client(baseUrl, AuthApiClient.class);
    }

    @Bean
    public MessageApiClient messageApiClient(
            @Value("${topbiz.clients.msg-service-url:http://localhost:18082}") String baseUrl) {
        return client(baseUrl, MessageApiClient.class);
    }

    @Bean
    public MessageAdminApiClient messageAdminApiClient(
            @Value("${topbiz.clients.msg-service-url:http://localhost:18082}") String baseUrl) {
        return client(baseUrl, MessageAdminApiClient.class);
    }

    @Bean
    public LogApiClient logApiClient(
            @Value("${topbiz.clients.log-service-url:http://localhost:18083}") String baseUrl) {
        return client(baseUrl, LogApiClient.class);
    }

    private <T> T client(String baseUrl, Class<T> clientType) {
        WebClient client = WebClient.builder().baseUrl(baseUrl).build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(client))
                .build();
        return factory.createClient(clientType);
    }
}
