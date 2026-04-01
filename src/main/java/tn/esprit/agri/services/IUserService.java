package tn.esprit.agri.services;

import tn.esprit.agri.entities.User;
import java.util.List;
import java.util.Optional;

public interface IUserService {

    User createUser(User user);

    Optional<User> getUserById(String id);

    Optional<User> getUserByEmail(String email);

    List<User> getAllUsers();

    Optional<User> updateUser(String id, User user);

    boolean deleteUser(String id);

    boolean emailExists(String email);
    
    List<User> searchByName(String keyword);

    void changePassword(String email, String oldPassword, String newPassword);
}
