package com.traps.RoshanNOCTraps.traps.hw;

import lombok.Data;
import org.snmp4j.smi.OID;

import java.util.Arrays;
import java.util.List;

@Data
public class HwOidConstants {

    public static List<Long> alarmIdList = Arrays.asList(
            21807L, 22214L, 65080L, 65070L, 65501L,
            65033L, 29201L, 25622L,
            65067L, 5700L, 65081L, 25621L,
            65059L, 65071L, 21825L, 65069L,65090L
    );

//    65334L BSC mains failure (no site id at all)
//    65084L Nodeb air condition alarm
//    65381L MSC Mains Failure (no site at all)
//     65502Lrectifier failure

//     65068L rectifier not needed
//    List<Long> alarmIdList = Arrays.asList(
//        65068L
//
//    );



    public static final String FILE_PATH = "hw-output.txt";

    // Trap OID
    public static final OID SNMP_TRAP_OID = new OID("1.3.6.1.6.3.1.1.4.1.0");

    // Huawei Alarm Trap OIDs
    public static final OID HW_INTENDED_ALARM = new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.9.0");
    public static final OID HW_TRAP_ID = new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.1.0");
    public static final OID HW_ALARM_CLEARED_TIME = new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.15.0");
    public static final OID HW_CLEAR_OR_NOT = new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.12.0");
    public static final OID HW_EVENT_TIME = new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.3.0");


    public static final OID HW_SITE_NAME = new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.4.0");
    public static final OID HW_OBJECT_INSTANCE_NAME = new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.27.0");
    public static final OID HW_ALARM_ARRIVAL_TIME = new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.3.0");
    public static final OID HW_ALARM_NAME = new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.28.0");
    public static final OID HW_ALARM_EVENT_TYPE = new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.10.0");
    public static final OID HW_ALARM_NET_TYPE = new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.6.0");
    public static final OID HW_ALARM_SEVERITY = new OID("1.3.6.1.4.1.2011.2.15.2.4.3.3.11.0");
}
