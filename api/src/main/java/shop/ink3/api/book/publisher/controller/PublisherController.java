package shop.ink3.api.book.publisher.controller;

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
import shop.ink3.api.book.publisher.dto.PublisherCreateRequest;
import shop.ink3.api.book.publisher.dto.PublisherResponse;
import shop.ink3.api.book.publisher.dto.PublisherUpdateRequest;
import shop.ink3.api.book.publisher.service.PublisherService;
import shop.ink3.api.common.dto.CommonResponse;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.common.exception.BadRequestException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pubs")
public class PublisherController {

    private final PublisherService publisherService;

    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<PublisherResponse>>> getPublishers(Pageable pageable) {
        return ResponseEntity.ok(CommonResponse.success(publisherService.getPublishers(pageable)));
    }

    @GetMapping("/detail")
    public ResponseEntity<CommonResponse<PublisherResponse>> getPublisher(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name) {

        if (id != null) {
            return ResponseEntity.ok(CommonResponse.success(publisherService.getPublisherById(id)));
        } else if (name != null) {
            return ResponseEntity.ok(CommonResponse.success(publisherService.getPublisherByName(name)));
        } else {
            throw new BadRequestException("Either id or name must be provided");
        }
    }

    @PostMapping
    public ResponseEntity<CommonResponse<PublisherResponse>> createPublisher(@RequestBody @Valid PublisherCreateRequest publisherCreateRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(publisherService.createPublisher(publisherCreateRequest)));
    }

    @PutMapping("/{publisherId}")
    public ResponseEntity<CommonResponse<PublisherResponse>> updatePublisher(@PathVariable Long publisherId,
                                                                             @RequestBody @Valid PublisherUpdateRequest publisherUpdateRequest) {
        return ResponseEntity.ok(CommonResponse.update(publisherService.updatePublisher(publisherId, publisherUpdateRequest)));
    }

    @DeleteMapping("/{publisherId}")
    public ResponseEntity<Void> deletePublisher(@PathVariable Long publisherId) {
        publisherService.deletePublisher(publisherId);
        return ResponseEntity.noContent().build();
    }
}