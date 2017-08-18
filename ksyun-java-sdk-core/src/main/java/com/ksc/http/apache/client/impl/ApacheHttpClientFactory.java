/*
 * Copyright (c) 2016. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://ksyun.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.ksc.http.apache.client.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import com.ksc.http.KSCHttpClient;
import com.ksc.http.IdleConnectionReaper;
import com.ksc.http.apache.SdkProxyRoutePlanner;
import com.ksc.http.apache.utils.ApacheUtils;
import com.ksc.http.client.ConnectionManagerFactory;
import com.ksc.http.client.HttpClientFactory;
import com.ksc.http.conn.ClientConnectionManagerFactory;
import com.ksc.http.conn.SdkConnectionKeepAliveStrategy;
import com.ksc.http.protocol.SdkHttpRequestExecutor;
import com.ksc.http.settings.HttpClientSettings;

/**
 * Factory class that builds the apache http client from the settings.
 */
public class ApacheHttpClientFactory implements HttpClientFactory<ConnectionManagerAwareHttpClient> {

    private static final Log LOG = LogFactory.getLog(KSCHttpClient.class);
    private final ConnectionManagerFactory<HttpClientConnectionManager>
            cmFactory = new ApacheConnectionManagerFactory();

    @Override
    public ConnectionManagerAwareHttpClient create(HttpClientSettings settings) {
        final HttpClientBuilder builder = HttpClients.custom();
        // Note that it is important we register the original connection manager with the
        // IdleConnectionReaper as it's required for the successful deregistration of managers
        final HttpClientConnectionManager cm = cmFactory.create(settings);

        builder.setRequestExecutor(new SdkHttpRequestExecutor())
                .setKeepAliveStrategy(buildKeepAliveStrategy(settings))
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .setConnectionManager(ClientConnectionManagerFactory.wrap(cm));

        // By default http client enables Gzip compression. So we disable it
        // here.
        // Apache HTTP client removes Content-Length, Content-Encoding and
        // Content-MD5 headers when Gzip compression is enabled. Currently
        // this doesn't affect S3 or Glacier which exposes these headers.
        //
        if (!(settings.useGzip())) {
            builder.disableContentCompression();
        }

        addProxyConfig(builder, settings);

        final ConnectionManagerAwareHttpClient httpClient = new SdkHttpClient(builder.build(), cm);

        if (settings.useReaper()) {
            IdleConnectionReaper.registerConnectionManager(cm);
        }

        return httpClient;
    }

    private void addProxyConfig(HttpClientBuilder builder,
                                HttpClientSettings settings) {
        if (isProxyEnabled(settings)) {

            LOG.info("Configuring Proxy. Proxy Host: " + settings.getProxyHost() + " " +
                    "Proxy Port: " + settings.getProxyPort());

            builder.setRoutePlanner(new SdkProxyRoutePlanner(
                    settings.getProxyHost(), settings.getProxyPort(), settings.getNonProxyHosts()));

            if (isAuthenticatedProxy(settings)) {
                builder.setDefaultCredentialsProvider(ApacheUtils
                        .newProxyCredentialsProvider(settings));
            }
        }
    }

    private ConnectionKeepAliveStrategy buildKeepAliveStrategy
            (HttpClientSettings settings) {
        return settings.getMaxIdleConnectionTime() > 0
                ? new SdkConnectionKeepAliveStrategy(settings.getMaxIdleConnectionTime())
                : null;
    }

    private boolean isAuthenticatedProxy(HttpClientSettings settings) {
        return settings.getProxyUsername() != null
                && settings.getProxyPassword() != null;
    }

    private boolean isProxyEnabled(HttpClientSettings settings) {
        return settings.getProxyHost() != null && settings.getProxyPort() > 0;
    }
}