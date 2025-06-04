package com.traps.RoshanNOCTraps.traps.hw;

import com.mycompany.app.sharedClasses.BssHwTrapBody;
import com.traps.RoshanNOCTraps.RoshanNocTrapsManagementApplication;
import lombok.Data;
import org.springframework.boot.SpringApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class HwData {

    public static List<BssHwTrapBody> getData(){
        BssHwTrapBody trap1 = new BssHwTrapBody();
        trap1.setId(null);
        trap1.setNewOrClear(1L);
        trap1.setTrapId("384990965");
        trap1.setAlarmCode("22214");
        trap1.setAlarmName("NodeB Unavailable");
        trap1.setSiteId("KBL552");
        trap1.setSiteName("KBLU552_Qargha Sar_e_Kariz_Etisalat_(KBL416)");
        trap1.setAlarmSeverity(2L);
        trap1.setAlarmEventType(7L);
        trap1.setAlarmNetType("172");
        trap1.setAlarmServiceType("3G");
        trap1.setDisplaySiteId("KBLU552");
        trap1.setAlarmFaultCause(null);
        trap1.setAlarmFaultClass(null);
        trap1.setAlarmArrivalTime("2024-09-24 10:20:34");
        trap1.setAlarmClearedTime("");
        trap1.setAlarmNodeBId(null);
        trap1.setAlarmCellId(null);
        trap1.setAlarmRncId(null);

        BssHwTrapBody trap2 = new BssHwTrapBody();
        trap2.setId(null);
        trap2.setNewOrClear(2L);
        trap2.setTrapId("384990965");
        trap2.setAlarmCode("22214");
        trap2.setAlarmName("NodeB Unavailable");
        trap2.setSiteId("KBL552");
        trap2.setSiteName("KBLU552_Qargha Sar_e_Kariz_Etisalat_(KBL416)");
        trap2.setAlarmSeverity(6L);
        trap2.setAlarmEventType(7L);
        trap2.setAlarmNetType("172");
        trap2.setAlarmServiceType("3G");
        trap2.setDisplaySiteId("KBLU552");
        trap2.setAlarmFaultCause(null);
        trap2.setAlarmFaultClass(null);
        trap2.setAlarmArrivalTime("2024-09-24 10:20:34");
        trap2.setAlarmClearedTime("2024-09-24 10:28:21");
        trap2.setAlarmNodeBId(null);
        trap2.setAlarmCellId(null);
        trap2.setAlarmRncId(null);

        BssHwTrapBody trap3 = new BssHwTrapBody();
        trap3.setId(null);
        trap3.setNewOrClear(1L);
        trap3.setTrapId("384992796");
        trap3.setAlarmCode("29201");
        trap3.setAlarmName("S1 Interface Fault");
        trap3.setSiteId("KBL552");
        trap3.setSiteName("KBL552_UL");
        trap3.setAlarmSeverity(2L);
        trap3.setAlarmEventType(3L);
        trap3.setAlarmNetType("194");
        trap3.setAlarmServiceType("4G");
        trap3.setDisplaySiteId("KBL552_UL");
        trap3.setAlarmFaultCause(null);
        trap3.setAlarmFaultClass(null);
        trap3.setAlarmArrivalTime("2024-09-24 10:24:37");
        trap3.setAlarmClearedTime("");
        trap3.setAlarmNodeBId(null);
        trap3.setAlarmCellId(null);
        trap3.setAlarmRncId(null);

        BssHwTrapBody trap4 = new BssHwTrapBody();
        trap4.setId(null);
        trap4.setNewOrClear(2L);
        trap4.setTrapId("384992796");
        trap4.setAlarmCode("29201");
        trap4.setAlarmName("S1 Interface Fault");
        trap4.setSiteId("KBL552");
        trap4.setSiteName("KBL552_UL");
        trap4.setAlarmSeverity(6L);
        trap4.setAlarmEventType(3L);
        trap4.setAlarmNetType("194");
        trap4.setAlarmServiceType("4G");
        trap4.setDisplaySiteId("KBL552_UL");
        trap4.setAlarmFaultCause(null);
        trap4.setAlarmFaultClass(null);
        trap4.setAlarmArrivalTime("2024-09-24 10:24:37");
        trap4.setAlarmClearedTime("2024-09-24 10:28:44");
        trap4.setAlarmNodeBId(null);
        trap4.setAlarmCellId(null);
        trap4.setAlarmRncId(null);

        // Add all instances to an array
        List<BssHwTrapBody> traps = new ArrayList<>(Arrays.asList(trap1, trap2, trap3, trap4));

        return traps;
    }



}
