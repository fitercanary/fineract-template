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
package org.apache.fineract.useradministration.data;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.joda.time.LocalDate;

/**
 * Immutable data object for application authorization request data.
 */
public class AuthorizationRequestData {

    private final Long id;
    private final AppUserData user;
    private final ClientData client;
    private final EnumOptionData status;
    private final LocalDate requestedDate;
    
    private final AppUserData approvedBy;
    private final LocalDate approvedDate;
    
    private final AppUserData rejectedBy;
    private final LocalDate rejectionDate;
    
    private final String comment;
    
    public AuthorizationRequestData(final Long id, final AppUserData user, final ClientData client, final EnumOptionData status,
            final LocalDate requestedDate, final AppUserData approvedBy, final LocalDate approvedDate, final AppUserData rejectedBy,
            final LocalDate rejectionDate, final String comment) {
        this.id = id;
        this.user = user;
        this.client = client;
        this.status = status;
        this.requestedDate = requestedDate;
        this.approvedBy = approvedBy;
        this.approvedDate = approvedDate;
        this.rejectedBy = rejectedBy;
        this.rejectionDate = rejectionDate;
        this.comment = comment;
        
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuthorizationRequestData that = (AuthorizationRequestData) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    
    public Long getId() {
        return this.id;
    }

    
    public AppUserData getUser() {
        return this.user;
    }

    
    public ClientData getClient() {
        return this.client;
    }

    
    public EnumOptionData getStatus() {
        return this.status;
    }

    
    public LocalDate getRequestedDate() {
        return this.requestedDate;
    }

    
    public AppUserData getApprovedBy() {
        return this.approvedBy;
    }

    
    public LocalDate getApprovedDate() {
        return this.approvedDate;
    }

    
    public AppUserData getRejectedBy() {
        return this.rejectedBy;
    }

    
    public LocalDate getRejectionDate() {
        return this.rejectionDate;
    }

    
    public String getComment() {
        return this.comment;
    }
    
    
}
