package com.traps.RoshanNOCTraps.traps.txn;

import com.mycompany.app.sharedClasses.TxnHwTrapBody;

import com.traps.RoshanNOCTraps.db.KafkaOperation;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static com.traps.RoshanNOCTraps.traps.txn.TxnHwOidConstants.*;

public class ProcessTxnPdu {

    public void processPdu(CommandResponderEvent crEvent) throws SQLException {

        PDU pdu = crEvent.getPDU();
        processTXNPDU(pdu);

    }



    private void processTXNPDU(PDU pdu) throws SQLException {


        try {
            if (pdu != null && pdu.getType() == PDU.TRAP) {
                Long intendedTxnHwAlarmId = getVariableAsLong(pdu, TXN_HW_ALARM_CODE);
                if (TxnHwOidConstants.alarmIdList.contains(intendedTxnHwAlarmId)) {

                    appendData(pdu,"TXN",intendedTxnHwAlarmId.toString());
                    appendData(pdu,"TXN",FILE_PATH);

                    TxnHwTrapBody trapBody = createTxnHwTrapBody(pdu);

                    KafkaOperation.sendTxnHwTrap(trapBody);



                    System.out.println("Created: "+TXN_HW_TRAP_IDENTIFIER);

                    appendData(trapBody,"TXN",intendedTxnHwAlarmId.toString()+"body");

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

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

    private Boolean getVariableAsBoolean(PDU pdu, OID oid) {
        String value = getVariableAsString(pdu, oid);
        return value != null ? ("1".equals(value) || "true".equalsIgnoreCase(value)) : null;
    }

    private TxnHwTrapBody createTxnHwTrapBody(PDU pdu) {
        TxnHwTrapBody body = new TxnHwTrapBody();

        body.setAlarmCode(getVariableAsLong(pdu, TXN_HW_ALARM_CODE));
        body.setAlarmFaultCause(getVariableAsString(pdu, TXN_HW_ALARM_FAULT_ID));
        body.setAlarmEventType(getVariableAsString(pdu, TXN_HW_ALARM_EVENT_TYPE));

        body.setLinkId(getVariableAsString(pdu, TXN_HW_ALARM_NETWORK_ELEMENT_NAME));
//        body.set(getVariableAsString(pdu, TXN_HW_ALARM_INSTANCE_INFO));

        body.setAlarmArrivalTime(getVariableAsString(pdu, TXN_HW_ALARM_OCCUR_TIME));
        body.setAlarmClearedTime(getVariableAsString(pdu, TXN_HW_ALARM_OCCUR_TIME));
        body.setAlarmFaultCause(getVariableAsString(pdu, TXN_HW_ALARM_PROBABLE_CAUSE));
        body.setAlarmSeverity(getVariableAsString(pdu, TXN_HW_ALARM_LEVEL));
        body.setTrapId(getVariableAsString(pdu, TXN_HW_ALARM_SERIAL_NUMBER));
        body.setAlarmName(getVariableAsString(pdu, TXN_HW_ALARM_EVENT_NAME));

//        body.setTxnHwAlarmDetails(getVariableAsString(pdu, TXN_HW_ALARM_DETAILS));
//        body.setTxnHwAlarmAdditionalInfo(getVariableAsString(pdu, TXN_HW_ALARM_ADDITIONAL_INFO));
//        body.setTxnHwAlarmFaultFlag(getVariableAsBoolean(pdu, TXN_HW_ALARM_FAULT_FLAG));
//        body.setTxnHwAlarmFunctionType(getVariableAsString(pdu, TXN_HW_ALARM_FUNCTION_TYPE));
//        body.setTxnHwAlarmIpAddress(getVariableAsString(pdu, TXN_HW_ALARM_IP_ADDRESS));
//
//        body.setTxnHwAlarmRepairRecommendations(getVariableAsString(pdu, TXN_HW_ALARM_REPAIR_RECOMMENDATIONS));
//        body.setTxnHwAlarmResourceId(getVariableAsString(pdu, TXN_HW_ALARM_RESOURCE_ID));



        return body;
    }


    private void appendData(TxnHwTrapBody pdu, String folderName, String fileName) {
        try {
            Path folderPath = Paths.get(folderName);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath); // Ensure the folder exists
            }

            Path filePath = folderPath.resolve(fileName); // Construct full file path

            Files.write(
                    filePath,
                    (pdu.toString() + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void appendData(PDU pdu, String folder, String fileName) {
        try {
            // Ensure the directory exists
            Path dirPath = Paths.get(folder);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // Construct full file path
            Path filePath = dirPath.resolve(fileName);

            // Write data to the file
            Files.write(filePath, (pdu.toString() + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
