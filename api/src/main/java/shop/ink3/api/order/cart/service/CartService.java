package shop.ink3.api.order.cart.service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.book.exception.BookNotFoundException;
import shop.ink3.api.book.book.repository.BookRepository;
import shop.ink3.api.common.uploader.MinioService;
import shop.ink3.api.coupon.store.dto.CouponStoreDto;
import shop.ink3.api.coupon.store.service.CouponStoreService;
import shop.ink3.api.order.cart.dto.CartCouponResponse;
import shop.ink3.api.order.cart.dto.CartRequest;
import shop.ink3.api.order.cart.dto.CartResponse;
import shop.ink3.api.order.cart.dto.CartUpdateRequest;
import shop.ink3.api.order.cart.entity.Cart;
import shop.ink3.api.order.cart.repository.CartRepository;
import shop.ink3.api.order.common.exception.CartNotFoundException;
import shop.ink3.api.user.user.entity.User;
import shop.ink3.api.user.user.exception.UserNotFoundException;
import shop.ink3.api.user.user.repository.UserRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService {
    private static final String CART_KEY_PREFIX = "cart:user:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final MinioService minioService;

    private final CouponStoreService couponStoreService;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final CartRepository cartRepository;

    @Value("${minio.book-bucket}")
    private String bookBucket;

    public CartResponse addCartItem(CartRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new BookNotFoundException(request.bookId()));

        Cart cart = cartRepository.findByUserIdAndBookId(user.getId(), book.getId());

        if (Objects.nonNull(cart)) {
            cart.updateQuantity(cart.getQuantity() + request.quantity());
        } else {
            cart = Cart.builder()
                    .user(user)
                    .book(book)
                    .quantity(request.quantity())
                    .build();
        }
        Cart savedCart = cartRepository.save(cart);
        CartResponse response = CartResponse.from(savedCart);
        cacheCart(savedCart, response);

        return response;
    }

    public CartResponse updateCartQuantity(Long cartId, CartUpdateRequest request) {
        Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new CartNotFoundException(cartId));

        cart.updateQuantity(request.quantity());
        cartRepository.save(cart);

        CartResponse response = CartResponse.from(cart);
        cacheCart(cart, response);

        return response;
    }

    public List<CartResponse> getCartItemsByUserId(Long userId) {
        List<Cart> carts = cartRepository.findByUserId(userId);
        return carts.stream()
            .map(cart -> {
                String presignedUrl = minioService.getPresignedUrl(cart.getBook().getThumbnailUrl(), bookBucket);
                return CartResponse.from(cart, presignedUrl);
            })
            .toList();
    }

    public List<CartCouponResponse> getCartItemsWithCoupons(Long userId) {
        List<Cart> carts = cartRepository.findByUserId(userId);
        return carts.stream()
            .map(cart -> {
                String presignedUrl = minioService.getPresignedUrl(cart.getBook().getThumbnailUrl(), bookBucket);
                List<CouponStoreDto> coupons = couponStoreService.getApplicableCouponStores(userId, cart.getBook().getId());
                return CartCouponResponse.from(cart, coupons, presignedUrl);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CartCouponResponse> getSelectCartsWithCoupon(Long userId, List<Long> cartIds) {
        List<Cart> carts = cartRepository.findAllByUserIdAndIdIn(userId, cartIds);
        return carts.stream()
            .map(cart -> {
                String presignedUrl = minioService.getPresignedUrl(cart.getBook().getThumbnailUrl(), bookBucket);
                List<CouponStoreDto> coupons = couponStoreService.getApplicableCouponStores(userId, cart.getBook().getId());
                return CartCouponResponse.from(cart, coupons, presignedUrl);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CartResponse> getCartItems(Long userId) {
        List<Cart> carts = cartRepository.findByUserId(userId);
        return carts.stream().map(CartResponse::from).toList();
    }

    public void deleteCartItems(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        cartRepository.deleteAllByUserId(userId);
        String key = CART_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    public void deleteCartItem(Long cartId) {
        Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new CartNotFoundException(cartId));

        cartRepository.deleteById(cartId);

        String key = CART_KEY_PREFIX + cart.getUser().getId();
        hashOps().delete(key, cartId.toString());
    }

    private HashOperations<String, String, CartResponse> hashOps() {
        return redisTemplate.opsForHash();
    }

    private void cacheCart(Cart cart, CartResponse response) {
        String key = CART_KEY_PREFIX + cart.getUser().getId();
        hashOps().put(key, cart.getId().toString(), response);
        redisTemplate.expire(key, Duration.ofDays(3));
    }

}
