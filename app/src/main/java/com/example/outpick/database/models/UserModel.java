package com.example.outpick.database.models;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/**
 * Model class representing a user in the application.
 * Updated for Supabase compatibility (String IDs instead of int IDs)
 */
public class UserModel implements Parcelable {
    private String id; // Changed from int to String for Supabase UUID
    private String username;
    private String password;
    private String gender;
    private String signupDate;
    private String lastLogin;
    private String lastLogout;
    private boolean suspended;
    private String role;       // "User" or "Admin"
    private String status;     // "Active" / "Offline" / "Suspended"
    private String profileImageUri; // URI or URL to the user's profile image

    // --- Empty constructor (needed for Firebase, Room/SQLite, and adapters) ---
    public UserModel() { }

    // --- Full constructor (with everything) ---
    public UserModel(String id, String username, String password, String gender,
                     String signupDate, String lastLogin, String lastLogout,
                     boolean suspended, String role, String status, String profileImageUri) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.gender = gender;
        this.signupDate = signupDate;
        this.lastLogin = lastLogin;
        this.lastLogout = lastLogout;
        this.suspended = suspended;
        this.role = role;
        this.status = status;
        this.profileImageUri = profileImageUri;
    }

    // --- Short constructor (for minimal data fetching, initializes other fields to safe defaults) ---
    public UserModel(String id, String username, String signupDate,
                     String lastLogin, String lastLogout, String role) {
        this.id = id;
        this.username = username;
        this.password = null;
        this.gender = null;
        this.signupDate = signupDate;
        this.lastLogin = lastLogin;
        this.lastLogout = lastLogout;
        this.suspended = false;
        this.role = role;
        this.status = "Offline";
        this.profileImageUri = null;
    }

    // ----------------- GETTERS -----------------
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getGender() { return gender; }
    public String getSignupDate() { return signupDate; }
    public String getLastLogin() { return lastLogin; }
    public String getLastLogout() { return lastLogout; }
    public boolean isSuspended() { return suspended; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getProfileImageUri() { return profileImageUri; }

    // ----------------- SETTERS -----------------
    public void setId(String id) { this.id = id; } // Changed to String
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setGender(String gender) { this.gender = gender; }
    public void setSignupDate(String signupDate) { this.signupDate = signupDate; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
    public void setLastLogout(String lastLogout) { this.lastLogout = lastLogout; }
    public void setSuspended(boolean suspended) { this.suspended = suspended; }
    public void setRole(String role) { this.role = role; }
    public void setStatus(String status) { this.status = status; }
    public void setProfileImageUri(String profileImageUri) { this.profileImageUri = profileImageUri; }

    // ----------------- Utility Methods -----------------

    @Override
    public String toString() {
        return "UserModel{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", gender='" + gender + '\'' +
                ", signupDate='" + signupDate + '\'' +
                ", lastLogin='" + lastLogin + '\'' +
                ", lastLogout='" + lastLogout + '\'' +
                ", suspended=" + suspended +
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                ", profileImageUri='" + profileImageUri + '\'' +
                '}';
    }

    /**
     * Checks equality based on the immutable ID and the unique username.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserModel)) return false;
        UserModel userModel = (UserModel) o;
        // Using String ID and Username for equality
        return Objects.equals(id, userModel.id) && Objects.equals(username, userModel.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    // ----------------- Parcelable Implementation -----------------

    protected UserModel(Parcel in) {
        id = in.readString(); // Changed to readString
        username = in.readString();
        password = in.readString();
        gender = in.readString();
        signupDate = in.readString();
        lastLogin = in.readString();
        lastLogout = in.readString();
        suspended = in.readByte() != 0;
        role = in.readString();
        status = in.readString();
        profileImageUri = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id); // Changed to writeString
        dest.writeString(username);
        dest.writeString(password);
        dest.writeString(gender);
        dest.writeString(signupDate);
        dest.writeString(lastLogin);
        dest.writeString(lastLogout);
        dest.writeByte((byte) (suspended ? 1 : 0));
        dest.writeString(role);
        dest.writeString(status);
        dest.writeString(profileImageUri);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserModel> CREATOR = new Creator<UserModel>() {
        @Override
        public UserModel createFromParcel(Parcel in) {
            return new UserModel(in);
        }

        @Override
        public UserModel[] newArray(int size) {
            return new UserModel[size];
        }
    };
}