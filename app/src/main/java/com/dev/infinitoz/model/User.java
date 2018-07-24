package com.dev.infinitoz.model;

public class User {

    private String name;
    private boolean onTrip;
    private String phone;
    private String vehicleType;
    private String uId;
    private String coins;
    private String emailId;


    public User() {
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOnTrip() {
        return onTrip;
    }

    public void setOnTrip(boolean onTrip) {
        this.onTrip = onTrip;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getCoins() {
        return coins;
    }

    public void setCoins(String coins) {
        this.coins = coins;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", onTrip=" + onTrip +
                ", phone='" + phone + '\'' +
                ", vehicleType='" + vehicleType + '\'' +
                ", uId='" + uId + '\'' +
                ", coins=" + coins +
                ", emailId='" + emailId + '\'' +
                '}';
    }

    /* @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(uId, user.uId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(uId);
    }*/
}
