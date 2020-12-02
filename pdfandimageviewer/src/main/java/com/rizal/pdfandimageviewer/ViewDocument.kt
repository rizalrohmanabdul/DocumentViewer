package com.rizal.pdfandimageviewer

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.util.Base64
import android.view.View
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.link.DefaultLinkHandler
import java.io.File
import java.lang.Exception

class ViewDocument @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr), DownloadFile.Listener {

    private val wrapperContent : LinearLayout
    private val wrapperProgress : ProgressBar
    private val wrapperRetry: LinearLayout
    private val vPdfContent : PDFView
    private val imageView: ImageView

    init {
        View.inflate(context, R.layout.view_document, this)
        wrapperContent = findViewById(R.id.wrapper)
        wrapperProgress = findViewById(R.id.wrapperProgress)
        wrapperRetry = findViewById(R.id.wrapperRetry)
        vPdfContent = findViewById(R.id.vPdfContent)
        imageView = findViewById(R.id.ivContent)
    }

    fun setupPdfViews(url: String){
        if (url.isNotEmpty()){
            setupImageorPdf(url, FileUtil.extractFileNameFromURL(url))
        } else {
            Toast.makeText(context, "URL not found", Toast.LENGTH_SHORT).show()
            return
        }
    }

    fun loadPdfFromBase64(pdf: String) {
        val byteArray = Base64.decode(pdf, Base64.DEFAULT)
        vPdfContent
            .fromBytes(byteArray)
            .onLoad {
                wrapperProgress.visibility = View.GONE
                wrapperRetry.visibility = View.GONE
            }
            .onError {
                Toast.makeText(context, "Cannot read content.", Toast.LENGTH_SHORT).show()
            }
            .enableAnnotationRendering(true)
            .linkHandler(DefaultLinkHandler(vPdfContent))
            .load()
    }

    fun setupImageorPdf(url: String, fileName: String) {
        wrapperProgress.visibility = View.VISIBLE
        wrapperRetry.visibility = View.GONE
        val downloadHandler = DownloadFileUrlConnectionImpl(context, Handler(), this)
        downloadHandler
            .download(url, File(context.cacheDir, fileName).absolutePath)
    }

    fun onRetry(action: () -> Unit) = with(wrapperRetry){
        setOnClickListener { action() }
    }

    fun showRetry(){
        wrapperRetry.visibility = View.VISIBLE
    }

    override fun onSuccess(url: String?, destinationPath: String?) {
        if(destinationPath == null) {
            onFailure(IllegalStateException("Path not found"))
        } else
            vPdfContent
                .fromFile(File(destinationPath))
                .onLoad {
                    wrapperProgress.visibility = View.GONE
                    wrapperRetry.visibility = View.GONE
                }
                .onError {
                    if (!destinationPath.endsWith(".pdf")){
                        vPdfContent.visibility = View.GONE
                        wrapperProgress.visibility = View.GONE
                        imageView.visibility = View.VISIBLE
                        imageView.load(destinationPath)
                    } else {
                        wrapperRetry.visibility = View.VISIBLE
                        onFailure(IllegalStateException("Cannot load file"))
                    }
                }
                .enableAnnotationRendering(true)
                .linkHandler(DefaultLinkHandler(vPdfContent))
                .load()
    }

    override fun onFailure(e: Exception?) {
        wrapperRetry.visibility = View.VISIBLE
        wrapperProgress.visibility = View.GONE
        Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
    }

    override fun onProgressUpdate(progress: Int, total: Int) {
        showProgress(progress, total)
    }

    private fun showProgress(value: Int, maxValue: Int = 100) {
        wrapperProgress.max = maxValue
        wrapperProgress.progress = value
    }

    private fun ImageView.load(data: String?) {
        Glide.with(this)
            .load(data)
            .apply(
                RequestOptions()
                    .centerInside()
            )
            .into(this)
    }

}