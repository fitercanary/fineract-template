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
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.glaccount.domain.GLAccountRepositoryWrapper;
import org.apache.fineract.accounting.journalentry.data.JournalEntryData;
import org.apache.fineract.accounting.journalentry.service.JournalEntryReadPlatformService;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformServiceUnavailableException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksWritePlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.holiday.domain.HolidayRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepositoryWrapper;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysRepositoryWrapper;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.domain.AccountTransferStandingInstruction;
import org.apache.fineract.portfolio.account.domain.StandingInstructionRepository;
import org.apache.fineract.portfolio.account.domain.StandingInstructionStatus;
import org.apache.fineract.portfolio.account.service.AccountAssociationsReadPlatformService;
import org.apache.fineract.portfolio.account.service.AccountTransfersReadPlatformService;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeCalculationType;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_ENTITY;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_EVENTS;
import org.apache.fineract.portfolio.common.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.exception.GroupNotActiveException;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.paymentdetail.service.PaymentDetailWritePlatformService;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.SavingsApiConstants;
import org.apache.fineract.portfolio.savings.SavingsTransactionBooleanValues;
import org.apache.fineract.portfolio.savings.data.SavingsAccountChargeDataValidator;
import org.apache.fineract.portfolio.savings.data.SavingsAccountDataValidator;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionDTO;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionDataValidator;
import org.apache.fineract.portfolio.savings.domain.DepositAccountOnHoldTransaction;
import org.apache.fineract.portfolio.savings.domain.DepositAccountOnHoldTransactionRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountCharge;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountChargeRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountDomainService;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransactionRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsTransactionRequest;
import org.apache.fineract.portfolio.savings.domain.SavingsTransactionRequestRepository;
import org.apache.fineract.portfolio.savings.exception.PostInterestAsOnDateException;
import org.apache.fineract.portfolio.savings.exception.PostInterestAsOnDateException.PostInterestAsOnException_TYPE;
import org.apache.fineract.portfolio.savings.exception.PostInterestClosingDateException;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountClosingNotAllowedException;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountDateException;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountTransactionNotFoundException;
import org.apache.fineract.portfolio.savings.exception.SavingsOfficerAssignmentException;
import org.apache.fineract.portfolio.savings.exception.SavingsOfficerUnassignmentException;
import org.apache.fineract.portfolio.savings.exception.TransactionUpdateNotAllowedException;
import org.apache.fineract.portfolio.savings.request.FixedDepositActivationReq;
import org.apache.fineract.portfolio.savings.request.SavingsAccountChargeReq;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepositoryWrapper;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.joda.time.MonthDay;

import static org.apache.fineract.portfolio.savings.SavingsApiConstants.SAVINGS_ACCOUNT_CHARGE_RESOURCE_NAME;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.amountParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.chargeIdParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.dueAsOfDateParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.withHoldTaxParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.withdrawBalanceParamName;

@Service
public class SavingsAccountWritePlatformServiceJpaRepositoryImpl implements SavingsAccountWritePlatformService {

    private final PlatformSecurityContext context;
    private final SavingsAccountDataValidator fromApiJsonDeserializer;
    private final SavingsAccountRepositoryWrapper savingAccountRepositoryWrapper;
    private final StaffRepositoryWrapper staffRepository;
    private final SavingsAccountTransactionRepository savingsAccountTransactionRepository;
    private final SavingsAccountAssembler savingAccountAssembler;
    private final SavingsAccountTransactionDataValidator savingsAccountTransactionDataValidator;
    private final SavingsAccountChargeDataValidator savingsAccountChargeDataValidator;
    private final PaymentDetailWritePlatformService paymentDetailWritePlatformService;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final SavingsAccountDomainService savingsAccountDomainService;
    private final NoteRepository noteRepository;
    private final AccountTransfersReadPlatformService accountTransfersReadPlatformService;
    private final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService;
    private final ChargeRepositoryWrapper chargeRepository;
    private final SavingsAccountChargeRepositoryWrapper savingsAccountChargeRepository;
    private final HolidayRepositoryWrapper holidayRepository;
    private final WorkingDaysRepositoryWrapper workingDaysRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository;
    private final EntityDatatableChecksWritePlatformService entityDatatableChecksWritePlatformService;
    private final AppUserRepositoryWrapper appuserRepository;
    private final StandingInstructionRepository standingInstructionRepository;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final SavingsTransactionRequestRepository savingsTransactionRequestRepository;
    private final SavingsAccountReadPlatformService savingsAccountReadPlatformService;
    private final CodeValueRepositoryWrapper codeValueRepositoryWrapper;
    final GLAccountRepositoryWrapper glAccountRepositoryWrapper;
    private final JournalEntryReadPlatformService journalEntryReadPlatformService;

    @Autowired
    public SavingsAccountWritePlatformServiceJpaRepositoryImpl(final PlatformSecurityContext context,
            final SavingsAccountRepositoryWrapper savingAccountRepositoryWrapper,
            final SavingsAccountTransactionRepository savingsAccountTransactionRepository,
            final SavingsAccountAssembler savingAccountAssembler,
            final SavingsAccountTransactionDataValidator savingsAccountTransactionDataValidator,
            final SavingsAccountChargeDataValidator savingsAccountChargeDataValidator,
            final PaymentDetailWritePlatformService paymentDetailWritePlatformService,
            final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper,
            final JournalEntryWritePlatformService journalEntryWritePlatformService,
            final SavingsAccountDomainService savingsAccountDomainService, final NoteRepository noteRepository,
            final AccountTransfersReadPlatformService accountTransfersReadPlatformService, final HolidayRepositoryWrapper holidayRepository,
            final WorkingDaysRepositoryWrapper workingDaysRepository,
            final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService,
            final ChargeRepositoryWrapper chargeRepository, final SavingsAccountChargeRepositoryWrapper savingsAccountChargeRepository,
            final SavingsAccountDataValidator fromApiJsonDeserializer, final StaffRepositoryWrapper staffRepository,
            final ConfigurationDomainService configurationDomainService,
            final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository,
            final EntityDatatableChecksWritePlatformService entityDatatableChecksWritePlatformService,
            final AppUserRepositoryWrapper appuserRepository, final StandingInstructionRepository standingInstructionRepository,
            final BusinessEventNotifierService businessEventNotifierService,
            final SavingsTransactionRequestRepository savingsTransactionRequestRepository,
            final SavingsAccountReadPlatformService savingsAccountReadPlatformService,
            final CodeValueRepositoryWrapper codeValueRepositoryWrapper, final GLAccountRepositoryWrapper glAccountRepositoryWrapper,
            final JournalEntryReadPlatformService journalEntryReadPlatformService) {
        this.context = context;
        this.savingAccountRepositoryWrapper = savingAccountRepositoryWrapper;
        this.savingsAccountTransactionRepository = savingsAccountTransactionRepository;
        this.savingAccountAssembler = savingAccountAssembler;
        this.savingsAccountTransactionDataValidator = savingsAccountTransactionDataValidator;
        this.savingsAccountChargeDataValidator = savingsAccountChargeDataValidator;
        this.paymentDetailWritePlatformService = paymentDetailWritePlatformService;
        this.applicationCurrencyRepositoryWrapper = applicationCurrencyRepositoryWrapper;
        this.journalEntryWritePlatformService = journalEntryWritePlatformService;
        this.savingsAccountDomainService = savingsAccountDomainService;
        this.noteRepository = noteRepository;
        this.accountTransfersReadPlatformService = accountTransfersReadPlatformService;
        this.accountAssociationsReadPlatformService = accountAssociationsReadPlatformService;
        this.chargeRepository = chargeRepository;
        this.savingsAccountChargeRepository = savingsAccountChargeRepository;
        this.holidayRepository = holidayRepository;
        this.workingDaysRepository = workingDaysRepository;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.staffRepository = staffRepository;
        this.configurationDomainService = configurationDomainService;
        this.depositAccountOnHoldTransactionRepository = depositAccountOnHoldTransactionRepository;
        this.entityDatatableChecksWritePlatformService = entityDatatableChecksWritePlatformService;
        this.appuserRepository = appuserRepository;
        this.standingInstructionRepository = standingInstructionRepository;
        this.businessEventNotifierService = businessEventNotifierService;
        this.savingsTransactionRequestRepository = savingsTransactionRequestRepository;
        this.savingsAccountReadPlatformService = savingsAccountReadPlatformService;
        this.codeValueRepositoryWrapper = codeValueRepositoryWrapper;
        this.glAccountRepositoryWrapper = glAccountRepositoryWrapper;
        this.journalEntryReadPlatformService = journalEntryReadPlatformService;
    }

    @Transactional
    @Override
    public CommandProcessingResult activate(final Long savingsId, final JsonCommand command) {

        final AppUser user = this.context.authenticatedUser();

        this.savingsAccountTransactionDataValidator.validateActivation(command);

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
        final Map<String, Object> changes = account.activate(user, FixedDepositActivationReq.instance(command));

        entityDatatableChecksWritePlatformService.runTheCheckForProduct(savingsId, EntityTables.SAVING.getName(),
                StatusEnum.ACTIVATE.getCode().longValue(), EntityTables.SAVING.getForeignKeyColumnNameOnDatatable(), account.productId());

        if (!changes.isEmpty()) {
            final Locale locale = command.extractLocale();
            final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
            processPostActiveActions(account, fmt, existingTransactionIds, existingReversedTransactionIds);

            this.savingAccountRepositoryWrapper.saveAndFlush(account);
        }

        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.SAVINGS_ACTIVATE,
                constructEntityMap(BUSINESS_ENTITY.SAVING, account));

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .with(changes) //
                .build();
    }

    @Override
    public void processPostActiveActions(final SavingsAccount account, final DateTimeFormatter fmt, final Set<Long> existingTransactionIds,
            final Set<Long> existingReversedTransactionIds) {

        AppUser user = getAppUserIfPresent();

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        Money amountForDeposit = account.activateWithBalance();
        boolean isRegularTransaction = false;
        if (amountForDeposit.isGreaterThanZero()) {
            boolean isAccountTransfer = false;
            this.savingsAccountDomainService.handleDeposit(account, fmt, account.getActivationLocalDate(), amountForDeposit.getAmount(),
                    null, isAccountTransfer, isRegularTransaction, null);
            updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
        }
        account.processAccountUponActivation(isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, user);
        List<DepositAccountOnHoldTransaction> depositAccountOnHoldTransactions = null;
        if (account.getOnHoldFunds().compareTo(BigDecimal.ZERO) == 1) {
            depositAccountOnHoldTransactions = this.depositAccountOnHoldTransactionRepository
                    .findBySavingsAccountAndReversedFalseOrderByCreatedDateAsc(account);
        }
        account.validateAccountBalanceDoesNotBecomeNegative(SavingsAccountTransactionType.PAY_CHARGE.name(),
                depositAccountOnHoldTransactions);
    }

    @Transactional
    @Override
    public CommandProcessingResult makeMultiplePostings(final JsonCommand command) {
        final Map<String, Object> changes = new LinkedHashMap<>();
        final ArrayList<Long> savingsCreditTransactionId = new ArrayList<>();
        final ArrayList<Long> savingsDebitTransactionId = new ArrayList<>();

        final JsonArray savingsCredits = command.arrayOfParameterNamed("savingsCredits");
        if (savingsCredits.size() > 0) {
            // make deposit for each savings account
            for (int i = 0; i < savingsCredits.size(); i++) {
                final JsonElement savingsAccountId = savingsCredits.get(i).getAsJsonObject()
                    .get("savingsAccountId");
                final JsonElement transactionAmount = savingsCredits.get(i).getAsJsonObject()
                    .get("amount");
                if (savingsAccountId != null) {
                    CommandProcessingResult deposit = deposit(savingsAccountId.getAsLong(), command, true);
                    savingsCreditTransactionId.add(deposit.resourceId());
                }
            }
        }
        final JsonArray savingsDebits = command.arrayOfParameterNamed("savingsDebits");
        if (savingsDebits.size() > 0) {
            // make withdrawal for each savings account
            for (int i = 0; i < savingsDebits.size(); i++) {
                final JsonElement savingsAccountId = savingsDebits.get(i).getAsJsonObject()
                    .get("savingsAccountId");
                final JsonElement transactionAmount = savingsDebits.get(i).getAsJsonObject()
                    .get("amount");
                if (savingsAccountId != null) {
                    CommandProcessingResult withdrawal = withdrawal(savingsAccountId.getAsLong(), command, true);
                    savingsDebitTransactionId.add(withdrawal.resourceId());
                }
            }
        }
        return this.journalEntryWritePlatformService.createJournalEntry(command, savingsCreditTransactionId, savingsDebitTransactionId);
    }

    private CommandProcessingResult deposit(final Long savingsId, final JsonCommand command, boolean shouldIgnoreGlAccountId) {

        this.context.authenticatedUser();

        this.savingsAccountTransactionDataValidator.validate(command);

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);
        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);

        final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        final LocalDate postingDate = command.localDateValueOfParameterNamed("postingDate");
        BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
        final String noteText = command.stringValueOfParameterNamed("note");
        final Long glAccountId = command.longValueOfParameterNamed("glAccountId");
        final JsonArray savingsCredits = command.arrayOfParameterNamed("savingsCredits");

        GLAccount glAccount = null;
        if (glAccountId != null && !shouldIgnoreGlAccountId) {
            glAccount = this.glAccountRepositoryWrapper.findOneWithNotFoundDetection(glAccountId);
        }
        final Map<String, Object> changes = new LinkedHashMap<>();
        final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);
        boolean isAccountTransfer = false;
        boolean isRegularTransaction = true;

        if (command.hasParameter("isManualTransaction")) {
            isRegularTransaction = !command.booleanPrimitiveValueOfParameterNamed("isManualTransaction");
        }

        if (savingsCredits != null && savingsCredits.size() > 0) {
            for (int i = 0; i < savingsCredits.size(); i++) {
                if (savingsCredits.get(i).getAsJsonObject().get("savingsAccountId").getAsLong() == savingsId) {
                    transactionAmount = savingsCredits.get(i).getAsJsonObject().get("amount").getAsBigDecimal();
                }
            }
        }
        final SavingsAccountTransaction deposit = this.savingsAccountDomainService.handleDeposit(account, fmt, transactionDate, postingDate,
                transactionAmount, paymentDetail, isAccountTransfer, isRegularTransaction, glAccount, noteText);

        this.saveTransactionRequest(command, deposit);

        //this is already handled in handle deposit method
//        if (StringUtils.isNotBlank(noteText)) {
//            final Note note = Note.savingsTransactionNote(account, deposit, noteText);
//            this.noteRepository.save(note);
//        }

        return new CommandProcessingResultBuilder() //
                .withEntityId(deposit.getId()) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .with(changes) //
                .build();

    }

    @Transactional
    @Override
    public CommandProcessingResult deposit(final Long savingsId, final JsonCommand command) {

        return deposit(savingsId, command, false);

    }

    @Override
    public void saveTransactionRequest(JsonCommand command, SavingsAccountTransaction transaction) {

        String notes = command.stringValueOfParameterNamed(SavingsApiConstants.notesParamName);
        String remarks = command.stringValueOfParameterNamed(SavingsApiConstants.remarksParamName);
        String category = command.stringValueOfParameterNamed(SavingsApiConstants.categoryParamName);
        String imageTag = command.stringValueOfParameterNamed(SavingsApiConstants.imageTagParamName);
        String latitude = command.stringValueOfParameterNamed(SavingsApiConstants.latitudeParamName);
        String longitude = command.stringValueOfParameterNamed(SavingsApiConstants.longitudeParamName);
        String notesImage = command.stringValueOfParameterNamed(SavingsApiConstants.notesImageParamName);
        String transactionBrand = command.stringValueOfParameterNamed(SavingsApiConstants.transactionBrandParamName);

        if (notes != null || remarks != null || category != null || imageTag != null || latitude != null || longitude != null
                || notesImage != null || transactionBrand != null) {
            SavingsTransactionRequest transactionRequest = this.savingsTransactionRequestRepository
                    .findByTransactionId(transaction.getId());
            if (transactionRequest == null) {
                transactionRequest = new SavingsTransactionRequest();
                transactionRequest.setTransaction(transaction);
            }
            if (StringUtils.isNotBlank(notes)) transactionRequest.setNotes(notes);
            if (StringUtils.isNotBlank(remarks)) transactionRequest.setRemarks(remarks);
            if (StringUtils.isNotBlank(category)) transactionRequest.setCategory(category);
            if (StringUtils.isNotBlank(imageTag)) transactionRequest.setImageTag(imageTag);
            if (StringUtils.isNotBlank(latitude)) transactionRequest.setLatitude(latitude);
            if (StringUtils.isNotBlank(longitude)) transactionRequest.setLongitude(longitude);
            if (StringUtils.isNotBlank(notesImage)) transactionRequest.setNoteImage(notesImage);
            if (StringUtils.isNotBlank(transactionBrand)) transactionRequest.setTransactionBrandName(transactionBrand);
            this.savingsTransactionRequestRepository.saveAndFlush(transactionRequest);
        }
    }

    private Long saveTransactionToGenerateTransactionId(final SavingsAccountTransaction transaction) {
        this.savingsAccountTransactionRepository.saveAndFlush(transaction);
        return transaction.getId();
    }

    @Transactional
    @Override
    public CommandProcessingResult withdrawal(final Long savingsId, final JsonCommand command) {

        return withdrawal(savingsId, command, false);
    }
    public CommandProcessingResult withdrawal(final Long savingsId, final JsonCommand command, boolean shouldIgnoreGlAccountId) {

        this.savingsAccountTransactionDataValidator.validate(command);

        final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        final LocalDate postingDate = command.localDateValueOfParameterNamed("postingDate");
        final Long glAccountId = command.longValueOfParameterNamed("glAccountId");
        BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
        final String noteText = command.stringValueOfParameterNamed("note");
        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
        final JsonArray savingsDebits = command.arrayOfParameterNamed("savingsDebits");

        final Map<String, Object> changes = new LinkedHashMap<>();
        final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        Client client = account.getClient();

        GLAccount glAccount = null;
        if (glAccountId != null && !shouldIgnoreGlAccountId) {
            glAccount = this.glAccountRepositoryWrapper.findOneWithNotFoundDetection(glAccountId);
        }
        if (savingsDebits != null && savingsDebits.size() > 0) {
            for (int i = 0; i < savingsDebits.size(); i++) {
                if (savingsDebits.get(i).getAsJsonObject().get("savingsAccountId").getAsLong() == savingsId) {
                    transactionAmount = savingsDebits.get(i).getAsJsonObject().get("amount").getAsBigDecimal();
                }
            }
        }

        checkClientOrGroupActive(account);
        final boolean isAccountTransfer = false;
        final boolean isApplyWithdrawFee = true;
        final boolean isInterestTransfer = false;
        final boolean isWithdrawBalance = false;
        final boolean isApplyOverdraftFee = true;
        boolean isRegularTransaction = true;

        if (command.hasParameter("isManualTransaction")) {
            isRegularTransaction = !command.booleanPrimitiveValueOfParameterNamed("isManualTransaction");
        }
        final SavingsTransactionBooleanValues transactionBooleanValues = new SavingsTransactionBooleanValues(isAccountTransfer,
                isRegularTransaction, isApplyWithdrawFee, isInterestTransfer, isWithdrawBalance, isApplyOverdraftFee);

        final SavingsAccountTransaction withdrawal = this.savingsAccountDomainService.handleWithdrawal(account, fmt, transactionDate, postingDate,
                transactionAmount, paymentDetail, transactionBooleanValues, false, glAccount, noteText);

        this.saveTransactionRequest(command, withdrawal);

        //this is already hadnled in the savingsAccountDomainService.handleWithdrawal method
//        if (StringUtils.isNotBlank(noteText)) {
//            final Note note = Note.savingsTransactionNote(account, withdrawal, noteText);
//            this.noteRepository.save(note);
//        }

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
    public CommandProcessingResult applyAnnualFee(final Long savingsAccountChargeId, final Long accountId) {

        AppUser user = getAppUserIfPresent();

        final SavingsAccountCharge savingsAccountCharge = this.savingsAccountChargeRepository
                .findOneWithNotFoundDetection(savingsAccountChargeId, accountId);

        final DateTimeFormatter fmt = DateTimeFormat.forPattern("dd MM yyyy");

        this.payCharge(savingsAccountCharge, savingsAccountCharge.getDueLocalDate(), savingsAccountCharge.amount(), fmt, user);
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
    public CommandProcessingResult calculateInterest(final Long savingsId) {

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);
        final LocalDate today = DateUtils.getLocalDateOfTenant().minusDays(1);//DateUtils.getLocalDateOfTenant();
        final MathContext mc = new MathContext(15, MoneyHelper.getRoundingMode());
        boolean isInterestTransfer = false;
        final LocalDate postInterestOnDate = null;
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

    @Override
    public CommandProcessingResult postInterest(final JsonCommand command) {

        Long savingsId = command.getSavingsId();
        final boolean postInterestAs = command.booleanPrimitiveValueOfParameterNamed("isPostInterestAsOn");
        final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);
        if (postInterestAs == true) {

            if (transactionDate == null) {

                throw new PostInterestAsOnDateException(PostInterestAsOnException_TYPE.VALID_DATE);
            }
            if (transactionDate.isBefore(account.accountSubmittedOrActivationDate())) {
                throw new PostInterestAsOnDateException(PostInterestAsOnException_TYPE.ACTIVATION_DATE);
            }
            List<SavingsAccountTransaction> savingTransactions = account.getTransactions();
            for (SavingsAccountTransaction savingTransaction : savingTransactions) {
                if (!savingTransaction.isReversed() && !savingTransaction.isAccrualInterestPostingAndNotReversed()
                        && !savingTransaction.isOverdraftAccrualInterestAndNotReversed()
                        && transactionDate.toDate().before(savingTransaction.getDateOf())) {
                    throw new PostInterestAsOnDateException(PostInterestAsOnException_TYPE.LAST_TRANSACTION_DATE);
                }
            }

            LocalDate today = DateUtils.getLocalDateOfTenant();
            if (transactionDate.isAfter(today)) { throw new PostInterestAsOnDateException(PostInterestAsOnException_TYPE.FUTURE_DATE); }

        }
        postInterest(account, postInterestAs, transactionDate);

        this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.SAVINGS_POST_INTEREST,
                constructEntityMap(BUSINESS_ENTITY.SAVING, account));
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
    public void postInterest(final SavingsAccount account, final boolean postInterestAs, final LocalDate transactionDate) {

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();
        if ((account.getNominalAnnualInterestRate() != null && account.getNominalAnnualInterestRate().compareTo(BigDecimal.ZERO) > 0)
                || (account.getNominalAnnualInterestRateOverdraft() != null && account.getNominalAnnualInterestRateOverdraft().compareTo(BigDecimal.ZERO) > 0)) {
            final Set<Long> existingTransactionIds = new HashSet<>();
            final Set<Long> existingReversedTransactionIds = new HashSet<>();
            updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
            final LocalDate today = DateUtils.getLocalDateOfTenant();
            final MathContext mc = new MathContext(10, MoneyHelper.getRoundingMode());
            boolean isInterestTransfer = false;
            LocalDate postInterestOnDate = null;
            if (postInterestAs) {
                postInterestOnDate = transactionDate;
            }
            account.postAccrualInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth, postInterestOnDate, null);
            account.postInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth,
                    postInterestOnDate);
            // for generating transaction id's
            List<SavingsAccountTransaction> transactions = account.getTransactions();
            for (SavingsAccountTransaction accountTransaction : transactions) {
                if (accountTransaction.getId() == null) {
                    this.savingsAccountTransactionRepository.save(accountTransaction);

                    String noteText = SavingsEnumerations.transactionType(accountTransaction.getTypeOf()).getValue();
                    this.noteRepository.save(Note.savingsTransactionNote(account, accountTransaction, noteText));
                }
            }
            this.savingAccountRepositoryWrapper.saveAndFlush(account);
            postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult revertMultipleTransactions(final JsonCommand command) {
        final String transactionId = command.stringValueOfParameterNamed("transactionId");
        List<JournalEntryData> jlEntries = this.journalEntryReadPlatformService.retrieveJournalEntriesByTransactionId(transactionId).getPageItems();
        System.out.println(jlEntries.size());
        for (JournalEntryData entry : jlEntries) {
            if (entry.getSavingsAccountId() != null && entry.getSavingsAccountId() > 0) {
                undoTransaction(entry.getSavingsAccountId(), entry.getSavingsTransactionId(), false);
            }
        }
        return this.journalEntryWritePlatformService.revertJournalEntry(command);
    }

    @Override
    public CommandProcessingResult undoTransaction(final Long savingsId, final Long transactionId,
            final boolean allowAccountTransferModification) {

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);

        final SavingsAccountTransaction savingsAccountTransaction = this.savingsAccountTransactionRepository
                .findOneByIdAndSavingsAccountId(transactionId, savingsId);
        if (savingsAccountTransaction == null) { throw new SavingsAccountTransactionNotFoundException(savingsId, transactionId); }

        if (!allowAccountTransferModification
                && this.accountTransfersReadPlatformService.isAccountTransfer(transactionId, PortfolioAccountType.SAVINGS)) {
            throw new PlatformServiceUnavailableException("error.msg.saving.account.transfer.transaction.update.not.allowed",
                    "Savings account transaction:" + transactionId + " update not allowed as it involves in account transfer",
                    transactionId);
        }

        if (!account.allowModify()) {
            throw new PlatformServiceUnavailableException("error.msg.saving.account.transaction.update.not.allowed",
                    "Savings account transaction:" + transactionId + " update not allowed for this savings type", transactionId);
        }

        final LocalDate today = DateUtils.getLocalDateOfTenant();
        final MathContext mc = new MathContext(15, MoneyHelper.getRoundingMode());

        if (account.isNotActive()) {
            throwValidationForActiveStatus(SavingsApiConstants.undoTransactionAction);
        }
        account.undoTransaction(transactionId);

        // undoing transaction is withdrawal then undo withdrawal fee
        // transaction if any
        if (savingsAccountTransaction.isWithdrawal()) {
            final SavingsAccountTransaction nextSavingsAccountTransaction = this.savingsAccountTransactionRepository
                    .findOneByIdAndSavingsAccountId(transactionId + 1, savingsId);
            if (nextSavingsAccountTransaction != null && nextSavingsAccountTransaction.isWithdrawalFeeAndNotReversed()) {
                account.undoTransaction(transactionId + 1);
            }
        }
        checkClientOrGroupActive(account);
        if (savingsAccountTransaction.isPostInterestCalculationRequired()
                && account.isBeforeLastAccrualPostingPeriod(savingsAccountTransaction.transactionLocalDate())) {
            account.postAccrualInterest(mc, today, false, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth, null, null);
        }
        if (savingsAccountTransaction.isPostInterestCalculationRequired()
                && account.isBeforeLastPostingPeriod(savingsAccountTransaction.transactionLocalDate())) {
            account.postInterest(mc, today, false, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth,
                    null);
        } else {
            account.calculateInterestUsing(mc, today, false, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth, null);
        }
        List<DepositAccountOnHoldTransaction> depositAccountOnHoldTransactions = null;
        if (account.getOnHoldFunds().compareTo(BigDecimal.ZERO) == 1) {
            depositAccountOnHoldTransactions = this.depositAccountOnHoldTransactionRepository
                    .findBySavingsAccountAndReversedFalseOrderByCreatedDateAsc(account);
        }
        account.validateAccountBalanceDoesNotBecomeNegative(SavingsApiConstants.undoTransactionAction, depositAccountOnHoldTransactions);
        account.activateAccountBasedOnBalance();
        this.savingAccountRepositoryWrapper.saveAndFlush(account);
        if (!savingsAccountTransaction.isWaiveCharge()) {
            postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        }

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .build();
    }

    @Override
    public CommandProcessingResult adjustSavingsTransaction(final Long savingsId, final Long transactionId, final JsonCommand command) {

        AppUser user = getAppUserIfPresent();

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final SavingsAccountTransaction savingsAccountTransaction = this.savingsAccountTransactionRepository
                .findOneByIdAndSavingsAccountId(transactionId, savingsId);
        if (savingsAccountTransaction == null) { throw new SavingsAccountTransactionNotFoundException(savingsId, transactionId); }

        if (!(savingsAccountTransaction.isDeposit() || savingsAccountTransaction.isWithdrawal())
                || savingsAccountTransaction.isReversed()) {
            throw new TransactionUpdateNotAllowedException(savingsId, transactionId);
        }

        if (this.accountTransfersReadPlatformService.isAccountTransfer(transactionId, PortfolioAccountType.SAVINGS)) {
            throw new PlatformServiceUnavailableException("error.msg.saving.account.transfer.transaction.update.not.allowed",
                    "Savings account transaction:" + transactionId + " update not allowed as it involves in account transfer",
                    transactionId);
        }

        this.savingsAccountTransactionDataValidator.validate(command);

        final LocalDate today = DateUtils.getLocalDateOfTenant();

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        if (account.isNotActive()) {
            throwValidationForActiveStatus(SavingsApiConstants.adjustTransactionAction);
        }
        if (!account.allowModify()) {
            throw new PlatformServiceUnavailableException("error.msg.saving.account.transaction.update.not.allowed",
                    "Savings account transaction:" + transactionId + " update not allowed for this savings type", transactionId);
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

        // for undo withdrawal fee
        final SavingsAccountTransaction nextSavingsAccountTransaction = this.savingsAccountTransactionRepository
                .findOneByIdAndSavingsAccountId(transactionId + 1, savingsId);
        if (nextSavingsAccountTransaction != null && nextSavingsAccountTransaction.isWithdrawalFeeAndNotReversed()) {
            account.undoTransaction(transactionId + 1);
        }

        SavingsAccountTransaction transaction = null;
        boolean isInterestTransfer = false;
        Integer accountType = null;
        final SavingsAccountTransactionDTO transactionDTO = new SavingsAccountTransactionDTO(fmt, transactionDate, transactionAmount,
                paymentDetail, savingsAccountTransaction.createdDate(), user, accountType,
                savingsAccountTransaction.getIsAccountTransfer());
        if (savingsAccountTransaction.isDeposit()) {
            transaction = account.deposit(transactionDTO);
        } else {
            transaction = account.withdraw(transactionDTO, true);
        }
        final Long newtransactionId = saveTransactionToGenerateTransactionId(transaction);
        final LocalDate postInterestOnDate = null;
        if (account.isBeforeLastAccrualPostingPeriod(transactionDate)
                || account.isBeforeLastAccrualPostingPeriod(savingsAccountTransaction.transactionLocalDate())) {
            account.postAccrualInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth, postInterestOnDate, null);
        }
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

    @Override
    public CommandProcessingResult modifyTransactionRequest(final Long savingsId, final Long transactionId, final JsonCommand command) {

        final SavingsAccountTransaction savingsAccountTransaction = this.savingsAccountTransactionRepository
                .findOneByIdAndSavingsAccountId(transactionId, savingsId);
        this.saveTransactionRequest(command, savingsAccountTransaction);

        return new CommandProcessingResultBuilder().withEntityId(transactionId).build();
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
    public CommandProcessingResult close(final Long savingsId, final JsonCommand command) {
        final AppUser user = this.context.authenticatedUser();

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        this.savingsAccountTransactionDataValidator.validateClosing(command, account);

        final boolean isLinkedWithAnyActiveLoan = this.accountAssociationsReadPlatformService.isLinkedWithAnyActiveAccount(savingsId);

        if (isLinkedWithAnyActiveLoan) {
            final String defaultUserMessage = "Closing savings account with id:" + savingsId
                    + " is not allowed, since it is linked with one of the active accounts";
            throw new SavingsAccountClosingNotAllowedException("linked", defaultUserMessage, savingsId);
        }

        entityDatatableChecksWritePlatformService.runTheCheckForProduct(savingsId, EntityTables.SAVING.getName(),
                StatusEnum.CLOSE.getCode().longValue(), EntityTables.SAVING.getForeignKeyColumnNameOnDatatable(), account.productId());

        final boolean isWithdrawBalance = command.booleanPrimitiveValueOfParameterNamed(withdrawBalanceParamName);

        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
        final LocalDate closedDate = command.localDateValueOfParameterNamed(SavingsApiConstants.closedOnDateParamName);
        final boolean isPostInterest = command.booleanPrimitiveValueOfParameterNamed(SavingsApiConstants.postInterestValidationOnClosure);
        // postInterest(account,closedDate,flag);
        if (isPostInterest) {
            boolean postInterestOnClosingDate = false;
            List<SavingsAccountTransaction> savingTransactions = account.getTransactions();
            for (SavingsAccountTransaction savingTransaction : savingTransactions) {
                if (savingTransaction.isInterestPosting() && savingTransaction.isNotReversed()
                        && closedDate.isEqual(savingTransaction.getTransactionLocalDate())) {
                    postInterestOnClosingDate = true;
                    break;
                }
            }
            if (postInterestOnClosingDate == false) { throw new PostInterestClosingDateException(); }
        }

        final Map<String, Object> changes = new LinkedHashMap<>();

        if (isWithdrawBalance && account.getSummary().getAccountBalance(account.getCurrency()).isGreaterThanZero()) {

            final BigDecimal transactionAmount = account.getSummary().getAccountBalance();

            final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);

            final boolean isAccountTransfer = false;
            final boolean isRegularTransaction = true;
            final boolean isApplyWithdrawFee = false;
            final boolean isInterestTransfer = false;
            final SavingsTransactionBooleanValues transactionBooleanValues = new SavingsTransactionBooleanValues(isAccountTransfer,
                    isRegularTransaction, isApplyWithdrawFee, isInterestTransfer, isWithdrawBalance);
            account.setMinRequiredBalance(BigDecimal.ZERO);
            this.savingsAccountDomainService.handleWithdrawal(account, fmt, closedDate, transactionAmount, paymentDetail,
                    transactionBooleanValues, false, null, null);

        }

        final Map<String, Object> accountChanges = account.close(user, command, DateUtils.getLocalDateOfTenant());
        changes.putAll(accountChanges);
        if (!changes.isEmpty()) {
            this.savingAccountRepositoryWrapper.save(account);
            final String noteText = command.stringValueOfParameterNamed("note");
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.savingNote(account, noteText);
                changes.put("note", noteText);
                this.noteRepository.save(note);
            }

        }

        this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.SAVINGS_CLOSE,
                constructEntityMap(BUSINESS_ENTITY.SAVING, account));
        // disable all standing orders linked to the savings account
        this.disableStandingInstructionsLinkedToClosedSavings(account);
        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .with(changes) //
                .build();
    }

    @Override
    public SavingsAccountTransaction initiateSavingsTransfer(final SavingsAccount savingsAccount, final LocalDate transferDate) {

        AppUser user = getAppUserIfPresent();

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();
        this.savingAccountAssembler.setHelpers(savingsAccount);
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(savingsAccount, existingTransactionIds, existingReversedTransactionIds);

        final SavingsAccountTransaction newTransferTransaction = SavingsAccountTransaction.initiateTransfer(savingsAccount,
                savingsAccount.office(), transferDate, user, false);
        savingsAccount.addTransaction(newTransferTransaction);
        savingsAccount.setStatus(SavingsAccountStatusType.TRANSFER_IN_PROGRESS.getValue());
        final MathContext mc = MathContext.DECIMAL64;
        boolean isInterestTransfer = false;
        LocalDate postInterestOnDate = null;
        savingsAccount.calculateInterestUsing(mc, transferDate, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                financialYearBeginningMonth, postInterestOnDate);

        this.savingsAccountTransactionRepository.save(newTransferTransaction);
        this.savingAccountRepositoryWrapper.saveAndFlush(savingsAccount);

        postJournalEntries(savingsAccount, existingTransactionIds, existingReversedTransactionIds);

        return newTransferTransaction;
    }

    @Override
    public SavingsAccountTransaction withdrawSavingsTransfer(final SavingsAccount savingsAccount, final LocalDate transferDate) {

        AppUser user = getAppUserIfPresent();

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();
        this.savingAccountAssembler.setHelpers(savingsAccount);
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
    public void rejectSavingsTransfer(final SavingsAccount savingsAccount) {
        this.savingAccountAssembler.setHelpers(savingsAccount);
        savingsAccount.setStatus(SavingsAccountStatusType.TRANSFER_ON_HOLD.getValue());
        this.savingAccountRepositoryWrapper.save(savingsAccount);
    }

    @Override
    public SavingsAccountTransaction acceptSavingsTransfer(final SavingsAccount savingsAccount, final LocalDate transferDate,
            final Office acceptedInOffice, final Staff fieldOfficer) {

        AppUser user = getAppUserIfPresent();

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();
        this.savingAccountAssembler.setHelpers(savingsAccount);
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
        final MathContext mc = MathContext.DECIMAL64;
        LocalDate postInterestOnDate = null;
        savingsAccount.calculateInterestUsing(mc, transferDate, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                financialYearBeginningMonth, postInterestOnDate);

        this.savingsAccountTransactionRepository.save(acceptTransferTransaction);
        this.savingAccountRepositoryWrapper.saveAndFlush(savingsAccount);

        postJournalEntries(savingsAccount, existingTransactionIds, existingReversedTransactionIds);

        return acceptTransferTransaction;
    }

    @Transactional
    @Override
    public CommandProcessingResult addSavingsAccountCharge(final JsonCommand command) {

        this.context.authenticatedUser();
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SAVINGS_ACCOUNT_RESOURCE_NAME);

        final Long savingsAccountId = command.getSavingsId();
        this.savingsAccountChargeDataValidator.validateAdd(command.json());

        final SavingsAccount savingsAccount = this.savingAccountAssembler.assembleFrom(savingsAccountId);
        checkClientOrGroupActive(savingsAccount);

        final Locale locale = command.extractLocale();
        final String format = command.dateFormat();
        final DateTimeFormatter fmt = StringUtils.isNotBlank(format) ? DateTimeFormat.forPattern(format).withLocale(locale)
                : DateTimeFormat.forPattern("dd MM yyyy");

        final Long chargeDefinitionId = command.longValueOfParameterNamed(chargeIdParamName);
        final Charge chargeDefinition = this.chargeRepository.findOneWithNotFoundDetection(chargeDefinitionId);

        Integer chargeTimeType = chargeDefinition.getChargeTimeType();
        LocalDate dueAsOfDateParam = command.localDateValueOfParameterNamed(dueAsOfDateParamName);
        if ((chargeTimeType.equals(ChargeTimeType.WITHDRAWAL_FEE.getValue())
                || chargeTimeType.equals(ChargeTimeType.SAVINGS_NOACTIVITY_FEE.getValue())
                || chargeTimeType.equals(ChargeTimeType.OVERDRAFT_FEE.getValue())) && dueAsOfDateParam != null) {
            baseDataValidator.reset().parameter(dueAsOfDateParamName).value(dueAsOfDateParam.toString(fmt))
                    .failWithCodeNoParameterAddedToErrorCode(
                            "charge.due.date.is.invalid.for." + ChargeTimeType.fromInt(chargeTimeType).getCode());
        }

        // calculation is in this line
        SavingsAccountChargeReq savingsAccountChargeReq = SavingsAccountChargeReq.instance(command);

        //  FIXME take care of m% charges in this case total withdrawals. We can look into how to make this more generic.
        // FIXME For now this works for CANARY case. Time was not enough to make it more generic
        if(ChargeCalculationType.fromInt(chargeDefinition.getChargeCalculation()).equals(ChargeCalculationType.PERCENT_OF_TOTAL_WITHDRAWALS)) {
            BigDecimal amountPercentAppliedTo = BigDecimal.ZERO;

            if (dueAsOfDateParam != null && savingsAccountChargeReq.getFeeInterval() != null) {
                LocalDate startDate = dueAsOfDateParam.minusMonths(savingsAccountChargeReq.getFeeInterval());

                // get total withdrawals for period i.e if due date is 1st Feb and interval is 1 Month
                // then we get charges from 1st Jan to 31st Jan [ 1 Month]
                // we use date before due date so we can capture all withdrawals up to midnight the day
                amountPercentAppliedTo = savingsAccount.getTotalWithdrawalsBetweenDatesInclusive(startDate, dueAsOfDateParam.minusDays(1));

                savingsAccountChargeReq.setAmountPercentAppliedTo(amountPercentAppliedTo);
            }

        }
        final SavingsAccountCharge savingsAccountCharge = SavingsAccountCharge.createNew(savingsAccount, chargeDefinition, savingsAccountChargeReq);

        if (savingsAccountCharge.getDueLocalDate() != null) {
            // transaction date should not be on a holiday or non working day
            if (!this.configurationDomainService.allowTransactionsOnHolidayEnabled()
                    && this.holidayRepository.isHoliday(savingsAccount.officeId(), savingsAccountCharge.getDueLocalDate())) {
                baseDataValidator.reset().parameter(dueAsOfDateParamName).value(savingsAccountCharge.getDueLocalDate().toString(fmt))
                        .failWithCodeNoParameterAddedToErrorCode("charge.due.date.is.on.holiday");
            }

            if (!this.configurationDomainService.allowTransactionsOnNonWorkingDayEnabled()
                    && !this.workingDaysRepository.isWorkingDay(savingsAccountCharge.getDueLocalDate())) {
                baseDataValidator.reset().parameter(dueAsOfDateParamName).value(savingsAccountCharge.getDueLocalDate().toString(fmt))
                        .failWithCodeNoParameterAddedToErrorCode("charge.due.date.is.a.nonworking.day");
            }
        }
        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }

        savingsAccount.addCharge(fmt, savingsAccountCharge, chargeDefinition);
        this.savingsAccountChargeRepository.save(savingsAccountCharge);
        this.savingAccountRepositoryWrapper.saveAndFlush(savingsAccount);
        return new CommandProcessingResultBuilder()
                .withEntityId(savingsAccountCharge.getId())
                .withOfficeId(savingsAccount.officeId())
                .withClientId(savingsAccount.clientId())
                .withGroupId(savingsAccount.groupId())
                .withSavingsId(savingsAccountId)
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult updateSavingsAccountCharge(final JsonCommand command) {

        this.context.authenticatedUser();
        this.savingsAccountChargeDataValidator.validateUpdate(command.json());
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SAVINGS_ACCOUNT_RESOURCE_NAME);

        final Long savingsAccountId = command.getSavingsId();
        // SavingsAccount Charge entity
        final Long savingsChargeId = command.entityId();

        final SavingsAccount savingsAccount = this.savingAccountAssembler.assembleFrom(savingsAccountId);
        checkClientOrGroupActive(savingsAccount);

        final SavingsAccountCharge savingsAccountCharge = this.savingsAccountChargeRepository.findOneWithNotFoundDetection(savingsChargeId,
                savingsAccountId);

        // calculate transaction amount
        BigDecimal transactionAmount = new BigDecimal(0);

        final Map<String, Object> changes = savingsAccountCharge.update(command, transactionAmount);
        //  FIXME take care of m% charges in this case total withdrawals. We can look into how to make this more generic.
        // FIXME For now this works for CANARY case.
        if(ChargeCalculationType.fromInt(savingsAccountCharge.getChargeCalculation()).equals(ChargeCalculationType.PERCENT_OF_TOTAL_WITHDRAWALS)) {
            BigDecimal amountPercentAppliedTo = BigDecimal.ZERO;

            if(changes.containsKey(dueAsOfDateParamName)) {
                if ( savingsAccountCharge.getDueLocalDate()!= null && savingsAccountCharge.getFeeInterval() != null) {
                    LocalDate startDate = savingsAccountCharge.getDueLocalDate().minusMonths(savingsAccountCharge.getFeeInterval());

                    // get total withdrawals for period i.e if due date is 1st Feb and interval is 1 Month
                    // then we get charges from 1st Jan to 31st Jan [ 1 Month]
                    // we use date before due date so we can capture all withdrawals up to midnight the day
                    amountPercentAppliedTo = savingsAccount.getTotalWithdrawalsBetweenDatesInclusive(startDate, savingsAccountCharge.getDueLocalDate().minusDays(1));

                    savingsAccountCharge.setAmountPercentageAppliedTo(amountPercentAppliedTo);
                    savingsAccountCharge.setAmount(savingsAccountCharge.percentageOf(amountPercentAppliedTo, savingsAccountCharge.getPercentage()));
                    savingsAccountCharge.setAmountOutstanding(savingsAccountCharge.calculateOutstanding());
                }
            }

        }

        if ( savingsAccountCharge.getDueLocalDate() != null) {
            final Locale locale = command.extractLocale();
            final String format = command.dateFormat();
            final DateTimeFormatter fmt = StringUtils.isNotBlank(format) ? DateTimeFormat.forPattern(format).withLocale(locale)
                    : DateTimeFormat.forPattern("dd MM yyyy");

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
    public CommandProcessingResult waiveCharge(final Long savingsAccountId, final Long savingsAccountChargeId) {

        AppUser user = getAppUserIfPresent();

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        final SavingsAccountCharge savingsAccountCharge = this.savingsAccountChargeRepository
                .findOneWithNotFoundDetection(savingsAccountChargeId, savingsAccountId);

        // Get Savings account from savings charge
        final SavingsAccount account = savingsAccountCharge.savingsAccount();
        this.savingAccountAssembler.assignSavingAccountHelpers(account);

        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);

        account.waiveCharge(savingsAccountChargeId, user);
        boolean isInterestTransfer = false;
        LocalDate postInterestOnDate = null;
        final MathContext mc = MathContext.DECIMAL64;
        if (account.isBeforeLastAccrualPostingPeriod(savingsAccountCharge.getDueLocalDate())) {
            account.postAccrualInterest(mc, DateUtils.getLocalDateOfTenant(), isInterestTransfer,
                    isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, postInterestOnDate, null);
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
        // postJournalEntries(account, existingTransactionIds,
        // existingReversedTransactionIds);
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
            @SuppressWarnings("unused") final JsonCommand command) {
        this.context.authenticatedUser();

        final SavingsAccount savingsAccount = this.savingAccountAssembler.assembleFrom(savingsAccountId);
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
    public CommandProcessingResult payCharge(final Long savingsAccountId, final Long savingsAccountChargeId, final JsonCommand command) {

        AppUser user = getAppUserIfPresent();

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

        this.payCharge(savingsAccountCharge, transactionDate, amountPaid, fmt, user);
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
    public void applyChargeDue(final Long savingsAccountChargeId, final Long accountId) {
        // always use current date as transaction date for batch job
        AppUser user = null;

        final LocalDate transactionDate = DateUtils.getLocalDateOfTenant();
        final SavingsAccountCharge savingsAccountCharge = this.savingsAccountChargeRepository
                .findOneWithNotFoundDetection(savingsAccountChargeId, accountId);

        final DateTimeFormatter fmt = DateTimeFormat.forPattern("dd MM yyyy");
        fmt.withZone(DateUtils.getDateTimeZoneOfTenant());

        while (transactionDate.isAfter(savingsAccountCharge.getDueLocalDate()) && savingsAccountCharge.isNotFullyPaid()) {
            payCharge(savingsAccountCharge, transactionDate, savingsAccountCharge.amoutOutstanding(), fmt, user);
        }

    }

    @Transactional
    @Override
    public void payCharge(final SavingsAccountCharge savingsAccountCharge, final LocalDate transactionDate, final BigDecimal amountPaid,
            final DateTimeFormatter formatter, final AppUser user) {

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

        // Get Savings account from savings charge
        final SavingsAccount account = savingsAccountCharge.savingsAccount();
        this.savingAccountAssembler.assignSavingAccountHelpers(account);
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
        account.payCharge(savingsAccountCharge, amountPaid, transactionDate, formatter, user);
        boolean isInterestTransfer = false;
        LocalDate postInterestOnDate = null;
        final MathContext mc = MathContext.DECIMAL64;
        if (account.isBeforeLastAccrualPostingPeriod(transactionDate)) {
            account.postAccrualInterest(mc, DateUtils.getLocalDateOfTenant(), isInterestTransfer,
                    isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, postInterestOnDate, null);
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

    private void updateExistingTransactionsDetails(SavingsAccount account, Set<Long> existingTransactionIds,
            Set<Long> existingReversedTransactionIds) {
        existingTransactionIds.addAll(account.findExistingTransactionIds());
        existingReversedTransactionIds.addAll(account.findExistingReversedTransactionIds());
    }

    private void postJournalEntries(final SavingsAccount savingsAccount, final Set<Long> existingTransactionIds,
            final Set<Long> existingReversedTransactionIds) {

        final MonetaryCurrency currency = savingsAccount.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepositoryWrapper.findOneWithNotFoundDetection(currency);
        boolean isAccountTransfer = false;
        final Map<String, Object> accountingBridgeData = savingsAccount.deriveAccountingBridgeData(applicationCurrency.toData(),
                existingTransactionIds, existingReversedTransactionIds);
        this.journalEntryWritePlatformService.createJournalEntriesForSavings(accountingBridgeData, null, null);
    }

    @Override
    public CommandProcessingResult inactivateCharge(final Long savingsAccountId, final Long savingsAccountChargeId) {

        this.context.authenticatedUser();

        final SavingsAccountCharge savingsAccountCharge = this.savingsAccountChargeRepository
                .findOneWithNotFoundDetection(savingsAccountChargeId, savingsAccountId);

        final SavingsAccount account = savingsAccountCharge.savingsAccount();
        this.savingAccountAssembler.assignSavingAccountHelpers(account);

        final LocalDate inactivationOnDate = DateUtils.getLocalDateOfTenant();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SAVINGS_ACCOUNT_CHARGE_RESOURCE_NAME);

        /***
         * Only recurring fees are allowed to inactivate (For VFD, allow
         * deactivating non recurring charges)
         */
        if (!savingsAccountCharge.isRecurringFee()) {

            account.inactivateCharge(savingsAccountCharge, inactivationOnDate);
        } else {
            // handle recurring charges here
            final LocalDate nextDueDate = savingsAccountCharge.getNextDueDateFrom(inactivationOnDate);

            if (savingsAccountCharge.isChargeIsDue(nextDueDate)) {
                baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode("inactivation.of.charge.not.allowed.when.charge.is.due");
                if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
            } else if (savingsAccountCharge.isChargeIsOverPaid(nextDueDate)) {

                final List<SavingsAccountTransaction> chargePayments = new ArrayList<>();
                SavingsAccountCharge updatedCharge = savingsAccountCharge;
                do {
                    chargePayments.clear();
                    for (SavingsAccountTransaction transaction : account.getTransactions()) {
                        if (transaction.isPayCharge() && transaction.isNotReversed()
                                && transaction.isPaymentForCurrentCharge(savingsAccountCharge)) {
                            chargePayments.add(transaction);
                        }
                    }
                    /***
                     * Reverse the excess payments of charge transactions
                     */
                    SavingsAccountTransaction lastChargePayment = getLastChargePayment(chargePayments);
                    this.undoTransaction(savingsAccountCharge.savingsAccount().getId(), lastChargePayment.getId(), false);
                    updatedCharge = account.getUpdatedChargeDetails(savingsAccountCharge);
                } while (updatedCharge.isChargeIsOverPaid(nextDueDate));
            }
            account.inactivateCharge(savingsAccountCharge, inactivationOnDate);
        }

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsAccountCharge.getId()) //
                .withOfficeId(savingsAccountCharge.savingsAccount().officeId()) //
                .withClientId(savingsAccountCharge.savingsAccount().clientId()) //
                .withGroupId(savingsAccountCharge.savingsAccount().groupId()) //
                .withSavingsId(savingsAccountCharge.savingsAccount().getId()) //
                .build();
    }

    private SavingsAccountTransaction getLastChargePayment(final List<SavingsAccountTransaction> chargePayments) {
        if (!CollectionUtils.isEmpty(chargePayments)) { return chargePayments.get(chargePayments.size() - 1); }
        return null;
    }

    @Transactional
    @Override
    public CommandProcessingResult assignFieldOfficer(Long savingsAccountId, JsonCommand command) {
        this.context.authenticatedUser();
        final Map<String, Object> actualChanges = new LinkedHashMap<>(5);

        Staff fromSavingsOfficer = null;
        Staff toSavingsOfficer = null;
        this.fromApiJsonDeserializer.validateForAssignSavingsOfficer(command.json());

        final SavingsAccount savingsForUpdate = this.savingAccountRepositoryWrapper.findOneWithNotFoundDetection(savingsAccountId);
        final Long fromSavingsOfficerId = command.longValueOfParameterNamed("fromSavingsOfficerId");
        final Long toSavingsOfficerId = command.longValueOfParameterNamed("toSavingsOfficerId");
        final LocalDate dateOfSavingsOfficerAssignment = command.localDateValueOfParameterNamed("assignmentDate");

        if (fromSavingsOfficerId != null) {
            fromSavingsOfficer = this.staffRepository.findByOfficeHierarchyWithNotFoundDetection(fromSavingsOfficerId,
                    savingsForUpdate.office().getHierarchy());
        }
        if (toSavingsOfficerId != null) {
            toSavingsOfficer = this.staffRepository.findByOfficeHierarchyWithNotFoundDetection(toSavingsOfficerId,
                    savingsForUpdate.office().getHierarchy());
            actualChanges.put("toSavingsOfficerId", toSavingsOfficer.getId());
        }
        if (!savingsForUpdate.hasSavingsOfficer(fromSavingsOfficer)) {
            throw new SavingsOfficerAssignmentException(savingsAccountId, fromSavingsOfficerId);
        }

        savingsForUpdate.reassignSavingsOfficer(toSavingsOfficer, dateOfSavingsOfficerAssignment);

        this.savingAccountRepositoryWrapper.saveAndFlush(savingsForUpdate);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withOfficeId(savingsForUpdate.officeId()) //
                .withEntityId(savingsForUpdate.getId()) //
                .withSavingsId(savingsAccountId) //
                .with(actualChanges) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult unassignFieldOfficer(Long savingsAccountId, JsonCommand command) {

        this.context.authenticatedUser();

        final Map<String, Object> actualChanges = new LinkedHashMap<>(5);
        this.fromApiJsonDeserializer.validateForUnAssignSavingsOfficer(command.json());

        final SavingsAccount savingsForUpdate = this.savingAccountRepositoryWrapper.findOneWithNotFoundDetection(savingsAccountId);
        if (savingsForUpdate.getSavingsOfficer() == null) { throw new SavingsOfficerUnassignmentException(savingsAccountId); }

        final LocalDate dateOfSavingsOfficerUnassigned = command.localDateValueOfParameterNamed("unassignedDate");

        savingsForUpdate.removeSavingsOfficer(dateOfSavingsOfficerUnassigned);

        this.savingAccountRepositoryWrapper.saveAndFlush(savingsForUpdate);

        actualChanges.put("toSavingsOfficerId", null);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withOfficeId(savingsForUpdate.officeId()) //
                .withEntityId(savingsForUpdate.getId()) //
                .withSavingsId(savingsAccountId) //
                .with(actualChanges) //
                .build();
    }

    @Override
    public CommandProcessingResult modifyWithHoldTax(Long savingsAccountId, JsonCommand command) {
        final Map<String, Object> actualChanges = new HashMap<>(1);
        final SavingsAccount savingsForUpdate = this.savingAccountRepositoryWrapper.findOneWithNotFoundDetection(savingsAccountId);
        if (command.isChangeInBooleanParameterNamed(withHoldTaxParamName, savingsForUpdate.withHoldTax())) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(withHoldTaxParamName);
            actualChanges.put(withHoldTaxParamName, newValue);
            savingsForUpdate.setWithHoldTax(newValue);
            if (savingsForUpdate.getTaxGroup() == null) {
                final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
                final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("account");
                baseDataValidator.reset().parameter(withHoldTaxParamName).failWithCode("not.supported");
                throw new PlatformApiDataValidationException(dataValidationErrors);
            }
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(savingsAccountId) //
                .withSavingsId(savingsAccountId) //
                .with(actualChanges) //
                .build();
    }

    @Override
    @Transactional
    public void setSubStatusInactive(Long savingsId) {
        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
        account.setSubStatusInactive(appuserRepository.fetchSystemUser());
        this.savingAccountRepositoryWrapper.saveAndFlush(account);
        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
    }

    @Override
    @Transactional
    public void setSubStatusDormant(Long savingsId) {
        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        account.setSubStatusDormant();
        this.savingAccountRepositoryWrapper.save(account);
    }

    @Override
    @Transactional
    public void escheat(Long savingsId) {
        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        final Set<Long> existingTransactionIds = new HashSet<>();
        final Set<Long> existingReversedTransactionIds = new HashSet<>();
        updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
        account.escheat(appuserRepository.fetchSystemUser());
        this.savingAccountRepositoryWrapper.saveAndFlush(account);
        postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
    }

    private AppUser getAppUserIfPresent() {
        AppUser user = null;
        if (this.context != null) {
            user = this.context.getAuthenticatedUserIfPresent();
        }
        return user;
    }

    /**
     * Disable all standing instructions linked to the savings account if the
     * status is "closed"
     *
     * @param savingsAccount
     *            -- the savings account object
     */
    @Transactional
    private void disableStandingInstructionsLinkedToClosedSavings(final SavingsAccount savingsAccount) {
        if (savingsAccount != null && savingsAccount.isClosed()) {
            final Integer standingInstructionStatus = StandingInstructionStatus.ACTIVE.getValue();
            final Collection<AccountTransferStandingInstruction> accountTransferStandingInstructions = this.standingInstructionRepository
                    .findBySavingsAccountAndStatus(savingsAccount, standingInstructionStatus);

            if (!accountTransferStandingInstructions.isEmpty()) {
                for (AccountTransferStandingInstruction accountTransferStandingInstruction : accountTransferStandingInstructions) {
                    accountTransferStandingInstruction.updateStatus(StandingInstructionStatus.DISABLED.getValue());
                    this.standingInstructionRepository.save(accountTransferStandingInstruction);
                }
            }
        }
    }

    private Map<BUSINESS_ENTITY, Object> constructEntityMap(final BUSINESS_ENTITY entityEvent, Object entity) {
        Map<BUSINESS_ENTITY, Object> map = new HashMap<>(1);
        map.put(entityEvent, entity);
        return map;
    }

    @Override
    public CommandProcessingResult blockAccount(final Long savingsId) {

        this.context.authenticatedUser();

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);

        final Map<String, Object> changes = account.block();
        if (!changes.isEmpty()) {

            this.savingAccountRepositoryWrapper.save(account);
        }
        return new CommandProcessingResultBuilder().withEntityId(savingsId).withOfficeId(account.officeId())
                .withClientId(account.clientId()).withGroupId(account.groupId()).withSavingsId(savingsId).with(changes).build();
    }

    @Override
    public CommandProcessingResult unblockAccount(final Long savingsId) {
        this.context.authenticatedUser();

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);

        final Map<String, Object> changes = account.unblock();
        if (!changes.isEmpty()) {

            this.savingAccountRepositoryWrapper.save(account);
        }
        return new CommandProcessingResultBuilder().withEntityId(savingsId).withOfficeId(account.officeId())
                .withClientId(account.clientId()).withGroupId(account.groupId()).withSavingsId(savingsId).with(changes).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult holdAmount(final Long savingsId, final JsonCommand command) {

        final AppUser submittedBy = this.context.authenticatedUser();
        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);

        SavingsAccountTransaction transacton = this.savingsAccountTransactionDataValidator.validateHoldAndAssembleForm(command.json(),
                account, submittedBy);

        this.savingsAccountTransactionRepository.save(transacton);
        this.savingAccountRepositoryWrapper.saveAndFlush(account);

        return new CommandProcessingResultBuilder().withEntityId(transacton.getId()).withOfficeId(account.officeId())
                .withClientId(account.clientId()).withGroupId(account.groupId()).withSavingsId(savingsId).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult releaseAmount(final Long savingsId, final Long savingsTransactionId) {

        final AppUser submittedBy = this.context.authenticatedUser();
        SavingsAccountTransaction holdTransaction = this.savingsAccountTransactionRepository
                .findOneByIdAndSavingsAccountId(savingsTransactionId, savingsId);

        final SavingsAccountTransaction transaction = this.savingsAccountTransactionDataValidator
                .validateReleaseAmountAndAssembleForm(holdTransaction, submittedBy);
        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);
        account.releaseAmount(transaction.getAmount());

        this.savingsAccountTransactionRepository.save(transaction);
        holdTransaction.updateReleaseId(transaction.getId());
        this.savingAccountRepositoryWrapper.save(account);

        return new CommandProcessingResultBuilder().withEntityId(transaction.getId()).withOfficeId(account.officeId())
                .withClientId(account.clientId()).withGroupId(account.groupId()).withSavingsId(account.getId()).build();
    }

    @Override
    public CommandProcessingResult blockCredits(final Long savingsId, final JsonCommand command) {
        this.context.authenticatedUser();

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);

        final Long narrationId = command.longValueOfParameterNamed("narrationId");
        
        final CodeValue codeValue =  narrationId != null ? codeValueRepositoryWrapper.findOneWithNotFoundDetection(narrationId) : null;
        
        final Map<String, Object> changes = account.blockCredits(account.getSubStatus(),codeValue);
        if (!changes.isEmpty()) {

            this.savingAccountRepositoryWrapper.save(account);
        }
        return new CommandProcessingResultBuilder().withEntityId(savingsId).withOfficeId(account.officeId())
                .withClientId(account.clientId()).withGroupId(account.groupId()).withSavingsId(savingsId).with(changes).build();
    }

    @Override
    public CommandProcessingResult unblockCredits(final Long savingsId, final JsonCommand command) {
        this.context.authenticatedUser();

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);

        final Long narrationId = command.longValueOfParameterNamed("narrationId");
        final CodeValue codeValue =  narrationId != null ? codeValueRepositoryWrapper.findOneWithNotFoundDetection(narrationId) : null;
        
        final Map<String, Object> changes = account.unblockCredits(codeValue);
        if (!changes.isEmpty()) {

            this.savingAccountRepositoryWrapper.save(account);
        }
        return new CommandProcessingResultBuilder().withEntityId(savingsId).withOfficeId(account.officeId())
                .withClientId(account.clientId()).withGroupId(account.groupId()).withSavingsId(savingsId).with(changes).build();
    }

    @Override
    public CommandProcessingResult blockDebits(final Long savingsId, final JsonCommand command) {
        this.context.authenticatedUser();

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);

        final Long narrationId = command.longValueOfParameterNamed("narrationId");
        
        final CodeValue codeValue =  narrationId != null ? codeValueRepositoryWrapper.findOneWithNotFoundDetection(narrationId) : null;
        
        final Map<String, Object> changes = account.blockDebits(account.getSubStatus(), codeValue);
        if (!changes.isEmpty()) {

            this.savingAccountRepositoryWrapper.save(account);
        }
        return new CommandProcessingResultBuilder().withEntityId(savingsId).withOfficeId(account.officeId())
                .withClientId(account.clientId()).withGroupId(account.groupId()).withSavingsId(savingsId).with(changes).build();
    }

    @Override
    public CommandProcessingResult unblockDebits(final Long savingsId, final JsonCommand command) {
        this.context.authenticatedUser();

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);
        
        final Long narrationId = command.longValueOfParameterNamed("narrationId");
        
        final CodeValue codeValue =  narrationId != null ? codeValueRepositoryWrapper.findOneWithNotFoundDetection(narrationId) : null;

        final Map<String, Object> changes = account.unblockDebits(codeValue);
        if (!changes.isEmpty()) {

            this.savingAccountRepositoryWrapper.save(account);
        }
        return new CommandProcessingResultBuilder().withEntityId(savingsId).withOfficeId(account.officeId())
                .withClientId(account.clientId()).withGroupId(account.groupId()).withSavingsId(savingsId).with(changes).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult applyOverdraft(Long savingsAccountId, JsonCommand command) {

        this.context.authenticatedUser();
        this.fromApiJsonDeserializer.validateApplyOverdraftParams(command.json());
        validateOverdraftInputDates(command);
        final SavingsAccount savingsForUpdate = this.savingAccountRepositoryWrapper.findOneWithNotFoundDetection(savingsAccountId);

        final Boolean allowOverdraft = command.booleanObjectValueOfParameterNamed("allowOverdraft");
        final BigDecimal overdraftLimit = command.bigDecimalValueOfParameterNamed("overdraftLimit");
        final BigDecimal nominalAnnualInterestRateOverdraft = command.bigDecimalValueOfParameterNamed("nominalAnnualInterestRateOverdraft");
        final BigDecimal minOverdraftForInterestCalculation = command.bigDecimalValueOfParameterNamed("minOverdraftForInterestCalculation");
        final LocalDate overdraftStartedOnDate = command.localDateValueOfParameterNamed("overdraftStartedOnDate");
        final LocalDate overdraftClosedOnDate = command.localDateValueOfParameterNamed("overdraftClosedOnDate");
        if (!allowOverdraft) {
            savingsForUpdate.validateAccountBalanceDoesNotBecomeNegativeAtTheTimeOfDisableOverdraft(BigDecimal.ZERO,
                    SavingsApiConstants.allowOverdraftParamName);
        }
        final Map<String, Object> actualChanges = savingsForUpdate.applyOverdraft(allowOverdraft, overdraftLimit,
                nominalAnnualInterestRateOverdraft, minOverdraftForInterestCalculation, overdraftStartedOnDate, overdraftClosedOnDate);

        this.savingAccountRepositoryWrapper.saveAndFlush(savingsForUpdate);

        return new CommandProcessingResultBuilder().withEntityId(savingsForUpdate.getId()) //
                .withOfficeId(savingsForUpdate.officeId()) //
                .withClientId(savingsForUpdate.clientId()) //
                .withGroupId(savingsForUpdate.groupId()) //
                .withSavingsId(savingsAccountId) //
                .with(actualChanges) //
                .build();
    }

    @Transactional
    @Override
    public void startOrCloseSavingsAccountOverdraft(List<SavingsAccount> savingsAccountList, Boolean start) {
        savingsAccountList.forEach(savingAccount -> {
            if (start) {
                savingAccount.startOverdraft();
            } else {
                savingAccount.closeOverdraft();
            }
            this.savingAccountRepositoryWrapper.saveAndFlush(savingAccount);
        });
    }

    private void validateOverdraftInputDates(final JsonCommand command) {
        final LocalDate startDate = command.localDateValueOfParameterNamed("overdraftStartedOnDate");
        final LocalDate closeDate = command.localDateValueOfParameterNamed("overdraftClosedOnDate");
        if (startDate != null && closeDate != null) {
            if (closeDate.isBefore(startDate)) { throw new SavingsAccountDateException(startDate.toString(), closeDate.toString()); }
        }
    }

    @Override
    public CommandProcessingResult postAccrualInterest(final JsonCommand command) {

        Long savingsId = command.getSavingsId();
        final boolean postInterestAs = command.booleanPrimitiveValueOfParameterNamed("isPostInterestAsOn");
        final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);
        if (postInterestAs == true) {

            if (transactionDate == null) {

                throw new PostInterestAsOnDateException(PostInterestAsOnException_TYPE.VALID_DATE);
            }
            if (transactionDate.isBefore(account.accountSubmittedOrActivationDate())) {
                throw new PostInterestAsOnDateException(PostInterestAsOnException_TYPE.ACTIVATION_DATE);
            }

            LocalDate today = DateUtils.getLocalDateOfTenant();
            if (transactionDate.isAfter(today)) { throw new PostInterestAsOnDateException(PostInterestAsOnException_TYPE.FUTURE_DATE); }

        }

        postAccrualInterest(account, postInterestAs, transactionDate, true);

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .build();
    }

    @Override
    public void postAccrualInterest(final SavingsAccount account, final boolean postInterestAs, final LocalDate transactionDate,
            boolean isUserPosting) {
        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();
        if (account.getNominalAnnualInterestRate().compareTo(BigDecimal.ZERO) > 0
                || (account.getNominalAnnualInterestRateOverdraft() != null && account.getNominalAnnualInterestRateOverdraft().compareTo(BigDecimal.ZERO) > 0)) {
            final Set<Long> existingTransactionIds = new HashSet<>();
            final Set<Long> existingReversedTransactionIds = new HashSet<>();
            updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
            final LocalDate today = DateUtils.getLocalDateOfTenant();
            final MathContext mc = new MathContext(10, MoneyHelper.getRoundingMode());
            boolean isInterestTransfer = false;
            LocalDate postInterestOnDate = null;
            if (postInterestAs) {
                postInterestOnDate = transactionDate;
            }
            account.postAccrualInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                    financialYearBeginningMonth, postInterestOnDate, null);

            // for generating transaction id's
            List<SavingsAccountTransaction> transactions = account.getTransactions();
            for (SavingsAccountTransaction accountTransaction : transactions) {
                if (accountTransaction.getId() == null) {
                    this.savingsAccountTransactionRepository.save(accountTransaction);
                }
            }

            this.savingAccountRepositoryWrapper.saveAndFlush(account);

            postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        }

    }

    @Override
    public CommandProcessingResult postAccrualInterest(Long savingsId, LocalDate transactionDate, boolean isUserPosting) {

        final SavingsAccount account = this.savingAccountAssembler.assembleFrom(savingsId);
        checkClientOrGroupActive(account);

        if (transactionDate == null) {

            throw new PostInterestAsOnDateException(PostInterestAsOnException_TYPE.VALID_DATE);
        }
        if (transactionDate.isBefore(account.accountSubmittedOrActivationDate())) {
            throw new PostInterestAsOnDateException(PostInterestAsOnException_TYPE.ACTIVATION_DATE);
        }
        List<SavingsAccountTransaction> savingTransactions = account.getTransactions();
        for (SavingsAccountTransaction savingTransaction : savingTransactions) {
            if (transactionDate.toDate().before(savingTransaction.getDateOf())) {
                throw new PostInterestAsOnDateException(PostInterestAsOnException_TYPE.LAST_TRANSACTION_DATE);
            }
        }

        LocalDate today = DateUtils.getLocalDateOfTenant();
        if (transactionDate.isAfter(today)) { throw new PostInterestAsOnDateException(PostInterestAsOnException_TYPE.FUTURE_DATE); }

        postAccrualInterest(account, true, transactionDate, isUserPosting);

        return new CommandProcessingResultBuilder() //
                .withEntityId(savingsId) //
                .withOfficeId(account.officeId()) //
                .withClientId(account.clientId()) //
                .withGroupId(account.groupId()) //
                .withSavingsId(savingsId) //
                .build();

    }

    @Override
    public CommandProcessingResult modifyNickName(Long savingsAccountId, JsonCommand command) {

        final Map<String, Object> actualChanges = new HashMap<>(1);
        final SavingsAccount savingsForUpdate = this.savingAccountRepositoryWrapper.findOneWithNotFoundDetection(savingsAccountId);
        if (command.isChangeInStringParameterNamed(SavingsApiConstants.nicknameParamName, savingsForUpdate.getNickname())) {
            final String newValue = command.stringValueOfParameterNamed(SavingsApiConstants.nicknameParamName);
            actualChanges.put(SavingsApiConstants.nicknameParamName, newValue);
            savingsForUpdate.setNickname(newValue);
        }

        return new CommandProcessingResultBuilder()
                .withCommandId(command.commandId())
                .withEntityId(savingsAccountId)
                .withSavingsId(savingsAccountId)
                .with(actualChanges)
                .build();
    }

    @Override
    public CommandProcessingResult updateInterestRate(Long savingsAccountId, JsonCommand command) {

        final Map<String, Object> actualChanges = new HashMap<>(1);

        // Verify request
        this.fromApiJsonDeserializer.validateForUpdatingNominalInterestRate(command.json());
        final SavingsAccount savingsForUpdate = this.savingAccountAssembler.assembleFrom(savingsAccountId);
        checkClientOrGroupActive(savingsForUpdate);

        // Post Interest
        final LocalDate updateDate = command.localDateValueOfParameterNamed("updatedOnDate");

        this.postInterest(savingsForUpdate, true, updateDate, updateDate);

        // Update rate

        if (command.isChangeInBigDecimalParameterNamed(SavingsApiConstants.nominalAnnualInterestRateParamName,
                savingsForUpdate.getNominalAnnualInterestRate())) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(SavingsApiConstants.nominalAnnualInterestRateParamName);
            actualChanges.put(SavingsApiConstants.nominalAnnualInterestRateParamName, newValue);
            savingsForUpdate.setNominalAnnualInterestRate(newValue);
        }

        // Update start interest calculation date
        savingsForUpdate.setStartInterestCalculationDate(savingsForUpdate.retrieveLastTransactionDate().toDate());

        this.savingAccountRepositoryWrapper.saveAndFlush(savingsForUpdate);;

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(savingsAccountId) //
                .withSavingsId(savingsAccountId) //
                .with(actualChanges) //
                .build();
    }

    /* Post interest up to a specific date */
    private void postInterest(final SavingsAccount account, final boolean postInterestAs, final LocalDate transactionDate, final LocalDate interestPostingUpToDate) {

        final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                .isSavingsInterestPostingAtCurrentPeriodEnd();
        final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();
        if (account.getNominalAnnualInterestRate().compareTo(BigDecimal.ZERO) > 0
                || (account.allowOverdraft() && account.getNominalAnnualInterestRateOverdraft().compareTo(BigDecimal.ZERO) > 0)) {
            final Set<Long> existingTransactionIds = new HashSet<>();
            final Set<Long> existingReversedTransactionIds = new HashSet<>();
            updateExistingTransactionsDetails(account, existingTransactionIds, existingReversedTransactionIds);
            final LocalDate today = interestPostingUpToDate != null ? interestPostingUpToDate : DateUtils.getLocalDateOfTenant();
            final MathContext mc = new MathContext(10, MoneyHelper.getRoundingMode());
            boolean isInterestTransfer = false;
            LocalDate postInterestOnDate = null;
            if (postInterestAs) {
                postInterestOnDate = transactionDate;
            }
            account.postInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth,
                    postInterestOnDate);
            // for generating transaction id's
            List<SavingsAccountTransaction> transactions = account.getTransactions();
            for (SavingsAccountTransaction accountTransaction : transactions) {
                if (accountTransaction.getId() == null) {
                    this.savingsAccountTransactionRepository.save(accountTransaction);
                }
            }

            this.savingAccountRepositoryWrapper.saveAndFlush(account);

            postJournalEntries(account, existingTransactionIds, existingReversedTransactionIds);
        }
    }

    //
    private void handleMonthlyWithdrawCharges(SavingsAccountCharge savingsAccountCharge, SavingsAccount savingsAccount, Charge chargeDefinition,
                                              SavingsAccountChargeReq savingsAccountChargeReq){
        // calculate transaction amount
        BigDecimal transactionAmount = BigDecimal.ZERO;

        // generate the due date
        //  job will run on 1st of every month
        MonthDay feeOnMonthDay = savingsAccountChargeReq.getFeeOnMonthDay();
        feeOnMonthDay = (feeOnMonthDay == null) ? chargeDefinition.getFeeOnMonthDay() : feeOnMonthDay;
        LocalDate chargeDueDate = feeOnMonthDay.toLocalDate(LocalDate.now().getYear());

        // calculate the withdrawals
        BigDecimal totalWithdrawals = BigDecimal.ZERO;//savingsAccount.getTotalWithdrawalsInSameYearAndMonthAsDate( chargeDueDate );
        transactionAmount = totalWithdrawals == null ? new BigDecimal(0) : totalWithdrawals;

        // calculate transfers to loan accounts of same client


        // calculate the charges

        savingsAccountCharge.updateDueDate(chargeDueDate.toDate());
    }

    /**
     * Used to update the amount in monthly % charge on total withdrawals
     * This is used by the "Pay Due Savings Charges" JOB
     * Once previous charge is paid, we can update the charge at end of next period
     * */
    public void updateSavingsAccountCharge(final Long savingsAccountId, final Long savingsChargeId) {

        final SavingsAccount savingsAccount = this.savingAccountAssembler.assembleFrom(savingsAccountId);
        checkClientOrGroupActive(savingsAccount);

        final SavingsAccountCharge savingsAccountCharge = this.savingsAccountChargeRepository.findOneWithNotFoundDetection(savingsChargeId,
                savingsAccountId);

        // calculate transaction amount

        //  FIXME take care of m% charges in this case total withdrawals. We can look into how to make this more generic.
        // FIXME For now this works for CANARY case.
        if(ChargeCalculationType.fromInt(savingsAccountCharge.getChargeCalculation()).equals(ChargeCalculationType.PERCENT_OF_TOTAL_WITHDRAWALS)) {
            BigDecimal amountPercentAppliedTo = BigDecimal.ZERO;

            // Generate next due date
            if ( savingsAccountCharge.getDueLocalDate()!= null && savingsAccountCharge.getFeeInterval() != null) {
                //savingsAccountCharge.updateNextDueDateForRecurringFees();
                LocalDate nextDueDate = savingsAccountCharge.getDueLocalDate();
                //savingsAccountCharge.getDueLocalDate().plusMonths(savingsAccountCharge.getFeeInterval());
                LocalDate startDate = nextDueDate.minusMonths(savingsAccountCharge.getFeeInterval());


                // get total withdrawals for period i.e if due date is 1st Feb and interval is 1 Month
                // then we get charges from 1st Jan to 31st Jan [ 1 Month]
                // we use date before due date so we can capture all withdrawals up to midnight the day
                amountPercentAppliedTo = savingsAccount.getTotalWithdrawalsBetweenDatesInclusive(startDate, nextDueDate.minusDays(1));

                if(Money.of(savingsAccount.getCurrency(), amountPercentAppliedTo).isGreaterThanZero()) {
                    savingsAccountCharge.setAmountPercentageAppliedTo(amountPercentAppliedTo);
                    savingsAccountCharge.setAmount(savingsAccountCharge.percentageOf(amountPercentAppliedTo, savingsAccountCharge.getPercentage()));
                    savingsAccountCharge.resetPayProperties();
                    BigDecimal outstanding = savingsAccountCharge.calculateOutstanding();
                    savingsAccountCharge.setAmountOutstanding(outstanding);
                }
            }
        }

        this.savingsAccountChargeRepository.saveAndFlush(savingsAccountCharge);
    }
}
