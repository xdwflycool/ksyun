/*
 * Copyright 2011-2016 ksyun.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://ksyun.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.ksc.http;

import java.net.URI;
import java.util.List;

import org.apache.http.annotation.NotThreadSafe;

import com.ksc.KscWebServiceClient;
import com.ksc.auth.AWSCredentialsProvider;
import com.ksc.auth.Signer;
import com.ksc.auth.credentials.AWSCredentials;
import com.ksc.handlers.RequestHandler2;
import com.ksc.http.timers.client.ClientExecutionAbortTrackerTask;
import com.ksc.retry.internal.AuthErrorRetryStrategy;
import com.ksc.util.KscRequestMetrics;
import com.ksc.util.KscRequestMetricsFullSupport;

/**
 * @NotThreadSafe This class should only be accessed by a single thread and be used throughout
 *                a single request lifecycle.
 */
@NotThreadSafe
public class ExecutionContext {
    private final KscRequestMetrics kscRequestMetrics;
    private final List<RequestHandler2> requestHandler2s;
    private final KscWebServiceClient kscClient;

    private boolean retryCapacityConsumed;

    /**
     * Optional credentials to enable the runtime layer to handle signing requests (and resigning on
     * retries).
     */
    private AWSCredentialsProvider credentialsProvider;

    /**
     * An internal retry strategy for auth errors. This is currently only used by the S3 client for
     * auto-resolving V4-required regions.
     */
    private AuthErrorRetryStrategy authErrorRetryStrategy;

    private ClientExecutionAbortTrackerTask clientExecutionTrackerTask;

    /** For testing purposes. */
    public ExecutionContext(boolean isMetricEnabled) {
        this(null, isMetricEnabled, null);
    }

    /** For testing purposes. */
    public ExecutionContext() {
        this(null, false, null);
    }

    public ExecutionContext(List<RequestHandler2> requestHandler2s, boolean isMetricEnabled,
            KscWebServiceClient kscClient) {
        this.requestHandler2s = requestHandler2s;
        kscRequestMetrics = isMetricEnabled ? new KscRequestMetricsFullSupport() : new KscRequestMetrics();
        this.kscClient = kscClient;
    }

    public List<RequestHandler2> getRequestHandler2s() {
        return requestHandler2s;
    }

    public KscRequestMetrics getKscRequestMetrics() {
        return kscRequestMetrics;
    }

    protected KscWebServiceClient getKscClient() {
        return kscClient;
    }

    /**
     * There is in general no need to set the signer in the execution context, since the signer for
     * each request may differ depending on the URI of the request. The exception is S3 where the
     * signer is currently determined only when the S3 client is constructed. Hence the need for
     * this method. We may consider supporting a per request level signer determination for S3 later
     * on.
     */
    public void setSigner(Signer signer) {
    }

    /**
     * Returns whether retry capacity was consumed during this request lifecycle.
     * This can be inspected to determine whether capacity should be released if a retry succeeds.
     *
     * @return true if retry capacity was consumed
     */
    public boolean retryCapacityConsumed() { return retryCapacityConsumed; }

    /**
     * Marks that a retry during this request lifecycle has consumed retry capacity.  This is inspected
     * when determining if capacity should be released if a retry succeeds.
     */
    public void markRetryCapacityConsumed() {
        this.retryCapacityConsumed = true;
    }

    /**
     * Returns the signer for the given uri. Note S3 in particular overrides this method.
     */
    public Signer getSignerByURI(URI uri) {
        return kscClient == null ? null : kscClient.getSignerByURI(uri);
    }

    /**
     * Sets the credentials provider used for fetching the credentials. The credentials fetched is
     * used for signing the request. If there is no credential provider, then the runtime will not
     * attempt to sign (or resign on retries) requests.
     *
     * @param credentialsProvider
     *            the credentials provider to fetch {@link AWSCredentials}
     */
    public void setCredentialsProvider(AWSCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    /**
     * Returns the credentials provider used for fetching the credentials. The credentials fetched
     * is used for signing the request. If there is no credential provider, then the runtime will
     * not attempt to sign (or resign on retries) requests.
     *
     * @return the credentials provider to fetch {@link AWSCredentials}
     */
    public AWSCredentialsProvider getCredentialsProvider() {
        return this.credentialsProvider;
    }

    /**
     * Returns the retry strategy for auth errors. This is currently only used by the S3 client for
     * auto-resolving sigv4-required regions.
     * <p>
     * Note that this will be checked BEFORE the HTTP client consults the user-specified
     * RetryPolicy. i.e. if the configured AuthErrorRetryStrategy says the request should be
     * retried, the retry will be performed internally and the effect is transparent to the user's
     * RetryPolicy.
     */
    public AuthErrorRetryStrategy getAuthErrorRetryStrategy() {
        return authErrorRetryStrategy;
    }

    /**
     * Sets the optional auth error retry strategy for this request execution.
     *
     * @see #getAuthErrorRetryStrategy()
     */
    public void setAuthErrorRetryStrategy(AuthErrorRetryStrategy authErrorRetryStrategy) {
        this.authErrorRetryStrategy = authErrorRetryStrategy;
    }

    public ClientExecutionAbortTrackerTask getClientExecutionTrackerTask() {
        return clientExecutionTrackerTask;
    }

    public void setClientExecutionTrackerTask(ClientExecutionAbortTrackerTask clientExecutionTrackerTask) {
        this.clientExecutionTrackerTask = clientExecutionTrackerTask;
    }

}
