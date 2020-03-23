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
package org.apache.fineract.portfolio.savings.classification.service;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.savings.classification.domain.TransactionClassification;
import org.apache.fineract.portfolio.savings.classification.domain.TransactionClassificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionClassificationWritePlatformServiceJPAImpl implements TransactionClassificationWritePlatformService {

    TransactionClassificationRepository transactionClassificationRepository;

    @Autowired
    public TransactionClassificationWritePlatformServiceJPAImpl(TransactionClassificationRepository transactionClassificationRepository) {
        this.transactionClassificationRepository = transactionClassificationRepository;
    }

    @Override
    public CommandProcessingResult createTransactionClassification(JsonCommand command) {
        String classification = command.stringValueOfParameterNamed("classification");
        String operator = command.stringValueOfParameterNamed("operator");
        TransactionClassification transctionClassification = new TransactionClassification(classification, operator);
        TransactionClassification transactionClassification = this.transactionClassificationRepository.save(transctionClassification);
        return new CommandProcessingResult(transactionClassification.getId());
    }

    @Override
    public CommandProcessingResult deleteTransactionClassification(JsonCommand command) {
        Long id = command.longValueOfParameterNamed("transactionClassificationId");
        transactionClassificationRepository.delete(transactionClassificationRepository.findOne(id));
        return new CommandProcessingResult(id);
    }
}
