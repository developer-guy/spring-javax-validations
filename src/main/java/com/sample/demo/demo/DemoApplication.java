package com.sample.demo.demo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.GroupSequence;
import javax.validation.constraints.NotBlank;
import javax.validation.groups.Default;
import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(StudentRepository studentRepository) {

        return args -> {
            studentRepository.save(new Student("Batuhan", "Apaydın"));
            studentRepository.save(new Student("Asena", "Tekin"));
        };

    }
}


@Data
@Entity
@AllArgsConstructor
class Student implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private Long id;

    public Student() {

    }

    public Student(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    private String firstName;
    private String lastName;
}

@Repository
interface StudentRepository extends CrudRepository<Student, Long> {
}


interface Expensive {
}

@GroupSequence({Default.class, Expensive.class})
interface ValidationSequence {
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class StudentCreateRequest implements Serializable {
    @NotBlank(groups = Expensive.class)
    private String firstName;
    @NotBlank(groups = Expensive.class)
    private String lastName;

    public Student aStudent() {
        return new Student(null, getFirstName(), getLastName());
    }
}


@RestController
@RequestMapping(value = "students")
@RequiredArgsConstructor
class StudentController {
    private final StudentRepository studentRepository;

    @GetMapping
    public ResponseEntity<List<Student>> findAll() {
        List<Student> body = StreamSupport.stream(studentRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
        return ResponseEntity.ok().body(body);
    }


    @PostMapping
    public ResponseEntity<Object> save(@Validated(ValidationSequence.class) @RequestBody StudentCreateRequest request, Errors errors) {
        if (errors.hasErrors()) {
            String errorsWithJoiningComma = errors.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(","));
            return ResponseEntity
                    .badRequest()
                    .body(errorsWithJoiningComma);
        }

        Student student = request.aStudent();

        URI uri = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

        return ResponseEntity
                .created(uri)
                .body(studentRepository.save(student));
    }

}