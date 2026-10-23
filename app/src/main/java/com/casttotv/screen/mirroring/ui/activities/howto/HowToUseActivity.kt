package com.casttotv.screen.mirroring.ui.activities.howto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivityHowToYouBinding
import com.casttotv.screen.mirroring.databinding.LayoutHtyBinding

/** Port of `HowToUseActivity` (`TYPE_HTY`: 1 = connect, 2 = cast, 3 = mirror). */
class HowToUseActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHowToYouBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val type = intent.getIntExtra(EXTRA_TYPE, 1)
        with(binding.include5) {
            castConnect.visibility = View.GONE
            castPremium.visibility = View.GONE
            imvBack.setOnClickListener { finish() }
            tvTitle.setText(
                when (type) {
                    1 -> R.string.connecting
                    2 -> R.string.how_to_cast
                    else -> R.string.text_screen_mirror
                },
            )
        }
        binding.btnGotIt.setOnClickListener { finish() }
        val steps = listOf(R.string.guide_connect_same_wifi to R.drawable.imv_introdution) + when (type) {
            1 -> listOf(
                R.string.hty_connect2 to R.drawable.img_intro_connect_1,
                R.string.hty_connect3 to R.drawable.img_intro_connect_2,
                R.string.hty_connect4 to R.drawable.img_intro_connect_3,
            )
            2 -> listOf(
                R.string.hty_remote2 to R.drawable.img_intro_connect_2,
                R.string.hty_remote3 to R.drawable.img_intro_cast_1,
                R.string.hty_remote4 to R.drawable.img_intro_cast_2,
            )
            else -> listOf(
                R.string.hty_mirror2 to R.drawable.img_intro_mirror_2,
                R.string.hty_mirror3 to R.drawable.img_intro_mirror_3,
                R.string.hty_mirror4 to R.drawable.img_intro_mirror_4,
            )
        }
        binding.rcvList.layoutManager = LinearLayoutManager(this)
        binding.rcvList.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = steps.size
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(LayoutHtyBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val row = LayoutHtyBinding.bind(holder.itemView)
                row.tvHty.setText(steps[position].first)
                row.imvHty.setImageResource(steps[position].second)
            }
        }
    }

    companion object {
        const val EXTRA_TYPE = "TYPE_HTY"
    }
}
