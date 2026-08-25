package com.store.repository;

import com.store.entity.user.User;
import com.store.entity.user.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmailAndUserIdNot(String email, Long userId);

    boolean existsByPhoneAndUserIdNot(String phone, Long userId);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByRoles_RoleId(Integer roleId);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.roleName = 'ROLE_ADMIN' AND u.status = com.store.entity.user.UserStatus.ACTIVE")
    long countActiveAdmins();

    @Query(value = "SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE " +
           "(:keyword IS NULL OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:roleName IS NULL OR r.roleName = :roleName) AND " +
           "(:status IS NULL OR u.status = :status)",
           countQuery = "SELECT COUNT(DISTINCT u) FROM User u LEFT JOIN u.roles r WHERE " +
           "(:keyword IS NULL OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:roleName IS NULL OR r.roleName = :roleName) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<User> findAllFiltered(
            @Param("keyword") String keyword,
            @Param("roleName") String roleName,
            @Param("status") UserStatus status,
            Pageable pageable
    );

    @Query(value = "SELECT DISTINCT u FROM User u JOIN u.roles r WHERE " +
           "r.roleName = 'ROLE_CUSTOMER' AND " +
           "(:keyword IS NULL OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR u.status = :status)",
           countQuery = "SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE " +
           "r.roleName = 'ROLE_CUSTOMER' AND " +
           "(:keyword IS NULL OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<User> findAllCustomersFiltered(
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            Pageable pageable
    );

    @Query(value = "SELECT DISTINCT u FROM User u JOIN u.roles r WHERE " +
           "r.roleName != 'ROLE_CUSTOMER' AND " +
           "(:keyword IS NULL OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:roleName IS NULL OR r.roleName = :roleName) AND " +
           "(:status IS NULL OR u.status = :status)",
           countQuery = "SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE " +
           "r.roleName != 'ROLE_CUSTOMER' AND " +
           "(:keyword IS NULL OR " +
           " LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:roleName IS NULL OR r.roleName = :roleName) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<User> findAllStaffFiltered(
            @Param("keyword") String keyword,
            @Param("roleName") String roleName,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}
