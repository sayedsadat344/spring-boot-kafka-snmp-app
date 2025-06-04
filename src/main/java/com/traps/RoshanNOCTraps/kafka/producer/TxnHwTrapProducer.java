package com.traps.RoshanNOCTraps.kafka.producer;

import com.mycompany.app.sharedClasses.TxnHwTrapBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;


@Service
public class TxnHwTrapProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZteTrapProducer.class);

    private KafkaTemplate<String, TxnHwTrapBody> kafkaTemplate;

    public TxnHwTrapProducer(KafkaTemplate<String, TxnHwTrapBody> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(TxnHwTrapBody data){

        Message<TxnHwTrapBody> message = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, "TXN_HW_TRAPS")
                .build();

        System.out.println("TXN HW TRAP PRODUCED!!");
        kafkaTemplate.send(message);
    }
}
