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


CREATE TABLE `m_validation_limits` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `client_level_id` int(20) NOT NULL,
  `maximum_single_deposit_amount` bigint(50) DEFAULT NULL,
  `maximum_cumulative_balance` bigint(50) DEFAULT NULL,
  `maximum_transaction_limit` bigint(20) DEFAULT NULL,
  `maximum_daily_transaction_amount_limit` bigint(20) DEFAULT NULL,
 
  PRIMARY KEY (`id`)
);

INSERT INTO `m_permission`
(`grouping`,`code`,`entity_name`,`action_name`,`can_maker_checker`)

VALUES ('product', 'READ_VALIDATIONLIMIT', 'VALIDATIONLIMITS', 'READ', 0),('product', 'CREATE_VALIDATIONLIMIT', 'VALIDATIONLIMITS', 'CREATE', 0),('product', 'CREATE_VALIDATIONLIMIT_CHECKER', 'VALIDATIONLIMITS', 'CREATE_CHECKER', 0), ('product', 'UPDATE_VALIDATIONLIMIT', 'VALIDATIONLIMITS', 'UPDATE', 0),('product', 'UPDATE_VALIDATIONLIMIT_CHECKER', 'VALIDATIONLIMITS', 'UPDATE_CHECKER', 0);

