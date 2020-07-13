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
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.account.AccountDetailConstants;
import org.apache.fineract.portfolio.account.data.BalanceVerificationStatus;
import org.apache.fineract.portfolio.account.runner.BalanceBackupRunner;
import org.apache.fineract.portfolio.account.runner.BalanceVerificationRunner;
import org.apache.fineract.portfolio.account.service.BalanceVerificationService;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsBalanceHistory;
import org.apache.fineract.portfolio.savings.domain.SavingsBalanceHistoryRepository;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private final JdbcTemplate jdbcTemplate;
    private final SavingsAccountRepositoryWrapper savingsAccountRepository;
    private final SavingsBalanceHistoryRepository savingsBalanceHistoryRepository;

    private static int index;
    private static String action;
    private static int numberOfAccounts;

    static {
        index = 0;
        action = "";
        numberOfAccounts = 0;
    }

    @Autowired
    public SavingsBalanceVerificationService(RoutingDataSource dataSource,
                                             SavingsAccountRepositoryWrapper savingsAccountRepository,
                                             SavingsBalanceHistoryRepository savingsBalanceHistoryRepository) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsBalanceHistoryRepository = savingsBalanceHistoryRepository;
    }

    private static void setIndex(int index) {
        SavingsBalanceVerificationService.index = index;
    }

    private static void setAction(String action) {
        SavingsBalanceVerificationService.action = action;
    }

    private static void setNumberOfAccounts(int numberOfAccounts) {
        SavingsBalanceVerificationService.numberOfAccounts = numberOfAccounts;
    }

    private static void resetStatus() {
        setIndex(0);
        setAction("");
        setNumberOfAccounts(0);
    }

    private static boolean actionNotInProgress() {
        return index == 0 && numberOfAccounts == 0;
    }

    @Override
    public CommandProcessingResult backupBalancesAsAt(JsonCommand command) {
        if (actionNotInProgress()) {
            this.savingsBalanceHistoryRepository.deleteAll();
            LocalDate verificationDate = command.localDateValueOfParameterNamed(AccountDetailConstants.verificationDateParamName);
            BalanceBackupRunner balanceVerificationRunner = new BalanceBackupRunner(verificationDate, ThreadLocalContextUtil.getTenant(),
                    this);
            balanceVerificationRunner.start();
            return this.buildCommandProcessingResult(verificationDate);
        } else {
            return CommandProcessingResult.empty();
        }
    }

    public void backupBalancesAsAt(LocalDate verificationDate) {
        setAction("Backup");
        List<Long> savingsAccountIds = getSavingsAccountIds();
        setNumberOfAccounts(savingsAccountIds.size());
        for (int i = 0; i < savingsAccountIds.size(); i++) {
            setIndex(i);
            this.backupBalance(savingsAccountIds.get(i), verificationDate);
        }
        resetStatus();
    }

    private List<Long> getSavingsAccountIds() {
        String sql = String.format("SELECT id FROM `m_savings_account` WHERE status_enum = %d AND deposit_type_enum = %d",
                SavingsAccountStatusType.ACTIVE.getValue(), DepositAccountType.SAVINGS_DEPOSIT.getValue());
        return this.jdbcTemplate.queryForList(sql, Long.class);
    }

    private CommandProcessingResult buildCommandProcessingResult(LocalDate verificationDate) {
        Map<String, Object> changes = new HashMap<>();
        changes.put(AccountDetailConstants.verificationDateParamName, verificationDate);
        return new CommandProcessingResultBuilder().with(changes).build();
    }

    private void backupBalance(Long accountId, LocalDate verificationDate) {
        SavingsAccount savingsAccount = this.savingsAccountRepository.findOneWithNotFoundDetection(accountId);
        SavingsAccountTransaction transaction = this.getLatestTransactionBy(savingsAccount, verificationDate);
        SavingsBalanceHistory balanceHistory = new SavingsBalanceHistory();
        balanceHistory.setSavingsAccount(savingsAccount);
        balanceHistory.setBalanceDate(verificationDate.toDate());
        if (transaction != null) {
            balanceHistory.setBalance(transaction.getRunningBalance(savingsAccount.getCurrency()).getAmount());
        } else {
            balanceHistory.setBalance(BigDecimal.ZERO);
        }
        this.savingsBalanceHistoryRepository.saveAndFlush(balanceHistory);
    }

    private SavingsAccountTransaction getLatestTransactionBy(SavingsAccount account, LocalDate verificationDate) {
        TreeSet<SavingsAccountTransaction> transactions = account.getTransactions()
                .stream()
                .filter(t -> this.isTransactionAfter(t, verificationDate))
                .collect(Collectors.toCollection(() -> new TreeSet<>(
                        Comparator.comparing(SavingsAccountTransaction::transactionLocalDate)
                                .thenComparing(SavingsAccountTransaction::getId)
                )));
        return transactions.pollLast();
    }

    private boolean isTransactionAfter(SavingsAccountTransaction transaction, LocalDate verificationDate) {
        return transaction != null && transaction.isNotReversed() && !transaction.transactionLocalDate().isAfter(verificationDate) && transaction.getId() != null;
    }

    @Override
    public CommandProcessingResult verifyBalancesAsAt(JsonCommand command) {
        if (actionNotInProgress()) {
            LocalDate verificationDate = command.localDateValueOfParameterNamed(AccountDetailConstants.verificationDateParamName);
            BalanceVerificationRunner balanceVerificationRunner = new BalanceVerificationRunner(verificationDate, ThreadLocalContextUtil.getTenant(),
                    this);
            balanceVerificationRunner.start();
            return this.buildCommandProcessingResult(verificationDate);
        } else {
            return CommandProcessingResult.empty();
        }
    }

    @Override
    public void verifyBalancesAsAt(LocalDate verificationDate) {
        Map<Long, SavingsBalanceHistory> balances = this.getBalancesMap();
        setAction("Verification");
        List<Long> savingsAccountIds = getSavingsAccountIds();
        setNumberOfAccounts(savingsAccountIds.size());
        for (int i = 0; i < savingsAccountIds.size(); i++) {
            setIndex(i);
            Long accountId = savingsAccountIds.get(i);
            this.verifyBalance(balances.get(accountId), accountId, verificationDate);
        }
        resetStatus();
    }

    private void verifyBalance(SavingsBalanceHistory balanceHistory, Long accountId, LocalDate verificationDate) {
        SavingsAccount savingsAccount = this.savingsAccountRepository.findOneWithNotFoundDetection(accountId);
        savingsAccount.recalculateDailyBalances(Money.zero(savingsAccount.getCurrency()), verificationDate);
        SavingsAccountTransaction transaction = this.getLatestTransactionBy(savingsAccount, verificationDate);
        balanceHistory.setDerivedBalance(transaction != null
                ? transaction.getRunningBalance(savingsAccount.getCurrency()).getAmount()
                : BigDecimal.ZERO);
        balanceHistory.setValid(balanceHistory.getBalance().compareTo(balanceHistory.getDerivedBalance()) == 0);
        this.savingsBalanceHistoryRepository.save(balanceHistory);
    }

    private Map<Long, SavingsBalanceHistory> getBalancesMap() {
        Map<Long, SavingsBalanceHistory> balances = new HashMap<>();
        List<SavingsBalanceHistory> balanceList = this.savingsBalanceHistoryRepository.findAll();
        balanceList.forEach(x -> balances.put(x.getSavingsAccount().getId(), x));
        return balances;
    }

    @Override
    public BalanceVerificationStatus getBalanceVerificationStatus() {
        return new BalanceVerificationStatus(index, action, numberOfAccounts);
    }
}
