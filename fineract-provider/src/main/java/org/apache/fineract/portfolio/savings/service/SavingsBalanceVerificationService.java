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

package org.apache.fineract.portfolio.savings.service;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.portfolio.account.AccountDetailConstants;
import org.apache.fineract.portfolio.account.service.BalanceVerificationService;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsBalanceHistory;
import org.apache.fineract.portfolio.savings.domain.SavingsBalanceHistoryRepository;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
public class SavingsBalanceVerificationService implements BalanceVerificationService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsBalanceHistoryRepository savingsBalanceHistoryRepository;

    @Autowired
    public SavingsBalanceVerificationService(SavingsAccountRepository savingsAccountRepository,
                                             SavingsBalanceHistoryRepository savingsBalanceHistoryRepository) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsBalanceHistoryRepository = savingsBalanceHistoryRepository;
    }

    @Override
    public CommandProcessingResult verifyBalancesAsAt(JsonCommand command) {
        LocalDate verificationDate = command.localDateValueOfParameterNamed(AccountDetailConstants.verificationDateParamName);
        List<SavingsAccount> savingsAccounts = this.savingsAccountRepository.findSavingsAccountsByStatusAndDepositType(SavingsAccountStatusType.ACTIVE.getValue(),
                DepositAccountType.SAVINGS_DEPOSIT.getValue());
        savingsAccounts.forEach(account -> this.backUpBalances(account, verificationDate));
        return this.buildCommandProcessingResult(verificationDate);
    }

    private CommandProcessingResult buildCommandProcessingResult(LocalDate verificationDate) {
        Map<String, Object> changes = new HashMap<>();
        changes.put(AccountDetailConstants.verificationDateParamName, verificationDate);
        return new CommandProcessingResultBuilder().with(changes).build();
    }

    private void backUpBalances(SavingsAccount savingsAccount, LocalDate verificationDate) {
        SavingsAccountTransaction transaction = this.getLatestTransactionBy(savingsAccount, verificationDate);
        SavingsBalanceHistory balanceHistory = new SavingsBalanceHistory();
        balanceHistory.setSavingsAccount(savingsAccount);
        balanceHistory.setBalanceDate(verificationDate.toDate());
        if (transaction != null) {
            balanceHistory.setBalance(transaction.getRunningBalance(savingsAccount.getCurrency()).getAmount());
        } else {
            balanceHistory.setBalance(BigDecimal.ZERO);
        }
        this.savingsBalanceHistoryRepository.save(balanceHistory);
    }

    private SavingsAccountTransaction getLatestTransactionBy(SavingsAccount account, LocalDate verificationDate) {
        TreeSet<SavingsAccountTransaction> transactions = account.getTransactions()
                .stream().filter(t->t.isNotReversed() && !t.transactionLocalDate().isAfter(verificationDate))
                .collect(Collectors.toCollection(() -> new TreeSet<>(
                        Comparator.comparing(SavingsAccountTransaction::transactionLocalDate)
                                .thenComparing(SavingsAccountTransaction::getId)
                )));
        return transactions.pollLast();
    }
}
