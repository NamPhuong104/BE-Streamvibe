package movieapp.controller.exceptionHandler;

import movieapp.controller.AuthController;
import movieapp.dto.RestResponse;
import movieapp.exception.PartnerAuthenticationException;
import movieapp.exception.ProviderPasswordNotFound;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AuthController.class})
public class AuthExceptionHandler {
    @ExceptionHandler(value = {ProviderPasswordNotFound.class})
    public ResponseEntity<RestResponse<Object>> handleProviderPasswordNotFoundException(ProviderPasswordNotFound e) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.UNAUTHORIZED.value());
        res.setError("Unauthorized");
        res.setMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
    }

    @ExceptionHandler(value = {UsernameNotFoundException.class, BadCredentialsException.class})
    public ResponseEntity<RestResponse<Object>> handleUsernameNotFoundExceptionException(Exception e) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.BAD_REQUEST.value());
        res.setError(e.getMessage());
        res.setMessage("Tài khoản hoặc mật khẩu không chính xác");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(value = {PartnerAuthenticationException.class})
    public ResponseEntity<RestResponse<Object>> handlePartnerAuthenticationException(PartnerAuthenticationException e) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.BAD_REQUEST.value());
        res.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        res.setMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }
}
