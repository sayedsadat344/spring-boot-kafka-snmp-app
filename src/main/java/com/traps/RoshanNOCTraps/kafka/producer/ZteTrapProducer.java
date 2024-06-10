package com.traps.RoshanNOCTraps.kafka.producer;

import com.traps.RoshanNOCTraps.traps.zte.ZteTrapBody;
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

    private KafkaTemplate<String, ZteTrapBody> kafkaTemplate;

    public ZteTrapProducer(KafkaTemplate<String, ZteTrapBody> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(ZteTrapBody data){

//        LOGGER.info(String.format("Message sent -> %s", data.toString()));

        Message<ZteTrapBody> message = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, "ZTE_TRAPS")
                .build();

        kafkaTemplate.send(message);
    }

}
