package com.traps.RoshanNOCTraps.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {


    @Bean
    public NewTopic zteTrapTopic(){
        return TopicBuilder.name("ZTE_TRAPS").partitions(1).replicas(2)
                .build();
    }

    @Bean
    public NewTopic hwTrapsTopic(){
        return TopicBuilder.name("HW_TRAPS").partitions(1).replicas(2)
                .build();
    }

}
