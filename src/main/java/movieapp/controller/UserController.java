package movieapp.controller;

import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.entity.User;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.User.ResUserDTO;
import movieapp.dto.User.UserCreateDTO;
import movieapp.dto.User.UserUpdateDTO;
import movieapp.exception.CommonMessageException;
import movieapp.service.UserService;
import movieapp.util.annotation.ApiMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @GetMapping
    @ApiMessage("Get All Users")
    public ResultPaginationDTO getAllUsers(@Filter Specification<User> spec, Pageable pageable) {
        return userService.handleGetAllUser(spec, pageable);
    }

    @GetMapping("/check/{username}")
    @ApiMessage("Check Exist Username")
    public Boolean checkUsername(@Valid @PathVariable String username) {
        return userService.handleFindByUserName(username);
    }

    @GetMapping("/{id}")
    @ApiMessage("Get User By ID")

    public ResUserDTO getUserById(@PathVariable("id") long id) {
        User user = userService.handleGetUserById(id);
        if (user == null) throw new CommonMessageException("User với id:  " + id + " không tồn tại !!!!!");
        return userService.convertToResUserDTO(user);
    }

    @GetMapping("/email/{email}")
    @ApiMessage("Get User By Email")
    public ResUserDTO getUserByEmail(@Valid @PathVariable("email") String email) {
        return userService.handleFindUserByEmail(email);
    }

    @PostMapping
    @ApiMessage("Create User")
    public ResponseEntity<ResUserDTO> createUser(@Valid @RequestBody UserCreateDTO userReq) {
        ResUserDTO newUser = userService.handleCreateUser(userReq);
        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @PutMapping("/{id}")
    @ApiMessage("Update User")
    public ResUserDTO updateUser(@Valid @PathVariable("id") long id, @Valid @RequestBody UserUpdateDTO user) {
        return userService.handleUpdateUser(id, user);
    }

    @PutMapping("/email-verification/{id}")
    @ApiMessage("Email Verification")
    public ResUserDTO verificationEmail(@Valid @PathVariable("id") long id) {
        return userService.handleUpdateEmail(id);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete User")
    public ResponseEntity<Void> deleteUser(@Valid @PathVariable Long id) {
        userService.handleDeleteUser(id);
        return ResponseEntity.ok(null);
    }
}
