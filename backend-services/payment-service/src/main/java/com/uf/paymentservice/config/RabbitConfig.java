package com.uf.paymentservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {
    public static final String TICKET_EXCHANGE = "ticket.events";
    public static final String PAYMENT_EXCHANGE = "payment.events";

    public static final String PAYMENT_QUEUE = "payment.ticket.created"; // queue where ticket-service publishes new ticket events
    public static final String PAYMENT_COMPLETED_ROUTING = "payment.completed";
    public static final String TICKET_CREATED_ROUTING = "ticket.created";

    @Bean
    public TopicExchange ticketExchange() {
        return new TopicExchange(TICKET_EXCHANGE);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue paymentQueue() {
        // Ensure queue is declared with same TTL as ticket-service to avoid precondition failures
        return QueueBuilder.durable(PAYMENT_QUEUE)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL to match ticket-service
                .build();
    }

    @Bean
    public Binding bindingPaymentQueue(Queue paymentQueue, TopicExchange ticketExchange) {
        return BindingBuilder.bind(paymentQueue).to(ticketExchange).with(TICKET_CREATED_ROUTING);
    }

    // Use Jackson JSON converter and map incoming/outgoing type ids between services
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();

        Map<String, Class<?>> idClassMapping = new HashMap<>();
        // Ticket Service sends its TicketCreatedEvent with type id com.example.ticket_service.messaging.event.TicketCreatedEvent
        // Map it to this service's DTO class so deserialization succeeds
        idClassMapping.put("com.example.ticket_service.messaging.event.TicketCreatedEvent", com.uf.paymentservice.dto.TicketCreatedEvent.class);

        typeMapper.setIdClassMapping(idClassMapping);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        return rabbitTemplate;
    }
}
