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

import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_ENTITY;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_EVENTS;
import org.apache.fineract.portfolio.common.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.SavingsTransactionBooleanValues;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionDTO;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionDataValidator;
import org.apache.fineract.portfolio.savings.exception.DepositAccountTransactionNotAllowedException;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountDoesNotBelongToClientException;
import org.apache.fineract.portfolio.validation.limit.data.ValidationLimitData;
import org.apache.fineract.portfolio.validation.limit.domain.ValidationLimit;
import org.apache.fineract.portfolio.validation.limit.domain.ValidationLimitRepository;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SavingsAccountDomainServiceJpa implements SavingsAccountDomainService {

    private final PlatformSecurityContext context;
    private final SavingsAccountRepositoryWrapper savingsAccountRepository;
    private final SavingsAccountTransactionRepository savingsAccountTransactionRepository;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final ConfigurationDomainService configurationDomainService;
    private final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final SavingsAccountAssembler savingAccountAssembler;
    private final SavingsAccountTransactionDataValidator savingsAccountTransactionDataValidator;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final ValidationLimitRepository validationLimitRepository;
    private final ClientReadPlatformService clientReadPlatformService;

    @Autowired
    public SavingsAccountDomainServiceJpa(final SavingsAccountRepositoryWrapper savingsAccountRepository,
                                          final SavingsAccountTransactionRepository savingsAccountTransactionRepository,
                                          final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper,
                                          final JournalEntryWritePlatformService journalEntryWritePlatformService,
                                          final ConfigurationDomainService configurationDomainService, final PlatformSecurityContext context,
                                          final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository,
                                          final BusinessEventNotifierService businessEventNotifierService,
                                          final SavingsAccountAssembler savingAccountAssembler, SavingsAccountTransactionDataValidator savingsAccountTransactionDataValidator,
                                          final ClientRepositoryWrapper clientRepositoryWrapper,
                                          final ValidationLimitRepository validationLimitRepository, ClientReadPlatformService clientReadPlatformService) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsAccountTransactionRepository = savingsAccountTransactionRepository;
        this.applicationCurrencyRepositoryWrapper = applicationCurrencyRepositoryWrapper;
        this.journalEntryWritePlatformService = journalEntryWritePlatformService;
        this.configurationDomainService = configurationDomainService;
        this.context = context;
        this.depositAccountOnHoldTransactionRepository = depositAccountOnHoldTransactionRepository;
        this.businessEventNotifierService = businessEventNotifierService;
        this.savingAccountAssembler = savingAccountAssembler;
        this.savingsAccountTransactionDataValidator = savingsAccountTransactionDataValidator;
        this.clientRepositoryWrapper = clientRepositoryWrapper;
        this.validationLimitRepository = validationLimitRepository;
        this.clientReadPlatformService = clientReadPlatformService;
    }

    @Transactional
    @Override
    public SavingsAccountTransaction handleWithdrawal(final SavingsAccount account, final DateTimeFormatter fmt,
            final LocalDate transactionDate, final BigDecimal transactionAmount, final PaymentDetail paymentDetail,
            final SavingsTransactionBooleanValues transactionBooleanValues, final boolean isNotTransferToOtherAccount) {

        AppUser user = getAppUserIfPresent();
        account.validateForAccountBlock();
        account.validateForDebitBlock();
        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();
        final boolean isClientLevelValidationEnabled = this.configurationDomainService.isClientLevelValidationEnabled();

        if (isClientLevelValidationEnabled && account.depositAccountType().isSavingsDeposit() && !isNotTransferToOtherAccount) {
            BigDecimal totalWithdrawOnDate = this.getTotalWithdrawAmountOnDate(account.clientId(), transactionDate, transactionAmount);
            this.savingsAccountTransactionDataValidator.validateWithdrawLimits(account.getClient(), transactionAmount, totalWithdrawOnDate);
        }

        if (transactionBooleanValues.isRegularTransaction() && !account.allowWithdrawal()) {
            throw new DepositAccountTransactionNotAllowedException(account.getId(), "withdraw", account.depositAccountType());
        }
        final Set<Long> existingTransactionIds = new HashSet<>();
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
                    isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, null);
        }
        if (account.isBeforeLastPostingPeriod(transactionDate)) {
            final LocalDate today = DateUtils.getLocalDateOfTenant();
            account.postInterest(mc, today, transactionBooleanValues.isInterestTransfer(), isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth, null);
        } else {
            final LocalDate today = DateUtils.getLocalDateOfTenant();
            account.calculateInterestUsing(mc, today, transactionBooleanValues.isInterestTransfer(),
                    isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, null);
        }
        List<DepositAccountOnHoldTransaction> depositAccountOnHoldTransactions = null;
        if (account.getOnHoldFunds().compareTo(BigDecimal.ZERO) == 1) {
            depositAccountOnHoldTransactions = this.depositAccountOnHoldTransactionRepository
                    .findBySavingsAccountAndReversedFalseOrderByCreatedDateAsc(account);
        }
        account.validateAccountBalanceDoesNotBecomeNegative(transactionAmount, transactionBooleanValues.isExceptionForBalanceCheck(),
                depositAccountOnHoldTransactions);
        saveTransactionToGenerateTransactionId(withdrawal);
        this.savingsAccountRepository.save(account);

        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.SAVINGS_WITHDRAWAL,
                constructEntityMap(withdrawal));
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
            this.savingsAccountTransactionDataValidator.validateSingleDepositLimits(account.getClient(), transactionAmount);
        }

        if (isRegularTransaction && !account.allowDeposit()) {
            throw new DepositAccountTransactionNotAllowedException(account.getId(), "deposit", account.depositAccountType());
        }

        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
        Integer accountType = null;
        final SavingsAccountTransactionDTO transactionDTO = new SavingsAccountTransactionDTO(fmt, transactionDate, transactionAmount,
                paymentDetail, new Date(), user, accountType, isAccountTransfer);
        final SavingsAccountTransaction deposit = account.deposit(transactionDTO, savingsAccountTransactionType);

        final MathContext mc = MathContext.DECIMAL64;
        if (account.isBeforeLastAccrualPostingPeriod(transactionDate)) {
            account.postAccrualInterest(mc, DateUtils.getLocalDateOfTenant(), false,
                    isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, null);
        }
        if (account.isBeforeLastPostingPeriod(transactionDate)) {
            final LocalDate today = DateUtils.getLocalDateOfTenant();
            account.postInterest(mc, today, false, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth,
                    null);
        } else {
            final LocalDate today = DateUtils.getLocalDateOfTenant();
            account.calculateInterestUsing(mc, today, false, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth, null);
        }
        if (isClientLevelValidationEnabled && account.depositAccountType().isSavingsDeposit() && !isAccountTransfer ) {
            this.savingsAccountTransactionDataValidator.validateCumulativeBalanceByLimit(account, transactionAmount);
        }

        saveTransactionToGenerateTransactionId(deposit);

        this.savingsAccountRepository.saveAndFlush(account);

        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.SAVINGS_DEPOSIT,
                constructEntityMap(deposit));
        return deposit;
    }

    @Override
    public SavingsAccountTransaction handleDividendPayout(final SavingsAccount account, final LocalDate transactionDate,
            final BigDecimal transactionAmount) {
        final boolean isAccountTransfer = false;
        final boolean isRegularTransaction = true;
        final SavingsAccountTransactionType savingsAccountTransactionType = SavingsAccountTransactionType.DIVIDEND_PAYOUT;
        return handleDeposit(account, null, transactionDate, transactionAmount, null, isAccountTransfer, isRegularTransaction,
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

    private BigDecimal getTotalWithdrawAmountOnDate(Long clientId, LocalDate transactionDate, BigDecimal transactionAmount) {

        BigDecimal totalWithdrawOnDate = transactionAmount;
        for (SavingsAccount acc : this.savingAccountAssembler.findSavingAccountByClientId(clientId)) {
            if (acc.depositAccountType().isSavingsDeposit()) {
                for (SavingsAccountTransaction tran : acc.getTransactions()) {
                    if (!tran.isReversed() && tran.isWithdrawal() &&
                            tran.getTransactionLocalDate().isEqual(transactionDate) &&
                            !tran.getIsAccountTransfer()) {
                        totalWithdrawOnDate = totalWithdrawOnDate.add(tran.getAmount());
                    }
                }
            }
        }
        return totalWithdrawOnDate;
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

    private Map<BUSINESS_ENTITY, Object> constructEntityMap(Object entity) {
        Map<BUSINESS_ENTITY, Object> map = new HashMap<>(1);
        map.put(BUSINESS_ENTITY.SAVINGS_TRANSACTION, entity);
        return map;
    }
    
    @Transactional
    @Override
    public ValidationLimitData getCurrentValidationLimitsOnDate(Long clientId, LocalDate transactionDate, Long savingsAccountId) {

        this.clientReadPlatformService.validateUserHasAuthorityToViewClient(clientId);
        Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);
        
        SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsAccountId);
        
        // verify account belongs to client
        if(account.clientId().compareTo(client.getId()) != 0) {
            throw new SavingsAccountDoesNotBelongToClientException(savingsAccountId, clientId);
        }
               
        BigDecimal totalWithdrawOnDate = this.getTotalWithdrawAmountOnDate(clientId, transactionDate, BigDecimal.ZERO);
        
        BigDecimal  cumulativeBalanceOnDate = account.getAccountBalance();
        
        ValidationLimit validationLimit = this.validationLimitRepository.findByClientLevelId(client.clientLevelId());
        
        
        if(validationLimit != null ) {
            
            // Maximum Daily Withdraw Limit
            BigDecimal dailyWithdrawLimit = client.getDailyWithdrawLimit(); 
            
            if(BigDecimal.ZERO.equals(dailyWithdrawLimit)) {
                totalWithdrawOnDate = validationLimit.getMaximumDailyWithdrawLimit() != null ?
                        validationLimit.getMaximumDailyWithdrawLimit().subtract(totalWithdrawOnDate) : null;
            }else {
                totalWithdrawOnDate = dailyWithdrawLimit.subtract(totalWithdrawOnDate);
            }
            
            // Cumulative Balance 
            cumulativeBalanceOnDate = validationLimit.getMaximumCumulativeBalance() != null ?
                        validationLimit.getMaximumCumulativeBalance().subtract(cumulativeBalanceOnDate) : null;
            
            // Single Withdraw Limit
            BigDecimal singleWithdrawLimit = client.getSingleWithdrawLimit(); 
            
            if( BigDecimal.ZERO.equals(singleWithdrawLimit)) {
                singleWithdrawLimit = validationLimit.getMaximumSingleWithdrawLimit();
            }
                    
            return ValidationLimitData.instance(null, null, validationLimit.getMaximumSingleDepositAmount(), 
                            cumulativeBalanceOnDate, singleWithdrawLimit, totalWithdrawOnDate, null, null);
        }
        
        return null;
        
        
    }
    
    @SuppressWarnings("unused")
    private BigDecimal getCumulativeBalanceOnDate(Long clientId) {

        BigDecimal balance = BigDecimal.ZERO;
        for (SavingsAccount account : this.savingAccountAssembler.findSavingAccountByClientId(clientId)) {
            
            BigDecimal accountBalance = account.getSummary().getAccountBalance() != null ? account.getSummary().getAccountBalance() : BigDecimal.ZERO;
            balance = balance.add(accountBalance);
        }
        return balance;
    }
}