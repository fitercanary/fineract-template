/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.useradministration.domain;

public enum AuthorizationRequestStatusType {

    INVALID(0, "requestAuthorizationStatusType.invalid", "invalid"), //
    SUBMITTED_AND_PENDING_APPROVAL(100, "requestAuthorizationStatusType.submitted.and.pending.approval", "Pending"), //
    APPROVED(200, "requestAuthorizationStatusType.approved", "Approved"), //
    ACTIVE(300, "requestAuthorizationStatusType.active", "Active"), //
    REJECTED(500, "requestAuthorizationStatusType.rejected", "Rejected"), //
    CLOSED(600, "requestAuthorizationStatusType.closed", "Closed");
    
    private final Integer value;
    private final String code;
    private final String name;

    public static AuthorizationRequestStatusType fromInt(final Integer type) {
        
        AuthorizationRequestStatusType enumeration = AuthorizationRequestStatusType.INVALID;
        
        switch (type) {
            case 100:
                enumeration = AuthorizationRequestStatusType.SUBMITTED_AND_PENDING_APPROVAL;
            break;
            case 200:
                enumeration = AuthorizationRequestStatusType.APPROVED;
            break;
            case 300:
                enumeration = AuthorizationRequestStatusType.ACTIVE;
            break;
            case 500:
                enumeration = AuthorizationRequestStatusType.REJECTED;
            break;
        }
        return enumeration;
    }
    
    private AuthorizationRequestStatusType(final Integer value, final String code, final String name) {
        this.value = value;
        this.code = code;
        this.name = name;
    }

    public boolean hasStateOf(final AuthorizationRequestStatusType state) {
        return this.value.equals(state.getValue());
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }
    
    public String getName() {
        return this.name;
    }

    public boolean isSubmittedAndPendingApproval() {
        return this.value.equals(AuthorizationRequestStatusType.SUBMITTED_AND_PENDING_APPROVAL.getValue());
    }

    public boolean isApproved() {
        return this.value.equals(AuthorizationRequestStatusType.APPROVED.getValue());
    }

    public boolean isRejected() {
        return this.value.equals(AuthorizationRequestStatusType.REJECTED.getValue());
    }

    public boolean isActive() {
        return this.value.equals(AuthorizationRequestStatusType.ACTIVE.getValue());
    }
}
