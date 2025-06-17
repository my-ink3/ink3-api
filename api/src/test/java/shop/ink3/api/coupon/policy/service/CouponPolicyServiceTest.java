package shop.ink3.api.coupon.policy.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.coupon.policy.dto.PolicyCreateRequest;
import shop.ink3.api.coupon.policy.dto.PolicyResponse;
import shop.ink3.api.coupon.policy.dto.PolicyUpdateRequest;
import shop.ink3.api.coupon.policy.entity.CouponPolicy;
import shop.ink3.api.coupon.policy.entity.DiscountType;
import shop.ink3.api.coupon.policy.exception.PolicyAlreadyExistException;
import shop.ink3.api.coupon.policy.exception.PolicyNotFoundException;
import shop.ink3.api.coupon.policy.repository.PolicyRepository;

@ExtendWith(MockitoExtension.class)
class CouponPolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private PolicyService policyService;

    @Test
    void getPolicy_success() {
        CouponPolicy policy1 = CouponPolicy.builder()
            .id(1L)
            .name("POLICY1")
            .discountType(DiscountType.RATE)
            .discountPercentage(10)
            .discountValue(0)
            .minimumOrderAmount(1000)
            .maximumDiscountAmount(3000)
            .createdAt(LocalDateTime.now())
            .build();

        CouponPolicy policy2 = CouponPolicy.builder()
            .id(2L)
            .name("POLICY2")
            .discountType(DiscountType.FIXED)
            .discountPercentage(0)
            .discountValue(2000)
            .minimumOrderAmount(5000)
            .maximumDiscountAmount(2000)
            .createdAt(LocalDateTime.now())
            .build();

        Page<CouponPolicy> policyPage = new PageImpl<>(List.of(policy1, policy2), PageRequest.of(0, 10), 2);

        when(policyRepository.findAll(any(PageRequest.class))).thenReturn(policyPage);

        // when
        PageResponse<PolicyResponse> result = policyService.getPolicy(PageRequest.of(0, 10));

        // then
        Assertions.assertEquals(2, result.totalElements());
        Assertions.assertEquals("POLICY1", result.content().get(0).policyName());
        Assertions.assertEquals("POLICY2", result.content().get(1).policyName());
    }

    @Test
    void getPolicyById_success() {
        // given
        CouponPolicy policy = CouponPolicy.builder()
                .id(1L)
                .name("WELCOME10")
                .discountType(DiscountType.RATE)
                .discountValue(10)
                .discountPercentage(15)
                .minimumOrderAmount(10000)
                .maximumDiscountAmount(5000)
                .createdAt(LocalDateTime.now())
                .build();

        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));

        // when
        PolicyResponse response = policyService.getPolicyById(1L);

        // then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(1L, response.policyId());
        Assertions.assertEquals("WELCOME10", response.policyName());
        Assertions.assertEquals(DiscountType.RATE, response.discountType());
        Assertions.assertEquals(10, response.discountValue());
        Assertions.assertEquals(15, response.discountPercentage());
        Assertions.assertEquals(10000, response.minimumOrderAmount());
        Assertions.assertEquals(5000, response.maximumDiscountAmount());
        Assertions.assertNotNull(response.createdAt());
    }

    @Test
    void getPolicyById_notFound_throwsException() {
        // given
        when(policyRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        Assertions.assertThrows(PolicyNotFoundException.class, () -> {
            policyService.getPolicyById(1L);
        });
    }

    @Test
    void createPolicy_success() {
        // given
        PolicyCreateRequest request = new PolicyCreateRequest(
                "WELCOME10",
                1000,               // minimumOrderAmount
                DiscountType.RATE,
                0,                  // discountValue (ignored for RATE)
                10,                 // discountPercentage
                5000                // maximumDiscountAmount
        );

        when(policyRepository.existsByName("WELCOME10")).thenReturn(false);

        CouponPolicy savedPolicy = CouponPolicy.builder()
                .id(1L)
                .name(request.name())
                .discountType(request.discountType())
                .minimumOrderAmount(request.minimumOrderAmount())
                .discountValue(request.discountValue())
                .discountPercentage(request.discountPercentage())
                .maximumDiscountAmount(request.maximumDiscountAmount())
                .createdAt(LocalDateTime.now())
                .build();

        when(policyRepository.save(any(CouponPolicy.class))).thenReturn(savedPolicy);

        // when
        PolicyResponse response = policyService.createPolicy(request);

        // then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(1L, response.policyId());
        Assertions.assertEquals("WELCOME10", response.policyName());
        Assertions.assertEquals(DiscountType.RATE, response.discountType());
        Assertions.assertEquals(0, response.discountValue());
        Assertions.assertEquals(10, response.discountPercentage());
        Assertions.assertEquals(1000, response.minimumOrderAmount());
        Assertions.assertEquals(5000, response.maximumDiscountAmount());
    }

    @Test
    void createPolicy_alreadyExists_throwsException() {
        // given
        PolicyCreateRequest request = new PolicyCreateRequest(
                "DUPLICATE",
                5000,
                DiscountType.FIXED,
                3000,
                0,
                0
        );

        when(policyRepository.existsByName("DUPLICATE")).thenReturn(true);

        // when & then
        Assertions.assertThrows(PolicyAlreadyExistException.class, () -> {
            policyService.createPolicy(request);
        });
    }

    @Test
    void updatePolicy_success() {
        // given
        PolicyUpdateRequest request = new PolicyUpdateRequest(
                "SPRINGSALE",
                DiscountType.FIXED,
                2000,
                1500,
                0,
                3000
        );

        CouponPolicy existingPolicy = CouponPolicy.builder()
                .id(1L)
                .name("OLDSALE")
                .discountType(DiscountType.RATE)
                .discountValue(0)
                .discountPercentage(5)
                .minimumOrderAmount(1000)
                .maximumDiscountAmount(3000)
                .createdAt(LocalDateTime.now())
                .build();

        when(policyRepository.findById(1L)).thenReturn(Optional.of(existingPolicy));
        when(policyRepository.save(any(CouponPolicy.class))).thenReturn(existingPolicy);

        // when
        PolicyResponse response = policyService.updatePolicy(1L, request);

        // then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(1L, response.policyId());
        Assertions.assertEquals("SPRINGSALE", response.policyName());
        Assertions.assertEquals(DiscountType.FIXED, response.discountType());
        Assertions.assertEquals(1500, response.discountValue());
        Assertions.assertEquals(0, response.discountPercentage());
        Assertions.assertEquals(3000, response.maximumDiscountAmount());
    }

    @Test
    void updatePolicy_notFound_throwsException() {
        // given
        PolicyUpdateRequest request = new PolicyUpdateRequest(
                "NOEXIST",
                DiscountType.FIXED,
                1000,
                500,
                0,
                1000
        );

        when(policyRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        Assertions.assertThrows(PolicyNotFoundException.class, () -> {
            policyService.updatePolicy(999L, request);
        });
    }

    @Test
    void deletePolicyById_success() {
        // given
        CouponPolicy policy = CouponPolicy.builder()
                .id(1L)
                .name("DELETE_ME")
                .discountType(DiscountType.RATE)
                .discountValue(0)
                .discountPercentage(10)
                .minimumOrderAmount(10000)
                .maximumDiscountAmount(5000)
                .createdAt(LocalDateTime.now())
                .build();

        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));

        // when
        PolicyResponse response = policyService.deletePolicyById(1L);

        // then
        verify(policyRepository).delete(policy);
        Assertions.assertEquals(1L, response.policyId());
        Assertions.assertEquals("DELETE_ME", response.policyName());
    }

    @Test
    void deletePolicyById_notFound_throwsException() {
        // given
        when(policyRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        Assertions.assertThrows(PolicyNotFoundException.class, () -> {
            policyService.deletePolicyById(999L);
        });
    }
}
