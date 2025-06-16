package shop.ink3.api.user.point.policy.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import shop.ink3.api.user.point.policy.dto.PointPolicyCreateRequest;
import shop.ink3.api.user.point.policy.dto.PointPolicyResponse;
import shop.ink3.api.user.point.policy.dto.PointPolicyUpdateRequest;
import shop.ink3.api.user.point.policy.entity.PointPolicy;
import shop.ink3.api.user.point.policy.repository.PointPolicyRepository;

@ExtendWith(MockitoExtension.class)
public class PointPolicyServiceTest {

    @Mock private PointPolicyRepository pointPolicyRepository;
    @InjectMocks private PointPolicyService pointPolicyService;

    @Test
    @DisplayName("포인트 정책 생성")
    void createPointPolicy() {
        PointPolicyCreateRequest request = new PointPolicyCreateRequest("가입 포인트", 1000, 500, 800, 10);
        PointPolicy savedPolicy = new PointPolicy("가입 포인트", 1000, 500, 800, 10);

        when(pointPolicyRepository.save(any(PointPolicy.class))).thenReturn(savedPolicy);

        PointPolicyResponse response = pointPolicyService.createPointPolicy(request);

        assertThat(response.name()).isEqualTo("가입 포인트");
    }

    @Test
    @DisplayName("포인트 정책 단건 조회")
    void getPointPolicy() {
        PointPolicy policy = new PointPolicy("정책", 1000, 500, 800, 5);
        when(pointPolicyRepository.findById(1L)).thenReturn(Optional.of(policy));

        PointPolicyResponse response = pointPolicyService.getPointPolicy(1L);

        assertThat(response.name()).isEqualTo("정책");
    }

    @Test
    @DisplayName("포인트 정책 전체 조회")
    void getPointPolicies() {
        PointPolicy policy = new PointPolicy("정책", 1000, 500, 800, 5);
        Pageable pageable = PageRequest.of(0, 10);
        when(pointPolicyRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(policy)));

        var result = pointPolicyService.getPointPolicies(pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("정책");
    }

    @Test
    @DisplayName("포인트 정책 수정")
    void updatePointPolicy() {
        PointPolicy policy = new PointPolicy("정책", 1000, 500, 800, 5);
        PointPolicyUpdateRequest request = new PointPolicyUpdateRequest("변경", 2000, 1000, 1000, 8);

        when(pointPolicyRepository.findById(1L)).thenReturn(Optional.of(policy));
        when(pointPolicyRepository.save(any(PointPolicy.class))).thenReturn(policy);

        PointPolicyResponse response = pointPolicyService.updatePointPolicy(1L, request);

        assertThat(response.name()).isEqualTo("변경");
    }

    @Test
    @DisplayName("포인트 정책 삭제 - 성공")
    void deletePolicySuccess() {
        PointPolicy policy = new PointPolicy("정책", 1000, 500, 800, 5);
        when(pointPolicyRepository.findById(1L)).thenReturn(Optional.of(policy));

        pointPolicyService.deletePointPolicy(1L);
        verify(pointPolicyRepository).deleteById(1L);
    }

    @Test
    @DisplayName("포인트 정책 삭제 - 실패 (활성화 정책)")
    void deletePolicyFailIfActive() {
        PointPolicy policy = new PointPolicy("정책", 1000, 500, 800, 5);
        policy.activate();
        when(pointPolicyRepository.findById(1L)).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> pointPolicyService.deletePointPolicy(1L))
            .isInstanceOf(shop.ink3.api.user.point.policy.exception.CannotDeleteActivePointPolicyException.class);
    }
}
