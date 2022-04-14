package task.practice.reactivepracticetask.domain.model;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Person {

    private String name;
    private String email;
    private String phone;

    public void modifyName(String name) {
        this.name = name;
    }
}
