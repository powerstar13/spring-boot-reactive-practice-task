package task.practice.reactivepracticetask;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import task.practice.reactivepracticetask.domain.model.Person;

import java.time.Duration;

@SpringBootTest
public class PracticeTaskTest {

    /**
     * 1. ["Blenders", "Old", "Johnnie"] 와 ["Pride", "Monk", "Walker"] 를 순서대로 하나의 스트림으로 처리되는 로직 검증
     */
    @Test
    void task1_flux_concat() {

        Flux<String> nameList1 = Flux.just("Blenders", "Old", "Johnnie")
            .delayElements(Duration.ofMillis(100)); // delayElements :  순서를 보장한다.
        Flux<String> nameList2 = Flux.just("Pride", "Monk", "Walker")
            .delayElements(Duration.ofMillis(100));

        Flux<String> nameList = Flux.concat(nameList1, nameList2) // '순서대로' 병합하여 하나의 스트림으로 만들기 위해 concat 사용
            .log();

        StepVerifier.create(nameList)
            .expectSubscription()
            .expectNext("Blenders", "Old", "Johnnie", "Pride", "Monk", "Walker")
            .as("순서대로 하나의 스트림으로 처리되었는지 검증")
            .verifyComplete();
    }

    /**
     * 2. 1~100 까지의 자연수 중 짝수만 출력하는 로직 검증
     */
    @Test
    void task2_flux_filter() {
        // 새로운 Flux를 만들어 짝수만 이루어져 있는지, 갯수가 맞는지 검증
        // 목표는 문제에서 요구하는 Flux와 그 Flux를 검증할 수 있는 Test를 만드는 것이다.

        Flux<Integer> flux = Flux.range(1, 100) // 1 ~ 100 까지의 자연수 Flux 생성
            .filter(i -> i % 2 == 0) // 짝수만 통과
            .log();

        StepVerifier.create(flux)
            .expectNextCount(50)
            .as("짝수의 갯수가 50개가 맞는지 검증")
            .thenConsumeWhile(i -> {
                Assertions.assertEquals(0, i % 2);
                return true;
            })
            .as("짝수만 이루어져 있는지 검증")
            .verifyComplete();
    }

    /**
     * 3. "hello", "there" 를 순차적으로 publish하여 순서대로 나오는지 검증
     */
    @Test
    void task3_flux_publish() {
        // Flux에 hello와 there를 넣어서 publish 하면 된다.
        // just해서 onNext를 확인하라는 말이다.

        Flux<String> flux = Flux.just("hello", "there")
            .publishOn(Schedulers.single())
            .log();

        StepVerifier.create(flux)
            .expectNext("hello", "there")
            .as("onNext에서 hello -> there 순서인지 검증")
            .verifyComplete();
    }

    /**
     * 4. 아래와 같은 객체가 전달될 때 "JOHN", "JACK" 등 이름이 대문자로 변환되어 출력되는 로직 검증
     *
     *     Person("John", "[john@gmail.com](mailto:john@gmail.com)", "12345678")
     *     Person("Jack", "[jack@gmail.com](mailto:jack@gmail.com)", "12345678")
     */
    @Test
    void task4_flux_map() {
        // map 같은 변환 구문을 만들어야 한다.
        // Person Object를 넣고 John과 Jack의 이름을 대문자로 만들고 StepVerifier 하면 된다.

        Person john = new Person("John", "[john@gmail.com](mailto:john@gmail.com)", "12345678");
        Person jack = new Person("Jack", "[jack@gmail.com](mailto:jack@gmail.com)", "12345678");

        Flux<Person> flux = Flux.just(john, jack)
            .map(person -> {
                person.modifyName(person.getName().toUpperCase()); // 이름을 대문자로 변경
                return person; // 변경된 Person 객체를 반환
            })
            .log()
            .doOnNext(person -> System.out.println("Person name : " + person.getName()));

        StepVerifier.create(flux)
            .assertNext(person -> Assertions.assertEquals("JOHN", person.getName()))
            .as("이름이 대문자 JOHN으로 치환되었는지 검증")
            .assertNext(person -> Assertions.assertEquals("JACK", person.getName()))
            .as("이름이 대문자 JACK으로 치환되었는지 검증")
            .verifyComplete();
    }

    /**
     * 5. ["Blenders", "Old", "Johnnie"] 와 ["Pride", "Monk", "Walker"]를 압축하여 스트림으로 처리 검증
     *    - 예상되는 스트림 결과값 ["Blenders Pride", "Old Monk", "Johnnie Walker"]
     */
    @Test
    void task5_flux_zip() {
        // Flux1과 Flux2를 합쳤을 때 1번끼리 붙고, 2번끼리 붙고, 3번끼리 붙는 답이 나와야 한다.

        Flux<String> flux1 = Flux.just("Blenders", "Old", "Johnnie");
        Flux<String> flux2 = Flux.just("Pride", "Monk", "Walker");

        Flux<String> fluxZip = Flux.zip(
                flux1, flux2, // 2개의 Flux를 하나로 병합
                (item1, item2) -> item1 + " " + item2 // 각 Flux 흐름에서 문자열을 나란히 띄워서 놓기
            )
            .log();

        StepVerifier.create(fluxZip)
            .expectNext("Blenders Pride", "Old Monk", "Johnnie Walker")
            .as("값이 나란히 띄워져서 순서대로 붙었는지 검증")
            .verifyComplete();
    }

    /**
     * 6. ["google", "abc", "fb", "stackoverflow"] 의 문자열 중 5자 이상 되는 문자열만 대문자로 비동기로 치환하여 1번 반복하는 스트림으로 처리하는 로직 검증
     *    - 예상되는 스트림 결과값 ["GOOGLE", "STACKOVERFLOW", "GOOGLE", "STACKOVERFLOW"]
     */
    @Test
    void task6_flux_filter_flatMap_repeat() {
        // 문자열 중에서 글자 수 5자 이상만 대문자로 만드는데, 비동기로 처리하고 1번 더 반복시키면 된다.

        Flux<String> flux = Flux.just("google", "abc", "fb", "stackoverflow")
            .filter(s -> s.length() >= 5) // 문자열 중 5자 이상 되는 문자열만 추출
            .publishOn(Schedulers.boundedElastic()) // Reactor는 비동기를 강제하지 않기 때문에 Schedulers를 사용하여 비동기 처리
            .flatMap(s -> Mono.just(s.toUpperCase())) // 대문자 치환
            .repeat(1) // 1번 더 반복
            .log();

        StepVerifier.create(flux)
            .expectNext("GOOGLE", "STACKOVERFLOW", "GOOGLE", "STACKOVERFLOW")
            .as("5자 이상 되는 문자열만 비동기로 대문자로 치환되고 한 번 더 반복 했는지 검증")
            .verifyComplete();
    }
}
