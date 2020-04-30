package com.eirsteir.coffeewithme.service;

import com.eirsteir.coffeewithme.domain.user.NewUserForm;
import com.eirsteir.coffeewithme.domain.user.User;
import com.eirsteir.coffeewithme.repository.UserRepository;
import com.eirsteir.coffeewithme.testUtils.BlankStringsArgumentsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@RunWith(SpringRunner.class)
class UserServiceImplTest {

    public static final String USERNAME_ALEX = "alex";
    public static final String EMAIL_ALEX = "alex@email.com";
    public static final String PASSWORD_ALEX = "12345678";

    @TestConfiguration
    static class UserServiceImplTestContextConfiguration {

        @Bean
        public UserService userService() {
            return new UserServiceImpl();
        }
    }

    private static final String USER_NAME_ALEX = "Alex";

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    private User user;
    private NewUserForm newUserForm;
    List<User> allUsers = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        user = User.builder()
                .username(USER_NAME_ALEX)
                .password("12345678")
                .roles(new ArrayList<>())
                .build();

        allUsers.add(user);

        newUserForm = NewUserForm.builder()
                .username(USERNAME_ALEX)
                .email(EMAIL_ALEX)
                .verifyEmail(EMAIL_ALEX)
                .password(PASSWORD_ALEX)
                .verifyPassword(PASSWORD_ALEX)
                .build();

        Mockito.when(userRepository.findByUsername(user.getUsername()))
                .thenReturn(Optional.of(user));

        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenReturn(user);

        Mockito.when(userRepository.findAll()).thenReturn(allUsers);
    }

    @Test
    void testGetUserByUsernameWithValidUsernameShouldFindUser() {
        Optional<User> foundUser = userService.findUserByUsername(USER_NAME_ALEX);

        assertThat(foundUser).isPresent();
        assertThat(USER_NAME_ALEX).isEqualTo(foundUser.get().getUsername());
    }


    @ParameterizedTest
    @ArgumentsSource(BlankStringsArgumentsProvider.class)
    void testGetUserByUsernameWithInvalidUsernameDoesNotFindUser() {
        Optional<User> foundUser = userService.findUserByUsername(null);
        assertThat(foundUser).isEmpty();
    }

    @Test
    void testSaveUserReturnsSavedUser() {
        User savedUser = userService.saveUser(newUserForm);
        assertThat(savedUser.getUsername()).isEqualTo(USER_NAME_ALEX);
    }

    @Test
    void testGetAllUsersReturnsAllUsers() {
        List<User> foundUsers = userService.getAllUsers();
        assertThat(foundUsers).isEqualTo(allUsers);
    }

    @Test
    void testUpdateUserReturnsUpdatedUser() {
        User savedUser = userService.updateProfile(user);
        assertThat(savedUser.getUsername()).isEqualTo(USER_NAME_ALEX);
    }
}
