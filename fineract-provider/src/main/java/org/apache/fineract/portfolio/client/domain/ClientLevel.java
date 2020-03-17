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
package org.apache.fineract.portfolio.client.domain;

import org.apache.fineract.portfolio.charge.domain.ChargeAppliesTo;

/**
 * Type used to differentiate the type of client
 */
public enum ClientLevel {

    LEVEL_1(1, "clientLevelType.Level-1"),

    LEVEL_2(2, "clientLevelType.Level-2"),

    LEVEL_3(3,"clientLevelType.Level-3");

    private final Integer value;
    private final String code;

    private ClientLevel(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public static ClientLevel fromInt(final Integer type) {

        ClientLevel clientLevel = null;
        switch (type) {
            case 1:
                clientLevel = ClientLevel.LEVEL_1;
            break;
            case 2:
                clientLevel = ClientLevel.LEVEL_2;
            break;
            case 3:
                clientLevel = ClientLevel.LEVEL_3;
            break;
        }
        return clientLevel;
    }

    public boolean isClientLevel_1() {
        return this.value.equals(ClientLevel.LEVEL_1.getValue());
    }

    public boolean isClientLevel_2() {
        return this.value.equals(ClientLevel.LEVEL_2.getValue());
    }
    
    public boolean isClientLevel_3() {
        return this.value.equals(ClientLevel.LEVEL_3.getValue());
    }
    
    public static Object[] validValues() {
        return new Object[] { ClientLevel.LEVEL_1.getValue(), ChargeAppliesTo.SAVINGS.getValue(), ClientLevel.LEVEL_2.getValue(), ClientLevel.LEVEL_3.getValue()};
    }
}
