package org.pyrrha_platform.ui.login;

import android.content.Context;
import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ibm.cloud.appid.android.api.AppID;
import com.ibm.cloud.appid.android.api.AppIDAuthorizationManager;
import com.ibm.cloud.appid.android.api.AuthorizationException;
import com.ibm.cloud.appid.android.api.TokenResponseListener;
import com.ibm.cloud.appid.android.api.tokens.AccessToken;
import com.ibm.cloud.appid.android.api.tokens.IdentityToken;
import com.ibm.cloud.appid.android.api.tokens.RefreshToken;

import org.pyrrha_platform.R;
import org.pyrrha_platform.login.LoginDataSource;
import org.pyrrha_platform.login.LoginRepository;


public class LoginViewModel extends ViewModel {
    private final static String TAG = LoginDataSource.class.getName();
    private final static String region = AppID.REGION_UK;
    private final static String authTenantId = "2e5771a5-80bf-4879-bbf9-d5135ccde03a";

    private AppID appId;
    private AppIDAuthorizationManager appIDAuthorizationManager;

    private final MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private final MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private final LoginRepository loginRepository;
    private final Context mcontext;

    LoginViewModel(LoginRepository loginRepository, Context context) {
        this.loginRepository = loginRepository;
        this.mcontext = context;
    }

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    public LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login(String username, String password) {
        this.appId = AppID.getInstance();
        appId.initialize(this.mcontext, authTenantId, region);
        this.appIDAuthorizationManager = new AppIDAuthorizationManager(this.appId);
        AppID.getInstance().signinWithResourceOwnerPassword(this.mcontext, username, password, new TokenResponseListener() {
            @Override
            public void onAuthorizationFailure(AuthorizationException exception) {
                // Exception occurred
                loginResult.postValue(new LoginResult(R.string.login_failed));
            }

            @Override
            public void onAuthorizationSuccess(AccessToken accessToken, IdentityToken identityToken, RefreshToken refreshToken) {
                // User authenticated
                loginResult.postValue(new LoginResult(new LoggedInUserView(identityToken.getName(), identityToken.getSubject())));
                System.out.println(identityToken.getSubject());
            }
        });
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