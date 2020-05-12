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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.portfolio.loanaccount.data.LoanScheduleAccrualData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanAccrualPlatformServiceImpl implements LoanAccrualPlatformService {

    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanAccrualWritePlatformService loanAccrualWritePlatformService;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;

    @Autowired
    public LoanAccrualPlatformServiceImpl(final LoanReadPlatformService loanReadPlatformService,
            final LoanAccrualWritePlatformService loanAccrualWritePlatformService,
            final LoanRepositoryWrapper loanRepositoryWrapper,
            final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper,
            final JournalEntryWritePlatformService journalEntryWritePlatformService) {
        this.loanReadPlatformService = loanReadPlatformService;
        this.loanAccrualWritePlatformService = loanAccrualWritePlatformService;
        this.loanRepositoryWrapper = loanRepositoryWrapper;
        this.applicationCurrencyRepositoryWrapper = applicationCurrencyRepositoryWrapper;
        this.journalEntryWritePlatformService = journalEntryWritePlatformService;
    }

    @Override
    @CronTarget(jobName = JobName.ADD_ACCRUAL_ENTRIES)
    public void addAccrualAccounting() throws JobExecutionException {
        verifyAndRemoveWrongAccrualTransactions();
        Collection<LoanScheduleAccrualData> loanScheduleAccrualDatas = this.loanReadPlatformService.retriveScheduleAccrualData();
        StringBuilder sb = new StringBuilder();
        Map<Long, Collection<LoanScheduleAccrualData>> loanDataMap = new HashMap<>();
        for (final LoanScheduleAccrualData accrualData : loanScheduleAccrualDatas) {
            if (loanDataMap.containsKey(accrualData.getLoanId())) {
                loanDataMap.get(accrualData.getLoanId()).add(accrualData);
            } else {
                Collection<LoanScheduleAccrualData> accrualDatas = new ArrayList<>();
                accrualDatas.add(accrualData);
                loanDataMap.put(accrualData.getLoanId(), accrualDatas);
            }
        }

        for (Map.Entry<Long, Collection<LoanScheduleAccrualData>> mapEntry : loanDataMap.entrySet()) {
            try {
                this.loanAccrualWritePlatformService.addAccrualAccounting(mapEntry.getKey(), mapEntry.getValue());
            } catch (Exception e) {
                Throwable realCause = e;
                if (e.getCause() != null) {
                    realCause = e.getCause();
                }
                if(realCause.getMessage() != null) {
                sb.append("failed to add accural transaction for loan " + mapEntry.getKey() + " with message " + realCause.getMessage());
                }else {
                    sb.append("failed to add accural transaction for loan " + mapEntry.getKey() + " with message " + realCause.toString());
                }
            }
        }

        if (sb.length() > 0) { throw new JobExecutionException(sb.toString()); }
    }

    @Override
    @CronTarget(jobName = JobName.ADD_PERIODIC_ACCRUAL_ENTRIES)
    public void addPeriodicAccruals() throws JobExecutionException {
        verifyAndRemoveWrongAccrualTransactions();
        String errors = addPeriodicAccruals(DateUtils.getLocalDateOfTenant());
        if (errors.length() > 0) { throw new JobExecutionException(errors); }
    }

    @Override
    public String addPeriodicAccruals(final LocalDate tilldate) {
        Collection<LoanScheduleAccrualData> loanScheduleAccrualDatas = this.loanReadPlatformService.retrivePeriodicAccrualData(tilldate);
        return addPeriodicAccruals(tilldate, loanScheduleAccrualDatas);
    }

    @Override
    public String addPeriodicAccruals(final LocalDate tilldate, Collection<LoanScheduleAccrualData> loanScheduleAccrualDatas) {
        StringBuilder sb = new StringBuilder();
        Map<Long, Collection<LoanScheduleAccrualData>> loanDataMap = new HashMap<>();
        for (final LoanScheduleAccrualData accrualData : loanScheduleAccrualDatas) {
            if (loanDataMap.containsKey(accrualData.getLoanId())) {
                loanDataMap.get(accrualData.getLoanId()).add(accrualData);
            } else {
                Collection<LoanScheduleAccrualData> accrualDatas = new ArrayList<>();
                accrualDatas.add(accrualData);
                loanDataMap.put(accrualData.getLoanId(), accrualDatas);
            }
        }

        for (Map.Entry<Long, Collection<LoanScheduleAccrualData>> mapEntry : loanDataMap.entrySet()) {
            try {
                this.loanAccrualWritePlatformService.addPeriodicAccruals(tilldate, mapEntry.getKey(), mapEntry.getValue());
            } catch (Exception e) {
                Throwable realCause = e;
                if (e.getCause() != null) {
                    realCause = e.getCause();
                }
                sb.append("failed to add accural transaction for loan " + mapEntry.getKey() + " with message " + realCause.getMessage());
            }
        }

        return sb.toString();
    }

    @Override
    @CronTarget(jobName = JobName.ADD_PERIODIC_ACCRUAL_ENTRIES_FOR_LOANS_WITH_INCOME_POSTED_AS_TRANSACTIONS)
    public void addPeriodicAccrualsForLoansWithIncomePostedAsTransactions() throws JobExecutionException {
        Collection<Long> loanIds = this.loanReadPlatformService.retrieveLoanIdsWithPendingIncomePostingTransactions();
        if(loanIds != null && loanIds.size() > 0){
            StringBuilder sb = new StringBuilder();
            for (Long loanId : loanIds) {
                try {
                    this.loanAccrualWritePlatformService.addIncomeAndAccrualTransactions(loanId);
                } catch (Exception e) {
                    Throwable realCause = e;
                    if (e.getCause() != null) {
                        realCause = e.getCause();
                    }
                    sb.append("failed to add income and accrual transaction for loan " + loanId + " with message " + realCause.getMessage());
                }
            }
            if (sb.length() > 0) { throw new JobExecutionException(sb.toString()); }
        }
    }
    
    @Transactional
    void verifyAndRemoveWrongAccrualTransactions() {
        Collection<Long> loanIds = this.loanReadPlatformService.retriveActiveAndClosedLoans();
        LocalDate accruedTilldefault = new LocalDate(2019, 10, 1);
        for (Long loanid : loanIds) {
            Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanid, true);
            final List<Long> existingTransactionIds = new ArrayList<>();
            final List<Long> existingReversedTransactionIds = new ArrayList<>();
            existingTransactionIds.addAll(loan.findExistingTransactionIds());
            existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());
            for (LoanRepaymentScheduleInstallment installment : loan.getRepaymentScheduleInstallments()) {
                if (installment.getDueDate().isAfter(DateUtils.getLocalDateOfTenant()) || installment.isRecalculatedInterestComponent()
                        || installment.getDueDate().isBefore(accruedTilldefault)) {
                    if(installment.getDueDate().isBefore(accruedTilldefault)) {
                        installment.setInterestAccrued(installment.getInterestCharged(loan.getCurrency()).getAmount());
                        installment.setFeeAccrued(installment.getFeeChargesCharged(loan.getCurrency()).getAmount());
                        installment.setPenaltyAccrued(installment.getPenaltyChargesCharged(loan.getCurrency()).getAmount());
                    }
                    continue;
                }
                ArrayList<LoanTransaction> loanAccrualTransactions = new ArrayList<LoanTransaction>();
                BigDecimal accruedInterestAmount = BigDecimal.ZERO;
                BigDecimal accruedFeesAmount = BigDecimal.ZERO;
                BigDecimal accruedPenaltiesAmount = BigDecimal.ZERO;
                LocalDate startDate = installment.getFromDate().plusDays(1);
                LocalDate endDate = installment.getDueDate();
                if (startDate.isAfter(endDate)) {
                    startDate = endDate;
                }
                for (LoanTransaction transaction : loan.getLoanTransactions()) {
                    BigDecimal accruedInterestAmountTransaction = BigDecimal.ZERO;
                    BigDecimal accruedFeesAmountTransaction = BigDecimal.ZERO;
                    BigDecimal accruedPenaltiesAmountTransaction = BigDecimal.ZERO;
                    if (transaction.isAccrualTransaction() && (!transaction.getTransactionDate().isBefore(startDate)
                            && !transaction.getTransactionDate().isAfter(endDate))) {
                        if (transaction.getInterestPortion(loan.getCurrency()) != null) {
                            accruedInterestAmountTransaction = transaction.getInterestPortion(loan.getCurrency()).getAmount();
                        }
                        if (transaction.getFeeChargesPortion(loan.getCurrency()) != null) {
                            accruedFeesAmountTransaction = transaction.getFeeChargesPortion(loan.getCurrency()).getAmount();
                        }
                        if (transaction.getPenaltyChargesPortion() != null) {
                            accruedPenaltiesAmountTransaction = transaction.getPenaltyChargesPortion();
                        }
                        accruedInterestAmount = accruedInterestAmount.add(accruedInterestAmountTransaction);
                        accruedFeesAmount = accruedFeesAmount.add(accruedFeesAmountTransaction);
                        accruedPenaltiesAmount = accruedPenaltiesAmount.add(accruedPenaltiesAmountTransaction);
                        loanAccrualTransactions.add(transaction);
                    }
                    if (transaction.isRepayment()) {
                        if (transaction.getIncomeFeeChargesPortion(loan.getCurrency()) != null) {
                            accruedFeesAmount = accruedFeesAmount.add(transaction.getIncomeFeeChargesPortion(loan.getCurrency()).getAmount());
                        }
                        if (transaction.getIncomeInterestPortion(loan.getCurrency()) != null) {
                            accruedInterestAmount = accruedInterestAmount.add(transaction.getIncomeInterestPortion(loan.getCurrency()).getAmount());
                        }
                        if (transaction.getIncomePenaltyChargesPortion(loan.getCurrency()) != null) {
                            accruedPenaltiesAmount = accruedPenaltiesAmount.add(transaction.getIncomePenaltyChargesPortion(loan.getCurrency()).getAmount());
                        }
                    }
                }
                installment.setInterestAccrued(accruedInterestAmount);
                installment.setFeeAccrued(accruedFeesAmount);
                installment.setPenaltyAccrued(accruedPenaltiesAmount);
                if (!accruedInterestAmount.equals(installment.getInterestCharged(loan.getCurrency()).getAmount())
                        || !accruedFeesAmount.equals(installment.getFeeChargesCharged(loan.getCurrency()).getAmount())
                        || !accruedPenaltiesAmount.equals(installment.getPenaltyChargesCharged(loan.getCurrency()).getAmount())) {
                    for (LoanTransaction tran : loanAccrualTransactions) {
                        tran.reverse();
                    }
                    installment.setInterestAccrued(null);
                    installment.setFeeAccrued(null);
                    installment.setPenaltyAccrued(null);
                }
                accruedInterestAmount = BigDecimal.ZERO;
                accruedFeesAmount = BigDecimal.ZERO;
                accruedPenaltiesAmount = BigDecimal.ZERO;
            }
            for (LoanTransaction transaction : loan.getLoanTransactions()) {
                if (transaction.isAccrualTransaction()) {
                    if (transaction.getTransactionDate().isAfter(accruedTilldefault)) {
                        accruedTilldefault = transaction.getTransactionDate();
                    }
                }
            }
            loan.setAccruedTill(accruedTilldefault.toDate());
            accruedTilldefault = new LocalDate(2019, 10, 1);
            this.loanRepositoryWrapper.save(loan);
            postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, false);
        }
    }
    
    private void postJournalEntries(final Loan loanAccount, final List<Long> existingTransactionIds,
            final List<Long> existingReversedTransactionIds, boolean isLoanToLoanTransfer) {

        final MonetaryCurrency currency = loanAccount.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepositoryWrapper.findOneWithNotFoundDetection(currency);

        final Map<String, Object> accountingBridgeData = loanAccount.deriveAccountingBridgeData(applicationCurrency.toData(),
                existingTransactionIds, existingReversedTransactionIds);
        accountingBridgeData.put("isLoanToLoanTransfer", isLoanToLoanTransfer);
        this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData);
    }
}
