package com.store.repository;

import com.store.entity.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);

    boolean existsByRoleNameAndRoleIdNot(String roleName, Integer roleId);

    java.util.List<Role> findAllByOrderByRoleIdAsc();
}
