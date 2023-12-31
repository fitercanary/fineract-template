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
package org.apache.fineract.accounting.provisioning.service;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.glaccount.domain.GLAccountRepository;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.accounting.producttoaccountmapping.domain.PortfolioProductType;
import org.apache.fineract.accounting.provisioning.data.ProductProvisioningEntryData;
import org.apache.fineract.accounting.provisioning.data.ProvisioningEntryData;
import org.apache.fineract.accounting.provisioning.domain.LoanProductProvisioningEntry;
import org.apache.fineract.accounting.provisioning.domain.ProvisioningEntry;
import org.apache.fineract.accounting.provisioning.domain.ProvisioningEntryRepository;
import org.apache.fineract.accounting.provisioning.domain.SavingsProductProvisioningEntry;
import org.apache.fineract.accounting.provisioning.exception.NoProvisioningCriteriaDefinitionFound;
import org.apache.fineract.accounting.provisioning.exception.ProvisioningEntryAlreadyCreatedException;
import org.apache.fineract.accounting.provisioning.exception.ProvisioningEntryNotfoundException;
import org.apache.fineract.accounting.provisioning.exception.ProvisioningJournalEntriesCannotbeCreatedException;
import org.apache.fineract.accounting.provisioning.serialization.ProvisioningEntriesDefinitionJsonDeserializer;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.provisioning.data.ProvisioningCriteriaData;
import org.apache.fineract.organisation.provisioning.domain.ProvisioningCategory;
import org.apache.fineract.organisation.provisioning.domain.ProvisioningCategoryRepository;
import org.apache.fineract.organisation.provisioning.service.ProvisioningCriteriaReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallmentRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsProduct;
import org.apache.fineract.portfolio.savings.domain.SavingsProductRepository;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;

@Service
public class ProvisioningEntriesWritePlatformServiceJpaRepositoryImpl implements ProvisioningEntriesWritePlatformService {

    private final ProvisioningEntriesReadPlatformService provisioningEntriesReadPlatformService;
    private final ProvisioningCriteriaReadPlatformService provisioningCriteriaReadPlatformService;
    private final LoanProductRepository loanProductRepository;
    private final LoanRepaymentScheduleInstallmentRepository repaymentScheduleRepository;
    private final LoanRepository loanRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsProductRepository savingsProductRepository;
    private final GLAccountRepository glAccountRepository;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final ProvisioningCategoryRepository provisioningCategoryRepository;
    private final PlatformSecurityContext platformSecurityContext;
    private final ProvisioningEntryRepository provisioningEntryRepository;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final ProvisioningEntriesDefinitionJsonDeserializer fromApiJsonDeserializer;
    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public ProvisioningEntriesWritePlatformServiceJpaRepositoryImpl(
            final ProvisioningEntriesReadPlatformService provisioningEntriesReadPlatformService,
            final ProvisioningCriteriaReadPlatformService provisioningCriteriaReadPlatformService,
            final LoanProductRepository loanProductRepository, LoanRepository loanRepository,
            SavingsAccountRepository savingsAccountRepository, SavingsProductRepository savingsProductRepository,
            final GLAccountRepository glAccountRepository, final OfficeRepositoryWrapper officeRepositoryWrapper,
            final ProvisioningCategoryRepository provisioningCategoryRepository, final PlatformSecurityContext platformSecurityContext,
            final ProvisioningEntryRepository provisioningEntryRepository,
            final JournalEntryWritePlatformService journalEntryWritePlatformService,
            final ProvisioningEntriesDefinitionJsonDeserializer fromApiJsonDeserializer, final FromJsonHelper fromApiJsonHelper,
            final LoanRepaymentScheduleInstallmentRepository repaymentScheduleRepository) {
        this.repaymentScheduleRepository = repaymentScheduleRepository;
        this.provisioningEntriesReadPlatformService = provisioningEntriesReadPlatformService;
        this.provisioningCriteriaReadPlatformService = provisioningCriteriaReadPlatformService;
        this.loanProductRepository = loanProductRepository;
        this.loanRepository = loanRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsProductRepository = savingsProductRepository;
        this.glAccountRepository = glAccountRepository;
        this.officeRepositoryWrapper = officeRepositoryWrapper;
        this.provisioningCategoryRepository = provisioningCategoryRepository;
        this.platformSecurityContext = platformSecurityContext;
        this.provisioningEntryRepository = provisioningEntryRepository;
        this.journalEntryWritePlatformService = journalEntryWritePlatformService;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    @Override
    public CommandProcessingResult createProvisioningJournalEntries(Long provisioningEntryId, JsonCommand command) {
        ProvisioningEntry requestedEntry = this.provisioningEntryRepository.findOne(provisioningEntryId);
        if (requestedEntry == null) { throw new ProvisioningEntryNotfoundException(provisioningEntryId); }

        ProvisioningEntryData existingProvisioningEntryData = this.provisioningEntriesReadPlatformService
                .retrieveExistingProvisioningIdDateWithJournals();
        revertAndAddJournalEntries(existingProvisioningEntryData, requestedEntry);
        revertAndAddJournalEntriesForSavingsProductProvisioning(existingProvisioningEntryData, requestedEntry);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(requestedEntry.getId()).build();
    }

    private void revertAndAddJournalEntries(ProvisioningEntryData existingEntryData, ProvisioningEntry requestedEntry) {
        if (existingEntryData != null) {
            validateForCreateJournalEntry(existingEntryData, requestedEntry);
            this.journalEntryWritePlatformService.revertProvisioningJournalEntries(requestedEntry.getCreatedDate(),
                    existingEntryData.getId(), PortfolioProductType.PROVISIONING.getValue());
        }
        if (requestedEntry.getLoanProductProvisioningEntries() == null || requestedEntry.getLoanProductProvisioningEntries().size() == 0) {
            requestedEntry.setJournalEntryCreated(Boolean.FALSE);
        } else {
            requestedEntry.setJournalEntryCreated(Boolean.TRUE);
        }

        this.provisioningEntryRepository.save(requestedEntry);
        this.journalEntryWritePlatformService.createProvisioningJournalEntries(requestedEntry);
    }

    private void revertAndAddJournalEntriesForSavingsProductProvisioning(ProvisioningEntryData existingEntryData,
            ProvisioningEntry requestedEntry) {
        if (existingEntryData != null) {
            validateForCreateJournalEntry(existingEntryData, requestedEntry);
            this.journalEntryWritePlatformService.revertProvisioningJournalEntries(requestedEntry.getCreatedDate(),
                    existingEntryData.getId(), PortfolioProductType.PROVISIONING.getValue());
        }
        if (requestedEntry.getSavingsProductProvisioningEntries() == null
                || requestedEntry.getSavingsProductProvisioningEntries().size() == 0) {
            requestedEntry.setJournalEntryCreated(Boolean.FALSE);
        } else {
            requestedEntry.setJournalEntryCreated(Boolean.TRUE);
        }

        this.provisioningEntryRepository.save(requestedEntry);
        this.journalEntryWritePlatformService.createSavingsProductProvisioningJournalEntries(requestedEntry);
    }

    private void validateForCreateJournalEntry(ProvisioningEntryData existingEntry, ProvisioningEntry requested) {
        Date existingDate = existingEntry.getCreatedDate();
        Date requestedDate = requested.getCreatedDate();
        if (existingDate.after(requestedDate) || existingDate.equals(requestedDate)) {
            throw new ProvisioningJournalEntriesCannotbeCreatedException(existingEntry.getCreatedDate(), requestedDate);
        }
    }

    private boolean isJournalEntriesRequired(JsonCommand command) {
        boolean bool = false;
        if (this.fromApiJsonHelper.parameterExists("createjournalentries", command.parsedJson())) {
            JsonObject jsonObject = command.parsedJson().getAsJsonObject();
            bool = jsonObject.get("createjournalentries").getAsBoolean();
        }
        return bool;
    }

    private Date parseDate(JsonCommand command) {
        LocalDate localDate = this.fromApiJsonHelper.extractLocalDateNamed("date", command.parsedJson());
        return localDate.toDate();
    }

    @Override
    @CronTarget(jobName = JobName.GENERATE_LOANLOSS_PROVISIONING)
    public void generateLoanLossProvisioningAmount() {
        Date currentDate = DateUtils.getLocalDateOfTenant().toDate();
        boolean addJournalEntries = true;
        try {
            Collection<ProvisioningCriteriaData> criteriaCollection = this.provisioningCriteriaReadPlatformService
                    .retrieveAllProvisioningCriterias();
            if (criteriaCollection == null || criteriaCollection.size() == 0) {
                return;
                // FIXME: Do we need to throw
                // NoProvisioningCriteriaDefinitionFound()?
            }
            createProvisioningEntry(currentDate, addJournalEntries);
        } catch (ProvisioningEntryAlreadyCreatedException peace) {} catch (DataIntegrityViolationException dive) {}
    }

    @Override
    public CommandProcessingResult createProvisioningEntries(JsonCommand command) {
        this.fromApiJsonDeserializer.validateForCreate(command.json());
        Date createdDate = parseDate(command);
        boolean addJournalEntries = isJournalEntriesRequired(command);
        try {
            Collection<ProvisioningCriteriaData> criteriaCollection = this.provisioningCriteriaReadPlatformService
                    .retrieveAllProvisioningCriterias();
            if (criteriaCollection == null || criteriaCollection.size() == 0) { throw new NoProvisioningCriteriaDefinitionFound(); }
            ProvisioningEntry requestedEntry = createProvisioningEntry(createdDate, addJournalEntries);
            return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(requestedEntry.getId()).build();
        } catch (DataIntegrityViolationException dve) {
            return CommandProcessingResult.empty();
        }
    }

    private ProvisioningEntry createProvisioningEntry(Date date, boolean addJournalEntries) {
        ProvisioningEntry existingEntry = this.provisioningEntryRepository.findByProvisioningEntryDate(date);
        if (existingEntry != null) {
            throw new ProvisioningEntryAlreadyCreatedException(existingEntry.getId(), existingEntry.getCreatedDate());
        }
        AppUser currentUser = this.platformSecurityContext.authenticatedUser();
        AppUser lastModifiedBy = null;
        Date lastModifiedDate = null;
        Set<LoanProductProvisioningEntry> nullEntries = null;
        ProvisioningEntry requestedEntry = new ProvisioningEntry(currentUser, date, lastModifiedBy, lastModifiedDate, nullEntries);
        Collection<LoanProductProvisioningEntry> entries = generateLoanProvisioningEntry(requestedEntry, date);
        Collection<SavingsProductProvisioningEntry> savingsProductProvisioningEntries = generateSavingsProvisioningEntry(requestedEntry,
                date);
        requestedEntry.setProvisioningEntries(entries);
        requestedEntry.setSavingsProductProvisioningEntries(savingsProductProvisioningEntries);
        if (addJournalEntries) {
            ProvisioningEntryData existingProvisioningEntryData = this.provisioningEntriesReadPlatformService
                    .retrieveExistingProvisioningIdDateWithJournals();
            revertAndAddJournalEntries(existingProvisioningEntryData, requestedEntry);
            revertAndAddJournalEntriesForSavingsProductProvisioning(existingProvisioningEntryData, requestedEntry);
        } else {
            this.provisioningEntryRepository.save(requestedEntry);
        }
        return requestedEntry;
    }

    @Override
    public CommandProcessingResult reCreateProvisioningEntries(Long provisioningEntryId, JsonCommand command) {
        ProvisioningEntry requestedEntry = this.provisioningEntryRepository.findOne(provisioningEntryId);
        if (requestedEntry == null) { throw new ProvisioningEntryNotfoundException(provisioningEntryId); }
        requestedEntry.getLoanProductProvisioningEntries().clear();
        this.provisioningEntryRepository.save(requestedEntry);
        Collection<LoanProductProvisioningEntry> entries = generateLoanProvisioningEntry(requestedEntry, requestedEntry.getCreatedDate());
        requestedEntry.setProvisioningEntries(entries);
        this.provisioningEntryRepository.save(requestedEntry);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(requestedEntry.getId()).build();
    }

    private Collection<LoanProductProvisioningEntry> generateLoanProvisioningEntry(ProvisioningEntry parent, Date date) {
        Collection<ProductProvisioningEntryData> entries = this.provisioningEntriesReadPlatformService
                .retrieveLoanProductsProvisioningData(date);
        Map<LoanProductProvisioningEntry, LoanProductProvisioningEntry> provisioningEntries = new HashMap<>();
        for (ProductProvisioningEntryData data : entries) {

            List<LoanRepaymentScheduleInstallment> installements = this.repaymentScheduleRepository.findByIdAndDueDate(data.getLoanId(), date);
            LoanProduct loanProduct = this.loanProductRepository.findOne(data.getProductId());
            Loan loan = this.loanRepository.findLoanByAccountNumber(data.getAccountNumber());
            Office office = this.officeRepositoryWrapper.findOneWithNotFoundDetection(data.getOfficeId());
            ProvisioningCategory provisioningCategory = provisioningCategoryRepository.findOne(data.getCategoryId());
            GLAccount liabilityAccount = glAccountRepository.findOne(data.getLiabilityAccount());
            GLAccount expenseAccount = glAccountRepository.findOne(data.getExpenseAccount());
            MonetaryCurrency currency = loanProduct.getPrincipalAmount().getCurrency();
            Long criteriaId = data.getCriteriaId();
            Money interestDue = Money.zero(currency);
            Money feeCharge = Money.zero(currency);
            Money penality = Money.zero(currency);
            Money interestCompleted = Money.zero(currency);
            Money amountToReserve = Money.zero(currency);
            Money feeChargePaid = Money.zero(currency);
            Money penalityChargePaid = Money.zero(currency);
            if (installements.size() > 0) {
                for (LoanRepaymentScheduleInstallment installment : installements) {
                    feeCharge = installment.getFeeChargesCharged(currency) != null ? installment.getFeeChargesCharged(currency)
                            : Money.zero(currency);
                    penality = installment.getPenaltyChargesCharged(currency) != null ? installment.getPenaltyChargesCharged(currency)
                            : Money.zero(currency);

                    feeChargePaid = installment.getFeeChargesPaid(currency) != null ? installment.getFeeChargesPaid(currency)
                            : Money.zero(currency);

                    penalityChargePaid = installment.getPenaltyChargesPaid(currency) != null ? installment.getPenaltyChargesPaid(currency)
                            : Money.zero(currency);

                    penality = installment.getPenaltyChargesCharged(currency) != null ? installment.getPenaltyChargesCharged(currency)
                            : Money.zero(currency);

                    interestCompleted = installment.getInterestPaid(currency) != null ? installment.getInterestPaid(currency)
                            : Money.zero(currency);

                    Money interestOfSingleInstallment = installment.getInterestCharged(currency) != null
                            ? installment.getInterestCharged(currency)
                            : Money.zero(currency);
                            
                    if (feeChargePaid.isNotEqualTo(Money.zero(currency)) || penalityChargePaid.isNotEqualTo(Money.zero(currency))) {
                        interestCompleted = interestCompleted.plus(penalityChargePaid).plus(feeChargePaid);
                    }
                    if (interestCompleted.isNotEqualTo(Money.zero(currency))) {
                        if (penality.isNotEqualTo(Money.zero(currency))) {
                            interestCompleted = interestCompleted.minus(penality);
                        }
                        if (feeCharge.isNotEqualTo(Money.zero(currency))) {
                            interestCompleted = interestCompleted.minus(feeCharge);
                        }
                    }
                    if (interestCompleted.isGreaterThan(Money.zero(currency))) {
                        interestOfSingleInstallment = interestOfSingleInstallment.minus(interestCompleted);
                    }
                    interestDue = interestDue.plus(interestOfSingleInstallment);

                }
                Money money = Money.of(currency, data.getOutstandingBalance()).plus(interestDue);
                amountToReserve = money.percentageOf(data.getPercentage(), MoneyHelper.getRoundingMode());
            } else {
                Money money = Money.of(currency, data.getOutstandingBalance());
                amountToReserve = money.percentageOf(data.getPercentage(), MoneyHelper.getRoundingMode());
            }

            LoanProductProvisioningEntry entry = new LoanProductProvisioningEntry(loanProduct, office, data.getCurrencyCode(),
                    provisioningCategory, data.getOverdueInDays(), amountToReserve.getAmount(), liabilityAccount, expenseAccount,
                    criteriaId, loan);
            entry.setProvisioningEntry(parent);
            if (!provisioningEntries.containsKey(entry)) {
                provisioningEntries.put(entry, entry);
            } else {
                LoanProductProvisioningEntry entry1 = provisioningEntries.get(entry);
                entry1.addReservedAmount(entry.getReservedAmount());
            }
        }
        return provisioningEntries.values();
    }

    private Collection<SavingsProductProvisioningEntry> generateSavingsProvisioningEntry(ProvisioningEntry parent, Date date) {
        Collection<ProductProvisioningEntryData> entries = this.provisioningEntriesReadPlatformService
                .retrieveSavingsProductsProvisioningData(date);
        Map<SavingsProductProvisioningEntry, SavingsProductProvisioningEntry> provisioningEntries = new HashMap<>();
        for (ProductProvisioningEntryData data : entries) {
            SavingsProduct savingsProduct = this.savingsProductRepository.findOne(data.getProductId());
            SavingsAccount savingsAccount = this.savingsAccountRepository.findByAccountNumber(data.getAccountNumber());
            Office office = this.officeRepositoryWrapper.findOneWithNotFoundDetection(data.getOfficeId());
            ProvisioningCategory provisioningCategory = provisioningCategoryRepository.findOne(data.getCategoryId());
            GLAccount liabilityAccount = glAccountRepository.findOne(data.getLiabilityAccount());
            GLAccount expenseAccount = glAccountRepository.findOne(data.getExpenseAccount());
            MonetaryCurrency currency = savingsProduct.getCurrency();
            Money money = Money.of(currency, data.getOutstandingBalance());
            Money amountToReserve = money.percentageOf(data.getPercentage(), MoneyHelper.getRoundingMode());
            Long criteriaId = data.getCriteriaId();
            SavingsProductProvisioningEntry entry = new SavingsProductProvisioningEntry(savingsProduct, office, data.getCurrencyCode(),
                    provisioningCategory, data.getOverdueInDays(), amountToReserve.getAmount(), liabilityAccount, expenseAccount,
                    criteriaId, savingsAccount);
            entry.setProvisioningEntry(parent);
            if (!provisioningEntries.containsKey(entry)) {
                provisioningEntries.put(entry, entry);
            } else {
                SavingsProductProvisioningEntry entry1 = provisioningEntries.get(entry);
                entry1.addReservedAmount(entry.getReservedAmount());
            }
        }
        return provisioningEntries.values();
    }
}
