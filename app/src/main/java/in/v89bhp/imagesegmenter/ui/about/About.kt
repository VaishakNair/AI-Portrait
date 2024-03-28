package `in`.v89bhp.imagesegmenter.ui.about

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import `in`.v89bhp.imagesegmenter.BuildConfig
import `in`.v89bhp.imagesegmenter.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun About(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    val launcherIconBitmap = ResourcesCompat.getDrawable(
        context.resources,
        R.mipmap.ic_launcher, context.theme
    )?.let { drawable ->
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.about))
                },
                actions = {

                },
                navigationIcon = {
                    IconButton(onClick = {
                        navigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                })

        }) { contentPadding ->
        Card(
            modifier = modifier
                .padding(contentPadding).padding(16.dp),

            ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    bitmap = launcherIconBitmap!!.asImageBitmap(), contentDescription = "89 bhp",
                    modifier = Modifier.size(110.dp)
                )

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = stringResource(R.string.version_name, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = stringResource(R.string.connect_with_me_message),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.contact_message_above),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                val email = stringResource(R.string.email)
                HyperlinkedText(text = email,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:") // only email apps should handle this
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                        }
                        context.startActivity(Intent.createChooser(intent, "Email"))
                    })

                Text(
                    text = stringResource(R.string.contact_message_below),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )


                Button(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            context.getString(R.string.share_app_message)
                        )
                    }

                    context.startActivity(Intent.createChooser(intent, "Share Link"))
                }) {
                    Text(text = stringResource(R.string.share_app))
                }

                HyperlinkedText(text = stringResource(R.string.privacy_policy),
                    modifier = Modifier.padding(top = 32.dp),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://89bhp.in/privacy_policy.html")
                        }
                        context.startActivity(intent)
                    })
                HyperlinkedText(text = stringResource(R.string.gauge_credit),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://github.com/anastr/SpeedView")
                        }
                        context.startActivity(intent)
                    })

            }
        }
    }


}

@Composable
fun HyperlinkedText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val annotatedText = buildAnnotatedString {
        val startIndex = 0
        val endIndex = text.length
        append(text)
        addStyle(
            style = SpanStyle(
                color = Color(0xff64B5F6),
                textDecoration = TextDecoration.Underline
            ), start = startIndex, end = endIndex
        )

        // Attach a string annotation that stores a email id to
        // hyperlinked email id shown:
        addStringAnnotation(
            tag = "hyperlink",
            annotation = text,
            start = startIndex,
            end = endIndex
        )
    }

    ClickableText(
        modifier = modifier.wrapContentWidth(),
        text = annotatedText,
        style = MaterialTheme.typography.bodyLarge,
        onClick = { offset ->
            annotatedText.getStringAnnotations(
                tag = "hyperlink", start = offset, end = offset
            ).firstOrNull()?.let { annotation ->
                // If yes, we log its value
                onClick()
                Log.d("Clicked URL", annotation.item)
            }
        })
}