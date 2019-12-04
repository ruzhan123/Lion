package com.ruzhan.movie.detail.adapter.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.lion.font.FontHelper
import com.lion.imageloader.glide.ImageLoader
import com.ruzhan.movie.listener.OnItemClickListener
import com.ruzhan.movie.model.Video
import com.ruzhan.movie.utils.ViewUtils
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.lion_item_movie_detail_video.*

class MovieDetailVideoHolder(itemView: View, listener: OnItemClickListener<Video>?) :
        RecyclerView.ViewHolder(itemView), LayoutContainer {

    override val containerView: View?
        get() = itemView

    private lateinit var video: Video

    init {
        val lightFontTypeface = FontHelper.get().lightFontTypeface
        titleTv.typeface = lightFontTypeface
        if (listener != null) {
            rootCardView.setOnClickListener { listener.onItemClick(adapterPosition, video, it) }
        }
    }

    fun bind(bean: Video) {
        video = bean
        titleTv.text = bean.title
        ImageLoader.get().load(imageIv, bean.image,
                ViewUtils.getPlaceholder(itemView.context, adapterPosition))
    }
}