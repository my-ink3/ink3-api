package shop.ink3.api.book.category.exception;

public class CategoryHierarchyCycleException extends RuntimeException {
    public CategoryHierarchyCycleException(long categoryId, long parentId) {
        super("카테고리 [" + categoryId + "]는 자식 카테고리 [" + parentId + "]를 부모로 설정할 수 없습니다.");
    }
}
