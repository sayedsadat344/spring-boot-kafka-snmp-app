package com.traps.RoshanNOCTraps.db;

import com.mycompany.app.sharedClasses.BssHwTrapBody;
import com.mycompany.app.sharedClasses.BssZteTrapBody;

import com.mycompany.app.sharedClasses.TxnHwTrapBody;
import com.traps.RoshanNOCTraps.kafka.producer.HwTrapProducer;
import com.traps.RoshanNOCTraps.kafka.producer.TxnHwTrapProducer;
import com.traps.RoshanNOCTraps.kafka.producer.ZteTrapProducer;
import org.springframework.stereotype.Component;

@Component
public class KafkaOperation {

    public static HwTrapProducer hwTrapProducer;


    public static ZteTrapProducer zteTrapProducer;

    public static TxnHwTrapProducer txnHwTrapProducer;


    public KafkaOperation(ZteTrapProducer zteTrapProducer,HwTrapProducer hwTrapProducer,TxnHwTrapProducer txnHwTrapProducer) {
        this.zteTrapProducer = zteTrapProducer;
        this.hwTrapProducer = hwTrapProducer;
        this.txnHwTrapProducer = txnHwTrapProducer;
    }


    public static void sendTxnHwTrap(TxnHwTrapBody txnHwTrapBody){
        txnHwTrapProducer.sendMessage(txnHwTrapBody);
    }


    public static void sendHwTrap(BssHwTrapBody hwTrapBody){
        hwTrapProducer.sendMessage(hwTrapBody);
    }

    public static void sendZteTrap(BssZteTrapBody zteTrapBody){
        zteTrapProducer.sendMessage(zteTrapBody);
    }

}
