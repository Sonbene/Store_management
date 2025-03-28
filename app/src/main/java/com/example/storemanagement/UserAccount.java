package com.example.storemanagement;

public class UserAccount {
    private String userName;
    private String passWord;
    private String phone;
    private String email;
    private String idRole;

    public UserAccount(String userName, String passWord, String phone, String email, String idRole) {
        this.userName = userName;
        this.passWord = passWord;
        this.phone = phone;
        this.email = email;
        this.idRole = idRole;
    }

    public String getUserName() { return userName; }
    public String getPassWord() { return passWord; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getIDRole() { return idRole; }

    public void setUserName(String userName) { this.userName = userName; }
    public void setPassWord(String passWord) { this.passWord = passWord; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setIDRole(String idRole) { this.idRole = idRole; }
}
