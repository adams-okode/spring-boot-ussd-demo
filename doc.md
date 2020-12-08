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

This tutorial is tightly coupled to Africa's talking hence session management depends heavily on the ***sessionId*** Param provided to the callback. We therefore need to store the session Id
to enable us track and destroy the session accordingly. Caching is extremely important at this point as it will allow faster storage and retreival of the active sessions.
The cache layer for this particular tutorial will rely heavily on redis,

1. Create  @[redis labs](https://redislabs.com/try-free/) account and deploy a redis instance
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


## Deployment of the USSD platform