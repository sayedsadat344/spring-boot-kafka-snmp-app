package com.traps.RoshanNOCTraps.db;

import com.traps.RoshanNOCTraps.kafka.producer.HwTrapProducer;
import com.traps.RoshanNOCTraps.kafka.producer.ZteTrapProducer;
import com.traps.RoshanNOCTraps.traps.hw.HwTrapBody;
import com.traps.RoshanNOCTraps.traps.zte.ZteTrapBody;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaOperation {

    public static HwTrapProducer hwTrapProducer;


    public static ZteTrapProducer zteTrapProducer;


    public KafkaOperation(ZteTrapProducer zteTrapProducer,HwTrapProducer hwTrapProducer) {
        this.zteTrapProducer = zteTrapProducer;
        this.hwTrapProducer = hwTrapProducer;
    }


    public static void sendHwTrap(HwTrapBody hwTrapBody){
        hwTrapProducer.sendMessage(hwTrapBody);
    }

    public static void sendZteTrap(ZteTrapBody zteTrapBody){
        zteTrapProducer.sendMessage(zteTrapBody);
    }

}