package shop.ink3.api.book.category.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import shop.ink3.api.book.category.dto.CategoryChangeParentRequest;
import shop.ink3.api.book.category.dto.CategoryCreateRequest;
import shop.ink3.api.book.category.dto.CategoryFlatDto;
import shop.ink3.api.book.category.dto.CategoryTreeDto;
import shop.ink3.api.book.category.dto.CategoryUpdateNameRequest;
import shop.ink3.api.book.category.service.CategoryService;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("카테고리 트리 조회")
    void getAllCategoriesTree() throws Exception {
        List<CategoryTreeDto> mockTree = List.of(new CategoryTreeDto(1L, "문학", List.of()));
        given(categoryService.getCategoriesTree()).willReturn(mockTree);

        mockMvc.perform(get("/categories/tree"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(1L));
    }

    @Test
    @DisplayName("카테고리 평면 조회")
    void getAllCategoriesFlat() throws Exception {
        List<CategoryFlatDto> mockList = List.of(new CategoryFlatDto(1L, "문학", 1L, 0));
        given(categoryService.getCategoriesFlat()).willReturn(mockList);

        mockMvc.perform(get("/categories/flat"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("문학"));
    }

    @Test
    @DisplayName("하위 카테고리 조회")
    void getAllDescendants() throws Exception {
        Long id = 1L;
        CategoryTreeDto mockTree = new CategoryTreeDto(id, "문학", List.of());
        given(categoryService.getAllDescendants(id)).willReturn(mockTree);

        mockMvc.perform(get("/categories/{id}/descendants", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    @DisplayName("상위 카테고리 조회")
    void getAllAncestors() throws Exception {
        Long id = 1L;
        List<CategoryFlatDto> mockList = List.of(new CategoryFlatDto(1L, "문학", 1L, 0));
        given(categoryService.getAllAncestors(id)).willReturn(mockList);

        mockMvc.perform(get("/categories/{id}/ancestor", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("문학"));
    }

    @Test
    @DisplayName("카테고리 생성")
    void createCategory() throws Exception {
        CategoryCreateRequest req = new CategoryCreateRequest("에세이", 1L);
        CategoryTreeDto res = new CategoryTreeDto(10L, "에세이", List.of());

        given(categoryService.createCategory(req)).willReturn(res);

        mockMvc.perform(post("/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("에세이"));
    }

    @Test
    @DisplayName("카테고리 이름 수정")
    void updateCategoryName() throws Exception {
        long id = 5L;
        CategoryUpdateNameRequest req = new CategoryUpdateNameRequest("역사");

        doNothing().when(categoryService).updateCategoryName(id, req);

        mockMvc.perform(patch("/categories/{id}/name", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("카테고리 부모 변경")
    void changeParent() throws Exception {
        long id = 7L;
        CategoryChangeParentRequest req = new CategoryChangeParentRequest(2L);

        doNothing().when(categoryService).changeParent(id, req);

        mockMvc.perform(patch("/categories/{id}/parent", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("카테고리 삭제")
    void deleteCategory() throws Exception {
        long id = 8L;
        doNothing().when(categoryService).deleteCategory(id);

        mockMvc.perform(delete("/categories/{id}", id))
            .andExpect(status().isNoContent());
    }
}
