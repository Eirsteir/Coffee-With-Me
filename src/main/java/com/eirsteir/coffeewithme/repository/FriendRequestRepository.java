package com.eirsteir.coffeewithme.repository;

import com.eirsteir.coffeewithme.domain.request.FriendRequest;
import com.eirsteir.coffeewithme.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    boolean existsByFromAndTo(User from, User to);

}