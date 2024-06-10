package com.traps.RoshanNOCTraps.kafka.consumer;

import com.traps.RoshanNOCTraps.db.DbOperation;
import com.traps.RoshanNOCTraps.traps.hw.HwTrapBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
public class HwTrapConsumer {


    private static final Logger LOGGER = LoggerFactory.getLogger(HwTrapConsumer.class);

//    @KafkaListener(topics = "HW_TRAPS", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(HwTrapBody hwTrapBody, Acknowledgment acknowledgment){


        int opValue;
        if(hwTrapBody.getNewOrClear() == 1L){
            //inset new records
           opValue =  DbOperation.addHwTrap(hwTrapBody);

        }else{
            //update old record
            System.out.println("HW Clear time: "+hwTrapBody.getAlarmClearedTime());
          opValue = DbOperation.updateHwTrap(hwTrapBody.getTrapId(), hwTrapBody);

        }


        //here i want to manually acknowldge the consumption of a message from kafka

        if(opValue != -1){
            acknowledgment.acknowledge();
        }


        LOGGER.info(String.format("Json message recieved -> %s", hwTrapBody.toString()));
    }

}
