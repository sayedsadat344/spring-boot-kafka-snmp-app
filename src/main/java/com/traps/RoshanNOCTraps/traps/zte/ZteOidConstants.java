package com.traps.RoshanNOCTraps.traps.zte;

import lombok.Data;
import org.snmp4j.smi.OID;

import java.util.Arrays;
import java.util.List;

@Data
public class ZteOidConstants {

    public static List<Long> alarmValues = Arrays.asList(
            199087337L, 198092550L, 198087337L, 198092295L,
            198083023L, 199083023L, 198092562L, 198094422L,
            198099803L, 198200011L, 198200001L,
            1014L, 198094466L, 198200004L,
            198092551L, 198092552L,
            198094420L, 198097604L
    );



    public static final String FILE_PATH = "zte-output.txt";

    // Standard OIDs
    public static final OID SNMP_TRAP_OID = new OID("1.3.6.1.6.3.1.1.4.1.0");

    // From ZTE-ALARM-IRP-MIB
    public static final OID ALARM_EVENT_TIME = new OID("1.3.6.1.4.1.3902.4101.1.3.1.3");
    public static final OID ALARM_CODE = new OID("1.3.6.1.4.1.3902.4101.1.3.1.11");
    public static final OID ALARM_TRAP_ID = new OID("1.3.6.1.4.1.3902.4101.1.3.1.24");
    public static final OID ALARM_NAME = new OID("1.3.6.1.4.1.3902.4101.1.3.1.14");
    public static final OID SITE_NAME = new OID("1.3.6.1.4.1.3902.4101.1.3.1.26");
    public static final OID LOCAL_RNC_ID = new OID("1.3.6.1.4.1.3902.4101.1.3.1.15");
    public static final OID OBJECT_INSTANCE_NAME = new OID("1.3.6.1.4.1.3902.4101.1.3.1.8");
    public static final OID ALARM_EVENT_TYPE = new OID("1.3.6.1.4.1.3902.4101.1.3.1.4");

    // Trap type OIDs
    public static final OID ALARM_NEW_TRAP = new OID("1.3.6.1.4.1.3902.4101.1.4.1.1");
    public static final OID ALARM_CLEARED_TRAP = new OID("1.3.6.1.4.1.3902.4101.1.4.1.2");
    public static final OID ALARM_CLEARED_TRAP_2 = new OID("1.3.6.1.4.1.3902.4101.1.4.1.3");

}