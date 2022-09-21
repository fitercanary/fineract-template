--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

ALTER TABLE `mifostenant-default`.`m_deposit_account_term_and_preclosure`
ADD COLUMN `maturity_notification_period` int(11) NULL AFTER `pre_closure_charge_applicable`,
ADD COLUMN `maturity_notification_frequency` int(11) NULL AFTER `maturity_notification_period`,
ADD COLUMN `next_maturity_notification_date` date NULL AFTER `maturity_notification_frequency`,
ADD COLUMN `maturity_sms_notification` tinyint(1) NULL DEFAULT 0 AFTER `next_maturity_notification_date`;


INSERT INTO `mifostenant-default`.`job` (`name`, `display_name`, `cron_expression`, `create_time`, `task_priority`, `group_name`, `previous_run_start_time`, `next_run_time`, `job_key`, `initializing_errorlog`, `is_active`, `currently_running`, `updates_allowed`, `scheduler_group`, `is_misfired`) VALUES ('Notify Deposit Accounts Maturity', 'Notify Deposit Accounts Maturity', '0 0 0 1/1 * ? *', NOW(), 5, NULL, null, NOW(), 'Notify Deposit Accounts MaturityJobDetail1 _ DEFAULT', NULL, 1, 0, 1, 0, 0);
