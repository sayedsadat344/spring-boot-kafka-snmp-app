package com.traps.RoshanNOCTraps.kafka.consumer;

import com.traps.RoshanNOCTraps.db.DbOperation;
import com.traps.RoshanNOCTraps.traps.hw.HwTrapBody;
import com.traps.RoshanNOCTraps.traps.zte.ZteTrapBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
public class ZteTrapConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZteTrapConsumer.class);

    @KafkaListener(topics = "ZTE_TRAPS", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ZteTrapBody zteTrapBody, Acknowledgment acknowledgment){

        int opValue;

        if(zteTrapBody.getNewOrClear() == 1L){
            //inset new records
           opValue =  DbOperation.addZteTrap(zteTrapBody);

        }else{
            //update old record
          opValue =  DbOperation.updateZteTrap(zteTrapBody.getTrapId(), zteTrapBody);
        }

        if(opValue != -1){
            acknowledgment.acknowledge();
        }

        LOGGER.info(String.format("Json message recieved -> %s", zteTrapBody.toString()));
    }
}
