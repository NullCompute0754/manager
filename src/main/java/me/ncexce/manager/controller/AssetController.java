package me.ncexce.manager.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.ncexce.manager.service.UAssetService;

@RestController
@RequestMapping("/assets")
public class AssetController {
    private final UAssetService assetService;

    public AssetController(UAssetService assetService) {
        this.assetService = assetService;
    }
}
