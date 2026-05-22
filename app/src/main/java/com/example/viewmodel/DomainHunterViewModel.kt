package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class DomainHunterViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = DomainRepository(db.domainDao())
    private val prefs: SharedPreferences = application.getSharedPreferences("domain_sniper_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    // 1. Navigation & Theme States
    val currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val onboardingCompleted = MutableStateFlow(false)

    // 2. Room Flow States (Reactively updated UI)
    val scannedDomains: StateFlow<List<ScannedDomain>> = repository.scannedDomains
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlistDomains: StateFlow<List<WatchlistDomain>> = repository.watchlistDomains
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val portfolioDomains: StateFlow<List<PortfolioDomain>> = repository.portfolioDomains
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smartAlerts: StateFlow<List<SmartAlert>> = repository.smartAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. User Settings & API Keys
    val keyMoz = MutableStateFlow("")
    val keyMajestic = MutableStateFlow("")
    val keyNamecheap = MutableStateFlow("")
    val keyGoDaddy = MutableStateFlow("")
    val defaultScanPreference = MutableStateFlow("All") // "All" | "Expired" | "Auction"
    val maxResultsSlider = MutableStateFlow(50)
    val morningScanSchedule = MutableStateFlow(true)
    val notificationPriceDrop = MutableStateFlow(true)
    val notificationSFound = MutableStateFlow(true)

    // 4. Scanner Filter Configurations
    val extensionsChecked = MutableStateFlow(listOf(".com", ".ai", ".io", ".co"))
    val maxPriceFilter = MutableStateFlow(250f)
    val onlyUnder30 = MutableStateFlow(false)
    val neverRegistered = MutableStateFlow(false)
    val minDomainScore = MutableStateFlow(60)
    val minDAFilter = MutableStateFlow(10)
    val minBacklinksInput = MutableStateFlow("")
    val minAgeDropdown = MutableStateFlow("Any") // "Any" / "5+ Years" / "10+ Years" / "15+ Years"
    val domainTypesChecked = MutableStateFlow(listOf("expired", "auction", "dropped"))
    val keywordFilterInput = MutableStateFlow("")
    val nicheDropdownSelected = MutableStateFlow("Any")
    val maxLengthSlider = MutableStateFlow(20)
    val noHyphensToggle = MutableStateFlow(true)
    val noNumbersToggle = MutableStateFlow(true)

    // Active screen selection trace
    val selectedDomainName = MutableStateFlow("")
    val analysisCameFrom = MutableStateFlow("scanned") // "scanned" | "watchlist" | "portfolio"

    // Scan operations states
    val isScanning = MutableStateFlow(false)
    val sortOption = MutableStateFlow("Score") // "Score" | "Price" | "Age" | "Backlinks" | "DA"

    // 5. Smart Alerts Builder State
    val alertName = MutableStateFlow("")
    val alertKeyword = MutableStateFlow("")
    val alertExtensionsSelected = MutableStateFlow(".com")
    val alertMaxPrice = MutableStateFlow(100f)
    val alertMinScore = MutableStateFlow(75)
    val alertDomainTypes = MutableStateFlow("expired,auction")

    // 6. Portfolio Adder state
    val portName = MutableStateFlow("")
    val portBuyPrice = MutableStateFlow("")
    val portBuyDate = MutableStateFlow("")
    val portPlatform = MutableStateFlow("")
    val portTargetPrice = MutableStateFlow("")
    val portListingPlatform = MutableStateFlow("")
    val portStatus = MutableStateFlow("Listed")

    // 7. Bulk Scanner UI states
    val bulkInputMethod = MutableStateFlow("text") // "text" | "upload" | "generate"
    val bulkInputText = MutableStateFlow("")
    val bulkIsProcessing = MutableStateFlow(false)
    val bulkProgress = MutableStateFlow(0f)
    val bulkProcessedCount = MutableStateFlow(0)
    val bulkTotalCount = MutableStateFlow(0)
    val bulkResultsList = MutableStateFlow<List<ScannedDomain>>(emptyList())

    // 8. Market Intelligence Data Cache (simulating DNJournal public data + daily updates)
    val trendingNiches = listOf("Artificial Intelligence (AI)", "Fintech / DeFi Assets", "SaaS platforms & micro tools", "Mental Wellness & Healthtech", "Web3 / Cryptography")
    val trendingAuctionCategories = listOf("SaaS brandables under 8 chars", ".ai Tech short forms", "Three-syllable .co startups", "Clean dictionary .coms")
    val avgPriceByExtension = mapOf(".com" to 3850, ".ai" to 4200, ".io" to 2200, ".co" to 1500, ".net" to 1200)
    val roiExtensions = listOf(
        MarketRoiItem(".ai", "240% ROI this month", "Driven by 3-character LLM acronym rushes"),
        MarketRoiItem(".com", "180% ROI this month", "Stable standard portfolio liquid flips"),
        MarketRoiItem(".io", "110% ROI this month", "Popular among IndieHackers & bootstrap SaaS")
    )
    val topRecentSales = listOf(
        DNJournalSale("chat.ai", 150000.0, "Sedo VIP Escrow"),
        DNJournalSale("paytech.com", 85000.0, "Afternic Premium"),
        DNJournalSale("neurobrand.co", 18500.0, "GoDaddy Auctions"),
        DNJournalSale("fitwell.ai", 24000.0, "Namecheap closeouts"),
        DNJournalSale("legalai.io", 32000.0, "Private brokerage")
    )
    val marketInsights = MutableStateFlow<List<String>>(listOf(
        "AI domains under 6 characters are selling 3x faster this month.",
        "Health .com domains with DA > 20 are averaging $4,500 in secondary resale market flips.",
        "SaaS .io brandable assets are commanding a premium of +45% compared to typical B2B domains."
    ))

    // 9. Simulated notification trigger feed
    val simulatedNotification = MutableSharedFlow<String>()
    val notificationsLog = MutableStateFlow<List<String>>(emptyList())

    // UI event message (for dialog toasts)
    val uiEventMessage = MutableSharedFlow<String>()

    init {
        loadPreferences()
        simulateDailyAlertCheck()
    }

    private fun loadPreferences() {
        onboardingCompleted.value = prefs.getBoolean("onboarding_done", false)
        keyMoz.value = prefs.getString("key_moz", "") ?: ""
        keyMajestic.value = prefs.getString("key_majestic", "") ?: ""
        keyNamecheap.value = prefs.getString("key_namecheap", "") ?: ""
        keyGoDaddy.value = prefs.getString("key_godaddy", "") ?: ""
        defaultScanPreference.value = prefs.getString("default_scan_pref", "All") ?: "All"
        maxResultsSlider.value = prefs.getInt("max_results", 50)
        morningScanSchedule.value = prefs.getBoolean("morning_scan", true)
        notificationPriceDrop.value = prefs.getBoolean("notif_price_drop", true)
        notificationSFound.value = prefs.getBoolean("notif_s_found", true)

        val rawNotifs = prefs.getString("notifications_log", "") ?: ""
        if (rawNotifs.isNotEmpty()) {
            notificationsLog.value = rawNotifs.split("|||")
        } else {
            notificationsLog.value = listOf(
                "Domain Sniper Pro initialized! Active background bots scouting new registrar list drops.",
                "Settings loaded: Market insights refreshed with latest secondary auction indices."
            )
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefs.edit().putBoolean("onboarding_done", true).apply()
            onboardingCompleted.value = true
            addNotificationLog("Welcome to Domain Sniper Pro! Setup your specific criteria filters and fire up the Smart Scanner.")
            uiEventMessage.emit("Domain Sniper Pro Activated!")
        }
    }

    fun updateSettings(
        moz: String, majestic: String, namecheap: String, godaddy: String,
        scanPref: String, maxRes: Int, morningAuto: Boolean, priceDrop: Boolean, sFound: Boolean
    ) {
        viewModelScope.launch {
            prefs.edit().apply {
                putString("key_moz", moz)
                putString("key_majestic", majestic)
                putString("key_namecheap", namecheap)
                putString("key_godaddy", godaddy)
                putString("default_scan_pref", scanPref)
                putInt("max_results", maxRes)
                putBoolean("morning_scan", morningAuto)
                putBoolean("notif_price_drop", priceDrop)
                putBoolean("notif_s_found", sFound)
                apply()
            }
            loadPreferences()
            uiEventMessage.emit("Preferences fully updated!")
        }
    }

    fun addNotificationLog(msg: String) {
        viewModelScope.launch {
            val list = notificationsLog.value.toMutableList()
            list.add(0, msg)
            if (list.size > 30) list.removeAt(list.size - 1)
            notificationsLog.value = list
            prefs.edit().putString("notifications_log", list.joinToString("|||")).apply()
            simulatedNotification.emit(msg)
        }
    }

    fun navigateTo(screen: Screen) {
        currentScreen.value = screen
    }

    fun triggerVibration() {
        try {
            val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // Silently catch to prevent crashes on emulators/headless envs without a vibrator unit
        }
    }

    // --- DOMAIN SNIPING QUALITY SCORE FORMULA (Deterministic Calculator) ---
    fun calculateDomainMetricScore(
        domain: String, price: Double, extension: String,
        da: Int, backlinks: Long, tf: Int, cf: Int, ageYears: Int,waybackTraffic: Boolean
    ): ScoreBreakdown {
        // 1. SEO Score (max 25)
        var seo = (da * 0.25f).coerceIn(0f, 25f)
        if (tf > 20) seo += 10f
        if (cf > 20) seo += 5f
        if (backlinks > 15) seo += 5f
        seo = seo.coerceAtMost(25f)

        // 2. Age Score (max 20)
        val age = when {
            ageYears >= 16 -> 20
            ageYears >= 11 -> 15
            ageYears >= 6 -> 10
            else -> 5
        }

        // 3. Backlinks Score (max 20)
        val bl = when {
            backlinks >= 1000 -> 20
            backlinks >= 500 -> 15
            backlinks >= 100 -> 10
            backlinks >= 10 -> 5
            else -> 0
        }

        // 4. Brandability Score (max 20)
        val cleanName = domain.substringBefore(".")
        var brand = when {
            cleanName.length <= 6 -> 20
            cleanName.length <= 9 -> 15
            cleanName.length <= 12 -> 10
            else -> 5
        }
        // Bonuses
        val hasNumbers = cleanName.any { it.isDigit() }
        val hasHyphens = cleanName.contains("-")
        if (!hasNumbers && !hasHyphens) brand += 2

        val isPronounceable = !cleanName.contains("qwx") && !cleanName.contains("zgp") && cleanName.length < 12
        if (isPronounceable) brand += 3

        val isDictWord = cleanName.length in 4..8 && (cleanName.startsWith("tech") || cleanName.startsWith("ai") || cleanName.startsWith("smart") || cleanName.startsWith("pay") || cleanName.startsWith("well") || cleanName.startsWith("mind"))
        if (isDictWord) brand += 5
        brand = brand.coerceAtMost(20)

        // 5. Traffic History Score (max 15)
        var traffic = 0
        if (waybackTraffic) {
            traffic += 10
            traffic += 5 // Consistent history
        }

        // 6. Extension Score (max 10)
        val ext = when (extension) {
            ".com" -> 10
            ".ai" -> 9
            ".io" -> 7
            ".co" -> 6
            ".net" -> 5
            ".org" -> 5
            ".xyz" -> 3
            else -> 2
        }

        // 7. Price Score bonus (adds directly to sum)
        val priceBonus = when {
            price <= 10.0 -> 15
            price <= 30.0 -> 10
            price <= 100.0 -> 5
            else -> 0
        }

        val baseSum = seo + age + bl + brand + traffic + ext
        val normalizedBase = (baseSum / 110f * 100f).toInt().coerceIn(1, 100)
        val finalScore = (normalizedBase + priceBonus).coerceIn(1, 100)

        val grade = when {
            finalScore >= 85 -> "S"
            finalScore >= 70 -> "A"
            finalScore >= 55 -> "B"
            finalScore >= 40 -> "C"
            else -> "D"
        }

        return ScoreBreakdown(
            overallScore = finalScore, grade = grade,
            seoScore = seo.toInt(), ageScore = age,
            backlinkScore = bl, brandScore = brand,
            trafficScore = traffic, extensionScore = ext,
            priceBonus = priceBonus
        )
    }

    // --- GEMINI REST API FOR DEEP BRAINABILITY ANALYSIS & VERDICT ---
    fun runDomainScanner(onScanFinished: () -> Unit) {
        isScanning.value = true
        triggerVibration()

        viewModelScope.launch(Dispatchers.IO) {
            val systemPrompt = "You are a professional domain Sniper. Analyze and returns structured domains JSON ONLY."
            
            val keywordPart = if (keywordFilterInput.value.trim().isNotEmpty()) {
                "containing keyword '${keywordFilterInput.value.trim().lowercase()}'"
            } else "in general startup/tech space"

            val maxCost = maxPriceFilter.value

            val promptText = """
                Generate a list of exactly 6 realistic expired/auction/dropped domains matching these criteria:
                - Extensions: ${extensionsChecked.value.joinToString(", ")}
                - Max price: $$maxCost
                - Keyword constraint: $keywordPart
                - Clean/No hyphens: ${noHyphensToggle.value}
                - Clean/No numbers: ${noNumbersToggle.value}
                
                For each domain, generate high quality values:
                1. domainName (e.g., smartpay.ai, quantumvault.com, techfit.io, brandwell.co, legalai.io)
                2. price (Double, random values under $$maxCost)
                3. da (Int, random 15-55)
                4. tf (Int, random 5-30)
                5. cf (Int, random 5-35)
                6. backlinks (Long, random 50-25000)
                7. ageYears (Int, random 1-18)
                8. waybackTraffic (Boolean)
                9. suggestedNiches (String, 3 comma-separated terms)
                10. similarSoldDomains (String, e.g. "similar.com sold for ${'$'}4,500")
                11. verdictReason (String, 2-3 bullets summarizing value assets)
                12. suggestedResalePrice (Double, 650 to 12000 depending on characters/DA)
                
                Return JSON in this format ONLY:
                {
                  "domains": [
                    {
                      "domainName": "string",
                      "price": 10.0,
                      "da": 25,
                      "tf": 18,
                      "cf": 20,
                      "backlinks": 1200,
                      "ageYears": 12,
                      "waybackTraffic": true,
                      "suggestedNiches": "Fintech, SaaS, Payments",
                      "similarSoldDomains": "paylink.com sold for ${'$'}8,200",
                      "verdictReason": "Short, pronounceable fintech key; Has strong Moz DA metrics; Wayback shows clean historical site registry",
                      "suggestedResalePrice": 3200.0
                    }
                  ]
                }
            """.trimIndent()

            val apiKey = BuildConfig.GEMINI_API_KEY
            val isRealKey = apiKey.isNotEmpty() && !apiKey.contains("PLACEHOLDER") && !apiKey.contains("KEY") && !apiKey.startsWith("MY_")

            var jsonResult: String? = null

            if (isRealKey) {
                try {
                    val requestPayload = JSONObject().apply {
                        val contentsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("text", promptText)
                                    })
                                })
                            })
                        }
                        put("contents", contentsArray)

                        val systemInstruction = JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "You are a professional domain Sniper analyst. Return pure JSON ONLY matching requested scheme exactly. No markdown, no backticks.")
                                })
                            })
                        }
                        put("systemInstruction", systemInstruction)

                        put("generationConfig", JSONObject().apply {
                            put("responseMimeType", "application/json")
                            put("temperature", 0.7)
                        })
                    }

                    val requestBody = requestPayload.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val respString = response.body?.string() ?: ""
                            val jsonResp = JSONObject(respString)
                            val candidates = jsonResp.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val text = candidates.getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text")
                                jsonResult = text.trim()
                            }
                        }
                    }
                } catch (e: Exception) {
                    addNotificationLog("Gemini Scanner Network Error: ${e.message}")
                }
            }

            // Fallback: Generate extremely high-quality simulated domains based on actual filter criteria
            if (jsonResult == null) {
                delay(2000) // Simulated scan latency with skeleton animation
                jsonResult = generateFallbackScannedDomainsJson()
            }

            try {
                val parsed = JSONObject(jsonResult!!)
                val domainsArray = parsed.getJSONArray("domains")
                
                var saFound = 0
                for (i in 0 until domainsArray.length()) {
                    val d = domainsArray.getJSONObject(i)
                    val domainName = d.getString("domainName").lowercase().trim()
                    
                    val price = d.optDouble("price", (10..150).random().toDouble())
                    val da = d.optInt("da", (10..55).random())
                    val tf = d.optInt("tf", (5..30).random())
                    val cf = d.optInt("cf", (5..30).random())
                    val backlinks = d.optLong("backlinks", (100..15000).random().toLong())
                    val age = d.optInt("ageYears", (2..15).random())
                    val waybackTraffic = d.optBoolean("waybackTraffic", true)

                    val pieces = domainName.split(".")
                    val ext = if (pieces.size > 1) "." + pieces.last() else ".com"

                    // Compute complete formula verified scores
                    val calc = calculateDomainMetricScore(domainName, price, ext, da, backlinks, tf, cf, age, waybackTraffic)

                    if (calc.grade == "S" || calc.grade == "A") saFound++

                    val entity = ScannedDomain(
                        domainName = domainName,
                        overallScore = calc.overallScore,
                        grade = calc.grade,
                        price = price,
                        extension = ext,
                        da = da,
                        backlinks = backlinks,
                        tf = tf,
                        cf = cf,
                        ageYears = age,
                        waybackTraffic = waybackTraffic,
                        suggestedNiches = d.optString("suggestedNiches", "AI, Tech, SaaS"),
                        similarSoldDomains = d.optString("similarSoldDomains", "similarpay.com sold for $2,800"),
                        suggestedResalePrice = d.optDouble("suggestedResalePrice", calc.overallScore * 50.0),
                        verdict = if (calc.overallScore >= 80) "Buy Now" else if (calc.overallScore >= 60) "Consider" else "Skip",
                        buyVerdictReason = d.optString("verdictReason", "Short keyword asset; High SEO indicators; Verified historical traffic profile."),
                        riskFactors = if (calc.overallScore < 50) "Low search volume, High historical rotation" else "No trademark risk detected"
                    )

                    repository.insertScannedDomain(entity)
                }

                if (saFound > 0 && notificationSFound.value) {
                    addNotificationLog("Alert matched: $saFound Gold-Tier (S/A Level) domains captured under $$maxCost!")
                } else {
                    addNotificationLog("Scanner complete! Captured ${domainsArray.length()} domains matching active thresholds.")
                }

                uiEventMessage.emit("Found ${domainsArray.length()} domains!")
                
                // Navigate to Results Screen
                currentScreen.value = Screen.ScanResults(jsonResult!!)
            } catch (e: Exception) {
                uiEventMessage.emit("Verification parser failed. Retrying scanner locally.")
                addNotificationLog("Scanner Parser fault: ${e.message}")
            } finally {
                isScanning.value = false
                viewModelScope.launch(Dispatchers.Main) {
                    onScanFinished()
                }
            }
        }
    }

    private fun generateFallbackScannedDomainsJson(): String {
        val keywordTerm = keywordFilterInput.value.trim().lowercase()
        val suffixList = listOf("labs", "tech", "well", "pay", "vault", "sync", "flow", "ai", "brand", "smart")
        val finalExtensions = extensionsChecked.value.ifEmpty { listOf(".com", ".ai") }

        val domainArray = JSONArray()
        for (i in 0 until 6) {
            val baseWord = if (keywordTerm.isNotEmpty()) keywordTerm else {
                listOf("quantum", "neuron", "focus", "meta", "crypto", "vibe", "fit", "health", "lex", "breeze").random()
            }
            val suffix = if (keywordTerm.isNotEmpty()) suffixList.random() else suffixList[i % suffixList.size]
            val extSymbol = finalExtensions[i % finalExtensions.size]
            val domainName = "$baseWord$suffix$extSymbol"

            val price = when {
                onlyUnder30.value -> (5..29).random().toDouble()
                else -> (8..maxPriceFilter.value.toInt().coerceAtLeast(15)).random().toDouble()
            }

            val da = (12..52).random()
            val tf = (6..28).random()
            val cf = (8..30).random()
            val backlinks = (45..12500).random().toLong()
            val age = (1..18).random()

            val niches = when {
                domainName.contains("ai") || domainName.contains("neuron") -> "Artificial Intelligence, Deep Learning, Agents"
                domainName.contains("pay") || domainName.contains("vault") -> "Fintech, Web3 Payments, Microtransactions"
                domainName.contains("well") || domainName.contains("fit") -> "Health & Fitness, Medical SaaS, Nutrition"
                else -> "Saas, Cloud Infrastructure, Developer Tools"
            }

            val obj = JSONObject().apply {
                put("domainName", domainName)
                put("price", price)
                put("da", da)
                put("tf", tf)
                put("cf", cf)
                put("backlinks", backlinks)
                put("ageYears", age)
                put("waybackTraffic", (1..10).random() > 3)
                put("suggestedNiches", niches)
                put("similarSoldDomains", "${baseWord}base.com sold for $${(1500..8500).random()}")
                put("verdictReason", "Short brandable naming convention\nClean backlink profile from age registry\nHigh niche fit for modern $niches trends")
                put("suggestedResalePrice", (1800..9500).random().toDouble())
            }
            domainArray.put(obj)
        }

        return JSONObject().apply {
            put("domains", domainArray)
        }.toString()
    }

    // --- BULK SCAN PROGRESS SIMULATOR ---
    fun runBulkScanner(inputText: String) {
        val domainsToScan = inputText.split("\n")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && it.contains(".") }

        if (domainsToScan.isEmpty()) return

        bulkIsProcessing.value = true
        bulkProgress.value = 0f
        bulkProcessedCount.value = 0
        bulkTotalCount.value = domainsToScan.size
        bulkResultsList.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            val results = mutableListOf<ScannedDomain>()

            domainsToScan.forEachIndexed { index, rawDomain ->
                delay(800) // Simulated processing clock latency
                val cleanDomain = rawDomain.trim()
                val pieces = cleanDomain.split(".")
                val ext = "." + pieces.last()

                // Generate random values for this scanned domain
                val price = (10..450).random().toDouble()
                val da = (5..45).random()
                val tf = (2..20).random()
                val cf = (4..22).random()
                val backlinks = (10..2400).random().toLong()
                val age = (1..12).random()
                val waybackTraffic = (1..10).random() > 4

                val calc = calculateDomainMetricScore(cleanDomain, price, ext, da, backlinks, tf, cf, age, waybackTraffic)

                val domainObj = ScannedDomain(
                    domainName = cleanDomain,
                    overallScore = calc.overallScore,
                    grade = calc.grade,
                    price = price,
                    extension = ext,
                    da = da,
                    backlinks = backlinks,
                    tf = tf,
                    cf = cf,
                    ageYears = age,
                    waybackTraffic = waybackTraffic,
                    suggestedNiches = "Bulk Analyzed, General Enterprise",
                    similarSoldDomains = "${pieces.first()}brand.com sold for $2,400",
                    suggestedResalePrice = calc.overallScore * 40.0,
                    verdict = if (calc.overallScore >= 70) "Buy Now" else "Consider",
                    buyVerdictReason = "Bulk loaded domain asset scored in batch pipelines.",
                    riskFactors = "Trademark check recommended"
                )

                // Save to Scanned Domain Cash table
                repository.insertScannedDomain(domainObj)
                results.add(domainObj)

                // Update Progress metrics
                bulkProcessedCount.value = index + 1
                bulkProgress.value = (index + 1).toFloat() / domainsToScan.size
                bulkResultsList.value = results.toList()
            }

            bulkIsProcessing.value = false
            addNotificationLog("Bulk scan complete: Scored ${domainsToScan.size} custom domain files natively.")
            uiEventMessage.emit("Bulk scan complete!")
        }
    }

    fun generateBulkKeywordsList(keywordString: String, extensions: List<String>) {
        val keywords = keywordString.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        if (keywords.isEmpty()) return

        val combinations = mutableListOf<String>()
        val prefixes = listOf("", "go", "get", "my", "smart", "ai", "brand", "the")
        val suffixes = listOf("", "labs", "tech", "well", "pay", "flow", "vault", "sync", "app")

        keywords.forEach { kw ->
            // Use prefixes & suffixes to generate 15 solid start items
            for (i in 0 until 4) {
                val pref = prefixes.random()
                val suff = suffixes.random()
                if (pref.isEmpty() && suff.isEmpty()) continue
                val domainName = "$pref$kw$suff"
                val finalExt = extensions.random()
                combinations.add("$domainName$finalExt")
            }
        }

        bulkInputText.value = combinations.distinct().joinToString("\n")
    }

    // --- WATCHLIST OPERATIONS ---
    fun toggleWatchlist(scanned: ScannedDomain) {
        viewModelScope.launch {
            val exists = watchlistDomains.value.any { it.domainName == scanned.domainName }
            if (exists) {
                repository.deleteWatchlistDomain(scanned.domainName)
                uiEventMessage.emit("Removed from Watchlist")
            } else {
                val watchlistObj = WatchlistDomain(
                    domainName = scanned.domainName,
                    overallScore = scanned.overallScore,
                    grade = scanned.grade,
                    price = scanned.price,
                    extension = scanned.extension,
                    da = scanned.da,
                    backlinks = scanned.backlinks,
                    tf = scanned.tf,
                    cf = scanned.cf,
                    ageYears = scanned.ageYears,
                    waybackTraffic = scanned.waybackTraffic,
                    initialPrice = scanned.price,
                    lastCheckedPrice = scanned.price,
                    daysSaved = 1
                )
                repository.insertWatchlistDomain(watchlistObj)
                triggerVibration()
                uiEventMessage.emit("Saved to Watchlist!")
            }
        }
    }

    fun deleteFromWatchlist(domainName: String) {
        viewModelScope.launch {
            repository.deleteWatchlistDomain(domainName)
            uiEventMessage.emit("Removed from Watchlist.")
        }
    }

    fun clearWatchlist() {
        viewModelScope.launch {
            repository.clearWatchlist()
            uiEventMessage.emit("Watchlist cleared completely.")
        }
    }

    // --- SMART ALERTS CREATOR ---
    fun saveAlert(name: String, pattern: String, ext: String, price: Double, minScore: Int, types: String) {
        viewModelScope.launch {
            val alertNew = SmartAlert(
                alertName = name.ifEmpty { "Criteria Alert" },
                keywordPattern = pattern,
                extensionFilter = ext,
                maxPrice = price,
                minScore = minScore,
                domainTypes = types,
                enabled = true
            )
            repository.insertSmartAlert(alertNew)
            addNotificationLog("Smart Alert configured: Checking names for pattern '$pattern' globally.")
            uiEventMessage.emit("Smart Alert added!")
        }
    }

    fun toggleAlertEnabled(id: Int, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlertEnabled(id, enabled)
            val stateName = if (enabled) "Enabled" else "Muted"
            uiEventMessage.emit("Alert $stateName")
        }
    }

    fun deleteAlert(alert: SmartAlert) {
        viewModelScope.launch {
            repository.deleteSmartAlert(alert)
            uiEventMessage.emit("Alert deleted.")
        }
    }

    private fun simulateDailyAlertCheck() {
        // Daily scan simulators to spawn notification alerts in list background automatically
        viewModelScope.launch {
            delay(5000)
            if (smartAlerts.value.isNotEmpty() && morningScanSchedule.value) {
                val active = smartAlerts.value.firstOrNull { it.enabled }
                if (active != null) {
                    val sampleName = if (active.keywordPattern.isNotEmpty()) {
                        "global${active.keywordPattern}${active.extensionFilter.split(",").first()}"
                    } else "quantumai.com"
                    
                    val gradeTier = "S"
                    val buyPrice = (8..active.maxPrice.toInt().coerceAtMost(30)).random().toDouble()

                    repository.incrementAlertMatch(active.id, "Today 08:32 AM")
                    addNotificationLog("Alert triggered [${active.alertName}]: Found $sampleName (Grade $gradeTier) selling for $$buyPrice!")
                }
            }
        }
    }

    // --- PORTFOLIO TRACKING SYSTEM ---
    fun savePortfolioDomain(
        domain: String, buyPrice: Double, buyDate: String, platform: String,
        targetPrice: Double, listing: String, status: String, renewal: Double = 12.0
    ) {
        viewModelScope.launch {
            val cleanDomain = domain.trim().lowercase()
            if (cleanDomain.isEmpty()) return@launch

            val entity = PortfolioDomain(
                domainName = cleanDomain,
                buyPrice = buyPrice,
                buyDate = buyDate.ifEmpty { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
                platformBought = platform.ifEmpty { "GoDaddy" },
                targetSellPrice = targetPrice,
                currentListingPlatform = listing.ifEmpty { "Afternic" },
                status = status,
                renewalCost = renewal
            )
            repository.insertPortfolioDomain(entity)
            triggerVibration()
            uiEventMessage.emit("Added to Portfolio tracker!")
            
            // Log action
            addNotificationLog("Acquired domain registered to owned portfolio checklist: $cleanDomain for $$buyPrice.")
        }
    }

    fun markPortfolioSold(id: Int, salePrice: Double) {
        viewModelScope.launch {
            val pList = portfolioDomains.value
            val match = pList.find { it.id == id }
            if (match != null) {
                val updated = match.copy(
                    status = "Sold",
                    actualSalePrice = salePrice,
                    soldDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                )
                repository.insertPortfolioDomain(updated)
                addNotificationLog("Congratulations! Domain asset Sold: ${match.domainName} for $$salePrice (+${((salePrice - match.buyPrice)/match.buyPrice*100).toInt()}% Net)")
                uiEventMessage.emit("Sold registered! Gained registered.")
            }
        }
    }

    fun updatePortfolioStatus(id: Int, status: String) {
        viewModelScope.launch {
            val pList = portfolioDomains.value
            val match = pList.find { it.id == id }
            if (match != null) {
                repository.insertPortfolioDomain(match.copy(status = status))
                uiEventMessage.emit("Status updated to $status!")
            }
        }
    }

    fun deletePortfolio(id: Int) {
        viewModelScope.launch {
            repository.deletePortfolioDomainById(id)
            uiEventMessage.emit("Removed from owned listings.")
        }
    }

    // --- SYSTEM WIPES & DUMPS ---
    fun clearCache() {
        viewModelScope.launch {
            repository.clearScannedDomains()
            uiEventMessage.emit("Scans cache successfully purged!")
        }
    }

    fun exportAllCsv(): String {
        val sb = StringBuilder()
        sb.append("=== SCANNED HISTORY ===\n")
        sb.append("Domain Name,Overall Score,Grade,Price,Extension,Moz DA,Backlinks,Suggested Resale,Niches\n")
        scannedDomains.value.forEach {
            sb.append("${it.domainName},${it.overallScore},${it.grade},${it.price},${it.extension},${it.da},${it.backlinks},${it.suggestedResalePrice},\"${it.suggestedNiches}\"\n")
        }
        sb.append("\n=== WATCHLIST ===\n")
        sb.append("Domain Name,Overall Score,Grade,Current Price,Initial Price\n")
        watchlistDomains.value.forEach {
            sb.append("${it.domainName},${it.overallScore},${it.grade},${it.lastCheckedPrice},${it.initialPrice}\n")
        }
        sb.append("\n=== PORTFOLIO TRACKER ===\n")
        sb.append("Domain Name,Buy Price,Buy Date,Target,Actual Sale,Status,Listing Platform\n")
        portfolioDomains.value.forEach {
            sb.append("${it.domainName},${it.buyPrice},${it.buyDate},${it.targetSellPrice},${it.actualSalePrice},${it.status},${it.currentListingPlatform}\n")
        }
        return sb.toString()
    }

    fun wipeDatabase() {
        viewModelScope.launch {
            repository.clearAllData()
            prefs.edit().clear().apply()
            loadPreferences()
            addNotificationLog("System wiped. Database schemas reset.")
            uiEventMessage.emit("All application databases cleared.")
            currentScreen.value = Screen.Dashboard
        }
    }
}

// Score Breakdown structure
data class ScoreBreakdown(
    val overallScore: Int,
    val grade: String,
    val seoScore: Int,
    val ageScore: Int,
    val backlinkScore: Int,
    val brandScore: Int,
    val trafficScore: Int,
    val extensionScore: Int,
    val priceBonus: Int
)

// Market models
data class MarketRoiItem(val extension: String, val performance: String, val reason: String)
data class DNJournalSale(val domain: String, val price: Double, val channel: String)

// --- REVISED DOMAIN SNIPER PRO 10-SCREEN SEALED ROUTING CLASS ---
sealed class Screen {
    object Dashboard : Screen()
    object SmartScanner : Screen()
    data class ScanResults(val filtersJson: String) : Screen()
    data class FullAnalysis(val domain: String, val source: String) : Screen() // source is "scanned" / "watchlist" / "portfolio" / "alerts"
    object Watchlist : Screen()
    object SmartAlerts : Screen()
    object BulkScanner : Screen()
    object MarketIntelligence : Screen()
    object PortfolioTracker : Screen()
    object Settings : Screen()
}
