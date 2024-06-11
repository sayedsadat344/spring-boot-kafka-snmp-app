package com.traps.RoshanNOCTraps.kafka.producer;

import com.mycompany.app.sharedClasses.HwTrapBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class HwTrapProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZteTrapProducer.class);

    private KafkaTemplate<String, HwTrapBody> kafkaTemplate;

    public HwTrapProducer(KafkaTemplate<String, HwTrapBody> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(HwTrapBody data){

        Message<HwTrapBody> message = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, "HW_TRAPS")
                .build();

        System.out.println("HW TRAP PRODUCED!!");
        kafkaTemplate.send(message);
    }
}
