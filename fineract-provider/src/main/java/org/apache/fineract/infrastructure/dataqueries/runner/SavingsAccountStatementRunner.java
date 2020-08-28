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
package org.apache.fineract.infrastructure.dataqueries.runner;

import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.dataqueries.service.ReadReportingService;
import org.apache.fineract.infrastructure.report.provider.ReportingProcessServiceProvider;
import org.apache.fineract.infrastructure.report.service.ReportingProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

public class SavingsAccountStatementRunner implements Runnable{

    private final FineractPlatformTenant tenant;
    private static final Logger logger = LoggerFactory.getLogger(SavingsAccountStatementRunner.class);

    private final ReadReportingService readExtraDataAndReportingService;
    private final ReportingProcessServiceProvider reportingProcessServiceProvider;
    private final boolean parameterType;
    private final String reportName;
    private final UriInfo uriInfo;
    private final MultivaluedMap<String, String> queryParams;
    private final SecurityContext context;

    public SavingsAccountStatementRunner(final FineractPlatformTenant tenant, final SecurityContext context,
                                         final ReadReportingService readExtraDataAndReportingService,
                                         final ReportingProcessServiceProvider reportingProcessServiceProvider,
                                         final boolean parameterType, final String reportName, UriInfo uriInfo,
                                         final MultivaluedMap<String, String> queryParams) {
        this.tenant = tenant;
        this.readExtraDataAndReportingService = readExtraDataAndReportingService;
        this.reportingProcessServiceProvider = reportingProcessServiceProvider;
        this.parameterType = parameterType;
        this.reportName = reportName;
        this.uriInfo = uriInfo;
        this.queryParams = queryParams;
        this.context = context;
    }

    @Override
    public void run() {

        logger.info("Starting run, thread runner");

        ThreadLocalContextUtil.setTenant(this.tenant);
        SecurityContextHolder.setContext(this.context);

        if (!parameterType) {
            String reportType = this.readExtraDataAndReportingService.getReportType(reportName);
            ReportingProcessService reportingProcessService = this.reportingProcessServiceProvider.findReportingProcessService(reportType);
            if (reportingProcessService != null) {
                reportingProcessService.processAndSendStatement(reportName, queryParams);
            }
        }
    }

    public void start() {
        logger.info("Starting SavingsAccountStatementRunner...");
        Thread executorThread = new Thread(this);
        executorThread.start();
    }
}
