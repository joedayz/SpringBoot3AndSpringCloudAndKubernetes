package se.magnus.util.http;

import java.time.ZonedDateTime;
import org.springframework.http.HttpStatus;

public class HttpErrorInfo {
  private final ZonedDateTime timestamp;
  private final String path;
  private final HttpStatus httpStatus;
  private final String message;

  public HttpErrorInfo() {
    timestamp = null;
    this.httpStatus = null;
    this.path = null;
    this.message = null;
  }

  public HttpErrorInfo(HttpStatus httpStatus,String path,
      String message) {
    this.timestamp = ZonedDateTime.now();
    this.path = path;
    this.httpStatus = httpStatus;
    this.message = message;
  }

  public ZonedDateTime getTimestamp() {
    return timestamp;
  }

  public String getPath() {
    return path;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public String getMessage() {
    return message;
  }
}
