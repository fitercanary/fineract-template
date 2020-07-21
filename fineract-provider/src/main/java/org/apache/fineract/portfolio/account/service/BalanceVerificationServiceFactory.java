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

package org.apache.fineract.portfolio.account.factory;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.account.AccountDetailConstants;
import org.apache.fineract.portfolio.account.data.BalanceVerificationDataValidator;
import org.apache.fineract.portfolio.account.domain.BalanceAccountType;
import org.apache.fineract.portfolio.account.service.BalanceVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BalanceVerificationServiceFactory {

    private final BalanceVerificationService loanBalanceVerificationService;
    private final BalanceVerificationService savingsBalanceVerificationService;
    private final BalanceVerificationDataValidator balanceVerificationDataValidator;

    @Autowired
    public BalanceVerificationServiceFactory(BalanceVerificationService loanBalanceVerificationService,
                                             BalanceVerificationService savingsBalanceVerificationService,
                                             BalanceVerificationDataValidator balanceVerificationDataValidator) {
        this.loanBalanceVerificationService = loanBalanceVerificationService;
        this.savingsBalanceVerificationService = savingsBalanceVerificationService;
        this.balanceVerificationDataValidator = balanceVerificationDataValidator;
    }

    public BalanceVerificationService getService(BalanceAccountType balanceAccountType) {
        switch(balanceAccountType) {
            case LOAN:
                return this.loanBalanceVerificationService;
            case SAVINGS_DEPOSIT:
                return this.savingsBalanceVerificationService;
            default:
                throw new IllegalArgumentException("unknown.balance.type");
        }
    }

    public CommandProcessingResult backupBalance(JsonCommand command) {
        this.balanceVerificationDataValidator.validate(command);
        String accountType = command.stringValueOfParameterNamed(AccountDetailConstants.accountTypeParamName);
        return this.getService(BalanceAccountType.fromCode(accountType)).backupBalancesAsAt(command);
    }

    public CommandProcessingResult verifyBalance(JsonCommand command) {
        this.balanceVerificationDataValidator.validate(command);
        String accountType = command.stringValueOfParameterNamed(AccountDetailConstants.accountTypeParamName);
        return this.getService(BalanceAccountType.fromCode(accountType)).verifyBalancesAsAt(command);
    }
}
