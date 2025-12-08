package shop.ink3.api.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Configuration;

/*

[Producer]
    ↓  RabbitTemplate.convertAndSend()
[Exchange: coupon.exchange]
    ↓ (routingKey = coupon.routing)
[Queue: coupon.queue]
    ↓
[Consumer (@RabbitListener)]

*/

@Configuration
@EnableRabbit
@Profile("!test")
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "coupon.exchange";

    /*
     coupon을 담을 queue 생성
     durable = true : 서버를 재시작해도 큐가 (영구적으로) 지속됨
     message는 이 큐에 쌓이고, 이후 @RabbitListener에서 소비됨.
    */
    @Bean
    public Queue welcomeQueue() {
        return QueueBuilder.durable("coupon.welcome")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "dlx.coupon.welcome")
                .withArgument("x-queue-type", "classic")
                .build();
    }

    @Bean
    public Queue welcomeQueueDead() {
        return new Queue("coupon.welcome.dead", true);
    }

    @Bean
    public Queue birthdayQueue() {
        return QueueBuilder.durable("coupon.birthday")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "dlx.coupon.birthday")
                .withArgument("x-queue-type", "classic")
                .build();
    }

    @Bean
    public Queue birthdayQueueDead() {
        return new Queue("coupon.birthday.dead", true);
    }

    /*
     message를 어디로 보낼지 라우팅해주는 교환기 생성
     Topic 타입은 "coupon.* -> coupon.routing, coupon.created : 모두 수신 가능
                 "coupon.welcome : 특정 type에서 수신
    */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange("dlx.exchange");
    }

    /*
     특정 Routing key를 통해 message를 Exchange -> Queue로 연결
     즉, "coupon.routing" 키를 가진 message가 오면 coupon.queue로 전달
     이걸 해줘야 message가 queue로 들어가게 됨
    */

    @Bean
    public Binding bindWelcomeQueue() {
        return BindingBuilder.bind(welcomeQueue()).to(exchange()).with("coupon.welcome");
    }

    @Bean
    public Binding bindWelcomeDLQ() {
        return BindingBuilder.bind(welcomeQueueDead()).to(dlxExchange()).with("dlx.coupon.welcome");
    }

    @Bean
    public Binding bindBirthdayQueue() {
        return BindingBuilder.bind(birthdayQueue()).to(exchange()).with("coupon.birthday");
    }

    @Bean
    public Binding bindBirthdayDLQ() {
        return BindingBuilder.bind(birthdayQueueDead()).to(dlxExchange()).with("dlx.coupon.birthday");
    }
    /*
     message를 자동으로 Json <-> java객체로 직렬화/역직렬화 해주는 변환기
     RebbitTemplate 및 @RabbitListener에서 DTO객체를 바로 주고받을 수 있게 해줌
    */

    // 1) POJO용 Jackson 컨버터
    @Bean
    public SimpleRabbitListenerContainerFactory pojoListenerContainerFactory(
            ConnectionFactory cf,
            Jackson2JsonMessageConverter jacksonConverter
    ) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(jacksonConverter);
        return f;
    }

    // 2) String(raw)용 컨테이너 팩토리
    @Bean
    public SimpleRabbitListenerContainerFactory stringListenerContainerFactory(
            ConnectionFactory cf
    ) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        return f;
    }

    @Bean
    public Jackson2JsonMessageConverter jacksonConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

}


