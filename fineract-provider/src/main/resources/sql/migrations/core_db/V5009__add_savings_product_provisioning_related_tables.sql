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

CREATE TABLE `m_savings_product_provisioning_entry` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `history_id` bigint(20) NOT NULL,
  `criteria_id` bigint(20) NOT NULL,
  `currency_code` varchar(3) NOT NULL,
  `office_id` bigint(20) NOT NULL,
  `product_id` bigint(20) NOT NULL,
  `category_id` bigint(20) NOT NULL,
  `overdue_in_days` bigint(20) DEFAULT '0',
  `reseve_amount` decimal(20,6) DEFAULT '0.000000',
  `liability_account` bigint(20) DEFAULT NULL,
  `expense_account` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `history_id` (`history_id`),
  KEY `criteria_id` (`criteria_id`),
  KEY `office_id` (`office_id`),
  KEY `product_id` (`product_id`),
  KEY `category_id` (`category_id`),
  KEY `liability_account` (`liability_account`),
  KEY `expense_account` (`expense_account`),
  CONSTRAINT `m_savings_product_provisioning_entry_ibfk_1` FOREIGN KEY (`history_id`) REFERENCES `m_provisioning_history` (`id`),
  CONSTRAINT `m_savings_product_provisioning_entry_ibfk_2` FOREIGN KEY (`criteria_id`) REFERENCES `m_provisioning_criteria` (`id`),
  CONSTRAINT `m_savings_product_provisioning_entry_ibfk_3` FOREIGN KEY (`office_id`) REFERENCES `m_office` (`id`),
  CONSTRAINT `m_savings_product_provisioning_entry_ibfk_4` FOREIGN KEY (`product_id`) REFERENCES `m_savings_product` (`id`),
  CONSTRAINT `m_savings_product_provisioning_entry_ibfk_5` FOREIGN KEY (`category_id`) REFERENCES `m_provision_category` (`id`),
  CONSTRAINT `m_savings_product_provisioning_entry_ibfk_6` FOREIGN KEY (`liability_account`) REFERENCES `acc_gl_account` (`id`),
  CONSTRAINT `m_savings_product_provisioning_entry_ibfk_7` FOREIGN KEY (`expense_account`) REFERENCES `acc_gl_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=258 DEFAULT CHARSET=latin1;

CREATE TABLE `m_savings_product_provisioning_mapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `product_id` bigint(20) NOT NULL,
  `criteria_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `product_id` (`product_id`),
  KEY `criteria_id` (`criteria_id`),
  CONSTRAINT `m_savings_product_provisioning_mapping_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `m_savings_product` (`id`),
  CONSTRAINT `m_savings_product_provisioning_mapping_ibfk_2` FOREIGN KEY (`criteria_id`) REFERENCES `m_provisioning_criteria` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=latin1;