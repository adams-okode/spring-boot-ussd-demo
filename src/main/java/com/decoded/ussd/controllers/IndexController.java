package com.decoded.ussd.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.decoded.ussd.data.Menu;
import com.decoded.ussd.services.MenuService;
import com.decoded.ussd.services.UssdRoutingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class IndexController {

    @Autowired
    private MenuService menuService;

    @Autowired
    private UssdRoutingService ussdRoutingService;

    /**
     * 
     * @return
     * @throws IOException
     */
    @GetMapping(path = "menus")
    public Map<String, Menu> menusLoad() throws IOException {
        return menuService.loadMenus();
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    @GetMapping(path = "")
    public String index() throws IOException {
        return "Your have reached us";
    }

    /**
     * 
     * @param sessionId
     * @param serviceCode
     * @param phoneNumber
     * @param text
     * @return
     * @throws IOException
     */
    @PostMapping(path = "ussd")
    public String ussdIngress(@RequestParam String sessionId, @RequestParam String serviceCode,
            @RequestParam String phoneNumber, @RequestParam String text) throws IOException {
        try {
            return ussdRoutingService.menuLevelRouter(sessionId, serviceCode, phoneNumber, text);
        } catch (IOException e) {
            return "END " + e.getMessage();
        }
    }
}
