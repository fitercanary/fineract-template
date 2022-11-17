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
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormat;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormatRepositoryWrapper;
import org.apache.fineract.infrastructure.accountnumberformat.domain.EntityAccountType;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.data.AccountTransferDTO;
import org.apache.fineract.portfolio.account.domain.AccountTransferType;
import org.apache.fineract.portfolio.account.service.AccountTransfersWritePlatformService;
import org.apache.fineract.portfolio.calendar.domain.Calendar;
import org.apache.fineract.portfolio.calendar.domain.CalendarEntityType;
import org.apache.fineract.portfolio.calendar.domain.CalendarFrequencyType;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstance;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstanceRepository;
import org.apache.fineract.portfolio.calendar.domain.CalendarType;
import org.apache.fineract.portfolio.calendar.service.CalendarUtils;
import org.apache.fineract.portfolio.charge.domain.ChargeCalculationType;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.client.domain.AccountNumberGenerator;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.savings.DepositAccountOnClosureType;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.DepositsApiConstants;
import org.apache.fineract.portfolio.savings.SavingsApiConstants;
import org.apache.fineract.portfolio.savings.SavingsTransactionBooleanValues;
import org.apache.fineract.portfolio.savings.request.FixedDepositPreClosureReq;
import org.apache.fineract.portfolio.savings.service.NubanAccountService;
import org.apache.fineract.portfolio.savings.service.SavingsAccountWritePlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsEnumerations;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.apache.fineract.portfolio.savings.DepositsApiConstants.FIXED_DEPOSIT_ACCOUNT_RESOURCE_NAME;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.noteParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.onAccountClosureIdParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.toSavingsAccountIdParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.transferDescriptionParamName;

@Service
public class DepositAccountDomainServiceJpa implements DepositAccountDomainService {

    private final PlatformSecurityContext context;
    private final SavingsAccountRepositoryWrapper savingsAccountRepository;
    private final SavingsAccountChargeRepository savingsAccountChargeRepository;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final AccountNumberGenerator accountNumberGenerator;
    private final DepositAccountAssembler depositAccountAssembler;
    private final SavingsAccountDomainService savingsAccountDomainService;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private final ConfigurationDomainService configurationDomainService;
    private final AccountNumberFormatRepositoryWrapper accountNumberFormatRepository;
    private final CalendarInstanceRepository calendarInstanceRepository;
    private final NubanAccountService nubanAccountService;
    private final SavingsAccountWritePlatformService savingsAccountWritePlatformService;

    @Autowired
    public DepositAccountDomainServiceJpa(final PlatformSecurityContext context,
            final SavingsAccountRepositoryWrapper savingsAccountRepository, SavingsAccountChargeRepository savingsAccountChargeRepository,
            final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper,
            final JournalEntryWritePlatformService journalEntryWritePlatformService, final AccountNumberGenerator accountNumberGenerator,
            final DepositAccountAssembler depositAccountAssembler, final SavingsAccountDomainService savingsAccountDomainService,
            final AccountTransfersWritePlatformService accountTransfersWritePlatformService,
            final ConfigurationDomainService configurationDomainService,
            final AccountNumberFormatRepositoryWrapper accountNumberFormatRepository,
            final CalendarInstanceRepository calendarInstanceRepository, NubanAccountService nubanAccountService,
            SavingsAccountWritePlatformService savingsAccountWritePlatformService) {
        this.context = context;
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsAccountChargeRepository = savingsAccountChargeRepository;
        this.applicationCurrencyRepositoryWrapper = applicationCurrencyRepositoryWrapper;
        this.journalEntryWritePlatformService = journalEntryWritePlatformService;
        this.accountNumberGenerator = accountNumberGenerator;
        this.depositAccountAssembler = depositAccountAssembler;
        this.savingsAccountDomainService = savingsAccountDomainService;
        this.accountTransfersWritePlatformService = accountTransfersWritePlatformService;
        this.configurationDomainService = configurationDomainService;
        this.accountNumberFormatRepository = accountNumberFormatRepository;
        this.calendarInstanceRepository = calendarInstanceRepository;
        this.nubanAccountService = nubanAccountService;
        this.savingsAccountWritePlatformService = savingsAccountWritePlatformService;
    }

    @Transactional
    @Override
    public SavingsAccountTransaction handleWithdrawal(final SavingsAccount account, final DateTimeFormatter fmt,
            final LocalDate transactionDate, final BigDecimal transactionAmount, final PaymentDetail paymentDetail,
            final boolean applyWithdrawFee, final boolean isRegularTransaction) {
        boolean isAccountTransfer = false;
        boolean isInterestTransfer = false;
        boolean isWithdrawBalance = false;

        SavingsTransactionBooleanValues transactionBooleanValues = new SavingsTransactionBooleanValues(isAccountTransfer,
                isRegularTransaction, applyWithdrawFee, isInterestTransfer, isWithdrawBalance);
        return this.savingsAccountDomainService.handleWithdrawal(account, fmt, transactionDate, transactionAmount, paymentDetail,
                transactionBooleanValues, false, null, null);
    }

    @Transactional
    @Override
    public SavingsAccountTransaction handleFDDeposit(final FixedDepositAccount account, final DateTimeFormatter fmt,
            final LocalDate transactionDate, final BigDecimal transactionAmount, final PaymentDetail paymentDetail) {
        boolean isAccountTransfer = false;
        boolean isRegularTransaction = false;
        return this.savingsAccountDomainService.handleDeposit(account, fmt, transactionDate, transactionAmount, paymentDetail,
                isAccountTransfer, isRegularTransaction, null);
    }

    @Transactional
    @Override
    public SavingsAccountTransaction handleRDDeposit(final RecurringDepositAccount account, final DateTimeFormatter fmt,
            final LocalDate transactionDate, final BigDecimal transactionAmount, final PaymentDetail paymentDetail,
            final boolean isRegularTransaction) {
        AppUser user = getAppUserIfPresent();
        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        boolean isAccountTransfer = false;
        final boolean isPreMatureClosure = false;
        final MathContext mc = MathContext.DECIMAL64;
        account.updateDepositAmount(transactionAmount);
        final SavingsAccountTransaction deposit = this.savingsAccountDomainService.handleDeposit(account, fmt, transactionDate,
                transactionAmount, paymentDetail, isAccountTransfer, isRegularTransaction, null);
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        final boolean isAnyActivationChargesDue = isAnyActivationChargesDue(account);
        if (isAnyActivationChargesDue) {
            updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
            account.processAccountUponActivation(isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, user);
            this.savingsAccountRepository.saveAndFlush(account);
        }
        account.handleScheduleInstallments(deposit);
        account.updateMaturityDateAndAmount(mc, isPreMatureClosure, isSavingsInterestPostingAtCurrentPeriodEnd,
                financialYearBeginningMonth);
        account.updateOverduePayments(DateUtils.getLocalDateOfTenant());
        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        return deposit;
    }

    @Transactional
    @Override
    public SavingsAccountTransaction handleSavingDeposit(final SavingsAccount account, final DateTimeFormatter fmt,
            final LocalDate transactionDate, final BigDecimal transactionAmount, final PaymentDetail paymentDetail,
            final boolean isRegularTransaction) {
        final SavingsAccountTransaction deposit = this.savingsAccountDomainService.handleDeposit(account, fmt, transactionDate,
                transactionAmount, paymentDetail, false, isRegularTransaction, null);
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        return deposit;
    }

    private boolean isAnyActivationChargesDue(final RecurringDepositAccount account) {
        for (final SavingsAccountCharge savingsAccountCharge : account.charges()) {
            if (savingsAccountCharge.isSavingsActivation() && savingsAccountCharge.amoutOutstanding() != null
                    && savingsAccountCharge.amoutOutstanding().compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    @Override
    public Long handleFDAccountClosure(final FixedDepositAccount account, final PaymentDetail paymentDetail, final AppUser user,
            final JsonCommand command, final LocalDate tenantsTodayDate, final Map<String, Object> changes) {

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        boolean isRegularTransaction = false;
        boolean isAccountTransfer = false;
        final boolean isPreMatureClosure = false;
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        /***
         * Update account transactionIds for post journal entries.
         */
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
        /*
         * final SavingsAccountTransactionDTO transactionDTO = new
         * SavingsAccountTransactionDTO(fmt, transactionDate, transactionAmount,
         * paymentDetail, new Date()); final SavingsAccountTransaction deposit =
         * account.deposit(transactionDTO); boolean isInterestTransfer = false;
         * final MathContext mc = MathContext.DECIMAL64; if
         * (account.isBeforeLastPostingPeriod(transactionDate)) { final
         * LocalDate today = DateUtils.getLocalDateOfTenant();
         * account.postInterest(mc, today, isInterestTransfer); } else { final
         * LocalDate today = DateUtils.getLocalDateOfTenant();
         * account.calculateInterestUsing(mc, today, isInterestTransfer);
         * =======
         */
        final MathContext mc = MathContext.DECIMAL64;
        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
        final LocalDate closedDate = command.localDateValueOfParameterNamed(SavingsApiConstants.closedOnDateParamName);
        Long savingsTransactionId = null;
        account.postMaturityInterest(isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth);
        final Integer onAccountClosureId = command.integerValueOfParameterNamed(onAccountClosureIdParamName);
        final DepositAccountOnClosureType onClosureType = DepositAccountOnClosureType.fromInt(onAccountClosureId);
        if (onClosureType.isReinvest()) {
            FixedDepositAccount reinvestedDeposit = account.reInvest(account.getAccountBalance());
            this.depositAccountAssembler.assignSavingAccountHelpers(reinvestedDeposit);
            reinvestedDeposit.updateMaturityDateAndAmountBeforeAccountActivation(mc, isPreMatureClosure,
                    isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth);
            this.savingsAccountRepository.save(reinvestedDeposit);
            autoGenerateAccountNumber(reinvestedDeposit);
            final SavingsAccountTransaction withdrawal = this.handleWithdrawal(account, fmt, closedDate, account.getAccountBalance(),
                    paymentDetail, false, isRegularTransaction);
            savingsTransactionId = withdrawal.getId();
        } else if (onClosureType.isTransferToSavings()) {
            final Long toSavingsId = command.longValueOfParameterNamed(toSavingsAccountIdParamName);
            final String transferDescription = command.stringValueOfParameterNamed(transferDescriptionParamName);
            final SavingsAccount toSavingsAccount = this.depositAccountAssembler.assembleFrom(toSavingsId,
                    DepositAccountType.SAVINGS_DEPOSIT);
            final boolean isExceptionForBalanceCheck = false;
            final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(closedDate, account.getAccountBalance(),
                    PortfolioAccountType.SAVINGS, PortfolioAccountType.SAVINGS, null, null, transferDescription, locale, fmt, null, null,
                    null, null, null, AccountTransferType.ACCOUNT_TRANSFER.getValue(), null, null, null, null, toSavingsAccount, account,
                    isAccountTransfer, isExceptionForBalanceCheck);
            this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
            updateAlreadyPostedTransactions(existingTransactionIds, account);
        } else {
            final SavingsAccountTransaction withdrawal = this.handleWithdrawal(account, fmt, closedDate, account.getAccountBalance(),
                    paymentDetail, false, isRegularTransaction);
            savingsTransactionId = withdrawal.getId();
        }

        account.close(user, command, tenantsTodayDate, changes);
        this.savingsAccountRepository.save(account);

        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);

        return savingsTransactionId;
    }

    @Transactional
    @Override
    public Long handleRDAccountClosure(final RecurringDepositAccount account, final PaymentDetail paymentDetail, final AppUser user,
            final JsonCommand command, final LocalDate tenantsTodayDate, final Map<String, Object> changes) {

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        boolean isRegularTransaction = false;
        boolean isAccountTransfer = false;
        final boolean isPreMatureClosure = false;
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        /***
         * Update account transactionIds for post journal entries.
         */
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);

        final MathContext mc = MathContext.DECIMAL64;
        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
        final LocalDate closedDate = command.localDateValueOfParameterNamed(SavingsApiConstants.closedOnDateParamName);
        Long savingsTransactionId = null;
        account.postMaturityInterest(isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, closedDate);
        final BigDecimal transactionAmount = account.getAccountBalance();
        final Integer onAccountClosureId = command.integerValueOfParameterNamed(onAccountClosureIdParamName);
        final DepositAccountOnClosureType onClosureType = DepositAccountOnClosureType.fromInt(onAccountClosureId);
        if (onClosureType.isReinvest()) {
            RecurringDepositAccount reinvestedDeposit = account.reInvest(transactionAmount);
            depositAccountAssembler.assignSavingAccountHelpers(reinvestedDeposit);
            this.savingsAccountRepository.save(reinvestedDeposit);
            final CalendarInstance calendarInstance = getCalendarInstance(account, reinvestedDeposit);
            this.calendarInstanceRepository.save(calendarInstance);
            final Calendar calendar = calendarInstance.getCalendar();
            final PeriodFrequencyType frequencyType = CalendarFrequencyType.from(CalendarUtils.getFrequency(calendar.getRecurrence()));
            Integer frequency = CalendarUtils.getInterval(calendar.getRecurrence());
            frequency = frequency == -1 ? 1 : frequency;
            reinvestedDeposit.generateSchedule(frequencyType, frequency, calendar);
            reinvestedDeposit.processAccountUponActivation(fmt, user);
            reinvestedDeposit.updateMaturityDateAndAmount(mc, isPreMatureClosure, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth);
            this.savingsAccountRepository.save(reinvestedDeposit);
            autoGenerateAccountNumber(reinvestedDeposit);

            final SavingsAccountTransaction withdrawal = this.handleWithdrawal(account, fmt, closedDate, account.getAccountBalance(),
                    paymentDetail, false, isRegularTransaction);
            savingsTransactionId = withdrawal.getId();

        } else if (onClosureType.isTransferToSavings()) {
            final Long toSavingsId = command.longValueOfParameterNamed(toSavingsAccountIdParamName);
            final String transferDescription = command.stringValueOfParameterNamed(transferDescriptionParamName);
            final SavingsAccount toSavingsAccount = this.depositAccountAssembler.assembleFrom(toSavingsId,
                    DepositAccountType.SAVINGS_DEPOSIT);
            final boolean isExceptionForBalanceCheck = false;
            final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(closedDate, transactionAmount,
                    PortfolioAccountType.SAVINGS, PortfolioAccountType.SAVINGS, null, null, transferDescription, locale, fmt, null, null,
                    null, null, null, AccountTransferType.ACCOUNT_TRANSFER.getValue(), null, null, null, null, toSavingsAccount, account,
                    isRegularTransaction, isExceptionForBalanceCheck);
            this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
            updateAlreadyPostedTransactions(existingTransactionIds, account);
        } else {
            final SavingsAccountTransaction withdrawal = this.handleWithdrawal(account, fmt, closedDate, account.getAccountBalance(),
                    paymentDetail, false, isRegularTransaction);
            savingsTransactionId = withdrawal.getId();
        }

        account.close(user, command, tenantsTodayDate, changes);

        this.savingsAccountRepository.save(account);

        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);

        return savingsTransactionId;
    }

    private CalendarInstance getCalendarInstance(final RecurringDepositAccount account, RecurringDepositAccount reinvestedDeposit) {
        CalendarInstance calendarInstance = null;
        CalendarInstance parentCalendarInstance = this.calendarInstanceRepository.findByEntityIdAndEntityTypeIdAndCalendarTypeId(
                account.getId(), CalendarEntityType.SAVINGS.getValue(), CalendarType.COLLECTION.getValue());
        if (account.isCalendarInherited()) {
            calendarInstance = CalendarInstance.from(parentCalendarInstance.getCalendar(), account.getId(),
                    CalendarEntityType.SAVINGS.getValue());
        } else {
            LocalDate calendarStartDate = reinvestedDeposit.depositStartDate();
            Calendar parentCalendar = parentCalendarInstance.getCalendar();
            final String recurrence = parentCalendar.getRecurrence();
            final String title = "recurring_savings_" + reinvestedDeposit.getId();
            final Calendar calendar = Calendar.createRepeatingCalendar(title, calendarStartDate, CalendarType.COLLECTION.getValue(),
                    recurrence);
            calendarInstance = CalendarInstance.from(calendar, reinvestedDeposit.getId(), CalendarEntityType.SAVINGS.getValue());
        }
        if (calendarInstance == null) {
            final String defaultUserMessage = "No valid recurring details available for recurring depost account creation.";
            throw new GeneralPlatformDomainRuleException(
                    "error.msg.recurring.deposit.account.cannot.create.no.valid.recurring.details.available", defaultUserMessage,
                    account.clientId());
        }
        return calendarInstance;
    }

    private void autoGenerateAccountNumber(final SavingsAccount account) {
        if (account.isAccountNumberRequiresAutoGeneration()) {
            final AccountNumberFormat accountNumberFormat = this.accountNumberFormatRepository.findByAccountType(EntityAccountType.SAVINGS);
            account.updateAccountNo(this.accountNumberGenerator.generate(account, accountNumberFormat));
            String serialNumber = account.getAccountNumber();
            String nubanAccountNumber = this.nubanAccountService.generateNubanAccountNumber(serialNumber, "1");
            SavingsAccount existingAccount = this.savingsAccountRepository.findByAccountNumber(nubanAccountNumber);
            while (existingAccount != null) {
                serialNumber = this.nubanAccountService.generateNextSerialNumber(serialNumber);
                nubanAccountNumber = this.nubanAccountService.generateNubanAccountNumber(serialNumber, "1");
                existingAccount = this.savingsAccountRepository.findByAccountNumber(nubanAccountNumber);
            }
            account.updateAccountNo(nubanAccountNumber);
            this.savingsAccountRepository.save(account);
        }
    }

    @Transactional
    @Override
    public Long prematurelyCloseFDAccount(FixedDepositAccount account, PaymentDetail paymentDetail,
                                          FixedDepositPreClosureReq fixedDepositPreclosureReq, Map<String, Object> changes) {
        final AppUser user = this.context.authenticatedUser();

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final boolean isPreMatureClosure = true;
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        /***
         * Update account transactionIds for post journal entries.
         */
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);

        final LocalDate closedDate = fixedDepositPreclosureReq.getClosedDate();
        final DateTimeFormatter fmt = fixedDepositPreclosureReq.getFormatter();
        Long savingsTransactionId = null;

        DepositAccountOnClosureType closureType = fixedDepositPreclosureReq.getClosureType();

        // post interest
        account.postPreMaturityInterest(closedDate, isPreMatureClosure, isSavingsInterestPostingAtCurrentPeriodEnd,
                financialYearBeginningMonth, !fixedDepositPreclosureReq.isTopUp());

        boolean applyWithdrawalFeeForTransfer = account.withdrawalFeeApplicableForTransfer;
        if (account.shouldApplyPreclosureCharges()) {
            // Apply pre-closure charge
            this.applyPreclosureCharges(account, user, closedDate);
            if (applyWithdrawalFeeForTransfer || !closureType.isTransferToSavings()) {
                // Apply withdrawal charges
                List<SavingsAccountCharge> withdrawalCharges = this.savingsAccountChargeRepository.findWithdrawalFeeByAccountId(account.getId(),
                        ChargeTimeType.WITHDRAWAL_FEE.getValue());
                for (SavingsAccountCharge charge : withdrawalCharges) {
                    charge.setAmountOutstanding(charge.amount());
                    this.savingsAccountWritePlatformService.payCharge(charge, closedDate, charge.amount(),
                            DateTimeFormat.forPattern("dd MM yyyy"), user);
                }
            }
        }
        if (closureType.isTransferToSavings()) {
            final boolean isExceptionForBalanceCheck = false;
            final Long toSavingsId = fixedDepositPreclosureReq.getToSavingsId();
            final String transferDescription = fixedDepositPreclosureReq.getTransferDescription();
            final SavingsAccount toSavingsAccount = this.depositAccountAssembler.assembleFrom(toSavingsId,
                    DepositAccountType.SAVINGS_DEPOSIT);
            account.withdrawalFeeApplicableForTransfer = false;
            final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(closedDate, account.getAccountBalance(),
                    PortfolioAccountType.SAVINGS, PortfolioAccountType.SAVINGS, null, null, transferDescription, fixedDepositPreclosureReq.getLocale(), fmt, null, null,
                    null, null, null, AccountTransferType.ACCOUNT_TRANSFER.getValue(), null, null, null, null, toSavingsAccount, account,
                    false, isExceptionForBalanceCheck);
            this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
            updateAlreadyPostedTransactions(existingTransactionIds, account);
            account.withdrawalFeeApplicableForTransfer = applyWithdrawalFeeForTransfer;
        } else {
            final SavingsAccountTransaction withdrawal = this.handleWithdrawal(account, fmt, closedDate, account.getAccountBalance(),
                    paymentDetail, false, false);
            savingsTransactionId = withdrawal.getId();
        }

        this.prematurelyCloseFD(account, fixedDepositPreclosureReq, changes);
        // Force interest earned to equal interest posted when FD is prematurely closed
        account.getSummary().setTotalInterestEarned(account.getSummary().getTotalInterestPosted());
        this.savingsAccountRepository.save(account);

        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);

        return savingsTransactionId;
    }

    private void applyPreclosureCharges(SavingsAccount account, AppUser user, LocalDate closedDate) {
        List<SavingsAccountCharge> preclosureCharges = this.savingsAccountChargeRepository.findFdaPreclosureCharges(account.getId(),
                Arrays.asList(ChargeTimeType.FDA_PRE_CLOSURE_FEE.getValue(), ChargeTimeType.FDA_PARTIAL_LIQUIDATION_FEE.getValue()));
        SavingsAccountTransaction withholdTaxTransaction = account.getTransactions()
                .stream().filter(SavingsAccountTransaction::isWithHoldTaxAndNotReversed).findFirst().orElse(null);
        for (SavingsAccountCharge charge : preclosureCharges) {
            BigDecimal amount = account.getSummary().getTotalInterestPosted() != null ? account.getSummary().getTotalInterestPosted()
                    : account.getSummary().getTotalInterestEarned();
            ChargeCalculationType chargeCalculationType = ChargeCalculationType.fromInt(charge.getCharge().getChargeCalculation());
            if (chargeCalculationType.isPercentageOfAmount()) {
                amount = account.getAccountBalance();
            }
            if (withholdTaxTransaction != null) {
                amount = amount.subtract(withholdTaxTransaction.getAmount());
            }
            if (chargeCalculationType.isPercentageBased()) {
                charge.setPercentage(charge.getCharge().getAmount());
                charge.setAmountPercentageAppliedTo(amount);
                charge.setAmount(charge.percentageOf(amount, charge.getPercentage()));
            } else {
                charge.setAmount(charge.amount());
            }
            charge.setAmountOutstanding(charge.amount());
            charge.setChargePaid();
            if (!charge.isPaid()) {
                this.savingsAccountWritePlatformService.payCharge(charge, closedDate, charge.amount(), DateUtils.getDefaultFormatter(), user);
            }
        }
    }

    private void prematurelyCloseFD(FixedDepositAccount account, FixedDepositPreClosureReq fixedDepositPreclosureReq, Map<String, Object> changes) {
        final AppUser user = this.context.authenticatedUser();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(FIXED_DEPOSIT_ACCOUNT_RESOURCE_NAME + DepositsApiConstants.preMatureCloseAction);

        final SavingsAccountStatusType currentStatus = SavingsAccountStatusType.fromInt(account.getStatus());
        if (!currentStatus.isActive()) {
            baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode("not.in.active.state");
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }
        final LocalDate closedDate = fixedDepositPreclosureReq.getClosedDate();

        if (closedDate.isBefore(account.getActivationLocalDate())) {
            baseDataValidator.reset().parameter(SavingsApiConstants.closedOnDateParamName).value(closedDate)
                    .failWithCode("must.be.after.activation.date");
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        if (account.isAccountLocked(closedDate)) {
            baseDataValidator.reset().parameter(SavingsApiConstants.closedOnDateParamName).value(closedDate)
                    .failWithCode("must.be.after.lockin.period");
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        if (closedDate.isAfter(account.maturityDate())) {
            baseDataValidator.reset().parameter(SavingsApiConstants.closedOnDateParamName).value(closedDate)
                    .failWithCode("must.be.before.maturity.date");
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        if (closedDate.isAfter(DateUtils.getLocalDateOfTenant())) {
            baseDataValidator.reset().parameter(SavingsApiConstants.closedOnDateParamName).value(closedDate)
                    .failWithCode("cannot.be.a.future.date");
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }
        final List<SavingsAccountTransaction> savingsAccountTransactions = account.retreiveListOfTransactions();
        if (savingsAccountTransactions.size() > 0) {
            final SavingsAccountTransaction accountTransaction = savingsAccountTransactions.get(savingsAccountTransactions.size() - 1);
            if (accountTransaction.isAfter(closedDate) && !accountTransaction.isAccrualInterestPosting()) {
                baseDataValidator.reset().parameter(SavingsApiConstants.closedOnDateParamName).value(closedDate)
                        .failWithCode("must.be.after.last.transaction.date");
                if (!dataValidationErrors.isEmpty()) {
                    throw new PlatformApiDataValidationException(dataValidationErrors);
                }
            }
        }

        account.validateActivityNotBeforeClientOrGroupTransferDate(SavingsEvent.SAVINGS_CLOSE_ACCOUNT, closedDate);
        account.setStatus(SavingsAccountStatusType.PRE_MATURE_CLOSURE.getValue());

        account.getAccountTermAndPreClosure().updateOnAccountClosureStatus(fixedDepositPreclosureReq.getClosureType());

        changes.put(SavingsApiConstants.statusParamName, SavingsEnumerations.status(account.getStatus()));
        changes.put(SavingsApiConstants.localeParamName, fixedDepositPreclosureReq.getLocale());
        changes.put(SavingsApiConstants.dateFormatParamName, fixedDepositPreclosureReq.getDateFormat());
        changes.put(SavingsApiConstants.closedOnDateParamName, closedDate.toString(fixedDepositPreclosureReq.getFormatter()));

        account.setRejectedOnDate(null);
        account.setRejectedBy(null);
        account.setWithdrawnOnDate(null);
        account.setWithdrawnBy(null);
        account.setClosedOnDate(closedDate.toDate());
        account.setClosedBy(user);
        account.getSummary().updateSummary(account.getCurrency(), account.getSavingsAccountTransactionSummaryWrapper(), account.getTransactions());
    }

    @Transactional
    @Override
    public Long handleRDAccountPreMatureClosure(final RecurringDepositAccount account, final PaymentDetail paymentDetail,
            final AppUser user, final JsonCommand command, final LocalDate tenantsTodayDate, final Map<String, Object> changes) {

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        boolean isAccountTransfer = false;
        final boolean isPreMatureClosure = true;
        boolean isRegularTransaction = false;
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        /***
         * Update account transactionIds for post journal entries.
         */
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);

        final LocalDate closedDate = command.localDateValueOfParameterNamed(SavingsApiConstants.closedOnDateParamName);
        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
        Long savingsTransactionId = null;
        // post interest
        account.postPreMaturityInterest(closedDate, isPreMatureClosure, isSavingsInterestPostingAtCurrentPeriodEnd,
                financialYearBeginningMonth);
        this.applyPreclosureCharges(account, user, closedDate);
        final Integer closureTypeValue = command.integerValueOfParameterNamed(DepositsApiConstants.onAccountClosureIdParamName);
        DepositAccountOnClosureType closureType = DepositAccountOnClosureType.fromInt(closureTypeValue);

        if (closureType.isTransferToSavings()) {
            final boolean isExceptionForBalanceCheck = false;
            final Long toSavingsId = command.longValueOfParameterNamed(toSavingsAccountIdParamName);
            final String transferDescription = command.stringValueOfParameterNamed(transferDescriptionParamName);
            final SavingsAccount toSavingsAccount = this.depositAccountAssembler.assembleFrom(toSavingsId,
                    DepositAccountType.SAVINGS_DEPOSIT);
            final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(closedDate, account.getAccountBalance(),
                    PortfolioAccountType.SAVINGS, PortfolioAccountType.SAVINGS, null, null, transferDescription, locale, fmt, null, null,
                    null, null, null, AccountTransferType.ACCOUNT_TRANSFER.getValue(), null, null, null, null, toSavingsAccount, account,
                    isRegularTransaction, isExceptionForBalanceCheck);
            this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
            updateAlreadyPostedTransactions(existingTransactionIds, account);
        } else {
            final SavingsAccountTransaction withdrawal = this.handleWithdrawal(account, fmt, closedDate, account.getAccountBalance(),
                    paymentDetail, false, isRegularTransaction);
            savingsTransactionId = withdrawal.getId();
        }

        account.prematureClosure(user, command, tenantsTodayDate, changes);
        this.savingsAccountRepository.save(account);
        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        return savingsTransactionId;
    }

    private void updateExistingTransactionsDetails(SavingsAccount account, Set<Long> existingTransactionIds,
            Set<Long> existingReversedTransactionIds) {
        existingTransactionIds.addAll(account.findExistingTransactionIds());
        existingReversedTransactionIds.addAll(account.findExistingReversedTransactionIds());
    }

    private void postJournalEntries(final SavingsAccount savingsAccount, final Set<Long> existingTransactionIds,
            final Set<Long> existingReversedTransactionIds) {

        final MonetaryCurrency currency = savingsAccount.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepositoryWrapper.findOneWithNotFoundDetection(currency);

        final Map<String, Object> accountingBridgeData = savingsAccount.deriveAccountingBridgeData(applicationCurrency.toData(),
                existingTransactionIds, existingReversedTransactionIds);
        this.journalEntryWritePlatformService.createJournalEntriesForSavings(accountingBridgeData, null, null);
    }

    private void updateAlreadyPostedTransactions(final Set<Long> existingTransactionIds, final SavingsAccount savingsAccount) {
        List<SavingsAccountTransaction> transactions = savingsAccount.getTransactions();
        int size = transactions.size();
        for (int i = size - 1;; i--) {
            SavingsAccountTransaction transaction = transactions.get(i);
            if (transaction.isWithdrawal() || transaction.isWithdrawalFee()) {
                existingTransactionIds.add(transaction.getId());
            } else {
                break;
            }
        }
    }

    private AppUser getAppUserIfPresent() {
        AppUser user = null;
        if (this.context != null) {
            user = this.context.getAuthenticatedUserIfPresent();
        }
        return user;
    }
}
