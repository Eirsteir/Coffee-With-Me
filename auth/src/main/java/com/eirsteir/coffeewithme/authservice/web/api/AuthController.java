package com.eirsteir.coffeewithme.authservice.web.api;


import com.eirsteir.coffeewithme.authservice.domain.Account;
import com.eirsteir.coffeewithme.authservice.service.UserService;
import com.eirsteir.coffeewithme.authservice.web.request.UserRegistrationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    ResponseEntity<Object> register(@RequestBody @Valid UserRegistrationRequest userRegistrationRequest) {

        Optional<Account> account = userService.registerUser(userRegistrationRequest);

        if (account.isPresent()) {
            userService.dispatch(account.get());
            return ResponseEntity.created(URI.create("/api/me")).build();
        }

        return ResponseEntity.badRequest().build();
    }

}
