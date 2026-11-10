package za.co.skoolswap.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import za.co.skoolswap.data.repository.ChatRepository
import za.co.skoolswap.domain.repository.ChatRepositoryInterface
import javax.inject.Singleton

/**
 * ✅ CHAT MODULE
 * Tells Hilt: "When someone asks for ChatRepositoryInterface,
 *             give them a ChatRepository."
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ChatModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepository
    ): ChatRepositoryInterface
}