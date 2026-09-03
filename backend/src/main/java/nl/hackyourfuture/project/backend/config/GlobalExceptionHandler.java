package nl.hackyourfuture.project.backend.config;

import nl.hackyourfuture.project.backend.auth.exceptions.EmailAlreadyExistsException;
import nl.hackyourfuture.project.backend.auth.exceptions.InvalidCredentialsException;
import nl.hackyourfuture.project.backend.event.comment.exceptions.AdminReplyAlreadyExistsException;
import nl.hackyourfuture.project.backend.event.comment.exceptions.AdminReplyNotFoundException;
import nl.hackyourfuture.project.backend.event.comment.exceptions.CommentNotFoundException;
import nl.hackyourfuture.project.backend.event.exceptions.EventNotFoundException;
import nl.hackyourfuture.project.backend.event.image.exceptions.ImageUploadException;
import nl.hackyourfuture.project.backend.feedback.FeedbackNotFoundException;
import nl.hackyourfuture.project.backend.notification.NotificationNotFoundException;
import nl.hackyourfuture.project.backend.location.ExternalServiceException;
import nl.hackyourfuture.project.backend.user.exceptions.BadRequestException;
import nl.hackyourfuture.project.backend.user.exceptions.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailExist(EmailAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Email already registered");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredential(InvalidCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Invalid credentials");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("User not found");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad request");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Malformed request body");
        problem.setDetail("The request body is missing or not valid JSON");
        return problem;
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        problem.setTitle("Unsupported media type");
        problem.setDetail(
                "The request content type is not supported for this endpoint"
        );
        return problem;
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingRequestPart(
            MissingServletRequestPartException ex
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Missing multipart part");
        problem.setDetail(
                "Required multipart part '" + ex.getRequestPartName()
                        + "' is missing"
        );
        return problem;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);
        problem.setTitle("Method not allowed");
        problem.setDetail("The HTTP method '" + ex.getMethod() + "' is not supported for this endpoint");
        return problem;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNotFound(NoResourceFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not found");
        problem.setDetail("The requested resource does not exist");
        return problem;
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ProblemDetail handleEventNotFound(EventNotFoundException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problem.setTitle("Event not found");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ProblemDetail handleCommentNotFound(
            CommentNotFoundException ex
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problem.setTitle("Comment not found");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(AdminReplyAlreadyExistsException.class)
    public ProblemDetail handleAdminReplyAlreadyExists(
            AdminReplyAlreadyExistsException ex
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.CONFLICT);

        problem.setTitle("Admin reply already exists");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(AdminReplyNotFoundException.class)
    public ProblemDetail handleAdminReplyNotFound(
            AdminReplyNotFoundException ex
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problem.setTitle("Admin reply not found");
        problem.setDetail(ex.getMessage());

        return problem;
    }


    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodValidation(
            HandlerMethodValidationException ex
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setTitle("Validation failed");
        problem.setDetail(
                "One or more request parameters are outside the allowed range"
        );

        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problem.setTitle("Invalid request parameter");
        problem.setDetail(
                "The value provided for '" + ex.getName()
                        + "' has an invalid format"
        );

        return problem;
    }

    @ExceptionHandler(ImageUploadException.class)
    public ProblemDetail handleImageUpload(ImageUploadException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);

        problem.setTitle("Image upload failed");
        problem.setDetail(
                "The image service could not upload the file. Please try again."
        );

        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(
            MaxUploadSizeExceededException ex
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.CONTENT_TOO_LARGE);

        problem.setTitle("Image is too large");
        problem.setDetail("The image must not exceed 5 MB");

        return problem;
    }

    @ExceptionHandler(FeedbackNotFoundException.class)
    public ProblemDetail handleFeedbackNotFound(
        FeedbackNotFoundException ex
    ) {
        ProblemDetail problem =
            ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problem.setTitle("Feedback not found");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ProblemDetail handleNotificationNotFound(
            NotificationNotFoundException ex
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problem.setTitle("Notification not found");
        problem.setDetail(ex.getMessage());

        return problem;
    }


    @ExceptionHandler(ExternalServiceException.class)
    public ProblemDetail handleExternalServiceError(ExternalServiceException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("External service unavailable");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Missing required request parameter");
        problem.setDetail("Required parameter '" + ex.getParameterName() + "' is not present.");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.merge(
                        error.getField(),
                        Objects.requireNonNullElse(error.getDefaultMessage(), ""), (a, b) -> a + "; " + b)
                );

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail("One or more fields are invalid");
        problem.setProperty("errors", errors);
        return problem;
    }
}
