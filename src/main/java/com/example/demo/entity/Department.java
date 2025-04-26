package com.example.demo.entity;


import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Department")
@Data
@NoArgsConstructor
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;  // 부서명 추가

    @Column(nullable = false)
    private Boolean submission;
    // List를 Entity 필드로 사용하려면 @ElementCollection을 사용해야 합니다
    @ElementCollection
    @CollectionTable(name = "department_files", joinColumns = @JoinColumn(name = "department_id"))
    @Column(name = "file_path")
    private List<String> additionalFilePaths = new ArrayList<>();

    // Member와의 양방향 관계 설정 (필요한 경우)
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private List<Member> members = new ArrayList<>();

    public Department(String name) {
        this.name = name;
        this.submission = false;
    }
}
