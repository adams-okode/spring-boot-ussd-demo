# Introduction

According to Wikipedia USSD(Unstructured Supplementary Service Data)  is a communications protocol used by GSM cellular telephones to communicate with the mobile network operator's computers. USSD can be used for WAP browsing, prepaid callback service, mobile-money services, location-based content services, menu-based information services, and as part of configuring the phone on the network.
Basically 

- User dials the provided USSD code.
- The request is forwarded to the MNO(Mobile Network Operator).
- The MNO routes the request through a gateway to the machine hosting the web application.
- The application process the requests it receives and sends back a valid response.
- The feedback is processed by the MNO and sent back to the mobile phone.

What you'll need 
- Some knowledge of Java and Spring boot
- Africas talking account 
- Some docker Knowledge (minimal)
- AWS account

## Getting Started

USSD is session driven. What does this mean , well whenever you dial *XXX# a session with a unique Id is created and maintained to allow change of data from the end device to the remote API.[The session typically lasts 180s for most MNOs in kenya ](https://help.africastalking.com/en/articles/1284071-what-is-the-duration-of-a-ussd-session-for-kenyan-telcos) it may be different for other MNOs. As the developer you'll need to keep track of the session and be catious of the time limit to ensure that menus as well as responses are sevrved faster and better to ensure a seamless user experience.
The MNO also lets you control the session this is done basically by attaching CON or END at the sart of every response

    CON: It means an intermediate menu or that session is CONtinuing and hence will require user input
    END: Means the final menu and will trigger session termination i.e session is ENDing.

The above is properly documented on [Africas Talking's dev docs](https://developers.africastalking.com/docs/ussd/handle_sessions)

To get us started head over to [Spring Initializr](https://start.spring.io/) and start your project with the following dependencies
- Spring Web
- Lombok
- Spring Data Redis

The src folder structure is as below
```bash
main
├── java
│   └── com
│       └── decoded
│           └── ussd
│               ├── configs
│               ├── controllers
│               ├── data
│               ├── enums
│               ├── repositories
│               ├── services
│               └── UssdApplication.java
```

To start us off lets create a controller IndexController.java 


```java
/**
* @param text This shows the user input. It is an empty string in the first notification of a session. After that, it concatenates all the user input within the session with a * until the session ends
* @param sessionId A unique value generated when the session starts and sent every time a mobile subscriber response has been received.
* @param serviceCode This is the USSD code assigned to your application
* @param phoneNumber The number of the mobile subscriber interacting with your ussd application.
* @throws IOException
* @return
**/
@PostMapping(path = "ussd")
public String ussdIngress(@RequestParam String sessionId, @RequestParam String serviceCode,
        @RequestParam String phoneNumber, @RequestParam String text) throws IOException {
    // forward to service layer
}
```
## Session Handling

This tutorial is tightly coupled to Africa's talking USSD provision, hence session management depends heavily on the ***sessionId*** Param provided to the callback registerd for the serviceCode. We inturn need to store the session Id to enable us track and destroy the session accordingly from the API end. Caching becomes extremely important at this point as it will allow faster storage and retreival of the active sessions.
The cache layer for this particular tutorial will be implemented on redis. Let's start by configuring everrything needed by Redis to handle the session form the application end.

1. Create  @[redis labs](https://redislabs.com/try-free/) account and deploy a free redis instance
2. Setting Up application configurations
    ```java
    @Configuration
    @ConfigurationProperties(prefix = "decoded")
    @Data
    public class ApplicationConfiguration {

        private CacheConfigurationProperties cache;

        @Getter(value = AccessLevel.PUBLIC)
        private class CacheConfigurationProperties {
            private Integer port;
            private String host;
            private String password;
            private String defaultTtl;
        }
    }
    ```

    application.yaml for the corresponding configurations will be as below 
    ```yaml
    decoded:
        cache:
            port: #redis-port
            host: #redis-host
            password: #provided password
            default-ttl: 180
    ```
3. Redis configurations
    You we'll use Lettuce which comes readliy within spring data redis to configure the connection Factory
    configs/RedisConfiguration.java
   ```java
    @Configuration
    @EnableRedisRepositories
    public class RedisConfiguration extends CachingConfigurerSupport {

        @Value("${decoded.cache.host}")
        private String host;

        @Value("${decoded.cache.port}")
        private int port;

        @Value("${decoded.cache.password}")
        private String password;

        @Value("${decoded.cache.default-ttl}")
        private String defaultTTL;

        @Bean
        public LettuceConnectionFactory redisConnectionFactory() {
            RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
            redisStandaloneConfiguration.setHostName(host);
            redisStandaloneConfiguration.setPort(port);
            redisStandaloneConfiguration.setPassword(password);
            return new LettuceConnectionFactory(redisStandaloneConfiguration);
        }
    }
   ```

4. Session Dto
   
   This is how we'll store the session data in the cache layer.

   ```java
    @Data
    /**
     * This solution adds phantom keys in Redis that apps written in other languages may not understand,
     * hence you may have to account for that, if the redis cluster is utilised by multiple cross-platform applications
     * these keys will get two values for same key(1 orignal and 1 phantom)
     **/
    @RedisHash(value = "sessions", timeToLive = 180)
    public class UssdSession implements Serializable {
        private static final long serialVersionUID = 1L;
        @Id
        private String id;
        private String sessionId;
        private String serviceCode;
        private String phoneNumber;
        private String text;
        private String previousMenuLevel; 
        private String currentMenuLevel;
    }
   ```

5. Session DAO
   ```java
   public interface UssdSessionRepository extends CrudRepository<UssdSession, String> { }
   ```
   
6. Session Management Service
    ```java
    @Service
    public class SessionService {

        @Autowired
        private UssdSessionRepository ussdSessionRepository;

        public UssdSession createUssdSession(UssdSession session) {
            return ussdSessionRepository.save(session);
        }

        public UssdSession get(String id) {
            return ussdSessionRepository.findById(id).orElse(null);
        }

        public UssdSession update(UssdSession session) {
            if (ussdSessionRepository.existsById(session.getId())) {
                return ussdSessionRepository.save(session);
            }
            throw new IllegalArgumentException("A session must have an id to be updated");
        }

        public void delete(String id) {
            // deleting the session
            ussdSessionRepository.deleteById(id);
        }
    }
    ```

## Dynamic Menus

It's important to have the ability to load menus dynamically at runtime, reason being that making changes to dynamically served data is much easier than on statically defines menus may need recompilation and redeployment of the app.

### Menu Structure

1. Menu Levels
   For every request to the API the server responds with a message, sending the user deeper into the implemented sequence hence the term levels. Its important to keep track of where the user is currently at to know which Menu or reponse to serve.
2. Menu Options
   As the name suggests these are just options provided to the user within the Menu to allow the user know how to respond on the USSD interface.

To start us off with the Menus we'll define the structure of the Menu Levels and Options

#### MenuLevels Definition
```java 
@Data
public class Menu {
    @JsonProperty("id")
    private String id; // id of Record as stored in the database or whatever Datastore used

    @JsonProperty("menu_level")
    private String menuLevel; // Menu Level unique identifier

    @JsonProperty("text")
    private String text; // Text to be return for the exact response 

    @JsonProperty("menu_options")
    private List<MenuOption> menuOptions; // Options available for this Menu Level

    @JsonProperty("max_selections")
    private Integer maxSelections; // Max selection identifier to enable diferentiation between value provided and options selections i.e. if request is for Acc No and options are 3 the as simple check for when the value is above 3 would suffice.
}
```

#### Menu Options Definition
```java Menu Options Definition
@Data
public class MenuOption {

    private String type; // helps to determine if the next step should return a response or serve a different menu

    private String response; // response text to be returned

    @JsonProperty("next_menu_level")
    private String nextMenuLevel;// next Menu to be displayed if  the type should return a menu

    private MenuOptionAction action; // action router .i.e. What process should be performed to retrieve the correct set of data for display
}
```

Actions will help us know how to route the options selected by the user

#### Option Actions Definition
```java Menu Option Action
public enum MenuOptionAction {

    PROCESS_ACC_BALANCE("PROCESS_ACC_BALANCE"),
    PROCESS_ACC_PHONE_NUMBER("PROCESS_ACC_PHONE_NUMBER"),
    PROCESS_ACC_NUMBER("PROCESS_ACC_NUMBER");

    private String action;

    MenuOptionAction(String action) {
        this.action = action;
    }

    @JsonValue
    private String getAction() {
        return action;
    }
}

```

#### The Actual Menus
For this tutorial we'll store the menus in a json file within the resource folder.

> **Note** that replaceable vars are denotes as ${XXXXXX}

```json menus.json
{
    "1": {
        "id": 230,
        "menu_level": 1,
        "text": "CON What would you like to check\n1. My account\n2. My phone number",
        "menu_options": [
            {
                "type": "level",
                "response": null,
                "next_menu_level": 2,
                "action": null
            },
            {
                "type": "response",
                "response": "END Your Phone Number is ${phone_number}",
                "next_menu_level": null,
                "action": "PROCESS_ACC_PHONE_NUMBER"
            }
        ],
        "max_selections": 3
    },
    "2": {
        "id": 230,
        "menu_level": 2,
        "text": "CON Choose account information you want to view\n1. Account number\n2. Account balance",
        "menu_options": [
            {
                "type": "response",
                "response": "END Your account number is ${account_number}",
                "next_menu_level": null,
                "action": "PROCESS_ACC_NUMBER"
            },
            {
                "type": "response",
                "response": "END Your Account balance is ${account_balance}",
                "next_menu_level": null,
                "action": "PROCESS_ACC_BALANCE"
            }
        ],
        "max_selections": 3
    }
}
```

#### Fetching the Menu from the store. 
Since the Menus are stored in the resource folder all we need to do is directlu read the data into an InputStream that will be converted to String and Map that to the correct Type.

Create a MenuService within the service folder and add the code below.

```java

@Service
public class MenuService {

    @Autowired
    ResourceLoader resourceLoader;

    /**
     * 
     * @param inputStream
     * @return
     * @throws IOException
     */
    private String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    public Map<String, Menu> loadMenus() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Resource resource = resourceLoader.getResource("classpath:menu.json");
        InputStream input = resource.getInputStream();
        String json = readFromInputStream(input);
        return objectMapper.readValue(json, new TypeReference<Map<String, Menu>>() {
        });
    }

}
```

#### Menu Routing
Let's now put everything together by creating the UssdRoutingService within the services folder 

```java

    /**
     * Inject The necessary services
     * 
     **/
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
         * On first contact the value will be empty
         */
        if (text.length() > 0) {
            return getNextMenuItem(session, menus);
        } else {
            return menus.get(session.getCurrentMenuLevel()).getText();
        }
    }
```


- Fetching the next Menu level to be displayed
```java
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
```


- Get the Menu Level Properties directly from the Menu Service
```java
    /**
     * 
     * @param menuLevel
     * @return
     * @throws IOException
     */
    public String getMenu(String menuLevel) throws IOException {
        Map<String, Menu> menus = menuService.loadMenus();
        return menus.get(menuLevel).getText();
    }
```


- Processing the provided input, determining the response type and updatting the session accordingly

```java
    /**
     * 
     * @param menuOption
     * @return
     * @throws IOException
     */
    public String processMenuOption(UssdSession session, MenuOption menuOption) throws IOException {
        if (menuOption.getType().equals("response")) {
            return processMenuOptionResponses(menuOption);
        } else if (menuOption.getType().equals("level")) {
            updateSessionMenuLevel(session, menuOption.getNextMenuLevel());
            return getMenu(menuOption.getNextMenuLevel());
        } else {
            return "CON ";
        }
    }
```


- Checking the response action and and swithing throun the avaialble Action types
This is where API calls to an external service can be made to trigger certain events or pull data from said API.

```java
    /**
     * 
     * @param menuOption
     * @return
     */
    public String processMenuOptionResponses(MenuOption menuOption) {
        String response = menuOption.getResponse();
        Map<String, String> variablesMap = new HashMap<>();

        if (menuOption.getAction() == MenuOptionAction.PROCESS_ACC_BALANCE) {
            variablesMap.put("account_balance", "10000");
            response = replaceVariable(variablesMap, response);
        } else if (menuOption.getAction() == MenuOptionAction.PROCESS_ACC_NUMBER) {
            variablesMap.put("account_number", "123412512");
            response = replaceVariable(variablesMap, response);
        } else if (menuOption.getAction() == MenuOptionAction.PROCESS_ACC_PHONE_NUMBER) {
            variablesMap.put("phone_number", "254702759950");
            response = replaceVariable(variablesMap, response);
        }

        return response;
    }
```
- Replace variables on the response text with the fetched data usin the **StringSubstitutor** class. for this step you need to first add the org.apache.commons, commons-text and commons-lang3 dependencies

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-text</artifactId>
    <version>1.9</version>
</dependency>

<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.11</version>
</dependency>
```

```java
    /**
     * 
     * @param variablesMap
     * @param response
     * @return
     */
    public String replaceVariable(Map<String, String> variablesMap, String response) {
        StringSubstitutor sub = new StringSubstitutor(variablesMap);
        return sub.replace(response);
    }
```


- Updating Session Data wiht the newly set level as well as the previous session this will allow for complex functionality such as allowing movment back and forth through the available menus

```java
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
```



- This is processed on every request to track session updating or creating the session based on the details provided.

```java
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
```

## Deployment of the USSD platform
For this Particular implementation Elastic Beanstalk was chosen for deployment. find more on this refer to [@aws beanstalk](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/create_deploy_docker.html). For More in formation on how to dockerize Spring boot applications please refer to [this](https://developers.decoded.africa/dockerizing-spring-boot/) article. 

Once we have deployed the application. Next is to link the particular deployment to a service code and allow phone users access our application on USSD.

Steps to linking service Code
- Head over to Africas talking create an account or login if you already have one. 
- Switch to sandbox
- Open the USSD menu and create a channel
  ![](./images/1.png)
- Link Channel callback to the url of the hosted app
    ![](./images/2.png)

    ![](./images/3.png)

- Test App Using the simulator

