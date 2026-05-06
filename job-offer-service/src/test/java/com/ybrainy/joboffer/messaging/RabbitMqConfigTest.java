package com.ybrainy.joboffer.messaging;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

class RabbitMqConfigTest {

  private final RabbitMqConfig config = new RabbitMqConfig();

  @Test
  void shouldCreateExchangeAndQueues() {
    TopicExchange exchange = config.appExchange("job.exchange");
    Queue partnershipQueue = config.partnershipEventsQueue("partnership.queue");
    Queue applicationQueue = config.jobApplicationEventsQueue("application.queue");
    Queue analyticsQueue = config.jobApplicationAnalyticsQueue("analytics.queue");

    assertEquals("job.exchange", exchange.getName());
    assertTrue(exchange.isDurable());
    assertEquals("partnership.queue", partnershipQueue.getName());
    assertEquals("application.queue", applicationQueue.getName());
    assertEquals("analytics.queue", analyticsQueue.getName());
  }

  @Test
  void shouldCreateBindingsWithExpectedRoutingKeys() {
    TopicExchange exchange = config.appExchange("job.exchange");
    Queue partnershipQueue = config.partnershipEventsQueue("partnership.queue");
    Queue applicationQueue = config.jobApplicationEventsQueue("application.queue");
    Queue analyticsQueue = config.jobApplicationAnalyticsQueue("analytics.queue");

    Binding partnershipBinding = config.partnershipBinding(partnershipQueue, exchange, "partner.key");
    Binding applicationBinding = config.jobApplicationBinding(applicationQueue, exchange, "application.key");
    Binding analyticsBinding = config.jobApplicationAnalyticsBinding(analyticsQueue, exchange, "application.key");

    assertEquals("partnership.queue", partnershipBinding.getDestination());
    assertEquals("job.exchange", partnershipBinding.getExchange());
    assertEquals("partner.key", partnershipBinding.getRoutingKey());

    assertEquals("application.queue", applicationBinding.getDestination());
    assertEquals("job.exchange", applicationBinding.getExchange());
    assertEquals("application.key", applicationBinding.getRoutingKey());

    assertEquals("analytics.queue", analyticsBinding.getDestination());
    assertEquals("job.exchange", analyticsBinding.getExchange());
    assertEquals("application.key", analyticsBinding.getRoutingKey());
  }

  @Test
  void jacksonMessageConverter_shouldReturnJacksonConverter() {
    MessageConverter converter = config.jacksonMessageConverter(new ObjectMapper());

    assertInstanceOf(Jackson2JsonMessageConverter.class, converter);
  }
}

