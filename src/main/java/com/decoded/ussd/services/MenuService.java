package com.decoded.ussd.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import com.decoded.ussd.data.Menu;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

    public Map<String, Menu> loadMenus() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File resource = new ClassPathResource("menu.json").getFile();
        String json = new String(Files.readAllBytes(resource.toPath()));
        return objectMapper.readValue(json, new TypeReference<Map<String, Menu>>() {});
    }

}
