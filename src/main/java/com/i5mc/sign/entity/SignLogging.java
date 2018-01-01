package com.i5mc.sign.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Created by on 1月1日.
 */
@Entity
@Data
public class SignLogging {

    @Id
    private int id;
    private UUID player;
    @Column(length = 16)
    private String name;
    private Timestamp dateSigned;
}
