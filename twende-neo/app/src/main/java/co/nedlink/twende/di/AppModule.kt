package co.nedlink.twende.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "twende_prefs")

/**
 * Deliberately tiny DI graph — two providers. Everything else is
 * constructor-injected @Singleton, so Hilt generates almost no code and
 * contributes near-zero to cold start.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore

    @Provides @Singleton
    fun provideFusedLocation(@ApplicationContext ctx: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(ctx)
}
