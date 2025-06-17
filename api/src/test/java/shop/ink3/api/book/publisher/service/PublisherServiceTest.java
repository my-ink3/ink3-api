package shop.ink3.api.book.publisher.service;

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
import shop.ink3.api.book.publisher.dto.PublisherCreateRequest;
import shop.ink3.api.book.publisher.dto.PublisherResponse;
import shop.ink3.api.book.publisher.dto.PublisherUpdateRequest;
import shop.ink3.api.book.publisher.entity.Publisher;
import shop.ink3.api.book.publisher.exception.PublisherAlreadyExistsException;
import shop.ink3.api.book.publisher.exception.PublisherNotFoundException;
import shop.ink3.api.book.publisher.repository.PublisherRepository;
import shop.ink3.api.common.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class PublisherServiceTest {
    @Mock
    PublisherRepository publisherRepository;

    @InjectMocks
    PublisherService publisherService;

    @Test
    void getPublisherById() {
        Publisher publisher = Publisher.builder()
                .id(1L)
                .name("testPublisher")
                .build();
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        PublisherResponse response = publisherService.getPublisherById(1L);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(PublisherResponse.from(publisher), response);
    }

    @Test
    void getPublisherByIdWithNotFound() {
        when(publisherRepository.findById(1L)).thenThrow(new PublisherNotFoundException(1L));
        Assertions.assertThrows(PublisherNotFoundException.class, () -> publisherService.getPublisherById(1L));
    }

    @Test
    void getPublisherByName() {
        Publisher publisher = Publisher.builder()
                .id(1L)
                .name("testPublisher")
                .build();
        when(publisherRepository.findByName("testPublisher")).thenReturn(Optional.of(publisher));
        PublisherResponse response = publisherService.getPublisherByName("testPublisher");
        Assertions.assertNotNull(response);
        Assertions.assertEquals(PublisherResponse.from(publisher), response);
    }

    @Test
    void getPublisherByNameWithNotFound() {
        when(publisherRepository.findByName("testPublisher")).thenThrow(new PublisherNotFoundException("testPublisher"));
        Assertions.assertThrows(PublisherNotFoundException.class, () -> publisherService.getPublisherByName("testPublisher"));
    }

    @Test
    void getPublishers() {
        List<Publisher> publishers = List.of(
                Publisher.builder()
                        .id(1L)
                        .name("testPublisher1")
                        .build(),
                Publisher.builder()
                        .id(2L)
                        .name("testPublisher2")
                        .build()
        );
        Pageable pageable = PageRequest.of(0, 10);
        Page<Publisher> page = new PageImpl<>(
                publishers,
                pageable,
                publishers.size()
        );
        when(publisherRepository.findAll(pageable)).thenReturn(page);
        PageResponse<PublisherResponse> response = publisherService.getPublishers(pageable);

        Assertions.assertEquals(0, response.page());
        Assertions.assertEquals(10, response.size());
        Assertions.assertEquals(2, response.totalElements());
        Assertions.assertEquals(1, response.totalPages());
        Assertions.assertEquals("testPublisher1", response.content().get(0).name());
        Assertions.assertEquals("testPublisher2", response.content().get(1).name());
        verify(publisherRepository, times(1)).findAll(pageable);
    }

    @Test
    void createPublisher() {
        PublisherCreateRequest request = new PublisherCreateRequest("testPublisher");
        when(publisherRepository.existsByName("testPublisher")).thenReturn(false);
        when(publisherRepository.save(any(Publisher.class))).thenAnswer(inv -> inv.getArgument(0));
        PublisherResponse response = publisherService.createPublisher(request);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(request.name(), response.name());
    }

    @Test
    void createPublisherWithAlreadyExists() {
        PublisherCreateRequest request = new PublisherCreateRequest("testPublisher");
        when(publisherRepository.existsByName("testPublisher")).thenReturn(true);
        Assertions.assertThrows(PublisherAlreadyExistsException.class, () -> publisherService.createPublisher(request));
    }

    @Test
    void updatePublisher() {
        Publisher publisher = Publisher.builder()
                .id(1L)
                .name("testPublisher")
                .build();
        PublisherUpdateRequest request = new PublisherUpdateRequest("newPublisher");
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        when(publisherRepository.save(any(Publisher.class))).thenAnswer(inv -> inv.getArgument(0));
        PublisherResponse response = publisherService.updatePublisher(1L, request);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(1L, response.id());
        Assertions.assertEquals(request.name(), response.name());
    }

    @Test
    void updatePublisherWithNotFound() {
        PublisherUpdateRequest request = new PublisherUpdateRequest("newPublisher");
        when(publisherRepository.findById(1L)).thenThrow(new PublisherNotFoundException(1L));
        Assertions.assertThrows(PublisherNotFoundException.class, () -> publisherService.updatePublisher(1L, request));
    }

    @Test
    void updatePublisherWithAlreadyExists() {
        Publisher publisher = Publisher.builder()
                .id(1L)
                .name("oldPublisher")
                .build();
        PublisherUpdateRequest request = new PublisherUpdateRequest("newPublisher");
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        when(publisherRepository.existsByName("newPublisher")).thenReturn(true);
        Assertions.assertThrows(PublisherAlreadyExistsException.class, () -> publisherService.updatePublisher(1L, request));
    }

    @Test
    void deletePublisher() {
        Publisher publisher = Publisher.builder().id(1L).build();
        when(publisherRepository.findById(1L)).thenReturn(Optional.of(publisher));
        publisherService.deletePublisher(1L);
        verify(publisherRepository, times(1)).delete(publisher);
    }

    @Test
    void deletePublisherWithNotFound() {
        when(publisherRepository.findById(1L)).thenThrow(new PublisherNotFoundException(1L));
        Assertions.assertThrows(PublisherNotFoundException.class, () -> publisherService.deletePublisher(1L));
    }
}
