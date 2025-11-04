package org.pyrrha_platform.ui.login;

import android.content.Context;
import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

// IBM App ID imports temporarily removed for modernization

import org.pyrrha_platform.BuildConfig;
import org.pyrrha_platform.R;
import org.pyrrha_platform.login.LoginDataSource;
import org.pyrrha_platform.login.LoginRepository;


public class LoginViewModel extends ViewModel {
    private final static String TAG = LoginDataSource.class.getName();
    // IBM App ID configuration temporarily removed for modernization

    private final MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private final MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private final Context mcontext;

    LoginViewModel(LoginRepository loginRepository, Context context) {
        this.mcontext = context;
    }

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    public LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login(String username, String password) {
        // Simplified authentication - IBM App ID temporarily removed
        // TODO: Replace with simplified authentication service call
        
        // Simple validation for demo purposes
        if (isUserNameValid(username) && isPasswordValid(password)) {
            // Mock successful login
            loginResult.postValue(new LoginResult(new LoggedInUserView(username, "demo-user-id")));
        } else {
            loginResult.postValue(new LoginResult(R.string.login_failed));
        }
    }

    public void loginDataChanged(String username, String password) {
        if (!isUserNameValid(username)) {
            loginFormState.setValue(new LoginFormState(R.string.invalid_username, null));
        } else if (!isPasswordValid(password)) {
            loginFormState.setValue(new LoginFormState(null, R.string.invalid_password));
        } else {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        if (username == null) {
            return false;
        }
        if (username.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(username).matches();
        } else {
            return !username.trim().isEmpty();
        }
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        return password != null && password.trim().length() > 5;
    }
}