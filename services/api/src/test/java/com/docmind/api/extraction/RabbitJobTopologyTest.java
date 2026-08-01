package com.docmind.api.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.docmind.api.extraction.messaging.AsyncJobProperties;
import com.docmind.api.extraction.messaging.RabbitJobTopologyConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

class RabbitJobTopologyTest {

  @Test
  void bindsDurableWorkQueueToDedicatedDeadLetterQueue() {
    AsyncJobProperties properties = properties();
    RabbitJobTopologyConfiguration configuration = new RabbitJobTopologyConfiguration();
    DirectExchange exchange = configuration.asyncJobExchange(properties);
    DirectExchange deadLetterExchange = configuration.asyncJobDeadLetterExchange(properties);
    Queue queue = configuration.asyncJobQueue(properties);
    Queue deadLetterQueue = configuration.asyncJobDeadLetterQueue(properties);
    Binding binding = configuration.asyncJobBinding(queue, exchange, properties);
    Binding deadLetterBinding =
        configuration.asyncJobDeadLetterBinding(
            deadLetterQueue, deadLetterExchange, properties);

    assertThat(exchange.isDurable()).isTrue();
    assertThat(queue.isDurable()).isTrue();
    assertThat(queue.getArguments())
        .containsEntry("x-dead-letter-exchange", properties.deadLetterExchange())
        .containsEntry("x-dead-letter-routing-key", properties.deadLetterRoutingKey());
    assertThat(binding.getRoutingKey()).isEqualTo(properties.routingKey());
    assertThat(deadLetterQueue.isDurable()).isTrue();
    assertThat(deadLetterBinding.getRoutingKey()).isEqualTo(properties.deadLetterRoutingKey());
  }

  static AsyncJobProperties properties() {
    return new AsyncJobProperties(
        true,
        true,
        true,
        "docmind.jobs.v1",
        "jobs.execute",
        "docmind.jobs.execute.v1",
        "docmind.jobs.dead-letter.v1",
        "jobs.dead",
        "docmind.jobs.dead-letter.v1",
        Duration.ofSeconds(5),
        Duration.ofSeconds(2),
        Duration.ofSeconds(3),
        Duration.ofSeconds(30),
        Duration.ofSeconds(5),
        Duration.ofMinutes(2),
        Duration.ofSeconds(30),
        Duration.ofSeconds(5),
        Duration.ofMinutes(5),
        2.0,
        50,
        4);
  }
}
