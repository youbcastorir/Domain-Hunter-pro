package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. ScannedDomain Entity
@Entity(tableName = "scanned_domains")
data class ScannedDomain(
    @PrimaryKey val domainName: String,
    val overallScore: Int,
    val grade: String, // S, A, B, C, D
    val price: Double,
    val extension: String,
    val da: Int,
    val backlinks: Long,
    val tf: Int,
    val cf: Int,
    val ageYears: Int,
    val waybackTraffic: Boolean, // Had active website & traffic
    val hadActiveWebsite: Boolean = true,
    val registryAgeCreatedYear: Int = 2012,
    val uniqueReferringDomains: Int = 120,
    val dofollowRatio: Float = 0.75f,
    val characterCount: Int = 10,
    val pronounceable: Boolean = true,
    val memorable: Boolean = true,
    val suggestedNiches: String, // Comma-separated niches
    val similarSoldDomains: String, // E.g., "ai-smart.com: $15,000"
    val suggestedResalePrice: Double = 2500.0,
    val verdict: String = "Consider", // "Buy Now" | "Consider" | "Skip"
    val buyVerdictReason: String = "",
    val riskFactors: String = "", // Comma-separated list
    val scannedAt: Long = System.currentTimeMillis()
)

// 2. WatchlistDomain Entity
@Entity(tableName = "watchlist_domains")
data class WatchlistDomain(
    @PrimaryKey val domainName: String,
    val overallScore: Int,
    val grade: String,
    val price: Double,
    val extension: String,
    val da: Int,
    val backlinks: Long,
    val tf: Int,
    val cf: Int,
    val ageYears: Int,
    val waybackTraffic: Boolean,
    val addedAt: Long = System.currentTimeMillis(),
    val initialPrice: Double,
    val lastCheckedPrice: Double,
    val daysSaved: Int = 1
)

// 3. PortfolioDomain Entity
@Entity(tableName = "portfolio_domains")
data class PortfolioDomain(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val domainName: String,
    val buyPrice: Double,
    val buyDate: String, // E.g., "2026-03-12"
    val platformBought: String, // E.g., "GoDaddy", "Namecheap"
    val targetSellPrice: Double,
    val currentListingPlatform: String, // E.g., "Afternic", "Sedo"
    val status: String, // "Listed" | "Not Listed" | "Sold" | "Expired"
    val renewalCost: Double = 12.0,
    val actualSalePrice: Double = 0.0,
    val soldDate: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

// 4. SmartAlert Entity
@Entity(tableName = "smart_alerts")
data class SmartAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val alertName: String,
    val keywordPattern: String,
    val extensionFilter: String, // Comma-separated, e.g. ".com,.ai,.io"
    val maxPrice: Double,
    val minScore: Int,
    val domainTypes: String, // Comma-separated list, e.g. "expired,auction"
    val enabled: Boolean = true,
    val lastTriggeredDate: String = "Never",
    val matchCount: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

// Legacy compatibility tables for Broker Suite/Outreach references (if needed)
@Entity(tableName = "analyzed_domains")
data class AnalyzedDomain(
    @PrimaryKey val domainName: String,
    val sellabilityScore: Int,
    val suggestedPrice: Double,
    val minimumPrice: Double,
    val strategy: String,
    val bestPlatforms: String,
    val buyerProfiles: String,
    val coldEmailSubject: String,
    val coldEmailBody: String,
    val linkedinDM: String,
    val whatsappMessage: String,
    val followUpDay3: String,
    val followUpDay7: String,
    val followUpDay14: String,
    val negotiationTactic: String,
    val negotiationUrgencyLine: String,
    val autoFollowUpEnabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "outreach_leads")
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firstName: String,
    val lastName: String,
    val email: String,
    val company: String,
    val source: String,
    val assignedDomain: String,
    val stage: String,
    val daysInStage: Int = 0,
    val lastAction: String = "Lead Found",
    val salePrice: Double = 0.0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sent_emails")
data class SentEmail(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recipientName: String,
    val recipientEmail: String,
    val domain: String,
    val subject: String,
    val body: String,
    val dateSent: Long = System.currentTimeMillis(),
    val status: String
)

// 5. Data Access Object (DAO)
@Dao
interface DomainDao {
    // Legacy compatible tables
    @Query("SELECT * FROM analyzed_domains ORDER BY addedAt DESC")
    fun getAllDomainsFlow(): Flow<List<AnalyzedDomain>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomain(domain: AnalyzedDomain)
    @Query("DELETE FROM analyzed_domains WHERE domainName = :domainName")
    suspend fun deleteDomainByName(domainName: String)
    @Query("UPDATE analyzed_domains SET autoFollowUpEnabled = :enabled WHERE domainName = :domainName")
    suspend fun toggleAutoFollowUp(domainName: String, enabled: Boolean)

    @Query("SELECT * FROM outreach_leads ORDER BY addedAt DESC")
    fun getAllLeadsFlow(): Flow<List<Lead>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: Lead)
    @Delete
    suspend fun deleteLead(lead: Lead)
    @Query("DELETE FROM outreach_leads WHERE id = :id")
    suspend fun deleteLeadById(id: Int)
    @Query("UPDATE outreach_leads SET stage = :stage, lastAction = :lastAction, daysInStage = :daysInStage WHERE id = :id")
    suspend fun updateLeadStage(id: Int, stage: String, lastAction: String, daysInStage: Int)
    @Query("UPDATE outreach_leads SET salePrice = :salePrice, stage = 'Sold', lastAction = 'Deal Sold' WHERE id = :id")
    suspend fun markLeadAsSold(id: Int, salePrice: Double)

    @Query("SELECT * FROM sent_emails ORDER BY dateSent DESC")
    fun getAllSentEmailsFlow(): Flow<List<SentEmail>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentEmail(email: SentEmail)
    @Query("UPDATE sent_emails SET status = :status WHERE id = :id")
    suspend fun updateEmailStatus(id: Int, status: String)

    // Domain Sniper Pro DAOs
    @Query("SELECT * FROM scanned_domains ORDER BY scannedAt DESC")
    fun getAllScannedDomains(): Flow<List<ScannedDomain>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannedDomain(domain: ScannedDomain)
    @Query("DELETE FROM scanned_domains WHERE domainName = :domainName")
    suspend fun deleteScannedDomain(domainName: String)
    @Query("DELETE FROM scanned_domains")
    suspend fun clearScannedDomains()

    @Query("SELECT * FROM watchlist_domains ORDER BY addedAt DESC")
    fun getWatchlistFlow(): Flow<List<WatchlistDomain>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistDomain(domain: WatchlistDomain)
    @Query("DELETE FROM watchlist_domains WHERE domainName = :domainName")
    suspend fun deleteWatchlistDomain(domainName: String)
    @Query("DELETE FROM watchlist_domains")
    suspend fun clearWatchlist()

    @Query("SELECT * FROM portfolio_domains ORDER BY addedAt DESC")
    fun getPortfolioFlow(): Flow<List<PortfolioDomain>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolioDomain(domain: PortfolioDomain)
    @Delete
    suspend fun deletePortfolioDomain(domain: PortfolioDomain)
    @Query("DELETE FROM portfolio_domains WHERE id = :id")
    suspend fun deletePortfolioDomainById(id: Int)
    @Query("DELETE FROM portfolio_domains")
    suspend fun clearPortfolio()

    @Query("SELECT * FROM smart_alerts ORDER BY addedAt DESC")
    fun getSmartAlertsFlow(): Flow<List<SmartAlert>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmartAlert(alert: SmartAlert)
    @Delete
    suspend fun deleteSmartAlert(alert: SmartAlert)
    @Query("UPDATE smart_alerts SET enabled = :enabled WHERE id = :id")
    suspend fun toggleAlertEnabled(id: Int, enabled: Boolean)
    @Query("UPDATE smart_alerts SET lastTriggeredDate = :date, matchCount = matchCount + 1 WHERE id = :id")
    suspend fun incrementAlertMatch(id: Int, date: String)
    @Query("DELETE FROM smart_alerts")
    suspend fun clearSmartAlerts()

    // Global clear operations
    @Query("DELETE FROM analyzed_domains")
    suspend fun clearAllDomains()
    @Query("DELETE FROM outreach_leads")
    suspend fun clearAllLeads()
    @Query("DELETE FROM sent_emails")
    suspend fun clearAllSentEmails()
}

// 6. Database Class
@Database(
    entities = [
        AnalyzedDomain::class, Lead::class, SentEmail::class,
        ScannedDomain::class, WatchlistDomain::class,
        PortfolioDomain::class, SmartAlert::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun domainDao(): DomainDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "domain_outreach_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// 7. DomainRepository
class DomainRepository(private val domainDao: DomainDao) {
    val analyzedDomains: Flow<List<AnalyzedDomain>> = domainDao.getAllDomainsFlow()
    val outreachLeads: Flow<List<Lead>> = domainDao.getAllLeadsFlow()
    val sentEmails: Flow<List<SentEmail>> = domainDao.getAllSentEmailsFlow()

    val scannedDomains: Flow<List<ScannedDomain>> = domainDao.getAllScannedDomains()
    val watchlistDomains: Flow<List<WatchlistDomain>> = domainDao.getWatchlistFlow()
    val portfolioDomains: Flow<List<PortfolioDomain>> = domainDao.getPortfolioFlow()
    val smartAlerts: Flow<List<SmartAlert>> = domainDao.getSmartAlertsFlow()

    suspend fun insertDomain(domain: AnalyzedDomain) = domainDao.insertDomain(domain)
    suspend fun deleteDomainByName(name: String) = domainDao.deleteDomainByName(name)
    suspend fun toggleAutoFollowUp(name: String, enabled: Boolean) = domainDao.toggleAutoFollowUp(name, enabled)

    suspend fun insertLead(lead: Lead) = domainDao.insertLead(lead)
    suspend fun deleteLead(lead: Lead) = domainDao.deleteLead(lead)
    suspend fun deleteLeadById(id: Int) = domainDao.deleteLeadById(id)
    suspend fun updateLeadStage(id: Int, stage: String, lastAction: String, daysInStage: Int) = 
        domainDao.updateLeadStage(id, stage, lastAction, daysInStage)
    suspend fun markLeadAsSold(id: Int, salePrice: Double) = domainDao.markLeadAsSold(id, salePrice)

    suspend fun insertSentEmail(email: SentEmail) = domainDao.insertSentEmail(email)
    suspend fun updateEmailStatus(id: Int, status: String) = domainDao.updateEmailStatus(id, status)

    // Domain Sniper Pro
    suspend fun insertScannedDomain(domain: ScannedDomain) = domainDao.insertScannedDomain(domain)
    suspend fun deleteScannedDomain(name: String) = domainDao.deleteScannedDomain(name)
    suspend fun clearScannedDomains() = domainDao.clearScannedDomains()

    suspend fun insertWatchlistDomain(domain: WatchlistDomain) = domainDao.insertWatchlistDomain(domain)
    suspend fun deleteWatchlistDomain(name: String) = domainDao.deleteWatchlistDomain(name)
    suspend fun clearWatchlist() = domainDao.clearWatchlist()

    suspend fun insertPortfolioDomain(domain: PortfolioDomain) = domainDao.insertPortfolioDomain(domain)
    suspend fun deletePortfolioDomain(domain: PortfolioDomain) = domainDao.deletePortfolioDomain(domain)
    suspend fun deletePortfolioDomainById(id: Int) = domainDao.deletePortfolioDomainById(id)
    suspend fun clearPortfolio() = domainDao.clearPortfolio()

    suspend fun insertSmartAlert(alert: SmartAlert) = domainDao.insertSmartAlert(alert)
    suspend fun deleteSmartAlert(alert: SmartAlert) = domainDao.deleteSmartAlert(alert)
    suspend fun toggleAlertEnabled(id: Int, enabled: Boolean) = domainDao.toggleAlertEnabled(id, enabled)
    suspend fun incrementAlertMatch(id: Int, date: String) = domainDao.incrementAlertMatch(id, date)
    suspend fun clearSmartAlerts() = domainDao.clearSmartAlerts()

    suspend fun clearAllData() {
        domainDao.clearAllDomains()
        domainDao.clearAllLeads()
        domainDao.clearAllSentEmails()
        domainDao.clearScannedDomains()
        domainDao.clearWatchlist()
        domainDao.clearPortfolio()
        domainDao.clearSmartAlerts()
    }
}
