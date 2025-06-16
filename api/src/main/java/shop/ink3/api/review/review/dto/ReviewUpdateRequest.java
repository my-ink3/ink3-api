package shop.ink3.api.review.review.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewUpdateRequest {
    private String title;
    private String content;
    private int rating;

    public ReviewUpdateRequest(String title, String content, int rating) {
        this.title = title;
        this.content = content;
        this.rating = rating;
    }
}
