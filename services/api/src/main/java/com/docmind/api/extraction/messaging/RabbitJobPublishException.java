package com.docmind.api.extraction.messaging;

public class RabbitJobPublishException extends RuntimeException {

  public RabbitJobPublishException() {
    super("RabbitMQ did not confirm asynchronous job publication");
  }

  public RabbitJobPublishException(Throwable cause) {
    super("RabbitMQ did not confirm asynchronous job publication", cause);
  }
}
