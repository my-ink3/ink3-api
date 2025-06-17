package shop.ink3.api.book.tag.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shop.ink3.api.book.tag.dto.TagCreateRequest;
import shop.ink3.api.book.tag.dto.TagResponse;
import shop.ink3.api.book.tag.dto.TagUpdateRequest;
import shop.ink3.api.book.tag.service.TagService;
import shop.ink3.api.common.dto.CommonResponse;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.common.exception.BadRequestException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tags")
public class TagController {
    private final TagService tagService;

    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<TagResponse>>> getTags(Pageable pageable) {
        return ResponseEntity.ok(CommonResponse.success(tagService.getTags(pageable)));
    }

    @GetMapping("/detail")
    public ResponseEntity<CommonResponse<TagResponse>> getTag(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name) {

        if (id != null) {
            return ResponseEntity.ok(CommonResponse.success(tagService.getTagById(id)));
        } else if (name != null) {
            return ResponseEntity.ok(CommonResponse.success(tagService.getTagByName(name)));
        } else {
            throw new BadRequestException("Either id or name must be provided");
        }
    }

    @PostMapping
    public ResponseEntity<CommonResponse<TagResponse>> createTag(@RequestBody @Valid TagCreateRequest tagCreateRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(tagService.createTag(tagCreateRequest)));
    }

    @PutMapping("/{tagId}")
    public ResponseEntity<CommonResponse<TagResponse>> updateTag(
            @PathVariable Long tagId,
            @RequestBody @Valid TagUpdateRequest request
    ) {
        return ResponseEntity.ok(CommonResponse.success(tagService.updateTag(tagId, request)));
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
        return ResponseEntity.noContent().build();
    }
}
