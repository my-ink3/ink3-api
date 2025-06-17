package shop.ink3.api.order.guest.dto;

import shop.ink3.api.order.guest.entiity.Guest;

public record GuestResponse(
        Long guestId,
        Long orderId,
        String email
) {
    public static GuestResponse from(Guest guest){
        return new GuestResponse(
                guest.getId(),
                guest.getOrder().getId(),
                guest.getEmail()
        );
    }
}
