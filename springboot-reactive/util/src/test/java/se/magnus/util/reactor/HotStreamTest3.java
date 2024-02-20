package se.magnus.util.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class HotStreamTest3 {
  private List<Integer> one = new ArrayList<Integer>();
  private List<Integer> two = new ArrayList<Integer>();
  private List<Integer> three = new ArrayList<Integer>();

  private Consumer<Integer> subscribe(List<Integer> list){
    return list::add;
  }

  @Test
  public void publish() throws Exception {
    Flux<Integer> pileOn = Flux.just(1, 2, 3).publish().autoConnect(3)
        .subscribeOn(Schedulers.immediate());
    pileOn.subscribe(subscribe(one));
    assertEquals(this.one.size(), 0);

    pileOn.subscribe(subscribe(two));
    assertEquals(this.two.size(), 0);

    pileOn.subscribe(subscribe(three));
    assertEquals(this.three.size(), 3);
    assertEquals(this.two.size(), 3);
    assertEquals(this.three.size(), 3);

  }

}
