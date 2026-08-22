package com.store.service;

import com.store.dto.request.CategoryRequest;
import com.store.dto.response.CategoryResponse;
import com.store.dto.response.PageResponse;
import com.store.entity.category.Category;
import com.store.entity.category.CategoryStatus;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.CategoryRepository;
import com.store.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category rootComponent;
    private Category childCpu;
    private Category grandChildIntel;

    @BeforeEach
    void setUp() {
        rootComponent = Category.builder()
                .categoryId(1)
                .name("Linh kiện máy tính")
                .slug("linh-kien-may-tinh")
                .parent(null)
                .sortOrder(1)
                .status(CategoryStatus.ACTIVE)
                .children(new ArrayList<>())
                .build();

        childCpu = Category.builder()
                .categoryId(2)
                .name("CPU - Bộ vi xử lý")
                .slug("cpu-bo-vi-xu-ly")
                .parent(rootComponent)
                .sortOrder(1)
                .status(CategoryStatus.ACTIVE)
                .children(new ArrayList<>())
                .build();

        grandChildIntel = Category.builder()
                .categoryId(3)
                .name("CPU Intel")
                .slug("cpu-intel")
                .parent(childCpu)
                .sortOrder(1)
                .status(CategoryStatus.ACTIVE)
                .children(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("Read Operations")
    class ReadTests {

        @Test
        @DisplayName("getAllCategories should return flat list of category responses")
        void getAllCategories_success() {
            when(categoryRepository.findAll(any(Sort.class)))
                    .thenReturn(List.of(rootComponent, childCpu));

            List<CategoryResponse> result = categoryService.getAllCategories();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Linh kiện máy tính");
            assertThat(result.get(1).getParentId()).isEqualTo(1);
            assertThat(result.get(1).getParentName()).isEqualTo("Linh kiện máy tính");
        }

        @Test
        @DisplayName("getCategoryTree should build nested tree structure with 1 single query")
        void getCategoryTree_success() {
            when(categoryRepository.findAll(any(Sort.class)))
                    .thenReturn(List.of(rootComponent, childCpu, grandChildIntel));

            List<CategoryResponse> tree = categoryService.getCategoryTree();

            // Root level must have 1 root node
            assertThat(tree).hasSize(1);
            CategoryResponse root = tree.get(0);
            assertThat(root.getCategoryId()).isEqualTo(1);
            assertThat(root.getName()).isEqualTo("Linh kiện máy tính");

            // First level children
            assertThat(root.getChildren()).hasSize(1);
            CategoryResponse cpu = root.getChildren().get(0);
            assertThat(cpu.getCategoryId()).isEqualTo(2);
            assertThat(cpu.getName()).isEqualTo("CPU - Bộ vi xử lý");

            // Second level grandchild
            assertThat(cpu.getChildren()).hasSize(1);
            CategoryResponse intel = cpu.getChildren().get(0);
            assertThat(intel.getCategoryId()).isEqualTo(3);
            assertThat(intel.getName()).isEqualTo("CPU Intel");
        }

        @Test
        @DisplayName("getRootCategories should return only top-level categories")
        void getRootCategories_success() {
            when(categoryRepository.findByParentIsNullOrderBySortOrderAscNameAsc())
                    .thenReturn(List.of(rootComponent));

            List<CategoryResponse> result = categoryService.getRootCategories();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getParentId()).isNull();
        }

        @Test
        @DisplayName("getChildrenByParentId should return direct subcategories when parent exists")
        void getChildrenByParentId_whenParentExists() {
            when(categoryRepository.existsById(1)).thenReturn(true);
            when(categoryRepository.findByParent_CategoryIdOrderBySortOrderAscNameAsc(1))
                    .thenReturn(List.of(childCpu));

            List<CategoryResponse> children = categoryService.getChildrenByParentId(1);

            assertThat(children).hasSize(1);
            assertThat(children.get(0).getCategoryId()).isEqualTo(2);
        }

        @Test
        @DisplayName("getChildrenByParentId should throw ResourceNotFoundException when parent does not exist")
        void getChildrenByParentId_whenParentNotFound() {
            when(categoryRepository.existsById(99)).thenReturn(false);

            assertThatThrownBy(() -> categoryService.getChildrenByParentId(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Parent category not found with id: 99");
        }

        @Test
        @DisplayName("getCategoryById should return category when found")
        void getCategoryById_found() {
            when(categoryRepository.findById(1)).thenReturn(Optional.of(rootComponent));

            CategoryResponse response = categoryService.getCategoryById(1);

            assertThat(response).isNotNull();
            assertThat(response.getCategoryId()).isEqualTo(1);
            assertThat(response.getName()).isEqualTo("Linh kiện máy tính");
        }

        @Test
        @DisplayName("getCategoryById should throw ResourceNotFoundException when not found")
        void getCategoryById_notFound() {
            when(categoryRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategoryById(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found with id: 99");
        }

        @Test
        @DisplayName("getCategoryBySlug should return category when found")
        void getCategoryBySlug_found() {
            when(categoryRepository.findBySlug("linh-kien-may-tinh")).thenReturn(Optional.of(rootComponent));

            CategoryResponse response = categoryService.getCategoryBySlug("linh-kien-may-tinh");

            assertThat(response).isNotNull();
            assertThat(response.getSlug()).isEqualTo("linh-kien-may-tinh");
        }

        @Test
        @DisplayName("getCategoriesPaginated should return PageResponse")
        void getCategoriesPaginated_success() {
            Page<Category> page = new PageImpl<>(List.of(rootComponent, childCpu), PageRequest.of(0, 10), 2);
            when(categoryRepository.findAll(any(PageRequest.class))).thenReturn(page);

            PageResponse<CategoryResponse> result = categoryService.getCategoriesPaginated(0, 10, "sortOrder", "asc");

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Create Operations")
    class CreateTests {

        @Test
        @DisplayName("createCategory as root should succeed")
        void createCategory_root_success() {
            CategoryRequest request = CategoryRequest.builder()
                    .name("Laptop")
                    .slug("laptop")
                    .status("active")
                    .sortOrder(1)
                    .build();

            when(categoryRepository.existsByNameAndParentIsNull("Laptop")).thenReturn(false);
            when(categoryRepository.existsBySlug("laptop")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setCategoryId(10);
                return c;
            });

            CategoryResponse created = categoryService.createCategory(request);

            assertThat(created).isNotNull();
            assertThat(created.getCategoryId()).isEqualTo(10);
            assertThat(created.getName()).isEqualTo("Laptop");
            assertThat(created.getParentId()).isNull();
            assertThat(created.getStatus()).isEqualTo("active");
        }

        @Test
        @DisplayName("createCategory with parent should succeed")
        void createCategory_withParent_success() {
            CategoryRequest request = CategoryRequest.builder()
                    .name("VGA - Card màn hình")
                    .slug("vga-card-man-hinh")
                    .parentId(1)
                    .status("active")
                    .build();

            when(categoryRepository.findById(1)).thenReturn(Optional.of(rootComponent));
            when(categoryRepository.existsByNameAndParent_CategoryId("VGA - Card màn hình", 1)).thenReturn(false);
            when(categoryRepository.existsBySlug("vga-card-man-hinh")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setCategoryId(20);
                return c;
            });

            CategoryResponse created = categoryService.createCategory(request);

            assertThat(created).isNotNull();
            assertThat(created.getCategoryId()).isEqualTo(20);
            assertThat(created.getParentId()).isEqualTo(1);
        }

        @Test
        @DisplayName("createCategory with nonexistent parent should throw ResourceNotFoundException")
        void createCategory_parentNotFound() {
            CategoryRequest request = CategoryRequest.builder()
                    .name("VGA")
                    .parentId(999)
                    .build();

            when(categoryRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Parent category not found with id: 999");
        }

        @Test
        @DisplayName("createCategory with duplicate name under same parent should throw DuplicateResourceException")
        void createCategory_duplicateNameUnderSameParent() {
            CategoryRequest request = CategoryRequest.builder()
                    .name("CPU - Bộ vi xử lý")
                    .parentId(1)
                    .build();

            when(categoryRepository.findById(1)).thenReturn(Optional.of(rootComponent));
            when(categoryRepository.existsByNameAndParent_CategoryId("CPU - Bộ vi xử lý", 1)).thenReturn(true);

            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("already exists under parent id 1");
        }

        @Test
        @DisplayName("createCategory with duplicate slug globally should throw DuplicateResourceException")
        void createCategory_duplicateSlug() {
            CategoryRequest request = CategoryRequest.builder()
                    .name("CPU AMD")
                    .slug("cpu-bo-vi-xu-ly")
                    .parentId(1)
                    .build();

            when(categoryRepository.findById(1)).thenReturn(Optional.of(rootComponent));
            when(categoryRepository.existsByNameAndParent_CategoryId("CPU AMD", 1)).thenReturn(false);
            when(categoryRepository.existsBySlug("cpu-bo-vi-xu-ly")).thenReturn(true);

            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Category already exists with slug: cpu-bo-vi-xu-ly");
        }
    }

    @Nested
    @DisplayName("Update Operations")
    class UpdateTests {

        @Test
        @DisplayName("updateCategory should succeed")
        void updateCategory_success() {
            CategoryRequest request = CategoryRequest.builder()
                    .name("Linh kiện PC & Laptop")
                    .slug("linh-kien-pc-laptop")
                    .status("inactive")
                    .build();

            when(categoryRepository.findById(1)).thenReturn(Optional.of(rootComponent));
            when(categoryRepository.existsByNameAndParentIsNullAndCategoryIdNot("Linh kiện PC & Laptop", 1)).thenReturn(false);
            when(categoryRepository.existsBySlugAndCategoryIdNot("linh-kien-pc-laptop", 1)).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(rootComponent);

            CategoryResponse updated = categoryService.updateCategory(1, request);

            assertThat(updated).isNotNull();
            assertThat(rootComponent.getName()).isEqualTo("Linh kiện PC & Laptop");
            assertThat(rootComponent.getStatus()).isEqualTo(CategoryStatus.INACTIVE);
        }

        @Test
        @DisplayName("updateCategory cannot set category as its own parent")
        void updateCategory_selfAsParent_shouldThrow() {
            CategoryRequest request = CategoryRequest.builder()
                    .name("Linh kiện máy tính")
                    .parentId(1)
                    .build();

            when(categoryRepository.findById(1)).thenReturn(Optional.of(rootComponent));

            assertThatThrownBy(() -> categoryService.updateCategory(1, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("A category cannot be its own parent.");
        }

        @Test
        @DisplayName("updateCategory cannot set descendant as parent (circular reference check)")
        void updateCategory_circularHierarchy_shouldThrow() {
            // Attempting to set grandChildIntel (ID: 3) as parent of rootComponent (ID: 1)
            // Hierarchy: rootComponent (1) -> childCpu (2) -> grandChildIntel (3)
            CategoryRequest request = CategoryRequest.builder()
                    .name("Linh kiện máy tính")
                    .parentId(3)
                    .build();

            when(categoryRepository.findById(1)).thenReturn(Optional.of(rootComponent));
            when(categoryRepository.findById(3)).thenReturn(Optional.of(grandChildIntel));

            assertThatThrownBy(() -> categoryService.updateCategory(1, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Circular reference detected");
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteTests {

        @Test
        @DisplayName("deleteCategory should delete successfully when found")
        void deleteCategory_success() {
            when(categoryRepository.findById(1)).thenReturn(Optional.of(rootComponent));

            categoryService.deleteCategory(1);

            verify(categoryRepository).delete(rootComponent);
        }

        @Test
        @DisplayName("deleteCategory should throw ResourceNotFoundException when not found")
        void deleteCategory_notFound() {
            when(categoryRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.deleteCategory(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found with id: 99");

            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("countChildren should return number of subcategories")
        void countChildren_success() {
            when(categoryRepository.countByParent_CategoryId(1)).thenReturn(5L);

            long count = categoryService.countChildren(1);

            assertThat(count).isEqualTo(5L);
        }
    }
}
