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

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Embeddable;

@Embeddable
public class ClientUserId implements Serializable {

    private static final long serialVersionUID = 1L;
    
    Long clientId;
    Long userId;
    
    public ClientUserId(Long clientId, Long userId) {
        super();
        
        this.clientId = clientId;
        this.userId = userId;
        
    }
    
    public ClientUserId() {
        super();
    }

    public Long getClientId() {
        return this.clientId;
    }
    
    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.clientId == null) ? 0 : this.clientId.hashCode());
        result = prime * result + ((this.userId == null) ? 0 : this.userId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ClientUserId other = (ClientUserId) obj;
        return Objects.equals(getClientId(), other.getClientId()) && Objects.equals(getUserId(), other.getUserId());
    
    }
    
    
}
