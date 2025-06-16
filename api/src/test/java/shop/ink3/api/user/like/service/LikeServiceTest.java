package shop.ink3.api.user.like.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.book.repository.BookRepository;
import shop.ink3.api.user.like.dto.LikeCreateRequest;
import shop.ink3.api.user.like.dto.LikeResponse;
import shop.ink3.api.user.like.entity.Like;
import shop.ink3.api.user.like.exception.LikeAlreadyExistsException;
import shop.ink3.api.user.like.exception.LikeNotFoundException;
import shop.ink3.api.user.like.repository.LikeRepository;
import shop.ink3.api.user.user.entity.User;
import shop.ink3.api.user.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class LikeServiceTest {

    @Mock private LikeRepository likeRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookRepository bookRepository;
    @InjectMocks private LikeService likeService;

    @Test
    @DisplayName("좋아요 생성")
    void createLike() {
        long userId = 1L;
        long bookId = 10L;
        LikeCreateRequest request = new LikeCreateRequest(bookId);

        User user = User.builder().id(userId).build();
        Book book = Book.builder()
            .id(bookId)
            .title("테스트 도서")
            .originalPrice(10000)
            .salePrice(8000)
            .build();
        Like like = Like.builder().user(user).book(book).build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(likeRepository.existsByUserIdAndBookId(userId, bookId)).thenReturn(false);
        when(bookRepository.existsById(bookId)).thenReturn(true);
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(bookRepository.getReferenceById(bookId)).thenReturn(book);
        when(likeRepository.save(any(Like.class))).thenReturn(like);

        LikeResponse response = likeService.createLike(userId, request);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("좋아요 중복 생성 시 예외")
    void createLike_duplicate() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(likeRepository.existsByUserIdAndBookId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> likeService.createLike(1L, new LikeCreateRequest(2L)))
            .isInstanceOf(LikeAlreadyExistsException.class);
    }

    @Test
    @DisplayName("좋아요 삭제")
    void deleteLike() {
        User user = User.builder().id(1L).build();
        Book book = Book.builder().id(10L).originalPrice(10000).salePrice(9000).build();
        Like like = Like.builder().id(5L).user(user).book(book).build();

        when(likeRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(like));

        likeService.deleteLike(1L, 5L);
        verify(likeRepository).delete(like);
    }

    @Test
    @DisplayName("좋아요 삭제 실패")
    void deleteLike_notFound() {
        when(likeRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.deleteLike(1L, 100L))
            .isInstanceOf(LikeNotFoundException.class);
    }

    @Test
    @DisplayName("좋아요 목록 조회")
    void getLikes() {
        when(userRepository.existsById(1L)).thenReturn(true);
        User user = User.builder().id(1L).build();
        Book book = Book.builder()
            .id(10L)
            .title("테스트 도서")
            .originalPrice(10000)
            .salePrice(9000)
            .build();
        Like like = Like.builder().user(user).book(book).build();

        when(likeRepository.findAllByUserId(eq(1L), any())).thenReturn(new PageImpl<>(List.of(like)));

        var result = likeService.getLikes(1L, PageRequest.of(0, 10));
        assertThat(result.content()).hasSize(1);
    }
}
