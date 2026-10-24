package com.casttotv.screen.mirroring.ui.activities.function.screen_mirror

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivityScreenMirroringBinding
import com.casttotv.screen.mirroring.databinding.FragmentFaqBinding
import com.casttotv.screen.mirroring.databinding.FragmentMirrorDirectBinding
import com.casttotv.screen.mirroring.databinding.FragmentMirrorSmartBinding
import com.casttotv.screen.mirroring.databinding.FragmentMirrorWebBinding
import com.casttotv.screen.mirroring.databinding.ItemFaqBinding
import com.casttotv.screen.mirroring.mirror.MirrorService
import com.casttotv.screen.mirroring.mirror.MirrorTargets
import com.casttotv.screen.mirroring.tv.DeviceType
import com.casttotv.screen.mirroring.tv.RokuEcp
import com.casttotv.screen.mirroring.tv.TvConnect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Port of `ScreenMirroringActivity`: pick Smart TV (system cast), Direct (to connected TV) or Web mirroring. */
class ScreenMirroringActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityScreenMirroringBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindMediaHeader(binding.header, getString(R.string.screen_mirroring))
        binding.llMirrorTV.setOnClickListener { show(MirrorSmartFragment()) }
        binding.llMirrorDirect.setOnClickListener { show(MirrorDirectFragment()) }
        binding.llMirrorWeb.setOnClickListener { show(MirrorWebFragment()) }
        binding.txtFaq.setOnClickListener { show(FaqFragment()) }
    }

    /** `p(fragment, tag)`: full-screen pages stacked over the menu. */
    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .add(android.R.id.content, fragment)
            .addToBackStack(fragment.javaClass.simpleName)
            .commit()
    }
}

/** `ry3`: instructions + button opening the system Wireless display / Cast settings. */
class MirrorSmartFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentMirrorSmartBinding.inflate(inflater, container, false)
        with(binding.layoutHeader) {
            tvTitle.setText(R.string.smart_tv)
            castConnect.visibility = View.GONE
            castPremium.visibility = View.GONE
            imvBack.setOnClickListener { parentFragmentManager.popBackStack() }
        }
        binding.root.isClickable = true
        binding.btnStartScreenMirroring.setOnClickListener { openCastSettings() }
        return binding.root
    }

    private fun openCastSettings() {
        val opened = listOf("android.settings.WIFI_DISPLAY_SETTINGS", "android.settings.CAST_SETTINGS")
            .any { action -> runCatching { startActivity(Intent(action)) }.isSuccess }
        if (!opened) Toast.makeText(requireContext(), "Device not supported", Toast.LENGTH_LONG).show()
    }
}

/** Shared screen-capture permission + stream start for the Direct and Web pages. */
abstract class StreamingFragment : Fragment() {
    private var onStreaming: ((MirrorService.State.Streaming) -> Unit)? = null

    private val capture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode != android.app.Activity.RESULT_OK || data == null) return@registerForActivityResult
        MirrorService.start(requireContext(), result.resultCode, data)
        awaitStream()
    }

    /** Starts (or reuses) the MJPEG stream, then runs [action] with it. */
    protected fun withStream(action: (MirrorService.State.Streaming) -> Unit) {
        onStreaming = action
        if (MirrorService.state.value is MirrorService.State.Streaming) awaitStream()
        else runCatching { capture.launch(MirrorService.captureIntent(requireContext())) }
            .onFailure { Toast.makeText(requireContext(), R.string.error_server, Toast.LENGTH_SHORT).show() }
    }

    private fun awaitStream() {
        viewLifecycleOwner.lifecycleScope.launch {
            val stream = MirrorService.state.filterIsInstance<MirrorService.State.Streaming>().first()
            onStreaming?.invoke(stream)
        }
    }
}

/** `my3`: mirror to the connected TV. */
class MirrorDirectFragment : StreamingFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentMirrorDirectBinding.inflate(inflater, container, false)
        binding.root.isClickable = true
        binding.imvBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnStart.setOnClickListener {
            (requireActivity() as BaseActivity).withConnection(::start)
        }
        return binding.root
    }

    /** `l()`: Roku needs the app's channel; it is launched before the stream is sent. */
    private fun start() {
        if (TvConnect.typeOf(TvConnect.device ?: return) == DeviceType.ROKU) {
            val host = activity as? com.casttotv.screen.mirroring.base.BaseActivity ?: return
            if (com.casttotv.screen.mirroring.ui.activities.channel_roku.InstallChannelRokuActivity.checkRoku(host)) return
        }
        withStream(MirrorTargets::showOnTv)
    }
}

/** `dz3`: mirror into any browser through bcastmirror.com (scan the QR code shown on that page). */
class MirrorWebFragment : StreamingFragment() {
    private var binding: FragmentMirrorWebBinding? = null

    private val scan = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val key = result.data?.getStringExtra(QrScanActivity.EXTRA_RESULT)
        if (result.resultCode != android.app.Activity.RESULT_OK) return@registerForActivityResult
        if (key == null) {
            Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        withStream { stream ->
            showStatus(R.string.connecting, connecting = true)
            MirrorTargets.showOnWebPage(key, stream.address) { ok ->
                activity?.runOnUiThread { if (ok) showConnected() else showFailed() }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentMirrorWebBinding.inflate(inflater, container, false)
        this.binding = binding
        binding.root.isClickable = true
        binding.imvBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.llCopy.setOnClickListener {
            requireContext().getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("link", binding.tvLink.text))
            Toast.makeText(requireContext(), R.string.copy, Toast.LENGTH_SHORT).show()
        }
        binding.llShare.setOnClickListener {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "https://${binding.tvLink.text}"), null))
        }
        binding.btnStartScreenMirroring.setOnClickListener {
            scan.launch(Intent(requireContext(), QrScanActivity::class.java))
        }
        return binding.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun showStatus(text: Int, connecting: Boolean) = binding?.run {
        llConnected.isVisible = true
        lottieConnecting.isVisible = connecting
        lottieConnected.isVisible = false
        lottieConnectFail.isVisible = false
        tvStatusConnect.setText(text)
    }

    private fun showConnected() = binding?.run {
        lottieConnecting.isVisible = false
        lottieConnected.isVisible = true
        tvStatusConnect.setText(R.string.connected_successfully)
        lottieConnected.playAnimation()
        root.postDelayed({ binding?.llConnected?.isVisible = false }, 2000)
    }

    private fun showFailed() = binding?.run {
        lottieConnecting.isVisible = false
        lottieConnectFail.isVisible = true
        tvStatusConnect.setText(R.string.text_connected_fail)
        lottieConnectFail.playAnimation()
        root.postDelayed({ binding?.llConnected?.isVisible = false }, 2000)
    }
}

/** `gz1`: nine expandable questions. */
class FaqFragment : Fragment() {
    private data class Faq(val title: String, val detail: String, var expanded: Boolean = false)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentFaqBinding.inflate(inflater, container, false)
        binding.root.isClickable = true
        binding.imgBack.setOnClickListener { parentFragmentManager.popBackStack() }
        val res = resources
        val items = (1..9).map { i ->
            Faq(
                res.getString(res.getIdentifier("text_faq_title_$i", "string", requireContext().packageName)),
                res.getString(res.getIdentifier("text_faq_detail_$i", "string", requireContext().packageName)),
            )
        }
        binding.rcvFaq.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvFaq.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = items.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(ItemFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}

            @SuppressLint("NotifyDataSetChanged")
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val item = items[position]
                val row = ItemFaqBinding.bind(holder.itemView)
                row.txtTitle.text = item.title
                row.txtDetail.text = item.detail
                row.llDetail.isVisible = item.expanded
                row.imgArrow.setImageResource(if (item.expanded) R.drawable.ic_arrow_top else R.drawable.ic_arrow_bottom)
                holder.itemView.setOnClickListener {
                    item.expanded = !item.expanded
                    notifyItemChanged(holder.bindingAdapterPosition)
                }
            }
        }
        return binding.root
    }
}
