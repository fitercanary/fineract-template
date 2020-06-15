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

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorizationRequestRepository extends JpaRepository<AuthorizationRequest, Long>, JpaSpecificationExecutor<AuthorizationRequest>{

    List<AuthorizationRequest> findAllByStatus(Integer status);
    
    @Query("SELECT ar FROM AuthorizationRequest ar WHERE ar.client.id = :clientId AND ar.user.id = :userId AND ar.status = :status")
    List<AuthorizationRequest> findAllByClientAndUserAndStatus(@Param("clientId") Long clientId, @Param("userId") Long userId, @Param("status") Integer status);
    
    @Query("SELECT ar FROM AuthorizationRequest ar WHERE ar.user.id = :userId AND ar.status = :status")
    List<AuthorizationRequest> findAllByUserAndStatus( @Param("userId") Long userId, @Param("status") Integer status);

    @Query("SELECT ar FROM AuthorizationRequest ar WHERE ar.client.id = :clientId AND  ar.status = :status")
    List<AuthorizationRequest> findAllByClientAndStatus(@Param("clientId") Long clientId, @Param("status") Integer status);

}
