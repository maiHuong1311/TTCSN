package com.learnflow.backend.address;

import com.learnflow.backend.address.domain.Address;
import com.learnflow.backend.address.dto.*;
import com.learnflow.backend.auth.AuthService;
import com.learnflow.backend.common.web.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddressService {

    private final AddressRepository repository;
    private final AuthService users;

    public AddressService(AddressRepository r, AuthService u) {
        repository = r;
        users = u;
    }

    public List<AddressResponse> list(Long u) {
        return repository.findByUserId(u).stream().map(this::dto).toList();
    }

    public Address require(Long u, Long id) {
        return repository
            .findById(id)
            .filter(a -> a.userId.equals(u))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Address not found"));
    }

    public AddressResponse save(Long u, Long id, AddressRequest r) {
        users.lockUser(u);
        Address a = id == null ? new Address() : require(u, id);
        a.userId = u;
        a.receiverName = r.receiverName();
        a.phoneNumber = r.phoneNumber();
        a.province = r.province();
        a.detailAddress = r.detailAddress();
        a.isDefault = r.isDefault() || repository.findByUserId(u).isEmpty();
        if (a.isDefault) repository.findByUserId(u).forEach(x -> x.isDefault = false);
        return dto(repository.save(a));
    }

    public void delete(Long u, Long id) {
        users.lockUser(u);
        Address a = require(u, id);
        repository.delete(a);
        repository.flush();
        if (a.isDefault) repository
            .findByUserId(u)
            .stream()
            .findFirst()
            .ifPresent(x -> x.isDefault = true);
    }

    private AddressResponse dto(Address a) {
        return new AddressResponse(
            a.id,
            a.receiverName,
            a.phoneNumber,
            a.province,
            a.detailAddress,
            a.isDefault
        );
    }
}
