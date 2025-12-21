package com.example.waiter_rating.service;




import com.example.waiter_rating.model.Business;
import java.util.List;

public interface BusinessService {

    Business create(Business business);

    Business getById(Long id);

    List<Business> listAll();
}

