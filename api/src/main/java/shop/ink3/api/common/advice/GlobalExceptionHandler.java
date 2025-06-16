package shop.ink3.api.common.advice;

import java.util.HashMap;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import shop.ink3.api.common.dto.CommonResponse;
import shop.ink3.api.common.exception.AlreadyExistsException;
import shop.ink3.api.common.exception.BadRequestException;
import shop.ink3.api.common.exception.NotFoundException;
import shop.ink3.api.coupon.store.exception.CouponInvalidPeriodException;
import shop.ink3.api.order.order.exception.InsufficientBookStockException;
import shop.ink3.api.payment.exception.PaymentParserFailException;
import shop.ink3.api.payment.exception.PaymentProcessorFailException;
import shop.ink3.api.user.common.exception.DormantException;
import shop.ink3.api.user.common.exception.InvalidPasswordException;
import shop.ink3.api.user.common.exception.WithdrawnException;
import shop.ink3.api.user.point.history.exception.PointHistoryAlreadyCanceledException;
import shop.ink3.api.user.user.exception.InsufficientPointException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = {NotFoundException.class})
    public ResponseEntity<CommonResponse<Void>> handleNotFoundException(NotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.error(HttpStatus.NOT_FOUND, e.getMessage(), null));
    }

    @ExceptionHandler(value = {AlreadyExistsException.class})
    public ResponseEntity<CommonResponse<Void>> handleAlreadyExistsException(AlreadyExistsException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(CommonResponse.error(HttpStatus.CONFLICT, e.getMessage(), null));
    }

    @ExceptionHandler(value = {
            IllegalStateException.class,
            InsufficientPointException.class,
            InvalidPasswordException.class,
            PointHistoryAlreadyCanceledException.class,
            BadRequestException.class,
    })
    public ResponseEntity<CommonResponse<Void>> handleBadRequestException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(HttpStatus.BAD_REQUEST, e.getMessage(), null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CommonResponse<Void>> handleDuplicateKey(Exception e) {
        String message = e.getMessage();
        if (e.getCause() instanceof ConstraintViolationException cve) {
            if (cve.getMessage().contains("uk_user_login_id") || cve.getMessage().contains("uk_admin_login_id")) {
                message = "Login ID is already in use.";
            } else if (cve.getMessage().contains("uk_user_email")) {
                message = "Email is already in use.";
            }
        }
        return ResponseEntity.badRequest().body(CommonResponse.error(HttpStatus.BAD_REQUEST, message, null));
    }

    @ExceptionHandler(DormantException.class)
    public ResponseEntity<CommonResponse<Void>> handleDormantException(DormantException e) {
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(CommonResponse.error(HttpStatus.LOCKED, e.getMessage(), null));
    }

    @ExceptionHandler(WithdrawnException.class)
    public ResponseEntity<CommonResponse<Void>> handleWithdrawnException(WithdrawnException e) {
        return ResponseEntity.status(HttpStatus.GONE).body(CommonResponse.error(HttpStatus.GONE, e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(HttpStatus.BAD_REQUEST, "Invalid input values.", errors));
    }

    @ExceptionHandler(CouponInvalidPeriodException.class)
    public ResponseEntity<CommonResponse<String>> handleCouponInvalidPeriodException(
            CouponInvalidPeriodException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(CommonResponse.error(HttpStatus.CONFLICT, "COUPON_INVALID_PERIOD", e.getMessage()));
    }

    @ExceptionHandler(InsufficientBookStockException.class)
    public ResponseEntity<CommonResponse<String>> handleInsufficientBookStockException(
            InsufficientBookStockException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(CommonResponse.error(HttpStatus.CONFLICT, "BOOK_INSUFFICIENT_STOCK", e.getMessage()));
    }

    @ExceptionHandler(PaymentParserFailException.class)
    public ResponseEntity<CommonResponse<String>> handlePaymentParserFailException(PaymentParserFailException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(HttpStatus.BAD_REQUEST, "Payment parsing error", e.getMessage()));
    }

    @ExceptionHandler(PaymentProcessorFailException.class)
    public ResponseEntity<CommonResponse<String>> handlePaymentProcessorFailException(PaymentProcessorFailException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(HttpStatus.BAD_REQUEST, "Payment processor Approve fail error.",
                        e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception e) {
        return ResponseEntity.internalServerError()
                .body(CommonResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), null));
    }
}
