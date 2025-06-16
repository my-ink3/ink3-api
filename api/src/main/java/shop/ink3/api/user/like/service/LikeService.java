package shop.ink3.api.user.like.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.book.exception.BookNotFoundException;
import shop.ink3.api.book.book.repository.BookRepository;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.user.like.dto.LikeCreateRequest;
import shop.ink3.api.user.like.dto.LikeResponse;
import shop.ink3.api.user.like.entity.Like;
import shop.ink3.api.user.like.exception.LikeAlreadyExistsException;
import shop.ink3.api.user.like.exception.LikeNotFoundException;
import shop.ink3.api.user.like.repository.LikeRepository;
import shop.ink3.api.user.user.entity.User;
import shop.ink3.api.user.user.exception.UserNotFoundException;
import shop.ink3.api.user.user.repository.UserRepository;

@Transactional
@RequiredArgsConstructor
@Service
public class LikeService {
    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public PageResponse<LikeResponse> getLikes(long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        Page<Like> likes = likeRepository.findAllByUserId(userId, pageable);
        return PageResponse.from(likes.map(like -> LikeResponse.from(userId, like)));
    }

    public LikeResponse createLike(long userId, LikeCreateRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        if (likeRepository.existsByUserIdAndBookId(userId, request.bookId())) {
            throw new LikeAlreadyExistsException(userId, request.bookId());
        }

        if (!bookRepository.existsById(request.bookId())) {
            throw new BookNotFoundException(request.bookId());
        }

        User user = userRepository.getReferenceById(userId);
        Book book = bookRepository.getReferenceById(request.bookId());

        book.incrementLikeCount();
        bookRepository.save(book);

        Like like = Like.builder().user(user).book(book).build();
        return LikeResponse.from(likeRepository.save(like));
    }

    public void deleteLike(long userId, long likeId) {
        Like like = likeRepository.findByIdAndUserId(likeId, userId)
                .orElseThrow(() -> new LikeNotFoundException(likeId));

        Book book = like.getBook();
        book.decrementLikeCount();
        bookRepository.save(book);

        likeRepository.delete(like);
    }
}
