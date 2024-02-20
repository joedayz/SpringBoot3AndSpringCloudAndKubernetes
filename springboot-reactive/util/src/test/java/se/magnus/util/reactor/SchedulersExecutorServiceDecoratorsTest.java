package se.magnus.util.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.log4j.Log4j2;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactoryBean;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

@Log4j2
public class SchedulersExecutorServiceDecoratorsTest {

  private final AtomicInteger methodInvocationCounts = new AtomicInteger();

  private String rsb = "rsb";

  @BeforeEach
  public  void before(){
    Schedulers.resetFactory();
    Schedulers.addExecutorServiceDecorator(this.rsb, (scheduler, scheduledExecutorService) ->
     this.decorate(scheduledExecutorService));
  }

  @Test
  public void changeDefaultDecorator(){
    Flux<Integer> integerFlux = Flux.just(1).delayElements(Duration.ofMillis(1));

    StepVerifier.create(integerFlux)
        .thenAwait(Duration.ofMillis(10))
        .expectNextCount(1).verifyComplete();
    assertEquals(1, this.methodInvocationCounts.get());
  }

  @AfterEach
  public  void after(){
    Schedulers.resetFactory();
    Schedulers.removeExecutorServiceDecorator(this.rsb);
  }

  private ScheduledExecutorService decorate(ScheduledExecutorService executorService){
    try{
      var pfb = new ProxyFactoryBean();
      pfb.setProxyInterfaces(new Class[] {
        ScheduledExecutorService.class
      });
      pfb.addAdvice( (MethodInterceptor) methodInvocation -> {
        var methodName = methodInvocation.getMethod().getName().toLowerCase();

        this.methodInvocationCounts.incrementAndGet();

        log.info("methodName: (" + methodName + ") incrementing...");

        return methodInvocation.proceed();
      });

      pfb.setSingleton(true);
      pfb.setTarget(executorService);
      return (ScheduledExecutorService) pfb.getObject();
    } catch (Exception e) {
      log.error(e);
    }
    return null;
  }

}
