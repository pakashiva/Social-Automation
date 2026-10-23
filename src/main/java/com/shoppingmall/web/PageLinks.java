package com.shoppingmall.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component("pageLinks")
public class PageLinks {

    public String url(int page) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        return ServletUriComponentsBuilder.fromRequest(request).replaceQueryParam("page", page).toUriString();
    }
}
