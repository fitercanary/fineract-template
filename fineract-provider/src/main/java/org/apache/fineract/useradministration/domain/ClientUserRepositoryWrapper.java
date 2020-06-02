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

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientUserRepositoryWrapper {

    private final ClientUserRepository clientUserRepository;
    
    @Autowired
    public ClientUserRepositoryWrapper(final ClientUserRepository clientUserRepository) {
        this.clientUserRepository = clientUserRepository;
    }
    
    public void save(ClientUser clientUser) {
        this.clientUserRepository.save(clientUser);
    }
    
    public ClientUser findClientUserByUserIdAndClientId(Long userId, Long clientId) {
        return this.clientUserRepository.findByIdClientIdAndIdUserId(clientId, userId);
    }
    
    public ClientUser findClientUserByUserIdAndClientIdAndExpiry(Long userId, Long clientId, boolean isExpired) {
        return this.clientUserRepository.findByIdClientIdAndIdUserIdAndIsExpired(clientId, userId, isExpired);
    }
    
    public ClientUser findByIdClientIdAndIdUserIdAndEndTimeBefore(Long userId, Long clientId, Date endTime) {
        return this.clientUserRepository.findByIdClientIdAndIdUserIdAndEndTimeBefore(clientId, userId, endTime);
    }
    
    public ClientUser findByIdClientIdAndIdUserIdAndEndTimeAfter(Long userId, Long clientId, Date endTime) {
        return this.clientUserRepository.findByIdClientIdAndIdUserIdAndEndTimeAfter(clientId, userId, endTime);
    }
}
