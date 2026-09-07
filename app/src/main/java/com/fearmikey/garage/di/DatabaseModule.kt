package com.fearmikey.garage.di

import android.content.Context
import androidx.room.Room
import com.fearmikey.garage.data.local.GARAGE_DATABASE_NAME
import com.fearmikey.garage.data.local.GarageDatabase
import com.fearmikey.garage.data.local.dao.MaintenanceDao
import com.fearmikey.garage.data.local.dao.ReminderDao
import com.fearmikey.garage.data.local.dao.VehicleDao
import com.fearmikey.garage.data.local.dao.VehicleSpecsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGarageDatabase(@ApplicationContext context: Context): GarageDatabase =
        Room.databaseBuilder(context, GarageDatabase::class.java, GARAGE_DATABASE_NAME)
            .build()

    @Provides
    fun provideVehicleDao(database: GarageDatabase): VehicleDao = database.vehicleDao()

    @Provides
    fun provideMaintenanceDao(database: GarageDatabase): MaintenanceDao = database.maintenanceDao()

    @Provides
    fun provideReminderDao(database: GarageDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun provideVehicleSpecsDao(database: GarageDatabase): VehicleSpecsDao = database.vehicleSpecsDao()
}
