package com.shoppingmall.controller;

import com.shoppingmall.config.UploadProperties;
import com.shoppingmall.exception.NotFoundException;
import com.shoppingmall.service.ProductService;
import com.shoppingmall.service.ShopService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MediaController {

    private final UploadProperties uploads;
    private final ShopService shops;
    private final ProductService products;

    public MediaController(UploadProperties uploads, ShopService shops, ProductService products) {
        this.uploads = uploads;
        this.shops = shops;
        this.products = products;
    }

    @GetMapping("/media/shops/{shopId}/{file}")
    public ResponseEntity<byte[]> shop(@PathVariable long shopId, @PathVariable String file) {
        String stored = shops.logoPath(shopId);
        return bytes(stored, "shops/" + shopId + "/" + file);
    }

    @GetMapping("/media/products/{productId}/{file}")
    public ResponseEntity<byte[]> product(@PathVariable long productId, @PathVariable String file) {
        String stored = products.imagePath(productId);
        return bytes(stored, "products/" + productId + "/" + file);
    }

    private ResponseEntity<byte[]> bytes(String stored, String relative) {
        if (stored == null || !stored.equals(relative) || relative.contains("..") || relative.contains("\\")) {
            throw new NotFoundException("Image was not found.");
        }
        Path root = Path.of(uploads.getDir()).toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw new NotFoundException("Image was not found.");
        }
        try {
            byte[] body = Files.readAllBytes(target);
            return ResponseEntity.ok().contentType(type(relative)).body(body);
        } catch (Exception exception) {
            throw new NotFoundException("Image was not found.");
        }
    }

    private static MediaType type(String relative) {
        if (relative.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (relative.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
