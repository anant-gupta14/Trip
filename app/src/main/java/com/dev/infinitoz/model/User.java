package com.dev.infinitoz.model;

public class User {

    private String name;
    private boolean onTrip;
    private String phone;
    private String vehicleType;
    private String uId;
    private Integer credits;


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

    public Integer getCredits() {
        return credits;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", onTrip=" + onTrip +
                ", phone='" + phone + '\'' +
                ", vehicleType='" + vehicleType + '\'' +
                ", uId='" + uId + '\'' +
                ", credits=" + credits +
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
