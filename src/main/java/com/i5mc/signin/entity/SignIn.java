package com.i5mc.signin.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created on 16-8-10.
 */
@Entity
@Table(name = "pre_plugin_k_misign")
public class SignIn {

    @Id
    private int uid;

    @Column// unix timestamp
    private int time;

    @Column// total signed days
    private int days;

    @Column// the day lasted
    private int lasted;

    @Column// the day lasted this month
    private int mdays;

    @Column// earned total
    private int reward;

    @Column
    private int lastreward;

    @Column
    private int timeApp;


    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public int getLasted() {
        return lasted;
    }

    public void setLasted(int lasted) {
        this.lasted = lasted;
    }

    public int getMdays() {
        return mdays;
    }

    public void setMdays(int mdays) {
        this.mdays = mdays;
    }

    public int getReward() {
        return reward;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }

    public int getLastreward() {
        return lastreward;
    }

    public void setLastreward(int lastreward) {
        this.lastreward = lastreward;
    }

    public int getTimeApp() {
        return timeApp;
    }

    public void setTimeApp(int timeApp) {
        this.timeApp = timeApp;
    }

}
