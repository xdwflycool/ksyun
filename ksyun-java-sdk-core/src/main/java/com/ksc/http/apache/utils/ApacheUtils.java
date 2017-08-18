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
package com.ksc.http.apache.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;

import com.ksc.KscClientException;
import com.ksc.http.settings.HttpClientSettings;
import com.ksc.util.FakeIOException;

public class ApacheUtils {

    /**
     * Utility function for creating a new StringEntity and wrapping any errors
     * as an AmazonClientException.
     *
     * @param s The string contents of the returned HTTP entity.
     * @return A new StringEntity with the specified contents.
     */
    public static HttpEntity newStringEntity(String s) {
        try {
            return new StringEntity(s);
        } catch (UnsupportedEncodingException e) {
            throw new KscClientException("Unable to create HTTP entity: " + e.getMessage(), e);
        }
    }

    /**
     * Utility function for creating a new BufferedEntity and wrapping any errors
     * as an AmazonClientException.
     *
     * @param entity The HTTP entity to wrap with a buffered HTTP entity.
     * @return A new BufferedHttpEntity wrapping the specified entity.
     * @throws FakeIOException only for test simulation
     */
    public static HttpEntity newBufferedHttpEntity(HttpEntity entity) throws
            FakeIOException {
        try {
            return new BufferedHttpEntity(entity);
        } catch (FakeIOException e) {
            // Only for test simulation.
            throw e;
        } catch (IOException e) {
            throw new KscClientException("Unable to create HTTP entity: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a new HttpClientContext used for request execution.
     */
    public static HttpClientContext newClientContext(HttpClientSettings settings,
                                                     Map<String, ? extends Object>
                                                             attributes) {
        final HttpClientContext clientContext = new HttpClientContext();

        if (attributes != null && !attributes.isEmpty()) {
            for (Map.Entry<String, ?> entry : attributes.entrySet()) {
                clientContext.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        addPreemptiveAuthenticationProxy(clientContext, settings);
        return clientContext;

    }

    /**
     * Returns a new Credentials Provider for use with proxy authentication.
     */
    public static CredentialsProvider newProxyCredentialsProvider
    (HttpClientSettings settings) {
        final CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(newAuthScope(settings), newNTCredentials(settings));
        return provider;
    }

    /**
     * Returns a new instance of NTCredentials used for proxy authentication.
     */
    private static Credentials newNTCredentials(HttpClientSettings settings) {
        return new NTCredentials(settings.getProxyUsername(),
                settings.getProxyPassword(),
                settings.getProxyWorkstation(),
                settings.getProxyDomain());
    }

    /**
     * Returns a new instance of AuthScope used for proxy authentication.
     */
    private static AuthScope newAuthScope(HttpClientSettings settings) {
        return new AuthScope(settings.getProxyHost(), settings.getProxyPort());
    }

    private static void addPreemptiveAuthenticationProxy(HttpClientContext clientContext,
                                                         HttpClientSettings settings) {

        if (settings.isPreemptiveBasicProxyAuth()) {
            HttpHost targetHost = new HttpHost(settings.getProxyHost(), settings
                    .getProxyPort());
            final CredentialsProvider credsProvider = newProxyCredentialsProvider(settings);
            // Create AuthCache instance
            AuthCache authCache = new BasicAuthCache();
            // Generate BASIC scheme object and add it to the local auth cache
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(targetHost, basicAuth);

            clientContext.setCredentialsProvider(credsProvider);
            clientContext.setAuthCache(authCache);
        }
    }
}
