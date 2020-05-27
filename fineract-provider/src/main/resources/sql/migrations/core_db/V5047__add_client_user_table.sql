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

CREATE TABLE `m_client_user` (
  `client_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `duration_type` bigint(20) NOT NULL,
  `duration` bigint(20) NOT NULL,
  `start_time` datetime NULL,
  `end_time` datetime NULL,
  `is_expired` boolean DEFAULT false,
  `authorized_by` bigint(20) NULL,
  `comment` varchar(500) NULL,
  KEY `fk_m_client` (`client_id`),
  KEY `fk_m_appuser` (`user_id`),
  KEY `fk_m_appuser_authorized_by` (`authorized_by`),
  CONSTRAINT `fk_m_client` FOREIGN KEY (`client_id`) REFERENCES `m_client` (`id`),
  CONSTRAINT `fk_m_appuser` FOREIGN KEY (`user_id`) REFERENCES `m_appuser` (`id`),
  CONSTRAINT `fk_m_appuser_authorized_by` FOREIGN KEY (`authorized_by`) REFERENCES `m_appuser` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;