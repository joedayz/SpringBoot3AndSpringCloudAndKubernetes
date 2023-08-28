package se.magnus.microservices.composite.product.services;

import static org.springframework.http.HttpMethod.GET;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.util.http.HttpErrorInfo;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService,
    ReviewService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

  private final ObjectMapper mapper;

  private final RestTemplate restTemplate;
  private final String productServiceUrl;
  private final String recommendationServiceUrl;
  private final String reviewServiceUrl;

  @Autowired
  public ProductCompositeIntegration(RestTemplate restTemplate,
      ObjectMapper mapper,
      @Value("${app.product-service.host}") String productServiceHost,
      @Value("${app.product-service.port}") int productServicePort,
      @Value("${app.recommendation-service.host}") String recommendationServiceHost,
      @Value("${app.recommendation-service.port}") int recommendationServicePort,
      @Value("${app.review-service.host}") String reviewServiceHost,
      @Value("${app.review-service.port}") int reviewServicePort) {
    this.restTemplate = restTemplate;
    this.mapper = mapper;

    this.productServiceUrl =
        "http://" + productServiceHost + ":" + productServicePort + "/product/";
    recommendationServiceUrl =
        "http://" + recommendationServiceHost + ":" + recommendationServicePort
            + "/recommendation?productId=";
    reviewServiceUrl =
        "http://" + reviewServiceHost + ":" + reviewServicePort + "/review?productId=";
  }

  @Override
  public Product getProduct(int productId) {
    try {
      String url = this.productServiceUrl + productId;
      Product product = restTemplate.getForObject(url, Product.class);
      return product;
    } catch (HttpClientErrorException ex) {

      switch (HttpStatus.resolve(ex.getStatusCode().value())) {
        case NOT_FOUND -> throw new NotFoundException(getErrorMessage(ex));
        case UNPROCESSABLE_ENTITY -> throw new InvalidInputException(getErrorMessage(ex));
        default -> throw ex;
      }
    }

  }

  private String getErrorMessage(HttpClientErrorException ex) {
    try {
      return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
    } catch (JsonProcessingException e) {
      return ex.getMessage();
    }
  }

  @Override
  public List<Recommendation> getRecommendations(int productId) {
    try {
      String url = recommendationServiceUrl + productId;

      List<Recommendation> recommendations = restTemplate.exchange(url, GET, null,
          new ParameterizedTypeReference<List<Recommendation>>() {
          }).getBody();

      return recommendations;
    } catch (Exception ex) {
      return new ArrayList<>();
    }
  }

  @Override
  public List<Review> getReviews(int productId) {
    try {
      String url = reviewServiceUrl + productId;

      LOG.debug("Will call getReviews API on URL: {}", url);
      List<Review> reviews = restTemplate
          .exchange(url, GET, null, new ParameterizedTypeReference<List<Review>>() {
          })
          .getBody();

      LOG.debug("Found {} reviews for a product with id: {}", reviews.size(), productId);
      return reviews;

    } catch (Exception ex) {
      LOG.warn("Got an exception while requesting reviews, return zero reviews: {}",
          ex.getMessage());
      return new ArrayList<>();
    }
  }
}
