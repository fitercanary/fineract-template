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
package org.apache.fineract.infrastructure.jobs.service;

import java.util.Random;

import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.service.TenantDetailsService;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
public class SchedulerTriggerListener implements TriggerListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerTriggerListener.class);

    private final String name = "Global trigger Listner";

    private final SchedularWritePlatformService schedularService;

    private final TenantDetailsService tenantDetailsService;

    @Value("${fineract.allowJobs:true}")
    private volatile @NotNull
    boolean allowJobs;

    @Autowired
    public SchedulerTriggerListener(final SchedularWritePlatformService schedularService, final TenantDetailsService tenantDetailsService) {
        this.schedularService = schedularService;
        this.tenantDetailsService = tenantDetailsService;

    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void triggerFired(@SuppressWarnings("unused") final Trigger trigger,
                             @SuppressWarnings("unused") final JobExecutionContext context) {

    }

    @Override
    public boolean vetoJobExecution(final Trigger trigger, final JobExecutionContext context) {
        if (allowJobs) {
            final String tenantIdentifier = trigger.getJobDataMap().getString(SchedulerServiceConstants.TENANT_IDENTIFIER);
            final FineractPlatformTenant tenant = this.tenantDetailsService.loadTenantById(tenantIdentifier);
            ThreadLocalContextUtil.setTenant(tenant);
            final JobKey key = trigger.getJobKey();
            final String jobKey = key.getName() + SchedulerServiceConstants.JOB_KEY_SEPERATOR + key.getGroup();
            String triggerType = SchedulerServiceConstants.TRIGGER_TYPE_CRON;
            if (context.getMergedJobDataMap().containsKey(SchedulerServiceConstants.TRIGGER_TYPE_REFERENCE)) {
                triggerType = context.getMergedJobDataMap().getString(SchedulerServiceConstants.TRIGGER_TYPE_REFERENCE);
            }
            int maxNumberOfRetries = ThreadLocalContextUtil.getTenant().getConnection().getMaxRetriesOnDeadlock();
            int maxIntervalBetweenRetries = ThreadLocalContextUtil.getTenant().getConnection().getMaxIntervalBetweenRetries();
            int numberOfRetries = 0;
            boolean proceedJob = false;
            while (numberOfRetries <= maxNumberOfRetries) {
                try {
                    proceedJob = this.schedularService.processJobDetailForExecution(jobKey, triggerType);
                    numberOfRetries = maxNumberOfRetries + 1;
                } catch (Exception exception) { //Adding generic exception as it depends on JPA provider
                    LOGGER.debug("Not able to acquire the lock to update job running status for JobKey: {}", jobKey);
                    try {
                        Random random = new Random();
                        int randomNum = random.nextInt(maxIntervalBetweenRetries + 1);
                        Thread.sleep(1000 + (randomNum * 1000));
                        numberOfRetries = numberOfRetries + 1;
                    } catch (InterruptedException e) {
                        LOGGER.error(e.getMessage());
                    }
                }
            }
            return proceedJob;
        }
        return false;
    }

    @Override
    public void triggerMisfired(@SuppressWarnings("unused") final Trigger trigger) {

    }

    @Override
    public void triggerComplete(@SuppressWarnings("unused") final Trigger trigger,
                                @SuppressWarnings("unused") final JobExecutionContext context,
                                @SuppressWarnings("unused") final CompletedExecutionInstruction triggerInstructionCode) {

    }
}
