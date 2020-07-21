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

package org.apache.fineract.portfolio.loanaccount.service;

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
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanBalanceHistory;
import org.apache.fineract.portfolio.loanaccount.domain.LoanBalanceHistoryRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsBalanceHistory;
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
public class LoanBalanceVerificationService implements BalanceVerificationService {

    private final JdbcTemplate jdbcTemplate;
    private final LoanRepositoryWrapper loanRepository;
    private final LoanBalanceHistoryRepository loanBalanceHistoryRepository;

    @Autowired
    public LoanBalanceVerificationService(RoutingDataSource dataSource,
                                          LoanRepositoryWrapper loanRepository,
                                          LoanBalanceHistoryRepository loanBalanceHistoryRepository) {
        this.loanRepository = loanRepository;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.loanBalanceHistoryRepository = loanBalanceHistoryRepository;
    }

    private static int index;
    private static String action;
    private static int numberOfAccounts;

    static {
        index = 0;
        action = "";
        numberOfAccounts = 0;
    }

    private static void setIndex(int index) {
        LoanBalanceVerificationService.index = index;
    }

    private static void setAction(String action) {
        LoanBalanceVerificationService.action = action;
    }

    private static void setNumberOfAccounts(int numberOfAccounts) {
        LoanBalanceVerificationService.numberOfAccounts = numberOfAccounts;
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
    public void backupBalancesAsAt(LocalDate verificationDate) {
        setAction("Backup");
        List<Long> loanIds = getLoanIds();
        setNumberOfAccounts(loanIds.size());
        for (int i = 0; i < loanIds.size(); i++) {
            setIndex(i);
            this.backupBalance(loanIds.get(i), verificationDate);
        }
        resetStatus();
    }

    private List<Long> getLoanIds() {
        String sql = String.format("SELECT id FROM `m_loan` WHERE loan_status_id = %d", LoanStatus.ACTIVE.getValue());
        return this.jdbcTemplate.queryForList(sql, Long.class);
    }

    private void backupBalance(Long accountId, LocalDate verificationDate) {
        Loan loan = this.loanRepository.findOneWithNotFoundDetection(accountId, true);
        LoanTransaction transaction = this.getLatestTransactionBy(loan, verificationDate);
        LoanBalanceHistory balanceHistory = new LoanBalanceHistory();
        balanceHistory.setLoan(loan);
        balanceHistory.setBalanceDate(verificationDate.toDate());
        if (transaction != null) {
            balanceHistory.setBalance(transaction.getOutstandingLoanBalance());
        } else {
            balanceHistory.setBalance(BigDecimal.ZERO);
        }
        this.loanBalanceHistoryRepository.saveAndFlush(balanceHistory);
    }

    private LoanTransaction getLatestTransactionBy(Loan loan, LocalDate verificationDate) {
        TreeSet<LoanTransaction> transactions = loan.getLoanTransactions()
                .stream()
                .filter(t -> this.isTransactionAfter(t, verificationDate))
                .collect(Collectors.toCollection(() -> new TreeSet<>(
                        Comparator.comparing(LoanTransaction::getTransactionDate)
                                .thenComparing(LoanTransaction::getId)
                )));
        return transactions.pollLast();
    }

    private boolean isTransactionAfter(LoanTransaction transaction, LocalDate verificationDate) {
        return transaction != null && transaction.isNotReversed() && !transaction.getTransactionDate().isAfter(verificationDate)
                && transaction.isRepayment() && transaction.getId() != null;
    }

    @Override
    public void verifyBalancesAsAt(LocalDate verificationDate) {
        Map<Long, LoanBalanceHistory> balances = this.getBalancesMap();
        setAction("Verification");
        List<Long> loanIds = getLoanIds();
        setNumberOfAccounts(loanIds.size());
        for (int i = 0; i < loanIds.size(); i++) {
            setIndex(i);
            Long loanId = loanIds.get(i);
            this.verifyBalance(balances.get(loanId), loanId, verificationDate);
        }
        resetStatus();
    }

    private void verifyBalance(LoanBalanceHistory balanceHistory, Long loanId, LocalDate verificationDate) {
        Loan loan = this.loanRepository.findOneWithNotFoundDetection(loanId, true);
        LoanTransaction transaction = this.getLatestTransactionBy(loan, verificationDate);
        if (transaction != null) {
            balanceHistory.setDerivedBalance(transaction.getOutstandingLoanBalance());
        } else {
            balanceHistory.setDerivedBalance(BigDecimal.ZERO);
        }
        balanceHistory.setValid(balanceHistory.getBalance().compareTo(balanceHistory.getDerivedBalance()) == 0);
        this.loanBalanceHistoryRepository.save(balanceHistory);
    }

    private Map<Long, LoanBalanceHistory> getBalancesMap() {
        Map<Long, LoanBalanceHistory> balances = new HashMap<>();
        List<LoanBalanceHistory> balanceList = this.loanBalanceHistoryRepository.findAll();
        balanceList.forEach(x -> balances.put(x.getLoan().getId(), x));
        return balances;
    }

    @Override
    public BalanceVerificationStatus getBalanceVerificationStatus() {
        return new BalanceVerificationStatus(index, action, numberOfAccounts);
    }

    @Override
    public CommandProcessingResult backupBalancesAsAt(JsonCommand command) {
        if (actionNotInProgress()) {
            this.loanBalanceHistoryRepository.deleteAll();
            LocalDate verificationDate = command.localDateValueOfParameterNamed(AccountDetailConstants.verificationDateParamName);
            BalanceBackupRunner balanceVerificationRunner = new BalanceBackupRunner(verificationDate, ThreadLocalContextUtil.getTenant(),
                    this);
            balanceVerificationRunner.start();
            return this.buildCommandProcessingResult(verificationDate);
        } else {
            return CommandProcessingResult.empty();
        }
    }

    private CommandProcessingResult buildCommandProcessingResult(LocalDate verificationDate) {
        Map<String, Object> changes = new HashMap<>();
        changes.put(AccountDetailConstants.verificationDateParamName, verificationDate);
        return new CommandProcessingResultBuilder().with(changes).build();
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
}
