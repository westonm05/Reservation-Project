package edu.campbellsville.lablink.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** What the "Change password" form on the account page submits. */
public class PasswordChangeForm {

    @NotBlank(message = "Enter your current password")
    private String currentPassword;

    @NotBlank(message = "Enter a new password")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Please confirm your new password")
    private String confirmPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
