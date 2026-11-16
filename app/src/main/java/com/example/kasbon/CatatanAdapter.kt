package com.example.kasbon

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kasbon.databinding.ItemCatatanBinding

// Menampilkan daftar catatan di RecyclerView
class CatatanAdapter(
    private val onKlik: (Catatan) -> Unit,
    private val onHapus: (Catatan) -> Unit
) : ListAdapter<Catatan, CatatanAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Catatan>() {
        override fun areItemsTheSame(a: Catatan, b: Catatan) = a.id == b.id
        override fun areContentsTheSame(a: Catatan, b: Catatan) = a == b
    }

    class VH(val binding: ItemCatatanBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCatatanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val ctx = holder.itemView.context
        with(holder.binding) {
            tvNama.text = item.nama
            tvJenis.text = if (item.jenis == Catatan.JENIS_PIUTANG) {
                ctx.getString(R.string.jenis_piutang)
            } else {
                ctx.getString(R.string.jenis_hutang)
            }
            tvNominal.text = Format.rupiah(item.nominal)
            tvJatuhTempo.text = ctx.getString(R.string.label_jatuh_tempo, Format.tanggal(item.jatuhTempo))
            tvStatus.text = if (item.lunas) {
                ctx.getString(R.string.status_lunas)
            } else {
                ctx.getString(R.string.status_belum_lunas)
            }
            root.setOnClickListener { onKlik(item) }
            btnHapus.setOnClickListener { onHapus(item) }
        }
    }
}
