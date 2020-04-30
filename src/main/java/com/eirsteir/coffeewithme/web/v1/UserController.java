package com.eirsteir.coffeewithme.web.v1;


import com.eirsteir.coffeewithme.dto.UserDto;
import com.eirsteir.coffeewithme.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("api/v1/users")
@Api(tags = {"Swagger Resource"})
@SwaggerDefinition(tags = {
        @Tag(name = "Swagger Resource", description = "User management operations for this application")
})
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public Map<String, Object> signUp(@AuthenticationPrincipal OAuth2User principal) {

        return Collections.singletonMap("name", principal.getAttribute("name")); // email, sub - id
    }

    @PutMapping
    UserDto updateUser(@RequestBody @Valid UserDto userDto) {
        return userService.updateProfile(userDto);
    }
}
