package com.agri.supplytracker.identity.application;
import com.agri.supplytracker.model.User;
import com.agri.supplytracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.NoSuchElementException;
@Service
public class UserDirectoryService {
    private final UserRepository users;
    public UserDirectoryService(UserRepository users){this.users=users;}
    public User requireUser(String username){return users.findByUsername(username).orElseThrow(()->new NoSuchElementException("User not found"));}
}
