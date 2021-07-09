package org.pyrrha_platform.ui.login;

/**
 * Class exposing authenticated user details to the UI.
 */
class LoggedInUserView {
    private String displayName;
    private String userToken;
    //... other data fields that may be accessible to the UI

    LoggedInUserView(String displayName, String userToken) {
        this.displayName = displayName;
        this.userToken = userToken;
    }

    String getDisplayName() {
        return displayName;
    }

    String getUserToken() {
        return userToken;
    }

}