package za.co.skoolswap.di


import androidx.lifecycle.ViewModel
import za.co.skoolswap.ui.login.LoginViewModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelModule {

    @Binds
    @ViewModelScoped
    abstract fun bindLoginViewModel(loginViewModel: LoginViewModel): ViewModel
}