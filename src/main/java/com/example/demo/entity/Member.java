package com.example.demo.entity;


import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "Member")
@Data
@NoArgsConstructor
public class Member {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    // Department는 단순 컬럼이 아닌 관계로 표현해야 합니다
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private String position;

    @Column
    private Integer orders;

    @Column(nullable = false)
    private Boolean isSubmissionRequired;

    @Column(name = "ppt_file_path")
    private String pptFilePath;

    @Column(name = "ppt_file_title")
    private String pptFileTitle;

    public Member(String name, String email, Department department, String position) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.position = position;
        this.orders = 1;
        this.isSubmissionRequired = false;
    }
    public Member(String name, String email, Department department, String position,Boolean isSubmissionRequired) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.position = position;
        this.orders = 1;
        this.isSubmissionRequired = isSubmissionRequired;
    }
    public Member(String name, String email, Department department, String position,Integer orders,Boolean isSubmissionRequired) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.position = position;
        this.orders = orders;
        this.isSubmissionRequired = isSubmissionRequired;
    }
}
