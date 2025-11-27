package com.dbapplication.bouget.repository;

import com.dbapplication.bouget.entity.Inquiry;
import com.dbapplication.bouget.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findByUser(User user, Pageable pageable);
}
