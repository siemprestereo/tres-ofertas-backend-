package com.example.waiter_rating.service;



import com.example.waiter_rating.dto.response.AppUserResponse;
import com.example.waiter_rating.dto.request.AppUserRequest;

import java.util.List;

public interface AppUserService {

    /** Crea un usuario (CLIENT o WAITER según la lógica que definas) */
    AppUserResponse create(AppUserRequest request);

    /** Devuelve un usuario por id (mapeado a DTO de salida) */
    AppUserResponse getById(Long id);

    /** Lista todos los usuarios (mapeados a DTO de salida) */
    List<AppUserResponse> listAll();
}
