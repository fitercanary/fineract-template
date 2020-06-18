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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.UnsupportedParameterException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepositoryWrapper;
import org.apache.fineract.portfolio.account.service.AccountTransfersReadPlatformService;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.collectionsheet.CollectionSheetConstants;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.domain.GroupRepositoryWrapper;
import org.apache.fineract.portfolio.group.exception.CenterNotActiveException;
import org.apache.fineract.portfolio.group.exception.ClientNotInGroupException;
import org.apache.fineract.portfolio.group.exception.GroupNotActiveException;
import org.apache.fineract.portfolio.interestratechart.domain.InterestRateChart;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetailAssembler;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.SavingsCompoundingInterestPeriodType;
import org.apache.fineract.portfolio.savings.SavingsInterestCalculationDaysInYearType;
import org.apache.fineract.portfolio.savings.SavingsInterestCalculationType;
import org.apache.fineract.portfolio.savings.SavingsPeriodFrequencyType;
import org.apache.fineract.portfolio.savings.SavingsPostingInterestPeriodType;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionDTO;
import org.apache.fineract.portfolio.savings.request.FixedDepositApplicationReq;
import org.apache.fineract.portfolio.savings.request.RecurringAccountDetailReq;
import org.apache.fineract.useradministration.domain.AppUser;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import static org.apache.fineract.portfolio.collectionsheet.CollectionSheetConstants.bulkSavingsDueTransactionsParamName;
import static org.apache.fineract.portfolio.collectionsheet.CollectionSheetConstants.savingsIdParamName;
import static org.apache.fineract.portfolio.collectionsheet.CollectionSheetConstants.transactionAmountParamName;
import static org.apache.fineract.portfolio.collectionsheet.CollectionSheetConstants.transactionDateParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.withHoldTaxParamName;

@Service
public class DepositAccountAssembler {

    private final PlatformSecurityContext context;
    private final SavingsAccountTransactionSummaryWrapper savingsAccountTransactionSummaryWrapper;
    private final SavingsHelper savingsHelper;
    private final ClientRepositoryWrapper clientRepository;
    private final GroupRepositoryWrapper groupRepository;
    private final StaffRepositoryWrapper staffRepository;
    private final SavingsAccountRepositoryWrapper savingsAccountRepository;
    private final FromJsonHelper fromApiJsonHelper;
    private final DepositProductAssembler depositProductAssembler;
    private final PaymentDetailAssembler paymentDetailAssembler;

    @Autowired
    public DepositAccountAssembler(final SavingsAccountTransactionSummaryWrapper savingsAccountTransactionSummaryWrapper,
            final ClientRepositoryWrapper clientRepository, final GroupRepositoryWrapper groupRepository,
            final StaffRepositoryWrapper staffRepository,
            final SavingsAccountRepositoryWrapper savingsAccountRepository, final FromJsonHelper fromApiJsonHelper,
            final DepositProductAssembler depositProductAssembler,
            final AccountTransfersReadPlatformService accountTransfersReadPlatformService, final PlatformSecurityContext context,
            final PaymentDetailAssembler paymentDetailAssembler) {

        this.savingsAccountTransactionSummaryWrapper = savingsAccountTransactionSummaryWrapper;
        this.clientRepository = clientRepository;
        this.groupRepository = groupRepository;
        this.staffRepository = staffRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.depositProductAssembler = depositProductAssembler;
        this.savingsHelper = new SavingsHelper(accountTransfersReadPlatformService);
        this.context = context;
        this.paymentDetailAssembler = paymentDetailAssembler;
    }

    /**
     * Assembles a new {@link SavingsAccount} from JSON details passed in
     * request inheriting details where relevant from chosen
     * {@link SavingsProduct}.
     */
    public SavingsAccount assembleFrom(FixedDepositApplicationReq fixedDepositApplicationReq, SavingsProduct product, DepositAccountType depositAccountType) {

        final AppUser submittedBy = this.context.authenticatedUser();
        final String accountNo = fixedDepositApplicationReq.getAccountNo();
        final String externalId = fixedDepositApplicationReq.getExternalId();
        final String nickname = fixedDepositApplicationReq.getNickname();

        Client client = null;
        Group group = null;
        Staff fieldOfficer = null;
        AccountType accountType = AccountType.INVALID;
        final Long clientId = fixedDepositApplicationReq.getClientId();
        if (clientId != null) {
            final boolean isCalendarInherited = fixedDepositApplicationReq.isCalendarInherited();
            client = this.clientRepository.findOneWithNotFoundDetection(clientId, isCalendarInherited); //we need group collection if isCalendarInherited is true
            accountType = AccountType.INDIVIDUAL;
            if (client.isNotActive()) { throw new ClientNotActiveException(clientId); }
        }

        final Long groupId = fixedDepositApplicationReq.getGroupId();
        if (groupId != null) {
            group = this.groupRepository.findOneWithNotFoundDetection(groupId);
            accountType = AccountType.GROUP;
        }

        if (group != null && client != null) {
            if (!group.hasClientAsMember(client)) { throw new ClientNotInGroupException(clientId, groupId); }
            accountType = AccountType.JLG;
            if (group.isNotActive()) {
                if (group.isCenter()) { throw new CenterNotActiveException(groupId); }
                throw new GroupNotActiveException(groupId);
            }
        }

        final Long fieldOfficerId = fixedDepositApplicationReq.getFieldOfficerId();
        if (fieldOfficerId != null) {
            fieldOfficer = this.staffRepository.findOneWithNotFoundDetection(fieldOfficerId);
        }

        final LocalDate submittedOnDate = fixedDepositApplicationReq.getSubmittedOnDate();

        BigDecimal interestRate;
        if (fixedDepositApplicationReq.isInterestRateSet()) {
            interestRate = fixedDepositApplicationReq.getInterestRate();
        } else {
            interestRate = product.nominalAnnualInterestRate();
        }

        SavingsCompoundingInterestPeriodType interestCompoundingPeriodType;
        final Integer interestPeriodTypeValue = fixedDepositApplicationReq.getInterestPeriodTypeValue();
        if (interestPeriodTypeValue != null) {
            interestCompoundingPeriodType = SavingsCompoundingInterestPeriodType.fromInt(interestPeriodTypeValue);
        } else {
            interestCompoundingPeriodType = product.interestCompoundingPeriodType();
        }

        SavingsPostingInterestPeriodType interestPostingPeriodType;
        final Integer interestPostingPeriodTypeValue = fixedDepositApplicationReq.getInterestPostingPeriodTypeValue();
        if (interestPostingPeriodTypeValue != null) {
            interestPostingPeriodType = SavingsPostingInterestPeriodType.fromInt(interestPostingPeriodTypeValue);
        } else {
            interestPostingPeriodType = product.interestPostingPeriodType();
        }

        SavingsInterestCalculationType interestCalculationType;
        final Integer interestCalculationTypeValue = fixedDepositApplicationReq.getInterestCalculationTypeValue();
        if (interestCalculationTypeValue != null) {
            interestCalculationType = SavingsInterestCalculationType.fromInt(interestCalculationTypeValue);
        } else {
            interestCalculationType = product.interestCalculationType();
        }

        SavingsInterestCalculationDaysInYearType interestCalculationDaysInYearType;
        final Integer interestCalculationDaysInYearTypeValue = fixedDepositApplicationReq.getInterestCalculationDaysInYearTypeValue();
        if (interestCalculationDaysInYearTypeValue != null) {
            interestCalculationDaysInYearType = SavingsInterestCalculationDaysInYearType.fromInt(interestCalculationDaysInYearTypeValue);
        } else {
            interestCalculationDaysInYearType = product.interestCalculationDaysInYearType();
        }

        BigDecimal minRequiredOpeningBalance;
        if (fixedDepositApplicationReq.isMinRequiredOpeningBalanceSet()) {
            minRequiredOpeningBalance = fixedDepositApplicationReq.getMinRequiredOpeningBalance();
        } else {
            minRequiredOpeningBalance = product.minRequiredOpeningBalance();
        }

        Integer lockinPeriodFrequency;
        if (fixedDepositApplicationReq.isLockinPeriodFrequencySet()) {
            lockinPeriodFrequency = fixedDepositApplicationReq.getLockinPeriodFrequency();
        } else {
            lockinPeriodFrequency = product.lockinPeriodFrequency();
        }

        SavingsPeriodFrequencyType lockinPeriodFrequencyType = null;

        if (fixedDepositApplicationReq.isLockinPeriodFrequencyTypeValueSet()) {
            Integer lockinPeriodFrequencyTypeValue;
            lockinPeriodFrequencyTypeValue = fixedDepositApplicationReq.getLockinPeriodFrequencyTypeValue();
            if (lockinPeriodFrequencyTypeValue != null) {
                lockinPeriodFrequencyType = SavingsPeriodFrequencyType.fromInt(lockinPeriodFrequencyTypeValue);
            }
        } else {
            lockinPeriodFrequencyType = product.lockinPeriodFrequencyType();
        }
        boolean isWithdrawalFeeApplicableForTransfer = fixedDepositApplicationReq.isWithdrawalFeeApplicableForTransfer();

        DepositAccountInterestRateChart accountChart = null;
        InterestRateChart productChart = null;

        if (fixedDepositApplicationReq.isChartIdSet()) {
            productChart = product.findChart(fixedDepositApplicationReq.getChartId());
        } else {
            productChart = product.applicableChart(submittedOnDate);
        }

        if (productChart != null) {
            accountChart = DepositAccountInterestRateChart.from(productChart);
        }
        
        boolean withHoldTax = product.withHoldTax();
        if (fixedDepositApplicationReq.isWithHoldTaxSet()) {
            withHoldTax = fixedDepositApplicationReq.isWithHoldTax();
            if(withHoldTax && product.getTaxGroup()  == null){
                throw new UnsupportedParameterException(Arrays.asList(withHoldTaxParamName));
            }
        }

        SavingsAccount account = null;
        if (depositAccountType.isFixedDeposit()) {
            final DepositProductTermAndPreClosure prodTermAndPreClosure = ((FixedDepositProduct) product).depositProductTermAndPreClosure();
            final DepositAccountTermAndPreClosure accountTermAndPreClosure = this.assembleAccountTermAndPreClosure(fixedDepositApplicationReq,
                    prodTermAndPreClosure);

            FixedDepositAccount fdAccount = FixedDepositAccount.createNewApplicationForSubmission(client, group, product, fieldOfficer,
                    accountNo, externalId, accountType, submittedOnDate, submittedBy, interestRate, interestCompoundingPeriodType,
                    interestPostingPeriodType, interestCalculationType, interestCalculationDaysInYearType, minRequiredOpeningBalance,
                    lockinPeriodFrequency, lockinPeriodFrequencyType, isWithdrawalFeeApplicableForTransfer, null,
                    accountTermAndPreClosure, accountChart, withHoldTax, nickname);
            accountTermAndPreClosure.updateAccountReference(fdAccount);
            fdAccount.validateDomainRules();
            account = fdAccount;
        } else if (depositAccountType.isRecurringDeposit()) {
            final DepositProductTermAndPreClosure prodTermAndPreClosure = ((RecurringDepositProduct) product)
                    .depositProductTermAndPreClosure();
            final DepositAccountTermAndPreClosure accountTermAndPreClosure = this.assembleAccountTermAndPreClosure(fixedDepositApplicationReq,
                    prodTermAndPreClosure);

            final DepositProductRecurringDetail prodRecurringDetail = ((RecurringDepositProduct) product).depositRecurringDetail();
            final DepositAccountRecurringDetail accountRecurringDetail = this.assembleAccountRecurringDetail(fixedDepositApplicationReq.getRecurringAccountDetailReq(),
                    prodRecurringDetail.recurringDetail());

            RecurringDepositAccount rdAccount = RecurringDepositAccount.createNewApplicationForSubmittal(client, group, product,
                    fieldOfficer, accountNo, externalId, accountType, submittedOnDate, submittedBy, interestRate,
                    interestCompoundingPeriodType, interestPostingPeriodType, interestCalculationType, interestCalculationDaysInYearType,
                    minRequiredOpeningBalance, lockinPeriodFrequency, lockinPeriodFrequencyType, isWithdrawalFeeApplicableForTransfer,
                    null, accountTermAndPreClosure, accountRecurringDetail, accountChart, withHoldTax, nickname);

            accountTermAndPreClosure.updateAccountReference(rdAccount);
            accountRecurringDetail.updateAccountReference(rdAccount);
            rdAccount.validateDomainRules();
            account = rdAccount;
        }

        if (account != null) {
            account.setHelpers(this.savingsAccountTransactionSummaryWrapper, this.savingsHelper);
            account.validateNewApplicationState(DateUtils.getLocalDateOfTenant(), depositAccountType.resourceName());
        }

        return account;
    }

    public SavingsAccount assembleFrom(final Long savingsId, DepositAccountType depositAccountType) {
        final SavingsAccount account = this.savingsAccountRepository.findOneWithNotFoundDetection(savingsId, depositAccountType);
        account.setHelpers(this.savingsAccountTransactionSummaryWrapper, this.savingsHelper);
        return account;
    }

    public void assignSavingAccountHelpers(final SavingsAccount savingsAccount) {
        savingsAccount.setHelpers(this.savingsAccountTransactionSummaryWrapper, this.savingsHelper);
    }

    public DepositAccountTermAndPreClosure assembleAccountTermAndPreClosure(FixedDepositApplicationReq fixedDepositApplicationReq,
                                                                            final DepositProductTermAndPreClosure productTermAndPreclosure) {
        final DepositPreClosureDetail productPreClosure = (productTermAndPreclosure == null) ? null : productTermAndPreclosure
                .depositPreClosureDetail();
        final DepositTermDetail productTerm = (productTermAndPreclosure == null) ? null : productTermAndPreclosure.depositTermDetail();

        final DepositPreClosureDetail updatedProductPreClosure = this.depositProductAssembler.assemblePreClosureDetail(fixedDepositApplicationReq.getFixedDepositApplicationPreClosureReq(),
                productPreClosure);
        final DepositTermDetail updatedProductTerm = this.depositProductAssembler.assembleDepositTermDetail(fixedDepositApplicationReq.getFixedDepositApplicationTermsReq(), productTerm);

        final BigDecimal depositAmount = fixedDepositApplicationReq.getDepositAmount();
        final Integer depositPeriod = fixedDepositApplicationReq.getDepositPeriod();
        final SavingsPeriodFrequencyType depositPeriodFrequency = fixedDepositApplicationReq.getDepositPeriodFrequency();
        final LocalDate expectedFirstDepositOnDate = fixedDepositApplicationReq.getExpectedFirstDepositOnDate();
        final Boolean transferInterest = fixedDepositApplicationReq.getTransferInterest();

        // maturityAmount and maturityDate are calculated and updated in the account
        return DepositAccountTermAndPreClosure.createNew(updatedProductPreClosure, updatedProductTerm, null, depositAmount,
                null, null, depositPeriod, depositPeriodFrequency, expectedFirstDepositOnDate, null,
                transferInterest);
    }

    public DepositAccountRecurringDetail assembleAccountRecurringDetail(RecurringAccountDetailReq recurringAccountDetailReq,
                                                                        final DepositRecurringDetail prodRecurringDetail) {

        final BigDecimal recurringDepositAmount = recurringAccountDetailReq.getRecurringDepositAmount();
        boolean isMandatoryDeposit;
        boolean allowWithdrawal;
        boolean adjustAdvanceTowardsFuturePayments;
        boolean isCalendarInherited = recurringAccountDetailReq.isCalendarInherited();

        if (recurringAccountDetailReq.isMandatoryDepositSet()) {
            isMandatoryDeposit = recurringAccountDetailReq.isMandatoryDeposit();
        } else {
            isMandatoryDeposit = prodRecurringDetail.isMandatoryDeposit();
        }
        if (recurringAccountDetailReq.isAllowWithdrawalSet()) {
            allowWithdrawal = recurringAccountDetailReq.isAllowWithdrawal();
        } else {
            allowWithdrawal = prodRecurringDetail.allowWithdrawal();
        }
        if (recurringAccountDetailReq.isAdjustAdvanceTowardsFuturePaymentsSet()) {
            adjustAdvanceTowardsFuturePayments = recurringAccountDetailReq.isAdjustAdvanceTowardsFuturePayments();
        } else {
            adjustAdvanceTowardsFuturePayments = prodRecurringDetail.adjustAdvanceTowardsFuturePayments();
        }

        final DepositRecurringDetail depositRecurringDetail = DepositRecurringDetail.createFrom(isMandatoryDeposit, allowWithdrawal,
                adjustAdvanceTowardsFuturePayments);
        return DepositAccountRecurringDetail.createNew(recurringDepositAmount,
                depositRecurringDetail, null, isCalendarInherited);
    }

    public Collection<SavingsAccountTransactionDTO> assembleBulkMandatorySavingsAccountTransactionDTOs(final JsonCommand command,final PaymentDetail paymentDetail) {
        AppUser user = getAppUserIfPresent();
        final String json = command.json();
        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }
        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final Collection<SavingsAccountTransactionDTO> savingsAccountTransactions = new ArrayList<>();
        final LocalDate transactionDate = this.fromApiJsonHelper.extractLocalDateNamed(transactionDateParamName, element);
        final String dateFormat = this.fromApiJsonHelper.extractDateFormatParameter(element.getAsJsonObject());
        final JsonObject topLevelJsonElement = element.getAsJsonObject();
        final Locale locale = this.fromApiJsonHelper.extractLocaleParameter(topLevelJsonElement);
        final DateTimeFormatter formatter = DateTimeFormat.forPattern(dateFormat).withLocale(locale);

        if (element.isJsonObject()) {
            if (topLevelJsonElement.has(bulkSavingsDueTransactionsParamName)
                    && topLevelJsonElement.get(bulkSavingsDueTransactionsParamName).isJsonArray()) {
                final JsonArray array = topLevelJsonElement.get(bulkSavingsDueTransactionsParamName).getAsJsonArray();

                for (int i = 0; i < array.size(); i++) {
                    final JsonObject savingsTransactionElement = array.get(i).getAsJsonObject();
                    final Long savingsId = this.fromApiJsonHelper.extractLongNamed(savingsIdParamName, savingsTransactionElement);
                    final BigDecimal dueAmount = this.fromApiJsonHelper.extractBigDecimalNamed(transactionAmountParamName,
                            savingsTransactionElement, locale);
                    final Integer depositAccountType = this.fromApiJsonHelper.extractIntegerNamed(
                            CollectionSheetConstants.depositAccountTypeParamName, savingsTransactionElement, locale);
                    PaymentDetail detail = paymentDetail;
                    if (paymentDetail == null) {
                        detail = this.paymentDetailAssembler.fetchPaymentDetail(savingsTransactionElement);
                    }
                    final SavingsAccountTransactionDTO savingsAccountTransactionDTO = new SavingsAccountTransactionDTO(formatter,
                            transactionDate, dueAmount, detail, new Date(), savingsId, user, depositAccountType, false);
                    savingsAccountTransactions.add(savingsAccountTransactionDTO);
                }
            }
        }

        return savingsAccountTransactions;
    }

    private AppUser getAppUserIfPresent() {
        AppUser user = null;
        if (this.context != null) {
            user = this.context.getAuthenticatedUserIfPresent();
        }
        return user;
    }

}