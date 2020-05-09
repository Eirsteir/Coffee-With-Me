package com.eirsteir.coffeewithme.web.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateProfileRequest {

    // TODO: 09.05.2020 Verify no spaces in username
    @NotBlank(message = "Username is required")
    private String username;

}
