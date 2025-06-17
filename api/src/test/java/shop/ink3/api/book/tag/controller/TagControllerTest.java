package shop.ink3.api.book.tag.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.ink3.api.book.tag.dto.TagCreateRequest;
import shop.ink3.api.book.tag.dto.TagResponse;
import shop.ink3.api.book.tag.dto.TagUpdateRequest;
import shop.ink3.api.book.tag.service.TagService;

import java.util.List;
import shop.ink3.api.common.dto.PageResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TagController.class)
@AutoConfigureMockMvc
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTag() throws Exception {
        TagCreateRequest request = new TagCreateRequest("java");
        TagResponse response = new TagResponse(1L, "java");

        given(tagService.createTag(any())).willReturn(response);

        mockMvc.perform(post("/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("java"));
    }

    @Test
    void getAllTags() throws Exception {
        List<TagResponse> tagResponses = List.of(
                new TagResponse(1L, "java"),
                new TagResponse(2L, "spring")
        );
        Page<TagResponse> page = new PageImpl<>(
                tagResponses,
                PageRequest.of(0, 10),
                tagResponses.size()
        );
        PageResponse<TagResponse> pageResponse = PageResponse.from(page);

        given(tagService.getTags(any(Pageable.class))).willReturn(pageResponse);

        mockMvc.perform(get("/tags")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("java"))
                .andExpect(jsonPath("$.data.content[1].name").value("spring"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.hasPrevious").value(false));
    }

    @Test
    void updateTag() throws Exception {
        TagUpdateRequest request = new TagUpdateRequest("springboot");
        TagResponse response = new TagResponse(1L, "springboot");

        given(tagService.updateTag(eq(1L), any())).willReturn(response);

        mockMvc.perform(put("/tags/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("springboot"));
    }

    @Test
    void deleteTag() throws Exception {
        willDoNothing().given(tagService).deleteTag(1L);

        mockMvc.perform(delete("/tags/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getTagById() throws Exception {
        TagResponse response = new TagResponse(1L, "java");
        given(tagService.getTagById(1L)).willReturn(response);

        mockMvc.perform(get("/tags/detail?id=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("java"));
    }



    @Test
    void getTagByName() throws Exception {
        TagResponse response = new TagResponse(1L, "java");
        given(tagService.getTagByName("java")).willReturn(response);

        mockMvc.perform(get("/tags/detail?name=java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("java"));
    }

    @Test
    void getTagWithoutIdOrName() throws Exception {
        mockMvc.perform(get("/tags/detail"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }
}