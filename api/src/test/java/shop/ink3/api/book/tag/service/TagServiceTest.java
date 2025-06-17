package shop.ink3.api.book.tag.service;

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
import shop.ink3.api.book.tag.dto.TagCreateRequest;
import shop.ink3.api.book.tag.dto.TagResponse;
import shop.ink3.api.book.tag.dto.TagUpdateRequest;
import shop.ink3.api.book.tag.entity.Tag;
import shop.ink3.api.book.tag.exception.TagAlreadyExistsException;
import shop.ink3.api.book.tag.exception.TagNotFoundException;
import shop.ink3.api.book.tag.repository.TagRepository;
import shop.ink3.api.common.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
public class TagServiceTest {
    @Mock
    TagRepository tagRepository;

    @InjectMocks
    TagService tagService;

    @Test
    void getTags() {
        List<Tag> tags = List.of(
                Tag.builder()
                        .id(1L)
                        .name("testTag1")
                        .build(),
                Tag.builder()
                        .id(2L)
                        .name("testTag2")
                        .build()
        );
        Pageable pageable = PageRequest.of(0, 10);
        Page<Tag> page = new PageImpl<>(
                tags,
                pageable,
                tags.size()
        );

        when(tagRepository.findAll(pageable)).thenReturn(page);
        PageResponse<TagResponse> response = tagService.getTags(pageable);

        Assertions.assertEquals(0, response.page());
        Assertions.assertEquals(10, response.size());
        Assertions.assertEquals(2, response.totalElements());
        Assertions.assertEquals(1, response.totalPages());
        Assertions.assertEquals("testTag1", response.content().get(0).name());
        Assertions.assertEquals("testTag2", response.content().get(1).name());
        verify(tagRepository, times(1)).findAll(pageable);
    }

    @Test
    void getTagById() {
        Tag tag = Tag.builder()
                .id(1L)
                .name("testTag1")
                .build();
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        TagResponse response = tagService.getTagById(1L);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(TagResponse.from(tag), response);
    }

    @Test
    void getTagByIdWithNotFound() {
        when(tagRepository.findById(1L)).thenThrow(new TagNotFoundException(1L));
        Assertions.assertThrows(TagNotFoundException.class, () -> tagService.getTagById(1L));
    }

    @Test
    void getTagByName() {
        Tag tag = Tag.builder()
                .id(1L)
                .name("testTag1")
                .build();
        when(tagRepository.findByName("testTag1")).thenReturn(Optional.of(tag));
        TagResponse response = tagService.getTagByName("testTag1");
        Assertions.assertNotNull(response);
        Assertions.assertEquals(TagResponse.from(tag), response);
    }

    @Test
    void getTagByNameWithNotFound() {
        when(tagRepository.findByName("testTag1")).thenThrow(new TagNotFoundException("testTag1"));
        Assertions.assertThrows(TagNotFoundException.class, () -> tagService.getTagByName("testTag1"));
    }
    
    @Test
    void createTag() {
        TagCreateRequest request = new TagCreateRequest("testTag1");
        when(tagRepository.existsByName("testTag1")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));
        TagResponse response = tagService.createTag(request);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(request.name(), response.name());
    }

    @Test
    void createTagWithAlreadyExists() {
        TagCreateRequest request = new TagCreateRequest("testTag1");
        when(tagRepository.existsByName("testTag1")).thenReturn(true);
        Assertions.assertThrows(TagAlreadyExistsException.class, () -> tagService.createTag(request));
    }
    
    @Test
    void updateTag() {
        Tag tag = Tag.builder()
                .id(1L)
                .name("oldTag")
                .build();
        TagUpdateRequest request = new TagUpdateRequest("newTag");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));
        TagResponse response = tagService.updateTag(1L, request);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(1L, response.id());
        Assertions.assertEquals(request.name(), response.name());
    }

    @Test
    void updateTagWithNotFound() {
        TagUpdateRequest request = new TagUpdateRequest("newTag");
        when(tagRepository.findById(1L)).thenThrow(new TagNotFoundException(1L));
        Assertions.assertThrows(TagNotFoundException.class, () -> tagService.updateTag(1L, request));
    }

    @Test
    void updateTagWithAlreadyExists() {
        Tag tag = Tag.builder()
                .id(1L)
                .name("oldTag")
                .build();
        TagUpdateRequest request = new TagUpdateRequest("newTag");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByName("newTag")).thenReturn(true);
        Assertions.assertThrows(TagAlreadyExistsException.class, () -> tagService.updateTag(1L, request));
    }

    @Test
    void deleteTag() {
        Tag tag = Tag.builder().id(1L).name("testTag1").build();
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        tagService.deleteTag(1L);
        verify(tagRepository, times(1)).delete(tag);
    }

    @Test
    void deleteTagWithNotFound() {
        when(tagRepository.findById(1L)).thenThrow(new TagNotFoundException(1L));
        Assertions.assertThrows(TagNotFoundException.class, () -> tagService.deleteTag(1L));
    }
}
