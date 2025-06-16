package shop.ink3.api.book.category.exception;

public class SelfParentingCategoryException extends RuntimeException {
    public SelfParentingCategoryException(long id) {
        super("카테고리 [" + id + "] 가 자기 자신을 부모로 설정할 수 없습니다.");
    }
}
