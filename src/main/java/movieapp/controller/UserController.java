package movieapp.controller;

import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.domain.User;
import movieapp.dto.MetaAndHead.ResultPaginationDTO;
import movieapp.dto.User.UseCreateDTO;
import movieapp.dto.User.UserUpdateDTO;
import movieapp.dto.User.ResUserDTO;
import movieapp.repository.WatchHistoryRepository;
import movieapp.service.UserService;
import movieapp.util.annotation.ApiMessage;
import movieapp.util.error.IdInvalidException;
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
    public ResponseEntity<ResultPaginationDTO> getAllUsers(@Filter Specification<User> spec, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.handleGetAllUser(spec, pageable));
    }

    @GetMapping("/{id}")
    @ApiMessage("Get User By ID")
    public ResponseEntity<ResUserDTO> getUserById(@PathVariable("id") long id) throws IdInvalidException {
        User user = userService.handleGetUserById(id);
        if (user == null) throw new IdInvalidException("User với id:  " + id + " không tồn tại !!!!!");
        return ResponseEntity.status(HttpStatus.OK).body(userService.convertToResUserDTO(user));
    }

    @GetMapping("/email/{email}")
    @ApiMessage("Get User By Email")
    public ResponseEntity<ResUserDTO> getUserByEmail(@Valid @PathVariable("email") String email) throws IdInvalidException {
        return ResponseEntity.ok(userService.handleFindUserByEmail(email));
    }

    @PostMapping
    @ApiMessage("Create User")
    public ResponseEntity<ResUserDTO> createUser(@Valid @RequestBody UseCreateDTO userReq) throws IdInvalidException {
        ResUserDTO newUser = userService.handleCreateUser(userReq);
        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @PutMapping("/{id}")
    @ApiMessage("Update User")
    public ResponseEntity<ResUserDTO> updateUser(@Valid @PathVariable("id") long id, @Valid @RequestBody UserUpdateDTO user) throws IdInvalidException {
        return ResponseEntity.ok(userService.handleUpdateUser(id, user));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete User")
    public ResponseEntity<Void> deleteUser(@Valid @PathVariable Long id) throws IdInvalidException {
        userService.handleDeleteUser(id);
        return ResponseEntity.ok(null);
    }
}
