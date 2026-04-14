package com.apollo.mira.di

import android.content.Context
import androidx.room3.Room
import com.apollo.mira.data.local.MiraDatabase
import com.apollo.mira.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideMiraDatabase(@ApplicationContext context: Context): MiraDatabase =
        Room.databaseBuilder(
            context,
            MiraDatabase::class.java,
            MiraDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        // fallbackToDestructiveMigration: khi version tăng mà chưa có Migration
        // -> xóa DB cũ, tạo lại từ đầu
        // CHỈ dùng trong development! Production cần Migration thật sự
        .build()

    @Provides
    // DAO KHÔNG cần @Singleton vì nó là interface, Room tạo proxy nhẹ
    // Mỗi lần inject đều lấy từ cùng 1 Database instance (đã @Singleton)
    fun provideTransactionDao(database: MiraDatabase): TransactionDao =
        database.transactionDao()
        // Hilt biết cần MiraDatabase -> tự gọi provideMiraDatabase() ở trên
        // Đây là "dependency graph" Hilt tự resolve thứ tự
}