/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.api;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModel;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.RescheduleLoansApiConstants;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.RestructureLoansApiConstants;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.LoanRescheduleRequestData;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.LoanRestructureScheduleDetails;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequest;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequestRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.service.LoanPartLiquidationPreviewPlatformService;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.service.LoanRescheduleRequestReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.service.LoanRestructurePreviewPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.HashSet;

@Path("/restructureloans/{loanId}")
@Component
@Scope("singleton")
public class ResctructureLoansApiResource {

    private final DefaultToApiJsonSerializer<LoanRescheduleRequestData> loanRescheduleRequestToApiJsonSerializer;
    private final DefaultToApiJsonSerializer<LoanScheduleData> loanRescheduleToApiJsonSerializer;
    private final PlatformSecurityContext platformSecurityContext;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final LoanRescheduleRequestReadPlatformService loanRescheduleRequestReadPlatformService;
    private final LoanRestructurePreviewPlatformService loanRestructurePreviewPlatformService;
    private final LoanPartLiquidationPreviewPlatformService loanPartLiquidationPreviewPlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanRescheduleRequestRepositoryWrapper loanRescheduleRequestRepositoryWrapper;

    @Autowired
    public ResctructureLoansApiResource(final DefaultToApiJsonSerializer<LoanRescheduleRequestData> loanRescheduleRequestToApiJsonSerializer,
                                        final PlatformSecurityContext platformSecurityContext,
                                        final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
                                        final LoanRescheduleRequestReadPlatformService loanRescheduleRequestReadPlatformService,
                                        final ApiRequestParameterHelper apiRequestParameterHelper,
                                        final DefaultToApiJsonSerializer<LoanScheduleData> loanRescheduleToApiJsonSerializer,
                                        final LoanRestructurePreviewPlatformService loanRestructurePreviewPlatformService,
                                        final LoanRescheduleRequestRepositoryWrapper loanRescheduleRequestRepositoryWrapper,
                                        final LoanRepositoryWrapper loanRepositoryWrapper,
                                        final LoanPartLiquidationPreviewPlatformService loanPartLiquidationPreviewPlatformService,                                        LoanReadPlatformService loanReadPlatformService) {
        this.loanRescheduleRequestToApiJsonSerializer = loanRescheduleRequestToApiJsonSerializer;
        this.platformSecurityContext = platformSecurityContext;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.loanRescheduleRequestReadPlatformService = loanRescheduleRequestReadPlatformService;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.loanRescheduleToApiJsonSerializer = loanRescheduleToApiJsonSerializer;
        this.loanRestructurePreviewPlatformService = loanRestructurePreviewPlatformService;
        this.loanReadPlatformService = loanReadPlatformService;
        this.loanRescheduleRequestRepositoryWrapper = loanRescheduleRequestRepositoryWrapper;
        this.loanRepositoryWrapper = loanRepositoryWrapper;
        this.loanPartLiquidationPreviewPlatformService = loanPartLiquidationPreviewPlatformService;
    }

    @GET
    @Path("template")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public String retrieveTemplate(@PathParam("loanId") final Long loanId, @Context final UriInfo uriInfo) {

        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(RescheduleLoansApiConstants.ENTITY_NAME);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        LoanRescheduleRequestData rescheduleRequestData = null;

        LoanRestructureScheduleDetails restructureScheduleDetails = this.loanReadPlatformService.retrieveInstallmentDetails(loanId, LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue());
        LoanRescheduleRequest loanRescheduleRequest = null;

        LoanTransactionData loanTransactionData = this.loanReadPlatformService.retrieveLoanPartLiquidationTemplate(loanId, DateUtils.getLocalDateOfTenant());

        rescheduleRequestData = this.loanRescheduleRequestReadPlatformService
                .retrieveAllRescheduleReasons(RescheduleLoansApiConstants.LOAN_RESCHEDULE_REASON, loanTransactionData,
                        restructureScheduleDetails, loanRescheduleRequest);



        return this.loanRescheduleRequestToApiJsonSerializer.serialize(settings, rescheduleRequestData);
    }

    @GET
    @Path("{requestId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public String readLoanRescheduleRequest(
            @Context final UriInfo uriInfo, @PathParam("loanId") final Long loanId,
            @PathParam("requestId") final Long requestId, @QueryParam("command") final String command) {
        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(RestructureLoansApiConstants.ENTITY_NAME);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        if (compareIgnoreCase(command, "previewRestructureRequest")) {
            final LoanScheduleModel loanRescheduleModel = this.loanRestructurePreviewPlatformService.previewLoanRestructure(requestId);

            return this.loanRescheduleToApiJsonSerializer.serialize(settings, loanRescheduleModel.toData(), new HashSet<String>());
        }

        final LoanRescheduleRequestData loanRescheduleRequestData = this.loanRescheduleRequestReadPlatformService
                .readLoanRescheduleRequest(requestId);

        return this.loanRescheduleRequestToApiJsonSerializer.serialize(settings, loanRescheduleRequestData);
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public String createLoanRescheduleRequest(@PathParam("loanId") final Long loanId, @QueryParam("command") final String command,
                                              final String apiRequestBodyAsJson) {
        final CommandWrapper commandWrapper = new CommandWrapperBuilder()
                .createLoanRestructureRequest(RestructureLoansApiConstants.ENTITY_NAME).withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult commandProcessingResult = this.commandsSourceWritePlatformService.logCommandSource(commandWrapper);

        return this.loanRescheduleRequestToApiJsonSerializer.serialize(commandProcessingResult);
    }

    @POST
    @Path("{requestId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public String updateLoanRescheduleRequest(@PathParam("loanId") final Long loanId,@PathParam("requestId") final Long requestId, @QueryParam("command") final String command,
                                              final String apiRequestBodyAsJson) {
        CommandWrapper commandWrapper = null;

        if (compareIgnoreCase(command, "approve")) {
            commandWrapper = new CommandWrapperBuilder().approveLoanRestructureRequest(RestructureLoansApiConstants.ENTITY_NAME, requestId)
                    .withJson(apiRequestBodyAsJson).build();
        } else if (compareIgnoreCase(command, "reject")) {
            commandWrapper = new CommandWrapperBuilder().rejectLoanRescheduleRequest(RestructureLoansApiConstants.ENTITY_NAME, requestId)
                    .withJson(apiRequestBodyAsJson).build();
        } else {
            throw new UnrecognizedQueryParamException("command", command, new Object[]{"approve", "reject", "restructure"});
        }

        final CommandProcessingResult commandProcessingResult = this.commandsSourceWritePlatformService.logCommandSource(commandWrapper);

        return this.loanRescheduleRequestToApiJsonSerializer.serialize(commandProcessingResult);
    }

    @POST
    @Path("/partLiquidate")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public String partLiquidateLoan(@PathParam("loanId") final Long loanId, @QueryParam("command") final String command,
                                              final String apiRequestBodyAsJson) {
        CommandWrapper commandWrapper = new CommandWrapperBuilder().confirmPartLiquidateLoan(loanId)
                .withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult commandProcessingResult = this.commandsSourceWritePlatformService.logCommandSource(commandWrapper);

        return this.loanRescheduleRequestToApiJsonSerializer.serialize(commandProcessingResult);
    }


    @GET
    @Path("previewPartLiquidation")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public String previewPartLiquidation(@PathParam("loanId") final Long loanId,
                                         @Context final UriInfo uriInfo) {
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(queryParameters);


        final LoanScheduleModel loanRescheduleModel = this.loanPartLiquidationPreviewPlatformService.previewLoanPartLiquidation(loanId, queryParameters);

        return this.loanRescheduleToApiJsonSerializer.serialize(settings, loanRescheduleModel.toData(), new HashSet<String>());
    }

    /**
     * Compares two strings, ignoring differences in case
     *
     * @param firstString
     *            the first string
     * @param secondString
     *            the second string
     * @return true if the two strings are equal, else false
     **/
    private boolean compareIgnoreCase(String firstString, String secondString) {
        return StringUtils.isNotBlank(firstString) && firstString.trim().equalsIgnoreCase(secondString);
    }
}
