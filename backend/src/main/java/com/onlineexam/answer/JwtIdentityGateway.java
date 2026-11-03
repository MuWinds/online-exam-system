package com.onlineexam.answer;

import com.onlineexam.common.LoginUser;
import com.onlineexam.common.UserContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.onlineexam.answer.Models.Actor;

@Configuration
@Profile("!demo")
public class JwtIdentityGateway {
    @Bean
    IdentityGateway identityGateway() {
        return authorization -> {
            LoginUser user = UserContext.require();
            return new Actor(user.id(), user.role());
        };
    }
}
