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
package com.ksc.transform;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonToken;

public class ListUnmarshaller<T> implements
        Unmarshaller<List<T>, JsonUnmarshallerContext> {

    private final Unmarshaller<T, JsonUnmarshallerContext> itemUnmarshaller;

    public ListUnmarshaller(
            Unmarshaller<T, JsonUnmarshallerContext> itemUnmarshaller) {
        this.itemUnmarshaller = itemUnmarshaller;
    }

    /**
     * Unmarshalls the response headers or the json doc in the payload to the
     * list
     */
    public List<T> unmarshall(JsonUnmarshallerContext context) throws Exception {
        if (context.isInsideResponseHeader()) {
            return unmarshallResponseHeaderToList(context);
        }
        return unmarshallJsonToList(context);
    }

    /**
     * Un marshalls the response header into the list.
     */
    private List<T> unmarshallResponseHeaderToList(
            JsonUnmarshallerContext context) throws Exception {

        String headerValue = context.readText();

        List<T> list = new ArrayList<T>();

        String[] headerValues = headerValue.split("[,]");

        for (final String headerVal : headerValues) {
            list.add(itemUnmarshaller.unmarshall(new JsonUnmarshallerContext() {
                @Override
                public String readText() {
                    return headerVal;
                }
            }));
        }
        return list;
    }

    /**
     * Unmarshalls the current token in the Json document to list.
     */
    private List<T> unmarshallJsonToList(JsonUnmarshallerContext context)
            throws Exception {
        List<T> list = new ArrayList<T>();

        int originalDepth = context.getCurrentDepth();

        if (context.getCurrentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        while (true) {
            JsonToken token = context.nextToken();
            if (token == null)
                return list;

            if (token == JsonToken.START_ARRAY) {
                continue;
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getCurrentDepth() < originalDepth)
                    return list;
            } else {
                list.add(itemUnmarshaller.unmarshall(context));
            }
        }
    }
}
