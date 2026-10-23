package com.learnflow.backend.store;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class StoreInfoController {

    private final StoreInfoService info;
    private final SizeGuideService sizes;

    public StoreInfoController(StoreInfoService info, SizeGuideService sizes) {
        this.info = info;
        this.sizes = sizes;
    }

    @GetMapping("/store-info")
    public StoreInfoService.Info info() {
        return info.info();
    }

    @GetMapping("/size-charts")
    public List<SizeGuideService.Chart> charts() {
        return sizes.charts();
    }
}
