package com.docmind.api.extraction.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    name = "docmind.jobs.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RabbitJobTopologyConfiguration {

  @Bean
  public DirectExchange asyncJobExchange(AsyncJobProperties properties) {
    return new DirectExchange(properties.exchange(), true, false);
  }

  @Bean
  public DirectExchange asyncJobDeadLetterExchange(AsyncJobProperties properties) {
    return new DirectExchange(properties.deadLetterExchange(), true, false);
  }

  @Bean
  public Queue asyncJobQueue(AsyncJobProperties properties) {
    return QueueBuilder.durable(properties.queue())
        .deadLetterExchange(properties.deadLetterExchange())
        .deadLetterRoutingKey(properties.deadLetterRoutingKey())
        .build();
  }

  @Bean
  public Queue asyncJobDeadLetterQueue(AsyncJobProperties properties) {
    return QueueBuilder.durable(properties.deadLetterQueue()).build();
  }

  @Bean
  public Binding asyncJobBinding(
      Queue asyncJobQueue, DirectExchange asyncJobExchange, AsyncJobProperties properties) {
    return BindingBuilder.bind(asyncJobQueue)
        .to(asyncJobExchange)
        .with(properties.routingKey());
  }

  @Bean
  public Binding asyncJobDeadLetterBinding(
      Queue asyncJobDeadLetterQueue,
      DirectExchange asyncJobDeadLetterExchange,
      AsyncJobProperties properties) {
    return BindingBuilder.bind(asyncJobDeadLetterQueue)
        .to(asyncJobDeadLetterExchange)
        .with(properties.deadLetterRoutingKey());
  }

  @Bean
  Jackson2JsonMessageConverter asyncJobMessageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper, AsyncJobCommand.class.getPackageName());
  }

  @Bean
  RabbitTemplateCustomizer asyncJobRabbitTemplateCustomizer(
      Jackson2JsonMessageConverter asyncJobMessageConverter) {
    return template -> {
      template.setMessageConverter(asyncJobMessageConverter);
      template.setMandatory(true);
    };
  }

  @Bean(name = "asyncJobRabbitListenerContainerFactory")
  SimpleRabbitListenerContainerFactory asyncJobRabbitListenerContainerFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory,
      Jackson2JsonMessageConverter asyncJobMessageConverter,
      AsyncJobProperties properties) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setMessageConverter(asyncJobMessageConverter);
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    factory.setDefaultRequeueRejected(false);
    factory.setPrefetchCount(properties.consumerPrefetch());
    return factory;
  }
}
