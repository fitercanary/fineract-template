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
package org.apache.fineract.useradministration.service;

public class AppUserConstants {

    public static final String PASSWORD_NEVER_EXPIRES = "passwordNeverExpires";
    public static final String IS_SELF_SERVICE_USER = "isSelfServiceUser";
    public static final String CLIENTS = "clients";
    
    // request parameters for authorize user
    public static final String clientIdParamName = "clientId";
    public static final String userIdParamName = "userId";
    public static final String startTimeParamName = "startTime";
    public static final String endTimeParamName = "endTime";
    public static final String isExpiredParamName = "isExpired";
    public static final String authorizedByParamName = "authorizedBy";
    public static final String commentParamName = "comment";
    public static final String durationParamName = "duration";
    public static final String durationTypeParamName = "durationType";
    
    // general
    public static final String localeParamName = "locale";
    public static final String dateFormatParamName = "dateFormat";

}
