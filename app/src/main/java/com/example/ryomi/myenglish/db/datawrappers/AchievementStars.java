package com.example.ryomi.myenglish.db.datawrappers;

public class AchievementStars {
    Boolean firstInstance;
    Boolean repeatInstance;
    Boolean secondInstance;
    public AchievementStars(){

    }

    public AchievementStars(Boolean firstInstance, Boolean repeatInstance, Boolean secondInstance){
        this.firstInstance = firstInstance;
        this.repeatInstance = repeatInstance;
        this.secondInstance = secondInstance;
    }

    public void setFirstInstance(Boolean firstInstance){
        this.firstInstance = firstInstance;
    }

    public void setRepeatInstance(Boolean repeatInstance){
        this.repeatInstance = repeatInstance;
    }

    public void setSecondInstance(Boolean secondInstance){
        this.secondInstance = secondInstance;
    }

    public Boolean getFirstInstance(){
        return this.firstInstance;
    }

    public Boolean getRepeatInstance(){
        return this.repeatInstance;
    }

    public Boolean getSecondInstance(){
        return this.secondInstance;
    }
}
