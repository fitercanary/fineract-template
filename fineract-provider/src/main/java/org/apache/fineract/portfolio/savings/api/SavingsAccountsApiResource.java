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
package org.apache.fineract.portfolio.savings.api;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.accounting.common.AccountingConstants;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.data.GlobalEntityType;
import org.apache.fineract.infrastructure.bulkimport.service.BulkImportWorkbookPopulatorService;
import org.apache.fineract.infrastructure.bulkimport.service.BulkImportWorkbookService;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.api.ApiParameterHelper;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.SavingsApiConstants;
import org.apache.fineract.portfolio.savings.data.SavingsAccountChargeData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionData;
import org.apache.fineract.portfolio.savings.service.SavingsAccountChargeReadPlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsAccountReadPlatformService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Path("/savingsaccounts")
@Component
@Scope("singleton")
public class SavingsAccountsApiResource {

    private final SavingsAccountReadPlatformService savingsAccountReadPlatformService;
    private final PlatformSecurityContext context;
    private final DefaultToApiJsonSerializer<SavingsAccountData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final SavingsAccountChargeReadPlatformService savingsAccountChargeReadPlatformService;
    private final BulkImportWorkbookService bulkImportWorkbookService;
    private final BulkImportWorkbookPopulatorService bulkImportWorkbookPopulatorService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;

    @Autowired
    public SavingsAccountsApiResource(final SavingsAccountReadPlatformService savingsAccountReadPlatformService,
            final PlatformSecurityContext context, final DefaultToApiJsonSerializer<SavingsAccountData> toApiJsonSerializer,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final ApiRequestParameterHelper apiRequestParameterHelper,
            final SavingsAccountChargeReadPlatformService savingsAccountChargeReadPlatformService,
            final BulkImportWorkbookService bulkImportWorkbookService,
            final BulkImportWorkbookPopulatorService bulkImportWorkbookPopulatorService,
            final CodeValueReadPlatformService codeValueReadPlatformService) {
        this.savingsAccountReadPlatformService = savingsAccountReadPlatformService;
        this.context = context;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.savingsAccountChargeReadPlatformService = savingsAccountChargeReadPlatformService;
        this.bulkImportWorkbookService = bulkImportWorkbookService;
        this.bulkImportWorkbookPopulatorService = bulkImportWorkbookPopulatorService;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String template(@QueryParam("clientId") final Long clientId, @QueryParam("groupId") final Long groupId,
            @QueryParam("productId") final Long productId,
            @DefaultValue("false") @QueryParam("staffInSelectedOfficeOnly") final boolean staffInSelectedOfficeOnly,
            @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);

        final SavingsAccountData savingsAccount = this.savingsAccountReadPlatformService.retrieveTemplate(clientId, groupId, productId,
                staffInSelectedOfficeOnly);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, savingsAccount,
                SavingsApiSetConstants.SAVINGS_ACCOUNT_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAll(@Context final UriInfo uriInfo, @QueryParam("sqlSearch") final String sqlSearch,
            @QueryParam("externalId") final String externalId,
            // @QueryParam("underHierarchy") final String hierarchy,
            @QueryParam("offset") final Integer offset, @QueryParam("limit") final Integer limit,
            @QueryParam("orderBy") final String orderBy, @QueryParam("sortOrder") final String sortOrder) {

        this.context.authenticatedUser().validateHasReadPermission(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);

        final SearchParameters searchParameters = SearchParameters.forSavings(sqlSearch, externalId, offset, limit, orderBy, sortOrder);

        final Page<SavingsAccountData> products = this.savingsAccountReadPlatformService.retrieveAll(searchParameters);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, products, SavingsApiSetConstants.SAVINGS_ACCOUNT_RESPONSE_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String submitApplication(final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createSavingsAccount().withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@PathParam("accountId") final Long accountId,
            @DefaultValue("false") @QueryParam("staffInSelectedOfficeOnly") final boolean staffInSelectedOfficeOnly,
            @DefaultValue("all") @QueryParam("chargeStatus") final String chargeStatus, @QueryParam("pageNumber") final Integer pageNumber,
            @QueryParam("pageSize") final Integer pageSize, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);

        if (!(is(chargeStatus, "all") || is(chargeStatus, "active") || is(chargeStatus, "inactive") || is(chargeStatus, "pageNumber")
                || is(chargeStatus, "pageSize"))) {
            throw new UnrecognizedQueryParamException("status", chargeStatus,
                    new Object[] { "all", "active", "inactive", "pageNumber", "pageSize" });
        }

        final SavingsAccountData savingsAccount = this.savingsAccountReadPlatformService.retrieveOne(accountId);

        final Set<String> mandatoryResponseParameters = new HashSet<>();
        final SavingsAccountData savingsAccountTemplate = populateTemplateAndAssociations(accountId, savingsAccount,
                staffInSelectedOfficeOnly, chargeStatus, uriInfo, mandatoryResponseParameters, pageNumber, pageSize);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters(),
                mandatoryResponseParameters);
        return this.toApiJsonSerializer.serialize(settings, savingsAccountTemplate,
                SavingsApiSetConstants.SAVINGS_ACCOUNT_RESPONSE_DATA_PARAMETERS);
    }
    
    @GET
    @Path("account/{accountNumber}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOneByAccount(@PathParam("accountNumber") final Long accountNumber,
            @DefaultValue("false") @QueryParam("staffInSelectedOfficeOnly") final boolean staffInSelectedOfficeOnly,
            @DefaultValue("all") @QueryParam("chargeStatus") final String chargeStatus, @QueryParam("pageNumber") final Integer pageNumber,
            @QueryParam("pageSize") final Integer pageSize, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);

        if (!(is(chargeStatus, "all") || is(chargeStatus, "active") || is(chargeStatus, "inactive") || is(chargeStatus, "pageNumber")
                || is(chargeStatus, "pageSize"))) {
            throw new UnrecognizedQueryParamException("status", chargeStatus,
                    new Object[] { "all", "active", "inactive", "pageNumber", "pageSize" });
        }

        final SavingsAccountData savingsAccount = this.savingsAccountReadPlatformService.retrieveOneByAccount(accountNumber);

        final Set<String> mandatoryResponseParameters = new HashSet<>();
        final SavingsAccountData savingsAccountTemplate = populateTemplateAndAssociations(savingsAccount.id(), savingsAccount,
                staffInSelectedOfficeOnly, chargeStatus, uriInfo, mandatoryResponseParameters, pageNumber, pageSize);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters(),
                mandatoryResponseParameters);
        return this.toApiJsonSerializer.serialize(settings, savingsAccountTemplate,
                SavingsApiSetConstants.SAVINGS_ACCOUNT_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("transaction/{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOneWithoutAccuralTransaction(@PathParam("accountId") final Long accountId,
            @DefaultValue("false") @QueryParam("staffInSelectedOfficeOnly") final boolean staffInSelectedOfficeOnly,
            @DefaultValue("all") @QueryParam("chargeStatus") final String chargeStatus, @QueryParam("pageNumber") final Integer pageNumber,
            @QueryParam("pageSize") final Integer pageSize, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);
        if (!(is(chargeStatus, "all") || is(chargeStatus, "active") || is(chargeStatus, "inactive") || is(chargeStatus, "pageNumber")
                || is(chargeStatus, "pageSize"))) {
            throw new UnrecognizedQueryParamException("status", chargeStatus,
                    new Object[] { "all", "active", "inactive", "pageNumber", "pageSize" });
        }

        final SavingsAccountData savingsAccount = this.savingsAccountReadPlatformService.retrieveOne(accountId);

        final Set<String> mandatoryResponseParameters = new HashSet<>();
        final SavingsAccountData savingsAccountTemplate = populateTemplateAndAssociationsWithoutAccuralTransactions(accountId,
                savingsAccount, staffInSelectedOfficeOnly, chargeStatus, uriInfo, mandatoryResponseParameters, pageNumber, pageSize);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters(),
                mandatoryResponseParameters);
        return this.toApiJsonSerializer.serialize(settings, savingsAccountTemplate,
                SavingsApiSetConstants.SAVINGS_ACCOUNT_RESPONSE_DATA_PARAMETERS);
    }

    private SavingsAccountData populateTemplateAndAssociations(final Long accountId, final SavingsAccountData savingsAccount,
            final boolean staffInSelectedOfficeOnly, final String chargeStatus, final UriInfo uriInfo,
            final Set<String> mandatoryResponseParameters, Integer pageNumber, Integer pageSize) {

        Collection<SavingsAccountTransactionData> transactions = null;
        Collection<SavingsAccountChargeData> charges = null;
        Integer transactionCount = null;
        Collection<CodeValueData> blockNarrationsOptions = null;

        final Set<String> associationParameters = ApiParameterHelper.extractAssociationsForResponseIfProvided(uriInfo.getQueryParameters());
        if (!associationParameters.isEmpty()) {

            if (associationParameters.contains("all")) {
                associationParameters.addAll(
                        Arrays.asList(SavingsApiConstants.transactions, SavingsApiConstants.charges, SavingsApiConstants.blockNarrations));
            }

            if (associationParameters.contains(SavingsApiConstants.transactions)
                    || associationParameters.contains(SavingsApiConstants.accrualTransactions)) {
                mandatoryResponseParameters.add(SavingsApiConstants.transactions);
                SavingsAccountTransactionType type = null;
                if (associationParameters.contains(SavingsApiConstants.accrualTransactions)) {
                    type = SavingsAccountTransactionType.ACCRUAL_INTEREST_POSTING;
                }
                if (pageNumber != null && pageSize != null) {
                    SearchParameters searchParameters = SearchParameters.forPagination(pageNumber, pageSize);
                    final Page<SavingsAccountTransactionData> savingsAccountTransactionData = this.savingsAccountReadPlatformService
                            .retrieveAllSavingAccTransactionsWithoutAccural(accountId, searchParameters, null, type);
                    Collection<SavingsAccountTransactionData> currentTransactions = savingsAccountTransactionData.getPageItems();
                    if (!CollectionUtils.isEmpty(currentTransactions)) {
                        transactions = currentTransactions;
                    } else {
                        transactions = new ArrayList<>();
                    }
                    transactionCount = savingsAccountTransactionData.getTotalFilteredRecords();
                } else {
                    final Collection<SavingsAccountTransactionData> currentTransactions = this.savingsAccountReadPlatformService
                            .retrieveAllTransactionsWithoutAccural(accountId, DepositAccountType.SAVINGS_DEPOSIT, null, null);
                    if (!CollectionUtils.isEmpty(currentTransactions)) {
                        transactions = currentTransactions;
                    }
                    if (transactions != null) {
                        transactionCount = transactions.size();
                    }
                }

            }
            if (associationParameters.contains(SavingsApiConstants.charges)) {
                mandatoryResponseParameters.add(SavingsApiConstants.charges);
                final Collection<SavingsAccountChargeData> currentCharges = this.savingsAccountChargeReadPlatformService
                        .retrieveSavingsAccountCharges(accountId, chargeStatus);
                if (!CollectionUtils.isEmpty(currentCharges)) {
                    charges = currentCharges;
                }
            }

            if (associationParameters.contains(SavingsApiConstants.blockNarrations)) {
                mandatoryResponseParameters.add(SavingsApiConstants.blockNarrations);
                blockNarrationsOptions = this.codeValueReadPlatformService
                        .retrieveCodeValuesByCode(AccountingConstants.BLOCK_UNBLOCK_OPTION_CODE_NAME);

            }
        }

        SavingsAccountData templateData = null;
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        if (settings.isTemplate()) {
            templateData = this.savingsAccountReadPlatformService.retrieveTemplate(savingsAccount.getClientId(), savingsAccount.groupId(),
                    savingsAccount.productId(), staffInSelectedOfficeOnly);
        }

        SavingsAccountData savingsAccountData = SavingsAccountData.withTemplateOptions(savingsAccount, templateData, transactions, charges,
                blockNarrationsOptions);
        savingsAccountData.setTransactionCount(transactionCount);
        return savingsAccountData;
    }

    private SavingsAccountData populateTemplateAndAssociationsWithoutAccuralTransactions(final Long accountId,
            final SavingsAccountData savingsAccount, final boolean staffInSelectedOfficeOnly, final String chargeStatus,
            final UriInfo uriInfo, final Set<String> mandatoryResponseParameters, Integer pageNumber, Integer pageSize) {

        Collection<SavingsAccountTransactionData> transactions = null;
        Collection<SavingsAccountChargeData> charges = null;
        Integer transactionCount = null;

        final Set<String> associationParameters = ApiParameterHelper.extractAssociationsForResponseIfProvided(uriInfo.getQueryParameters());
        if (!associationParameters.isEmpty()) {
            String ids = this.savingsAccountReadPlatformService.fetchContraEntryAndReversalTransaction(accountId);
            if (associationParameters.contains("all")) {
                associationParameters.addAll(Arrays.asList(SavingsApiConstants.transactions, SavingsApiConstants.charges));
            }

            if (associationParameters.contains(SavingsApiConstants.transactions)) {
                mandatoryResponseParameters.add(SavingsApiConstants.transactions);
                if (pageNumber != null && pageSize != null) {
                    SearchParameters searchParameters = SearchParameters.forPagination(pageNumber, pageSize);
                    final Page<SavingsAccountTransactionData> savingsAccountTransactionData = this.savingsAccountReadPlatformService
                            .retrieveAllSavingAccTransactionsWithoutAccural(accountId, searchParameters, ids, null);
                    Collection<SavingsAccountTransactionData> currentTransactions = savingsAccountTransactionData.getPageItems();
                    if (!CollectionUtils.isEmpty(currentTransactions)) {
                        transactions = currentTransactions;
                    } else {
                        transactions = new ArrayList<>();
                    }
                    transactionCount = savingsAccountTransactionData.getTotalFilteredRecords();
                } else {

                    final Collection<SavingsAccountTransactionData> currentTransactions = this.savingsAccountReadPlatformService
                            .retrieveAllTransactionsWithoutAccural(accountId, DepositAccountType.SAVINGS_DEPOSIT,
                                    SavingsAccountTransactionType.ACCRUAL_INTEREST_POSTING, ids);
                    if (!CollectionUtils.isEmpty(currentTransactions)) {
                        transactions = currentTransactions;
                    }
                    if (transactions != null) {
                        transactionCount = transactions.size();
                    }
                }

            }

            if (associationParameters.contains(SavingsApiConstants.charges)) {
                mandatoryResponseParameters.add(SavingsApiConstants.charges);
                final Collection<SavingsAccountChargeData> currentCharges = this.savingsAccountChargeReadPlatformService
                        .retrieveSavingsAccountCharges(accountId, chargeStatus);
                if (!CollectionUtils.isEmpty(currentCharges)) {
                    charges = currentCharges;
                }
            }
        }

        SavingsAccountData templateData = null;
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        if (settings.isTemplate()) {
            templateData = this.savingsAccountReadPlatformService.retrieveTemplate(savingsAccount.getClientId(), savingsAccount.groupId(),
                    savingsAccount.productId(), staffInSelectedOfficeOnly);
        }

        SavingsAccountData savingsAccountData = SavingsAccountData.withTemplateOptions(savingsAccount, templateData, transactions, charges);
        savingsAccountData.setTransactionCount(transactionCount);
        return savingsAccountData;
    }

    @PUT
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String update(@PathParam("accountId") final Long accountId, final String apiRequestBodyAsJson,
            @QueryParam("command") final String commandParam) {

        if (is(commandParam, "updateWithHoldTax")) {
            final CommandWrapper commandRequest = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson).updateWithHoldTax(accountId)
                    .build();
            final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
            return this.toApiJsonSerializer.serialize(result);
        } else if (is(commandParam, "updateNickName")) {
            final CommandWrapper commandRequest = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson).updateNickname(accountId)
                    .build();
            final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
            return this.toApiJsonSerializer.serialize(result);
        }else if (is(commandParam, "updateInterestRate")) {
            final CommandWrapper commandRequest = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson).updateInterestRate(accountId)
                    .build();
            final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
            return this.toApiJsonSerializer.serialize(result);
        }

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateSavingsAccount(accountId).withJson(apiRequestBodyAsJson)
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("modifytransactionrequest")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String adjustTransaction(final String apiRequestBodyAsJson) {

        String jsonApiRequest = apiRequestBodyAsJson;
        if (StringUtils.isBlank(jsonApiRequest)) {
            jsonApiRequest = "{}";
        }
        JSONObject jsonObject = new JSONObject(jsonApiRequest);
        if (jsonObject.has("transactionId")) {
            Long transactionId = jsonObject.getLong("transactionId");
            SavingsAccountTransactionData savingsAccountTransaction = this.savingsAccountReadPlatformService
                    .retrieveSavingsTransaction(transactionId);
            final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(jsonApiRequest);
            final CommandWrapper commandRequest = builder
                    .modifySavingsTransactionRequest(savingsAccountTransaction.getSavingsAccountId(), transactionId).build();
            CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
            return this.toApiJsonSerializer.serialize(result);
        } else {
            List<ApiParameterError> errors = new ArrayList<>();
            errors.add(ApiParameterError.generalError("transactionId.missing.from.payload", "transactionId is required in the payload"));
            throw new PlatformApiDataValidationException(errors);
        }
    }

    @POST
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String handleCommands(@PathParam("accountId") final Long accountId, @QueryParam("command") final String commandParam,
            final String apiRequestBodyAsJson) {

        String jsonApiRequest = apiRequestBodyAsJson;
        if (StringUtils.isBlank(jsonApiRequest)) {
            jsonApiRequest = "{}";
        }

        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(jsonApiRequest);

        CommandProcessingResult result = null;
        if (is(commandParam, "reject")) {
            final CommandWrapper commandRequest = builder.rejectSavingsAccountApplication(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "withdrawnByApplicant")) {
            final CommandWrapper commandRequest = builder.withdrawSavingsAccountApplication(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "approve")) {
            final CommandWrapper commandRequest = builder.approveSavingsAccountApplication(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "undoapproval")) {
            final CommandWrapper commandRequest = builder.undoSavingsAccountApplication(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "activate")) {
            final CommandWrapper commandRequest = builder.savingsAccountActivation(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "calculateInterest")) {
            final CommandWrapper commandRequest = builder.withNoJsonBody().savingsAccountInterestCalculation(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "postInterest")) {
            final CommandWrapper commandRequest = builder.savingsAccountInterestPosting(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "applyAnnualFees")) {
            final CommandWrapper commandRequest = builder.savingsAccountApplyAnnualFees(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "close")) {
            final CommandWrapper commandRequest = builder.closeSavingsAccountApplication(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "assignSavingsOfficer")) {
            final CommandWrapper commandRequest = builder.assignSavingsOfficer(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
            return this.toApiJsonSerializer.serialize(result);
        } else if (is(commandParam, "unassignSavingsOfficer")) {
            final CommandWrapper commandRequest = builder.unassignSavingsOfficer(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
            return this.toApiJsonSerializer.serialize(result);
        } else if (is(commandParam, "applyOverdraft")) {
            final CommandWrapper commandRequest = builder.savingsAccountApplyOverdraft(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, SavingsApiConstants.COMMAND_BLOCK_DEBIT)) {
            final CommandWrapper commandRequest = builder.blockDebitsFromSavingsAccount(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, SavingsApiConstants.COMMAND_UNBLOCK_DEBIT)) {
            final CommandWrapper commandRequest = builder.unblockDebitsFromSavingsAccount(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, SavingsApiConstants.COMMAND_BLOCK_CREDIT)) {
            final CommandWrapper commandRequest = builder.blockCreditsToSavingsAccount(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, SavingsApiConstants.COMMAND_UNBLOCK_CREDIT)) {
            final CommandWrapper commandRequest = builder.unblockCreditsToSavingsAccount(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, SavingsApiConstants.COMMAND_BLOCK_ACCOUNT)) {
            final CommandWrapper commandRequest = builder.withNoJsonBody().blockSavingsAccount(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, SavingsApiConstants.COMMAND_UNBLOCK_ACCOUNT)) {
            final CommandWrapper commandRequest = builder.withNoJsonBody().unblockSavingsAccount(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "postAccrualInterestAsOn")) {
            final CommandWrapper commandRequest = builder.savingsAccountAccrualInterestPosting(accountId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (result == null) {
            //
            throw new UnrecognizedQueryParamException("command", commandParam,
                    new Object[] { "reject", "withdrawnByApplicant", "approve", "undoapproval", "activate", "calculateInterest",
                            "postInterest", "close", "assignSavingsOfficer", "unassignSavingsOfficer",
                            SavingsApiConstants.COMMAND_BLOCK_DEBIT, SavingsApiConstants.COMMAND_UNBLOCK_DEBIT,
                            SavingsApiConstants.COMMAND_BLOCK_CREDIT, SavingsApiConstants.COMMAND_UNBLOCK_CREDIT,
                            SavingsApiConstants.COMMAND_BLOCK_ACCOUNT, SavingsApiConstants.COMMAND_UNBLOCK_ACCOUNT });
        }

        return this.toApiJsonSerializer.serialize(result);
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }

    @DELETE
    @Path("{accountId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String delete(@PathParam("accountId") final Long accountId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteSavingsAccount(accountId).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("downloadtemplate")
    @Produces("application/vnd.ms-excel")
    public Response getSavingsTemplate(@QueryParam("officeId") final Long officeId, @QueryParam("staffId") final Long staffId,
            @QueryParam("dateFormat") final String dateFormat) {
        return bulkImportWorkbookPopulatorService.getTemplate(GlobalEntityType.SAVINGS_ACCOUNT.toString(), officeId, staffId, dateFormat);
    }

    @POST
    @Path("uploadtemplate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String postSavingsTemplate(@FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("locale") final String locale,
            @FormDataParam("dateFormat") final String dateFormat) {
        final Long importDocumentId = this.bulkImportWorkbookService.importWorkbook(GlobalEntityType.SAVINGS_ACCOUNT.toString(),
                uploadedInputStream, fileDetail, locale, dateFormat);
        return this.toApiJsonSerializer.serialize(importDocumentId);
    }

    @GET
    @Path("transactions/downloadtemplate")
    @Produces("application/vnd.ms-excel")
    public Response getSavingsTransactionTemplate(@QueryParam("officeId") final Long officeId,
            @QueryParam("dateFormat") final String dateFormat) {
        return bulkImportWorkbookPopulatorService.getTemplate(GlobalEntityType.SAVINGS_TRANSACTIONS.toString(), officeId, null, dateFormat);
    }

    @POST
    @Path("transactions/uploadtemplate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String postSavingsTransactionTemplate(@FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("locale") final String locale,
            @FormDataParam("dateFormat") final String dateFormat) {
        final Long importDocumentId = this.bulkImportWorkbookService.importWorkbook(GlobalEntityType.SAVINGS_TRANSACTIONS.toString(),
                uploadedInputStream, fileDetail, locale, dateFormat);
        return this.toApiJsonSerializer.serialize(importDocumentId);
    }
}