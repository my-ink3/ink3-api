package shop.ink3.api.book.publisher.controller;

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
import shop.ink3.api.book.author.exception.AuthorNotFoundException;
import shop.ink3.api.book.publisher.dto.PublisherCreateRequest;
import shop.ink3.api.book.publisher.dto.PublisherResponse;
import shop.ink3.api.book.publisher.dto.PublisherUpdateRequest;
import shop.ink3.api.book.publisher.entity.Publisher;
import shop.ink3.api.book.publisher.exception.PublisherNotFoundException;
import shop.ink3.api.book.publisher.service.PublisherService;
import shop.ink3.api.common.dto.PageResponse;

@WebMvcTest(PublisherController.class)
class PublisherControllerTest {
    @MockitoBean
    PublisherService publisherService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @Test
    void getPublishers() throws Exception {
        List<PublisherResponse> publisherResponses = List.of(
                PublisherResponse.from(Publisher.builder().id(1L).name("testPublisher1").build()),
                PublisherResponse.from(Publisher.builder().id(2L).name("testPublisher2").build())
        );
        Page<PublisherResponse> page = new PageImpl<>(
                publisherResponses,
                PageRequest.of(0, 10),
                publisherResponses.size()
        );
        PageResponse<PublisherResponse> response = PageResponse.from(page);

        when(publisherService.getPublishers(any(Pageable.class))).thenReturn(response);
        mockMvc.perform(get("/pubs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[1].id").value(2))
                .andExpect(jsonPath("$.data.content[0].name").value("testPublisher1"))
                .andExpect(jsonPath("$.data.content[1].name").value("testPublisher2"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.hasPrevious").value(false))
                .andDo(print());
    }

    @Test
    void getPublisherById() throws Exception {
        Publisher publisher = Publisher.builder()
                .id(1L)
                .name("testPublisher")
                .build();
        PublisherResponse response = PublisherResponse.from(publisher);
        when(publisherService.getPublisherById(1L)).thenReturn(response);
        mockMvc.perform(get("/pubs/detail?id=1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("testPublisher"))
                .andDo(print());
    }

    @Test
    void getPublisherByIdWithNotFound() throws Exception {
        when(publisherService.getPublisherById(1L)).thenThrow(new PublisherNotFoundException(1L));
        mockMvc.perform(get("/pubs/detail?id=1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    void getPublisherByName() throws Exception {
        Publisher publisher = Publisher.builder()
                .id(2L)
                .name("testPublisher2")
                .build();
        PublisherResponse response = PublisherResponse.from(publisher);
        when(publisherService.getPublisherByName("testPublisher2")).thenReturn(response);
        mockMvc.perform(get("/pubs/detail?name=testPublisher2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(2L))
                .andExpect(jsonPath("$.data.name").value("testPublisher2"))
                .andDo(print());
    }

    @Test
    void getPublisherByNameWithNotFound() throws Exception {
        when(publisherService.getPublisherByName("testPublisher2")).thenThrow(new PublisherNotFoundException(2L));
        mockMvc.perform(get("/pubs/detail?name=testPublisher2"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    void getPublisherWithoutIdOrName() throws Exception {
        mockMvc.perform(get("/pubs/detail"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    void createPublisher() throws Exception {
        PublisherCreateRequest request = new PublisherCreateRequest("testPublisher");
        PublisherResponse response = new PublisherResponse(1L, "testPublisher");
        when(publisherService.createPublisher(request)).thenReturn(response);
        mockMvc.perform(post("/pubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("testPublisher"))
                .andDo(print());
    }

    @Test
    void updatePublisher() throws Exception {
        PublisherUpdateRequest request = new PublisherUpdateRequest("newPublisher");
        PublisherResponse response = new PublisherResponse(1L, "newPublisher");
        when(publisherService.updatePublisher(1L, request)).thenReturn(response);
        mockMvc.perform(put("/pubs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("newPublisher"))
                .andDo(print());
    }

    @Test
    void updatePublisherWithNotFound() throws Exception {
        PublisherUpdateRequest request = new PublisherUpdateRequest("newPublisher");
        when(publisherService.updatePublisher(1L, request)).thenThrow(new PublisherNotFoundException(1L));
        mockMvc.perform(put("/pubs/1")
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
    void deletePublisher() throws Exception {
        doNothing().when(publisherService).deletePublisher(1L);
        mockMvc.perform(delete("/pubs/1"))
                .andExpect(status().isNoContent())
                .andDo(print());
    }

    @Test
    void deleteAuthorWithNotFound() throws Exception {
        doThrow(new AuthorNotFoundException(1L)).when(publisherService).deletePublisher(1L);
        mockMvc.perform(delete("/pubs/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }
}
