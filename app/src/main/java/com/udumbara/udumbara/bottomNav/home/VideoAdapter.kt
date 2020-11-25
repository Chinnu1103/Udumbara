package com.udumbara.udumbara.bottomNav.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.udumbara.udumbara.R
import kotlinx.android.synthetic.main.rv_feed_video_view.view.*

class VideoAdapter(val context: Context): RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {
    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val image = view.iv_template
        val title = view.tv_title
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val videoView = LayoutInflater.from(parent.context)
            .inflate(R.layout.rv_feed_video_view, parent, false)
        return VideoViewHolder(videoView)
    }

    override fun getItemCount(): Int {
        return 15
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {

    }
}