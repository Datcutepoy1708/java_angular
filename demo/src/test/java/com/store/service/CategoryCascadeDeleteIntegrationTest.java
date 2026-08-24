package com.store.service;

import com.store.dto.request.CategoryRequest;
import com.store.dto.response.CategoryResponse;
import com.store.entity.category.Category;
import com.store.entity.category.CategoryStatus;
import com.store.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
@Transactional
class CategoryCascadeDeleteIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("Soft-delete parent category cascades to all 3-level descendants and keeps original status")
    void softDelete_parent_cascadesAllDescendants_and_preservesStatus() {
        long ts = System.currentTimeMillis();

        // 1. Create 3-level category hierarchy: A -> B -> C
        CategoryResponse catA = categoryService.createCategory(CategoryRequest.builder()
                .name("Cat A " + ts)
                .slug("cat-a-" + ts)
                .status("active")
                .build());

        CategoryResponse catB = categoryService.createCategory(CategoryRequest.builder()
                .name("Cat B " + ts)
                .slug("cat-b-" + ts)
                .parentId(catA.getCategoryId())
                .status("inactive") // specific status to test preservation
                .build());

        CategoryResponse catC = categoryService.createCategory(CategoryRequest.builder()
                .name("Cat C " + ts)
                .slug("cat-c-" + ts)
                .parentId(catB.getCategoryId())
                .status("active")
                .build());

        Integer aId = catA.getCategoryId();
        Integer bId = catB.getCategoryId();
        Integer cId = catC.getCategoryId();

        // 2. Soft-delete parent A
        categoryService.softDeleteCategory(aId);

        // 3. Verify all 3 have deleted_at set (in trash) and original status is untouched
        Category entityA = categoryRepository.findById(aId).orElseThrow();
        Category entityB = categoryRepository.findById(bId).orElseThrow();
        Category entityC = categoryRepository.findById(cId).orElseThrow();

        assertThat(entityA.getDeletedAt()).isNotNull();
        assertThat(entityB.getDeletedAt()).isNotNull();
        assertThat(entityC.getDeletedAt()).isNotNull();

        assertThat(entityA.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
        assertThat(entityB.getStatus()).isEqualTo(CategoryStatus.INACTIVE); // preserved
        assertThat(entityC.getStatus()).isEqualTo(CategoryStatus.ACTIVE);

        // 4. Restore parent A -> all descendants in subtree should have deleted_at cleared
        categoryService.restoreCategory(aId);

        Category restoredA = categoryRepository.findById(aId).orElseThrow();
        Category restoredB = categoryRepository.findById(bId).orElseThrow();
        Category restoredC = categoryRepository.findById(cId).orElseThrow();

        assertThat(restoredA.getDeletedAt()).isNull();
        assertThat(restoredB.getDeletedAt()).isNull();
        assertThat(restoredC.getDeletedAt()).isNull();

        assertThat(restoredB.getStatus()).isEqualTo(CategoryStatus.INACTIVE); // still inactive!
    }

    @Test
    @DisplayName("Restoring parent also restores child that was independently soft-deleted prior to parent")
    void restore_parent_alsoRestoresIndependentlyDeletedChild() {
        long ts = System.currentTimeMillis();

        CategoryResponse catA = categoryService.createCategory(CategoryRequest.builder()
                .name("Cat Parent " + ts)
                .slug("cat-parent-" + ts)
                .status("active")
                .build());

        CategoryResponse catB = categoryService.createCategory(CategoryRequest.builder()
                .name("Cat Child " + ts)
                .slug("cat-child-" + ts)
                .parentId(catA.getCategoryId())
                .status("active")
                .build());

        Integer aId = catA.getCategoryId();
        Integer bId = catB.getCategoryId();

        // 1. Delete child B independently first
        categoryService.softDeleteCategory(bId);
        assertThat(categoryRepository.findById(bId).orElseThrow().getDeletedAt()).isNotNull();

        // 2. Delete parent A
        categoryService.softDeleteCategory(aId);
        assertThat(categoryRepository.findById(aId).orElseThrow().getDeletedAt()).isNotNull();

        // 3. Restore parent A -> B is also restored per agreed specification
        categoryService.restoreCategory(aId);

        assertThat(categoryRepository.findById(aId).orElseThrow().getDeletedAt()).isNull();
        assertThat(categoryRepository.findById(bId).orElseThrow().getDeletedAt()).isNull();
    }
}
