package cc.synx.domain;

public class User {
    private String id;
    private String mobilePhone;
    private String name;

    public User() {
    }

    public User(String id, String mobilePhone, String name) {
        this.id = id;
        this.mobilePhone = mobilePhone;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
