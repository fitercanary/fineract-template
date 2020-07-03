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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.ws.rs.core.UriInfo;

import org.apache.fineract.batch.command.CommandStrategy;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.fineract.batch.exception.ErrorHandler;
import org.apache.fineract.batch.exception.ErrorInfo;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.notification.config.MessagingConfiguration;
import org.apache.fineract.portfolio.account.api.AccountTransfersApiResource;
import org.apache.fineract.portfolio.account.data.AccountTransferData;
import org.apache.fineract.portfolio.account.data.AccountTransfersDataValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

/**
 * Implements {@link org.apache.fineract.batch.command.CommandStrategy} to
 * handle reject authorization request to view a client. It passes the contents
 * of the body from the BatchRequest to
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
public class AccountTransfersCommandStrategy implements CommandStrategy {

    private final AccountTransfersApiResource accountTransfersApiResource;
    private final AccountTransfersDataValidator accountTransfersDataValidator;
    private final MessagingConfiguration messagingConfiguration;
    private final DefaultToApiJsonSerializer<AccountTransferData> toApiJsonSerializer;

    @Autowired
    public AccountTransfersCommandStrategy(final AccountTransfersApiResource accountTransfersApiResource,
            final AccountTransfersDataValidator accountTransfersDataValidator, final MessagingConfiguration messagingConfiguration,
            final DefaultToApiJsonSerializer<AccountTransferData> toApiJsonSerializer) {
        this.accountTransfersApiResource = accountTransfersApiResource;
        this.accountTransfersDataValidator = accountTransfersDataValidator;
        this.messagingConfiguration = messagingConfiguration;
        this.toApiJsonSerializer = toApiJsonSerializer;
    }

    @Override
    public BatchResponse execute(BatchRequest batchRequest, @SuppressWarnings("unused") UriInfo uriInfo) {
        final BatchResponse response = new BatchResponse();
        final String responseBody;

        response.setRequestId(batchRequest.getRequestId());
        response.setHeaders(batchRequest.getHeaders());

        // Try-catch blocks to map exceptions to appropriate status codes
        try {

            // Calls 'queueTransfer' function from 'accountTransfersApiResource'
            // to
            // add transfer request to ActiveMQ for later processing
            // this.accountTransfersDataValidator.validate(command);
            // responseBody =
            // accountTransfersApiResource.queueTransfer(batchRequest.getBody());

            this.messagingConfiguration.jmsTemplate().send("AccountTransferQueue", new MessageCreator() {

                @Override
                public Message createMessage(Session session) throws JMSException {

                    return session.createTextMessage(batchRequest.getBody());
                }
            });

            response.setStatusCode(200);

            final CommandProcessingResultBuilder builder = new CommandProcessingResultBuilder().withEntityId(batchRequest.getRequestId());
            final CommandProcessingResult result = builder.build();

            responseBody = this.toApiJsonSerializer.serialize(result);

            // Sets the body of the response after the successful transfer
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
