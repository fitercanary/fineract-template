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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.service;

import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.account.service.AccountTransfersWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.ChangedTransactionDetail;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanAccountDomainService;
import org.apache.fineract.portfolio.loanaccount.domain.LoanLifecycleStateMachine;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallmentRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleTransactionProcessorFactory;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRescheduleRequestToTermVariationMapping;
import org.apache.fineract.portfolio.loanaccount.domain.LoanSummaryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTermVariations;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionToRepaymentScheduleMapping;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.LoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleDTO;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.DefaultScheduledDateGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanApplicationTerms;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanRepaymentScheduleHistory;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanRepaymentScheduleHistoryRepository;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGeneratorFactory;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModel;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.RestructureLoansApiConstants;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.LoanPartLiquidatonDataValidator;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanaccount.service.LoanUtilService;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.paymentdetail.service.PaymentDetailWritePlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.MultivaluedMap;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LoanPartLiquidationPreviewPlatformServiceImpl implements LoanPartLiquidationPreviewPlatformService {
    private final static Logger logger = LoggerFactory.getLogger(LoanRestructureRequestWritePlatformServiceImpl.class);

    private final LoanUtilService loanUtilService;
    private final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory;
    private final LoanScheduleGeneratorFactory loanScheduleFactory;
    private final LoanSummaryWrapper loanSummaryWrapper;
    private final LoanAssembler loanAssembler;
    private final LoanPartLiquidatonDataValidator loanPartLiquidatonDataValidator;
    private FromJsonHelper fromJsonHelper;
    private final DefaultScheduledDateGenerator scheduledDateGenerator = new DefaultScheduledDateGenerator();
    private final PlatformSecurityContext platformSecurityContext;
    private final LoanTransactionRepository loanTransactionRepository;
    private final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService;
    private final LoanRepaymentScheduleHistoryRepository loanRepaymentScheduleHistoryRepository;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private final LoanAccountDomainService loanAccountDomainService;
    private final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository;
    private final PaymentDetailWritePlatformService paymentDetailWritePlatformService;


    @Autowired
    public LoanPartLiquidationPreviewPlatformServiceImpl(final LoanUtilService loanUtilService,final LoanAssembler loanAssembler,
                                                         final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory,
                                                         final LoanScheduleGeneratorFactory loanScheduleFactory, final LoanSummaryWrapper loanSummaryWrapper,
                                                         final LoanPartLiquidatonDataValidator loanPartLiquidatonDataValidator,
                                                         final PlatformSecurityContext platformSecurityContext,final LoanRepositoryWrapper loanRepositoryWrapper,
                                                         final LoanTransactionRepository loanTransactionRepository,
                                                         final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService,
                                                         final LoanRepaymentScheduleHistoryRepository loanRepaymentScheduleHistoryRepository,
                                                         final JournalEntryWritePlatformService journalEntryWritePlatformService,
                                                         final AccountTransfersWritePlatformService accountTransfersWritePlatformService,
                                                         final LoanAccountDomainService loanAccountDomainService,
                                                         final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository,
                                                         final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository,
                                                         final PaymentDetailWritePlatformService paymentDetailWritePlatformService) {
        this.loanUtilService = loanUtilService;
        this.loanRepaymentScheduleTransactionProcessorFactory = loanRepaymentScheduleTransactionProcessorFactory;
        this.loanScheduleFactory = loanScheduleFactory;
        this.loanSummaryWrapper = loanSummaryWrapper;
        this.loanAssembler = loanAssembler;
        this.loanPartLiquidatonDataValidator = loanPartLiquidatonDataValidator;
        this.platformSecurityContext = platformSecurityContext;
        this.loanTransactionRepository = loanTransactionRepository;
        this.loanScheduleHistoryWritePlatformService = loanScheduleHistoryWritePlatformService;
        this.loanRepaymentScheduleHistoryRepository = loanRepaymentScheduleHistoryRepository;
        this.journalEntryWritePlatformService = journalEntryWritePlatformService;
        this.accountTransfersWritePlatformService = accountTransfersWritePlatformService;
        this.loanAccountDomainService = loanAccountDomainService;
        this.repaymentScheduleInstallmentRepository = repaymentScheduleInstallmentRepository;
        this.loanRepositoryWrapper = loanRepositoryWrapper;
        this.applicationCurrencyRepository = applicationCurrencyRepository;
        this.paymentDetailWritePlatformService = paymentDetailWritePlatformService;
    }

    @Override
    public LoanScheduleModel previewLoanPartLiquidation(Long loanId, MultivaluedMap<String, String> queryParameters) {

        // use the loan id to get a Loan entity object
        final Loan loan = this.loanAssembler.assembleFrom(loanId);

        this.loanPartLiquidatonDataValidator.validateForCreateAction(queryParameters,loan);

        // use the reschedule reason code value id to get a CodeValue entity
        // object
//        final CodeValue rescheduleReasonCodeValue = this.codeValueRepositoryWrapper.findOneWithNotFoundDetection(rescheduleReasonId);

        DateTimeFormatter formatter = DateTimeFormat.forPattern(queryParameters.getFirst("dateFormat"));

        //new start Date
        final Money transactionAmount = Money.of(loan.getCurrency(),new BigDecimal(queryParameters.getFirst(RestructureLoansApiConstants.transactionAmountParam)));

//        loan.getLoanSummary().getTotalPrincipalOutstanding();
        final LocalDate nextStartDate = LocalDate.parse(queryParameters.getFirst(RestructureLoansApiConstants.rescheduleFromDateParamName),formatter);

        final LocalDate nextPaymentDate = nextStartDate.minusMonths(1);

        LocalDate expectedMaturityDate = LocalDate.parse(queryParameters.getFirst(RestructureLoansApiConstants.expectedMaturityDateParamName),formatter);


        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildRestructureScheduleGeneratorDTO(loan,
                nextPaymentDate, expectedMaturityDate);

        LocalDate rescheduleFromDate = nextPaymentDate;
        List<LoanTermVariationsData> removeLoanTermVariationsData = new ArrayList<>();

        final LoanApplicationTerms loanApplicationTerms = loan.constructLoanRestructureTerms(scheduleGeneratorDTO);

        LoanRepaymentScheduleInstallment repaymentScheduleInstallment = loan.getRepaymentScheduleInstallment(nextStartDate);
        if (repaymentScheduleInstallment ==null){
            throw new PlatformDataIntegrityException("error.msg.loan.schedule.date.existing.installment",
                    "No installment on the next start date selected. Please select the date of an existing installment");
        }


        if (nextStartDate.isBefore(loan.getLastUserTransactionDate())) {
            throw new PlatformDataIntegrityException("error.msg.loan.start.date.cannot.be.in.before.the.last.transaction",
                    "The new start date cannot be before the last transaction date: ");
        }

        LoanTermVariations dueDateVariationInCurrentRequest = null;
        if(dueDateVariationInCurrentRequest != null){
            for (LoanTermVariationsData loanTermVariation : loanApplicationTerms.getLoanTermVariations().getDueDateVariation()) {
                if (loanTermVariation.getDateValue().equals(dueDateVariationInCurrentRequest.fetchTermApplicaDate())) {
                    rescheduleFromDate = loanTermVariation.getTermApplicableFrom();
                    removeLoanTermVariationsData.add(loanTermVariation);
                }
            }
        }
        loanApplicationTerms.getLoanTermVariations().getDueDateVariation().removeAll(removeLoanTermVariationsData);

        List<LoanTermVariationsData> loanTermVariationsData = new ArrayList<>();
        LocalDate adjustedApplicableDate = null;
        Set<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings = new HashSet<>();
        if (!loanRescheduleRequestToTermVariationMappings.isEmpty()) {
            for (LoanRescheduleRequestToTermVariationMapping loanRescheduleRequestToTermVariationMapping : loanRescheduleRequestToTermVariationMappings) {
                if (loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().getTermType().isDueDateVariation()
                        && rescheduleFromDate != null) {
                    adjustedApplicableDate = loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().fetchDateValue();
                    loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().setTermApplicableFrom(
                            rescheduleFromDate.toDate());
                }
                loanTermVariationsData.add(loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().toData());
            }
        }

        for (LoanTermVariationsData loanTermVariation : loanApplicationTerms.getLoanTermVariations().getDueDateVariation()) {
            if (rescheduleFromDate.isBefore(loanTermVariation.getTermApplicableFrom())) {
                LocalDate applicableDate = this.scheduledDateGenerator.generateNextRepaymentDate(rescheduleFromDate, loanApplicationTerms,
                        false);
                if (loanTermVariation.getTermApplicableFrom().equals(applicableDate)) {
                    LocalDate adjustedDate = this.scheduledDateGenerator.generateNextRepaymentDate(adjustedApplicableDate,
                            loanApplicationTerms, false);
                    loanTermVariation.setApplicableFromDate(adjustedDate);
                    loanTermVariationsData.add(loanTermVariation);
                }
            }
        }

        loanApplicationTerms.getLoanTermVariations().updateLoanTermVariationsData(loanTermVariationsData);
        final RoundingMode roundingMode = MoneyHelper.getRoundingMode();
        final MathContext mathContext = new MathContext(8, roundingMode);
        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.loanRepaymentScheduleTransactionProcessorFactory
                .determineProcessor(loan.transactionProcessingStrategy());
        final LoanScheduleGenerator loanScheduleGenerator = this.loanScheduleFactory.create(loanApplicationTerms.getInterestMethod());
        final LoanLifecycleStateMachine loanLifecycleStateMachine = null;
        loan.setHelpers(loanLifecycleStateMachine, this.loanSummaryWrapper, this.loanRepaymentScheduleTransactionProcessorFactory);

        final LoanScheduleDTO loanSchedule = loanScheduleGenerator.rescheduleNextInstallmentsRestructure(
                mathContext, loanApplicationTerms,
                loan, loanApplicationTerms.getHolidayDetailDTO(),
                loanRepaymentScheduleTransactionProcessor, rescheduleFromDate,
                expectedMaturityDate, transactionAmount);

        final LoanScheduleModel loanScheduleModel = loanSchedule.getLoanScheduleModel();
        LoanScheduleModel loanScheduleModels = LoanScheduleModel.withPartLiquidationModelPeriods(loanScheduleModel.getPeriods(),
                loanScheduleModel);

        return loanScheduleModels;
    }

    @Override
    public CommandProcessingResult applyLoanPartLiquidation(JsonCommand jsonCommand) {
        return null;
    }

    @Override
    public CommandProcessingResult confirmPartLiquidation(JsonCommand jsonCommand) {

        try {

            Loan loan = this.loanAssembler.assembleFrom(jsonCommand.getLoanId());
            // parameter
            this.loanPartLiquidatonDataValidator.validateForPartLiquidateAction(jsonCommand, loan);

            final AppUser appUser = this.platformSecurityContext.authenticatedUser();
            final Map<String, Object> changes = new LinkedHashMap<>();

            changes.put("locale", jsonCommand.locale());
            changes.put("dateFormat", jsonCommand.dateFormat());
            changes.put("approvedByUserId", appUser.getId());

            final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
            final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());

            LocalDate expectedMaturityDate = jsonCommand.localDateValueOfParameterNamed(RestructureLoansApiConstants.expectedMaturityDateParamName);
            final LocalDate nextStartDate = jsonCommand.localDateValueOfParameterNamed(RestructureLoansApiConstants.rescheduleFromDateParamName);

            final LocalDate nextPaymentDate = nextStartDate.minusMonths(1);
            ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildRestructureScheduleGeneratorDTO(loan,
                    nextPaymentDate, expectedMaturityDate);

            BigDecimal amountBigDecimal = jsonCommand.bigDecimalValueOfParameterNamed(RestructureLoansApiConstants.transactionAmountParam);
            final Money transactionAmount = Money.of(loan.getCurrency(),amountBigDecimal);

            List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments = loan.getRepaymentScheduleInstallments();

            Collection<LoanRepaymentScheduleHistory> loanRepaymentScheduleHistoryList = this.loanScheduleHistoryWritePlatformService
                    .createLoanScheduleArchive(repaymentScheduleInstallments, loan, null);

            final LoanApplicationTerms loanApplicationTerms = loan.constructLoanRestructureTerms(scheduleGeneratorDTO);

            LocalDate rescheduleFromDate = null;

            if (rescheduleFromDate == null) {
                rescheduleFromDate = nextPaymentDate;
            }

            BigDecimal annualNominalInterestRate = null;
            List<LoanTermVariationsData> loanTermVariations = new ArrayList<>();
            loan.constructLoanTermVariations(scheduleGeneratorDTO.getFloatingRateDTO(), annualNominalInterestRate, loanTermVariations);
            loanApplicationTerms.getLoanTermVariations().setExceptionData(loanTermVariations);

            List<LoanTermVariationsData> loanTermVariationsData = new ArrayList<>();
            LocalDate adjustedApplicableDate = null;

            for (LoanTermVariationsData loanTermVariation : loanApplicationTerms.getLoanTermVariations().getDueDateVariation()) {
                if (rescheduleFromDate.isBefore(loanTermVariation.getTermApplicableFrom())) {
                    LocalDate applicableDate = this.scheduledDateGenerator.generateNextRepaymentDate(rescheduleFromDate, loanApplicationTerms,
                            false);
                    if (loanTermVariation.getTermApplicableFrom().equals(applicableDate)) {
                        LocalDate adjustedDate = this.scheduledDateGenerator.generateNextRepaymentDate(adjustedApplicableDate,
                                loanApplicationTerms, false);
                        loanTermVariation.setApplicableFromDate(adjustedDate);
                        loanTermVariationsData.add(loanTermVariation);
                    }
                }
            }

            loanApplicationTerms.getLoanTermVariations().updateLoanTermVariationsData(loanTermVariationsData);
            final RoundingMode roundingMode = MoneyHelper.getRoundingMode();
            final MathContext mathContext = new MathContext(8, roundingMode);
            final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.loanRepaymentScheduleTransactionProcessorFactory
                    .determineProcessor(loan.transactionProcessingStrategy());
            final LoanScheduleGenerator loanScheduleGenerator = this.loanScheduleFactory.create(loanApplicationTerms.getInterestMethod());
            final LoanLifecycleStateMachine loanLifecycleStateMachine = null;

            loan.setHelpers(loanLifecycleStateMachine, this.loanSummaryWrapper, this.loanRepaymentScheduleTransactionProcessorFactory);


            final LoanScheduleDTO loanSchedule = loanScheduleGenerator.rescheduleNextInstallmentsRestructure(
                    mathContext, loanApplicationTerms,
                    loan, loanApplicationTerms.getHolidayDetailDTO(),
                    loanRepaymentScheduleTransactionProcessor, rescheduleFromDate,
                    expectedMaturityDate, transactionAmount);


            loan.updateLoanSchedule(loanSchedule.getInstallments(), appUser);

//            loan.recalculateAllCharges();

            ChangedTransactionDetail changedTransactionDetail = loan.processPartLiquidationTransactions();

            loan.getLoanRepaymentScheduleDetail().setPrincipal(loanApplicationTerms.getPrincipal().getAmount());
            for (LoanRepaymentScheduleHistory loanRepaymentScheduleHistory : loanRepaymentScheduleHistoryList) {
                this.loanRepaymentScheduleHistoryRepository.save(loanRepaymentScheduleHistory);
            }

            loan.updateRescheduledByUser(appUser);
            loan.updateRescheduledOnDate(new LocalDate());


            for (int i = 0; i < loan.getRepaymentScheduleInstallments().size(); i++) {
                loan.getRepaymentScheduleInstallments().get(i).updateInstallmentNumber(i + 1);
            }

            loan.updateLoanSummaryDerivedFields();
            loanApplicationTerms.updatePricipal(loanApplicationTerms.getApprovedPrincipal());

            // update the loan object
            saveAndFlushLoanWithDataIntegrityViolationChecks(loan);



            if (changedTransactionDetail != null) {
                for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                    this.loanTransactionRepository.save(mapEntry.getValue());
                    // update loan with references to the newly created
                    // transactions
                    loan.addLoanTransaction(mapEntry.getValue());
                    this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
                }
            }

            postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
            CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder();

            final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(jsonCommand, changes);

            String noteText = jsonCommand.stringValueOfParameterNamed(RestructureLoansApiConstants.rescheduleReasonCommentParamName);


            this.loanAccountDomainService.makeRepayment(loan, commandProcessingResultBuilder,
                    DateUtils.getLocalDateOfTenant(), transactionAmount.getAmount(), paymentDetail,
                    noteText, null, false, false, scheduleGeneratorDTO.getHolidayDetailDTO(),
                    scheduleGeneratorDTO.getHolidayDetailDTO().isHolidayEnabled());

            this.loanAccountDomainService.recalculateAccruals(loan, true);

            return commandProcessingResultBuilder.withCommandId(jsonCommand.commandId()).withLoanId(loan.getId())
                    .with(changes).build();
        } catch (final DataIntegrityViolationException dve) {
            // handle the data integrity violation
            handleDataIntegrityViolation(dve);

            // return an empty command processing result object
            return CommandProcessingResult.empty();
        }
    }

    private void saveAndFlushLoanWithDataIntegrityViolationChecks(final Loan loan) {
        try {
            List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
            for (LoanRepaymentScheduleInstallment installment : installments) {
                if (installment.getId() == null) {
                    this.repaymentScheduleInstallmentRepository.save(installment);
                }
            }
            this.loanRepositoryWrapper.saveAndFlush(loan);
        } catch (final DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.transaction");
            if (realCause.getMessage().toLowerCase().contains("external_id_unique")) {
                baseDataValidator.reset().parameter("externalId").failWithCode("value.must.be.unique");
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
                        "Validation errors exist.", dataValidationErrors);
            }
        }
    }

    private void postJournalEntries(Loan loan, List<Long> existingTransactionIds, List<Long> existingReversedTransactionIds) {
        final MonetaryCurrency currency = loan.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);
        final Map<String, Object> accountingBridgeData = loan.deriveAccountingBridgeData(applicationCurrency.toData(),
                existingTransactionIds, existingReversedTransactionIds);
        this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData);
    }

    /**
     * handles the data integrity violation exception for loan reschedule write
     * services
     *
     * @param dve
     *            data integrity violation exception
     *
     **/
    private void handleDataIntegrityViolation(final DataIntegrityViolationException dve) {

        logger.error(dve.getMessage(), dve);

        throw new PlatformDataIntegrityException("error.msg.loan.reschedule.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }


}
