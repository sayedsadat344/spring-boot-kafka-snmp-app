package com.traps.RoshanNOCTraps.traps.zte;

import lombok.Data;

@Data
public class ZteTrapBody {

    private Long id;
    private Long newOrClear;
    private String trapId;

    private String alarmCode;

    private String alarmName;

    private String siteId;

    private String siteName;

    private Long alarmSeverity;

    private Long alarmEventType;

    private String alarmNetType;

    private String alarmServiceType;

    private String displaySiteId;

    private String alarmFaultCause;

    private Long alarmFaultClass;

    private String alarmArrivalTime;

    private String alarmClearedTime;

    private String alarmNodeBId;

    private String alarmCellId;

    private String alarmRncId;
}
