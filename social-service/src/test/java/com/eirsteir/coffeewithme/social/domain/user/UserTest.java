package com.eirsteir.coffeewithme.social.domain.user;

import com.eirsteir.coffeewithme.social.domain.friendship.Friendship;
import com.eirsteir.coffeewithme.social.domain.friendship.FriendshipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private User user;
    private User friend;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .build();

        friend = User.builder()
                .id(2L)
                .build();

        user.addFriend(friend, FriendshipStatus.REQUESTED);
    }

    @Test
    void testAddFriend() {
        User addressee = user.getFriendships().get(0).getAddressee();
        FriendshipStatus status = user.getFriendships().get(0).getStatus();

        assertThat(friend).isEqualTo(addressee);
        assertThat(status).isEqualTo(FriendshipStatus.REQUESTED);
    }

    @Test
    void testRemoveFriendship() {
        Friendship friendship = user.getFriendships().get(0);
        user.removeFriendship(friendship);

        assertThat(user.getFriends()).isEmpty();
    }
}