package org.pyrrha_platform.ui.login;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.pyrrha_platform.login.LoginDataSource;
import org.pyrrha_platform.login.LoginRepository;
import android.content.Context;

/**
 * ViewModel provider factory to instantiate LoginViewModel.
 * Required given LoginViewModel has a non-empty constructor
 */
public class LoginViewModelFactory implements ViewModelProvider.Factory {

    private Context mContext;

    public LoginViewModelFactory(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(LoginRepository.getInstance(new LoginDataSource()), mContext);
        } else {
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}