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
package org.apache.fineract.batch.command.internal;

import javax.ws.rs.core.UriInfo;

import org.apache.fineract.batch.command.CommandStrategy;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.fineract.batch.exception.ErrorHandler;
import org.apache.fineract.batch.exception.ErrorInfo;
import org.apache.fineract.portfolio.loanaccount.api.LoansApiResource;
import org.apache.fineract.useradministration.api.UsersApiResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implements {@link org.apache.fineract.batch.command.CommandStrategy} to handle
 * approve authorization request to view a client. It passes the contents of the body from the
 * BatchRequest to
 * {@link org.apache.fineract.useradministration.api.UsersApiResource} and gets
 * back the response. This class will also catch any errors raised by
 * {@link org.apache.fineract.useradministration.api.UsersApiResource} and map
 * those errors to appropriate status codes in BatchResponse.
 * 
 * @author Moses Kalema
 * 
 * @see org.apache.fineract.batch.command.CommandStrategy
 * @see org.apache.fineract.batch.domain.BatchRequest
 * @see org.apache.fineract.batch.domain.BatchResponse
 */
@Component
public class ApproveAuthorizationRequestToViewClientCommandStrategy implements CommandStrategy {

    private final UsersApiResource usersApiResource;
    
    @Autowired
    public ApproveAuthorizationRequestToViewClientCommandStrategy(final UsersApiResource usersApiResource) {
        this.usersApiResource = usersApiResource;
    }
    
    @Override
    public BatchResponse execute(BatchRequest batchRequest, @SuppressWarnings("unused") UriInfo uriInfo) {
        
        final BatchResponse response = new BatchResponse();
        final String responseBody;

        response.setRequestId(batchRequest.getRequestId());
        response.setHeaders(batchRequest.getHeaders());
        
        final String[] pathParameters = batchRequest.getRelativeUrl().split("/");
        Long authorizationRequestId = Long.parseLong(pathParameters[2].substring(0, pathParameters[2].indexOf("?")));

        // Try-catch blocks to map exceptions to appropriate status codes
        try {

            // Calls 'authorize' function from 'usersApiResource' to approve or reject request to view client          
            responseBody = usersApiResource.authorize(authorizationRequestId, "approve", batchRequest.getBody());

            response.setStatusCode(200);
            
            // Sets the body of the response after the successful approval of the loan
            response.setBody(responseBody);

        } catch (RuntimeException e) {

            // Gets an object of type ErrorInfo, containing information about
            // raised exception
            ErrorInfo ex = ErrorHandler.handler(e);

            response.setStatusCode(ex.getStatusCode());
            response.setBody(ex.getMessage());
        }

        return response;
    }

}
