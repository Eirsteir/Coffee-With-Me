package com.eirsteir.coffeewithme.social.dto;

import com.eirsteir.coffeewithme.commons.domain.UserDetails;
import com.eirsteir.coffeewithme.social.domain.university.Campus;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalTime;
import java.util.Set;

@Getter
@Setter
@ToString
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class CoffeeBreakDto {

    private LocalTime scheduledTo;
    private UserDetails requester;
    private Set<UserDetails> addressees;
    private Campus campus;

}
