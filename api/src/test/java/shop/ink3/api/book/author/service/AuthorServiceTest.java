package shop.ink3.api.book.author.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import shop.ink3.api.book.author.dto.AuthorCreateRequest;
import shop.ink3.api.book.author.dto.AuthorResponse;
import shop.ink3.api.book.author.dto.AuthorUpdateRequest;
import shop.ink3.api.book.author.entity.Author;
import shop.ink3.api.book.author.exception.AuthorNotFoundException;
import shop.ink3.api.book.author.repository.AuthorRepository;
import shop.ink3.api.common.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {
    @Mock
    AuthorRepository authorRepository;

    @InjectMocks
    AuthorService authorService;

    @Test
    void getAuthor() {
        Author author = Author.builder()
                .id(1L)
                .name("testAuthor")
                .build();
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        AuthorResponse response = authorService.getAuthor(1L);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(AuthorResponse.from(author), response);
    }

    @Test
    void getAuthorWithNotFound() {
        when(authorRepository.findById(1L)).thenThrow(new AuthorNotFoundException(1L));
        Assertions.assertThrows(AuthorNotFoundException.class, () -> authorService.getAuthor(1L));
    }

    @Test
    void getAuthors() {
        List<Author> authors = List.of(
                Author.builder()
                        .id(1L)
                        .name("testAuthor1")
                        .build(),
                Author.builder()
                        .id(2L)
                        .name("testAuthor2")
                        .build()
        );
        Pageable pageable = PageRequest.of(0, 10);
        Page<Author> page = new PageImpl<>(
                authors,
                pageable,
                authors.size()
        );
        when(authorRepository.findAll(pageable)).thenReturn(page);
        PageResponse<AuthorResponse> response = authorService.getAuthors(pageable);

        Assertions.assertEquals(0, response.page());
        Assertions.assertEquals(10, response.size());
        Assertions.assertEquals(2, response.totalElements());
        Assertions.assertEquals(1, response.totalPages());
        Assertions.assertEquals("testAuthor1", response.content().get(0).name());
        Assertions.assertEquals("testAuthor2", response.content().get(1).name());
        verify(authorRepository, times(1)).findAll(pageable);
    }

    @Test
    void createAuthor() {
        AuthorCreateRequest request = new AuthorCreateRequest(
                "testAuthor"
        );
        when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));
        AuthorResponse response = authorService.createAuthor(request);
        Assertions.assertNotNull(response);
        Assertions.assertAll(
                () -> Assertions.assertEquals(request.name(), response.name())
        );
    }

    @Test
    void updateAuthor() {
        Author author = Author.builder()
                .id(1L)
                .name("testAuthor")
                .build();
        AuthorUpdateRequest request = new AuthorUpdateRequest(
                "newAuthor"
        );
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));
        AuthorResponse response = authorService.updateAuthor(1L, request);
        Assertions.assertNotNull(response);
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, response.id()),
                () -> Assertions.assertEquals(request.name(), response.name())
        );
    }

    @Test
    void updateAuthorWithNotFound() {
        AuthorUpdateRequest request = new AuthorUpdateRequest(
                "newAuthor"
        );
        when(authorRepository.findById(1L)).thenThrow(new AuthorNotFoundException(1L));
        Assertions.assertThrows(AuthorNotFoundException.class, () -> authorService.updateAuthor(1L, request));
    }

    @Test
    void deleteAuthor() {
        Author author = Author.builder().id(1L).build();
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        authorService.deleteAuthor(1L);
        verify(authorRepository, times(1)).delete(author);
    }

    @Test
    void deleteAuthorWithNotFound() {
        when(authorRepository.findById(1L)).thenThrow(new AuthorNotFoundException(1L));
        Assertions.assertThrows(AuthorNotFoundException.class, () -> authorService.deleteAuthor(1L));
    }
}
