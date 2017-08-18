/*
 * Copyright 2010-2016 ksyun.com, Inc. or its affiliates. All Rights Reserved.
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
package com.ksc.retry;

import org.apache.http.HttpStatus;

import com.ksc.KscServiceException;
import com.ksc.annotation.SdkInternalApi;

import java.util.Random;
import java.util.HashSet;
import java.util.Set;

public class RetryUtils {

    private static final Set<String> THROTTLING_ERROR_CODES = new HashSet<String>(9);
    private static final Set<String> CLOCK_SKEW_ERROR_CODES = new HashSet<String>(6);

    static {
        THROTTLING_ERROR_CODES.add("Throttling");
        THROTTLING_ERROR_CODES.add("ThrottlingException");
        THROTTLING_ERROR_CODES.add("ProvisionedThroughputExceededException");
        THROTTLING_ERROR_CODES.add("SlowDown");
        THROTTLING_ERROR_CODES.add("TooManyRequestsException");
        THROTTLING_ERROR_CODES.add("LimitExceededException");
        THROTTLING_ERROR_CODES.add("RequestLimitExceeded");
        THROTTLING_ERROR_CODES.add("BandwidthLimitExceeded");
        THROTTLING_ERROR_CODES.add("RequestThrottled");

        CLOCK_SKEW_ERROR_CODES.add("RequestTimeTooSkewed");
        CLOCK_SKEW_ERROR_CODES.add("RequestExpired");
        CLOCK_SKEW_ERROR_CODES.add("InvalidSignatureException");
        CLOCK_SKEW_ERROR_CODES.add("SignatureDoesNotMatch");
        CLOCK_SKEW_ERROR_CODES.add("AuthFailure");
        CLOCK_SKEW_ERROR_CODES.add("RequestInTheFuture");
    }

    /**
     * Returns true if the specified exception is a retryable service side exception.
     *
     * @param ase The exception to test.
     * @return True if the exception resulted from a retryable service error, otherwise false.
     */
    public static boolean isRetryableServiceException(KscServiceException ase) {
        if (ase == null) {
            return false;
        }
        final int statusCode = ase.getStatusCode();
        return statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR ||
               statusCode == HttpStatus.SC_BAD_GATEWAY ||
               statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE ||
               statusCode == HttpStatus.SC_GATEWAY_TIMEOUT;
    }

    /**
     * Returns true if the specified exception is a throttling error.
     *
     * @param ase The exception to test.
     * @return True if the exception resulted from a throttling error message from a service,
     * otherwise false.
     */
    public static boolean isThrottlingException(KscServiceException ase) {
        if (ase == null) {
            return false;
        }
        return THROTTLING_ERROR_CODES.contains(ase.getErrorCode());
    }

    /**
     * Returns true if the specified exception is a request entity too large error.
     *
     * @param ase The exception to test.
     * @return True if the exception resulted from a request entity too large error message from a
     * service, otherwise false.
     */
    public static boolean isRequestEntityTooLargeException(KscServiceException ase) {
        return ase != null && ase.getStatusCode() == HttpStatus.SC_REQUEST_TOO_LONG;
    }

    /**
     * Returns true if the specified exception is a clock skew error.
     *
     * @param ase The exception to test.
     * @return True if the exception resulted from a clock skews error message from a service,
     * otherwise false.
     */
    public static boolean isClockSkewError(KscServiceException ase) {
        if (ase == null) {
            return false;
        }
        return CLOCK_SKEW_ERROR_CODES.contains(ase.getErrorCode());
    }

    @SdkInternalApi
    static int calculateFullJitterBackoff(int retriesAttempted,
                                          int baseDelay,
                                          int maxBackoffTime,
                                          Random random) {
        int delayUpperBound = Math.min(baseDelay * (1 << retriesAttempted), maxBackoffTime);
        return random.nextInt(delayUpperBound + 1);
    }
}
