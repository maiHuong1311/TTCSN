package com.learnflow.backend.address.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    public Long id;

    @Column(name = "user_id")
    public Long userId;

    @Column(name = "receiver_name", length = 100)
    public String receiverName;

    @Column(name = "phone_number", length = 15)
    public String phoneNumber;

    @Column(name = "province", length = 100)
    public String province;

    @Column(name = "detail_address", length = 255)
    public String detailAddress;

    @Column(name = "is_default")
    public Boolean isDefault;

    public Address() {}
}
