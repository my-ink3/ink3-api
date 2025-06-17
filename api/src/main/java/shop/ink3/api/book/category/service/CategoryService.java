package shop.ink3.api.book.category.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.ink3.api.book.category.dto.CategoryChangeParentRequest;
import shop.ink3.api.book.category.dto.CategoryCreateRequest;
import shop.ink3.api.book.category.dto.CategoryFlatDto;
import shop.ink3.api.book.category.dto.CategoryTreeDto;
import shop.ink3.api.book.category.dto.CategoryUpdateNameRequest;
import shop.ink3.api.book.category.entity.Category;
import shop.ink3.api.book.category.exception.CategoryAlreadyExistsException;
import shop.ink3.api.book.category.exception.CategoryHasChildrenException;
import shop.ink3.api.book.category.exception.CategoryHierarchyCycleException;
import shop.ink3.api.book.category.exception.CategoryNotFoundException;
import shop.ink3.api.book.category.exception.SelfParentingCategoryException;
import shop.ink3.api.book.category.repository.CategoryRepository;

@Transactional
@RequiredArgsConstructor
@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "categories")
    public List<CategoryTreeDto> getCategoriesTree() {
        List<Category> categories = categoryRepository.findAll(Sort.by(Direction.ASC, "path"));
        return buildTree(categories);
    }

    @Transactional(readOnly = true)
    public List<CategoryFlatDto> getCategoriesFlat() {
        List<Category> categories = categoryRepository.findAll(Sort.by(Direction.ASC, "path"));
        return categories.stream().map(CategoryFlatDto::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryTreeDto getAllDescendants(long id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        List<Category> descendents = categoryRepository.findAllByPathStartsWith(
                category.getPath() + "/" + category.getId(),
                Sort.by(Direction.ASC, "path")
        );
        return buildTree(category, descendents);
    }

    @Transactional(readOnly = true)
    public List<CategoryFlatDto> getAllAncestors(long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        if (category.getPath().isEmpty()) {
            return List.of(CategoryFlatDto.from(category));
        }

        List<Long> ancestorIds = Arrays.stream(category.getPath().split("/"))
                .filter(s -> s.matches("\\d+"))
                .map(Long::parseLong)
                .toList();

        Map<Long, Category> ancestorMap = categoryRepository.findAllById(ancestorIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<CategoryFlatDto> ancestors = ancestorIds.stream()
                .map(ancestorMap::get)
                .filter(Objects::nonNull)
                .map(CategoryFlatDto::from)
                .collect(Collectors.toCollection(ArrayList::new));

        ancestors.add(CategoryFlatDto.from(category));
        return ancestors;
    }

    @CacheEvict(value = "categories", allEntries = true)
    public CategoryTreeDto createCategory(CategoryCreateRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new CategoryAlreadyExistsException(request.name());
        }

        Category parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.parentId()));
        }

        Category category = Category.builder()
                .name(request.name())
                .parent(parent)
                .path(parent != null ? parent.getPath() + "/" + parent.getId() : "")
                .build();
        category = categoryRepository.save(category);
        return new CategoryTreeDto(category.getId(), category.getName(), new ArrayList<>());
    }

    @CacheEvict(value = "categories", allEntries = true)
    public List<List<CategoryFlatDto>> createCategoryHierarchy(String categoryPath) {
        List<Category> categories = new ArrayList<>();
        String[] categoryNames = categoryPath.split(">");

        Arrays.stream(categoryNames)
                .map(String::strip)
                .distinct()
                .map(c -> categoryRepository.findByName(c)
                        .orElseGet(() -> categoryRepository.save(Category.builder().name(c).path("").build())))
                .forEach(categories::add);

        for (int i = 1; i < categories.size(); i++) {
            categories.get(i).updateParent(categories.get(i - 1));
            categories.get(i).updatePath(categories.get(i - 1).getPath() + "/" + categories.get(i - 1).getId());
        }

        return List.of(categories.stream().map(CategoryFlatDto::from).toList());
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void updateCategoryName(long id, CategoryUpdateNameRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        category.updateName(request.name());
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void changeParent(long id, CategoryChangeParentRequest request) {
        if (id == request.parentId()) {
            throw new SelfParentingCategoryException(id);
        }

        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        Category newParent = categoryRepository.findById(request.parentId())
                .orElseThrow(() -> new CategoryNotFoundException(request.parentId()));

        String oldPath = category.getPath() + "/" + id;
        String newPath = newParent.getPath() + "/" + newParent.getId();

        if (newPath.startsWith(oldPath)) {
            throw new CategoryHierarchyCycleException(id, request.parentId());
        }

        category.updateParent(newParent);
        category.updatePath(newPath);

        List<Category> descendants = categoryRepository.findAllByPathStartsWith(oldPath,
                Sort.by(Direction.ASC, "path"));
        descendants.forEach(descendant -> {
            String updatedPath = descendant.getPath().replaceFirst(oldPath, newPath);
            descendant.updatePath(updatedPath);
        });
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(long id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));

        boolean hasChildren = categoryRepository.existsByPathStartingWith(category.getPath() + "/" + category.getId());

        if (hasChildren) {
            throw new CategoryHasChildrenException(id);
        }

        categoryRepository.deleteById(id);
    }

    private List<CategoryTreeDto> buildTree(List<Category> categories) {
        Map<Long, CategoryTreeDto> dtoMap = new HashMap<>();
        List<CategoryTreeDto> roots = new ArrayList<>();

        categories.forEach(category -> dtoMap.put(
                category.getId(),
                new CategoryTreeDto(category.getId(), category.getName(), new ArrayList<>())
        ));

        categories.forEach(category -> {
            CategoryTreeDto current = dtoMap.get(category.getId());
            if (category.isRoot()) {
                roots.add(current);
            } else {
                dtoMap.get(category.getParent().getId()).children().add(current);
            }
        });

        return roots;
    }

    private CategoryTreeDto buildTree(Category root, List<Category> descendents) {
        Map<Long, CategoryTreeDto> dtoMap = new HashMap<>();

        CategoryTreeDto rootDto = new CategoryTreeDto(root.getId(), root.getName(), new ArrayList<>());

        descendents.forEach(descendent -> dtoMap.put(
                descendent.getId(),
                new CategoryTreeDto(descendent.getId(), descendent.getName(), new ArrayList<>())
        ));

        descendents.forEach(descendent -> {
            CategoryTreeDto current = dtoMap.get(descendent.getId());
            if (Objects.equals(descendent.getParent().getId(), root.getId())) {
                rootDto.children().add(current);
            } else {
                dtoMap.get(descendent.getParent().getId()).children().add(current);
            }
        });
        return rootDto;
    }
}
