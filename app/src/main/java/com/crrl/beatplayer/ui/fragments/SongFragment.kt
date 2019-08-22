package com.crrl.beatplayer.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.crrl.beatplayer.R
import com.crrl.beatplayer.alertdialog.AlertDialog
import com.crrl.beatplayer.alertdialog.dialogs.AlertItemAction
import com.crrl.beatplayer.alertdialog.stylers.AlertItemStyle
import com.crrl.beatplayer.alertdialog.stylers.AlertItemTheme
import com.crrl.beatplayer.alertdialog.stylers.AlertType
import com.crrl.beatplayer.extensions.getColorByTheme
import com.crrl.beatplayer.extensions.observe
import com.crrl.beatplayer.models.Song
import com.crrl.beatplayer.ui.fragments.base.BaseFragment
import com.crrl.beatplayer.ui.modelview.SongAdapter
import com.crrl.beatplayer.ui.viewmodels.SongViewModel
import com.crrl.beatplayer.utils.SettingsUtility
import com.crrl.beatplayer.utils.SortModes
import com.dgreenhalgh.android.simpleitemdecoration.linear.EndOffsetItemDecoration
import kotlinx.android.synthetic.main.song_fragment.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

@Suppress("UNCHECKED_CAST")
class SongFragment : BaseFragment<Song>() {

    private val viewModel: SongViewModel by viewModel { parametersOf(context) }
    private lateinit var songAdapter: SongAdapter

    companion object {
        fun newInstance() = SongFragment()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.song_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
    }

    private fun init(view: View) {
        // Set up adapter
        songAdapter = SongAdapter(activity).apply {
            showHeader = true
            itemClickListener = this@SongFragment
        }

        // Set up RecyclerView
        view.song_list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = songAdapter
            addItemDecoration(EndOffsetItemDecoration(resources.getDimensionPixelOffset(R.dimen.song_item_size)))
        }

        dialog = buildSortModesDialog()

        reloadAdapter()
    }

    private fun buildSortModesDialog(): AlertDialog {
        val style = AlertItemStyle().apply {
            textColor = activity?.getColorByTheme(R.attr.titleTextColor, "titleTextColor")!!
            selectedTextColor = activity?.getColorByTheme(R.attr.colorAccent, "colorAccent")!!
            backgroundColor =
                activity?.getColorByTheme(R.attr.colorPrimarySecondary, "colorPrimarySecondary")!!
        }
        val alert = AlertDialog(
            getString(R.string.sort_title),
            getString(R.string.sort_msg),
            style,
            AlertType.DIALOG
        )
        alert.addItem(AlertItemAction(
            context!!.getString(R.string.sort_default),
            SettingsUtility.getInstance(context).songSortOrder == SortModes.SongModes.SONG_DEFAULT,
            AlertItemTheme.DEFAULT
        ) { action ->
            action.selected = true
            SettingsUtility.getInstance(context).songSortOrder = SortModes.SongModes.SONG_DEFAULT
            reloadAdapter()
        })
        alert.addItem(AlertItemAction(
            context!!.getString(R.string.sort_az),
            SettingsUtility.getInstance(context).songSortOrder == SortModes.SongModes.SONG_A_Z,
            AlertItemTheme.DEFAULT
        ) { action ->
            action.selected = true
            SettingsUtility.getInstance(context).songSortOrder = SortModes.SongModes.SONG_A_Z
            reloadAdapter()
        })
        alert.addItem(AlertItemAction(
            context!!.getString(R.string.sort_za),
            SettingsUtility.getInstance(context).songSortOrder == SortModes.SongModes.SONG_Z_A,
            AlertItemTheme.DEFAULT
        ) { action ->
            action.selected = true
            SettingsUtility.getInstance(context).songSortOrder = SortModes.SongModes.SONG_Z_A
            reloadAdapter()
        })
        alert.addItem(AlertItemAction(
            context!!.getString(R.string.sort_duration),
            SettingsUtility.getInstance(context).songSortOrder == SortModes.SongModes.SONG_DURATION,
            AlertItemTheme.DEFAULT
        ) { action ->
            action.selected = true
            SettingsUtility.getInstance(context).songSortOrder = SortModes.SongModes.SONG_DURATION
            reloadAdapter()
        })
        alert.addItem(AlertItemAction(
            context!!.getString(R.string.sort_year),
            SettingsUtility.getInstance(context).songSortOrder == SortModes.SongModes.SONG_YEAR,
            AlertItemTheme.DEFAULT
        ) { action ->
            action.selected = true
            SettingsUtility.getInstance(context).songSortOrder = SortModes.SongModes.SONG_YEAR
            reloadAdapter()
        })
        alert.addItem(AlertItemAction(
            context!!.getString(R.string.sort_last_added),
            SettingsUtility.getInstance(context).songSortOrder == SortModes.SongModes.SONG_LAST_ADDED,
            AlertItemTheme.DEFAULT
        ) { action ->
            action.selected = true
            SettingsUtility.getInstance(context).songSortOrder = SortModes.SongModes.SONG_LAST_ADDED
            reloadAdapter()
        })
        return alert
    }

    private fun reloadAdapter() {
        viewModel.liveData.observe(this) { list ->
            songAdapter.updateDataSet(list)
        }
    }

    override fun addToList(playListId: Long, song: Song) {
        viewModel.addToPlaylist(playListId, arrayOf(song.id).toLongArray())
    }

    override fun onItemClick(view: View, position: Int, item: Song) {
        Toast.makeText(context, "MediaItem: ${item.title}", Toast.LENGTH_LONG).show()
    }

    override fun onShuffleClick(view: View) {
        Toast.makeText(context, "Shuffle", Toast.LENGTH_LONG).show()
    }

    override fun onSortClick(view: View) {
        dialog.show(activity as AppCompatActivity)
    }

    override fun onPlayAllClick(view: View) {
        Toast.makeText(context, "Play All", Toast.LENGTH_LONG).show()
    }

    override fun onPopupMenuClick(view: View, position: Int, item: Song) {
        powerMenu!!.showAsAnchorRightTop(view)
        viewModel.playLists().observe(this) {
            buildPlaylistMenu(it, item)
        }
    }
}