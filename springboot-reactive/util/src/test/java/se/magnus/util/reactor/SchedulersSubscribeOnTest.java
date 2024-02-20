package se.magnus.util.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

@Log4j2
public class SchedulersSubscribeOnTest {
  @Test
  public void subscribeOn() {
    var rsbThreadName = SchedulersSubscribeOnTest.class.getName();
    var map = new ConcurrentHashMap< String , AtomicInteger>();
    var executor = Executors.newFixedThreadPool( 5 , runnable -> {
        Runnable wrapper = () -> {
        var key = Thread .currentThread().getName();
        var result = map.computeIfAbsent(key, s -> new AtomicInteger());
        result.incrementAndGet();
        runnable.run();
        };
        return new Thread (wrapper, rsbThreadName);
    });


    Scheduler scheduler = Schedulers.fromExecutor(executor);
    Mono< Integer > integerFlux = Mono.just( 1 ).subscribeOn(scheduler)
        .doFinally(signal -> map.forEach((k, v) -> log.info(k + '=' + v)));

    StepVerifier.create(integerFlux).expectNextCount( 1 ).verifyComplete();
    var atomicInteger = map.get(rsbThreadName);
    assertEquals(atomicInteger.get(), 1 );


  }
}
