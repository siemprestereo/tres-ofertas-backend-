package com.example.waiter_rating.repository;




import com.example.waiter_rating.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepo extends JpaRepository<Business, Long> {
}
