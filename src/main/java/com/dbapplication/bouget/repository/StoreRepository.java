package com.dbapplication.bouget.repository;

import com.dbapplication.bouget.entity.Bouquet;
import com.dbapplication.bouget.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Store findByBouquet(Bouquet bouquet);
}
