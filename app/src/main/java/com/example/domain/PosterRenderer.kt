package com.example.domain

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.data.model.PosterTemplate
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object PosterRenderer {

    private data class TemplatePalette(
        val bgStart: Int,
        val bgEnd: Int,
        val accent: Int,
        val accentSecondary: Int,
        val cardBg: Int,
        val badgeText: Int
    )

    private fun paletteFor(template: PosterTemplate): TemplatePalette = when (template) {
        PosterTemplate.VETEMENTS_LUXE -> TemplatePalette(
            bgStart = Color.parseColor("#0B0D13"),
            bgEnd = Color.parseColor("#1D1710"),
            accent = Color.parseColor("#F59E0B"),
            accentSecondary = Color.parseColor("#FDE68A"),
            cardBg = Color.parseColor("#161B26"),
            badgeText = Color.parseColor("#1A1000")
        )
        PosterTemplate.RESTO_SAVEURS -> TemplatePalette(
            bgStart = Color.parseColor("#180C0A"),
            bgEnd = Color.parseColor("#2B130B"),
            accent = Color.parseColor("#FB923C"),
            accentSecondary = Color.parseColor("#FACC15"),
            cardBg = Color.parseColor("#241410"),
            badgeText = Color.parseColor("#1E0B05")
        )
        PosterTemplate.COSMETIQUE_ECLAT -> TemplatePalette(
            bgStart = Color.parseColor("#071512"),
            bgEnd = Color.parseColor("#0F2420"),
            accent = Color.parseColor("#10B981"),
            accentSecondary = Color.parseColor("#A7F3D0"),
            cardBg = Color.parseColor("#10221E"),
            badgeText = Color.parseColor("#022017")
        )
        PosterTemplate.CHAUSSURES_URBAN -> TemplatePalette(
            bgStart = Color.parseColor("#090D1A"),
            bgEnd = Color.parseColor("#121E36"),
            accent = Color.parseColor("#38BDF8"),
            accentSecondary = Color.parseColor("#F59E0B"),
            cardBg = Color.parseColor("#131E33"),
            badgeText = Color.parseColor("#041926")
        )
        PosterTemplate.PROMO_FLASH -> TemplatePalette(
            bgStart = Color.parseColor("#120914"),
            bgEnd = Color.parseColor("#211226"),
            accent = Color.parseColor("#FBBF24"),
            accentSecondary = Color.parseColor("#F43F5E"),
            cardBg = Color.parseColor("#1E1226"),
            badgeText = Color.parseColor("#1D0B00")
        )
    }

    fun renderPosterBitmap(
        businessName: String,
        productName: String,
        priceFcfa: Long,
        description: String,
        promoBadge: String,
        contactPhone: String,
        template: PosterTemplate,
        productImage: Bitmap?,
        showWatermark: Boolean
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = paletteFor(template)

        // 1. Background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                palette.bgStart, palette.bgEnd, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Decorative glow circle top right
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent
            alpha = 32
        }
        canvas.drawCircle(width - 140f, 160f, 340f, glowPaint)
        canvas.drawCircle(120f, height - 180f, 260f, glowPaint)

        // Outer subtle accent frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = palette.accent
            alpha = 110
        }
        canvas.drawRoundRect(RectF(34f, 34f, width - 34f, height - 34f), 42f, 42f, framePaint)

        // 2. Top Brand Header
        val shopPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accentSecondary
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText(
            businessName.ifBlank { "MA BOUTIQUE" }.uppercase().take(26),
            76f,
            116f,
            shopPaint
        )

        // Promo pill top right
        val badgeText = promoBadge.ifBlank { template.categoryHint.uppercase() }.take(18)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent }
        val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.badgeText
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val textWidth = badgeTextPaint.measureText(badgeText)
        val pillRect = RectF(width - 76f - textWidth - 48f, 70f, width - 76f, 132f)
        canvas.drawRoundRect(pillRect, 31f, 31f, badgeBgPaint)
        canvas.drawText(badgeText, pillRect.left + 24f, pillRect.top + 41f, badgeTextPaint)

        // 3. Product Photo Container
        val photoRect = RectF(76f, 174f, width - 76f, 774f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.cardBg }
        canvas.drawRoundRect(photoRect, 36f, 36f, cardPaint)

        if (productImage != null) {
            canvas.save()
            val clipPath = android.graphics.Path().apply {
                addRoundRect(photoRect, 36f, 36f, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            val srcRatio = productImage.width.toFloat() / productImage.height.toFloat()
            val dstRatio = photoRect.width() / photoRect.height()
            val drawRect = if (srcRatio > dstRatio) {
                val scaledWidth = photoRect.height() * srcRatio
                val dx = (scaledWidth - photoRect.width()) / 2f
                RectF(photoRect.left - dx, photoRect.top, photoRect.right + dx, photoRect.bottom)
            } else {
                val scaledHeight = photoRect.width() / srcRatio
                val dy = (scaledHeight - photoRect.height()) / 2f
                RectF(photoRect.left, photoRect.top - dy, photoRect.right, photoRect.bottom + dy)
            }
            canvas.drawBitmap(productImage, null, drawRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            val placeholderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.accentSecondary
                alpha = 180
                textSize = 36f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText(
                "VISUEL PRODUIT",
                photoRect.centerX(),
                photoRect.centerY(),
                placeholderPaint
            )
        }

        // Photo border
        val photoStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = palette.accent
            alpha = 160
        }
        canvas.drawRoundRect(photoRect, 36f, 36f, photoStroke)

        // 4. Price Tag Pill overlapping photo bottom right
        val formattedPrice = CopyGenerator.formatFcfa(priceFcfa)
        val pricePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.badgeText
            textSize = 46f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val priceW = pricePaint.measureText(formattedPrice)
        val priceRect = RectF(width - 96f - priceW - 60f, 728f, width - 96f, 822f)
        val priceBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent }
        canvas.drawRoundRect(priceRect, 24f, 24f, priceBg)
        canvas.drawText(formattedPrice, priceRect.left + 30f, priceRect.top + 63f, pricePaint)

        // 5. Product Name
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 58f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val titleLayout = StaticLayout.Builder.obtain(
            productName.ifBlank { "Produit Vedette" },
            0,
            productName.ifBlank { "Produit Vedette" }.length,
            titlePaint,
            width - 152
        ).setMaxLines(2).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(76f, 860f)
        titleLayout.draw(canvas)
        canvas.restore()

        // 6. Description
        val descText = description.ifBlank { "Disponible dès maintenant. Qualité garantie." }
        val descPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val descLayout = StaticLayout.Builder.obtain(
            descText, 0, descText.length, descPaint, width - 152
        ).setMaxLines(3).build()
        canvas.save()
        canvas.translate(76f, 1005f)
        descLayout.draw(canvas)
        canvas.restore()

        // 7. Bottom CTA Bar
        val ctaRect = RectF(76f, 1175f, width - 76f, 1272f)
        val ctaBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.cardBg }
        canvas.drawRoundRect(ctaRect, 24f, 24f, ctaBg)
        canvas.drawRoundRect(ctaRect, 24f, 24f, photoStroke)

        val ctaText = if (contactPhone.isNotBlank()) {
            "Commandez maintenant : $contactPhone"
        } else {
            "Commandez en message privé dès aujourd'hui"
        }
        val ctaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accentSecondary
            textSize = 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val textBounds = Rect()
        ctaPaint.getTextBounds(ctaText, 0, ctaText.length, textBounds)
        canvas.drawText(
            ctaText,
            ctaRect.centerX(),
            ctaRect.centerY() + textBounds.height() / 2f,
            ctaPaint
        )

        if (showWatermark) {
            val wmPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = 22f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Créé avec KOMBA BOOST", width - 76f, height - 48f, wmPaint)
        }

        return bitmap
    }

    fun decodeUriToScaledBitmap(context: Context, uri: Uri, maxDim: Int = 960): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val raw = BitmapFactory.decodeStream(stream) ?: return null
                val scale = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height, 1f)
                if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        raw,
                        (raw.width * scale).toInt().coerceAtLeast(1),
                        (raw.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    raw
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun bitmapToCompactBase64(bitmap: Bitmap, maxDim: Int = 480, quality: Int = 65): String {
        val scale = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    fun base64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun savePosterToGallery(context: Context, bitmap: Bitmap, title: String): Result<String> {
        return try {
            val safeName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "affiche" }
            val filename = "KOMBA_${safeName}_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KombaBoost")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("Impossible d'enregistrer dans la galerie"))
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                Result.success("Affiche enregistrée dans Pictures/KombaBoost")
            } else {
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Result.success("Affiche enregistrée : ${file.name}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sharePosterBitmap(context: Context, bitmap: Bitmap, caption: String) {
        val cacheDir = File(context.cacheDir, "shared_posters").apply { mkdirs() }
        val file = File(cacheDir, "komba_poster_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager l'affiche publicitaire"))
    }
}
