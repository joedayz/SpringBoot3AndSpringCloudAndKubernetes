package se.magnus.api.composite.product;

import java.util.List;

public class ProductAggregate {
  private int productId;
  private String name;
  private int weight;

  private List<RecommendationSummary> recommendations;
  private List<ReviewSummary> reviews;

  private ServiceAddresses serviceAddresses;

  public ProductAggregate(
      int productId,
      String name,
      int weight,
      List<RecommendationSummary> recommendations,
      List<ReviewSummary> reviews,
      ServiceAddresses serviceAddresses
      ) {

    this.productId = productId;
    this.name = name;
    this.weight = weight;
    this.recommendations = recommendations;
    this.reviews = reviews;
    this.serviceAddresses = serviceAddresses;

  }

  public int getProductId() {
    return productId;
  }

  public String getName() {
    return name;
  }

  public int getWeight() {
    return weight;
  }

  public List<RecommendationSummary> getRecommendations() {
    return recommendations;
  }

  public List<ReviewSummary> getReviews() {
    return reviews;
  }

  public ServiceAddresses getServiceAddresses() {
    return serviceAddresses;
  }
}
