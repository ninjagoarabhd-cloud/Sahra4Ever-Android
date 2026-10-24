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
    private const val REWARDED_PLACEMENT_ID = "Rewarded_Android"

    // ✅ تم التعديل هنا: تفعيل وضع الاختبار لتظهر الإعلانات التجريبية فوراً
    private const val TEST_MODE = true 

    private const val SHOW_TIMEOUT_MS = 8_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isInitializing = false

    private var isLoading = false
    private var isLoaded = false

    private var isRewardedLoading = false
    private var isRewardedLoaded = false

    @Volatile
    private var isShowing = false

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

    fun showInterstitial(activity: Activity?, onFinished: (shown: Boolean) -> Unit) {
        val finishedGuard = AtomicBoolean(false)
        val timeoutToken = Any()
        fun finishOnce(shown: Boolean) {
            if (finishedGuard.compareAndSet(false, true)) {
                mainHandler.removeCallbacksAndMessages(timeoutToken)
                onFinished(shown)
            }
        }

        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            if (activity != null) initialize(activity)
            finishOnce(false)
            return
        }

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

    fun showRewardedAd(activity: Activity?, onFinished: (success: Boolean, failedToLoad: Boolean) -> Unit) {
        val finishedGuard = AtomicBoolean(false)
        val timeoutToken = Any()
        fun finishOnce(success: Boolean, failedToLoad: Boolean) {
            if (finishedGuard.compareAndSet(false, true)) {
                mainHandler.removeCallbacksAndMessages(timeoutToken)
                onFinished(success, failedToLoad)
            }
        }

        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            if (activity != null) initialize(activity)
            finishOnce(false, true)
            return
        }

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

    fun reset() {
        mainHandler.removeCallbacksAndMessages(null)
        isShowing = false
    }
}