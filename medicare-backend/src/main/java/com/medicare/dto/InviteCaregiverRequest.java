package com.medicare.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class InviteCaregiverRequest {

    @NotBlank(message = "Caregiver email is required")
    @Email(message = "Please enter a valid email address")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$", message = "Please enter a valid email address (e.g. user@example.com)")
    private String caregiverEmail;

    private String permissions = "VIEW_DOSES,MISSED_DOSE_ALERTS";

    public InviteCaregiverRequest() {}

    public InviteCaregiverRequest(String caregiverEmail, String permissions) {
        this.caregiverEmail = caregiverEmail;
        this.permissions = permissions;
    }

    public String getCaregiverEmail() {
        return caregiverEmail;
    }

    public void setCaregiverEmail(String caregiverEmail) {
        this.caregiverEmail = caregiverEmail;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
}
