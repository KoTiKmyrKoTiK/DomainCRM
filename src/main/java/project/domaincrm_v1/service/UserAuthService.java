//package project.domaincrm_v1.service;
//
//import org.springframework.beans.factory.annotation.Autowired;
//
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//import project.domaincrm_v1.entity.User;
//import project.domaincrm_v1.repository.UserRepository;
//
//import java.util.ArrayList;
//
//@Service
//public class UserAuthService implements UserDetailsService {
//    @Autowired
//    UserRepository userRepository;
//@Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        User user = userRepository.findByEmail(username);
//        if (user == null) {
//            throw new UsernameNotFoundException(username);
//        }
//    System.out.println(user);
//
//    return new org.springframework.security.core.userdetails.User(
//            user.getEmail(), user.getPassword(), new ArrayList<>());
//    }
//}