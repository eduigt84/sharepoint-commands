package sharepoint.beans.igt.com;

public class User {
    String title;
    String email;
    String displayName;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String toString() {
        return String.format("Name: %s, Email: %s, Title: %s", this.displayName, this.email, this.title);
    }
}
