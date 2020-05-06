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
package org.apache.fineract.portfolio.savings.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_ENTITY;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_EVENTS;
import org.apache.fineract.portfolio.common.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.SavingsTransactionBooleanValues;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionDTO;
import org.apache.fineract.portfolio.savings.exception.DepositAccountTransactionNotAllowedException;
import org.apache.fineract.portfolio.savings.exception.ExceedDailyWithdrawLimitException;
import org.apache.fineract.portfolio.savings.service.SavingsAccountReadPlatformService;
import org.apache.fineract.portfolio.validation.limit.domain.ValidationLimit;
import org.apache.fineract.portfolio.validation.limit.service.ValidationLimitReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavingsAccountDomainServiceJpa implements SavingsAccountDomainService {

    private final static Logger logger = LoggerFactory.getLogger(SavingsAccountDomainServiceJpa.class);
    private final PlatformSecurityContext context;
    private final SavingsAccountRepositoryWrapper savingsAccountRepository;
    private final SavingsAccountTransactionRepository savingsAccountTransactionRepository;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final ConfigurationDomainService configurationDomainService;
    private final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final ValidationLimitReadPlatformService validationLimitReadPlatformService;
    private final SavingsAccountReadPlatformService savingsAccountReadPlatformService;
    private final SavingsAccountAssembler savingAccountAssembler;

    @Autowired
    public SavingsAccountDomainServiceJpa(final SavingsAccountRepositoryWrapper savingsAccountRepository,
            final SavingsAccountTransactionRepository savingsAccountTransactionRepository,
            final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper,
            final JournalEntryWritePlatformService journalEntryWritePlatformService,
            final ConfigurationDomainService configurationDomainService, final PlatformSecurityContext context,
            final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository,
            final BusinessEventNotifierService businessEventNotifierService,
            final ValidationLimitReadPlatformService validationLimitReadPlatformService,
            final SavingsAccountReadPlatformService savingsAccountReadPlatformService,
            final SavingsAccountAssembler savingAccountAssembler) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsAccountTransactionRepository = savingsAccountTransactionRepository;
        this.applicationCurrencyRepositoryWrapper = applicationCurrencyRepositoryWrapper;
        this.journalEntryWritePlatformService = journalEntryWritePlatformService;
        this.configurationDomainService = configurationDomainService;
        this.context = context;
        this.depositAccountOnHoldTransactionRepository = depositAccountOnHoldTransactionRepository;
        this.businessEventNotifierService = businessEventNotifierService;
        this.validationLimitReadPlatformService = validationLimitReadPlatformService;
        this.savingsAccountReadPlatformService = savingsAccountReadPlatformService;
        this.savingAccountAssembler = savingAccountAssembler;
    }

    @Transactional
    @Override
    public SavingsAccountTransaction handleWithdrawal(final SavingsAccount account, final DateTimeFormatter fmt,
            final LocalDate transactionDate, final BigDecimal transactionAmount, final PaymentDetail paymentDetail,
            final SavingsTransactionBooleanValues transactionBooleanValues, final boolean isNotSavingToSavingTransfer) {

        AppUser user = getAppUserIfPresent();
        account.validateForAccountBlock();
        account.validateForDebitBlock();
        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();
        final boolean isClientLevelValidationEnabled = this.configurationDomainService.isClientLevelValidationEnabled();

        final GlobalConfigurationPropertyData dailyWithdrawalLimitConfigDetails = this.configurationDomainService
                .dailyWithdrawalLimitConfigDetails();
        if (dailyWithdrawalLimitConfigDetails.isEnabled() && account.depositAccountType().isSavingsDeposit() && !account.depositAccountType().isFixedDeposit() && !isNotSavingToSavingTransfer) {
            ValidateDailyWithdrawalLimit(account, transactionDate, transactionAmount,
                    dailyWithdrawalLimitConfigDetails.getValue().doubleValue());
        }

        if (isClientLevelValidationEnabled && account.depositAccountType().isSavingsDeposit() && !isNotSavingToSavingTransfer) {
            ValidationLimit validationLimit = getValidationLimitForClientLevel(account);
            account.validateTransactionAmountByLimit(transactionAmount, validationLimit, SavingsAccountTransactionType.WITHDRAWAL);
        }

        if (transactionBooleanValues.isRegularTransaction() && !account.allowWithdrawal()) {
            throw new DepositAccountTransactionNotAllowedException(account.getId(), "withdraw", account.depositAccountType());
        }
        final Set<Long> existingTransactionIds = new HashSet<>();
        final LocalDate postInterestOnDate = null;
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
        Integer accountType = null;
        final SavingsAccountTransactionDTO transactionDTO = new SavingsAccountTransactionDTO(fmt, transactionDate, transactionAmount,
                paymentDetail, new Date(), user, accountType, transactionBooleanValues.isAccountTransfer());

        final SavingsAccountTransaction withdrawal = account.withdraw(transactionDTO, transactionBooleanValues.isApplyWithdrawFee(),
                transactionBooleanValues.isApplyOverdraftFee());
        final MathContext mc = MathContext.DECIMAL64;
        if (account.isBeforeLastAccrualPostingPeriod(transactionDate)) {
            account.postAccrualInterest(mc, DateUtils.getLocalDateOfTenant(), transactionBooleanValues.isInterestTransfer(),
                    isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, postInterestOnDate);
        }
        if (account.isBeforeLastPostingPeriod(transactionDate)) {
            final LocalDate today = DateUtils.getLocalDateOfTenant();
            account.postInterest(mc, today, transactionBooleanValues.isInterestTransfer(), isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth, postInterestOnDate);
        } else {
            final LocalDate today = DateUtils.getLocalDateOfTenant();
            account.calculateInterestUsing(mc, today, transactionBooleanValues.isInterestTransfer(),
                    isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, postInterestOnDate);
        }
        List<DepositAccountOnHoldTransaction> depositAccountOnHoldTransactions = null;
        if (account.getOnHoldFunds().compareTo(BigDecimal.ZERO) == 1) {
            depositAccountOnHoldTransactions = this.depositAccountOnHoldTransactionRepository
                    .findBySavingsAccountAndReversedFalseOrderByCreatedDateAsc(account);
        }
        account.validateAccountBalanceDoesNotBecomeNegative(transactionAmount, transactionBooleanValues.isExceptionForBalanceCheck(),
                depositAccountOnHoldTransactions);
        if (isClientLevelValidationEnabled && account.depositAccountType().isSavingsDeposit() && !isNotSavingToSavingTransfer) {
            ValidationLimit validationLimit = getValidationLimitForClientLevel(account);
            account.validateDailyTranactionLimit(account, validationLimit, transactionDate);
        }
        saveTransactionToGenerateTransactionId(withdrawal);
        this.savingsAccountRepository.save(account);

        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.SAVINGS_WITHDRAWAL,
                constructEntityMap(BUSINESS_ENTITY.SAVINGS_TRANSACTION, withdrawal));
        return withdrawal;
    }

    private AppUser getAppUserIfPresent() {
        AppUser user = null;
        if (this.context != null) {
            user = this.context.getAuthenticatedUserIfPresent();
        }
        return user;
    }

    @Transactional
    @Override
    public SavingsAccountTransaction handleDeposit(final SavingsAccount account, final DateTimeFormatter fmt,
            final LocalDate transactionDate, final BigDecimal transactionAmount, final PaymentDetail paymentDetail,
            final boolean isAccountTransfer, final boolean isRegularTransaction) {
        final SavingsAccountTransactionType savingsAccountTransactionType = SavingsAccountTransactionType.DEPOSIT;
        return handleDeposit(account, fmt, transactionDate, transactionAmount, paymentDetail, isAccountTransfer, isRegularTransaction,
                savingsAccountTransactionType);
    }

    private SavingsAccountTransaction handleDeposit(final SavingsAccount account, final DateTimeFormatter fmt,
            final LocalDate transactionDate, final BigDecimal transactionAmount, final PaymentDetail paymentDetail,
            final boolean isAccountTransfer, final boolean isRegularTransaction,
            final SavingsAccountTransactionType savingsAccountTransactionType) {
        AppUser user = getAppUserIfPresent();

        account.validateForAccountBlock();
        account.validateForCreditBlock();
        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();
        final boolean isClientLevelValidationEnabled = this.configurationDomainService.isClientLevelValidationEnabled();

        if (isClientLevelValidationEnabled && account.depositAccountType().isSavingsDeposit() && !isAccountTransfer) {
            ValidationLimit validationLimit = getValidationLimitForClientLevel(account);
            account.validateTransactionAmountByLimit(transactionAmount, validationLimit, SavingsAccountTransactionType.DEPOSIT);
        }

        if (isRegularTransaction && !account.allowDeposit()) {
            throw new DepositAccountTransactionNotAllowedException(account.getId(), "deposit", account.depositAccountType());
        }
        boolean isInterestTransfer = false;
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
        Integer accountType = null;
        final SavingsAccountTransactionDTO transactionDTO = new SavingsAccountTransactionDTO(fmt, transactionDate, transactionAmount,
                paymentDetail, new Date(), user, accountType, isAccountTransfer);
        final SavingsAccountTransaction deposit = account.deposit(transactionDTO, savingsAccountTransactionType);
        final LocalDate postInterestOnDate = null;
        final MathContext mc = MathContext.DECIMAL64;
        if (account.isBeforeLastAccrualPostingPeriod(transactionDate)) {
            account.postAccrualInterest(mc, DateUtils.getLocalDateOfTenant(), isInterestTransfer,
                    isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, postInterestOnDate);
        }
        if (account.isBeforeLastPostingPeriod(transactionDate)) {
            final LocalDate today = DateUtils.getLocalDateOfTenant();
            account.postInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth,
                    postInterestOnDate);
        } else {
            final LocalDate today = DateUtils.getLocalDateOfTenant();
            account.calculateInterestUsing(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth, postInterestOnDate);
        }
        if (isClientLevelValidationEnabled && account.depositAccountType().isSavingsDeposit() && !isAccountTransfer ) {
            ValidationLimit validationLimit = getValidationLimitForClientLevel(account);
            account.validateCummulativeBalanceByLimit(account, validationLimit);
        }

        saveTransactionToGenerateTransactionId(deposit);

        this.savingsAccountRepository.saveAndFlush(account);

        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.SAVINGS_DEPOSIT,
                constructEntityMap(BUSINESS_ENTITY.SAVINGS_TRANSACTION, deposit));
        return deposit;
    }

    @Override
    public SavingsAccountTransaction handleDividendPayout(final SavingsAccount account, final LocalDate transactionDate,
            final BigDecimal transactionAmount) {
        final DateTimeFormatter fmt = null;
        final PaymentDetail paymentDetail = null;
        final boolean isAccountTransfer = false;
        final boolean isRegularTransaction = true;
        final SavingsAccountTransactionType savingsAccountTransactionType = SavingsAccountTransactionType.DIVIDEND_PAYOUT;
        return handleDeposit(account, fmt, transactionDate, transactionAmount, paymentDetail, isAccountTransfer, isRegularTransaction,
                savingsAccountTransactionType);
    }

    private Long saveTransactionToGenerateTransactionId(final SavingsAccountTransaction transaction) {
        this.savingsAccountTransactionRepository.save(transaction);
        return transaction.getId();
    }

    private void updateExistingTransactionsDetails(SavingsAccount account, Set<Long> existingTransactionIds,
            Set<Long> existingReversedTransactionIds) {
        existingTransactionIds.addAll(account.findExistingTransactionIds());
        existingReversedTransactionIds.addAll(account.findExistingReversedTransactionIds());
    }

    private ValidationLimit getValidationLimitForClientLevel(SavingsAccount account) {
        Long clientLevelId = account.getClient().clientLevel().getId();
        return this.validationLimitReadPlatformService.retrieveValidationLimitByClientLevelId(clientLevelId);
    }

    private void ValidateDailyWithdrawalLimit(SavingsAccount account, LocalDate transactionDate, BigDecimal transactionAmount,
            Double dailyWithdrawalLimitDefaultValue) {
        final ArrayList<Long> reversalTransactions = this.savingsAccountReadPlatformService
                .fetchReversalTransactionRequestList(account.getId());
        BigDecimal totalWithdrawOnDate = transactionAmount;
        if (account.productId() != 32 && account.productId() != 36 && account.productId() != 30) {
            for (SavingsAccount acc : this.savingAccountAssembler.findSavingAccountByClientId(account.clientId())) {
                for (SavingsAccountTransaction tran : acc.getTransactions()) {
                    if (!tran.isReversed() && tran.isWithdrawal() && !reversalTransactions.contains(tran.getId())
                            && tran.getTransactionLocalDate().isEqual(transactionDate)) {
                        totalWithdrawOnDate = totalWithdrawOnDate.add(tran.getAmount());

                    }
                }
            }
        }

        if (account.getClient().getDailyWithdrawLimit().doubleValue() > 0) {
            if (totalWithdrawOnDate.doubleValue() > account.getClient().getDailyWithdrawLimit().doubleValue()) {
                throw new ExceedDailyWithdrawLimitException("Withdraw Exceeding daily withdraw limit withdraw not allowed");
            }
        } else if (totalWithdrawOnDate.doubleValue() > dailyWithdrawalLimitDefaultValue) {
            throw new ExceedDailyWithdrawLimitException("Withdraw Exceeding daily withdraw limit withdraw not allowed");
        }

    }

    @Transactional
    @Override
    public void postJournalEntries(final SavingsAccount account, final Set<Long> existingTransactionIds,
            final Set<Long> existingReversedTransactionIds) {

        final MonetaryCurrency currency = account.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepositoryWrapper.findOneWithNotFoundDetection(currency);

        final Map<String, Object> accountingBridgeData = account.deriveAccountingBridgeData(applicationCurrency.toData(),
                existingTransactionIds, existingReversedTransactionIds);
        this.journalEntryWritePlatformService.createJournalEntriesForSavings(accountingBridgeData);
    }

    private Map<BUSINESS_ENTITY, Object> constructEntityMap(final BUSINESS_ENTITY entityEvent, Object entity) {
        Map<BUSINESS_ENTITY, Object> map = new HashMap<>(1);
        map.put(entityEvent, entity);
        return map;
    }
}