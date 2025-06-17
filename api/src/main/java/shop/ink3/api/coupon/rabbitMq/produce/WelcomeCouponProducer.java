package shop.ink3.api.coupon.rabbitMq.produce;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import shop.ink3.api.coupon.rabbitMq.message.WelcomeCouponMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class WelcomeCouponProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void send(WelcomeCouponMessage message) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(message);
            MessageProperties props = new MessageProperties();
            props.setContentType("application/json");
            props.setHeader("type", "WelcomeCouponMessage");

            Message rabbitMessage = new Message(body, props);
            rabbitTemplate.convertAndSend("coupon.exchange", "coupon.welcome", rabbitMessage);


            log.info("Welcome 메시지 전송 성공: {}", message);

        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패", e);
        } catch (AmqpException e) {
            log.error("RabbitMQ 전송 중 예외 발생", e);

        }
    }
}
