package com.nhnacademy.springailibrarycore.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE_NAME = "team4.book.review.queue";
    public static final String EXCHANGE_NAME = "team4.book.review.exchange";
    public static final String ROUTING_KEY = "team4.book.review.routing.key";

    public static final String DLX_NAME = "team4.book.review.dlx";
    public static final String DLQ_NAME = "team4.book.review.dlq";
    public static final String DLQ_ROUTING_KEY = "team4.book.review.dlx.routing.key";

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue reviewQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .deadLetterExchange(DLX_NAME)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding reviewBinding() {
        return BindingBuilder.bind(reviewQueue())
                .to(exchange())
                .with(ROUTING_KEY);
    }

    //
    @Bean
    public DirectExchange reviewDlx() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Queue reviewDlq() {
        return new Queue(DLQ_NAME, true);
    }

    @Bean
    public Binding reviewDlqBinding() {
        return BindingBuilder.bind(reviewDlq())
                .to(reviewDlx())
                .with(DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
