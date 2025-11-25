package com.dbapplication.bouget.repository;

import com.dbapplication.bouget.entity.ApplyImage;
import com.dbapplication.bouget.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplyImageRepository extends JpaRepository<ApplyImage, Long> {
        Page<ApplyImage> findByUser(User user, Pageable pageable);
}
