package com.eirsteir.coffeewithme.social.config;

import com.eirsteir.coffeewithme.social.repository.FriendshipRepository;
import com.eirsteir.coffeewithme.social.repository.UserRepository;
import com.eirsteir.coffeewithme.social.service.friendship.FriendshipService;
import com.eirsteir.coffeewithme.social.service.friendship.FriendshipServiceImpl;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class FriendshipConfiguration {

    @Bean
    public FriendshipService friendshipService(DomainEventPublisher domainEventPublisher,
                                               FriendshipRepository friendshipRepository,
                                               UserRepository userRepository) {
        return new FriendshipServiceImpl(domainEventPublisher, friendshipRepository, userRepository);
    }
}