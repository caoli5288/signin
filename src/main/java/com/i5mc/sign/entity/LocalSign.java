package com.i5mc.sign.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Created by on 2017/8/7.
 */
@Data
@Entity
@EqualsAndHashCode(of = "id")
public class LocalSign {

    @Id
    private UUID id;
    @Column(length = 16)
    private String name;
    private int dayTotal;
    private int lasted;
    private Timestamp latest;
}
