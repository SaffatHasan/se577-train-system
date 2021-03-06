package edu.drexel.TrainDemo.user.models;

import javax.persistence.*;

@Entity(name = "user_tbl")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "serial")
    private Long id;
    private String name;
    private String email;
    @Column(unique = true)
    private long externalId;

    public UserEntity() {
    }

    public UserEntity(String name, String email, long externalId) {
        this.name = name;
        this.email = email;
        this.externalId = externalId;
    }

    public UserEntity(Long id, String name, String email, long externalId) {
        this(name, email, externalId);
        this.id = id;
    }

    public UserEntity(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }

    public long getExternalId() {
        return externalId;
    }

    public void setExternalId(Long externalId) {
        this.externalId =  externalId;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + "\'" +
                ", email='" + email + "\'" +
                ", external_id=" + externalId +
                '}';
    }
}
