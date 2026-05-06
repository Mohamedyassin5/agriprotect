package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.IUserService;
import tn.esprit.agri.utils.PasswordValidator;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User createUser(User user) {
        if (user.getRole() == null) {
            user.setRole(tn.esprit.agri.entities.enums.Role.FARMER);
        }
        if (user.getStatus() == null) {
            user.setStatus(tn.esprit.agri.entities.enums.Status.ACTIVE);
        }
        if (user.getScore() == null) {
            user.setScore(50.0f);
        }

        // validate password
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }

        String trimmed = user.getPassword().trim();

        if (!PasswordValidator.isStrong(trimmed)) {
            throw new RuntimeException(
                    "Password must be at least 8 characters and include uppercase, lowercase, number, and special character (@$!%*?&._#-)"
            );
        }

        // encode password
        user.setPassword(passwordEncoder.encode(trimmed));

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> updateUser(String id, User userDetails) {
        return userRepository.findById(id).map(user -> {
            if (userDetails.getEmail() != null) {
                user.setEmail(userDetails.getEmail());
            }
            if (userDetails.getFirstName() != null) {
                user.setFirstName(userDetails.getFirstName());
            }
            if (userDetails.getLastName() != null) {
                user.setLastName(userDetails.getLastName());
            }
            if (userDetails.getRole() != null) {
                user.setRole(userDetails.getRole());
            }
            if (userDetails.getProfileImage() != null) {
                user.setProfileImage(userDetails.getProfileImage());
            }
            if (userDetails.getIdCardImage() != null) {
                user.setIdCardImage(userDetails.getIdCardImage());
            }
            if (userDetails.getScore() != null) {
                user.setScore(userDetails.getScore());
            }
            if (userDetails.getPhoneNumber() != null) {
                user.setPhoneNumber(userDetails.getPhoneNumber());
            }
            if (userDetails.getAddress() != null) {
                user.setAddress(userDetails.getAddress());
            }
            if (userDetails.getStatus() != null) {
                user.setStatus(userDetails.getStatus());
            }
            return userRepository.save(user);
        });
    }

    @Override
    public boolean deleteUser(String id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> searchByName(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return userRepository.findAll();
        }

        return userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                        keyword,
                        keyword
                );
    }

    @Override
    public Optional<User> updateStatus(String id, String status) {
        return userRepository.findById(id).map(user -> {
            try {
                user.setStatus(tn.esprit.agri.entities.enums.Status.valueOf(status.toUpperCase()));
                return userRepository.save(user);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status: " + status);
            }
        });
    }

    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1) verify old password (BCrypt)
        if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        // 2) validate new password
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("New password is required");
        }

        String trimmedNew = newPassword.trim();

        // 3) new password must be strong
        if (!PasswordValidator.isStrong(trimmedNew)) {
            throw new RuntimeException(
                    "Password must be at least 8 characters and include uppercase, lowercase, number, and special character (@$!%*?&._#-)"
            );
        }

        // 4) new password must be different from old password
        if (passwordEncoder.matches(trimmedNew, user.getPassword())) {
            throw new RuntimeException("New password must be different from old password");
        }

        // 5) save encoded new password
        user.setPassword(passwordEncoder.encode(trimmedNew));
        userRepository.save(user);
    }
}
