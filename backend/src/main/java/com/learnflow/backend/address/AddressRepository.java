package com.learnflow.backend.address;

import com.learnflow.backend.address.domain.Address;

public interface AddressRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Address, Long>
{
    java.util.List<Address> findByUserId(Long id);
}
