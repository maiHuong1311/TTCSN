package com.learnflow.backend.address;

import com.learnflow.backend.address.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService s;

    public AddressController(AddressService s) {
        this.s = s;
    }

    @GetMapping
    public List<AddressResponse> list(Authentication a) {
        return s.list((Long) a.getPrincipal());
    }

    @PostMapping
    public AddressResponse create(Authentication a, @Valid @RequestBody AddressRequest r) {
        return s.save((Long) a.getPrincipal(), null, r);
    }

    @PutMapping("/{id}")
    public AddressResponse update(
        Authentication a,
        @PathVariable Long id,
        @Valid @RequestBody AddressRequest r
    ) {
        return s.save((Long) a.getPrincipal(), id, r);
    }

    @DeleteMapping("/{id}")
    public void delete(Authentication a, @PathVariable Long id) {
        s.delete((Long) a.getPrincipal(), id);
    }
}
