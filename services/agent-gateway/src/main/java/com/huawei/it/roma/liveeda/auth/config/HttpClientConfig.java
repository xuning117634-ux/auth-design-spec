package com.huawei.it.roma.liveeda.auth.config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Clock;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    private final AgentGatewayProperties properties;

    public HttpClientConfig(AgentGatewayProperties properties) {
        this.properties = properties;
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RestClient.Builder restClientBuilder(ObjectProvider<RestClientCustomizer> customizers) {
        RestClient.Builder builder = RestClient.builder();
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder;
    }

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return builder -> {
            SimpleClientHttpRequestFactory requestFactory = properties.isInsecureSkipTlsVerify()
                    ? insecureRequestFactory()
                    : new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(5000);
            requestFactory.setReadTimeout(5000);
            builder.requestFactory(requestFactory);
        };
    }

    private SimpleClientHttpRequestFactory insecureRequestFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {new TrustAllX509TrustManager()}, new SecureRandom());
            return new InsecureTlsRequestFactory(sslContext.getSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException exception) {
            throw new IllegalStateException("Failed to initialize insecure TLS request factory", exception);
        }
    }

    private static class InsecureTlsRequestFactory extends SimpleClientHttpRequestFactory {

        private static final HostnameVerifier TRUST_ALL_HOSTNAMES = (hostname, session) -> true;

        private final SSLSocketFactory sslSocketFactory;

        private InsecureTlsRequestFactory(SSLSocketFactory sslSocketFactory) {
            this.sslSocketFactory = sslSocketFactory;
        }

        @Override
        protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
            if (connection instanceof HttpsURLConnection httpsConnection) {
                httpsConnection.setSSLSocketFactory(sslSocketFactory);
                httpsConnection.setHostnameVerifier(TRUST_ALL_HOSTNAMES);
            }
            super.prepareConnection(connection, httpMethod);
        }
    }

    private static class TrustAllX509TrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
