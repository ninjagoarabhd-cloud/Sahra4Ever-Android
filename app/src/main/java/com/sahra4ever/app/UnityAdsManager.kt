package com.sahra4ever.app

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import java.util.concurrent.atomic.AtomicBoolean

object UnityAdsManager {
    private const val TAG = "UnityAdsManager"

    private const val GAME_ID = "800408926"
    private const val INTERSTITIAL_PLACEMENT_ID = "BP_Interstitial_Android"
    // ✅ (جديد) Placement مخصص لإعلانات المكافأة (Rewarded) — يُستخدم حصرياً لفتح السيرفر الأول VIP
    private const val REWARDED_PLACEMENT_ID = "Rewarded_Android"

    // اجعلها true فقط أثناء الاختبار
    private const val TEST_MODE = false

    // ✅ مهلة أمان: إذا لم يستجب Unity SDK خلال هذه المدة (مشكلة معروفة أحياناً)
    // نُجبر إنهاء العملية بدل ترك المستخدم أو النافذة عالقَين للأبد.
    private const val SHOW_TIMEOUT_MS = 8_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private var isInitializing = false

    // --- حالة الإعلان البيني (Interstitial) ---
    private var isLoading = false
    private var isLoaded = false

    // --- حالة إعلان المكافأة (Rewarded) ---
    private var isRewardedLoading = false
    private var isRewardedLoaded = false

    // ✅ حالة عرض مشتركة بين النوعين لمنع تداخل الإعلانات (لا يمكن عرض إعلانين في آن واحد مهما كان نوعهما)
    @Volatile
    private var isShowing = false

    /** تهيئة الـ SDK (آمنة للاستدعاء أكثر من مرة). */
    fun initialize(context: Context) {
        if (UnityAds.isInitialized) {
            loadAd()
            loadRewardedAd()
            return
        }
        if (isInitializing) return
        isInitializing = true

        UnityAds.initialize(
            context.applicationContext,
            GAME_ID,
            TEST_MODE,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.d(TAG, "Unity Ads initialized")
                    isInitializing = false
                    loadAd()
                    loadRewardedAd()
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError?,
                    message: String?
                ) {
                    Log.w(TAG, "Unity Ads init failed: $error - $message")
                    isInitializing = false
                }
            }
        )
    }

    /** تحميل إعلان بيني مسبقاً ليكون جاهزاً للعرض. */
    fun loadAd() {
        if (!UnityAds.isInitialized || isLoading || isLoaded) return
        isLoading = true

        UnityAds.load(
            INTERSTITIAL_PLACEMENT_ID,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    Log.d(TAG, "Interstitial ad loaded: $placementId")
                    isLoading = false
                    isLoaded = true
                }

                override fun onUnityAdsFailedToLoad(
                    placementId: String?,
                    error: UnityAds.UnityAdsLoadError?,
                    message: String?
                ) {
                    Log.w(TAG, "Interstitial ad failed to load: $error - $message")
                    isLoading = false
                    isLoaded = false
                }
            }
        )
    }

    /** ✅ (جديد) تحميل إعلان المكافأة مسبقاً ليكون جاهزاً للعرض. */
    fun loadRewardedAd() {
        if (!UnityAds.isInitialized || isRewardedLoading || isRewardedLoaded) return
        isRewardedLoading = true

        UnityAds.load(
            REWARDED_PLACEMENT_ID,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    Log.d(TAG, "Rewarded ad loaded: $placementId")
                    isRewardedLoading = false
                    isRewardedLoaded = true
                }

                override fun onUnityAdsFailedToLoad(
                    placementId: String?,
                    error: UnityAds.UnityAdsLoadError?,
                    message: String?
                ) {
                    Log.w(TAG, "Rewarded ad failed to load: $error - $message")
                    isRewardedLoading = false
                    isRewardedLoaded = false
                }
            }
        )
    }

    /**
     * عرض الإعلان البيني.
     * onFinished(true)  => الإعلان عُرض وتم إغلاقه.
     * onFinished(false) => لا يوجد إعلان / فشل العرض / الـ Activity غير صالحة (افتح السيرفر مباشرة).
     * يُستدعى onFinished مرة واحدة فقط دائماً مهما حصل (بما في ذلك حالات الفشل الصامت).
     */
    fun showInterstitial(activity: Activity?, onFinished: (shown: Boolean) -> Unit) {
        val finishedGuard = AtomicBoolean(false)
        val timeoutToken = Any()
        fun finishOnce(shown: Boolean) {
            if (finishedGuard.compareAndSet(false, true)) {
                mainHandler.removeCallbacksAndMessages(timeoutToken)
                onFinished(shown)
            }
        }

        // ✅ تحقق أن الـ Activity ما زالت حيّة قبل محاولة فتح نافذة الإعلان.
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            if (activity != null) initialize(activity)
            finishOnce(false)
            return
        }

        // ✅ منع تداخل الإعلانات: لا نعرض إعلاناً بينياً إذا كان أي إعلان آخر (بيني أو مكافأة) معروضاً حالياً
        if (!UnityAds.isInitialized || !isLoaded || isShowing) {
            initialize(activity)
            loadAd()
            finishOnce(false)
            return
        }

        isShowing = true
        isLoaded = false

        mainHandler.postAtTime({
            if (finishedGuard.compareAndSet(false, true)) {
                Log.w(TAG, "Interstitial show timed out - forcing fallback")
                isShowing = false
                loadAd()
                onFinished(false)
            }
        }, timeoutToken, SystemClock.uptimeMillis() + SHOW_TIMEOUT_MS)

        try {
            UnityAds.show(
                activity,
                INTERSTITIAL_PLACEMENT_ID,
                UnityAdsShowOptions(),
                object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(
                        placementId: String?,
                        error: UnityAds.UnityAdsShowError?,
                        message: String?
                    ) {
                        Log.w(TAG, "Interstitial show failed: $error - $message")
                        isShowing = false
                        loadAd()
                        finishOnce(false)
                    }

                    override fun onUnityAdsShowStart(placementId: String?) {}

                    override fun onUnityAdsShowClick(placementId: String?) {}

                    override fun onUnityAdsShowComplete(
                        placementId: String?,
                        state: UnityAds.UnityAdsShowCompletionState?
                    ) {
                        isShowing = false
                        loadAd()
                        finishOnce(true)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception while showing interstitial ad", e)
            isShowing = false
            loadAd()
            finishOnce(false)
        }
    }

    /**
     * ✅ (جديد) عرض إعلان المكافأة (Rewarded) — مخصص لفتح السيرفر الأول VIP.
     * onFinished(success, failedToLoad):
     *   - success = true   => المستخدم شاهد الإعلان بالكامل (استحق المكافأة).
     *   - success = false, failedToLoad = false => المستخدم أغلق الإعلان مبكراً (لم يستحق المكافأة).
     *   - failedToLoad = true => لا يوجد إعلان جاهز / فشل التحميل أو العرض / Activity غير صالحة
     *                             (في هذه الحالة success تكون دائماً false، والمستدعي يقرر فتح السيرفر مجاناً).
     * يُستدعى onFinished مرة واحدة فقط دائماً مهما حصل.
     */
    fun showRewardedAd(activity: Activity?, onFinished: (success: Boolean, failedToLoad: Boolean) -> Unit) {
        val finishedGuard = AtomicBoolean(false)
        val timeoutToken = Any()
        fun finishOnce(success: Boolean, failedToLoad: Boolean) {
            if (finishedGuard.compareAndSet(false, true)) {
                mainHandler.removeCallbacksAndMessages(timeoutToken)
                onFinished(success, failedToLoad)
            }
        }

        // ✅ تحقق أن الـ Activity ما زالت حيّة قبل محاولة فتح نافذة الإعلان
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            if (activity != null) initialize(activity)
            finishOnce(false, true)
            return
        }

        // ✅ منع تداخل الإعلانات: لا نعرض إعلان مكافأة إذا كان أي إعلان آخر معروضاً حالياً،
        // وإن لم يكن الإعلان جاهزاً (لم يُحمَّل بعد / فشل التحميل) نعتبرها "فشل تحميل" فوراً
        // كي تفتح الشاشة المستدعية السيرفر VIP مجاناً دون تعليق المستخدم.
        if (!UnityAds.isInitialized || !isRewardedLoaded || isShowing) {
            initialize(activity)
            loadRewardedAd()
            finishOnce(false, true)
            return
        }

        isShowing = true
        isRewardedLoaded = false

        mainHandler.postAtTime({
            if (finishedGuard.compareAndSet(false, true)) {
                Log.w(TAG, "Rewarded ad show timed out - forcing fallback")
                isShowing = false
                loadRewardedAd()
                onFinished(false, true)
            }
        }, timeoutToken, SystemClock.uptimeMillis() + SHOW_TIMEOUT_MS)

        try {
            UnityAds.show(
                activity,
                REWARDED_PLACEMENT_ID,
                UnityAdsShowOptions(),
                object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(
                        placementId: String?,
                        error: UnityAds.UnityAdsShowError?,
                        message: String?
                    ) {
                        Log.w(TAG, "Rewarded show failed: $error - $message")
                        isShowing = false
                        loadRewardedAd()
                        finishOnce(false, true)
                    }

                    override fun onUnityAdsShowStart(placementId: String?) {}

                    override fun onUnityAdsShowClick(placementId: String?) {}

                    override fun onUnityAdsShowComplete(
                        placementId: String?,
                        state: UnityAds.UnityAdsShowCompletionState?
                    ) {
                        isShowing = false
                        loadRewardedAd()
                        // ✅ الإعلان يُعتبر "مُكافأً" فقط إذا اكتمل بالكامل (COMPLETED)؛
                        // إذا أغلقه المستخدم مبكراً تكون الحالة SKIPPED => لا مكافأة.
                        val earned = state == UnityAds.UnityAdsShowCompletionState.COMPLETED
                        finishOnce(earned, false)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception while showing rewarded ad", e)
            isShowing = false
            loadRewardedAd()
            finishOnce(false, true)
        }
    }

    /** يُستدعى عند تدمير النشاط الرئيسي (مثلاً من onDestroy في MainActivity) لتفادي أي تسريب حالة. */
    fun reset() {
        mainHandler.removeCallbacksAndMessages(null)
        isShowing = false
    }
}