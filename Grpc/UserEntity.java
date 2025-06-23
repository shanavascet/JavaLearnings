package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import javax.persistence.Entity;

@Entity
public class UserEntity extends PanacheEntity {
    public String name;
    public String email;
    public String password;
}
