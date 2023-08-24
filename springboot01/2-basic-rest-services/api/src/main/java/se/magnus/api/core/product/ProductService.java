package se.magnus.api.core.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface ProductService {

  @GetMapping(value="/product/{productId}",
      produces = "application/json")
  Product getProduct(@PathVariable int productId);
}
