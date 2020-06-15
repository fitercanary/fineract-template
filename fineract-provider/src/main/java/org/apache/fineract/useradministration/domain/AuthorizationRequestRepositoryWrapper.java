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

import org.apache.fineract.useradministration.exception.AuthorizationRequestNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorizationRequestRepositoryWrapper {

    private final AuthorizationRequestRepository authorizationRequestRepository;

    @Autowired
    public AuthorizationRequestRepositoryWrapper(final AuthorizationRequestRepository authorizationRequestRepository) {
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    public void save(AuthorizationRequest authorizationRequest) {
        this.authorizationRequestRepository.save(authorizationRequest);
    }

    public void update(AuthorizationRequest authorizationRequest) {
        this.authorizationRequestRepository.saveAndFlush(authorizationRequest);
    }
    
    public AuthorizationRequest findOneWithNotFoundDetection(Long id) {
        final AuthorizationRequest authorizationRequest = this.authorizationRequestRepository.findOne(id);
        if (authorizationRequest == null) {
            throw new AuthorizationRequestNotFoundException(id);
        }
        return authorizationRequest;
    }

    public List<AuthorizationRequest> findAuthorizationRequestsByStatus(Integer status) {
        return this.authorizationRequestRepository.findAllByStatus(status);
    }

    public List<AuthorizationRequest> findUserClientRequestsByStatus(Long clientId, Long userId, Integer status) {
        return this.authorizationRequestRepository.findAllByClientAndUserAndStatus(clientId, userId, status);
    }
    
    public List<AuthorizationRequest> findUserRequestsByStatus(Long userId, Integer status) {
        return this.authorizationRequestRepository.findAllByUserAndStatus(userId, status);
    }

    public List<AuthorizationRequest> findClientRequestsByStatus(Long clientId, Integer status) {
        return this.authorizationRequestRepository.findAllByClientAndStatus(clientId, status);
    }

}
