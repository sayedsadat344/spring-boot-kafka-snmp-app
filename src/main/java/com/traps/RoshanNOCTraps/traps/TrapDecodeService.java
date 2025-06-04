package com.traps.RoshanNOCTraps.traps;

import com.mycompany.app.sharedClasses.BssHwTrapBody;
import com.mycompany.app.sharedClasses.BssZteTrapBody;
import com.traps.RoshanNOCTraps.traps.zte.ZteOidConstants;
import org.snmp4j.PDU;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class TrapDecodeService {

    public Long getVariableAsLong(PDU pdu, OID oid) {
        try {
            String value = getVariableAsString(pdu, oid);
            return value.isEmpty() ? null : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    public String getVariableAsString(PDU pdu, OID oid) {
        Variable variable = pdu.getVariable(oid);
        return variable != null ? variable.toString() : "";
    }

    public void appendData(PDU pdu,String file) {
        try {
            Files.write(Paths.get(file), (pdu.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void appendData(BssZteTrapBody zte) {
        try {
            Files.write(Paths.get(ZteOidConstants.FILE_PATH), (zte.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void appendData(BssZteTrapBody pdu,String file) {
        try {
            Files.write(Paths.get(file), (pdu.toString() + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void logTrap(BssZteTrapBody trap) {
        System.out.println("\nZTE Trap Processed:");
        System.out.println("ID: " + trap.getTrapId());
        System.out.println("Alarm Code: " + trap.getAlarmCode());
        System.out.println("Site: " + trap.getSiteName());
        System.out.println("Type: " + (trap.getNewOrClear() == 1 ? "NEW" : "CLEARED"));
        System.out.println("*******************************************");
    }


    public void logTrap(BssHwTrapBody trap) {
        System.out.println("\nZTE Trap Processed:");
        System.out.println("ID: " + trap.getTrapId());
        System.out.println("Alarm Code: " + trap.getAlarmCode());
        System.out.println("Site: " + trap.getSiteName());
        System.out.println("Type: " + (trap.getNewOrClear() == 1 ? "NEW" : "CLEARED"));
        System.out.println("*******************************************");
    }

}
