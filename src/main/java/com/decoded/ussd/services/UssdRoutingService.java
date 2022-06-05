package com.decoded.ussd.services;

import com.decoded.ussd.data.Menu;
import com.decoded.ussd.data.MenuOption;
import com.decoded.ussd.data.UssdSession;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class UssdRoutingService {

    @Autowired
    private MenuService menuService;

    @Autowired
    private SessionService sessionService;

    /**
     * 
     * @param sessionId
     * @param serviceCode
     * @param phoneNumber
     * @param text
     * @return
     * @throws IOException
     */
    public String menuLevelRouter(String sessionId, String serviceCode, String phoneNumber, String text)
            throws IOException {
        Map<String, Menu> menus = menuService.loadMenus();
        UssdSession session = checkAndSetSession(sessionId, serviceCode, phoneNumber, text);
        /**
         * Check if response has some value
         */
        return text.length() > 0 ? getNextMenuItem(session, menus) : menus.get(session.getCurrentMenuLevel()).getText();
    }

    /**
     * 
     * @param session
     * @param menus
     * @return
     * @throws IOException
     */
    public String getNextMenuItem(UssdSession session, Map<String, Menu> menus) throws IOException {
        String[] levels = session.getText().split("\\*");
        String lastValue = levels[levels.length - 1];
        Menu menuLevel = menus.get(session.getCurrentMenuLevel());

        if (Integer.parseInt(lastValue) <= menuLevel.getMaxSelections()) {
            MenuOption menuOption = menuLevel.getMenuOptions().get(Integer.parseInt(lastValue) - 1);
            return processMenuOption(session, menuOption);
        }

        return "CON ";
    }

    /**
     * 
     * @param menuLevel
     * @return
     * @throws IOException
     */
    public String getMenu(String menuLevel) throws IOException {
        return menuService.loadMenus().get(menuLevel).getText();
    }

    /**
     * 
     * @param menuOption
     * @return
     * @throws IOException
     */
    public String processMenuOption(UssdSession session, MenuOption menuOption) throws IOException {
        switch (menuOption.getType()) {
            case "response":
                return processMenuOptionResponses(menuOption);
            case "level":
                updateSessionMenuLevel(session, menuOption.getNextMenuLevel());
                return getMenu(menuOption.getNextMenuLevel());
            default:
                return "CON ";
        }
    }

    /**
     * 
     * @param menuOption
     * @return
     */
    public String processMenuOptionResponses(MenuOption menuOption) {
        String response = menuOption.getResponse();
        Map<String, String> variablesMap = new HashMap<>();

        switch (menuOption.getAction()) {
            case PROCESS_ACC_BALANCE:
                variablesMap.put("account_balance", "10000");
                response = replaceVariable(variablesMap, response);
                break;
            case PROCESS_ACC_NUMBER:
                variablesMap.put("account_number", "123412512");
                response = replaceVariable(variablesMap, response);
                break;
            case PROCESS_ACC_PHONE_NUMBER:
                variablesMap.put("phone_number", "254702759950");
                response = replaceVariable(variablesMap, response);
                break;
        }

        return response;
    }

    /**
     * 
     * @param variablesMap
     * @param response
     * @return
     */
    public String replaceVariable(Map<String, String> variablesMap, String response) {
        return new StringSubstitutor(variablesMap).replace(response);
    }

    /**
     * 
     * @param session
     * @param menuLevel
     * @return
     */
    public UssdSession updateSessionMenuLevel(UssdSession session, String menuLevel) {
        session.setPreviousMenuLevel(session.getCurrentMenuLevel());
        session.setCurrentMenuLevel(menuLevel);
        return sessionService.update(session);
    }

    /**
     * Check, Set or update the existing session with the provided Session Id
     * 
     * @param sessionId
     * @param serviceCode
     * @param phoneNumber
     * @param text
     * @return
     */
    public UssdSession checkAndSetSession(String sessionId, String serviceCode, String phoneNumber, String text) {
        UssdSession session = sessionService.get(sessionId);

        if (session != null) {
            session.setText(text);
            return sessionService.update(session);
        }

        session = new UssdSession();
        session.setCurrentMenuLevel("1");
        session.setPreviousMenuLevel("1");
        session.setId(sessionId);
        session.setPhoneNumber(phoneNumber);
        session.setServiceCode(serviceCode);
        session.setText(text);

        return sessionService.createUssdSession(session);
    }
}
