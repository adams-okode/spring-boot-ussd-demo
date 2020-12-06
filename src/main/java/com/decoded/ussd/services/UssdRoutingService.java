package com.decoded.ussd.services;

import java.io.IOException;
import java.util.Map;

import com.decoded.ussd.data.Menu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UssdRoutingService {

    @Autowired
    private MenuService menuService;

    public String menuLevelRouter(String text) throws IOException {
        Map<String, Menu> menus = menuService.loadMenus();
        /**
         * Check if response has some value
         */
        if (text.length() > 0) {   
            String[] levels = text.split("\\*");
            return menus.get(levels[levels.length - 1]).getAction() + menus.get(levels[levels.length - 1]).getText();
        } else {
            return menus.get("1").getAction() + menus.get("1").getText();
        }

    }
}
