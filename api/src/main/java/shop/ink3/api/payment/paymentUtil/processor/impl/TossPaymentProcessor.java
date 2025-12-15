package shop.ink3.api.payment.paymentUtil.processor.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import shop.ink3.api.order.guest.dto.GuestPaymentConfirmRequest;
import shop.ink3.api.payment.dto.PaymentCancelRequest;
import shop.ink3.api.payment.dto.PaymentConfirmRequest;
import shop.ink3.api.payment.exception.PaymentProcessorFailException;
import shop.ink3.api.payment.paymentUtil.processor.PaymentProcessor;

import java.util.Map;
import java.util.Base64;
import shop.ink3.api.payment.paymentUtil.client.PaymentClient;

@RequiredArgsConstructor
@Component("TOSS-PROCESSOR")
public class TossPaymentProcessor implements PaymentProcessor {

    @Value("${payment.toss.secret_key}")
    private String SECRET_KEY;
    private final PaymentClient tossPaymentClient;
    private static final String PAYMENT_KEY = "paymentKey";
    private static final String PAYMENT_AMOUNT = "amount";
    private static final String PAYMENT_ORDER_ID = "orderId";
    private static final String AUTH_HEADER_PREFIX = "BASIC ";
    private static final String PAYMENT_CANCEL_REASON = "cancelReason";
    private static final String PAYMENT_METHOD = "TOSS";


    @Override
    public String processPayment(PaymentConfirmRequest confirmRequest) {
        return executePaymentConfirm(confirmRequest.paymentKey(), confirmRequest.amount(), confirmRequest.orderUUID());
    }

    @Override
    public String processPayment(GuestPaymentConfirmRequest confirmRequest) {
        return executePaymentConfirm(confirmRequest.paymentKey(), confirmRequest.amount(), confirmRequest.orderUUID());
    }

    @Override
    public String cancelPayment(PaymentCancelRequest cancelRequest) {
        return executePaymentCancel(cancelRequest.paymentKey(), cancelRequest.cancelReason());
    }



    private String executePaymentConfirm(String paymentKey, int amount, String orderUUID) {
        try {
            String basicAuthHeader = AUTH_HEADER_PREFIX + Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes());

            Map<String, Object> body = Map.of(
                    PAYMENT_KEY, paymentKey,
                    PAYMENT_AMOUNT, amount,
                    PAYMENT_ORDER_ID, orderUUID
            );

            return tossPaymentClient.confirmPayment(basicAuthHeader, body);
        } catch (Exception e) {
            throw new PaymentProcessorFailException(PAYMENT_METHOD, e);
        }
    }

    public String executePaymentCancel(String paymentKey, String cancelReason) {
        try {
            String basicAuthHeader = AUTH_HEADER_PREFIX + Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes());

            Map<String, Object> body = Map.of(
                    PAYMENT_CANCEL_REASON, cancelReason
            );
            return tossPaymentClient.cancelPayment(basicAuthHeader, paymentKey, body);
        } catch (Exception e) {
        throw new PaymentProcessorFailException(PAYMENT_METHOD, e);
        }
    }
}
