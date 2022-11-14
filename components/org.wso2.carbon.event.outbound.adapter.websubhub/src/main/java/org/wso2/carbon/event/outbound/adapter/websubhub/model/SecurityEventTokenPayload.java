/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.event.outbound.adapter.websubhub.model;

import java.util.Map;

/**
 * Model class for Security Event Token Payload.
 */
public class SecurityEventTokenPayload {

    private String iss;
    private String jti;
    private long iat;
    private String aud;
    private Map<String, EventPayload> event;

    public String getIss() {

        return iss;
    }

    public void setIss(String iss) {

        this.iss = iss;
    }

    public String getJti() {

        return jti;
    }

    public void setJti(String jti) {

        this.jti = jti;
    }

    public long getIat() {

        return iat;
    }

    public void setIat(long iat) {

        this.iat = iat;
    }

    public String getAud() {

        return aud;
    }

    public void setAud(String aud) {

        this.aud = aud;
    }

    public Map<String, EventPayload> getEvent() {

        return event;
    }

    public void setEvent(Map<String, EventPayload> event) {

        this.event = event;
    }
}
