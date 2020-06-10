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
package org.apache.fineract.useradministration.domain;


public enum DurationType {

    INVALID(0, "durationType.invalid", "invalid"), //
    HOURS(1, "durationType.invalid", "invalid"), //
    DAYS(2, "durationType.hours", "Hours"), //
    WEEKS(3, "durationType.days", "Days"), //
    MONTHS(4, "durationType.weeks", "Week"), //
    YEARS(5, "durationType.months", "Months");
    
    private final Integer value;
    private final String code;
    private final String name;
    
    public static DurationType fromInt(final Integer type) {
        
        DurationType enumeration = DurationType.INVALID;
        
        switch (type) {
            case 1:
                enumeration = DurationType.HOURS;
            break;
            case 2:
                enumeration = DurationType.DAYS;
            break;
            case 3:
                enumeration = DurationType.WEEKS;
            break;
            case 4:
                enumeration = DurationType.MONTHS;
            break;
            case 5:
                enumeration = DurationType.YEARS;
            break;
        }
        return enumeration;
    }
    
    private  DurationType(final Integer value, final String code, final String name) {
        this.value = value;
        this.code  = code;
        this.name = name;
        
    }
    
    public boolean hasStateOf(final DurationType state) {
        return this.value.equals(state.getValue());
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }
    
    public String getName() {
        return this.name;
    }
    
    public boolean isHours() {
        return this.value.equals(DurationType.HOURS.getValue());
    }
    
    public boolean isDays() {
        return this.value.equals(DurationType.DAYS.getValue());
    }
    
    public boolean isWeeks() {
        return this.value.equals(DurationType.WEEKS.getValue());
    }
    
    public boolean isMonths() {
        return this.value.equals(DurationType.MONTHS.getValue());
    }
    
    public boolean isYears() {
        return this.value.equals(DurationType.YEARS.getValue());
    }
}
