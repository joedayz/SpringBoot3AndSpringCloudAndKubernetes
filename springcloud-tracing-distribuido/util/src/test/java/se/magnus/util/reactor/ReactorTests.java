package se.magnus.util.reactor;

import static org.assertj.core.api.Assertions.assertThat;


import io.netty.handler.codec.ReplayingDecoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.test.StepVerifier;
import reactor.test.StepVerifier.Step;

class ReactorTests {

  private final ExecutorService executorService = Executors.newFixedThreadPool(1);

  @Test
  public void simple(){

    Publisher<Integer> rangeOfIntegers = Flux.range(0, 10);

    StepVerifier.create(rangeOfIntegers)
        .expectNextCount(10).verifyComplete();

    Flux<String> letters = Flux.just("A", "B", "C");

    StepVerifier.create(letters)
        .expectNext("A", "B", "C").verifyComplete();

    long now = System.currentTimeMillis();
    Mono<Date> greetingMono = Mono.just(new Date(now));

    StepVerifier.create(greetingMono)
        .expectNext(new Date(now)).verifyComplete();

    Mono<Object> empty = Mono.empty();
    StepVerifier.create(empty).verifyComplete();

    Flux<Integer> fromArray = Flux.fromArray(new Integer[]{1, 2, 3});

    StepVerifier.create(fromArray)
        .expectNext(1, 2, 3).verifyComplete();

    Flux<Integer> fromIterable = Flux.fromIterable(List.of(1, 2, 3));

    StepVerifier.create(fromIterable)
        .expectNext(1, 2, 3).verifyComplete();

    AtomicInteger integer = new AtomicInteger();
    Supplier<Integer> supplier = integer::incrementAndGet;
    Flux<Integer> integerFlux = Flux.fromStream(Stream.generate(supplier));

    StepVerifier.create(integerFlux.take(3))
        .expectNext(1).expectNext(2).expectNext(3).verifyComplete();

  }

  // Flow.Publisher de Java 9
  // reactor.adapter.JdkFlowAdapter para crear Flux<T> y Mono<T> a partir de Flow.Publisher<T>

  @Test
  public void convert(){
    Flux<Integer> original = Flux.range(0, 10);

    Flow.Publisher<Integer> rangeOfIntegersAsJdk9Flow = FlowAdapters.toFlowPublisher(original);

    Publisher<Integer> rangeOfIntegersAsReactiveStream = FlowAdapters.toPublisher(rangeOfIntegersAsJdk9Flow);

    StepVerifier.create(original)
        .expectNextCount(10).verifyComplete();

    StepVerifier.create(rangeOfIntegersAsReactiveStream).expectNextCount(10).verifyComplete();

    Flux<Integer> rangeOfIntegersAsReactorFluxAgain =
        JdkFlowAdapter.flowPublisherToFlux(rangeOfIntegersAsJdk9Flow);

    StepVerifier.create(rangeOfIntegersAsReactorFluxAgain).expectNextCount(10).verifyComplete();

  }

  @Test
  public void async(){

    Flux<Integer> integers = Flux.create(emitter -> this.launch(emitter, 5));

    StepVerifier.create(integers.doFinally(signalType -> this.executorService.shutdown()))
        .expectNextCount(5).verifyComplete();
  }

  private void launch(FluxSink<Integer> integerFluxSink, int count){
    this.executorService.submit(()->{
      var integer = new AtomicInteger();
      Assertions.assertNotNull(integerFluxSink);
      while(integer.get()<count){
        double random = Math.random();

        integerFluxSink.next(integer.incrementAndGet());
        this.sleep((long) (random*1000));
      }
      integerFluxSink.complete();
    });

  }

  @Test
  public void emitterProcessor(){
    EmitterProcessor<String> processor = EmitterProcessor.create();
    produce(processor.sink());
    consume(processor);
  }

  private void produce(FluxSink<String> sink){
    sink.next("1");
    sink.next("2");
    sink.next("3");
    sink.complete();
  }

  private void consume(Flux<String> publisher){
   StepVerifier.create(publisher)
       .expectNext("1")
        .expectNext("2")
        .expectNext("3")
       .verifyComplete();
  }


  @Test
  public void replayProcessor(){
    int historySize = 2;
    boolean unbounded = false;
    ReplayProcessor<String> processor = ReplayProcessor.create(historySize, unbounded);
    produce(processor.sink());
    consumeReplay(processor);
  }

  private void consumeReplay(Flux<String> publisher){
    for (int i = 0; i < 5; i++) {
      StepVerifier.create(publisher)
          .expectNext("2")
          .expectNext("3")
          .verifyComplete();
    }
  }

  //Transform
  @Test
  public void transform(){
    var finished = new AtomicBoolean();
    var letters = Flux.just("A", "B", "C")
        .transform(stringFlux -> stringFlux.doFinally(signalType -> finished.set(true)));

    StepVerifier.create(letters).expectNextCount(3).verifyComplete();
    Assertions.assertTrue(finished.get(), "the finished Boolean must be true");

  }


  @Test
  public void thenMany(){
    var letters = new AtomicInteger();
    var numbers = new AtomicInteger();

    Flux<String> letterPublisher = Flux.just("A", "B", "C")
        .doOnNext(s -> letters.incrementAndGet());

    Flux<Integer> numberPublisher = Flux.just(1, 2, 3).doOnNext(i -> numbers.incrementAndGet());

    Flux<Integer> thisBeforeThat = letterPublisher.thenMany(numberPublisher);

    StepVerifier.create(thisBeforeThat).expectNext(1, 2, 3).verifyComplete();

    Assertions.assertEquals(3, letters.get(), "the letters count must be 3");
    Assertions.assertEquals(3, numbers.get(), "the numbers count must be 3");

  }

  @Test
  public void maps(){
    var data = Flux.just("a", "b", "c")
        .map(String::toUpperCase);
    StepVerifier.create(data).expectNext("A", "B", "C").verifyComplete();
  }


  @Test
  public void flatMap(){
    Flux<Integer> data = Flux.just(new Pair(1, 300), new Pair(2, 200), new Pair(3, 100))
        .flatMap(id -> this.delayReplyfor(id.id, id.delay));

    StepVerifier.create(data).expectNext(3, 2, 1).verifyComplete();
  }

  @Test
  public void concatMap(){
    Flux<Integer> data = Flux.just(new Pair(1, 300), new Pair(2, 200), new Pair(3, 100))
        .concatMap(id -> this.delayReplyfor(id.id, id.delay));

    StepVerifier.create(data).expectNext(1, 2, 3).verifyComplete();
  }


  @Test
  public void switchMapWithLookaheads(){
    Flux<String> source = Flux.just("re", "rea", "reac", "react", "reactive")
        .delayElements(Duration.ofMillis(100))
        .switchMap(this::lookup);

    StepVerifier.create(source).expectNext("reactive -> reactive").verifyComplete();
  }


  @Test
  public void take(){
    var count = 10;
    Flux<Integer> take = range().take(count);

    StepVerifier.create(take).expectNextCount(count).verifyComplete();

  }

  @Test
  public void takeUntil(){
    var count = 50;
    Flux<Integer> take = range().takeUntil(i -> i == count-1);

    StepVerifier.create(take).expectNextCount(count).verifyComplete();
  }



  @Test
  public void filter(){
    Flux<Integer> range = Flux.range(0, 1000).take(5);
    Flux<Integer> filter = range.filter(i -> i % 2 == 0);

    StepVerifier.create(filter).expectNext(0, 2, 4).verifyComplete();
  }

  private Flux<Integer> range(){
    return Flux.range(0, 1000);
  }


  private Flux<String> lookup(String word){
    return Flux.just(word + " -> reactive").delayElements(Duration.ofMillis(500));
  }


  private Flux<Integer> delayReplyfor(Integer id, long delay) {
    return Flux.just(id).delayElements(Duration.ofMillis(delay));
  }

  static class Pair{
    private int id;
    private long delay;

    public Pair(int id, long delay) {
      this.id = id;
      this.delay = delay;
    }

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public long getDelay() {
      return delay;
    }

    public void setDelay(long delay) {
      this.delay = delay;
    }
  }


  private void sleep(long l) {
    try{
      Thread.sleep(l);
    }catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


  @Test
  void testFlux() {

    List<Integer> list = new ArrayList<>();

    Flux.just(1, 2, 3, 4)
      .filter(n -> n % 2 == 0)
      .map(n -> n * 2)
      .log()
      .subscribe(n -> list.add(n));

    assertThat(list).containsExactly(4, 8);
  }

  @Test
  void testFluxBlocking() {

    List<Integer> list = Flux.just(1, 2, 3, 4)
      .filter(n -> n % 2 == 0)
      .map(n -> n * 2)
      .log()
      .collectList().block();

    assertThat(list).containsExactly(4, 8);
  }
}
