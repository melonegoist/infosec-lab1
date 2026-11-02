package org.itmo.lab1.infrastructure.config;

import org.itmo.lab1.application.auth.LoginService;
import org.itmo.lab1.application.port.in.BrowsePostsUseCase;
import org.itmo.lab1.application.port.in.CreatePostUseCase;
import org.itmo.lab1.application.port.in.LoginUseCase;
import org.itmo.lab1.application.port.in.ProvisionUserUseCase;
import org.itmo.lab1.application.port.out.PasswordHasher;
import org.itmo.lab1.application.port.out.PostRepository;
import org.itmo.lab1.application.port.out.TokenIssuer;
import org.itmo.lab1.application.port.out.UserRepository;
import org.itmo.lab1.application.post.BrowsePostsService;
import org.itmo.lab1.application.post.CreatePostService;
import org.itmo.lab1.application.user.UserProvisioningService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApplicationServicesConfig {

    @Bean
    ProvisionUserUseCase provisionUserUseCase(UserRepository users, PasswordHasher hasher) {
        return new UserProvisioningService(users, hasher);
    }

    @Bean
    LoginUseCase loginUseCase(UserRepository users, PasswordHasher hasher, TokenIssuer tokens) {
        return new LoginService(users, hasher, tokens);
    }

    @Bean
    CreatePostUseCase createPostUseCase(PostRepository posts) {
        return new CreatePostService(posts);
    }

    @Bean
    BrowsePostsUseCase browsePostsUseCase(PostRepository posts) {
        return new BrowsePostsService(posts);
    }
}
