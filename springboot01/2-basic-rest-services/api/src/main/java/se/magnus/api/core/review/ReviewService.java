package se.magnus.api.core.review;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface ReviewService {

  /*
    "curl $HOST:$PORT/review?productId=1".
   */
  @GetMapping(value = "/review",
      produces = "application/json")
  List<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);
}
