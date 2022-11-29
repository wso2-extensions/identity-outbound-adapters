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

package org.wso2.identity.outbound.adapter.websubhub.model;

/**
 * Model Class for Event Payload.
 */
public abstract class EventPayload {

    private int organizationId;
    private String organizationName;
    private String ref;

    public String getRef() {

        return ref;
    }
    public void setRef(String ref) {

        this.ref = ref;
    }

    public int getOrganizationId() {

        return organizationId;
    }

    public void setOrganizationId(int organizationId) {

        this.organizationId = organizationId;
    }

    public String getOrganizationName() {

        return organizationName;
    }

    public void setOrganizationName(String organizationName) {

        this.organizationName = organizationName;
    }
}
