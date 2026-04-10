package com.apollo.mira.di

import com.apollo.mira.data.repository.TransactionRepositoryImpl
import com.apollo.mira.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    // Abstraction function: Hilt đọc signature để biết
    // "khi ai đó cần TransactionRepository -> inject TransactionRepositoryImpl"
    //
    // TransactionRepository được inject tự động vì nó có @Inject
    // (đã khai báo trong file TransactionRepositoryImpl.kt)
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    // Nếu sau này thêm Repository khác, thêm @Binds function ở đây
    // abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

}