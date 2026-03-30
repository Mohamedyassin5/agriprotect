package tn.esprit.agri.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.agri.entities.User;
import tn.esprit.agri.repositories.UserRepository;
import tn.esprit.agri.services.IUserService;

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
        // encode password before saving
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getFaceEnabled() == null) {
            user.setFaceEnabled(false);
        }
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
            if (userDetails.getPassword() != null) {
                user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
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
            if (userDetails.getExpertFundId() != null) {
                user.setExpertFundId(userDetails.getExpertFundId());
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
}
