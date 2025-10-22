package com.kleadingsolutions.expenseshare.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic expenseTopic() {
        return new NewTopic("expense.created", 1, (short) 1);
    }

    @Bean
    public NewTopic settlementTopic() {
        return new NewTopic("settlement.created", 1, (short) 1);
    }
}