package com.li.lwg.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /**
     * 使用 Jackson 将对象自动转为 JSON
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 所有“任务相关”的消息，都发到这个topic
     */
    @Bean
    public TopicExchange missionExchange() {
        return new TopicExchange("lwg.mission.exchange", true, false);
    }

    /**
     * 专门给“信誉系统”消费的队列
     */
    @Bean
    public Queue reputationQueue() {
        return new Queue("lwg.reputation.queue", true);
    }

    /**
     * 任务结算的 routingKey
     */
    @Bean
    public Binding bindingReputation() {
        return BindingBuilder.bind(reputationQueue())
                .to(missionExchange())
                .with("mission.settled");
    }
}