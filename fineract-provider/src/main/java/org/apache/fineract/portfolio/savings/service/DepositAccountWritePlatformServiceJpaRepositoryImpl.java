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

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformServiceUnavailableException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.holiday.domain.HolidayRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysRepositoryWrapper;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.data.AccountTransferDTO;
import org.apache.fineract.portfolio.account.data.PortfolioAccountData;
import org.apache.fineract.portfolio.account.domain.AccountAssociationType;
import org.apache.fineract.portfolio.account.domain.AccountAssociations;
import org.apache.fineract.portfolio.account.domain.AccountAssociationsRepository;
import org.apache.fineract.portfolio.account.domain.AccountTransferType;
import org.apache.fineract.portfolio.account.service.AccountAssociationsReadPlatformService;
import org.apache.fineract.portfolio.account.service.AccountTransfersReadPlatformService;
import org.apache.fineract.portfolio.account.service.AccountTransfersWritePlatformService;
import org.apache.fineract.portfolio.calendar.domain.Calendar;
import org.apache.fineract.portfolio.calendar.domain.CalendarEntityType;
import org.apache.fineract.portfolio.calendar.domain.CalendarFrequencyType;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstance;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstanceRepository;
import org.apache.fineract.portfolio.calendar.domain.CalendarType;
import org.apache.fineract.portfolio.calendar.service.CalendarUtils;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.exception.GroupNotActiveException;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.paymentdetail.service.PaymentDetailWritePlatformService;
import org.apache.fineract.portfolio.savings.DepositAccountOnClosureType;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.DepositsApiConstants;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.SavingsApiConstants;
import org.apache.fineract.portfolio.savings.SavingsPeriodFrequencyType;
import org.apache.fineract.portfolio.savings.data.DepositAccountTransactionDataValidator;
import org.apache.fineract.portfolio.savings.data.SavingsAccountChargeDataValidator;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionDTO;
import org.apache.fineract.portfolio.savings.domain.DepositAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.DepositAccountDomainService;
import org.apache.fineract.portfolio.savings.domain.DepositAccountOnHoldTransaction;
import org.apache.fineract.portfolio.savings.domain.DepositAccountOnHoldTransactionRepository;
import org.apache.fineract.portfolio.savings.domain.DepositAccountRecurringDetail;
import org.apache.fineract.portfolio.savings.domain.FixedDepositAccount;
import org.apache.fineract.portfolio.savings.domain.RecurringDepositAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountCharge;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountChargeRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransactionRepository;
import org.apache.fineract.portfolio.savings.exception.DepositAccountTransactionNotAllowedException;
import org.apache.fineract.portfolio.savings.exception.InsufficientAccountBalanceException;
import org.apache.fineract.portfolio.savings.exception.PostInterestAsOnDateException;
import org.apache.fineract.portfolio.savings.exception.PostInterestAsOnDateException.PostInterestAsOnException_TYPE;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountNotFoundException;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountTransactionNotFoundException;
import org.apache.fineract.portfolio.savings.exception.TransactionUpdateNotAllowedException;
import org.apache.fineract.portfolio.savings.request.FixedDepositActivationReq;
import org.apache.fineract.portfolio.savings.request.FixedDepositApplicationPreClosureReq;
import org.apache.fineract.portfolio.savings.request.FixedDepositApplicationReq;
import org.apache.fineract.portfolio.savings.request.FixedDepositApplicationTermsReq;
import org.apache.fineract.portfolio.savings.request.FixedDepositApprovalReq;
import org.apache.fineract.portfolio.savings.request.FixedDepositPreClosureReq;
import org.apache.fineract.portfolio.savings.request.SavingsAccountChargeReq;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.apache.fineract.portfolio.savings.DepositsApiConstants.RECURRING_DEPOSIT_ACCOUNT_RESOURCE_NAME;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.depositAmountParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.depositPeriodFrequencyIdParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.depositPeriodParamName;
import static org.apache.fineract.portfolio.savings.DepositsApiConstants.liquidationAmountParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.amountParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.chargeIdParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.dueAsOfDateParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.submittedOnDateParamName;

@Service
public class DepositAccountWritePlatformServiceJpaRepositoryImpl implements DepositAccountWritePlatformService {

    private final PlatformSecurityContext context;
    private final SavingsAccountRepositoryWrapper savingAccountRepositoryWrapper;
    private final SavingsAccountTransactionRepository savingsAccountTransactionRepository;
    private final DepositAccountAssembler depositAccountAssembler;
    private final DepositAccountTransactionDataValidator depositAccountTransactionDataValidator;
    private final SavingsAccountChargeDataValidator savingsAccountChargeDataValidator;
    private final PaymentDetailWritePlatformService paymentDetailWritePlatformService;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final DepositAccountDomainService depositAccountDomainService;
    private final NoteRepository noteRepository;
    private final AccountTransfersReadPlatformService accountTransfersReadPlatformService;
    private final ChargeRepositoryWrapper chargeRepository;
    private final SavingsAccountChargeRepositoryWrapper savingsAccountChargeRepository;
    private final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private final DepositAccountReadPlatformService depositAccountReadPlatformService;
    private final CalendarInstanceRepository calendarInstanceRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final HolidayRepositoryWrapper holidayRepository;
    private final WorkingDaysRepositoryWrapper workingDaysRepository;
    private final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository;
    private final AccountAssociationsRepository accountAssociationsRepository;
    private final DepositApplicationProcessWritePlatformService depositApplicationProcessWritePlatformService;
    private final SavingsAccountActionService savingsAccountActionService;

    @Autowired
    public DepositAccountWritePlatformServiceJpaRepositoryImpl(final PlatformSecurityContext context,
                                                               final SavingsAccountRepositoryWrapper savingAccountRepositoryWrapper,
                                                               final SavingsAccountTransactionRepository savingsAccountTransactionRepository,
                                                               final DepositAccountAssembler depositAccountAssembler,
                                                               final DepositAccountTransactionDataValidator depositAccountTransactionDataValidator,
                                                               final SavingsAccountChargeDataValidator savingsAccountChargeDataValidator,
                                                               final PaymentDetailWritePlatformService paymentDetailWritePlatformService,
                                                               final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper,
                                                               final JournalEntryWritePlatformService journalEntryWritePlatformService,
                                                               final DepositAccountDomainService depositAccountDomainService, final NoteRepository noteRepository,
                                                               final AccountTransfersReadPlatformService accountTransfersReadPlatformService, final ChargeRepositoryWrapper chargeRepository,
                                                               final SavingsAccountChargeRepositoryWrapper savingsAccountChargeRepository, final HolidayRepositoryWrapper holidayRepository,
                                                               final WorkingDaysRepositoryWrapper workingDaysRepository,
                                                               final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService,
                                                               final AccountTransfersWritePlatformService accountTransfersWritePlatformService,
                                                               final DepositAccountReadPlatformService depositAccountReadPlatformService,
                                                               final CalendarInstanceRepository calendarInstanceRepository,
                                                               final ConfigurationDomainService configurationDomainService,
                                                               final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository,
                                                               final AccountAssociationsRepository accountAssociationsRepository,
                                                               final DepositApplicationProcessWritePlatformService depositApplicationProcessWritePlatformService,
                                                               final SavingsAccountActionService savingsAccountActionService) {

        this.context = context;
        this.savingAccountRepositoryWrapper = savingAccountRepositoryWrapper;
        this.savingsAccountTransactionRepository = savingsAccountTransactionRepository;
        this.depositAccountAssembler = depositAccountAssembler;
        this.depositAccountTransactionDataValidator = depositAccountTransactionDataValidator;
        this.savingsAccountChargeDataValidator = savingsAccountChargeDataValidator;
        this.paymentDetailWritePlatformService = paymentDetailWritePlatformService;
        this.applicationCurrencyRepositoryWrapper = applicationCurrencyRepositoryWrapper;
        this.journalEntryWritePlatformService = journalEntryWritePlatformService;
        this.depositAccountDomainService = depositAccountDomainService;
        this.noteRepository = noteRepository;
        this.accountTransfersReadPlatformService = accountTransfersReadPlatformService;
        this.chargeRepository = chargeRepository;
        this.savingsAccountChargeRepository = savingsAccountChargeRepository;
        this.holidayRepository = holidayRepository;
        this.workingDaysRepository = workingDaysRepository;
        this.accountAssociationsReadPlatformService = accountAssociationsReadPlatformService;
        this.accountTransfersWritePlatformService = accountTransfersWritePlatformService;
        this.depositAccountReadPlatformService = depositAccountReadPlatformService;
        this.calendarInstanceRepository = calendarInstanceRepository;
        this.configurationDomainService = configurationDomainService;
        this.depositAccountOnHoldTransactionRepository = depositAccountOnHoldTransactionRepository;
        this.accountAssociationsRepository = accountAssociationsRepository;
        this.depositApplicationProcessWritePlatformService = depositApplicationProcessWritePlatformService;
        this.savingsAccountActionService = savingsAccountActionService;
    }

    @Transactional
    @Override
    public CommandProcessingResult activateFDAccount(final Long savingsId, final JsonCommand command) {

        this.depositAccountTransactionDataValidator.validateActivation(command);

        final FixedDepositAccount account = (FixedDepositAccount) this.depositAccountAssembler.assembleFrom(savingsId,
                DepositAccountType.FIXED_DEPOSIT);
        checkClientOrGroupActive(account);

        final Map<String, Object> changes = this.activateAccount(account, FixedDepositActivationReq.instance(command));

        return new CommandProcessingResultBuilder()
                .withEntityId(savingsId)
                .withOfficeId(account.officeId())
                .withClientId(account.clientId())
                .withGroupId(account.groupId())
                .withSavingsId(savingsId)
                .with(changes)
                .build();
    }

    private Map<String, Object> activateAccount(FixedDepositAccount account, FixedDepositActivationReq fixedDepositActivationReq) {
        AppUser user = this.context.authenticatedUser();
        boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();
        MathContext mc = MathContext.DECIMAL64;
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);

        final Map<String, Object> changes = account.activate(user, fixedDepositActivationReq);
        Money activationChargeAmount = getActivationCharge(account);
        if (!changes.isEmpty()) {
            final Locale locale = fixedDepositActivationReq.getLocale();
            final DateTimeFormatter fmt = fixedDepositActivationReq.getFormatter();
            Money amountForDeposit = account.activateWithBalance().plus(activationChargeAmount);
            if (amountForDeposit.isGreaterThanZero()) {
                AccountAssociations accountAssociation = this.accountAssociationsRepository.findBySavingsIdAndType(account.getId(),
                        AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue());

                if (accountAssociation == null) {
                    final PaymentDetail paymentDetail = null;
                    this.depositAccountDomainService.handleFDDeposit(account, fmt, account.getActivationLocalDate(),
                            amountForDeposit.getAmount(), paymentDetail);
                } else {
                    final SavingsAccount fromSavingsAccount = null;
                    boolean isRegularTransaction = false;
                    final boolean isExceptionForBalanceCheck = false;
                    final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(account.getActivationLocalDate(),
                            amountForDeposit.getAmount(), PortfolioAccountType.SAVINGS, PortfolioAccountType.SAVINGS,
                            accountAssociation.linkedSavingsAccount().getId(), account.getId(), "Account Transfer", locale, fmt, null, null, null, null,
                            null, AccountTransferType.ACCOUNT_TRANSFER.getValue(), null, null, null, null, account, fromSavingsAccount,
                            isRegularTransaction, isExceptionForBalanceCheck);
                    this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
                }
                final boolean isInterestTransfer = false;
                final LocalDate postInterestOnDate = null;
                if (activationChargeAmount.isGreaterThanZero()) {
                    payActivationCharge(account, user);
                }
                if (account.isBeforeLastAccrualPostingPeriod(account.getActivationLocalDate())) {
                    account.postAccrualInterest(mc, DateUtils.getLocalDateOfTenant(), isInterestTransfer,
                            isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, postInterestOnDate);
                }
                if (account.isBeforeLastPostingPeriod(account.getActivationLocalDate())) {
                    final LocalDate today = DateUtils.getLocalDateOfTenant();
                    account.postInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                            financialYearBeginningMonth, postInterestOnDate);
                } else {
                    final LocalDate today = DateUtils.getLocalDateOfTenant();
                    account.calculateInterestUsing(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                            financialYearBeginningMonth, postInterestOnDate);
                }

                updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
            }

            final boolean isPreMatureClosure = false;
            account.updateMaturityDateAndAmount(mc, isPreMatureClosure, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth);
            List<DepositAccountOnHoldTransaction> depositAccountOnHoldTransactions = null;
            if (account.getOnHoldFunds().compareTo(BigDecimal.ZERO) == 1) {
                depositAccountOnHoldTransactions = this.depositAccountOnHoldTransactionRepository
                        .findBySavingsAccountAndReversedFalseOrderByCreatedDateAsc(account);
            }
            account.validateAccountBalanceDoesNotBecomeNegative(SavingsAccountTransactionType.PAY_CHARGE.name(),
                    depositAccountOnHoldTransactions);
            this.savingAccountRepositoryWrapper.saveAndFlush(account);
        }
        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        return changes;
    }

    private Money getActivationCharge(final FixedDepositAccount account) {
        Money activationChargeAmount = Money.zero(account.getCurrency());
        for (SavingsAccountCharge savingsAccountCharge : account.charges()) {
            if (savingsAccountCharge.isSavingsActivation()) {
                activationChargeAmount = activationChargeAmount.plus(savingsAccountCharge.getAmount(account.getCurrency()));
            }
        }
        return activationChargeAmount;
    }

    private void payActivationCharge(final FixedDepositAccount account, AppUser user) {
        for (SavingsAccountCharge savingsAccountCharge : account.charges()) {
            if (savingsAccountCharge.isSavingsActivation()) {
                account.payCharge(savingsAccountCharge, savingsAccountCharge.getAmount(account.getCurrency()),
                        account.getActivationLocalDate(), user);
            }
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult activateRDAccount(final Long savingsId, final JsonCommand command) {
        boolean isRegularTransaction = false;

        final AppUser user = this.context.authenticatedUser();

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        this.depositAccountTransactionDataValidator.validateActivation(command);

        final RecurringDepositAccount account = (RecurringDepositAccount) this.depositAccountAssembler.assembleFrom(savingsId,
                DepositAccountType.RECURRING_DEPOSIT);
        checkClientOrGroupActive(account);

        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);

        final Map<String, Object> changes = account.activate(user, FixedDepositActivationReq.instance(command));

        if (!changes.isEmpty()) {
            final Locale locale = command.extractLocale();
            final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
            Money amountForDeposit = account.activateWithBalance();
            if (amountForDeposit.isGreaterThanZero()) {
                final PortfolioAccountData portfolioAccountData = this.accountAssociationsReadPlatformService
                        .retriveSavingsLinkedAssociation(savingsId);
                if (portfolioAccountData == null) {
                    this.depositAccountDomainService.handleRDDeposit(account, fmt, account.getActivationLocalDate(),
                            amountForDeposit.getAmount(), null, isRegularTransaction);
                } else {
                    final boolean isExceptionForBalanceCheck = false;
                    final SavingsAccount fromSavingsAccount = null;
                    final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(account.getActivationLocalDate(),
                            amountForDeposit.getAmount(), PortfolioAccountType.SAVINGS, PortfolioAccountType.SAVINGS,
                            portfolioAccountData.accountId(), account.getId(), "Account Transfer", locale, fmt, null, null, null, null,
                            null, AccountTransferType.ACCOUNT_TRANSFER.getValue(), null, null, null, null, account, fromSavingsAccount,
                            isRegularTransaction, isExceptionForBalanceCheck);
                    this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
                }
                updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
            }

            final MathContext mc = MathContext.DECIMAL64;

            // submitted and activation date are different then recalculate
            // maturity date and schedule
            if (!account.accountSubmittedAndActivationOnSameDate()) {
                final boolean isPreMatureClosure = false;
                final CalendarInstance calendarInstance = this.calendarInstanceRepository.findByEntityIdAndEntityTypeIdAndCalendarTypeId(
                        savingsId, CalendarEntityType.SAVINGS.getValue(), CalendarType.COLLECTION.getValue());

                final Calendar calendar = calendarInstance.getCalendar();
                final PeriodFrequencyType frequencyType = CalendarFrequencyType.from(CalendarUtils.getFrequency(calendar.getRecurrence()));
                Integer frequency = CalendarUtils.getInterval(calendar.getRecurrence());
                frequency = frequency == -1 ? 1 : frequency;
                account.generateSchedule(frequencyType, frequency, calendar);
                account.updateMaturityDateAndAmount(mc, isPreMatureClosure, isSavingsInterestPostingAtCurrentPeriodEnd,
                        financialYearBeginningMonth);
            }

            final LocalDate overdueUptoDate = DateUtils.getLocalDateOfTenant();
            account.updateOverduePayments(overdueUptoDate);
            final boolean isInterestTransfer = false;
            final LocalDate postInterestOnDate = null;
            if (account.isBeforeLastPostingPeriod(account.getActivationLocalDate())) {
                final LocalDate today = DateUtils.getLocalDateOfTenant();
                account.postInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth,
                        postInterestOnDate);
            } else {
                final LocalDate today = DateUtils.getLocalDateOfTenant();
                account.calculateInterestUsing(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                        financialYearBeginningMonth, postInterestOnDate);
            }
            List<DepositAccountOnHoldTransaction> depositAccountOnHoldTransactions = null;
            if (account.getOnHoldFunds().compareTo(BigDecimal.ZERO) == 1) {
                depositAccountOnHoldTransactions = this.depositAccountOnHoldTransactionRepository
                        .findBySavingsAccountAndReversedFalseOrderByCreatedDateAsc(account);
            }

            account.validateAccountBalanceDoesNotBecomeNegative(SavingsAccountTransactionType.PAY_CHARGE.name(),
                    depositAccountOnHoldTransactions);

            this.savingAccountRepositoryWrapper.saveAndFlush(account);
        }

        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .with(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult depositToFDAccount(final Long savingsId, @SuppressWarnings("unused") final JsonCommand command) {
        throw new DepositAccountTransactionNotAllowedException(savingsId, "deposit", DepositAccountType.FIXED_DEPOSIT);

    }

    @Transactional
    @Override
    public CommandProcessingResult updateDepositAmountForRDAccount(Long savingsId, JsonCommand command) {
        this.depositAccountTransactionDataValidator.validateDepositAmountUpdate(command);
        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final BigDecimal mandatoryRecommendedDepositAmount = command
                .bigDecimalValueOfParameterNamed(DepositsApiConstants.mandatoryRecommendedDepositAmountParamName);

        final LocalDate depositAmountUpdateEffectiveFromDate = command
                .localDateValueOfParameterNamed(DepositsApiConstants.effectiveDateParamName);

        final RecurringDepositAccount recurringDepositAccount = (RecurringDepositAccount) this.depositAccountAssembler
                .assembleFrom(savingsId, DepositAccountType.RECURRING_DEPOSIT);
        DepositAccountRecurringDetail recurringDetail = recurringDepositAccount.getRecurringDetail();
        Map<String, Object> changes = recurringDetail.updateMandatoryRecommendedDepositAmount(mandatoryRecommendedDepositAmount,
                depositAmountUpdateEffectiveFromDate, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth);

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsId) //
                .withOfficeId(recurringDepositAccount.officeId()) //
                .withClientId(recurringDepositAccount.clientId()) //
                .withGroupId(recurringDepositAccount.groupId()) //
                .withSavingsId(savingsId) //
                .with(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult depositToRDAccount(final Long savingsId, final JsonCommand command) {
        boolean isRegularTransaction = true;

        this.depositAccountTransactionDataValidator.validate(command, DepositAccountType.RECURRING_DEPOSIT);

        final RecurringDepositAccount account = (RecurringDepositAccount) this.depositAccountAssembler.assembleFrom(savingsId,
                DepositAccountType.RECURRING_DEPOSIT);
        checkClientOrGroupActive(account);

        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);

        final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");

        final Map<String, Object> changes = new LinkedHashMap<>();
        final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);
        final SavingsAccountTransaction deposit = this.depositAccountDomainService.handleRDDeposit(account, fmt, transactionDate,
                transactionAmount, paymentDetail, isRegularTransaction);

        return new CommandProcessingResultBuilder()
                .withEntityId(deposit.getId())
                .withOfficeId(account.officeId())
                .withClientId(account.clientId())
                .withGroupId(account.groupId())
                .withSavingsId(savingsId)
                .with(changes)
                .build();
    }

    private Long saveTransactionToGenerateTransactionId(final SavingsAccountTransaction transaction) {
        this.savingsAccountTransactionRepository.saveAndFlush(transaction);
        return transaction.getId();
    }

    @Transactional
    @Override
    public CommandProcessingResult withdrawal(final Long savingsId, final JsonCommand command,
            final DepositAccountType depositAccountType) {

        boolean isRegularTransaction = true;

        this.depositAccountTransactionDataValidator.validate(command, depositAccountType);

        final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");

        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);

        final Map<String, Object> changes = new LinkedHashMap<>();
        final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);

        final SavingsAccount account = this.depositAccountAssembler.assembleFrom(savingsId, depositAccountType);

        checkClientOrGroupActive(account);

        final SavingsAccountTransaction withdrawal = this.depositAccountDomainService.handleWithdrawal(account, fmt, transactionDate,
                transactionAmount, paymentDetail, true, isRegularTransaction);

        final String noteText = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(noteText)) {
            final Note note = Note.savingsTransactionNote(account, withdrawal, noteText);
            this.noteRepository.save(note) ;
        }

        return new CommandProcessingResultBuilder() //
                .withEntityId(withdrawal.getId()) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .with(changes)//
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult calculateInterest(final Long savingsId, final DepositAccountType depositAccountType) {

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final SavingsAccount account = this.depositAccountAssembler.assembleFrom(savingsId, depositAccountType);
        checkClientOrGroupActive(account);

        final LocalDate today = DateUtils.getLocalDateOfTenant();
        final MathContext mc = new MathContext(15, MoneyHelper.getRoundingMode());
        boolean isInterestTransfer = false;
        LocalDate postInterestOnDate = null;
        account.calculateInterestUsing(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                financialYearBeginningMonth, postInterestOnDate);

        this.savingAccountRepositoryWrapper.save(account);

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult postInterest(final Long savingsId, final DepositAccountType depositAccountType) {

        final SavingsAccount account = this.depositAccountAssembler.assembleFrom(savingsId, depositAccountType);
        checkClientOrGroupActive(account);
        postInterest(account);
        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult postAccrualInterest(JsonCommand command, final DepositAccountType depositAccountType) {

        final SavingsAccount account = this.depositAccountAssembler.assembleFrom(command.entityId(), depositAccountType);
        final boolean postInterestAs = command.booleanPrimitiveValueOfParameterNamed("isPostInterestAsOn");
        LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        checkClientOrGroupActive(account);
        if (postInterestAs) {

            if (transactionDate == null) {

                transactionDate = DateUtils.getLocalDateOfTenant();
            }
            if (transactionDate.isBefore(account.accountSubmittedOrActivationDate())) {
                throw new PostInterestAsOnDateException(PostInterestAsOnException_TYPE.ACTIVATION_DATE);
            }

            LocalDate today = DateUtils.getLocalDateOfTenant();
            if (transactionDate.isAfter(today)) { throw new PostInterestAsOnDateException(PostInterestAsOnException_TYPE.FUTURE_DATE); }

        }
        checkClientOrGroupActive(account);
        postAccrualInterest(account, transactionDate);
        return new CommandProcessingResultBuilder() //
                .withEntityId(command.entityId()) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(command.entityId()) //
                .build();
    }
    
    @Transactional
    @Override
    public CommandProcessingResult postAccrualInterest(Long fixedDepositAccountId, final DepositAccountType depositAccountType) {

        final SavingsAccount account = this.depositAccountAssembler.assembleFrom(fixedDepositAccountId, depositAccountType);
        checkClientOrGroupActive(account);
        postAccrualInterest(account, DateUtils.getLocalDateOfTenant());
        return new CommandProcessingResultBuilder()
                .withEntityId(fixedDepositAccountId)
                .withOfficeId(account.officeId())
                .withClientId(account.clientId())
                .withGroupId(account.groupId())
                .withSavingsId(fixedDepositAccountId)
                .build();
    }

    @Override
    @Transactional
    public CommandProcessingResult partiallyLiquidateAccount(Long accountId, JsonCommand command) {

        FixedDepositAccount account = (FixedDepositAccount) this.depositAccountAssembler.assembleFrom(accountId, DepositAccountType.FIXED_DEPOSIT);
        this.depositAccountTransactionDataValidator.validatePartialLiquidation(account, command);
        AccountAssociations accountAssociations = this.getLinkedSavingsAccount(accountId);

        this.checkClientOrGroupActive(account);
        this.createPartialLiquidationCharge(account);
        this.preCloseAccount(command, new LinkedHashMap<>(), account, accountAssociations);
        FixedDepositApplicationReq fixedDepositApplicationReq = this.generateFixedDepositApplicationReq(account, command);
        this.setDepositAmountForPartialLiquidation(fixedDepositApplicationReq, account, command);
        this.autoCreateNewFD(command, account, accountAssociations, fixedDepositApplicationReq);

        return new CommandProcessingResultBuilder()
                .withEntityId(accountId)
                .withOfficeId(account.officeId())
                .withClientId(account.clientId())
                .withGroupId(account.groupId())
                .withSavingsId(accountAssociations.linkedSavingsAccount().getId())
                .build();
    }

    private void createPartialLiquidationCharge(FixedDepositAccount account) {
        Charge charge = this.chargeRepository.findChargeByChargeTimeType(ChargeTimeType.FDA_PARTIAL_LIQUIDATION_FEE);
        if (charge != null) {
            List<SavingsAccountCharge> preclosureCharges = this.savingsAccountChargeRepository.findFdaPreclosureCharges(account.getId(),
                    Collections.singletonList(ChargeTimeType.FDA_PARTIAL_LIQUIDATION_FEE.getValue()));
            if (preclosureCharges.isEmpty()) {
                SavingsAccountChargeReq savingsAccountChargeReq = new SavingsAccountChargeReq();
                savingsAccountChargeReq.setAmount(charge.getAmount());
                SavingsAccountCharge savingsAccountCharge = SavingsAccountCharge.createNew(account, charge, savingsAccountChargeReq);
                account.addCharge(DateUtils.getDefaultFormatter(), savingsAccountCharge, charge);
                this.savingsAccountChargeRepository.save(savingsAccountCharge);
                this.savingAccountRepositoryWrapper.saveAndFlush(account);
            }
        }
    }

    private void autoCreateNewFD(JsonCommand command, FixedDepositAccount account, AccountAssociations accountAssociations, FixedDepositApplicationReq fixedDepositApplicationReq) {
        FixedDepositAccount newAccount = this.createNewAccount(fixedDepositApplicationReq, account, accountAssociations);
        this.autoApproveAccount(command, account, newAccount);
        this.autoActivateAccount(command, newAccount);
    }

    @Override
    public CommandProcessingResult topUpAccount(Long accountId, JsonCommand command) {

        this.depositAccountTransactionDataValidator.validateTopUp(command);
        FixedDepositAccount account = (FixedDepositAccount) this.depositAccountAssembler.assembleFrom(accountId, DepositAccountType.FIXED_DEPOSIT);
        AccountAssociations accountAssociations = this.getLinkedSavingsAccount(accountId);

        this.checkClientOrGroupActive(account);
        account.setApplyPreclosureCharges(false);
        this.preCloseAccount(command, new LinkedHashMap<>(), account, accountAssociations);
        FixedDepositApplicationReq fixedDepositApplicationReq = this.generateFixedDepositApplicationReq(account, command);
        fixedDepositApplicationReq.setDepositAmount(command.bigDecimalValueOfParameterNamed(depositAmountParamName));
        this.autoCreateNewFD(command, account, accountAssociations, fixedDepositApplicationReq);

        return new CommandProcessingResultBuilder()
                .withEntityId(accountId)
                .withOfficeId(account.officeId())
                .withClientId(account.clientId())
                .withGroupId(account.groupId())
                .withSavingsId(accountAssociations.linkedSavingsAccount().getId())
                .build();
    }

    private void autoApproveAccount(JsonCommand command, FixedDepositAccount account, FixedDepositAccount newAccount) {
        FixedDepositApprovalReq fixedDepositApprovalReq = this.generateFixedDepositApprovalReq(account, command);
        this.savingsAccountActionService.approveAccount(fixedDepositApprovalReq, newAccount);
    }

    private void autoActivateAccount(JsonCommand command, FixedDepositAccount account) {
        FixedDepositActivationReq fixedDepositActivationReq = this.generateFixedDepositActivationReq(command);
        this.activateAccount(account, fixedDepositActivationReq);
    }

    private FixedDepositActivationReq generateFixedDepositActivationReq(JsonCommand command) {
        FixedDepositActivationReq fixedDepositActivationReq = new FixedDepositActivationReq();
        fixedDepositActivationReq.setLocale(command.extractLocale());
        fixedDepositActivationReq.setDateFormat(command.dateFormat());
        fixedDepositActivationReq.setFormatter(DateTimeFormat.forPattern(fixedDepositActivationReq.getDateFormat()).withLocale(command.extractLocale()));
        fixedDepositActivationReq.setActivationDate(command.localDateValueOfParameterNamed(submittedOnDateParamName));

        return fixedDepositActivationReq;
    }

    private FixedDepositAccount createNewAccount(FixedDepositApplicationReq fixedDepositApplicationReq, FixedDepositAccount account, AccountAssociations accountAssociations) {
        fixedDepositApplicationReq.setSavingsAccountId(accountAssociations.linkedSavingsAccount().getId());
        Set<SavingsAccountCharge> charges = this.generateCharges(account);
        return this.depositApplicationProcessWritePlatformService.createFixedDepositAccount(fixedDepositApplicationReq, account.savingsProduct(), charges);
    }

    private void preCloseAccount(JsonCommand command, Map<String, Object> changes, FixedDepositAccount account, AccountAssociations accountAssociations) {
        FixedDepositPreClosureReq fixedDepositPreClosureReq = this.generateFixedDepositPreclosureRequest(accountAssociations, command);
        this.depositAccountDomainService.prematurelyCloseFDAccount(account, null, fixedDepositPreClosureReq, changes);
        this.saveNote(command, changes, account);
    }

    private AccountAssociations getLinkedSavingsAccount(Long accountId) {
        AccountAssociations accountAssociations = this.accountAssociationsRepository.findBySavingsIdAndType(accountId,
                AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue());
        if (accountAssociations == null) {
            throw new SavingsAccountNotFoundException("error.msg.saving.account.cannot.perform.this.action.without.linked.savings.account",
                    "Cannot perform this action on Fixed Deposit Account with no associated Savings Account");
        }
        return accountAssociations;
    }

    private Set<SavingsAccountCharge> generateCharges(FixedDepositAccount account) {
        final Set<SavingsAccountCharge> charges = new HashSet<>();
        account.getCharges().stream().filter(charge -> !charge.getCharge().isPartialLiquidationCharge()).forEach(charge -> charges.add(charge.copy()));
        return charges;
    }

    private FixedDepositApprovalReq generateFixedDepositApprovalReq(FixedDepositAccount account, JsonCommand command) {
        FixedDepositApprovalReq fixedDepositApprovalReq = new FixedDepositApprovalReq();
        fixedDepositApprovalReq.setLocale(command.extractLocale());
        fixedDepositApprovalReq.setDateFormat(command.dateFormat());
        fixedDepositApprovalReq.setFormatter(DateTimeFormat.forPattern(fixedDepositApprovalReq.getDateFormat()).withLocale(command.extractLocale()));
        fixedDepositApprovalReq.setApprovedOnDate(command.localDateValueOfParameterNamed(submittedOnDateParamName));
        fixedDepositApprovalReq.setApprovedOnDateChange(command.stringValueOfParameterNamed(submittedOnDateParamName));
        fixedDepositApprovalReq.setNote("Auto approved during partial liquidation or top up of " + account.getAccountNumber());

        return fixedDepositApprovalReq;
    }

    private FixedDepositApplicationReq generateFixedDepositApplicationReq(FixedDepositAccount account, JsonCommand command) {
        FixedDepositApplicationReq fixedDepositApplicationReq = new FixedDepositApplicationReq();

        fixedDepositApplicationReq.setLocale(command.extractLocale());
        fixedDepositApplicationReq.setDateFormat(command.dateFormat());
        fixedDepositApplicationReq.setSubmittedOnDate(command.localDateValueOfParameterNamed(submittedOnDateParamName));
        fixedDepositApplicationReq.setClientId(account.getClient().getId());
        Staff savingsOfficer = account.getSavingsOfficer();
        if (savingsOfficer != null) {
            fixedDepositApplicationReq.setFieldOfficerId(savingsOfficer.getId());
        }
        fixedDepositApplicationReq.setCalendarInherited(false);
        fixedDepositApplicationReq.setInterestPeriodTypeValue(account.getInterestCompoundingPeriodType());
        fixedDepositApplicationReq.setInterestPostingPeriodTypeValue(account.getInterestPostingPeriodType());
        fixedDepositApplicationReq.setInterestCalculationTypeValue(account.getInterestCalculationType());
        fixedDepositApplicationReq.setInterestCalculationDaysInYearTypeValue(account.getInterestCalculationDaysInYearType());
        fixedDepositApplicationReq.setLockinPeriodFrequencySet(true);
        fixedDepositApplicationReq.setLockinPeriodFrequency(account.getLockinPeriodFrequency());
        fixedDepositApplicationReq.setLockinPeriodFrequencyTypeValueSet(true);
        fixedDepositApplicationReq.setLockinPeriodFrequencyTypeValue(account.getLockinPeriodFrequencyType());
        fixedDepositApplicationReq.setWithdrawalFeeApplicableForTransfer(account.isWithdrawalFeeApplicableForTransfer());
        fixedDepositApplicationReq.setWithHoldTaxSet(true);
        fixedDepositApplicationReq.setWithHoldTax(account.isWithHoldTax());
        fixedDepositApplicationReq.setDepositPeriod(command.integerValueOfParameterNamed(depositPeriodParamName));
        fixedDepositApplicationReq.setDepositPeriodFrequency(SavingsPeriodFrequencyType.fromInt(command.integerValueOfParameterNamed(depositPeriodFrequencyIdParamName)));
        fixedDepositApplicationReq.setTransferInterest(account.getAccountTermAndPreClosure().isTransferInterestToLinkedAccount());
        fixedDepositApplicationReq.setFixedDepositApplicationTermsReq(new FixedDepositApplicationTermsReq());
        fixedDepositApplicationReq.setFixedDepositApplicationPreClosureReq(this.generateFixedDepositApplicationPreClosureReq(account));

        return fixedDepositApplicationReq;
    }

    private FixedDepositApplicationPreClosureReq generateFixedDepositApplicationPreClosureReq(FixedDepositAccount account) {
        FixedDepositApplicationPreClosureReq fixedDepositApplicationPreClosureReq = new FixedDepositApplicationPreClosureReq();
        fixedDepositApplicationPreClosureReq.setPreClosurePenalInterest(account.getAccountTermAndPreClosure().getPreClosureDetail().preClosurePenalInterest());
        fixedDepositApplicationPreClosureReq.setPreClosurePenalApplicable(account.getAccountTermAndPreClosure().isPreClosurePenalApplicable());
        fixedDepositApplicationPreClosureReq.setPreClosurePenalInterestOnTypeId(account.getAccountTermAndPreClosure().getPreClosureDetail().preClosurePenalInterestOnTypeId());
        fixedDepositApplicationPreClosureReq.setPreClosurePenalInterestOnTypeIdPramSet(true);
        fixedDepositApplicationPreClosureReq.setPreClosurePenalInterestParamSet(true);
        fixedDepositApplicationPreClosureReq.setPreClosurePenalApplicableParamSet(true);
        return fixedDepositApplicationPreClosureReq;
    }

    private void setDepositAmountForPartialLiquidation(FixedDepositApplicationReq fixedDepositApplicationReq,
                                                       FixedDepositAccount account, JsonCommand command) {
        BigDecimal liquidationAmount = command.bigDecimalValueOfParameterNamed(liquidationAmountParamName);
        fixedDepositApplicationReq.setDepositAmount(account.getAccountTermAndPreClosure().maturityAmount().subtract(liquidationAmount));
    }

    private FixedDepositPreClosureReq generateFixedDepositPreclosureRequest(AccountAssociations accountAssociations, JsonCommand command) {
        FixedDepositPreClosureReq fixedDepositPreclosureReq = new FixedDepositPreClosureReq();

        fixedDepositPreclosureReq.setLocale(command.extractLocale());
        fixedDepositPreclosureReq.setDateFormat(command.dateFormat());
        fixedDepositPreclosureReq.setFormatter(DateTimeFormat.forPattern(fixedDepositPreclosureReq.getDateFormat()).withLocale(fixedDepositPreclosureReq.getLocale()));
        fixedDepositPreclosureReq.setClosedDate(command.localDateValueOfParameterNamed(submittedOnDateParamName));
        fixedDepositPreclosureReq.setClosureType(DepositAccountOnClosureType.TRANSFER_TO_SAVINGS);
        fixedDepositPreclosureReq.setToSavingsId(accountAssociations.linkedSavingsAccount().getId());
        fixedDepositPreclosureReq.setTransferDescription("Partial Liquidation");
        return fixedDepositPreclosureReq;
    }

    private void saveNote(JsonCommand command, Map<String, Object> changes, FixedDepositAccount account) {
        final String noteText = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(noteText)) {
            final Note note = Note.savingNote(account, noteText);
            changes.put("note", noteText);
            this.noteRepository.save(note);
        }
    }


    @Transactional
    private void postInterest(final SavingsAccount account) {

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
        final LocalDate today = DateUtils.getLocalDateOfTenant();
        final MathContext mc = new MathContext(10, MoneyHelper.getRoundingMode());
        boolean isInterestTransfer = false;
        LocalDate postInterestOnDate = null;
        account.postAccrualInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth,
                postInterestOnDate);
        account.postInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth,
                postInterestOnDate);
        this.savingAccountRepositoryWrapper.saveAndFlush(account);

        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
    }

    @Transactional
    private void postAccrualInterest(final SavingsAccount account, final LocalDate transactionDate) {

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
        final MathContext mc = new MathContext(10, MoneyHelper.getRoundingMode());
        boolean isInterestTransfer = false;
        account.postAccrualInterest(mc, transactionDate, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth,
                transactionDate);
        this.savingAccountRepositoryWrapper.saveAndFlush(account);

        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
    }

    @Override
    @CronTarget(jobName = JobName.TRANSFER_INTEREST_TO_SAVINGS)
    public void transferInterestToSavings() throws JobExecutionException {
        Collection<AccountTransferDTO> accountTrasferData = this.depositAccountReadPlatformService.retrieveDataForInterestTransfer();
        StringBuilder sb = new StringBuilder(200);
        for (AccountTransferDTO accountTransferDTO : accountTrasferData) {
            try {
                this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
            } catch (final PlatformApiDataValidationException e) {
                sb.append("Validation exception while trasfering Interest form ").append(accountTransferDTO.getFromAccountId())
                        .append(" to ").append(accountTransferDTO.getToAccountId()).append("--------");
            } catch (final InsufficientAccountBalanceException e) {
                sb.append("InsufficientAccountBalance Exception while trasfering Interest form ")
                        .append(accountTransferDTO.getFromAccountId()).append(" to ").append(accountTransferDTO.getToAccountId())
                        .append("--------");
            }
        }
        if (sb.length() > 0) { throw new JobExecutionException(sb.toString()); }
    }

    @Override
    public CommandProcessingResult undoFDTransaction(final Long savingsId, @SuppressWarnings("unused") final Long transactionId,
            @SuppressWarnings("unused") final boolean allowAccountTransferModification) {

        throw new DepositAccountTransactionNotAllowedException(savingsId, "undo", DepositAccountType.FIXED_DEPOSIT);
    }

    @Override
    public CommandProcessingResult undoRDTransaction(final Long savingsId, final Long transactionId,
            final boolean allowAccountTransferModification) {

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final RecurringDepositAccount account = (RecurringDepositAccount) this.depositAccountAssembler.assembleFrom(savingsId,
                DepositAccountType.RECURRING_DEPOSIT);
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);

        final SavingsAccountTransaction savingsAccountTransaction = this.savingsAccountTransactionRepository
                .findOneByIdAndSavingsAccountId(transactionId, savingsId);
        if (savingsAccountTransaction == null) { throw new SavingsAccountTransactionNotFoundException(savingsId, transactionId); }

        if (!allowAccountTransferModification
                && this.accountTransfersReadPlatformService.isAccountTransfer(transactionId, PortfolioAccountType.SAVINGS)) {
            throw new PlatformServiceUnavailableException("error.msg.recurring.deposit.account.transfer.transaction.update.not.allowed",
                    "Recurring deposit account transaction:" + transactionId + " update not allowed as it involves in account transfer",
                    transactionId);
        }

        final LocalDate today = DateUtils.getLocalDateOfTenant();
        final MathContext mc = MathContext.DECIMAL64;

        if (account.isNotActive()) {
            throwValidationForActiveStatus(SavingsApiConstants.undoTransactionAction);
        }
        account.undoTransaction(transactionId);
        boolean isInterestTransfer = false;
        LocalDate postInterestOnDate = null;
        checkClientOrGroupActive(account);
        if (savingsAccountTransaction.isPostInterestCalculationRequired()
                && account.isBeforeLastPostingPeriod(savingsAccountTransaction.transactionLocalDate())) {
            account.postInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth,
                    postInterestOnDate);
        } else {
            account.calculateInterestUsing(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth, postInterestOnDate);
        }
        List<DepositAccountOnHoldTransaction> depositAccountOnHoldTransactions = null;
        if (account.getOnHoldFunds().compareTo(BigDecimal.ZERO) == 1) {
            depositAccountOnHoldTransactions = this.depositAccountOnHoldTransactionRepository
                    .findBySavingsAccountAndReversedFalseOrderByCreatedDateAsc(account);
        }

        account.validateAccountBalanceDoesNotBecomeNegative(SavingsApiConstants.undoTransactionAction, depositAccountOnHoldTransactions);
        // account.activateAccountBasedOnBalance();
        final boolean isPreMatureClosure = false;
        account.updateMaturityDateAndAmount(mc, isPreMatureClosure, isSavingsInterestPostingAtCurrentPeriodEnd,
                financialYearBeginningMonth);

        final LocalDate overdueUptoDate = DateUtils.getLocalDateOfTenant();

        if (savingsAccountTransaction.isDeposit()) {
            account.updateScheduleInstallments();
        }

        account.updateOverduePayments(overdueUptoDate);

        this.savingAccountRepositoryWrapper.saveAndFlush(account);
        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .build();
    }

    @Override
    public CommandProcessingResult adjustFDTransaction(final Long savingsId, @SuppressWarnings("unused") final Long transactionId,
            @SuppressWarnings("unused") final JsonCommand command) {

        throw new DepositAccountTransactionNotAllowedException(savingsId, "modify", DepositAccountType.FIXED_DEPOSIT);
    }

    @Override
    public CommandProcessingResult adjustRDTransaction(final Long savingsId, final Long transactionId, final JsonCommand command) {

        AppUser user = getAppUserIfPresent();

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        this.depositAccountTransactionDataValidator.validate(command, DepositAccountType.RECURRING_DEPOSIT);

        final SavingsAccountTransaction savingsAccountTransaction = this.savingsAccountTransactionRepository
                .findOneByIdAndSavingsAccountId(transactionId, savingsId);
        if (savingsAccountTransaction == null) { throw new SavingsAccountTransactionNotFoundException(savingsId, transactionId); }

        if (!(savingsAccountTransaction.isDeposit() || savingsAccountTransaction.isWithdrawal())
                || savingsAccountTransaction.isReversed()) {
            throw new TransactionUpdateNotAllowedException(savingsId, transactionId);
        }

        if (this.accountTransfersReadPlatformService.isAccountTransfer(transactionId, PortfolioAccountType.SAVINGS)) {
            throw new PlatformServiceUnavailableException("error.msg.saving.account.transfer.transaction.update.not.allowed",
                    "Deposit account transaction:" + transactionId + " update not allowed as it involves in account transfer",
                    transactionId);
        }

        final LocalDate today = DateUtils.getLocalDateOfTenant();

        final RecurringDepositAccount account = (RecurringDepositAccount) this.depositAccountAssembler.assembleFrom(savingsId,
                DepositAccountType.RECURRING_DEPOSIT);
        if (account.isNotActive()) {
            throwValidationForActiveStatus(SavingsApiConstants.adjustTransactionAction);
        }
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);

        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
        final LocalDate transactionDate = command.localDateValueOfParameterNamed(SavingsApiConstants.transactionDateParamName);
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed(SavingsApiConstants.transactionAmountParamName);
        final Map<String, Object> changes = new LinkedHashMap<>();
        final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);

        final MathContext mc = new MathContext(10, MoneyHelper.getRoundingMode());
        account.undoTransaction(transactionId);

        SavingsAccountTransaction transaction = null;
        Integer accountType = null;
        if (savingsAccountTransaction.isDeposit()) {
            final SavingsAccountTransactionDTO transactionDTO = new SavingsAccountTransactionDTO(fmt, transactionDate, transactionAmount,
                    paymentDetail, savingsAccountTransaction.createdDate(), user, accountType, false);
            transaction = account.deposit(transactionDTO);
        } else {
            final SavingsAccountTransactionDTO transactionDTO = new SavingsAccountTransactionDTO(fmt, transactionDate, transactionAmount,
                    paymentDetail, savingsAccountTransaction.createdDate(), user, accountType, false);
            transaction = account.withdraw(transactionDTO, true);
        }
        final Long newtransactionId = saveTransactionToGenerateTransactionId(transaction);
        boolean isInterestTransfer = false;
        final LocalDate postInterestOnDate = null;
        if (account.isBeforeLastPostingPeriod(transactionDate)
                || account.isBeforeLastPostingPeriod(savingsAccountTransaction.transactionLocalDate())) {
            account.postInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth,
                    postInterestOnDate);
        } else {
            account.calculateInterestUsing(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth, postInterestOnDate);
        }
        List<DepositAccountOnHoldTransaction> depositAccountOnHoldTransactions = null;
        if (account.getOnHoldFunds().compareTo(BigDecimal.ZERO) == 1) {
            depositAccountOnHoldTransactions = this.depositAccountOnHoldTransactionRepository
                    .findBySavingsAccountAndReversedFalseOrderByCreatedDateAsc(account);
        }

        account.validateAccountBalanceDoesNotBecomeNegative(SavingsApiConstants.adjustTransactionAction, depositAccountOnHoldTransactions);
        account.activateAccountBasedOnBalance();

        if (savingsAccountTransaction.isDeposit()) {
            account.handleScheduleInstallments(savingsAccountTransaction);
        }
        final LocalDate overdueUptoDate = DateUtils.getLocalDateOfTenant();
        account.updateOverduePayments(overdueUptoDate);

        this.savingAccountRepositoryWrapper.saveAndFlush(account);
        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        return new CommandProcessingResultBuilder() //
                .withEntityId(newtransactionId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .with(changes)//
                .build();
    }

    /**
     *
     */
    private void throwValidationForActiveStatus(final String actionName) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SAVINGS_ACCOUNT_RESOURCE_NAME + actionName);
        baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode("account.is.not.active");
        throw new PlatformApiDataValidationException(dataValidationErrors);
    }

    private void checkClientOrGroupActive(final SavingsAccount account) {
        final Client client = account.getClient();
        if (client != null) {
            if (client.isNotActive()) { throw new ClientNotActiveException(client.getId()); }
        }
        final Group group = account.group();
        if (group != null) {
            if (group.isNotActive()) { throw new GroupNotActiveException(group.getId()); }
        }
    }

    @Override
    public CommandProcessingResult closeFDAccount(final Long savingsId, final JsonCommand command) {
        final AppUser user = this.context.authenticatedUser();
        final boolean isPreMatureClose = false;
        this.depositAccountTransactionDataValidator.validateClosing(command, DepositAccountType.FIXED_DEPOSIT, isPreMatureClose);

        final Map<String, Object> changes = new LinkedHashMap<>();
        final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);

        final FixedDepositAccount account = (FixedDepositAccount) this.depositAccountAssembler.assembleFrom(savingsId,
                DepositAccountType.FIXED_DEPOSIT);
        checkClientOrGroupActive(account);

        this.depositAccountDomainService.handleFDAccountClosure(account, paymentDetail, user, command, DateUtils.getLocalDateOfTenant(),
                changes);

        saveNote(command, changes, account);

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .with(changes)//
                .build();

    }

    @Override
    public CommandProcessingResult closeRDAccount(final Long savingsId, final JsonCommand command) {
        final AppUser user = this.context.authenticatedUser();

        this.depositAccountTransactionDataValidator.validateClosing(command, DepositAccountType.RECURRING_DEPOSIT, false);

        final Map<String, Object> changes = new LinkedHashMap<>();
        final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);

        final RecurringDepositAccount account = (RecurringDepositAccount) this.depositAccountAssembler.assembleFrom(savingsId,
                DepositAccountType.RECURRING_DEPOSIT);
        checkClientOrGroupActive(account);

        this.depositAccountDomainService.handleRDAccountClosure(account, paymentDetail, user, command, DateUtils.getLocalDateOfTenant(),
                changes);

        final String noteText = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(noteText)) {
            final Note note = Note.savingNote(account, noteText);
            changes.put("note", noteText);
            this.noteRepository.save(note);
        }

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .with(changes)//
                .build();

    }

    @Override
    public CommandProcessingResult prematureCloseFDAccount(final Long savingsId, final JsonCommand command) {

        this.depositAccountTransactionDataValidator.validateClosing(command, DepositAccountType.FIXED_DEPOSIT, true);

        Map<String, Object> changes = new LinkedHashMap<>();
        PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);
        FixedDepositAccount account = (FixedDepositAccount) this.depositAccountAssembler.assembleFrom(savingsId,
                DepositAccountType.FIXED_DEPOSIT);
        checkClientOrGroupActive(account);

        this.depositAccountDomainService.prematurelyCloseFDAccount(account, paymentDetail, FixedDepositPreClosureReq.instance(command), changes);

        this.saveNote(command, changes, account);

        return new CommandProcessingResultBuilder()
                .withEntityId(savingsId)
                .withOfficeId(account.officeId())
                .withClientId(account.clientId())
                .withGroupId(account.groupId())
                .withSavingsId(savingsId)
                .with(changes)
                .build();
    }

    @Override
    public CommandProcessingResult prematureCloseRDAccount(final Long savingsId, final JsonCommand command) {
        final AppUser user = this.context.authenticatedUser();

        this.depositAccountTransactionDataValidator.validateClosing(command, DepositAccountType.RECURRING_DEPOSIT, true);

        final Map<String, Object> changes = new LinkedHashMap<>();
        final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);

        final RecurringDepositAccount account = (RecurringDepositAccount) this.depositAccountAssembler.assembleFrom(savingsId,
                DepositAccountType.RECURRING_DEPOSIT);
        checkClientOrGroupActive(account);
        if (account.maturityDate() == null) {
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                    .resource(RECURRING_DEPOSIT_ACCOUNT_RESOURCE_NAME + DepositsApiConstants.preMatureCloseAction);
            baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode("can.not.close.as.premature");
            if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
        }

        this.depositAccountDomainService.handleRDAccountPreMatureClosure(account, paymentDetail, user, command,
                DateUtils.getLocalDateOfTenant(), changes);

        final String noteText = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(noteText)) {
            final Note note = Note.savingNote(account, noteText);
            changes.put("note", noteText);
            this.noteRepository.save(note);
        }

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .with(changes)//
                .build();

    }

    @Override
    public SavingsAccountTransaction initiateSavingsTransfer(final Long accountId, final LocalDate transferDate,
            final DepositAccountType depositAccountType) {

        AppUser user = getAppUserIfPresent();
        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final SavingsAccount savingsAccount = this.depositAccountAssembler.assembleFrom(accountId, depositAccountType);
        final LocalDate postInterestOnDate = null;
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(savingsAccount, existingTransactionIds, existingReversedTransactionIds);

        final SavingsAccountTransaction newTransferTransaction = SavingsAccountTransaction.initiateTransfer(savingsAccount,
                savingsAccount.office(), transferDate, user, false);
        savingsAccount.addTransaction(newTransferTransaction);
        savingsAccount.setStatus(SavingsAccountStatusType.TRANSFER_IN_PROGRESS.getValue());
        final MathContext mc = MathContext.DECIMAL64;
        boolean isInterestTransfer = false;
        savingsAccount.calculateInterestUsing(mc, transferDate, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                financialYearBeginningMonth, postInterestOnDate);

        this.savingsAccountTransactionRepository.save(newTransferTransaction);
        this.savingAccountRepositoryWrapper.saveAndFlush(savingsAccount);

        postJournalEntries(savingsAccount, existingTransactionIds, existingReversedTransactionIds);

        return newTransferTransaction;
    }

    @Override
    public SavingsAccountTransaction withdrawSavingsTransfer(final Long accountId, final LocalDate transferDate,
            final DepositAccountType depositAccountType) {

        AppUser user = getAppUserIfPresent();

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final SavingsAccount savingsAccount = this.depositAccountAssembler.assembleFrom(accountId, depositAccountType);

        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(savingsAccount, existingTransactionIds, existingReversedTransactionIds);

        final SavingsAccountTransaction withdrawtransferTransaction = SavingsAccountTransaction.withdrawTransfer(savingsAccount,
                savingsAccount.office(), transferDate, user, false);
        savingsAccount.addTransaction(withdrawtransferTransaction);
        savingsAccount.setStatus(SavingsAccountStatusType.ACTIVE.getValue());
        final MathContext mc = MathContext.DECIMAL64;
        boolean isInterestTransfer = false;
        LocalDate postInterestOnDate = null;
        savingsAccount.calculateInterestUsing(mc, transferDate, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                financialYearBeginningMonth, postInterestOnDate);

        this.savingsAccountTransactionRepository.save(withdrawtransferTransaction);
        this.savingAccountRepositoryWrapper.saveAndFlush(savingsAccount);

        postJournalEntries(savingsAccount, existingTransactionIds, existingReversedTransactionIds);

        return withdrawtransferTransaction;
    }

    @Override
    public void rejectSavingsTransfer(final Long accountId, final DepositAccountType depositAccountType) {
        final SavingsAccount savingsAccount = this.depositAccountAssembler.assembleFrom(accountId, depositAccountType);
        savingsAccount.setStatus(SavingsAccountStatusType.TRANSFER_ON_HOLD.getValue());
        this.savingAccountRepositoryWrapper.save(savingsAccount);
    }

    @Override
    public SavingsAccountTransaction acceptSavingsTransfer(final Long accountId, final LocalDate transferDate,
            final Office acceptedInOffice, final Staff fieldOfficer, final DepositAccountType depositAccountType) {

        AppUser user = getAppUserIfPresent();

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final SavingsAccount savingsAccount = this.depositAccountAssembler.assembleFrom(accountId, depositAccountType);

        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(savingsAccount, existingTransactionIds, existingReversedTransactionIds);

        final SavingsAccountTransaction acceptTransferTransaction = SavingsAccountTransaction.approveTransfer(savingsAccount,
                acceptedInOffice, transferDate, user, false);
        savingsAccount.addTransaction(acceptTransferTransaction);
        savingsAccount.setStatus(SavingsAccountStatusType.ACTIVE.getValue());
        if (fieldOfficer != null) {
            savingsAccount.reassignSavingsOfficer(fieldOfficer, transferDate);
        }
        boolean isInterestTransfer = false;
        LocalDate postInterestOnDate = null;
        final MathContext mc = MathContext.DECIMAL64;
        savingsAccount.calculateInterestUsing(mc, transferDate, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                financialYearBeginningMonth, postInterestOnDate);

        this.savingsAccountTransactionRepository.save(acceptTransferTransaction);
        this.savingAccountRepositoryWrapper.saveAndFlush(savingsAccount);

        postJournalEntries(savingsAccount, existingTransactionIds, existingReversedTransactionIds);

        return acceptTransferTransaction;
    }

    @Transactional
    @Override
    public CommandProcessingResult addSavingsAccountCharge(final JsonCommand command, final DepositAccountType depositAccountType) {

        this.context.authenticatedUser();
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SAVINGS_ACCOUNT_RESOURCE_NAME);

        final Long savingsAccountId = command.getSavingsId();
        this.savingsAccountChargeDataValidator.validateAdd(command.json());

        final SavingsAccount savingsAccount = this.depositAccountAssembler.assembleFrom(savingsAccountId, depositAccountType);
        checkClientOrGroupActive(savingsAccount);

        final Locale locale = command.extractLocale();
        final String format = command.dateFormat();
        final DateTimeFormatter fmt = StringUtils.isNotBlank(format) ? DateTimeFormat.forPattern(format).withLocale(locale)
                : DateUtils.getDefaultFormatter();

        final Long chargeDefinitionId = command.longValueOfParameterNamed(chargeIdParamName);
        final Charge chargeDefinition = this.chargeRepository.findOneWithNotFoundDetection(chargeDefinitionId);

        final SavingsAccountCharge savingsAccountCharge = SavingsAccountCharge.createNew(savingsAccount, chargeDefinition, SavingsAccountChargeReq.instance(command));

        if (savingsAccountCharge.getDueLocalDate() != null) {
            // transaction date should not be on a holiday or non working day
            if (!this.configurationDomainService.allowTransactionsOnHolidayEnabled()
                    && this.holidayRepository.isHoliday(savingsAccount.officeId(), savingsAccountCharge.getDueLocalDate())) {
                baseDataValidator.reset().parameter(dueAsOfDateParamName).value(savingsAccountCharge.getDueLocalDate().toString(fmt))
                        .failWithCodeNoParameterAddedToErrorCode("charge.due.date.is.on.holiday");
                if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
            }

            if (!this.configurationDomainService.allowTransactionsOnNonWorkingDayEnabled()
                    && !this.workingDaysRepository.isWorkingDay(savingsAccountCharge.getDueLocalDate())) {
                baseDataValidator.reset().parameter(dueAsOfDateParamName).value(savingsAccountCharge.getDueLocalDate().toString(fmt))
                        .failWithCodeNoParameterAddedToErrorCode("charge.due.date.is.a.nonworking.day");
                if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
            }
        }

        savingsAccount.addCharge(fmt, savingsAccountCharge, chargeDefinition);

        this.savingAccountRepositoryWrapper.saveAndFlush(savingsAccount);

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsAccountCharge.getId()) //
                .withOfficeId(savingsAccount.officeId()) //
                .withClientId(savingsAccount.clientId()) //
                .withGroupId(savingsAccount.groupId()) //
                .withSavingsId(savingsAccountId) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult updateSavingsAccountCharge(final JsonCommand command, final DepositAccountType depositAccountType) {

        this.context.authenticatedUser();
        this.savingsAccountChargeDataValidator.validateUpdate(command.json());
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SAVINGS_ACCOUNT_RESOURCE_NAME);

        final Long savingsAccountId = command.getSavingsId();
        // SavingsAccount Charge entity
        final Long savingsChargeId = command.entityId();

        final SavingsAccount savingsAccount = this.depositAccountAssembler.assembleFrom(savingsAccountId, depositAccountType);
        checkClientOrGroupActive(savingsAccount);

        final SavingsAccountCharge savingsAccountCharge = this.savingsAccountChargeRepository.findOneWithNotFoundDetection(savingsChargeId,
                savingsAccountId);

        final Map<String, Object> changes = savingsAccountCharge.update(command);

        if (savingsAccountCharge.getDueLocalDate() != null) {
            final Locale locale = command.extractLocale();
            final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);

            // transaction date should not be on a holiday or non working day
            if (!this.configurationDomainService.allowTransactionsOnHolidayEnabled()
                    && this.holidayRepository.isHoliday(savingsAccount.officeId(), savingsAccountCharge.getDueLocalDate())) {
                baseDataValidator.reset().parameter(dueAsOfDateParamName).value(savingsAccountCharge.getDueLocalDate().toString(fmt))
                        .failWithCodeNoParameterAddedToErrorCode("charge.due.date.is.on.holiday");
                if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
            }

            if (!this.configurationDomainService.allowTransactionsOnNonWorkingDayEnabled()
                    && !this.workingDaysRepository.isWorkingDay(savingsAccountCharge.getDueLocalDate())) {
                baseDataValidator.reset().parameter(dueAsOfDateParamName).value(savingsAccountCharge.getDueLocalDate().toString(fmt))
                        .failWithCodeNoParameterAddedToErrorCode("charge.due.date.is.a.nonworking.day");
                if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
            }
        }

        this.savingsAccountChargeRepository.saveAndFlush(savingsAccountCharge);

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsAccountCharge.getId()) //
                .withOfficeId(savingsAccountCharge.savingsAccount().officeId()) //
                .withClientId(savingsAccountCharge.savingsAccount().clientId()) //
                .withGroupId(savingsAccountCharge.savingsAccount().groupId()) //
                .withSavingsId(savingsAccountCharge.savingsAccount().getId()) //
                .with(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult waiveCharge(final Long savingsAccountId, final Long savingsAccountChargeId,
            @SuppressWarnings("unused") final DepositAccountType depositAccountType) {

        AppUser user = getAppUserIfPresent();

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final SavingsAccountCharge savingsAccountCharge = this.savingsAccountChargeRepository
                .findOneWithNotFoundDetection(savingsAccountChargeId, savingsAccountId);

        // Get Savings account from savings charge
        final SavingsAccount account = savingsAccountCharge.savingsAccount();
        this.depositAccountAssembler.assignSavingAccountHelpers(account);

        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);

        account.waiveCharge(savingsAccountChargeId, user);
        boolean isInterestTransfer = false;
        LocalDate postInterestOnDate = null;
        final MathContext mc = MathContext.DECIMAL64;
        if (account.isBeforeLastAccrualPostingPeriod(savingsAccountCharge.getDueLocalDate())) {
            account.postAccrualInterest(mc, DateUtils.getLocalDateOfTenant(), isInterestTransfer,
                    isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, postInterestOnDate);
        }
        if (account.isBeforeLastPostingPeriod(savingsAccountCharge.getDueLocalDate())) {
            final LocalDate today = DateUtils.getLocalDateOfTenant();
            account.postInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth,
                    postInterestOnDate);
        } else {
            final LocalDate today = DateUtils.getLocalDateOfTenant();
            account.calculateInterestUsing(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth, postInterestOnDate);
        }
        List<DepositAccountOnHoldTransaction> depositAccountOnHoldTransactions = null;
        if (account.getOnHoldFunds().compareTo(BigDecimal.ZERO) == 1) {
            depositAccountOnHoldTransactions = this.depositAccountOnHoldTransactionRepository
                    .findBySavingsAccountAndReversedFalseOrderByCreatedDateAsc(account);
        }

        account.validateAccountBalanceDoesNotBecomeNegative(SavingsApiConstants.waiveChargeTransactionAction,
                depositAccountOnHoldTransactions);

        this.savingAccountRepositoryWrapper.saveAndFlush(account);

        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsAccountChargeId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsAccountId) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteSavingsAccountCharge(final Long savingsAccountId, final Long savingsAccountChargeId,
            @SuppressWarnings("unused") final JsonCommand command, final DepositAccountType depositAccountType) {
        this.context.authenticatedUser();

        final SavingsAccount savingsAccount = this.depositAccountAssembler.assembleFrom(savingsAccountId, depositAccountType);
        checkClientOrGroupActive(savingsAccount);
        final SavingsAccountCharge savingsAccountCharge = this.savingsAccountChargeRepository
                .findOneWithNotFoundDetection(savingsAccountChargeId, savingsAccountId);

        savingsAccount.removeCharge(savingsAccountCharge);
        this.savingAccountRepositoryWrapper.saveAndFlush(savingsAccount);

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsAccountChargeId) //
                .withOfficeId(savingsAccount.officeId()) //
                .withClientId(savingsAccount.clientId()) //
                .withGroupId(savingsAccount.groupId()) //
                .withSavingsId(savingsAccountId) //
                .build();
    }

    @Override
    public CommandProcessingResult payCharge(final Long savingsAccountId, final Long savingsAccountChargeId, final JsonCommand command,
            @SuppressWarnings("unused") final DepositAccountType depositAccountType) {

        this.context.authenticatedUser();

        this.savingsAccountChargeDataValidator.validatePayCharge(command.json());
        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
        final BigDecimal amountPaid = command.bigDecimalValueOfParameterNamed(amountParamName);
        final LocalDate transactionDate = command.localDateValueOfParameterNamed(dueAsOfDateParamName);

        final SavingsAccountCharge savingsAccountCharge = this.savingsAccountChargeRepository
                .findOneWithNotFoundDetection(savingsAccountChargeId, savingsAccountId);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SAVINGS_ACCOUNT_RESOURCE_NAME);

        // transaction date should not be on a holiday or non working day
        if (!this.configurationDomainService.allowTransactionsOnHolidayEnabled()
                && this.holidayRepository.isHoliday(savingsAccountCharge.savingsAccount().officeId(), transactionDate)) {
            baseDataValidator.reset().parameter(dueAsOfDateParamName).value(transactionDate.toString(fmt))
                    .failWithCodeNoParameterAddedToErrorCode("transaction.not.allowed.transaction.date.is.on.holiday");
            if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
        }

        if (!this.configurationDomainService.allowTransactionsOnNonWorkingDayEnabled()
                && !this.workingDaysRepository.isWorkingDay(transactionDate)) {
            baseDataValidator.reset().parameter(dueAsOfDateParamName).value(transactionDate.toString(fmt))
                    .failWithCodeNoParameterAddedToErrorCode("transaction.not.allowed.transaction.date.is.a.nonworking.day");
            if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
        }

        this.payCharge(savingsAccountCharge, transactionDate, amountPaid, fmt);
        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsAccountCharge.getId()) //
                .withOfficeId(savingsAccountCharge.savingsAccount().officeId()) //
                .withClientId(savingsAccountCharge.savingsAccount().clientId()) //
                .withGroupId(savingsAccountCharge.savingsAccount().groupId()) //
                .withSavingsId(savingsAccountCharge.savingsAccount().getId()) //
                .build();

    }

    @Transactional
    @Override
    public void applyChargeDue(final Long savingsAccountChargeId, final Long accountId,
            @SuppressWarnings("unused") final DepositAccountType depositAccountType) {
        // always use current date as transaction date for batch job
        final LocalDate transactionDate = DateUtils.getLocalDateOfTenant();
        final SavingsAccountCharge savingsAccountCharge = this.savingsAccountChargeRepository
                .findOneWithNotFoundDetection(savingsAccountChargeId, accountId);

        while (transactionDate.isAfter(savingsAccountCharge.getDueLocalDate())) {
            payCharge(savingsAccountCharge, transactionDate, savingsAccountCharge.amoutOutstanding(), DateUtils.getDefaultFormatter());
        }
    }

    @Transactional
    private void payCharge(final SavingsAccountCharge savingsAccountCharge, final LocalDate transactionDate, final BigDecimal amountPaid,
            final DateTimeFormatter formatter) {

        AppUser user = getAppUserIfPresent();

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        // Get Savings account from savings charge
        final SavingsAccount account = savingsAccountCharge.savingsAccount();
        this.depositAccountAssembler.assignSavingAccountHelpers(account);
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
        account.payCharge(savingsAccountCharge, amountPaid, transactionDate, formatter, user);
        boolean isInterestTransfer = false;
        LocalDate postInterestOnDate = null;
        final MathContext mc = MathContext.DECIMAL64;
        if (account.isBeforeLastAccrualPostingPeriod(transactionDate)) {
            account.postAccrualInterest(mc, DateUtils.getLocalDateOfTenant(), isInterestTransfer,
                    isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, null);
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
        List<DepositAccountOnHoldTransaction> depositAccountOnHoldTransactions = null;
        if (account.getOnHoldFunds().compareTo(BigDecimal.ZERO) == 1) {
            depositAccountOnHoldTransactions = this.depositAccountOnHoldTransactionRepository
                    .findBySavingsAccountAndReversedFalseOrderByCreatedDateAsc(account);
        }

        account.validateAccountBalanceDoesNotBecomeNegative("." + SavingsAccountTransactionType.PAY_CHARGE.getCode(),
                depositAccountOnHoldTransactions);

        this.savingAccountRepositoryWrapper.saveAndFlush(account);

        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
    }

    @Transactional
    @Override
    public void updateMaturityDetails(Long depositAccountId, DepositAccountType depositAccountType) {

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final SavingsAccount account = this.depositAccountAssembler.assembleFrom(depositAccountId, depositAccountType);
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);

        if (depositAccountType.isFixedDeposit()) {
            ((FixedDepositAccount) account).updateMaturityStatus(isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth);
        } else if (depositAccountType.isRecurringDeposit()) {
            ((RecurringDepositAccount) account).updateMaturityStatus(isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth);
        }
        this.savingAccountRepositoryWrapper.saveAndFlush(account);
        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
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
        this.journalEntryWritePlatformService.createJournalEntriesForSavings(accountingBridgeData);
    }

    @Transactional
    @Override
    public SavingsAccountTransaction mandatorySavingsAccountDeposit(final SavingsAccountTransactionDTO accountTransactionDTO) {
        boolean isRegularTransaction = false;
        final PaymentDetail paymentDetail = accountTransactionDTO.getPaymentDetail();
        if (paymentDetail != null && paymentDetail.getId() == null) {
            this.paymentDetailWritePlatformService.persistPaymentDetail(paymentDetail);
        }
        if (accountTransactionDTO.getAccountType().equals(DepositAccountType.RECURRING_DEPOSIT.getValue())) {
            RecurringDepositAccount account = (RecurringDepositAccount) this.depositAccountAssembler
                    .assembleFrom(accountTransactionDTO.getSavingsAccountId(), DepositAccountType.RECURRING_DEPOSIT);
            return this.depositAccountDomainService.handleRDDeposit(account, accountTransactionDTO.getFormatter(),
                    accountTransactionDTO.getTransactionDate(), accountTransactionDTO.getTransactionAmount(), paymentDetail,
                    isRegularTransaction);
        }
        SavingsAccount account = null;
        if (accountTransactionDTO.getAccountType().equals(DepositAccountType.SAVINGS_DEPOSIT.getValue())) {
            account = this.depositAccountAssembler.assembleFrom(accountTransactionDTO.getSavingsAccountId(),
                    DepositAccountType.SAVINGS_DEPOSIT);
        } else {
            account = this.depositAccountAssembler.assembleFrom(accountTransactionDTO.getSavingsAccountId(),
                    DepositAccountType.CURRENT_DEPOSIT);
        }
        return this.depositAccountDomainService.handleSavingDeposit(account, accountTransactionDTO.getFormatter(),
                accountTransactionDTO.getTransactionDate(), accountTransactionDTO.getTransactionAmount(), paymentDetail,
                isRegularTransaction);

    }

    private AppUser getAppUserIfPresent() {
        AppUser user = null;
        if (this.context != null) {
            user = this.context.getAuthenticatedUserIfPresent();
        }
        return user;
    }

    @Override
    public CommandProcessingResult updateDepositPeriodForRDAccount(Long savingsId, JsonCommand command) {
        this.depositAccountTransactionDataValidator.validateDepositPeriodUpdate(command);

        final Integer depositPeriod = command
                .integerValueOfParameterNamed(DepositsApiConstants.depositPeriodParamName);
        
        final Integer depositPeriodFrequencyType = command
                .integerValueOfParameterNamed(DepositsApiConstants.depositPeriodFrequencyIdParamName);

        final RecurringDepositAccount recurringDepositAccount = (RecurringDepositAccount) this.depositAccountAssembler
                .assembleFrom(savingsId, DepositAccountType.RECURRING_DEPOSIT);

        final Map<String, Object> actualChanges = new LinkedHashMap<>(10);
        actualChanges.put(DepositsApiConstants.depositPeriodParamName, depositPeriod);

        recurringDepositAccount.updateDepositPeriod(depositPeriod);
        recurringDepositAccount.updateDepositPeriodFrequencyType(depositPeriodFrequencyType);

        final CalendarInstance calendarInstance = this.calendarInstanceRepository.findByEntityIdAndEntityTypeIdAndCalendarTypeId(savingsId,
                CalendarEntityType.SAVINGS.getValue(), CalendarType.COLLECTION.getValue());
        this.saveCalendarDetails(recurringDepositAccount, calendarInstance);

        // update calendar details
        if (!recurringDepositAccount.isCalendarInherited()) {
            final LocalDate calendarStartDate = recurringDepositAccount.depositStartDate();
            Calendar calendar = calendarInstance.getCalendar();
            PeriodFrequencyType frequencyType = CalendarFrequencyType.from(CalendarUtils.getFrequency(calendar.getRecurrence()));
            Integer frequency = CalendarUtils.getInterval(calendar.getRecurrence());
            final Integer repeatsOnDay = calendarStartDate.getDayOfWeek();

            calendar.updateRepeatingCalendar(calendarStartDate, CalendarFrequencyType.from(frequencyType), frequency, repeatsOnDay,
                    null);
            this.calendarInstanceRepository.save(calendarInstance);
        }

        return new CommandProcessingResultBuilder()
                .withEntityId(savingsId)
                .withOfficeId(recurringDepositAccount.officeId())
                .withClientId(recurringDepositAccount.clientId())
                .withGroupId(recurringDepositAccount.groupId())
                .withSavingsId(savingsId)
                .with(actualChanges)
                .build();
    }

    private void saveCalendarDetails(RecurringDepositAccount account, CalendarInstance calendarInstance) {

        boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService.isSavingsInterestPostingAtCurrentPeriodEnd();
        Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();
        Calendar calendar = calendarInstance.getCalendar();
        PeriodFrequencyType frequencyType = CalendarFrequencyType.from(CalendarUtils.getFrequency(calendar.getRecurrence()));
        Integer frequency = CalendarUtils.getInterval(calendar.getRecurrence());

        frequency = frequency == -1 ? 1 : frequency;
        account.generateSchedule(frequencyType, frequency, calendar);
        account.updateMaturityDateAndAmount(MathContext.DECIMAL64, false, isSavingsInterestPostingAtCurrentPeriodEnd,
                financialYearBeginningMonth);
        account.validateApplicableInterestRate();
        this.savingAccountRepositoryWrapper.save(account);
    }

}