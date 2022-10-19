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
ALTER TABLE `m_loan_reschedule_request`
    ADD COLUMN `is_restructure_request` tinyint(1) NULL DEFAULT 0 AFTER `rejected_by_user_id`,
    ADD COLUMN `reschedule_to_date` date NULL AFTER `is_restructure_request`;

INSERT INTO `m_permission` (`grouping`, `code`, `entity_name`, `action_name`, `can_maker_checker`) VALUES ('loan_reschedule', 'READ_RESTRUCTURELOAN', 'RESTRUCTURELOAN', 'READ', 0);
INSERT INTO `m_permission` ( `grouping`, `code`, `entity_name`, `action_name`, `can_maker_checker`) VALUES ('loan_reschedule', 'CREATE_RESTRUCTURELOAN', 'RESTRUCTURELOAN', 'CREATE', 0);
INSERT INTO `m_permission` ( `grouping`, `code`, `entity_name`, `action_name`, `can_maker_checker`) VALUES ('loan_reschedule', 'REJECT_RESTRUCTURELOAN', 'RESTRUCTURELOAN', 'REJECT', 0);
INSERT INTO `m_permission` ( `grouping`, `code`, `entity_name`, `action_name`, `can_maker_checker`) VALUES ('loan_reschedule', 'APPROVE_RESTRUCTURELOAN', 'RESTRUCTURELOAN', 'APPROVE', 0);

