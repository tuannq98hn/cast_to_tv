package com.casttotv.screen.mirroring.ui.activities.function.remote

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.databinding.LayoutNumberBinding
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.ui.activities.function.connect.SearchTVActivity
import kotlinx.coroutines.launch
import java.util.Locale

/** Common behaviour of the brand remotes (`ay` base + per-fragment boilerplate in the original). */
abstract class BaseRemoteFragment : Fragment() {

    private var onSpeech: ((String) -> Unit)? = null

    private val speech = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == android.app.Activity.RESULT_OK && text != null) onSpeech?.invoke(text)
    }

    /** Called when the bottom tab switches between remote, touchpad and number pad. */
    protected abstract fun showMode(mode: RemoteActivity.Mode)

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                (requireActivity() as RemoteActivity).mode.collect(::showMode)
            }
        }
    }

    /** Runs [action] when connected; otherwise opens the TV search like every original remote button. */
    protected fun connected(action: () -> Unit) {
        if (!TvConnect.isConnected) {
            SearchTVActivity.open(requireActivity())
            return
        }
        vibrate()
        runCatching(action)
    }

    /** `ft4.G` / `ViewUtils.provideHapticFeedback`: 20 ms buzz. */
    protected fun vibrate() {
        if (!com.casttotv.screen.mirroring.data.AppPrefs.hapticFeedback) return
        val vibrator = requireContext().getSystemService(Vibrator::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vibrator.vibrate(20)
    }

    /** Speech prompt (request 68); [onText] gets the first recognition result. */
    protected fun listen(onText: (String) -> Unit) {
        onSpeech = onText
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt))
        try {
            speech.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.speech_not_supported, Toast.LENGTH_SHORT).show()
        }
    }

    protected fun bindNumbers(pad: LayoutNumberBinding, onDigit: (Int) -> Unit) = with(pad) {
        listOf(rltZero, rltOne, rltTwo, rltThree, rltFour, rltFive, rltSix, rltSeven, rltEight, rltNine)
            .forEachIndexed { digit, view -> view.setOnClickListener { connected { onDigit(digit) } } }
    }
}
