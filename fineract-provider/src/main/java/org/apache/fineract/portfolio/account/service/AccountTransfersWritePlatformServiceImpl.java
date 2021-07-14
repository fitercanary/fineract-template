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
package org.apache.fineract.portfolio.account.service;

import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyConstant;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationProperty;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.portfolio.account.AccountDetailConstants;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.api.AccountTransfersApiConstants;
import org.apache.fineract.portfolio.account.data.AccountTransferDTO;
import org.apache.fineract.portfolio.account.data.AccountTransfersDataValidator;
import org.apache.fineract.portfolio.account.domain.AccountTransferAssembler;
import org.apache.fineract.portfolio.account.domain.AccountTransferDetailRepository;
import org.apache.fineract.portfolio.account.domain.AccountTransferDetails;
import org.apache.fineract.portfolio.account.domain.AccountTransferRepository;
import org.apache.fineract.portfolio.account.domain.AccountTransferTransaction;
import org.apache.fineract.portfolio.account.domain.AccountTransferType;
import org.apache.fineract.portfolio.loanaccount.data.HolidayDetailDTO;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanAccountDomainService;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDisbursementDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.loanaccount.exception.InvalidPaidInAdvanceAmountException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanForeclosureException;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequest;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.savings.SavingsTransactionBooleanValues;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountDomainService;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsTransactionRequest;
import org.apache.fineract.portfolio.savings.domain.SavingsTransactionRequestRepository;
import org.apache.fineract.portfolio.savings.exception.InsufficientAccountBalanceException;
import org.apache.fineract.portfolio.savings.service.SavingsAccountWritePlatformService;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.apache.fineract.portfolio.account.AccountDetailConstants.fromAccountIdParamName;
import static org.apache.fineract.portfolio.account.AccountDetailConstants.fromAccountTypeParamName;
import static org.apache.fineract.portfolio.account.AccountDetailConstants.toAccountIdParamName;
import static org.apache.fineract.portfolio.account.AccountDetailConstants.toAccountTypeParamName;
import static org.apache.fineract.portfolio.account.api.AccountTransfersApiConstants.transferAmountParamName;
import static org.apache.fineract.portfolio.account.api.AccountTransfersApiConstants.transferDateParamName;
import static org.apache.fineract.portfolio.account.api.AccountTransfersApiConstants.transferDescriptionParamName;

@Service
public class AccountTransfersWritePlatformServiceImpl implements AccountTransfersWritePlatformService {

    private final AccountTransfersDataValidator accountTransfersDataValidator;
    private final AccountTransferAssembler accountTransferAssembler;
    private final AccountTransferRepository accountTransferRepository;
    private final SavingsAccountAssembler savingsAccountAssembler;
    private final SavingsAccountDomainService savingsAccountDomainService;
    private final LoanAssembler loanAccountAssembler;
    private final LoanAccountDomainService loanAccountDomainService;
    private final SavingsAccountWritePlatformService savingsAccountWritePlatformService;
    private final AccountTransferDetailRepository accountTransferDetailRepository;
    private final LoanReadPlatformService loanReadPlatformService;
    private final SavingsTransactionRequestRepository savingsTransactionRequestRepository;
    private final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService;
    private final GlobalConfigurationRepositoryWrapper globalConfigurationRepositoryWrapper;

    @Autowired
    public AccountTransfersWritePlatformServiceImpl(final AccountTransfersDataValidator accountTransfersDataValidator,
            final AccountTransferAssembler accountTransferAssembler, final AccountTransferRepository accountTransferRepository,
            final SavingsAccountAssembler savingsAccountAssembler, final SavingsAccountDomainService savingsAccountDomainService,
            final LoanAssembler loanAssembler, final LoanAccountDomainService loanAccountDomainService,
            final SavingsAccountWritePlatformService savingsAccountWritePlatformService,
            final AccountTransferDetailRepository accountTransferDetailRepository, final LoanReadPlatformService loanReadPlatformService,
            final SavingsTransactionRequestRepository savingsTransactionRequestRepository,
            final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService,
                                                    final GlobalConfigurationRepositoryWrapper globalConfigurationRepositoryWrapper) {
        this.accountTransfersDataValidator = accountTransfersDataValidator;
        this.accountTransferAssembler = accountTransferAssembler;
        this.accountTransferRepository = accountTransferRepository;
        this.savingsAccountAssembler = savingsAccountAssembler;
        this.savingsAccountDomainService = savingsAccountDomainService;
        this.loanAccountAssembler = loanAssembler;
        this.loanAccountDomainService = loanAccountDomainService;
        this.savingsAccountWritePlatformService = savingsAccountWritePlatformService;
        this.accountTransferDetailRepository = accountTransferDetailRepository;
        this.loanReadPlatformService = loanReadPlatformService;
        this.savingsTransactionRequestRepository = savingsTransactionRequestRepository;
        this.loanScheduleHistoryWritePlatformService = loanScheduleHistoryWritePlatformService;
        this.globalConfigurationRepositoryWrapper = globalConfigurationRepositoryWrapper;
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command) {
        boolean isRegularTransaction = true;

        this.accountTransfersDataValidator.validate(command);

        final LocalDate transactionDate = command.localDateValueOfParameterNamed(transferDateParamName);
        BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed(transferAmountParamName);

        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);

        final Integer fromAccountTypeId = command.integerValueSansLocaleOfParameterNamed(fromAccountTypeParamName);
        final PortfolioAccountType fromAccountType = PortfolioAccountType.fromInt(fromAccountTypeId);

        final Integer toAccountTypeId = command.integerValueSansLocaleOfParameterNamed(toAccountTypeParamName);
        final PortfolioAccountType toAccountType = PortfolioAccountType.fromInt(toAccountTypeId);
        
        final String noteText = command.stringValueOfParameterNamed(transferDescriptionParamName);

        final PaymentDetail paymentDetail = null;
        Long fromSavingsAccountId = null;
        Long transferDetailId = null;
        boolean isInterestTransfer = false;
        boolean isAccountTransfer = true;
        Long fromLoanAccountId = null;
        boolean isWithdrawBalance = false;
        boolean limitValidationSkip = false;
        String fromSavingsAccountNumber = null;
        String toSavingsAccountNumber = null;

        if (isSavingsToSavingsAccountTransfer(fromAccountType, toAccountType)) {

            fromSavingsAccountId = command.longValueOfParameterNamed(fromAccountIdParamName);
            final SavingsAccount fromSavingsAccount = this.savingsAccountAssembler.assembleFrom(fromSavingsAccountId);
            fromSavingsAccountNumber = fromSavingsAccount.getAccountNumber();

            final Long toSavingsId = command.longValueOfParameterNamed(toAccountIdParamName);
            final SavingsAccount toSavingsAccount = this.savingsAccountAssembler.assembleFrom(toSavingsId);
            toSavingsAccountNumber = toSavingsAccount.getAccountNumber();
            if (toSavingsAccount.depositAccountType().isFixedDeposit() || toSavingsAccount.depositAccountType().isCurrentDeposit()
                    || toSavingsAccount.depositAccountType().isRecurringDeposit() || fromSavingsAccount.clientId().equals(toSavingsAccount.clientId())) {
                limitValidationSkip = true;
            }

            final SavingsTransactionBooleanValues transactionBooleanValues = new SavingsTransactionBooleanValues(isAccountTransfer,
                    isRegularTransaction, fromSavingsAccount.isWithdrawalFeeApplicableForTransfer(), isInterestTransfer, isWithdrawBalance);

            final SavingsAccountTransaction withdrawal = this.savingsAccountDomainService.handleWithdrawal(fromSavingsAccount, fmt,
                    transactionDate, transactionAmount, paymentDetail, transactionBooleanValues, limitValidationSkip);

            this.savingsAccountWritePlatformService.saveTransactionRequest(command, withdrawal);

            final SavingsAccountTransaction deposit = this.savingsAccountDomainService.handleDeposit(toSavingsAccount, fmt, transactionDate,
                    transactionAmount, paymentDetail, isAccountTransfer, isRegularTransaction);

            this.setBeneficiaryTransactionRequest(deposit, withdrawal, command);

            final AccountTransferDetails accountTransferDetails = this.accountTransferAssembler.assembleSavingsToSavingsTransfer(command,
                    fromSavingsAccount, toSavingsAccount, withdrawal, deposit);
            this.accountTransferDetailRepository.saveAndFlush(accountTransferDetails);
            transferDetailId = accountTransferDetails.getId();

        } else if (isSavingsToLoanAccountTransfer(fromAccountType, toAccountType) && !isForeClosure(noteText)) {
            //
            fromSavingsAccountId = command.longValueOfParameterNamed(fromAccountIdParamName);
            final SavingsAccount fromSavingsAccount = this.savingsAccountAssembler.assembleFrom(fromSavingsAccountId);
            fromSavingsAccountNumber = fromSavingsAccount.getAccountNumber();

            GlobalConfigurationProperty allowLoanRepaymentThroughOverdraftProperty = globalConfigurationRepositoryWrapper.
                    findOneByNameWithNotFoundDetection(GlobalConfigurationPropertyConstant.ALLOW_LOAN_REPAYMENT_THROUGH_OVERDRAFT);
            boolean allowLoanRepaymentThroughOverdraft = allowLoanRepaymentThroughOverdraftProperty.getValue() == 1 ? true : false;
            if( !allowLoanRepaymentThroughOverdraft && fromSavingsAccount.getSummary().getAccountBalance(fromSavingsAccount.getCurrency()).minus(transactionAmount).isLessThanZero()){
                throw new InsufficientAccountBalanceException("transactionAmount",fromSavingsAccount.getSummary().getAccountBalance(),null,
                        transactionAmount);
            }

            final Long toLoanAccountId = command.longValueOfParameterNamed(toAccountIdParamName);
            final Loan toLoanAccount = this.loanAccountAssembler.assembleFrom(toLoanAccountId);

            final String description = command.stringValueOfParameterNamed(transferDescriptionParamName);

            final SavingsTransactionBooleanValues transactionBooleanValues = new SavingsTransactionBooleanValues(isAccountTransfer,
                    isRegularTransaction, fromSavingsAccount.isWithdrawalFeeApplicableForTransfer(), isInterestTransfer, isWithdrawBalance);

            if (toAccountType.isLoanAccount()) {
                limitValidationSkip = true;
            }

            final SavingsAccountTransaction withdrawal = this.savingsAccountDomainService.handleWithdrawal(fromSavingsAccount, fmt,
                    transactionDate, transactionAmount, paymentDetail, transactionBooleanValues, limitValidationSkip);

            final Boolean isHolidayValidationDone = false;
            final HolidayDetailDTO holidayDetailDto = null;
            final boolean isRecoveryRepayment = false;
            LoanTransaction loanRepaymentTransaction = null;

            if (description.equals(AccountTransfersApiConstants.DisBurseToSavingsCharges)) {
                BigDecimal chargeAmount = BigDecimal.ZERO;
                Set<LoanCharge> charges = toLoanAccount.charges();
                for (LoanCharge charge : charges) {
                    if (charge.isDisburseToSavings()) {
                        if (charge.getAmountPaid(toLoanAccount.getCurrency()).isZero()) {
                            chargeAmount = charge.getAmount(toLoanAccount.getCurrency()).getAmount();
                            loanRepaymentTransaction = this.loanAccountDomainService.makeRepayment(toLoanAccount,
                                    new CommandProcessingResultBuilder(), transactionDate, chargeAmount, paymentDetail, null, null,
                                    isRecoveryRepayment, isAccountTransfer, holidayDetailDto, isHolidayValidationDone);
                        }
                    }
                }
            } else {
                loanRepaymentTransaction = this.loanAccountDomainService.makeRepayment(toLoanAccount, new CommandProcessingResultBuilder(),
                        transactionDate, transactionAmount, paymentDetail, null, null, isRecoveryRepayment, isAccountTransfer,
                        holidayDetailDto, isHolidayValidationDone);
            }

            final AccountTransferDetails accountTransferDetails = this.accountTransferAssembler.assembleSavingsToLoanTransfer(command,
                    fromSavingsAccount, toLoanAccount, withdrawal, loanRepaymentTransaction);
            this.accountTransferDetailRepository.saveAndFlush(accountTransferDetails);
            transferDetailId = accountTransferDetails.getId();

        } else if (isLoanToSavingsAccountTransfer(fromAccountType, toAccountType)) {
            // FIXME - kw - ADD overpaid loan to savings account transfer
            // support.

            fromLoanAccountId = command.longValueOfParameterNamed(fromAccountIdParamName);
            final Loan fromLoanAccount = this.loanAccountAssembler.assembleFrom(fromLoanAccountId);

            final LoanTransaction loanRefundTransaction = this.loanAccountDomainService.makeRefund(fromLoanAccountId,
                    new CommandProcessingResultBuilder(), transactionDate, transactionAmount, paymentDetail, null, null, true);

            final Long toSavingsAccountId = command.longValueOfParameterNamed(toAccountIdParamName);
            final SavingsAccount toSavingsAccount = this.savingsAccountAssembler.assembleFrom(toSavingsAccountId);
            toSavingsAccountNumber = toSavingsAccount.getAccountNumber();

            final SavingsAccountTransaction deposit = this.savingsAccountDomainService.handleDeposit(toSavingsAccount, fmt, transactionDate,
                    transactionAmount, paymentDetail, isAccountTransfer, isRegularTransaction);

            final AccountTransferDetails accountTransferDetails = this.accountTransferAssembler.assembleLoanToSavingsTransfer(command,
                    fromLoanAccount, toSavingsAccount, deposit, loanRefundTransaction);
            this.accountTransferDetailRepository.saveAndFlush(accountTransferDetails);
            transferDetailId = accountTransferDetails.getId();

        } else  if (isSavingsToLoanAccountTransferForeclosure(fromAccountType, toAccountType, noteText)){
            
            fromSavingsAccountId = command.longValueOfParameterNamed(fromAccountIdParamName);
            final SavingsAccount fromSavingsAccount = this.savingsAccountAssembler.assembleFrom(fromSavingsAccountId);
            fromSavingsAccountNumber = fromSavingsAccount.getAccountNumber();
            if(fromSavingsAccount.getSummary().getAccountBalance(fromSavingsAccount.getCurrency()).minus(transactionAmount).isLessThanZero()){
                throw new InsufficientAccountBalanceException("transactionAmount",fromSavingsAccount.getSummary().getAccountBalance(),null,
                        transactionAmount);
            }

            final Long toLoanAccountId = command.longValueOfParameterNamed(toAccountIdParamName);
            final Loan toLoanAccount = this.loanAccountAssembler.assembleFrom(toLoanAccountId);
            
            LoanRescheduleRequest loanRescheduleRequest = null;
            for (LoanDisbursementDetails loanDisbursementDetails : toLoanAccount.getDisbursementDetails()) {
                if (!loanDisbursementDetails.expectedDisbursementDateAsLocalDate().isAfter(transactionDate)
                        && loanDisbursementDetails.actualDisbursementDate() == null) {
                    final String defaultUserMessage = "The loan with undisbursed tranche before foreclosure cannot be foreclosed.";
                    throw new LoanForeclosureException("loan.with.undisbursed.tranche.before.foreclosure.cannot.be.foreclosured",
                            defaultUserMessage, transactionDate);
                }
            }

            this.loanScheduleHistoryWritePlatformService.createAndSaveLoanScheduleArchive(toLoanAccount.getRepaymentScheduleInstallments(), toLoanAccount,
                    loanRescheduleRequest);

            final SavingsTransactionBooleanValues transactionBooleanValues = new SavingsTransactionBooleanValues(isAccountTransfer,
                    isRegularTransaction, fromSavingsAccount.isWithdrawalFeeApplicableForTransfer(), isInterestTransfer, isWithdrawBalance);

            if (toAccountType.isLoanAccount()) {
                limitValidationSkip = true;
            }

            final SavingsAccountTransaction withdrawal = this.savingsAccountDomainService.handleWithdrawal(fromSavingsAccount, fmt,
                    transactionDate, transactionAmount, paymentDetail, transactionBooleanValues, limitValidationSkip);

            LoanTransaction loanRepaymentTransaction = null;
            
            final Map<String, Object> modifications = this.loanAccountDomainService.foreCloseLoan(toLoanAccount, transactionDate, noteText, true);
           
             loanRepaymentTransaction = (LoanTransaction) modifications.get("transaction");

            final AccountTransferDetails accountTransferDetails = this.accountTransferAssembler.assembleSavingsToLoanTransfer(command,
                    fromSavingsAccount, toLoanAccount, withdrawal, loanRepaymentTransaction);
            this.accountTransferDetailRepository.saveAndFlush(accountTransferDetails);
            transferDetailId = accountTransferDetails.getId();
        }

        final CommandProcessingResultBuilder builder = new CommandProcessingResultBuilder().withEntityId(transferDetailId);

        if (fromAccountType.isSavingsAccount()) {
            builder.withSavingsId(fromSavingsAccountId);
            builder.withFromSavingsAccountNumber(fromSavingsAccountNumber);
            builder.withToSavingsAccountNumber(toSavingsAccountNumber);
        }
        if (fromAccountType.isLoanAccount()) {
            builder.withLoanId(fromLoanAccountId);
        }

        return builder.build();
    }

    private void setBeneficiaryTransactionRequest(SavingsAccountTransaction deposit, SavingsAccountTransaction withdrawal,
            JsonCommand command) {
        String category = "Inflows";
        String remarks = command.stringValueOfParameterNamed(AccountDetailConstants.beneficiaryNarrationParamName);
        String transactionBrand = deposit.getSavingsAccount().getClient().getId().equals(withdrawal.getSavingsAccount().getClient().getId())
                ? "Self"
                : withdrawal.getSavingsAccount().getClient().getDisplayName();
        SavingsTransactionRequest transactionRequest = new SavingsTransactionRequest();
        transactionRequest.setRemarks(remarks);
        transactionRequest.setCategory(category);
        transactionRequest.setTransaction(deposit);
        transactionRequest.setTransactionBrandName(transactionBrand);
        this.savingsTransactionRequestRepository.saveAndFlush(transactionRequest);
    }

    @Override
    @Transactional
    public void reverseTransfersWithFromAccountType(final Long accountNumber, final PortfolioAccountType accountTypeId) {
        List<AccountTransferTransaction> acccountTransfers = null;
        if (accountTypeId.isLoanAccount()) {
            acccountTransfers = this.accountTransferRepository.findByFromLoanId(accountNumber);
        }
        if (acccountTransfers != null && acccountTransfers.size() > 0) {
            undoTransactions(acccountTransfers);
        }

    }

    @Override
    @Transactional
    public void reverseTransfersWithFromAccountTransactions(final Collection<Long> fromTransactionIds,
            final PortfolioAccountType accountTypeId) {
        List<AccountTransferTransaction> acccountTransfers = null;
        if (accountTypeId.isLoanAccount()) {
            acccountTransfers = this.accountTransferRepository.findByFromLoanTransactions(fromTransactionIds);
        }
        if (acccountTransfers != null && acccountTransfers.size() > 0) {
            undoTransactions(acccountTransfers);
        }

    }

    @Override
    @Transactional
    public void reverseTransfersWithFromSavingsAccountTransactions(final Collection<Long> fromTransactionIds,
            final PortfolioAccountType accountTypeId) {
        List<AccountTransferTransaction> acccountTransfers = null;
        if (accountTypeId.isSavingsAccount()) {
            acccountTransfers = this.accountTransferRepository.findByFromSavingsTransactions(fromTransactionIds);
        }
        if (acccountTransfers != null && acccountTransfers.size() > 0) {
            undoTransactions(acccountTransfers);
        }

    }

    @Override
    @Transactional
    public void reverseAllTransactions(final Long accountId, final PortfolioAccountType accountTypeId) {
        List<AccountTransferTransaction> acccountTransfers = null;
        if (accountTypeId.isLoanAccount()) {
            acccountTransfers = this.accountTransferRepository.findAllByLoanId(accountId);
        }
        if (acccountTransfers != null && acccountTransfers.size() > 0) {
            undoTransactions(acccountTransfers);
        }
    }

    /**
     * @param acccountTransfers
     */
    private void undoTransactions(final List<AccountTransferTransaction> acccountTransfers) {
        for (final AccountTransferTransaction accountTransfer : acccountTransfers) {
            if (accountTransfer.getFromLoanTransaction() != null) {
                this.loanAccountDomainService.reverseTransfer(accountTransfer.getFromLoanTransaction());
            }
            if (accountTransfer.getToLoanTransaction() != null) {
                this.loanAccountDomainService.reverseTransfer(accountTransfer.getToLoanTransaction());
            }
            if (accountTransfer.getFromTransaction() != null) {
                this.savingsAccountWritePlatformService.undoTransaction(
                        accountTransfer.accountTransferDetails().fromSavingsAccount().getId(), accountTransfer.getFromTransaction().getId(),
                        true);
            }
            if (accountTransfer.getToSavingsTransaction() != null) {
                this.savingsAccountWritePlatformService.undoTransaction(accountTransfer.accountTransferDetails().toSavingsAccount().getId(),
                        accountTransfer.getToSavingsTransaction().getId(), true);
            }
            accountTransfer.reverse();
            this.accountTransferRepository.save(accountTransfer);
        }
    }

    @Override
    @Transactional
    public Long transferFunds(final AccountTransferDTO accountTransferDTO) {
        Long transferTransactionId = null;
        final boolean isAccountTransfer = true;
        final boolean isRegularTransaction = accountTransferDTO.isRegularTransaction();
        AccountTransferDetails accountTransferDetails = accountTransferDTO.getAccountTransferDetails();
        if (isSavingsToLoanAccountTransfer(accountTransferDTO.getFromAccountType(), accountTransferDTO.getToAccountType())) {
            //
            SavingsAccount fromSavingsAccount = null;
            Loan toLoanAccount = null;
            if (accountTransferDetails == null) {
                if (accountTransferDTO.getFromSavingsAccount() == null) {
                    fromSavingsAccount = this.savingsAccountAssembler.assembleFrom(accountTransferDTO.getFromAccountId());
                } else {
                    fromSavingsAccount = accountTransferDTO.getFromSavingsAccount();
                    this.savingsAccountAssembler.setHelpers(fromSavingsAccount);
                }
                if (accountTransferDTO.getLoan() == null) {
                    toLoanAccount = this.loanAccountAssembler.assembleFrom(accountTransferDTO.getToAccountId());
                } else {
                    toLoanAccount = accountTransferDTO.getLoan();
                    this.loanAccountAssembler.setHelpers(toLoanAccount);
                }

            } else {
                fromSavingsAccount = accountTransferDetails.fromSavingsAccount();
                this.savingsAccountAssembler.setHelpers(fromSavingsAccount);
                toLoanAccount = accountTransferDetails.toLoanAccount();
                this.loanAccountAssembler.setHelpers(toLoanAccount);
            }
            GlobalConfigurationProperty allowLoanRepaymentThroughOverdraftProperty = globalConfigurationRepositoryWrapper.
                    findOneByNameWithNotFoundDetection(GlobalConfigurationPropertyConstant.ALLOW_LOAN_REPAYMENT_THROUGH_OVERDRAFT);
            boolean allowLoanRepaymentThroughOverdraft = allowLoanRepaymentThroughOverdraftProperty.getValue() == 1 ? true : false;

            if( !allowLoanRepaymentThroughOverdraft && fromSavingsAccount.getSummary().getAccountBalance(toLoanAccount.getCurrency()).minus(accountTransferDTO.getTransactionAmount()).isLessThanZero()){
                throw new InsufficientAccountBalanceException("transactionAmount",fromSavingsAccount.getSummary().getAccountBalance(),null,
                        accountTransferDTO.getTransactionAmount());
            }

            final SavingsTransactionBooleanValues transactionBooleanValues = new SavingsTransactionBooleanValues(isAccountTransfer,
                    isRegularTransaction, fromSavingsAccount.isWithdrawalFeeApplicableForTransfer(),
                    AccountTransferType.fromInt(accountTransferDTO.getTransferType()).isInterestTransfer(),
                    accountTransferDTO.isExceptionForBalanceCheck());

            final SavingsAccountTransaction withdrawal = this.savingsAccountDomainService.handleWithdrawal(fromSavingsAccount,
                    accountTransferDTO.getFmt(), accountTransferDTO.getTransactionDate(), accountTransferDTO.getTransactionAmount(),
                    accountTransferDTO.getPaymentDetail(), transactionBooleanValues, false);

            LoanTransaction loanTransaction = null;

            if (AccountTransferType.fromInt(accountTransferDTO.getTransferType()).isChargePayment()) {
                loanTransaction = this.loanAccountDomainService.makeChargePayment(toLoanAccount, accountTransferDTO.getChargeId(),
                        accountTransferDTO.getTransactionDate(), accountTransferDTO.getTransactionAmount(),
                        accountTransferDTO.getPaymentDetail(), null, null, accountTransferDTO.getToTransferType(),
                        accountTransferDTO.getLoanInstallmentNumber(), true);

            } else {
                final boolean isRecoveryRepayment = false;
                final Boolean isHolidayValidationDone = false;
                final HolidayDetailDTO holidayDetailDto = null;
                loanTransaction = this.loanAccountDomainService.makeRepayment(toLoanAccount, new CommandProcessingResultBuilder(),
                        accountTransferDTO.getTransactionDate(), accountTransferDTO.getTransactionAmount(),
                        accountTransferDTO.getPaymentDetail(), null, null, isRecoveryRepayment, isAccountTransfer, holidayDetailDto,
                        isHolidayValidationDone);
            }

            accountTransferDetails = this.accountTransferAssembler.assembleSavingsToLoanTransfer(accountTransferDTO, fromSavingsAccount,
                    toLoanAccount, withdrawal, loanTransaction);
            this.accountTransferDetailRepository.saveAndFlush(accountTransferDetails);
            transferTransactionId = accountTransferDetails.getId();
        } else if (isSavingsToSavingsAccountTransfer(accountTransferDTO.getFromAccountType(), accountTransferDTO.getToAccountType())) {

            SavingsAccount fromSavingsAccount = null;
            SavingsAccount toSavingsAccount = null;
            if (accountTransferDetails == null) {
                if (accountTransferDTO.getFromSavingsAccount() == null) {
                    fromSavingsAccount = this.savingsAccountAssembler.assembleFrom(accountTransferDTO.getFromAccountId());
                } else {
                    fromSavingsAccount = accountTransferDTO.getFromSavingsAccount();
                    this.savingsAccountAssembler.setHelpers(fromSavingsAccount);
                }
                if (accountTransferDTO.getToSavingsAccount() == null) {
                    toSavingsAccount = this.savingsAccountAssembler.assembleFrom(accountTransferDTO.getToAccountId());
                } else {
                    toSavingsAccount = accountTransferDTO.getToSavingsAccount();
                    this.savingsAccountAssembler.setHelpers(toSavingsAccount);
                }
            } else {
                fromSavingsAccount = accountTransferDetails.fromSavingsAccount();
                this.savingsAccountAssembler.setHelpers(fromSavingsAccount);
                toSavingsAccount = accountTransferDetails.toSavingsAccount();
                this.savingsAccountAssembler.setHelpers(toSavingsAccount);
            }

            final SavingsTransactionBooleanValues transactionBooleanValues = new SavingsTransactionBooleanValues(isAccountTransfer,
                    isRegularTransaction, fromSavingsAccount.isWithdrawalFeeApplicableForTransfer(),
                    AccountTransferType.fromInt(accountTransferDTO.getTransferType()).isInterestTransfer(),
                    accountTransferDTO.isExceptionForBalanceCheck());

            final SavingsAccountTransaction withdrawal = this.savingsAccountDomainService.handleWithdrawal(fromSavingsAccount,
                    accountTransferDTO.getFmt(), accountTransferDTO.getTransactionDate(), accountTransferDTO.getTransactionAmount(),
                    accountTransferDTO.getPaymentDetail(), transactionBooleanValues, true);

            final SavingsAccountTransaction deposit = this.savingsAccountDomainService.handleDeposit(toSavingsAccount,
                    accountTransferDTO.getFmt(), accountTransferDTO.getTransactionDate(), accountTransferDTO.getTransactionAmount(),
                    accountTransferDTO.getPaymentDetail(), isAccountTransfer, isRegularTransaction);

            accountTransferDetails = this.accountTransferAssembler.assembleSavingsToSavingsTransfer(accountTransferDTO, fromSavingsAccount,
                    toSavingsAccount, withdrawal, deposit);
            this.accountTransferDetailRepository.saveAndFlush(accountTransferDetails);
            transferTransactionId = accountTransferDetails.getId();

        } else if (isLoanToSavingsAccountTransfer(accountTransferDTO.getFromAccountType(), accountTransferDTO.getToAccountType())) {

            Loan fromLoanAccount = null;
            SavingsAccount toSavingsAccount = null;
            if (accountTransferDetails == null) {
                if (accountTransferDTO.getLoan() == null) {
                    fromLoanAccount = this.loanAccountAssembler.assembleFrom(accountTransferDTO.getFromAccountId());
                } else {
                    fromLoanAccount = accountTransferDTO.getLoan();
                    this.loanAccountAssembler.setHelpers(fromLoanAccount);
                }
                toSavingsAccount = this.savingsAccountAssembler.assembleFrom(accountTransferDTO.getToAccountId());
            } else {
                fromLoanAccount = accountTransferDetails.fromLoanAccount();
                this.loanAccountAssembler.setHelpers(fromLoanAccount);
                toSavingsAccount = accountTransferDetails.toSavingsAccount();
                this.savingsAccountAssembler.setHelpers(toSavingsAccount);
            }
            LoanTransaction loanTransaction = null;
            if (LoanTransactionType.DISBURSEMENT.getValue().equals(accountTransferDTO.getFromTransferType())) {
                loanTransaction = this.loanAccountDomainService.makeDisburseTransaction(accountTransferDTO.getFromAccountId(),
                        accountTransferDTO.getTransactionDate(), accountTransferDTO.getTransactionAmount(),
                        accountTransferDTO.getPaymentDetail(), accountTransferDTO.getNoteText(), accountTransferDTO.getTxnExternalId(),
                        true);
            } else {
                loanTransaction = this.loanAccountDomainService.makeRefund(accountTransferDTO.getFromAccountId(),
                        new CommandProcessingResultBuilder(), accountTransferDTO.getTransactionDate(),
                        accountTransferDTO.getTransactionAmount(), accountTransferDTO.getPaymentDetail(), accountTransferDTO.getNoteText(),
                        accountTransferDTO.getTxnExternalId(), true);
            }

            final SavingsAccountTransaction deposit = this.savingsAccountDomainService.handleDeposit(toSavingsAccount,
                    accountTransferDTO.getFmt(), accountTransferDTO.getTransactionDate(), accountTransferDTO.getTransactionAmount(),
                    accountTransferDTO.getPaymentDetail(), isAccountTransfer, isRegularTransaction);
            accountTransferDetails = this.accountTransferAssembler.assembleLoanToSavingsTransfer(accountTransferDTO, fromLoanAccount,
                    toSavingsAccount, deposit, loanTransaction);
            this.accountTransferDetailRepository.saveAndFlush(accountTransferDetails);
            transferTransactionId = accountTransferDetails.getId();
        } else {
            throw new GeneralPlatformDomainRuleException("error.msg.accounttransfer.loan.to.loan.not.supported",
                    "Account transfer from loan to another loan is not supported");
        }

        return transferTransactionId;
    }

    @Override
    public AccountTransferDetails repayLoanWithTopup(AccountTransferDTO accountTransferDTO) {
        final boolean isAccountTransfer = true;
        Loan fromLoanAccount = null;
        if (accountTransferDTO.getFromLoan() == null) {
            fromLoanAccount = this.loanAccountAssembler.assembleFrom(accountTransferDTO.getFromAccountId());
        } else {
            fromLoanAccount = accountTransferDTO.getFromLoan();
            this.loanAccountAssembler.setHelpers(fromLoanAccount);
        }
        Loan toLoanAccount = null;
        if (accountTransferDTO.getToLoan() == null) {
            toLoanAccount = this.loanAccountAssembler.assembleFrom(accountTransferDTO.getToAccountId());
        } else {
            toLoanAccount = accountTransferDTO.getToLoan();
            this.loanAccountAssembler.setHelpers(toLoanAccount);
        }

        LoanTransaction disburseTransaction = this.loanAccountDomainService.makeDisburseTransaction(accountTransferDTO.getFromAccountId(),
                accountTransferDTO.getTransactionDate(), accountTransferDTO.getTransactionAmount(), accountTransferDTO.getPaymentDetail(),
                accountTransferDTO.getNoteText(), accountTransferDTO.getTxnExternalId(), true);


        //TODO handling loan closure here
        LoanRescheduleRequest loanRescheduleRequest = null;
        for (LoanDisbursementDetails loanDisbursementDetails : toLoanAccount.getDisbursementDetails()) {
            if (!loanDisbursementDetails.expectedDisbursementDateAsLocalDate().isAfter(accountTransferDTO.getTransactionDate())
                    && loanDisbursementDetails.actualDisbursementDate() == null) {
                final String defaultUserMessage = "The loan with undisbursed tranche before foreclosure cannot be foreclosed.";
                throw new LoanForeclosureException("loan.with.undisbursed.tranche.before.foreclosure.cannot.be.foreclosured",
                        defaultUserMessage, accountTransferDTO.getTransactionDate());
            }
        }
        this.loanScheduleHistoryWritePlatformService.createAndSaveLoanScheduleArchive(toLoanAccount.getRepaymentScheduleInstallments(), toLoanAccount,
                loanRescheduleRequest);
        final Map<String, Object> modifications = this.loanAccountDomainService.foreCloseLoan(toLoanAccount, accountTransferDTO.getTransactionDate(), accountTransferDTO.getNoteText(), true);
        LoanTransaction loanRepaymentTransaction = (LoanTransaction) modifications.get("transaction");


        AccountTransferDetails accountTransferDetails = this.accountTransferAssembler.assembleLoanToLoanTransfer(accountTransferDTO,
                fromLoanAccount, toLoanAccount, disburseTransaction, loanRepaymentTransaction);
        this.accountTransferDetailRepository.saveAndFlush(accountTransferDetails);

        return accountTransferDetails;
    }

    @Override
    @Transactional
    public void updateLoanTransaction(final Long loanTransactionId, final LoanTransaction newLoanTransaction) {
        final AccountTransferTransaction transferTransaction = this.accountTransferRepository.findByToLoanTransactionId(loanTransactionId);
        if (transferTransaction != null) {
            transferTransaction.updateToLoanTransaction(newLoanTransaction);
            this.accountTransferRepository.save(transferTransaction);
        }
    }

    private boolean isLoanToSavingsAccountTransfer(final PortfolioAccountType fromAccountType, final PortfolioAccountType toAccountType) {
        return fromAccountType.isLoanAccount() && toAccountType.isSavingsAccount();
    }

    private boolean isSavingsToLoanAccountTransfer(final PortfolioAccountType fromAccountType, final PortfolioAccountType toAccountType) {
        return fromAccountType.isSavingsAccount() && toAccountType.isLoanAccount();
    }

    private boolean isSavingsToSavingsAccountTransfer(final PortfolioAccountType fromAccountType,
            final PortfolioAccountType toAccountType) {
        return fromAccountType.isSavingsAccount() && toAccountType.isSavingsAccount();
    }
    
    private boolean isSavingsToLoanAccountTransferForeclosure(final PortfolioAccountType fromAccountType, final PortfolioAccountType toAccountType, final String noteText) {
        return fromAccountType.isSavingsAccount() && toAccountType.isLoanAccount() && noteText.equalsIgnoreCase("Foreclosure"); 
    }
    
    private boolean isForeClosure(String text) {
        return text.equalsIgnoreCase("Foreclosure");
    }

    @Override
    @Transactional
    public CommandProcessingResult refundByTransfer(JsonCommand command) {
        // TODO Auto-generated method stub
        this.accountTransfersDataValidator.validate(command);

        final LocalDate transactionDate = command.localDateValueOfParameterNamed(transferDateParamName);
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed(transferAmountParamName);

        final Locale locale = command.extractLocale();
        final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);

        final PaymentDetail paymentDetail = null;
        Long transferTransactionId = null;

        final Long fromLoanAccountId = command.longValueOfParameterNamed(fromAccountIdParamName);
        final Loan fromLoanAccount = this.loanAccountAssembler.assembleFrom(fromLoanAccountId);

        BigDecimal overpaid = this.loanReadPlatformService.retrieveTotalPaidInAdvance(fromLoanAccountId).getPaidInAdvance();

        if (overpaid == null || overpaid.equals(BigDecimal.ZERO) || transactionAmount.floatValue() > overpaid.floatValue()) {
            if (overpaid == null) overpaid = BigDecimal.ZERO;
            throw new InvalidPaidInAdvanceAmountException(overpaid.toPlainString());
        }

        final LoanTransaction loanRefundTransaction = this.loanAccountDomainService.makeRefundForActiveLoan(fromLoanAccountId,
                new CommandProcessingResultBuilder(), transactionDate, transactionAmount, paymentDetail, null, null, true);

        final Long toSavingsAccountId = command.longValueOfParameterNamed(toAccountIdParamName);
        final SavingsAccount toSavingsAccount = this.savingsAccountAssembler.assembleFrom(toSavingsAccountId);

        final SavingsAccountTransaction deposit = this.savingsAccountDomainService.handleDeposit(toSavingsAccount, fmt, transactionDate,
                transactionAmount, paymentDetail, true, true);

        final AccountTransferDetails accountTransferDetails = this.accountTransferAssembler.assembleLoanToSavingsTransfer(command,
                fromLoanAccount, toSavingsAccount, deposit, loanRefundTransaction);
        this.accountTransferDetailRepository.saveAndFlush(accountTransferDetails);
        transferTransactionId = accountTransferDetails.getId();

        final CommandProcessingResultBuilder builder = new CommandProcessingResultBuilder().withEntityId(transferTransactionId);

        // if (fromAccountType.isSavingsAccount()) {

        builder.withSavingsId(toSavingsAccountId);
        // }

        return builder.build();
    }

}