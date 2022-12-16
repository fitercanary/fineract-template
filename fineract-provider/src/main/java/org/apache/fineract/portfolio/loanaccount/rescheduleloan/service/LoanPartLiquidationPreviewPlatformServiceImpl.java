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
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
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
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.LoanRestructureRequestDataValidator;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequest;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequestRepository;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequestRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.exception.LoanRescheduleRequestNotFoundException;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanaccount.service.LoanUtilService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.MultivaluedMap;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LoanPartLiquidationPreviewPlatformServiceImpl implements LoanPartLiquidationPreviewPlatformService {

    private final LoanRescheduleRequestRepositoryWrapper loanRescheduleRequestRepository;
    private final LoanUtilService loanUtilService;
    private final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory;
    private final LoanScheduleGeneratorFactory loanScheduleFactory;
    private final LoanSummaryWrapper loanSummaryWrapper;
    private final LoanAssembler loanAssembler;
    private final LoanPartLiquidatonDataValidator loanPartLiquidatonDataValidator;
    private FromJsonHelper fromJsonHelper;
    private final DefaultScheduledDateGenerator scheduledDateGenerator = new DefaultScheduledDateGenerator();
    private final PlatformSecurityContext platformSecurityContext;


    @Autowired
    public LoanPartLiquidationPreviewPlatformServiceImpl(final LoanRescheduleRequestRepositoryWrapper loanRescheduleRequestRepository,
                                                         final LoanUtilService loanUtilService,final LoanAssembler loanAssembler,
                                                         final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory,
                                                         final LoanScheduleGeneratorFactory loanScheduleFactory, final LoanSummaryWrapper loanSummaryWrapper,
                                                         final LoanPartLiquidatonDataValidator loanPartLiquidatonDataValidator,final PlatformSecurityContext platformSecurityContext) {
        this.loanRescheduleRequestRepository = loanRescheduleRequestRepository;
        this.loanUtilService = loanUtilService;
        this.loanRepaymentScheduleTransactionProcessorFactory = loanRepaymentScheduleTransactionProcessorFactory;
        this.loanScheduleFactory = loanScheduleFactory;
        this.loanSummaryWrapper = loanSummaryWrapper;
        this.loanAssembler = loanAssembler;
        this.loanPartLiquidatonDataValidator = loanPartLiquidatonDataValidator;
        this.platformSecurityContext = platformSecurityContext;
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
        final LocalDate newStartDate = LocalDate.parse(queryParameters.getFirst(RestructureLoansApiConstants.rescheduleFromDateParamName),formatter);

        LocalDate expectedMaturityDate = LocalDate.parse(queryParameters.getFirst(RestructureLoansApiConstants.expectedMaturityDateParamName),formatter);


        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildRestructureScheduleGeneratorDTO(loan,
                newStartDate, expectedMaturityDate);

        LocalDate rescheduleFromDate = newStartDate;
        List<LoanTermVariationsData> removeLoanTermVariationsData = new ArrayList<>();
        final LoanApplicationTerms loanApplicationTerms = loan.constructPartLiquidationTerms(scheduleGeneratorDTO, transactionAmount);
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
        final LoanScheduleDTO loanSchedule = loanScheduleGenerator.rescheduleNextInstallmentsRestructure(mathContext, loanApplicationTerms,
                loan, loanApplicationTerms.getHolidayDetailDTO(),
                loanRepaymentScheduleTransactionProcessor, rescheduleFromDate, expectedMaturityDate, transactionAmount);
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
        throw new PlatformDataIntegrityException("error.msg.problem.generating.new.schedule",
                "Error while generating new schedule");
//
//        try {
//
//            Loan loan = this.loanAssembler.assembleFrom(jsonCommand.getLoanId());
//            // parameter
//            this.loanPartLiquidatonDataValidator.validateForPartLiquidateAction(jsonCommand, loan);
//
//            final AppUser appUser = this.platformSecurityContext.authenticatedUser();
//            final Map<String, Object> changes = new LinkedHashMap<>();
//
//            LocalDate approvedOnDate = jsonCommand.localDateValueOfParameterNamed("approvedOnDate");
//            final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(jsonCommand.dateFormat()).withLocale(
//                    jsonCommand.extractLocale());
//
//            changes.put("locale", jsonCommand.locale());
//            changes.put("dateFormat", jsonCommand.dateFormat());
//            changes.put("approvedOnDate", approvedOnDate.toString(dateTimeFormatter));
//            changes.put("approvedByUserId", appUser.getId());
//
//            final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
//            final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());
//
//            ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildRestructureScheduleGeneratorDTO(loan,
//                    loanRescheduleRequest.getRescheduleFromDate(), loanRescheduleRequest.getRescheduleToDate());
//
//            List<LoanRepaymentScheduleInstallment> repaymentScheduleInstallments = loan.getRepaymentScheduleInstallments();
//            Collection<LoanTransactionToRepaymentScheduleMapping> scheduleMappings = new HashSet<>();
////            for (LoanRepaymentScheduleInstallment installment: repaymentScheduleInstallments){
////                List<LoanTransactionToRepaymentScheduleMapping> installmentMappings =
////                        this.loanTransactionToRepaymentScheduleMappingRepository.
////                                findAllWithInstallmentNumber(
////                                        installment);
////                LoanTransactionToRepaymentScheduleMapping.updateMappingsList(scheduleMappings, installmentMappings);
////            }
//
//            List<LoanTransaction> loanTransactions = loan.getLoanTransactions();
//            for (LoanTransaction transaction : loanTransactions) {
//                LoanTransactionToRepaymentScheduleMapping.updateMappingsList(scheduleMappings, transaction, loan.getCurrency());
//                transaction.getLoanTransactionToRepaymentScheduleMappings();
//                this.loanTransactionRepository.saveAndFlush(transaction);
//            }
//
//
//            Collection<LoanRepaymentScheduleHistory> loanRepaymentScheduleHistoryList = this.loanScheduleHistoryWritePlatformService
//                    .createLoanScheduleArchive(repaymentScheduleInstallments, loan, loanRescheduleRequest);
//
//
//            //create the loan schedule transactions schedule mapping history
////            List<LoanTransaction> originalLoanTransactions = loan.getLoanTransactions();
//
//            final LoanApplicationTerms loanApplicationTerms = loan.constructLoanRestructureTerms(scheduleGeneratorDTO);
//
//            LocalDate rescheduleFromDate = null;
//            Set<LoanTermVariations> activeLoanTermVariations = loan.getActiveLoanTermVariations();
//            LoanTermVariations dueDateVariationInCurrentRequest = loanRescheduleRequest.getDueDateTermVariationIfExists();
//            if (dueDateVariationInCurrentRequest != null && activeLoanTermVariations != null) {
//                LocalDate fromScheduleDate = dueDateVariationInCurrentRequest.fetchTermApplicaDate();
//                LocalDate currentScheduleDate = fromScheduleDate;
//                LocalDate modifiedScheduleDate = dueDateVariationInCurrentRequest.fetchDateValue();
//                Map<LocalDate, LocalDate> changeMap = new HashMap<>();
//                changeMap.put(currentScheduleDate, modifiedScheduleDate);
//                for (LoanTermVariations activeLoanTermVariation : activeLoanTermVariations) {
//                    if (activeLoanTermVariation.getTermType().isDueDateVariation()
//                            && activeLoanTermVariation.fetchDateValue().equals(dueDateVariationInCurrentRequest.fetchTermApplicaDate())) {
//                        activeLoanTermVariation.markAsInactive();
//                        rescheduleFromDate = activeLoanTermVariation.fetchTermApplicaDate();
//                        dueDateVariationInCurrentRequest.setTermApplicableFrom(rescheduleFromDate.toDate());
//                    } else if (!activeLoanTermVariation.fetchTermApplicaDate().isBefore(fromScheduleDate)) {
//                        while (currentScheduleDate.isBefore(activeLoanTermVariation.fetchTermApplicaDate())) {
//                            currentScheduleDate = this.scheduledDateGenerator.generateNextRepaymentDate(currentScheduleDate,
//                                    loanApplicationTerms, false);
//                            modifiedScheduleDate = this.scheduledDateGenerator.generateNextRepaymentDate(modifiedScheduleDate,
//                                    loanApplicationTerms, false);
//                            changeMap.put(currentScheduleDate, modifiedScheduleDate);
//                        }
//                        if (changeMap.containsKey(activeLoanTermVariation.fetchTermApplicaDate())) {
//                            activeLoanTermVariation.setTermApplicableFrom(changeMap.get(activeLoanTermVariation.fetchTermApplicaDate())
//                                    .toDate());
//                        }
//                    }
//                }
//            }
//            if (rescheduleFromDate == null) {
//                rescheduleFromDate = loanRescheduleRequest.getRescheduleFromDate();
//            }
//            for (LoanRescheduleRequestToTermVariationMapping mapping : loanRescheduleRequest
//                    .getLoanRescheduleRequestToTermVariationMappings()) {
//                mapping.getLoanTermVariations().updateIsActive(true);
//            }
//            BigDecimal annualNominalInterestRate = null;
//            List<LoanTermVariationsData> loanTermVariations = new ArrayList<>();
//            loan.constructLoanTermVariations(scheduleGeneratorDTO.getFloatingRateDTO(), annualNominalInterestRate, loanTermVariations);
//            loanApplicationTerms.getLoanTermVariations().setExceptionData(loanTermVariations);
//
//            List<LoanTermVariationsData> loanTermVariationsData = new ArrayList<>();
//            LocalDate adjustedApplicableDate = null;
//
//            Set<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings = loanRescheduleRequest.getLoanRescheduleRequestToTermVariationMappings();
//            if (!loanRescheduleRequestToTermVariationMappings.isEmpty()) {
//                for (LoanRescheduleRequestToTermVariationMapping loanRescheduleRequestToTermVariationMapping : loanRescheduleRequestToTermVariationMappings) {
//                    if (loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().getTermType().isDueDateVariation()
//                            && rescheduleFromDate != null) {
//                        adjustedApplicableDate = loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().fetchDateValue();
//                        loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().setTermApplicableFrom(
//                                rescheduleFromDate.toDate());
//                    }
//                    loanTermVariationsData.add(loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().toData());
//                }
//            }
//
//            for (LoanTermVariationsData loanTermVariation : loanApplicationTerms.getLoanTermVariations().getDueDateVariation()) {
//                if (rescheduleFromDate.isBefore(loanTermVariation.getTermApplicableFrom())) {
//                    LocalDate applicableDate = this.scheduledDateGenerator.generateNextRepaymentDate(rescheduleFromDate, loanApplicationTerms,
//                            false);
//                    if (loanTermVariation.getTermApplicableFrom().equals(applicableDate)) {
//                        LocalDate adjustedDate = this.scheduledDateGenerator.generateNextRepaymentDate(adjustedApplicableDate,
//                                loanApplicationTerms, false);
//                        loanTermVariation.setApplicableFromDate(adjustedDate);
//                        loanTermVariationsData.add(loanTermVariation);
//                    }
//                }
//            }
//
//            loanApplicationTerms.getLoanTermVariations().updateLoanTermVariationsData(loanTermVariationsData);
//            final RoundingMode roundingMode = MoneyHelper.getRoundingMode();
//            final MathContext mathContext = new MathContext(8, roundingMode);
//            final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.loanRepaymentScheduleTransactionProcessorFactory
//                    .determineProcessor(loan.transactionProcessingStrategy());
//            final LoanScheduleGenerator loanScheduleGenerator = this.loanScheduleFactory.create(loanApplicationTerms.getInterestMethod());
//            final LoanLifecycleStateMachine loanLifecycleStateMachine = null;
//
//            loan.setHelpers(loanLifecycleStateMachine, this.loanSummaryWrapper, this.loanRepaymentScheduleTransactionProcessorFactory);
//
//            final LoanScheduleDTO loanSchedule = loanScheduleGenerator.rescheduleNextInstallmentsRestructure(
//                    mathContext,
//                    loanApplicationTerms,
//                    loan,
//                    loanApplicationTerms.getHolidayDetailDTO(),
//                    loanRepaymentScheduleTransactionProcessor,
//                    rescheduleFromDate,
//                    loanRescheduleRequest.getRescheduleToDate(),null
//            );
//
//
//            loan.updateLoanSchedule(loanSchedule.getInstallments(), appUser);
//
////            loan.recalculateAllCharges();
//            ChangedTransactionDetail changedTransactionDetail = loan.processTransactions();
//
//            for (LoanRepaymentScheduleHistory loanRepaymentScheduleHistory : loanRepaymentScheduleHistoryList) {
//                this.loanRepaymentScheduleHistoryRepository.save(loanRepaymentScheduleHistory);
//            }
////            for (LoanTransaction transaction :
////                    originalLoanTransactions) {
////                Set<LoanTransactionToRepaymentScheduleMappingHistory> mappingHistories = new HashSet<>();
////                LoanTransactionToRepaymentScheduleMappingHistory.archiveExistingMappings(mappingHistories,transaction,loanRepaymentScheduleHistoryList);
////                transaction.getLoanTransactionToRepaymentScheduleMappings().clear();
////                transaction.updateLoanTransactionToRepaymentScheduleMappingsHistory(mappingHistories);
////                this.loanTransactionRepository.saveAndFlush(transaction);
////            }
//
//
//            loan.updateRescheduledByUser(appUser);
//            loan.updateRescheduledOnDate(new LocalDate());
//
//            // update the status of the request
//            loanRescheduleRequest.approve(appUser, approvedOnDate);
//            //update installment numbers.
//            for (int i = 0; i < loan.getRepaymentScheduleInstallments().size(); i++) {
//                loan.getRepaymentScheduleInstallments().get(i).updateInstallmentNumber(i + 1);
//            }
//
//            //clear off the first installment
////            LoanRepaymentScheduleInstallment installment = loan.getRepaymentScheduleInstallments().get(0);
////
////            loan.getRepaymentScheduleInstallments().get(0).updatePrincipalCompleted(
////                    installment.getPrincipal(loan.getCurrency()).getAmount()
////            );
////            loan.getRepaymentScheduleInstallments().get(0).updateInterestCompleted(
////                    installment.getInterestCharged(loan.getCurrency()).getAmount()
////            );
////
////            loan.getRepaymentScheduleInstallments().get(0).updateInterestCompleted(
////                    installment.getInterestCharged(loan.getCurrency()).getAmount()
////            );
////            loan.getRepaymentScheduleInstallments().get(0).updateChargesPaid(
////                    installment.getFeeChargesCharged(loan.getCurrency()).getAmount()
////            );
////            loan.getRepaymentScheduleInstallments().get(0).updatePenaltiesPaid(
////                    installment.getPenaltyChargesCharged(loan.getCurrency()).getAmount()
////            );
////
////            loan.getRepaymentScheduleInstallments().get(0).updateObligationMet(true);
////            //TODO -to be changed to use make loan payment method to create all necessary transactions on the laon
////
////            loan.getRepaymentScheduleInstallments().get(0).writeOffOutstandingPenaltyCharges(approvedOnDate, loan.getCurrency());
////            loan.getRepaymentScheduleInstallments().get(0).writeOffOutstandingInterest(approvedOnDate, loan.getCurrency());
////            loan.getRepaymentScheduleInstallments().get(0).writeOffOutstandingFeeCharges(approvedOnDate, loan.getCurrency());
////            loan.getRepaymentScheduleInstallments().get(0).writeOffOutstandingPrincipal(approvedOnDate, loan.getCurrency());
//
//            // update the loan object
//            saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
//
//            if (changedTransactionDetail != null) {
//                for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
//                    this.loanTransactionRepository.save(mapEntry.getValue());
//                    // update loan with references to the newly created
//                    // transactions
//                    loan.addLoanTransaction(mapEntry.getValue());
//                    this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
//                }
//            }
//            postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
//
//            this.loanAccountDomainService.recalculateAccruals(loan, true);
//
//            return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).withEntityId(loanRescheduleRequestId)
//                    .withLoanId(loanRescheduleRequest.getLoan().getId()).with(changes).build();
//        } catch (final DataIntegrityViolationException dve) {
//            // handle the data integrity violation
//            handleDataIntegrityViolation(dve);
//
//            // return an empty command processing result object
//            return CommandProcessingResult.empty();
//        }
    }

}
