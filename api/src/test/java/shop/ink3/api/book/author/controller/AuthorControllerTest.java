package shop.ink3.api.book.author.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.ink3.api.book.author.dto.AuthorCreateRequest;
import shop.ink3.api.book.author.dto.AuthorResponse;
import shop.ink3.api.book.author.dto.AuthorUpdateRequest;
import shop.ink3.api.book.author.entity.Author;
import shop.ink3.api.book.author.exception.AuthorNotFoundException;
import shop.ink3.api.book.author.service.AuthorService;
import shop.ink3.api.common.dto.PageResponse;

@WebMvcTest(AuthorController.class)
class AuthorControllerTest {
    @MockitoBean
    AuthorService authorService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @Test
    void getAuthor() throws Exception {
        Author author = Author.builder()
                .id(1L)
                .name("testAuthor")
                .build();
        AuthorResponse response = AuthorResponse.from(author);
        when(authorService.getAuthor(1L)).thenReturn(response);
        mockMvc.perform(get("/authors/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("testAuthor"))
                .andDo(print());
    }

    @Test
    void getAuthorWithNotFound() throws Exception {
        when(authorService.getAuthor(1L)).thenThrow(new AuthorNotFoundException(1L));
        mockMvc.perform(get("/authors/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    void getAuthors() throws Exception {
        List<AuthorResponse> authorResponses = List.of(
                AuthorResponse.from(Author.builder().id(1L).build()),
                AuthorResponse.from(Author.builder().id(2L).build())
        );
        Page<AuthorResponse> page = new PageImpl<>(
                authorResponses,
                PageRequest.of(0, 10),
                authorResponses.size()
        );
        PageResponse<AuthorResponse> response = PageResponse.from(page);

        when(authorService.getAuthors(any(Pageable.class))).thenReturn(response);
        mockMvc.perform(get("/authors")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[1].id").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.hasPrevious").value(false))
                .andDo(print());
    }

    @Test
    void createAuthor() throws Exception {
        AuthorCreateRequest request = new AuthorCreateRequest(
                "testAuthor"
        );
        AuthorResponse response = new AuthorResponse(
                1L,
                "testAuthor"
        );
        when(authorService.createAuthor(request)).thenReturn(response);
        mockMvc.perform(post("/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andDo(print());
    }

    @Test
    void updateAuthor() throws Exception {
        AuthorUpdateRequest request = new AuthorUpdateRequest(
                "newAuthor"
        );
        AuthorResponse response = new AuthorResponse(
                1L,
                "newAuthor"
        );
        when(authorService.updateAuthor(1L, request)).thenReturn(response);
        mockMvc.perform(put("/authors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andDo(print());
    }

    @Test
    void updateAuthorWithNotFound() throws Exception {
        AuthorUpdateRequest request = new AuthorUpdateRequest(
                "newAuthor"
        );
        when(authorService.updateAuthor(1L, request)).thenThrow(new AuthorNotFoundException(1L));
        mockMvc.perform(put("/authors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    void deleteAuthor() throws Exception {
        doNothing().when(authorService).deleteAuthor(1L);
        mockMvc.perform(delete("/authors/1"))
                .andExpect(status().isNoContent())
                .andDo(print());
    }

    @Test
    void deleteAuthorWithNotFound() throws Exception {
        doThrow(new AuthorNotFoundException(1L)).when(authorService).deleteAuthor(1L);
        mockMvc.perform(delete("/authors/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }
}
