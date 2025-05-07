package com.traps.RoshanNOCTraps.traps.txn;

import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;

import java.sql.SQLException;

public class ProcessTxnPdu {

    public void processPdu(CommandResponderEvent crEvent) throws SQLException {

        PDU pdu = crEvent.getPDU();
        processTXNPDU(pdu);

    }


    private void processTXNPDU(PDU pdu) throws SQLException {


        if(pdu.getType() == PDU.TRAP){

            System.out.println("PDU: "+pdu);
        }
    }
}
