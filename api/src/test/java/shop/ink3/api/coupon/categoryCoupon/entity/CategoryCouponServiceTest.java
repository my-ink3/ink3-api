package shop.ink3.api.coupon.categoryCoupon.entity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import shop.ink3.api.book.category.dto.*;
import shop.ink3.api.book.category.entity.Category;
import shop.ink3.api.book.category.exception.*;
import shop.ink3.api.book.category.repository.CategoryRepository;
import shop.ink3.api.book.category.service.CategoryService;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("카테고리 트리 조회")
    void getCategoriesTree() {
        Category root = Category.builder().id(1L).name("문학").path("").build();
        given(categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "path")))
            .willReturn(List.of(root));

        List<CategoryTreeDto> result = categoryService.getCategoriesTree();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("문학");
    }

    @Test
    @DisplayName("카테고리 평면 조회")
    void getCategoriesFlat() {
        Category category = Category.builder().id(1L).name("문학").path("").build();
        given(categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "path")))
            .willReturn(List.of(category));

        List<CategoryFlatDto> result = categoryService.getCategoriesFlat();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("문학");
    }

    @Test
    @DisplayName("하위 카테고리 조회 - 존재하지 않는 ID 예외")
    void getAllDescendantsNotFound() {
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> {
            categoryService.getAllDescendants(99L);
        });
    }

    @Test
    @DisplayName("카테고리 생성 - 중복 이름 예외")
    void createCategory_duplicatedName() {
        given(categoryRepository.existsByName("문학")).willReturn(true);

        assertThrows(CategoryAlreadyExistsException.class, () ->
            categoryService.createCategory(new CategoryCreateRequest("문학", null)));
    }

    @Test
    @DisplayName("카테고리 이름 수정 - 정상 동작")
    void updateCategoryName() {
        Category category = Category.builder().id(1L).name("문학").path("").build();
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        categoryService.updateCategoryName(1L, new CategoryUpdateNameRequest("역사"));

        assertThat(category.getName()).isEqualTo("역사");
    }

    @Test
    @DisplayName("카테고리 부모 변경 - 자기 자신을 부모로 지정하면 예외")
    void changeParent_selfParentingException() {
        assertThrows(SelfParentingCategoryException.class, () ->
            categoryService.changeParent(1L, new CategoryChangeParentRequest(1L)));
    }

    @Test
    @DisplayName("카테고리 삭제 - 자식이 존재할 경우 예외 발생")
    void deleteCategory_hasChildren() {
        Category category = Category.builder().id(1L).name("문학").path("").build();

        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsByPathStartingWith("/1")).willReturn(true);

        assertThrows(CategoryHasChildrenException.class, () ->
            categoryService.deleteCategory(1L));
    }

    @Test
    @DisplayName("카테고리 삭제 - 정상 삭제")
    void deleteCategory_success() {
        Category category = Category.builder().id(1L).name("문학").path("").build();

        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsByPathStartingWith("/1")).willReturn(false);
        willDoNothing().given(categoryRepository).deleteById(1L);

        categoryService.deleteCategory(1L);

        then(categoryRepository).should().deleteById(1L);
    }

    @Test
    @DisplayName("카테고리 계층 생성 - 중복 이름 없이 전체 생성")
    void createCategoryHierarchy_success() {
        String path = "문학 > 소설 > 한국소설";

        // 중복 방지를 위해 존재 여부 확인 → 모두 없다고 가정
        given(categoryRepository.findByName(anyString()))
            .willReturn(Optional.empty());

        // 새로 저장된 Category 반환 (name만 다르고 나머지 동일)
        given(categoryRepository.save(any(Category.class)))
            .willAnswer(invocation -> {
                Category c = invocation.getArgument(0);
                return Category.builder().id(new Random().nextLong()).name(c.getName()).path("").build();
            });

        List<List<CategoryFlatDto>> result = categoryService.createCategoryHierarchy(path);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).extracting(CategoryFlatDto::name)
            .containsExactly("문학", "소설", "한국소설");
    }

    @Test
    @DisplayName("카테고리 부모 변경 - 순환 계층 예외 발생")
    void changeParent_cycleException() {
        long childId = 1L;
        long newParentId = 2L;

        // child 경로: /1, parent 경로: /1/2 (=> 순환 계층 발생)
        Category child = Category.builder().id(childId).name("소설").path("").build();
        Category parent = Category.builder().id(newParentId).name("문학").path("/1").build();

        given(categoryRepository.findById(childId)).willReturn(Optional.of(child));
        given(categoryRepository.findById(newParentId)).willReturn(Optional.of(parent));

        assertThrows(CategoryHierarchyCycleException.class, () -> {
            categoryService.changeParent(childId, new CategoryChangeParentRequest(newParentId));
        });
    }

    @Test
    @DisplayName("카테고리 부모 변경 - 정상 동작 (path 갱신 포함)")
    void changeParent_success() {
        long childId = 3L;
        long parentId = 1L;

        Category parent = Category.builder().id(parentId).name("문학").path("").build();
        Category child = Category.builder().id(childId).name("소설").path("").build();

        given(categoryRepository.findById(childId)).willReturn(Optional.of(child));
        given(categoryRepository.findById(parentId)).willReturn(Optional.of(parent));
        given(categoryRepository.findAllByPathStartsWith("/3", Sort.by(Sort.Direction.ASC, "path")))
            .willReturn(List.of());

        categoryService.changeParent(childId, new CategoryChangeParentRequest(parentId));

        assertThat(child.getParent()).isEqualTo(parent);
        assertThat(child.getPath()).isEqualTo("/1");
    }
}
