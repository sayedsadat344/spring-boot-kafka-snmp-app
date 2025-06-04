package com.traps.RoshanNOCTraps.kafka.producer;

import com.mycompany.app.sharedClasses.BssZteTrapBody;

import org.springframework.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class ZteTrapProducer {

//    @Value("${spring.kafka.topic.name}")
//    private String topicName;

    private static final Logger LOGGER = LoggerFactory.getLogger(ZteTrapProducer.class);

    private KafkaTemplate<String, BssZteTrapBody> kafkaTemplate;

    public ZteTrapProducer(KafkaTemplate<String, BssZteTrapBody> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(BssZteTrapBody data){

//        LOGGER.info(String.format("Message sent -> %s", data.toString()));

        Message<BssZteTrapBody> message = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, "ZTE_TRAPS")
                .build();

        System.out.println("ZTE TRAP PRODUCED!!");
        kafkaTemplate.send(message);
    }

}
